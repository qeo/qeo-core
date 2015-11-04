/*
 * Copyright (c) 2015 - Qeo LLC
 *
 * The source code form of this Qeo Open Source Project component is subject
 * to the terms of the Clear BSD license.
 *
 * You can redistribute it and/or modify it under the terms of the Clear BSD
 * License (http://directory.fsf.org/wiki/License:ClearBSD). See LICENSE file
 * for more details.
 *
 * The Qeo Open Source Project also includes third party Open Source Software.
 * See LICENSE file for more details.
 */

#ifndef DEBUG
#define NDEBUG
#endif

#include <string.h>
#include <stdbool.h>

#include <dds/dds_aux.h>

#include <qeo/log.h>
#include <qeo/mgmt_client_forwarder.h>
#include <qeo/mgmt_cert_parser.h>

#include "forwarder.h"
#include "core.h"
#include "config.h"
#include "core_util.h"
#include "security.h"
#include "security_util.h"

#include "qdm/qeo_Forwarder.h"
#include "qdm/qeo_RegistrationRequest.h"
#include "qdm/qeo_RegistrationCredentials.h"

#define bool2str(b) (b) ? "true" : "false"

/**
 * Local static variables to handle forwarding of registration
 * requests/responses to and from the open domain
 */
static qeocore_reader_t *_fwd_reg_req_reader_open;
static qeocore_writer_t *_fwd_reg_cred_writer_open;
static qeocore_reader_t *_fwd_reg_cred_reader;
static qeocore_writer_t *_fwd_reg_req_writer;
static qeocore_type_t   *_fwd_reg_req_type_open;
static qeocore_type_t   *_fwd_reg_cred_type_open;
static qeocore_type_t   *_fwd_reg_req_type;
static qeocore_type_t   *_fwd_reg_cred_type;


static void on_reg_req_update(const qeocore_reader_t *reader,
                              const qeocore_data_t   *data,
                              uintptr_t              userdata);

static qeocore_reader_listener_t _listener_open =
{
    .on_data          = on_reg_req_update,
    .on_policy_update = NULL,
    .userdata         = 0
};

static void on_reg_cred_update(const qeocore_reader_t *reader,
                               const qeocore_data_t   *data,
                               uintptr_t              userdata);

static qeocore_reader_listener_t _listener =
{
    .on_data          = on_reg_cred_update,
    .on_policy_update = NULL,
    .userdata         = 0
};

static void registration_proxy_teardown(const qeo_factory_t* closed_domain_factory); // forward
static bool registration_proxy_setup(qeo_factory_t* closed_domain_factory); //forward

/**
 * The possible events used in the Client state machine
 */
typedef enum {
    CLIENT_EVENT_START,                 /**< Start state machine */
    CLIENT_EVENT_TIMEOUT,               /**< A timeout occurred */
    CLIENT_EVENT_LOC_SRV_DATA_RECEIVED, /**< Data from the location service has been received */
    CLIENT_EVENT_FWD_DATA_RECEIVED,     /**< Data on the QeoFwd topic received */
    CLIENT_EVENT_FWD_DATA_REMOVED,      /**< The data is removed */
} client_state_events_t;

static qeo_retcode_t fwd_locator_create(qeo_factory_t *factory);
static void fwd_locator_destroy(qeo_factory_t *factory);

static void fwd_server_timeout(uintptr_t userdata);
static qeo_retcode_t fwd_server_instance_remove(const qeo_factory_t *factory);
static qeo_retcode_t fwd_server_instance_publish(const qeo_factory_t *factory);

static void fwd_client_discovery_timeout(uintptr_t userdata);

static void fwd_client_reconfig(qeo_factory_t *factory,
                                qeo_mgmt_client_locator_t *locator,
                                int64_t device_id,
                                flags_t flags);
static void client_state_machine_eval_ul(qeo_factory_t *factory,
                                         client_state_events_t event,
                                         qeo_mgmt_client_locator_t *locator,
                                         int64_t device_id,
                                         flags_t flags);
static void client_state_machine_eval(qeo_factory_t *factory,
                                      client_state_events_t event,
                                      qeo_mgmt_client_locator_t *locator,
                                      int64_t device_id,
                                      flags_t flags);


void notify_cb(DDS_DomainId_t domain_id,
               DDS_BuiltinTopicKey_t *client_key,
               DDS_ActivitiesClientState state)
{
    qeo_log_i("Domain: %d, topic: %x-%x-%x, state: %d", domain_id, client_key->value[0], client_key->value[1], client_key->value[2], state);
}

/** This callback will be called for each forwarder in the list received from the management client. */
qeo_mgmt_client_retcode_t forwarder_cb(qeo_mgmt_client_forwarder_t* forwarder, void *cookie)
{
    qeo_factory_t             *factory = (qeo_factory_t *) cookie;
    qeo_mgmt_client_locator_t *locator = NULL;
    int                       i = 0;

    /* Get the IP address and port from the qeo_mgmt_client_forwarder_t info. */
    if ((forwarder != NULL) && (forwarder->nrOfLocators > 0)) {
        qeo_log_d("received %d locators", forwarder->nrOfLocators);
        for (i = 0, locator = forwarder->locators; i < forwarder->nrOfLocators; i++, locator++) {
            qeo_log_d("locator %d", i);
            if (locator != NULL) {
                qeo_log_i("valid locator: %s:%d", locator->address, locator->port);
                if (factory->fwd.u.client.state == FWD_CLIENT_STATE_WAIT) {
                    qeo_log_i("Going to use this forwarder");
                    flags_t flags = { .forwarding_enabled = true };
                    client_state_machine_eval(factory, CLIENT_EVENT_LOC_SRV_DATA_RECEIVED, locator, forwarder->deviceID, flags);
                }
                else {
                    /* TODO: at the moment only one forwarder is taken into account. */
                    qeo_log_i("Going to ignore this forwarder as not the first in the list");
                }
                break;
            }
        }
        qeo_mgmt_client_free_forwarder(forwarder);
    }

    return QMGMTCLIENT_OK;
}

void result_cb(qeo_mgmt_client_retcode_t result, void *cookie)
{
    qeo_factory_t             *factory = (qeo_factory_t *) cookie;

    /* Allow other threads to wait until no request is pending anymore. */
    lock(&factory->mutex);
    factory->fwd.rqst_pending = false;
    if ((factory->fwd.timeout * 2) < qeocore_parameter_get_number("FWD_LOC_SRV_MAX_TIMEOUT")) {
        factory->fwd.timeout = factory->fwd.timeout * 2;
    } else {
        factory->fwd.timeout = qeocore_parameter_get_number("FWD_LOC_SRV_MAX_TIMEOUT");
    }
    pthread_cond_broadcast(&factory->fwd.wait_rqst_finished);
    unlock(&factory->mutex);

}

qeo_mgmt_client_retcode_t ssl_ctx_cb(SSL_CTX *ctx, void *cookie)
{
    qeo_mgmt_client_retcode_t client_ret  = QMGMTCLIENT_EFAIL;
    qeo_retcode_t             qeo_ret     = QEO_OK;
    qeo_security_hndl         qeo_sec   = (qeo_security_hndl)cookie;
    EVP_PKEY                  *key        = NULL;

    STACK_OF(X509) * certs = NULL;
    X509  *user_cert  = NULL;
    X509  *cert       = NULL;
    int   i           = 0;

    do {
        if (qeo_sec == NULL) {
            qeo_log_e("qeoSecPol == NULL");
            break;
        }

        qeo_ret = qeo_security_get_credentials(qeo_sec, &key, &certs);
        if (qeo_ret != QEO_OK) {
            qeo_log_e("failed to get credentials");
            break;
        }

        if (sk_X509_num(certs) <= 1) {
            qeo_log_e("not enough certificates in chain");
            break;
        }

        user_cert = sk_X509_value(certs, 0);
        if (user_cert == NULL) {
            qeo_log_e("user_cert == NULL");
            break;
        }

        if (!SSL_CTX_use_certificate(ctx, user_cert)) {
            qeo_log_e("SSL_CTX_use_certificate failed");
            break;
        }
        if (!SSL_CTX_use_PrivateKey(ctx, key)) {
            qeo_log_e("SSL_CTX_use_PrivateKey failed");
            break;
        }

        if (!SSL_CTX_check_private_key(ctx)) {
            qeo_log_e("SSL_CTX_check_private_key failed");
            break;
        }

        security_util_configure_ssl_ctx(ctx);
        for (i = 1; i < sk_X509_num(certs); i++) {
            qeo_log_i("add cert: %d", i);
            cert = sk_X509_value(certs, i);
            if (cert == NULL) {
                qeo_log_e("cert == NULL");
                break;
            }

            if (!X509_STORE_add_cert(SSL_CTX_get_cert_store(ctx), cert)) {
                dump_openssl_error_stack("X509_STORE_add_cert failed");
                break;
            }
        }

        client_ret = QMGMTCLIENT_OK;
    } while (0);

    return client_ret;
}

/* ===[ locator API ]==================== */

static qeo_retcode_t fwd_locator_create(qeo_factory_t *factory)
{
    qeo_retcode_t rc = QEO_OK;

    if (NULL == factory->fwd.locator) {
        factory->fwd.locator = calloc(1, sizeof(qeo_mgmt_client_locator_t));
    }
    if (NULL == factory->fwd.locator) {
        rc = QEO_ENOMEM;
    }

    return rc;
}

static void fwd_locator_destroy(qeo_factory_t *factory)
{
    if (factory->fwd.locator != NULL) {
        if (factory->fwd.locator->address != NULL) {
            free(factory->fwd.locator->address);
        }
        free(factory->fwd.locator);
        factory->fwd.locator = NULL;
    }
}

static qeo_retcode_t fwd_locator_update(qeo_factory_t *factory, qeo_mgmt_client_locator_t *locator)
{
    qeo_retcode_t rc = QEO_OK;

    if (NULL != factory->fwd.locator->address) {
        free(factory->fwd.locator->address);
    }
    factory->fwd.locator->type = locator->type;
    if (-1 != locator->port) {
        factory->fwd.locator->port = locator->port;
    }
    factory->fwd.locator->address = strdup(locator->address);
    if (NULL == factory->fwd.locator->address) {
        rc = QEO_ENOMEM;
    }

    return rc;
}

static qeo_retcode_t fwd_server_register(qeo_factory_t *factory)
{
    qeo_retcode_t             rc = QEO_OK;

    if (!qeocore_parameter_get_number("FWD_DISABLE_LOCATION_SERVICE")) {
        qeo_security_hndl         qeo_sec = factory->qeo_sec;
        qeo_mgmt_client_retcode_t mgmt_rc = QMGMTCLIENT_EFAIL;
        qeo_mgmt_client_ctx_t     *mgmt_client_ctx = NULL;
        qeo_mgmt_client_locator_t locator={factory->fwd.locator->type, factory->fwd.locator->address, factory->fwd.locator->port};
        int                       nrOfLocators = 1;

        do {
            if (QMGMT_LOCATORTYPE_UNKNOWN == locator.type) {
                qeo_log_d("Can't register this locator type");
                break;
            }
            if ((rc = qeo_security_get_mgmt_client_ctx(qeo_sec, &mgmt_client_ctx)) != QEO_OK) {
                qeo_log_e("register_forwarder get security mgmt client failed (rc=%d)", rc);
                break;
            }
            /* Now register the forwarder. */
            qeo_log_i("register the forwarder with locator address %s:port %d\n", locator.address, locator.port);
            if ((mgmt_rc = qeo_mgmt_client_register_forwarder(mgmt_client_ctx,
                                                              factory->qeo_id.url,
                                                              &locator,
                                                              nrOfLocators,
                                                              ssl_ctx_cb,
                                                              qeo_sec)) != QMGMTCLIENT_OK) {
                qeo_log_e("register forwarder failed (rc=%d)", mgmt_rc);
                rc = QEO_EFAIL;
                break;
            }

        } while (0);
    }
    return rc;
}

static qeo_retcode_t fwd_server_unregister(qeo_factory_t *factory)
{
    qeo_retcode_t             rc = QEO_OK;

    if (!qeocore_parameter_get_number("FWD_DISABLE_LOCATION_SERVICE")) {
        qeo_security_hndl         qeo_sec = factory->qeo_sec;
        qeo_mgmt_client_retcode_t mgmt_rc = QMGMTCLIENT_EFAIL;
        qeo_mgmt_client_ctx_t     *mgmt_client_ctx = NULL;
        int                       nrOfLocators = 0;


        do {
            if (QEO_OK != (rc = qeo_security_get_mgmt_client_ctx(qeo_sec, &mgmt_client_ctx))) {
                qeo_log_e("unregister_forwarder get security mgmt client failed (rc=%d)", rc);
                break;
            }

            /* Now register the forwarder. */
            qeo_log_i("unregister the forwarder");
            if ((mgmt_rc = qeo_mgmt_client_register_forwarder(mgmt_client_ctx,
                                                              factory->qeo_id.url,
                                                              NULL,
                                                              nrOfLocators,
                                                              ssl_ctx_cb,
                                                              qeo_sec)) != QMGMTCLIENT_OK) {
                qeo_log_e("unregister forwarder failed (rc=%d)", mgmt_rc);
                rc = QEO_EFAIL;
                break;
            }

        } while (0);
    }
    return rc;
}

void fwd_destroy(qeo_factory_t *factory)
{
    lock(&factory->mutex);
    if (true == factory->fwd.rqst_pending) {
        qeo_security_hndl         qeo_sec = factory->qeo_sec;
        qeo_mgmt_client_ctx_t     *mgmt_client_ctx = NULL;

        if (qeo_security_get_mgmt_client_ctx(qeo_sec, &mgmt_client_ctx) == QEO_OK) {
            qeo_mgmt_client_ctx_stop(mgmt_client_ctx);
        }

        /* Wait untill the fwd request is finished before continuing. */
        qeo_log_i("waiting for the fwd request to finish.");
        pthread_cond_wait(&factory->fwd.wait_rqst_finished, &factory->mutex);
    }
    unlock(&factory->mutex);
    registration_proxy_teardown(factory);
    if (factory->flags.is_server) {
        fwd_server_unregister(factory);
        if (FWD_STATE_ENABLED == factory->fwd.u.server.state) {
            if (QEO_OK != fwd_server_instance_remove(factory)) {
                qeo_log_e("failed to remove instance from forwarder topic");
            }
        }
        if (NULL != factory->fwd.u.server.writer) {
            qeocore_writer_close(factory->fwd.u.server.writer);
        }
    }
    if (NULL != factory->fwd.reader) {
        qeocore_reader_close(factory->fwd.reader);
    }
    if (NULL != factory->fwd.timer) {
        DDS_Timer_delete(factory->fwd.timer);
    }
    pthread_cond_destroy(&factory->fwd.wait_rqst_finished);
    fwd_locator_destroy(factory);
}

static qeo_retcode_t fwd_get_list(qeo_factory_t *factory)
{
    qeo_retcode_t             rc = QEO_OK;
    qeo_security_hndl         qeo_sec = factory->qeo_sec;
    qeo_mgmt_client_retcode_t mgmt_rc = QMGMTCLIENT_EFAIL;
    qeo_mgmt_client_ctx_t     *mgmt_client_ctx = NULL;

    do {
        if ((rc = qeo_security_get_mgmt_client_ctx(qeo_sec, &mgmt_client_ctx)) != QEO_OK) {
            qeo_log_e("get_forwarders get security mgmt client failed (rc=%d)", rc);
            break;
        }
        /* Factory is already locked when calling this function. */
        if (true == factory->fwd.rqst_pending) {
            /* Just break, we will retry later. */
            qeo_log_i("no need to send request (previous fwd request still ongoing)");
            break;
        }

        /* Now get the list of forwarders. */
        factory->fwd.rqst_pending = true;
        mgmt_rc = qeo_mgmt_client_get_forwarders(mgmt_client_ctx, factory->qeo_id.url, forwarder_cb, result_cb,
                                                 factory, ssl_ctx_cb, qeo_sec);
        if (mgmt_rc == QMGMTCLIENT_OK) {
            qeo_log_d("get_forwarders succeeded");
        } else {
            factory->fwd.rqst_pending = false; /* result callback will not be called. */
            if ((mgmt_rc == QMGMTCLIENT_ESSL) || (mgmt_rc == QMGMTCLIENT_ENOTALLOWED)) {
                qeo_log_e("get_forwarders failed (rc=%d), aborting", mgmt_rc);
                rc = QEO_EFAIL;
                break;
            }
            else {
                qeo_log_e("get_forwarders failed (rc=%d), ignoring", mgmt_rc);
            }
        }

    } while (0);
    return rc;
}

/* ===[ Forwarder state machine ]=========================================== */

static qeo_retcode_t fwd_server_start_forwarding(qeo_factory_t *factory)
{
    qeo_retcode_t rc = QEO_EFAIL;
    DDS_ReturnCode_t ddsrc = DDS_RETCODE_OK;

    if (factory->flags.forwarding_enabled) {
        char buf[64];

        snprintf(buf, sizeof(buf), "%s:%d", factory->fwd.locator->address, factory->fwd.locator->port);
        ddsrc = DDS_parameter_set("TCP_PUBLIC", buf);
        if (DDS_RETCODE_OK != ddsrc) {
            qeo_log_e("failed to set public IP address");
        }
    }
    if (DDS_RETCODE_OK == ddsrc) {
        rc = fwd_server_instance_publish(factory);
        if (QEO_OK != rc) {
            qeo_log_e("failed to publish instance on forwarder topic");
        }
        else {
            rc = fwd_server_register(factory);
            if (QEO_OK != rc) {
                qeo_log_e("failed to register at location service");
            }
        }
    }
    return rc;
}

#ifdef DEBUG
static char *server_state_to_str(forwarder_state_t state)
{
    switch (state) {
        case FWD_STATE_INIT:
            return "FWD_STATE_INIT";
        case FWD_STATE_WAIT_LOCAL:
            return "FWD_STATE_WAIT_LOCAL";
        case FWD_STATE_STARTING:
            return "FWD_STATE_STARTING";
        case FWD_STATE_ENABLED:
            return "FWD_STATE_ENABLED";
        case FWD_STATE_DISABLED:
            return "FWD_STATE_DISABLED";
        default:
            return "unknown state";
    }
}
#endif

/**
 * \pre This should be called with the factory lock taken.
 *
 * param[in] num_local Number of local forwarders discovered, -1 if not counted
 * param[in] timeout   True if a timeout occurred
 */
static qeo_retcode_t fwd_server_state_machine_eval_ul(qeo_factory_t *factory,
                                                      int num_local,
                                                      bool timeout,
                                                      bool public_ip_available)
{
    qeo_retcode_t rc = QEO_OK;

    qeo_log_i("old state %s : local fwd count = %d, timeout = %s, public ip available = %s",
              server_state_to_str(factory->fwd.u.server.state), num_local, bool2str(timeout), bool2str(public_ip_available));
    switch (factory->fwd.u.server.state) {
        case FWD_STATE_INIT: {
            /* done with initialization */
            int timeout = qeocore_parameter_get_number("FWD_WAIT_LOCAL_FWD");

            rc = ddsrc_to_qeorc(DDS_Timer_start(factory->fwd.timer, timeout, (uintptr_t)factory,
                                                fwd_server_timeout));
            if (QEO_OK != rc) {
                qeo_log_e("failed to start forwarder timer");
                break;
            }
            rc = qeocore_reader_enable(factory->fwd.reader);
            if (QEO_OK != rc) {
                qeo_log_e("failed to enable forwarder reader");
                break;
            }
            factory->fwd.u.server.state = FWD_STATE_WAIT_LOCAL;
            break;
        }
        case FWD_STATE_WAIT_LOCAL:
            /* done waiting for local forwarder */
            if (timeout) {
                /* no local forwarders found, try start forwarding ourselves */
                factory->fwd.u.server.state = FWD_STATE_STARTING;
                unlock(&factory->mutex);
                factory->listener.on_fwdfactory_get_public_locator(factory);
                lock(&factory->mutex);
            }
            else if (num_local > 0) {
                /* local forwarder(s) available stop timer */
                DDS_Timer_stop(factory->fwd.timer);
                factory->fwd.u.server.state = FWD_STATE_DISABLED;
            }
            break;
        case FWD_STATE_STARTING:
            /* done waiting for publicly available locator */
            if (num_local > 0) {
                factory->fwd.u.server.state = FWD_STATE_DISABLED;
            }
            else if (public_ip_available) {
                if (QEO_OK == fwd_server_start_forwarding(factory)) {
                    factory->fwd.u.server.state = FWD_STATE_ENABLED;
                }
                else {
                    /* failed to start */
                    factory->fwd.u.server.state = FWD_STATE_DISABLED;
                }
            }
            break;
        case FWD_STATE_ENABLED:
            if (public_ip_available) {
                if (QEO_OK == fwd_server_start_forwarding(factory)) {
                    factory->fwd.u.server.state = FWD_STATE_ENABLED;
                }
                else {
                    /* failed to start */
                    factory->fwd.u.server.state = FWD_STATE_DISABLED;
                }
            }
            break;
        case FWD_STATE_DISABLED:
            if (0 == num_local) {
                /* no local forwarders anymore, try start forwarding ourselves */
                factory->fwd.u.server.state = FWD_STATE_STARTING;
                unlock(&factory->mutex);
                factory->listener.on_fwdfactory_get_public_locator(factory);
                lock(&factory->mutex);
            }
            break;
    }
    qeo_log_i("new state %s", server_state_to_str(factory->fwd.u.server.state));
    return rc;
}

static void fwd_server_state_machine_eval(qeo_factory_t *factory,
                                          int num_local,
                                          bool timeout)
{
    lock(&factory->mutex);
    fwd_server_state_machine_eval_ul(factory, num_local, timeout, false);
    unlock(&factory->mutex);
}

/* ===[ Forwarder topic handling ]========================================== */

static qeo_retcode_t fwd_server_instance_remove(const qeo_factory_t *factory)
{
    qeo_retcode_t rc = QEO_EFAIL;
    org_qeo_system_Forwarder_t fwd_data = {};

    fwd_data.deviceId = factory->qeo_id.device_id;
    rc = qeocore_writer_remove(factory->fwd.u.server.writer, &fwd_data);
    return rc;
}

static qeo_retcode_t fwd_server_instance_publish(const qeo_factory_t *factory)
{
    qeo_retcode_t rc = QEO_OK;
    org_qeo_system_Forwarder_t fwd_data = {
        .locator =  DDS_SEQ_INITIALIZER(org_qeo_system_ForwarderLocator_t)
    };

    fwd_data.deviceId = factory->qeo_id.device_id;
    fwd_data.forwarder = factory->flags.forwarding_enabled;
    fwd_data.bgns = factory->flags.bgns_enabled;
    /* add locator if available */
    if (QMGMT_LOCATORTYPE_UNKNOWN != factory->fwd.locator->type) {
        org_qeo_system_ForwarderLocator_t locator_data;

        locator_data.type = factory->fwd.locator->type;
        locator_data.port = factory->fwd.locator->port;
        locator_data.address = factory->fwd.locator->address;
        rc = ddsrc_to_qeorc(dds_seq_append(&fwd_data.locator, &locator_data));
    }
    if (QEO_OK == rc) {
        rc = qeocore_writer_write(factory->fwd.u.server.writer, &fwd_data);
    }
    dds_seq_cleanup(&fwd_data.locator);
    return rc;
}

static int fwd_server_count_instances(const qeocore_reader_t *reader)
{
    qeo_retcode_t rc = QEO_OK;
    qeocore_filter_t filter = { 0 };
    qeocore_data_t *data;
    int cnt = 0;

    data = qeocore_reader_data_new(reader);
    if (NULL != data) {
        filter.instance_handle = DDS_HANDLE_NIL;
        while (1) {
            rc = qeocore_reader_read(reader, &filter, data);
            if (QEO_OK == rc) {
                filter.instance_handle = qeocore_data_get_instance_handle(data);
                cnt++;
#ifdef DEBUG
                {
                    const org_qeo_system_Forwarder_t *fwd = qeocore_data_get_data(data);

                    qeo_log_d("forwarder %d : id=%" PRIx64 " -> %s", cnt, fwd->deviceId,
                              (fwd->deviceId == reader->entity.factory->qeo_id.device_id ? "self" : "other"));
                }
#endif
                qeocore_data_reset(data);
                continue;
            }
            else if (QEO_ENODATA == rc) {
                rc = QEO_OK;
            }
            /* QEO_ENODATA or error */
            break;
        }
        qeocore_data_free(data);
    }
    if (QEO_OK == rc) {
        qeo_log_i("found %d local forwarders", cnt);
    }
    else {
        qeo_log_e("failed to read all local forwarders");
    }
    return cnt;
}

static void fwd_server_on_data(const qeocore_reader_t *reader,
                                const qeocore_data_t *data,
                                uintptr_t userdata)
{
    /* count number of local forwarder instances */
    if (QEOCORE_NOTIFY == qeocore_data_get_status(data)) {
        qeo_factory_t *factory = (qeo_factory_t *)reader->entity.factory;
        int cnt = fwd_server_count_instances(reader);
        fwd_server_state_machine_eval(factory, cnt, false);
    }
}

static void fwd_server_timeout(uintptr_t userdata)
{
    fwd_server_state_machine_eval((qeo_factory_t *)userdata, -1, true);
}

static void fwd_client_on_data(const qeocore_reader_t *reader,
                               const qeocore_data_t *data,
                               uintptr_t userdata)
{
    qeo_factory_t *factory = (qeo_factory_t *)userdata;
    qeocore_data_status_t status;
    org_qeo_system_Forwarder_t *fwd_data;
    org_qeo_system_ForwarderLocator_t fwd_locator;
    qeo_mgmt_client_locator_t locator;

    status = qeocore_data_get_status(data);
    if (QEOCORE_DATA == status) {
        fwd_data = (org_qeo_system_Forwarder_t *)qeocore_data_get_data(data);
        /* check locator */
        if ((NULL != fwd_data) && (DDS_SEQ_LENGTH(fwd_data->locator) > 0)) {
            flags_t flags = { .forwarding_enabled = fwd_data->forwarder, .bgns_enabled = fwd_data->bgns };
            fwd_locator = DDS_SEQ_ITEM(fwd_data->locator, 0);
            locator.type = fwd_locator.type;
            locator.port = fwd_locator.port;
            locator.address = fwd_locator.address;
            client_state_machine_eval(factory, CLIENT_EVENT_FWD_DATA_RECEIVED, &locator, fwd_data->deviceId, flags);
        }
    }
    else if (QEOCORE_REMOVE == status) {
        fwd_data = (org_qeo_system_Forwarder_t *)qeocore_data_get_data(data);
        if (NULL != fwd_data) {
            flags_t flags = { .forwarding_enabled = fwd_data->forwarder, .bgns_enabled = fwd_data->bgns };
            client_state_machine_eval(factory, CLIENT_EVENT_FWD_DATA_REMOVED, NULL, fwd_data->deviceId, flags);
        }
    }
}

static void on_reg_req_update(const qeocore_reader_t *reader,
                              const qeocore_data_t   *data,
                              uintptr_t              userdata)
{
    org_qeo_system_RegistrationRequest_t *reg_req = NULL;
    qeocore_data_status_t status = qeocore_data_get_status(data);

    if (status == QEOCORE_REMOVE) {
        qeo_log_i("Device request removed\r\n");
        reg_req = (org_qeo_system_RegistrationRequest_t *)qeocore_data_get_data(data);
        assert(reg_req != NULL);

        if (qeocore_writer_remove(_fwd_reg_req_writer, reg_req) != QEO_OK) {
            qeo_log_e("Removing of registration request failed\r\n");
        }
    } else if (status == QEOCORE_DATA) {
        qeo_log_i("Device request received\r\n");
        reg_req = (org_qeo_system_RegistrationRequest_t *)qeocore_data_get_data(data);
        assert(reg_req != NULL);

        if (qeocore_writer_write(_fwd_reg_req_writer, reg_req) != QEO_OK) {
            qeo_log_e("Writing of registration request failed\r\n");
        }
    }
}

static void on_reg_cred_update(const qeocore_reader_t *reader,
                               const qeocore_data_t   *data,
                               uintptr_t              userdata)
{
    org_qeo_system_RegistrationCredentials_t *reg_cred = NULL;
    qeocore_data_status_t status = qeocore_data_get_status(data);

    if (status == QEOCORE_REMOVE) {
        qeo_log_i("Device credentials removed\r\n");
        reg_cred = (org_qeo_system_RegistrationCredentials_t *)qeocore_data_get_data(data);
        assert(reg_cred != NULL);

       if (qeocore_writer_remove(_fwd_reg_cred_writer_open, reg_cred) != QEO_OK) {
            qeo_log_e("Removing of registration credentials failed\r\n");
        }
    } else if (status == QEOCORE_DATA) {
        qeo_log_i("Device credentials received\r\n");
        reg_cred = (org_qeo_system_RegistrationCredentials_t *)qeocore_data_get_data(data);
        assert(reg_cred != NULL);

        if (qeocore_writer_write(_fwd_reg_cred_writer_open, reg_cred) != QEO_OK) {
            qeo_log_e("Writing of registration credentials failed\r\n");
        }
    }
}

static void registration_proxy_teardown(const qeo_factory_t *closed_domain_factory)
{
    if (closed_domain_factory->flags.is_server && closed_domain_factory->flags.bgns_enabled) {
        if (_fwd_reg_req_reader_open != NULL) {
            core_delete_reader(_fwd_reg_req_reader_open, true);
            _fwd_reg_req_reader_open = NULL;
        }
        if (_fwd_reg_cred_writer_open != NULL) {
            core_delete_writer(_fwd_reg_cred_writer_open, true);
            _fwd_reg_cred_writer_open = NULL;
        }
        if (_fwd_reg_cred_reader != NULL) {
            core_delete_reader(_fwd_reg_cred_reader, true);
            _fwd_reg_cred_reader = NULL;
        }
        if (_fwd_reg_req_writer != NULL) {
            core_delete_writer(_fwd_reg_req_writer, true);
            _fwd_reg_req_writer = NULL;
        }

        qeocore_type_free(_fwd_reg_cred_type_open);
        _fwd_reg_cred_type_open = NULL;
        qeocore_type_free(_fwd_reg_req_type_open);
        _fwd_reg_req_type_open = NULL;
        qeocore_type_free(_fwd_reg_cred_type);
        _fwd_reg_cred_type = NULL;
        qeocore_type_free(_fwd_reg_req_type);
        _fwd_reg_req_type = NULL;
    }
}

static qeo_retcode_t create_registration_type(qeo_factory_t* factory, qeocore_type_t **reg_req_type, qeocore_type_t **reg_cred_type)
{
    qeo_retcode_t rc = QEO_EFAIL;

    do {
        *reg_req_type = qeocore_type_register_tsm(factory, org_qeo_system_RegistrationRequest_type,
                                                  org_qeo_system_RegistrationRequest_type->name);
        if (*reg_req_type == NULL) {
            qeo_log_e("Could not register registration request type");
            break;
        }

        *reg_cred_type = qeocore_type_register_tsm(factory, org_qeo_system_RegistrationCredentials_type,
                                                   org_qeo_system_RegistrationCredentials_type->name);
        if (*reg_cred_type == NULL) {
            qeo_log_e("Could not register registration credentials type");
            break;
        }
        rc = QEO_OK;
    } while (0);

    return rc;
}

static qeo_retcode_t create_registration_reader_writer(qeo_factory_t* factory, qeocore_type_t *reader_type, qeocore_type_t *writer_type,
                                                       qeocore_reader_t **reader, qeocore_writer_t **writer, qeocore_reader_listener_t* listener)
{
    qeo_retcode_t rc = QEO_EFAIL;

    do {
        *reader = core_create_reader(factory, reader_type, NULL,
                                     QEOCORE_EFLAG_STATE_DATA | QEOCORE_EFLAG_ENABLE, listener, NULL);
        if (*reader == NULL) {
            qeo_log_e("Could not open reader");
            break;
        }

        *writer = core_create_writer(factory, writer_type, NULL,
                                     QEOCORE_EFLAG_STATE_DATA | QEOCORE_EFLAG_ENABLE, NULL, NULL);
        if (*writer == NULL) {
            qeo_log_e("Could not open writer");
            break;
        }

        rc = QEO_OK;
    } while (0);

    return rc;
}

static bool registration_proxy_setup(qeo_factory_t* closed_domain_factory)
{
    qeo_factory_t *open_domain_factory = NULL;
    bool          retval = false;

    do {
        /* Registration proxy needed? */
        if (!closed_domain_factory->flags.is_server || !closed_domain_factory->flags.bgns_enabled) {
            retval = true;
            break;
        }
        /* Registration proxy already set up? */
        if (_fwd_reg_req_type_open != NULL) {
            retval = true;
            break;
        }
        /* Do set up */
        open_domain_factory = core_get_open_domain_factory();
        if (open_domain_factory == NULL) {
            qeo_log_e("Could not get open domain factory");
            break;
        }

        if (create_registration_type(open_domain_factory, &_fwd_reg_req_type_open, &_fwd_reg_cred_type_open) != QEO_OK) {
            qeo_log_e("Type registration failed on open domain");
            break;
        }

        if (create_registration_type(closed_domain_factory, &_fwd_reg_req_type, &_fwd_reg_cred_type) != QEO_OK) {
            qeo_log_e("Type registration failed on closed domain");
            break;
        }

        if (create_registration_reader_writer(open_domain_factory, _fwd_reg_req_type_open, _fwd_reg_cred_type_open,
                                              &_fwd_reg_req_reader_open, &_fwd_reg_cred_writer_open, &_listener_open)) {
            qeo_log_e("Reader/writer creation failed on open domain");
            break;
        }

        if (create_registration_reader_writer(closed_domain_factory, _fwd_reg_cred_type, _fwd_reg_req_type,
                                              &_fwd_reg_cred_reader, &_fwd_reg_req_writer, &_listener)) {
            qeo_log_e("Reader/writer creation failed on closed domain");
            break;
        }

        retval = true;
    } while (0);

    if (retval == false) {
        registration_proxy_teardown(closed_domain_factory);
    }
    return retval;
}

/* ===[ Forwarder factory 'public' API ]==================================== */

qeo_retcode_t fwd_init_pre_auth(qeo_factory_t *factory)
{
    qeo_retcode_t rc = QEO_OK;
    char buf[16];

    do {
        pthread_cond_init(&factory->fwd.wait_rqst_finished, NULL);
        factory->fwd.timer = DDS_Timer_create("qeofwd");
        if (NULL == factory->fwd.timer) {
            rc = QEO_ENOMEM;
            qeo_log_e("failed to create forwarder timer");
            break;
        }
        if (factory->flags.is_server) {
            /* enable forwarder logic */
            if (factory->flags.forwarding_enabled) {
                qeo_log_i("Forwarding service enabled");

                snprintf(buf, sizeof(buf), "%d", core_get_domain_id_closed());
                rc = ddsrc_to_qeorc(DDS_parameter_set("FWD_DOMAINS", buf));
                if (QEO_OK != rc) {
                    qeo_log_e("failed to set FWD_DOMAINS");
                }

                rc = ddsrc_to_qeorc(DDS_parameter_set("FORWARD", "15"));
                if (QEO_OK != rc) {
                    qeo_log_e("failed to enable FORWARD");
                    break;
                }
                rc = qeocore_factory_set_local_tcp_port(factory);
                if (QEO_OK != rc) {
                    qeo_log_e("Factory set local TCP port failed");
                    break;
                }
            }
            /* enable bgns logic */
            if (factory->flags.bgns_enabled) {
                qeo_log_i("Notification service enabled");
                rc = qeocore_factory_set_bgns(factory, NULL, (factory->flags.forwarding_enabled ? "@" : factory->local_port));
                if (QEO_OK != rc) {
                    break;
                }
                DDS_Activities_client_info(notify_cb);
            }
        }
    } while (0);
    return rc;
}

qeo_retcode_t fwd_init_post_auth(qeo_factory_t *factory)
{
    qeo_retcode_t rc = QEO_EFAIL;
    qeocore_type_t *type = NULL;

    /* this will eventually also take the factory lock, so postponing our lock */
    type = qeocore_type_register_tsm(factory, org_qeo_system_Forwarder_type, org_qeo_system_Forwarder_type->name);
    registration_proxy_setup(factory);
    if (NULL != type) {
        lock(&factory->mutex);
        if (factory->flags.is_server) {
            factory->fwd.listener.on_data = fwd_server_on_data;
            factory->fwd.reader = qeocore_reader_open(factory, type, org_qeo_system_Forwarder_type->name,
                                                      QEOCORE_EFLAG_STATE_UPDATE, &factory->fwd.listener, NULL);
            if (NULL != factory->fwd.reader) {
                factory->fwd.u.server.writer = qeocore_writer_open(factory, type, org_qeo_system_Forwarder_type->name,
                                                                   QEOCORE_EFLAG_STATE_DATA | QEOCORE_EFLAG_ENABLE,
                                                                   NULL, NULL);
                if (NULL != factory->fwd.u.server.writer) {
                    factory->fwd.u.server.state = FWD_STATE_INIT;
                    rc = fwd_server_state_machine_eval_ul(factory, -1, false, false);
                }
            }
        } else {
            flags_t flags = { 0 };
            factory->fwd.listener.on_data = fwd_client_on_data;
            factory->fwd.listener.userdata = (uintptr_t)factory;
            /* note: reader construction failure is not fatal but will limit features */
            factory->fwd.reader = qeocore_reader_open(factory, type, org_qeo_system_Forwarder_type->name,
                                                      QEOCORE_EFLAG_STATE_DATA, &factory->fwd.listener, NULL);
            if (NULL == factory->fwd.reader) {
                qeo_log_e("failed to create Forwarder topic reader");
            }
            factory->fwd.u.client.state = FWD_CLIENT_STATE_INIT;
            client_state_machine_eval_ul(factory, CLIENT_EVENT_START, NULL, -1, flags);
            rc = QEO_OK;
        }
        unlock(&factory->mutex);
        qeocore_type_free(type);
    }
    return rc;
}

qeo_retcode_t fwd_server_reconfig(qeo_factory_t *factory,
                                  const char *ip_address,
                                  int port)
{
    qeo_retcode_t rc = QEO_OK;

    VALIDATE_NON_NULL(factory);
    if (!factory->flags.is_server) {
        return QEO_EINVAL;
    }
    VALIDATE_NON_NULL(ip_address);
    if (!strcmp("", ip_address)) {
        return QEO_EINVAL;
    }
    lock(&factory->mutex);
    if (QEO_OK == fwd_locator_create(factory)) {
        qeo_mgmt_client_locator_type_t locatorType = QMGMT_LOCATORTYPE_TCPV4;
        if (strcmp("0.0.0.0", ip_address) == 0) {
            locatorType = QMGMT_LOCATORTYPE_UNKNOWN;
        }
        qeo_mgmt_client_locator_t locator = {locatorType, (char *)ip_address, port};

        if (QEO_OK == fwd_locator_update(factory, &locator)) {
            fwd_server_state_machine_eval_ul(factory, -1, false, true);
        }
    }
    unlock(&factory->mutex);
    return rc;
}

/* ===[ Client helper functions ]=========================================== */

static char *client_state_to_str(client_state_t state)
{
    switch (state) {
        case FWD_CLIENT_STATE_INIT:
            return "FWD_CLIENT_STATE_INIT";
        case FWD_CLIENT_STATE_WAIT:
            return "FWD_CLIENT_STATE_WAIT";
        case FWD_CLIENT_STATE_WAIT_READER:
            return "FWD_CLIENT_STATE_WAIT_READER";
        case FWD_CLIENT_STATE_READY:
            return "FWD_CLIENT_STATE_READY";
        default:
            return "unknown state";
    }
}

static char *client_event_to_str(client_state_events_t event)
{
    switch (event) {
        case CLIENT_EVENT_START:
            return "CLIENT_EVENT_START";
        case CLIENT_EVENT_TIMEOUT:
            return "CLIENT_EVENT_TIMEOUT";
        case CLIENT_EVENT_LOC_SRV_DATA_RECEIVED:
            return "CLIENT_EVENT_LOC_SRV_DATA_RECEIVED";
        case CLIENT_EVENT_FWD_DATA_RECEIVED:
            return "CLIENT_EVENT_FWD_DATA_RECEIVED";
        case CLIENT_EVENT_FWD_DATA_REMOVED:
            return "CLIENT_EVENT_FWD_DATA_REMOVED";
        default:
            return "unknown event";
    }
}

static void fwd_client_reconfig(qeo_factory_t *factory,
                                qeo_mgmt_client_locator_t *locator,
                                int64_t device_id,
                                flags_t flags)
{
    qeo_retcode_t rc = QEO_OK;
    qeo_log_d("Reconfiguring client");

    /* Configure the locator in the factory */
    if (QEO_OK == fwd_locator_create(factory)) {
        if (QEO_OK == fwd_locator_update(factory, locator)) {
            char tcp_server[128];
            bool fwd = (factory->flags.forwarding_enabled && flags.forwarding_enabled);
            bool bgns = factory->flags.bgns_enabled && flags.bgns_enabled;
            char *bgns_server = NULL;

            snprintf(tcp_server, sizeof(tcp_server), "%s:%d", locator->address, locator->port);
            if (fwd) {
                qeo_log_i("use forwarder '%s'", tcp_server);
                if ((rc = core_factory_set_tcp_server_no_lock(factory, tcp_server)) != QEO_OK) {
                    qeo_log_e("fwd: set tcp server failed (rc=%d)", rc);
                }
                if (bgns) {
                    bgns_server = "@"; /* use same as forwarder */
                }
            }
            else if (bgns) {
                bgns_server = tcp_server;
            }
            else {
                snprintf(tcp_server, sizeof(tcp_server), "%d.%d.%d.%d",
                         100 + factory->flags.forwarding_enabled, 100 + flags.forwarding_enabled,
                         100 + factory->flags.bgns_enabled, 100 + flags.bgns_enabled);
                bgns_server = tcp_server;
            }
            if (NULL != bgns_server) {
                qeo_log_i("use notification service '%s'", bgns_server);
                if ((rc = qeocore_factory_set_bgns(factory, bgns_server, NULL)) != QEO_OK) {
                    qeo_log_e("bgns: set tcp server failed (rc=%d)", rc);
                }
            }
        }
        else {
            qeo_log_e("fwd locator update failed");
        }
    }
    else {
        qeo_log_e("Cannot create fwd locator");
    }
    /* Configure the device ID in the factory */
    factory->fwd.device_id = device_id;
}

static qeo_retcode_t client_start_timer(qeo_factory_t *factory, bool reset)
{
    qeo_retcode_t rc = QEO_OK;

    if (reset) {
        factory->fwd.timeout = qeocore_parameter_get_number("FWD_LOC_SRV_MIN_TIMEOUT");
    }
    qeo_log_i("retry contacting location service after %ds", factory->fwd.timeout/1000);
    rc = ddsrc_to_qeorc(DDS_Timer_start(factory->fwd.timer, factory->fwd.timeout, (uintptr_t)factory,
                                        fwd_client_discovery_timeout));
    return rc;
}

/**
 * When the location of the forwarder is not known or lost, call this function
 * to trigger rediscovery.  The time-out timer will be reset and a request will
 * be sent to the location service.
 *
 * \post State machine state will be ::FWD_CLIENT_STATE_WAIT.
 */
static void fwd_client_contact_location_service(qeo_factory_t *factory,
                                                int reset)
{
    if (!qeocore_parameter_get_number("FWD_DISABLE_LOCATION_SERVICE")) {
        /* start request time-out timer */
        if (QEO_OK != client_start_timer(factory, reset)) {
            qeo_log_e("error starting timer");
        }
        /* request forwarder list from SMS */
        if (QEO_OK != fwd_get_list(factory)) {
            qeo_log_e("error requesting list of forwarders");
        }
    }
    factory->fwd.u.client.state = FWD_CLIENT_STATE_WAIT;
}

static void client_state_machine_eval_ul(qeo_factory_t *factory,
                                         client_state_events_t event,
                                         qeo_mgmt_client_locator_t *locator,
                                         int64_t device_id,
                                         flags_t flags)
{
    qeo_log_i("received event %s in state %s", client_event_to_str(event),
              client_state_to_str(factory->fwd.u.client.state));
    switch (factory->fwd.u.client.state) {
        case FWD_CLIENT_STATE_INIT:
            switch (event) {
                case CLIENT_EVENT_START: {
                    char tcp_server[128];
                    bool dds_fwd = (NULL != DDS_parameter_get("TCP_SERVER", tcp_server, sizeof(tcp_server)));
                    bool fwd = factory->flags.forwarding_enabled || dds_fwd;
                    bool bgns = factory->flags.bgns_enabled;

                    /* state machine goes to final state if both forwarding and
                     * notifications are disabled */
                    if (!fwd && !bgns) {
                        factory->fwd.u.client.state = FWD_CLIENT_STATE_READY;
                        break;
                    }
                    if (QEO_OK != qeocore_reader_enable(factory->fwd.reader)) {
                        qeo_log_e("error enabling forwarder topic reader");
                    }
                    /* if we want forwarding and it is not pre-configured */
                    if (fwd && !dds_fwd) {
                        fwd_client_contact_location_service(factory, 1);
                    }
                    else {
                        /* wait for arrival of forwarder topic instance */
                        factory->fwd.u.client.state = FWD_CLIENT_STATE_WAIT;
                    }
                    break;
                }
                default:
                    qeo_log_e("unexpected event %s in state %s", client_event_to_str(event),
                              client_state_to_str(factory->fwd.u.client.state));
                    break;
            }
            break;
        case FWD_CLIENT_STATE_WAIT:
            switch (event) {
                case CLIENT_EVENT_TIMEOUT:
                    fwd_client_contact_location_service(factory, 0);
                    break;
                case CLIENT_EVENT_LOC_SRV_DATA_RECEIVED:
                    fwd_client_reconfig(factory, locator, device_id, flags);
                    factory->fwd.u.client.state = FWD_CLIENT_STATE_WAIT_READER;
                    break;
                case CLIENT_EVENT_FWD_DATA_RECEIVED:
                    fwd_client_reconfig(factory, locator, device_id, flags);
                    DDS_Timer_stop(factory->fwd.timer);
                    factory->fwd.u.client.state = FWD_CLIENT_STATE_READY;
                    break;
                default:
                    qeo_log_e("unexpected event %s in state %s", client_event_to_str(event),
                              client_state_to_str(factory->fwd.u.client.state));
                    break;
            }
            break;
        case FWD_CLIENT_STATE_WAIT_READER:
            switch (event) {
                case CLIENT_EVENT_TIMEOUT:
                    fwd_client_contact_location_service(factory, 0);
                    break;
                case CLIENT_EVENT_FWD_DATA_RECEIVED:
                    DDS_Timer_stop(factory->fwd.timer);
                    factory->fwd.u.client.state = FWD_CLIENT_STATE_READY;
                    if ((factory->fwd.device_id != device_id) || flags.bgns_enabled) {
                        fwd_client_reconfig(factory, locator, device_id, flags);
                    }
                    break;
                default:
                    qeo_log_e("unexpected event %s in state %s", client_event_to_str(event),
                              client_state_to_str(factory->fwd.u.client.state));
                    break;
            }
            break;
        case FWD_CLIENT_STATE_READY:
            switch (event) {
                case CLIENT_EVENT_FWD_DATA_REMOVED:
                    fwd_client_contact_location_service(factory, 1);
                    break;
                default:
                    qeo_log_e("unexpected event %s in state %s", client_event_to_str(event),
                              client_state_to_str(factory->fwd.u.client.state));
                    break;
            }
            break;
    }
    qeo_log_i("new state %s", client_state_to_str(factory->fwd.u.client.state));
}

static void client_state_machine_eval(qeo_factory_t *factory,
                                      client_state_events_t event,
                                      qeo_mgmt_client_locator_t *locator,
                                      int64_t device_id,
                                      flags_t flags)
{
    lock(&factory->mutex);
    client_state_machine_eval_ul(factory, event, locator, device_id, flags);
    unlock(&factory->mutex);
}

static void fwd_client_discovery_timeout(uintptr_t userdata)
{
    qeo_factory_t *factory = (qeo_factory_t *)userdata;
    flags_t flags = { 0 };

    client_state_machine_eval(factory, CLIENT_EVENT_TIMEOUT, NULL, -1, flags);
}
