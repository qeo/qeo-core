/*
 * Copyright (c) 2016 - Qeo LLC
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
 # HEADER (INCLUDE) SECTION                                              #
 ########################################################################*/
#ifndef DEBUG
#define NDEBUG
#endif

#define _GNU_SOURCE
#include <stdlib.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>

#include <platform_api/platform_api.h>
#include <platform_api/platform_common.h>
#include <qeo/log.h>
#include <qeo/util_error.h>

#include "linux_default_device_p.h"

#ifdef __APPLE__
#if !(TARGET_OS_IPHONE)
// Mac OSX only
#include <qeo/platform.h>
#include <qeo/platform_security.h>
#include <CoreFoundation/CFArray.h>
#include <Security/SecTrust.h>
#include <Security/SecCertificate.h>
#include <Security/SecPolicy.h>
#endif
#endif
/*#######################################################################
 # STATIC FUNCTION PROTOTYPE
 ########################################################################*/
static qeo_util_retcode_t default_registration_params_needed(uintptr_t app_context, qeo_platform_security_context_t context);
static qeo_util_retcode_t remote_registration(qeo_platform_security_context_t context,
                                                                    const char *rrf);
static void default_security_update_state(uintptr_t app_context, qeo_platform_security_context_t context, qeo_platform_security_state state, qeo_platform_security_state_reason state_reason);
static qeo_util_retcode_t default_remote_registration_confirmation_needed(uintptr_t app_context, qeo_platform_security_context_t context,
                                                                                 const qeo_platform_security_remote_registration_credentials_t *rrcred);

/*#######################################################################
 # STATIC VARIABLES
 ########################################################################*/
const static qeo_platform_callbacks_t _default_platform_cbs = {
    .on_reg_params_needed = default_registration_params_needed,
    .on_sec_update = default_security_update_state,
    .on_rr_confirmation_needed = default_remote_registration_confirmation_needed
};

/*#######################################################################
 # STATIC FUNCTION IMPLEMENTATION                                        #
 ########################################################################*/

static qeo_util_retcode_t remote_registration(qeo_platform_security_context_t context,
                                                                    const char *rrf){
    qeo_util_retcode_t ret = QEO_UTIL_EFAIL;
    FILE *f = NULL;

    do {
        char suggested_username[64];
        unsigned long registration_window;
        /* file/fifo should be created in advance */
        if ((f = fopen(rrf, "r")) == NULL){
            qeo_log_e("Could not open remote registration file for reading");
            break;
        }

        if (fscanf(f, "%63s %lu", suggested_username, &registration_window) != 2){
            qeo_log_e("Could not read from remote_registration file");
            break;
        }
    
        ret = qeo_platform_set_remote_registration_params(context, suggested_username, registration_window);

    } while (0);

    if (f != NULL){
        fclose(f);
    }

    return ret;
}

static qeo_util_retcode_t cli_otc_url(qeo_platform_security_context_t context){

    qeo_util_retcode_t retval = QEO_UTIL_OK;
    size_t len = 0;
    char *otc = NULL;
    char *url = NULL;
    ssize_t bytes_read = 0;
    char *lf = NULL;

    do {
        fprintf(stdout, "Please provide the OTC (Press enter to cancel): ");
        fflush(stdout);
        bytes_read = getline(&otc, &len, stdin);
        /* not sure this will work on all terminals - some might return \r\n ... */
        if (bytes_read == -1 || (bytes_read == 1 && otc[0] == '\n')){
            retval = qeo_platform_cancel_registration(context);
            break;
        } else {
            lf = strchr(otc, '\n');
            if (lf != NULL){
                *lf = '\0';
            }
        }

        fprintf(stdout, "Please provide the URL [" QEO_REGISTRATION_URL "]: ");
        fflush(stdout);
        bytes_read = getline(&url, &len, stdin);
        if (bytes_read == -1 || (bytes_read == 1 && url[0] == '\n')){
            if (bytes_read != -1){
                free(url);
            }
            url = strdup(QEO_REGISTRATION_URL);
        } else {
            lf = strchr(url, '\n');
            if (lf != NULL){
                *lf = '\0';
            }
        }

        retval = qeo_platform_set_otc_url(context, otc, url);
    } while (0);

    free(otc);
    free(url);

    return retval;
}

static qeo_util_retcode_t default_registration_params_needed(uintptr_t app_context, qeo_platform_security_context_t context){


    qeo_util_retcode_t retval = QEO_UTIL_OK;
    char *rrf = NULL;

    if ((rrf = getenv("REMOTE_REGISTRATION_FILE")) != NULL){
        fprintf(stdout, "Starting remote registration. Please register this device from a Qeo enabled management app.\n");
        if (remote_registration(context, rrf) != QEO_UTIL_OK){
            qeo_log_w("Fallback to prompt");
            return cli_otc_url(context);
        }
    } else {
        return cli_otc_url(context);
    }

    return retval;
}

static void default_security_update_state(uintptr_t app_context, qeo_platform_security_context_t context, qeo_platform_security_state state, qeo_platform_security_state_reason state_reason)
{
    if (state == QEO_PLATFORM_SECURITY_AUTHENTICATION_FAILURE){
        fprintf(stderr, "Could not authenticate QEO (reason = %s) !\r\n", platform_security_state_reason_to_string(state_reason));
    }
    /* ignore all other states */
}

static qeo_util_retcode_t default_remote_registration_confirmation_needed(uintptr_t app_context, qeo_platform_security_context_t context,
                                                                                 const qeo_platform_security_remote_registration_credentials_t *rrcred){
                                                                                 

    const char *auto_confirmation = getenv("REMOTE_REGISTRATION_AUTO_CONFIRM");
    char reply[4];


    if (auto_confirmation != NULL){
        bool feedback = (bool)(auto_confirmation[0] - '0');
        fprintf(stdout, "Automatic confirmation (%c) of remote registration credentials (realm name = %s, url = %s)\r\n", feedback ? 'Y' : 'N', rrcred->realm_name, rrcred->url);
        return qeo_platform_confirm_remote_registration_credentials(context, feedback);
    }

    fprintf(stdout, "Management app wants to register us in realm %s, URL: %s. [Y/n]\r\n", rrcred->realm_name, rrcred->url);

    while (true) {
        int items = scanf("%3s", reply);
        if (items == 1) {
            if (reply[0] == 'n') {
                fprintf(stdout, "Ignoring request.\n");
                return qeo_platform_confirm_remote_registration_credentials(context, false);
            } else {
                fprintf(stdout, "Registering device...\n");
                return qeo_platform_confirm_remote_registration_credentials(context, true);
            }
            break;
        }
        else if (errno != 0) {
            perror("scanf");
            return QEO_UTIL_EFAIL;
        }
        else {
            qeo_log_d("scanf failed: %d", items);
        }
    } while (true);


    return QEO_UTIL_OK;
}

#ifdef __APPLE__
#if !(TARGET_OS_IPHONE)
static qeo_util_retcode_t on_platform_custom_certificate_validator_cb(qeo_der_certificate* certf, int size){

    qeo_util_retcode_t returnValue = QEO_UTIL_EFAIL;
    CFMutableArrayRef certificateChain = CFArrayCreateMutable(NULL,size,NULL);
    SecPolicyRef policyForSSLCertificateChains = NULL;
    SecTrustRef  trustManagementRef = NULL;

    do {

        // Step 1:
        // Convert array of raw "der"-certificate data into an array of iOS "SecCertificateRef" format
        for (int idx=0; idx<size; ++idx){
            CFDataRef rawDerCertificate = CFDataCreate(NULL,(const UInt8*)certf[idx].cert_data,certf[idx].size);

            if(0 < CFDataGetLength(rawDerCertificate)) {
                SecCertificateRef derCertificate = SecCertificateCreateWithData(NULL, rawDerCertificate);

                if(NULL != derCertificate) {
                    CFArrayAppendValue(certificateChain, derCertificate);
                } else {
                    qeo_log_e("CERTIFICATE Conversion to Mac OSX format FAILED");
                    break;
                }
            } else {
                qeo_log_e("Provided der-CERTIFICATE EMPTY");
                break;
            }

            // cleanup resource
            if (NULL != rawDerCertificate){
                CFRelease(rawDerCertificate);
            }
        }

        // Step 2:
        // Create policy object for evaluating SSL certificate chains
        policyForSSLCertificateChains = SecPolicyCreateSSL(true, NULL);
        if (NULL == policyForSSLCertificateChains){
            qeo_log_e("SSL POLICY CREATION FAILED");
            break;
        }

        // Step 3:
        // Create a trust management object based on certificates and policies
        OSStatus result = SecTrustCreateWithCertificates(certificateChain,
                                                         policyForSSLCertificateChains,
                                                         &trustManagementRef);

        if (errSecSuccess != result || NULL == trustManagementRef) {
            qeo_log_e("Mac OSX TRUST MANAGEMENT creation FAILED");
            break;
        }

        // Step 4:
        // Evaluate the certificate chain for the SSL policy
        SecTrustResultType resultType = 0;
        result = SecTrustEvaluate (trustManagementRef, &resultType);
        if (errSecSuccess != result || ((kSecTrustResultProceed != resultType) && (kSecTrustResultUnspecified != resultType))) {
            qeo_log_e("Certificate chain validation FAILED");
            break;
        }

        returnValue = QEO_UTIL_OK;

    } while (0);

    // Step 5
    // Cleanup of allocated resources
    if (NULL != trustManagementRef){
        CFRelease(trustManagementRef);
    }
    if (NULL != policyForSSLCertificateChains){
        CFRelease(policyForSSLCertificateChains);
    }
    for (int idx=0; idx<CFArrayGetCount(certificateChain); ++idx){
        CFRelease((SecCertificateRef)CFArrayGetValueAtIndex(certificateChain,idx));
    }
    if (NULL != certificateChain){
        CFRelease(certificateChain);
    }
    return returnValue;
}
#endif
#endif

/*#######################################################################
 # PUBLIC FUNCTION IMPLEMENTATION                                        #
 ########################################################################*/


#ifdef __mips__
void __attribute__ ((constructor)) default_impl_init(void){
#else
void __attribute__ ((constructor(1000))) default_impl_init(void){
#endif

    qeo_util_retcode_t ret;
    const char *ca_file = NULL;
    const char *ca_path = NULL;

    if ((ret = qeo_platform_init(0, &_default_platform_cbs)) != QEO_UTIL_OK){
        qeo_log_e("Could not init qeo platform layer with default implementation");
        return;
    }

    if (qeo_platform_set_device_info(get_default_device_info()) != QEO_UTIL_OK){
        qeo_log_e("Could not set device info");
        return;

    }

    if (qeo_platform_set_device_storage_path(get_default_device_storage_path()) != QEO_UTIL_OK){
        qeo_log_e("Could not set device storage path");
        return;
    }
    get_default_cacert_path(&ca_file, &ca_path);
    if (qeo_platform_set_cacert_path(ca_file, ca_path) != QEO_UTIL_OK) {
        qeo_log_e("Could not set CA certificates path");
        return;
    }

#ifdef __APPLE__
#if !(TARGET_OS_IPHONE)
    // Mac OSX only
    if (QEO_UTIL_OK != qeo_platform_set_custom_certificate_validator(on_platform_custom_certificate_validator_cb)){
        qeo_log_e("Could not set certificate validator in the platform layer");
        return;
    }
#endif
#endif
}


#ifdef __mips__
void __attribute__ ((destructor)) default_impl_destroy(void){
#else
void __attribute__ ((destructor(1000))) default_impl_destroy(void){
#endif

    free_default_paths();
    free_default_device_info();

}
