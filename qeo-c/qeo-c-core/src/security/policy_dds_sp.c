/*
 * Copyright (c) 2014 - Qeo LLC
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

/*#######################################################################
#HEADER (INCLUDE) SECTION #
########################################################################*/
#ifndef DEBUG
#define NDEBUG
#endif
#ifdef QEO_USE_DDS_NATIVE_SECURITY
#include <nsecplug/nsecplug.h>
#include <openssl/ssl.h>
#else
#include <msecplug/msecplug.h>
#endif
#include <assert.h>
#include <stdlib.h>
#include <inttypes.h>
#include <string.h>
#include <stdio.h>
#include <ctype.h>
#include <qeo/log.h>
#include <qeo/mgmt_cert_parser.h>
#include <dds/dds_aux.h>
#include "policy_dds_sp.h"
#include "security.h"
#include "core_util.h"
/*#######################################################################
# TYPES/DEFINES SECTION                                                 #
########################################################################*/
#define VALID_CERT_CHAIN_LENGTH (3)
#define DISABLED_READER "DISABLED_READER"
#define DISABLED_WRITER "DISABLED_WRITER"
/*#######################################################################
# STATIC FUNCTION DECLARATION #
########################################################################*/
static MSMode_t convert_permissions(const policy_dds_sp_perms_t *perms);
static  DDS_ReturnCode_t DDS_SP_authentication_check_cb (void *context, const char *name);
/*#######################################################################
# STATIC VARIABLE SECTION #
########################################################################*/
static unsigned int _domain;
static unsigned int _domain_id;
static bool _initialized;
static unsigned int _participants_added;
static unsigned int _partitionstrings_added;
/*#######################################################################
# STATIC FUNCTION IMPLEMENTATION #
########################################################################*/
static MSMode_t convert_permissions(const policy_dds_sp_perms_t *perms){

    MSMode_t mode = TA_CREATE | TA_DELETE;
    if (perms->read == true){
        mode |= TA_READ;
    }

    if (perms->write == true){
        mode |= TA_WRITE;
    }

    return mode;
}

static  DDS_ReturnCode_t DDS_SP_authentication_check_cb (void *context, const char *name)
{
    int i;
    SSL *ssl = (SSL*) context;
    int nb = sk_num ((const _STACK *) SSL_CTX_get_cert_store (SSL_get_SSL_CTX (ssl))->objs);
    int peerNb = sk_num ((const _STACK *) SSL_get_peer_cert_chain (ssl));
    X509 *myCert = SSL_get_certificate (ssl);
    X509 *peerCert = SSL_get_peer_certificate (ssl);
    X509 *myRealmCert = NULL;
    X509 *peerRealmCert = NULL;
    X509 *tmp = NULL;
    X509_STORE *myRealmCerts = SSL_CTX_get_cert_store (SSL_get_SSL_CTX (ssl));
    STACK_OF(X509) *peerRealmCerts = SSL_get_peer_cert_chain (ssl);
    qeo_mgmt_cert_contents peerCertContents = {};
    char *expectedPeerParticipantName = NULL;
    DDS_ReturnCode_t ret = DDS_RETCODE_ACCESS_DENIED;
    bool match = false;
    STACK_OF(X509) * sktmp = NULL;

    do {

        if (peerNb != VALID_CERT_CHAIN_LENGTH) {
            qeo_log_e("invalid certificate chain length: %d, expected: %d", peerNb, VALID_CERT_CHAIN_LENGTH);
        }
         //Verify whether we are in the same realm


        for (i = 0; i < peerNb; i++) {
            tmp =  sk_X509_value (peerRealmCerts, i);
            if (X509_name_cmp (X509_get_issuer_name (peerCert),
                        X509_get_subject_name (tmp)) == 0) {
                peerRealmCert = tmp;
                break;
            }
        }

        if (peerRealmCert == NULL) {
            qeo_log_e("peerRealmCert == NULL");
            break;
        }

        for (i = 0; i < nb; i++) {
            tmp =  (X509 *) ((X509_OBJECT *) sk_X509_OBJECT_value (myRealmCerts->objs
                        , i))->data.x509;
            if (X509_name_cmp (X509_get_issuer_name (myCert), 
                        X509_get_subject_name (tmp)) == 0) {
                myRealmCert = tmp;
                if (myRealmCert == NULL) {
                    qeo_log_e("myRealmCert == NULL");
                    break;
                }
                if (!(X509_cmp (myRealmCert, peerRealmCert))) {
                    match = true;
                }
                break;
            }
        }

        if ( match != true){
            qeo_log_e("Verification failed: not the same qeo realm certificate.");
            break;
        }


        //Verify whether the peer participant name is valid
        /* We use a temporary STACK so we can chop and hack at it */
        qeo_log_i("verify participant name: %s", name);

        sktmp = sk_X509_dup(peerRealmCerts);
        if (sktmp == NULL ) {
            qeo_log_e("sktmp == NULL");
            break;
        }


        if((qeo_mgmt_cert_parse(sktmp,&peerCertContents) !=  QCERT_OK)) {
            qeo_log_e("failed to get ids from certificate chain");
            break;
        }

        if (asprintf(&expectedPeerParticipantName,FRIENDLY_NAME_FORMAT,peerCertContents.realm, peerCertContents.device, peerCertContents.user) == -1) {
             qeo_log_e("asprintf failed");
            break;

        }

        if(strcmp(expectedPeerParticipantName,name) != 0) {
            qeo_log_e("Verification failed: Invalid peer participant name: %s, expected: %s", name, expectedPeerParticipantName);
            break;
        }
        
        ret = DDS_RETCODE_OK;

    }while(0);

    if (peerCert != NULL) {
        X509_free(peerCert);
    }

    if (sktmp != NULL ) {
        sk_X509_free(sktmp);
    }

    if(expectedPeerParticipantName != NULL) {
        free(expectedPeerParticipantName);
    }

    return ret ;

}
/*#######################################################################
#PUBLIC FUNCTION IMPLEMENTATION                                         #
########################################################################*/
qeo_retcode_t policy_dds_sp_init(void){

    qeo_retcode_t ret = QEO_EFAIL;
    DDS_ReturnCode_t ddsrc;

    if (_initialized == true){
        return QEO_EBADSTATE;
    }

    do 
    {
        /* set up security */
        ddsrc = DDS_SP_set_policy();
        if (DDS_RETCODE_OK != ddsrc) {
            qeo_log_e("DDS_SP_set_policy failed %d", ddsrc_to_qeorc(ddsrc));
            break;
        }

        DDS_SP_set_extra_authentication_check(DDS_SP_authentication_check_cb);    

        _initialized = true;

        ret = QEO_OK;
    }while(0);

    return ret;
}

void policy_dds_sp_destroy(void){
    policy_dds_sp_flush();
    _initialized = false;
}

qeo_retcode_t policy_dds_sp_add_domain(unsigned int domain_id)
{
    // TODO: will we ever support more than 1 domain_id, then don't keep this in static variables !
    // the factory object now has the domain_id as field so ...
    qeo_retcode_t ret = QEO_EFAIL;
    DDS_ReturnCode_t ddsrc;

    if (_initialized == false){
        return QEO_EBADSTATE;
    }

    do
    {
        uint32_t transport;

        if ((_domain = DDS_SP_get_domain_handle (domain_id)) == -1) {
            _domain = DDS_SP_add_domain();
        }

#ifdef DEBUG
        /* TODO: Remove this if statement once dtls/tls are fully working */
        if (NULL != getenv("HACK_NOSECURITY")) {
            transport = TRANS_BOTH_NONE;
#ifdef QEO_USE_DDS_NATIVE_SECURITY
            ddsrc = DDS_SP_set_domain_access(_domain, domain_id, DS_SECRET, 0, 1, 0, transport, 0);
#else
            ddsrc = DDS_SP_set_domain_access(_domain, domain_id, DS_SECRET, 0,  transport, 0);
#endif
        }
        else {
#endif
#ifdef QEO_USE_DDS_NATIVE_SECURITY
            transport = TRANS_BOTH_DDS_SEC | TRANS_BOTH_TLS_TCP;
            ddsrc = DDS_SP_set_domain_access(_domain, domain_id, DS_SECRET, 0, 1, 0, transport, 0);
#else
            transport = TRANS_BOTH_DTLS_UDP | TRANS_BOTH_TLS_TCP;
            ddsrc = DDS_SP_set_domain_access(_domain, domain_id, DS_SECRET, 0,  transport, 0);
#endif
#ifdef DEBUG
        }
#endif

        if ( ddsrc != DDS_RETCODE_OK) {
            qeo_log_e("DDS_SP_set_domain_access failed");
            ret = ddsrc_to_qeorc(ddsrc);
            break;
        }
        _domain_id = domain_id;

        ret = QEO_OK;

    } while(0);

    if (ret != QEO_OK) {
        ddsrc = DDS_SP_remove_domain(_domain);
        if ( ddsrc != DDS_RETCODE_OK) {
            qeo_log_e("DDS_SP_remove_domain failed");
        }
    }

    return ret;
}

void policy_dds_sp_update_start(void)
{
    _partitionstrings_added = 0;
    _participants_added = 0;
    DDS_SP_update_start();
}

void policy_dds_sp_update_done(void)
{
    DDS_SP_update_done();
    qeo_log_i("done (added %u participants, %u partition strings)", _participants_added, _partitionstrings_added);
}

qeo_retcode_t policy_dds_sp_add_participant(uintptr_t *cookie, const char *participant_name){

    DDS_ReturnCode_t ddsrc;
    qeo_retcode_t ret = QEO_EFAIL;
    ParticipantHandle_t participant = 0;
    PartitionHandle_t disabled_reader = 0;
    PartitionHandle_t disabled_writer = 0;

    if (_initialized == false){
        return QEO_EBADSTATE;
    }

    do {
        if ((participant = DDS_SP_get_participant_handle ((char *)participant_name)) == -1)
        {
            participant = DDS_SP_add_participant();
            if (participant == 0){
                qeo_log_e("DDS_SP_add_participant failed");
                ret = QEO_EFAIL;
                break;
            }
        }
        

        *cookie = (uintptr_t)participant; /* store participant handle */
        if ((ddsrc = DDS_SP_set_participant_access(participant, (char *)participant_name, DS_SECRET,0)) != DDS_RETCODE_OK){
            qeo_log_e("Could not set participant access %s", ddsrc_to_qeorc(ddsrc));
            ret = QEO_EFAIL;
            break;
        }

        /* Add default rules:
         * !DISABLED_READER w
         * !DISABLED_WRITER r     
         */

        if ((disabled_reader = DDS_SP_get_partition_handle (participant,_domain,DISABLED_READER, TA_CREATE | TA_DELETE | TA_WRITE)) == -1)
        {
            disabled_reader = DDS_SP_add_partition (participant, _domain);
            if (disabled_reader == 0){
                qeo_log_e("DDS_SP_add_partition failed");
                ret = QEO_EFAIL;
                break;
            }
        }

        if ((ddsrc = DDS_SP_set_partition_access(participant,
				       _domain,
				       disabled_reader,
				       DISABLED_READER,
				       TA_CREATE | TA_DELETE | TA_WRITE,
				       _domain_id,
                       1) != DDS_RETCODE_OK)){
            qeo_log_e("Could not set partition access %s", ddsrc_to_qeorc(ddsrc));
            ret = QEO_EFAIL;
            break;
        }

        if ((disabled_writer = DDS_SP_get_partition_handle (participant,_domain,DISABLED_WRITER, TA_CREATE | TA_DELETE | TA_WRITE)) == -1)
        {
            disabled_writer = DDS_SP_add_partition (participant, _domain);
            if (disabled_writer == 0){
                qeo_log_e("DDS_SP_add_partition failed");
                ret = QEO_EFAIL;
                break;
            }
        }

        if ((ddsrc = DDS_SP_set_partition_access(participant,
				       _domain,
				       disabled_writer,
				       DISABLED_WRITER,
				       TA_CREATE | TA_DELETE | TA_READ,
				       _domain_id,
                       1) != DDS_RETCODE_OK)){
            qeo_log_e("Could not set partition access %s", ddsrc_to_qeorc(ddsrc));
            ret = QEO_EFAIL;
            break;
        }
        ++_participants_added;
        ret = QEO_OK;
    } while (0);

    if (ret != QEO_OK) {
        if (participant != 0) {
            ddsrc = DDS_SP_remove_participant (participant);
            if ( ddsrc != DDS_RETCODE_OK) {
                qeo_log_e("DDS_SP_remove_participant failed");
            }
        }

        if(disabled_reader != 0) {
            ddsrc = DDS_SP_remove_participant (disabled_reader);
            if ( ddsrc != DDS_RETCODE_OK) {
                qeo_log_e("DDS_SP_remove_participant failed");
            }
        }

        if(disabled_writer != 0) {
            ddsrc = DDS_SP_remove_participant (disabled_writer);
            if ( ddsrc != DDS_RETCODE_OK) {
                qeo_log_e("DDS_SP_remove_participant failed");
            }
        }
    }

    return ret;
}

qeo_retcode_t policy_dds_sp_add_partition(uintptr_t cookie, const char *name, policy_dds_sp_perms_t * perms){

    DDS_ReturnCode_t ddsrc;
    qeo_retcode_t ret = QEO_OK;
    int blacklist = 0;
    PartitionHandle_t partition = 0;
    ParticipantHandle_t participant = (ParticipantHandle_t)cookie;

    if (_initialized == false){
        return QEO_EBADSTATE;
    }

    do {
        if (participant == 0) {
            qeo_log_e("Invalid participant (0)");
            break;
        } 

        if ((partition = DDS_SP_get_partition_handle (participant,_domain,(char*) name, convert_permissions(perms))) == -1)
        {
            partition = DDS_SP_add_partition (participant, _domain);
            if (partition == 0){
                qeo_log_e("DDS_SP_add_partition failed");
                ret = QEO_EFAIL;
                break;
            }
        }

        if(perms->blacklist == true) {
            blacklist=1;
        }

        if ((ddsrc = DDS_SP_set_partition_access(participant,
				       _domain,
				       partition,
				       (char *)name,
				       convert_permissions(perms),
				       _domain_id,
                       blacklist) != DDS_RETCODE_OK)){
            qeo_log_e("Could not set partition access %s", ddsrc_to_qeorc(ddsrc));
            ret = QEO_EFAIL;
            break;
        }
        ++_partitionstrings_added;
        ret = QEO_OK;
    } while (0);

    if (ret != QEO_OK) {
        if(partition != 0) {
            ddsrc = DDS_SP_remove_partition (participant, _domain, partition);
            if ( ddsrc != DDS_RETCODE_OK) {
                qeo_log_e("DDS_SP_remove_partition failed");
            }
        }
    }

    return ret;

}

qeo_retcode_t policy_dds_sp_flush(void){

    if (_initialized == false){
        return QEO_EBADSTATE;
    }

    (void) DDS_SP_access_db_cleanup();
    _domain = 0;
    _domain_id = 0;

    return QEO_OK;
}

