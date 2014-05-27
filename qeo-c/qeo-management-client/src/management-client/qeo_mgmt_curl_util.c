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
#                       HEADER (INCLUDE) SECTION                        #
########################################################################*/

#include <string.h>


#include "qeo/log.h"
#include <curl/curl.h>
#include <curl/easy.h>
#include "curl_util.h"
#include "errno.h"

#include "qeo/mgmt_client.h"
#include "qeo_mgmt_curl_util.h"

/*#######################################################################
#                       TYPES SECTION                                   #
########################################################################*/
#define DATA_MEMM_MAX 0x08000 /*32kB*/
#define DATA_MEMM_MIN 0x800 /*2kB*/

typedef struct {
    char* data;
    ssize_t offset;
    ssize_t length;
} qmgmt_curl_data_helper;

/*#######################################################################
#                   STATIC FUNCTION DECLARATION                         #
########################################################################*/

/*#######################################################################
#                       STATIC VARIABLE SECTION                         #
########################################################################*/

/*#######################################################################
#                   STATIC FUNCTION IMPLEMENTATION                      #
########################################################################*/

/*
 * Intentionally made non static for unit testing.
 */
size_t _write_to_memory_cb( char *buffer, size_t size, size_t nmemb, void *outstream){
    qmgmt_curl_data_helper *data = (qmgmt_curl_data_helper *)outstream;
    size_t totalsize = size * nmemb;
    char *buf = NULL;
    size_t newlength = 0;
    size_t result = 0;

    do {
        if (totalsize == 0){
            break;
        }
        while (totalsize >= data->length - data->offset) {
            if (data->length >= DATA_MEMM_MAX) {
                qeo_log_w("Returned amount of data is too much, exceeding %d", DATA_MEMM_MAX);
                break;
            }
            if (data->length == 0){
                newlength = DATA_MEMM_MIN;
            } else {
                newlength = data->length*2;
            }

            buf = realloc(data->data, newlength);
            if (buf == NULL ) {
                qeo_log_w("Could not allocate buffer of %d bytes", newlength);
                break;
            }
            data->data = buf;
            data->length = newlength;
        }
        if (totalsize >= data->length - data->offset)
            break;
        memcpy(data->data+data->offset, buffer, totalsize);
        data->offset+=totalsize;
        buf=data->data+data->offset;
        *buf = '\0';/*Make it always null terminated. */
        result=totalsize;
    } while (0);

    return result;
}

/*
 * Intentionally made non static for unit testing.
 */
size_t _read_from_memory_cb(char *buffer, size_t size, size_t nmemb, void *instream)
{
    qmgmt_curl_data_helper *data = (qmgmt_curl_data_helper *)instream;
    size_t result = size * nmemb;

    if (result > (data->length - data->offset))
        result = data->length - data->offset;
    memcpy(buffer, data->data + data->offset, result);
    data->offset+=result;
    return result;
}

/*#######################################################################
#                   PUBLIC FUNCTION IMPLEMENTATION                      #
########################################################################*/

qeo_mgmt_client_retcode_t qeo_mgmt_curl_util_translate_rc(int curlrc)
{
    switch (curlrc) {
        case CURLE_SSL_CACERT:
        case CURLE_SSL_CACERT_BADFILE:
        case CURLE_SSL_CERTPROBLEM:
        case CURLE_SSL_CIPHER:
        case CURLE_SSL_CONNECT_ERROR:
        case CURLE_SSL_CRL_BADFILE:
        case CURLE_SSL_ENGINE_INITFAILED:
        case CURLE_SSL_ENGINE_NOTFOUND:
        case CURLE_SSL_ENGINE_SETFAILED:
        case CURLE_SSL_ISSUER_ERROR:
        case CURLE_SSL_SHUTDOWN_FAILED:
        case CURLE_SSL_PEER_CERTIFICATE:
        case CURLE_USE_SSL_FAILED:
            return QMGMTCLIENT_ESSL;
        case CURLE_COULDNT_CONNECT:
        case CURLE_COULDNT_RESOLVE_HOST:
        case CURLE_COULDNT_RESOLVE_PROXY:
            return QMGMTCLIENT_ECONNECT;
        case CURLE_HTTP_RETURNED_ERROR:
            return QMGMTCLIENT_EHTTP;
        case CURLE_OK:
            return QMGMTCLIENT_OK;
    }
    return QMGMTCLIENT_EFAIL;
}

CURLcode qeo_mgmt_curl_sslctx_cb(CURL * curl, void* sslctx, void* userdata)
{
    curl_ssl_ctx_helper *sslctxhelper = (curl_ssl_ctx_helper*)userdata;
    qeo_mgmt_client_retcode_t rc = QMGMTCLIENT_EFAIL;

    do {
        if (!sslctxhelper) {
            break;
        }
        rc = sslctxhelper->cb(sslctx, sslctxhelper->cookie);
    } while (0);

    if (rc != QMGMTCLIENT_OK){
        return CURLE_USE_SSL_FAILED;
    } else {
        return CURLE_OK;

    }
}

qeo_mgmt_client_retcode_t qeo_mgmt_curl_util_perform(CURL *ctx,
                                                     const char* url,
                                                     char* cid){
    CURLcode cret = curl_util_perform(ctx, url, OP_VERBOSE, cid);

    if (cret == CURLE_HTTP_RETURNED_ERROR){
        long http_status = 0;
        if (CURLE_OK == curl_easy_getinfo(ctx, CURLINFO_RESPONSE_CODE, &http_status)){
            if ((http_status == 403) || (http_status == 401) || (http_status == 405)){
                return QMGMTCLIENT_ENOTALLOWED;
            }
        }
    }
    return qeo_mgmt_curl_util_translate_rc(cret);
}


qeo_mgmt_client_retcode_t qeo_mgmt_curl_util_http_get_with_cb(CURL *ctx,
                                                     const char* url,
                                                     char *header,
                                                     curl_write_callback data_cb,
                                                     void *write_cookie){
    qeo_mgmt_client_retcode_t ret = QMGMTCLIENT_EFAIL;
    struct curl_slist *chunk = (header != NULL)?curl_slist_append(NULL, header):NULL;
    long http_status = 0;
    char correlation_id[CURL_UTIL_CORRELATION_ID_MAX_SIZE];
    curl_opt_helper opts[] = {
        { CURLOPT_WRITEFUNCTION, (void*) data_cb },
        { CURLOPT_WRITEDATA, (void*)write_cookie},
        { CURLOPT_FAILONERROR, (void*)1 },
        { CURLOPT_HTTPHEADER, (void*) chunk}};
    bool reset = false;

    do {
        if ((ctx == NULL) || (url == NULL) || (data_cb == NULL)){
            ret = QMGMTCLIENT_EINVAL;
            break;
        }
        if ((header != NULL) && (chunk == NULL)) {
            ret = QMGMTCLIENT_EMEM;
            break;
        }
        reset = true;
        if (CURLE_OK != curl_util_set_opts(opts, sizeof(opts) / sizeof(curl_opt_helper), ctx)) {
            ret = QMGMTCLIENT_EINVAL;
            break;
        }

        qeo_log_i("Start fetching data from <%s>", url);
        ret = qeo_mgmt_curl_util_perform(ctx, url, NULL);
        if (ret != QMGMTCLIENT_OK) {
            break;
        }
        if (curl_easy_getinfo(ctx, CURLINFO_RESPONSE_CODE, &http_status) == CURLE_OK) {
            if (http_status >= 400){
                ret = qeo_mgmt_curl_util_translate_rc(CURLE_HTTP_RETURNED_ERROR);
                curl_util_log_http_error_description(ctx, correlation_id);
                break;
            }
        }
        qeo_log_i("Successfully downloaded data");
    } while (0);

    if (reset == true){
        /* Make sure we reset all configuration for next calls */
        curl_easy_reset(ctx);
    }
    if (chunk != NULL){
        curl_slist_free_all(chunk);
    }

    if (ret != QMGMTCLIENT_OK) {
        qeo_log_w("Failure getting %s",url);
    }

    return ret;
}

qeo_mgmt_client_retcode_t qeo_mgmt_curl_util_http_get(CURL *ctx,
                                                     const char* url,
                                                     char *header,
                                                     char **data,
                                                     size_t *length)

{
    qmgmt_curl_data_helper data_helper = {0};
    qeo_mgmt_client_retcode_t ret = QMGMTCLIENT_EFAIL;

    ret = qeo_mgmt_curl_util_http_get_with_cb(ctx, url, header, _write_to_memory_cb, &data_helper);
    if (ret == QMGMTCLIENT_OK) {
        *data = data_helper.data;
        *length = data_helper.offset;
    } else {
        free(data_helper.data);
    }
    return ret;

}

qeo_mgmt_client_retcode_t qeo_mgmt_curl_util_https_get_with_cb(CURL *ctx,
                                                     const char* url,
                                                     char *header,
                                                     qeo_mgmt_client_ssl_ctx_cb ssl_cb,
                                                     void *ssl_cookie,
                                                     curl_write_callback data_cb,
                                                     void *write_cookie)
{
    curl_ssl_ctx_helper curlsslhelper = { ssl_cb, ssl_cookie };
    qeo_mgmt_client_retcode_t ret = QMGMTCLIENT_EFAIL;
    curl_opt_helper opts[] = {
        { CURLOPT_SSL_VERIFYPEER, (void*)0 },
        { CURLOPT_SSL_CTX_FUNCTION, (void*)qeo_mgmt_curl_sslctx_cb },
        { CURLOPT_SSL_CTX_DATA, (void*)&curlsslhelper }
    };
    bool reset = false;

    do {
        if ((ctx == NULL )|| (url == NULL) || (ssl_cb == NULL) || (data_cb == NULL)){
            ret = QMGMTCLIENT_EINVAL;
            break;
        }

        reset = true;
        if (CURLE_OK != curl_util_set_opts(opts, sizeof(opts) / sizeof(curl_opt_helper), ctx)) {
            ret = QMGMTCLIENT_EINVAL;
            break;
        }
        ret = qeo_mgmt_curl_util_http_get_with_cb(ctx, url, header, data_cb, write_cookie);
        reset = false;/* Already done. */
    } while (0);

    if (reset == true){
        /* Make sure we reset all configuration for next calls */
        curl_easy_reset(ctx);
    }

    if (ret != QMGMTCLIENT_OK) {
        qeo_log_w("Failure in https_get_%s",url);
    }
    return ret;

}


qeo_mgmt_client_retcode_t qeo_mgmt_curl_util_https_get(CURL *ctx,
                                                     const char* url,
                                                     char *header,
                                                     qeo_mgmt_client_ssl_ctx_cb ssl_cb,
                                                     void *ssl_cookie,
                                                     char **data,
                                                     size_t *length)
{
    qmgmt_curl_data_helper data_helper = {0};
    qeo_mgmt_client_retcode_t ret = QMGMTCLIENT_EFAIL;

    ret = qeo_mgmt_curl_util_https_get_with_cb(ctx, url, header, ssl_cb, ssl_cookie, _write_to_memory_cb, &data_helper);
    if (ret == QMGMTCLIENT_OK) {
        *data = data_helper.data;
        *length = data_helper.offset;
    } else {
        free(data_helper.data);
    }
    return ret;
}

static qeo_mgmt_client_retcode_t qeo_mgmt_curl_util_https_put_with_cb(CURL *ctx,
                                                     const char* url,
                                                     char *header,
                                                     qeo_mgmt_client_ssl_ctx_cb ssl_cb,
                                                     void *ssl_cookie,
                                                     curl_read_callback data_cb,
                                                     void *read_cookie,
                                                     intptr_t length)
{
    curl_ssl_ctx_helper curlsslhelper = { ssl_cb, ssl_cookie };
    char correlation_id[CURL_UTIL_CORRELATION_ID_MAX_SIZE];
    qeo_mgmt_client_retcode_t ret = QMGMTCLIENT_EFAIL;
    struct curl_slist *chunk = (header != NULL)?curl_slist_append(NULL, header):NULL;
    long http_status = 0;
    curl_opt_helper opts[] = {
        { CURLOPT_INFILESIZE, (void*) length }, /* keep this at index 0; value is update in code */
        { CURLOPT_UPLOAD, (void*)1 },
        { CURLOPT_READFUNCTION, (void*) data_cb },
        { CURLOPT_READDATA, (void*)read_cookie},
        { CURLOPT_HTTPHEADER, (void*) chunk},
        { CURLOPT_SSL_VERIFYPEER, (void*)0 },
        { CURLOPT_SSL_CTX_FUNCTION, (void*)qeo_mgmt_curl_sslctx_cb },
        { CURLOPT_SSL_CTX_DATA, (void*)&curlsslhelper },
    };

    bool reset = false;
    do {
        if ((ctx == NULL )|| (url == NULL) || (ssl_cb == NULL) || (data_cb == NULL)){
            ret = QMGMTCLIENT_EINVAL;
            break;
        }

        reset = true;
        if (CURLE_OK != curl_util_set_opts(opts, sizeof(opts) / sizeof(curl_opt_helper), ctx)) {
            ret = QMGMTCLIENT_EINVAL;
            break;
        }
        ret = qeo_mgmt_curl_util_perform(ctx, url, correlation_id);

        if (curl_easy_getinfo(ctx, CURLINFO_RESPONSE_CODE, &http_status) == CURLE_OK) {
            qeo_log_d("returned status code %ld", http_status);
            if (http_status >= 400){
                ret = qeo_mgmt_curl_util_translate_rc(CURLE_HTTP_RETURNED_ERROR);
                curl_util_log_http_error_description(ctx, correlation_id);
                break;
            }
        }
    } while (0);

    if (reset == true){
        /* Make sure we reset all configuration for next calls */
        curl_easy_reset(ctx);
    }
    if (chunk != NULL){
        curl_slist_free_all(chunk);
    }

    if (ret != QMGMTCLIENT_OK) {
        qeo_log_w("Failure in https_put_%s",url);
    }

    return ret;
}

qeo_mgmt_client_retcode_t qeo_mgmt_curl_util_https_put(CURL *ctx,
                                                     const char* url,
                                                     char *header,
                                                     qeo_mgmt_client_ssl_ctx_cb ssl_cb,
                                                     void *ssl_cookie,
                                                     char *data,
                                                     size_t length)
{
    qmgmt_curl_data_helper data_helper = {0};

    data_helper.data = data;
    data_helper.length = length;
    return qeo_mgmt_curl_util_https_put_with_cb(ctx, url, header, ssl_cb, ssl_cookie, _read_from_memory_cb, &data_helper, length);
}


