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

#include <semaphore.h>
#include <errno.h>
#include <qeo/log.h>
#include <string.h>
#include <stdbool.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/tcp.h>

#include <qeo/factory.h>
#include <qeocore/api.h>
#include "forwarder.h"
#include "core_util.h"
#include "config.h"

static void on_qeocore_on_factory_init_done(qeo_factory_t *factory, bool success);
static void bgns_on_wakeup(const char *topic_name, const char *type_name, unsigned char id [12]);
static void bgns_on_connected(int fd, int connected);

typedef struct {
    pthread_cond_t cond;
    pthread_mutex_t mutex;
    bool finished;

    bool success;
} qeofactory_userdata;

static qeocore_factory_listener_t _listener = {
    .on_factory_init_done = on_qeocore_on_factory_init_done
};

static bool _factory_closed_init = false;
static bool _factory_open_init = false;

static void on_qeocore_on_factory_init_done(qeo_factory_t *factory, bool success){

    uintptr_t puserdata;

    qeocore_factory_get_user_data(factory, &puserdata);
    qeofactory_userdata *userdata = (qeofactory_userdata *)puserdata;

    pthread_mutex_lock(&userdata->mutex);
    userdata->success = success;
    userdata->finished = true;
    pthread_cond_signal(&userdata->cond);
    pthread_mutex_unlock(&userdata->mutex);

}

static qeo_factory_t *factory_create(const qeo_identity_t *id,
                                     qeocore_on_fwdfactory_get_public_locator cb,
                                     const char *local_port,
                                     bool bgns)
{
    qeo_factory_t *factory = NULL;
    qeo_retcode_t ret = QEO_EFAIL;
    qeofactory_userdata userdata = { 
        .cond = (pthread_cond_t)PTHREAD_COND_INITIALIZER,
        .mutex = (pthread_mutex_t)PTHREAD_MUTEX_INITIALIZER,
        .finished = false,
        .success = false
    };
    DDS_Activities_register(bgns_on_wakeup, bgns_on_connected);


    if (!((id == QEO_IDENTITY_DEFAULT && _factory_closed_init)
        || (id == QEO_IDENTITY_OPEN && _factory_open_init))) {
        do {
            if (id == QEO_IDENTITY_DEFAULT) {
                _factory_closed_init = true;
            }
            else if (id == QEO_IDENTITY_OPEN) {
                _factory_open_init = true;
            }

            factory = qeocore_factory_new(id);
            if (factory == NULL) {
                ret = QEO_ENOMEM;
                qeo_log_e("Failed to construct new factory");
                break;
            }

            /* Forwarder specific checks */
            if (NULL != local_port) {
                factory->local_port = strdup(local_port);
            }
            factory->flags.is_server = (NULL != cb ? 1 : 0);
            _listener.on_fwdfactory_get_public_locator = cb;

            if ((ret = qeocore_factory_set_user_data(factory, (uintptr_t)&userdata)) != QEO_OK){
                qeo_log_e("Factory set user data failed");
                break;
            }

            if ((ret = qeocore_factory_init(factory, &_listener)) != QEO_OK ) {
                qeo_log_e("Factory init failed");
                break;
            }

            /* wait until the mutex is unlocked through the callback ! */
            pthread_mutex_lock(&userdata.mutex);
            while (userdata.finished == false){
                pthread_cond_wait(&userdata.cond, &userdata.mutex);
            }
            pthread_mutex_unlock(&userdata.mutex);

            ret = QEO_OK;

        } while (0);

        qeocore_factory_set_user_data(factory, 0);
        pthread_mutex_destroy(&userdata.mutex);
        pthread_cond_destroy(&userdata.cond);

        if (ret != QEO_OK || userdata.success == false){
            qeocore_factory_close(factory);
            factory = NULL;
            if (id == QEO_IDENTITY_DEFAULT) {
                _factory_closed_init = false;
            }
            else if (id == QEO_IDENTITY_OPEN) {
                _factory_open_init = false;
            }
        }
    }
    else {
        qeo_log_e("Factory can only be created once");
    }

    return factory;
}



qeo_factory_t *qeo_factory_create()
{
    return qeo_factory_create_by_id(QEO_IDENTITY_DEFAULT);
}

qeo_factory_t *qeo_factory_create_by_id(const qeo_identity_t *id)
{
    return factory_create(id, NULL, NULL, false);
}


void qeo_factory_close(qeo_factory_t *factory)
{
    if (factory != NULL) {
        if (factory->domain_id == core_get_domain_id_open()) {
            _factory_open_init = false;
        }
        else if (factory->domain_id == core_get_domain_id_closed()) {
            _factory_closed_init = false;
        }
        qeocore_factory_close(factory);
    }
    else {
        qeo_log_e("Trying to close an invalid factory");
    }
}

qeo_factory_t *qeocore_fwdfactory_new(qeocore_on_fwdfactory_get_public_locator cb,
                                      const char *local_port,
                                      bool bgns)
{
    qeo_factory_t *factory = NULL;

    if ((NULL != cb) && (NULL != local_port)) {
        factory = factory_create(QEO_IDENTITY_DEFAULT, cb, local_port, bgns);
    }
    return factory;
}

void qeocore_fwdfactory_close(qeo_factory_t *factory)
{
    qeo_factory_close(factory);
}

qeo_retcode_t qeocore_fwdfactory_set_public_locator(qeo_factory_t *factory,
                                                    const char *ip_address,
                                                    int port)
{
    return fwd_server_reconfig(factory, ip_address, port);
}

const char *qeo_version_string(void)
{
#ifdef QEO_VERSION
    return QEO_VERSION;
#else
    return "x.x.x-UNKNOWN";
#endif
}

/* ===[ background notification service ]==================================== */

static qeo_bgns_listener_t _bgns_listener = {};
static uintptr_t _bgns_userdata = 0;
static bool _bgns_connected = false;

static void bgns_on_wakeup(const char *topic_name,
                           const char *type_name,
                           unsigned char id [12])
{
    qeo_log_d("bgns_on_wakeup: %s - %s", topic_name, type_name);
    if (NULL != _bgns_listener.on_wakeup) {
        _bgns_listener.on_wakeup(_bgns_userdata, type_name);
    }
}

static void bgns_on_connected(int fd,
                              int connected)
{
    int rc;
    qeo_log_d("bgns_on_connected: %i - %i", fd, connected);
    _bgns_connected = (connected == 1 ? true: false);
    if (NULL != _bgns_listener.on_connect) {
        _bgns_listener.on_connect(_bgns_userdata, fd, connected ? true : false);
    }
    if (connected) {
        /* Enable TCP keepalive on the bgns connection */
        int on = 1;
        int keep_cnt = qeocore_parameter_get_number("FWD_BGNS_TCP_KEEPCNT"); /* num probes */
        int keep_idle = qeocore_parameter_get_number("FWD_BGNS_TCP_KEEPIDLE"); /* min connection idle */
        int keep_intvl = qeocore_parameter_get_number("FWD_BGNS_TCP_KEEPINTVL"); /* seconds per probe */
        if (keep_cnt != -1 && keep_idle != -1 && keep_intvl != -1) {
            qeo_log_i("Enable TCP keepalive on BGNS channel (TCP_KEEPCNT=%d, TCP_KEEPIDLE=%d, TCP_KEEPINTVL=%d)", keep_cnt, keep_idle, keep_intvl);
            rc = setsockopt(fd, SOL_SOCKET, SO_KEEPALIVE, &on, sizeof(on));
            if (rc != 0) {
                qeo_log_e("Error setting SO_KEEPALIVE: %d: %s", rc, strerror(errno));
            }
#ifdef __APPLE__
            setsockopt(fd, IPPROTO_TCP, TCP_KEEPALIVE, &keep_idle, sizeof(keep_idle));
            if (rc != 0) {
                qeo_log_e("Error setting TCP_KEEPALIVE: %d: %s", rc, strerror(errno));
            }
#else
            setsockopt(fd, SOL_TCP, TCP_KEEPCNT, &keep_cnt, sizeof(keep_cnt));
            if (rc != 0) {
                qeo_log_e("Error setting TCP_KEEPCNT: %d: %s", rc, strerror(errno));
            }
            setsockopt(fd, SOL_TCP, TCP_KEEPIDLE, &keep_idle, sizeof(keep_idle));
            if (rc != 0) {
                qeo_log_e("Error setting TCP_KEEPIDLE: %d: %s", rc, strerror(errno));
            }
            setsockopt(fd, SOL_TCP, TCP_KEEPINTVL, &keep_intvl, sizeof(keep_intvl));
            if (rc != 0) {
                qeo_log_e("Error setting TCP_KEEPINTVL: %d: %s", rc, strerror(errno));
            }
#endif
        }
        else {
            qeo_log_i("Not enabling TCP keepalive on BGNS channel");
        }
    }
}

void qeo_bgns_register(const qeo_bgns_listener_t *listener,
                       uintptr_t userdata)
{
    if (NULL == listener) {
        memset(&_bgns_listener, 0, sizeof(_bgns_listener));
    }
    else {
        _bgns_listener = *listener;
    }
    _bgns_userdata = userdata;
    DDS_Activities_register(bgns_on_wakeup, bgns_on_connected);
}

#ifndef NDEBUG
#define SUSPEND_ACTIVITIES (DDS_ALL_ACTIVITY & ~DDS_DEBUG_ACTIVITY)
#else
#define SUSPEND_ACTIVITIES DDS_ALL_ACTIVITY
#endif

void qeo_bgns_suspend(void)
{
    qeo_log_i("Suspend Qeo");
    if (qeocore_get_num_factories() == 0) {
        qeo_log_w("No open factories, ignoring suspend call");
        return;
    }
    if (!_bgns_connected) {
        qeo_log_w("Suspend call while not connected to bgns");
    }
    DDS_Activities_suspend(SUSPEND_ACTIVITIES);
}

void qeo_bgns_resume(void)
{
    qeo_log_i("Resume Qeo");
    if (qeocore_get_num_factories() == 0) {
        qeo_log_w("No open factories, ignoring resume call");
        return;
    }
    DDS_Activities_resume(SUSPEND_ACTIVITIES);
}
