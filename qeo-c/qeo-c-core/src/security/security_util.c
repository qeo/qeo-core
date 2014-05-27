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
#include <qeo/log.h>
#include <openssl/ssl.h>
#include <openssl/err.h>

/*#######################################################################
#                       TYPES SECTION                                   #
########################################################################*/

/*#######################################################################
#                   STATIC FUNCTION DECLARATION                         #
########################################################################*/
static int verify_server_cb(int ok, X509_STORE_CTX *ctx);

/*#######################################################################
#                       STATIC VARIABLE SECTION                         #
########################################################################*/

/*#######################################################################
#                   STATIC FUNCTION IMPLEMENTATION                      #
########################################################################*/
static int verify_server_cb(int ok, X509_STORE_CTX *ctx)
{
    qeo_log_d("Verifying server");

    /* TODO: EXTEND ME */
    /* for this we need the root certificate */

    qeo_log_d("Verified");

    return 1;
}

/*#######################################################################
#                   PUBLIC FUNCTION IMPLEMENTATION                      #
########################################################################*/
void security_util_configure_ssl_ctx(SSL_CTX *ssl_ctx)
{
    SSL_CTX_set_options(ssl_ctx, SSL_OP_ALL | SSL_OP_NO_SSLv2);

    /* TODO: set the certificate file to verify the server authentication. we can't do it now because we don't have that file ...
     *     use SSL_CTX_load_verify_locations()
     * */

    SSL_CTX_set_verify(ssl_ctx, SSL_VERIFY_PEER, verify_server_cb);
#if 0
    SSL_CTX_set_quiet_shutdown(ctx, 1); // TODO: check if this is needed, probably not
#endif
}

void dump_openssl_error_stack(const char *msg)
{
    unsigned long err;
    const char    *file, *data;
    int           line, flags;
    char          buf[256];

    qeo_log_e("%s", msg);
    while ((err = ERR_get_error_line_data(&file, &line, &data, &flags))) {
        qeo_log_e("err %lu @ %s:%d -- %s\n", err, file, line, ERR_error_string(err, buf));
    }

    ERR_clear_error();
}

