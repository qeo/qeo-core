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

/*#######################################################################
 # HEADER (INCLUDE) SECTION                                              #
 ########################################################################*/
#ifndef DEBUG
#define NDEBUG
#endif

#include <jni.h>
#include <limits.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <qeo/util_error.h>
#include <qeo/log.h>
#include <qeocore/api.h>
#include <qeocore/config.h>
#include <platform_api/platform_api.h>
#include <platform_api/platform_common.h>
#include "org_qeo_jni_NativeQeo.h"
#include "org_qeo_jni_NativeQeo.h"
#include <stdbool.h>
#include "jni_platform_p.h"

/*#######################################################################
 # TYPES SECTION                                                         #
 ########################################################################*/

/*#######################################################################
 # STATIC FUNCTION DECLARATION                                           #
 ########################################################################*/
static qeo_util_retcode_t android_registration_params_needed(uintptr_t app_context, qeo_platform_security_context_t context);
static void android_security_update_state(uintptr_t app_context, qeo_platform_security_context_t context, qeo_platform_security_state state, qeo_platform_security_state_reason state_reason);
static qeo_util_retcode_t android_remote_registration_confirmation_needed(uintptr_t app_context, qeo_platform_security_context_t context,
                                                                                 const qeo_platform_security_remote_registration_credentials_t *rrcred);

/*#######################################################################
 # STATIC VARIABLE SECTION                                               #
 ########################################################################*/
static qeo_platform_device_info _device_info;
static bool _device_info_set;
#ifdef ANDROID
static char _device_storage_path[PATH_MAX] = "";
#endif
static JavaVM *_jvm = NULL;
static const interposer_data_t *_idata_otc_cb = NULL;
static const interposer_data_t *_idata_update_cb = NULL;
static qeo_platform_security_context_t _qeo_sec_context;


const static qeo_platform_callbacks_t _android_platform_cbs = {
    .on_reg_params_needed = android_registration_params_needed,
    .on_sec_update = android_security_update_state,
    .on_rr_confirmation_needed = android_remote_registration_confirmation_needed
};
/*#######################################################################
 # STATIC FUNCTION IMPLEMENTATION                                        #
 ########################################################################*/
static void set_device_info(const char** devInfoParam, int64_t* devIdParam)
{

    qeo_log_i("Setting Device Info in Platform API");
    if (_device_info_set == true){
        return;
    }

    _device_info.manufacturer = strdup(devInfoParam[0]);
    _device_info.modelName = strdup(devInfoParam[1]);
    _device_info.productClass = strdup(devInfoParam[2]);
    _device_info.serialNumber = strdup(devInfoParam[3]);
    _device_info.hardwareVersion = strdup(devInfoParam[4]);
    _device_info.softwareVersion = strdup(devInfoParam[5]);
    _device_info.userFriendlyName = strdup(devInfoParam[6]);
    _device_info.configURL = strdup(devInfoParam[7]);
    _device_info.qeoDeviceId.upperId = devIdParam[0];
    _device_info.qeoDeviceId.lowerId = devIdParam[1];

    qeo_platform_set_device_info(&_device_info);

    _device_info_set = true;
}

static void java_otc_cb_interposer(const interposer_data_t* idata)
{
    JNIEnv *env = NULL;
    int getEnvStat = JNI_ERR;

    if (_jvm == NULL) {
        qeo_log_e("No JVM for otc call-back\n");
        return;
    }

    if (_jvm) {
        getEnvStat = (*_jvm)->GetEnv(_jvm, (void **)&env, JNI_VERSION_1_4);
        if (getEnvStat == JNI_EDETACHED) {
#ifdef ANDROID
            getEnvStat = (*_jvm)->AttachCurrentThread(_jvm, &env, NULL);
#else
            getEnvStat = (*_jvm)->AttachCurrentThread(_jvm, (void**)&env, NULL );
#endif
            if (getEnvStat < 0){
                abort();
            }
        }
    }
    if (getEnvStat == JNI_OK) {
        qeo_log_d("calling otc call-back\n");
        (*env)->CallVoidMethod(env, idata->object, idata->method);
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in CallVoidMethod otc_cb");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }
    else {
        qeo_log_e("Could not get JNI environment for otc call-back: %d\n", getEnvStat);
    }

    (*_jvm)->DetachCurrentThread(_jvm);
}

static void java_update_cb_interposer(const interposer_data_t* idata, qeo_platform_security_state state,
                               qeo_platform_security_state_reason state_reason)
{
    JNIEnv *env = NULL;
    int getEnvStat = JNI_ERR;
    jstring update_state = NULL;
    jstring update_state_reason = NULL;

    if (NULL == _jvm) {
        qeo_log_e("No JVM for StatusUpdate call-back\n");
        return;
    }

    if (_jvm) {
        getEnvStat = (*_jvm)->GetEnv(_jvm, (void **)&env, JNI_VERSION_1_4);
        if (getEnvStat == JNI_EDETACHED) {
#ifdef ANDROID
            getEnvStat = (*_jvm)->AttachCurrentThread(_jvm, &env, NULL);
#else
            getEnvStat = (*_jvm)->AttachCurrentThread(_jvm, (void**)&env, NULL );
#endif
            if (getEnvStat < 0)
                abort();
        }
    }
    if (getEnvStat == JNI_OK) {
        update_state = (*env)->NewStringUTF(env, platform_security_state_to_string(state));
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in NewStringUTF update_state");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
        if (NULL == update_state) {
            qeo_log_e("Failed to NewStringUTF from update_state");
            return;
        }

        update_state_reason = (*env)->NewStringUTF(env, platform_security_state_reason_to_string(state_reason));
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in NewStringUTF update_state_reason");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
        if (NULL == update_state_reason) {
            qeo_log_e("Failed to NewStringUTF from update_state_reason");
            return;
        }

        qeo_log_d("StatusUpdate call-back called\n");
        (*env)->CallVoidMethod(env, idata->object, idata->method, update_state, update_state_reason);
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in CallVoidMethod otc_cb");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }
    else {
        qeo_log_e("Could not get JNI environment for StatusUpdate call-back: %d\n", getEnvStat);
    }

    if (NULL != update_state) {
        (*env)->DeleteLocalRef(env, update_state);
    }
    if (NULL != update_state_reason) {
        (*env)->DeleteLocalRef(env, update_state_reason);
    }

    (*_jvm)->DetachCurrentThread(_jvm);
}

static qeo_util_retcode_t android_registration_params_needed(uintptr_t app_context, qeo_platform_security_context_t context){

    qeo_log_d("You are now in qeo_platform_security_registration_credentials_needed: START");

    if (_idata_otc_cb == NULL ) {
        qeo_log_e("_idata_otc_cb == NULL !");
        return QEO_UTIL_EFAIL;
    }

    _qeo_sec_context = context;
    java_otc_cb_interposer(_idata_otc_cb);

    qeo_log_d("You are now in qeo_platform_security_registration_credentials_needed: DONE");
    return QEO_UTIL_OK;
}

static void android_security_update_state(uintptr_t app_context, qeo_platform_security_context_t context, qeo_platform_security_state state, qeo_platform_security_state_reason state_reason){

    qeo_log_d("You are now in qeo_platform_security_update_state: START");
    if (_idata_update_cb == NULL) {
        qeo_log_e("_idata_update_cb == NULL !");
        return;
    }

    java_update_cb_interposer(_idata_update_cb, state, state_reason);

    qeo_log_d("You are now in qeo_platform_security_update_state: DONE");

}

static qeo_util_retcode_t android_remote_registration_confirmation_needed(uintptr_t app_context, qeo_platform_security_context_t context,
                                                                                 const qeo_platform_security_remote_registration_credentials_t *rrcred){

   return qeo_platform_confirm_remote_registration_credentials(context, true);

}

/*#######################################################################
 # PUBLIC FUNCTION IMPLEMENTATION                                        #
 ########################################################################*/
bool platform_jni_init(void){

    qeo_util_retcode_t ret; 

    if ((ret = qeo_platform_init(0, &_android_platform_cbs)) != QEO_UTIL_OK){
        qeo_log_e("Could not init qeo platform layer with android implementation");
        return false;
    }
    return true;
}

bool platform_jni_set_interposer(JavaVM *jvm, const interposer_data_t *idata_otc_cb, const interposer_data_t *idata_update_cb){
    qeo_log_d("");
    if (_jvm != NULL || _idata_otc_cb != NULL || _idata_update_cb != NULL) {
        qeo_log_e("There is an old interposer registered, this would overwrite data.");
        return false;
    }

    if (jvm != NULL && idata_otc_cb != NULL && idata_update_cb != NULL) {
        _jvm = jvm;
        _idata_otc_cb = idata_otc_cb;
        _idata_update_cb = idata_update_cb;
    }
    else {
        return false;
    }

    return true;
}

void platform_jni_clear_interposer()
{
    _jvm = NULL;
    _idata_otc_cb = NULL;
    _idata_update_cb = NULL;
}

/*#######################################################################
 # JNI FUNCTION IMPLEMENTATION                                        #
 ########################################################################*/
JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeQeo_nativeSetRegistrationCredentials(JNIEnv* env, jobject thiz, jstring otc, jstring url)
{
    qeo_log_d("");
    const char *otcStr = NULL;
    const char *urlStr = NULL;

    if (NULL == otc) {
        qeo_log_e("Illegal NULL pointer provided for parameter otc in NativeQeo.nativeSetRegistrationCredentials");
        return (jint)QEO_EINVAL;
    }

    if (NULL == url) {
        qeo_log_e("Illegal NULL pointer provided for parameter url in NativeQeo.nativeSetRegistrationCredentials");
        return (jint)QEO_EINVAL;
    }

    qeo_retcode_t rc = QEO_OK;
    do {
        otcStr = (*env)->GetStringUTFChars(env, otc, 0);
        urlStr = (*env)->GetStringUTFChars(env, url, 0);

        if (NULL == otcStr) {
            qeo_log_e("Failed to GetStringUTFChars from otc");
            rc = QEO_EFAIL;
            break;
        }

        if (NULL == urlStr) {
            qeo_log_e("Failed to GetStringUTFChars from url");
            rc = QEO_EFAIL;
            break;
        }

        if (_qeo_sec_context == 0){
            qeo_log_e("Context was not set ");
            rc = QEO_EFAIL;
            break;
        }

        if (qeo_platform_set_otc_url(_qeo_sec_context, otcStr, urlStr) != QEO_UTIL_OK){
            qeo_log_e("Failed to set the otc and URL @ jni platform API");
            rc = QEO_EFAIL;
            break;
        }

        rc = QEO_OK;
    } while (0);

    if (NULL != otcStr) {
        (*env)->ReleaseStringUTFChars(env, otc, otcStr);
    }
    if (NULL != urlStr) {
        (*env)->ReleaseStringUTFChars(env, url, urlStr);
    }

    return (jint)rc;
}

/*This function is to be called from the Android service whenever the registration is canceled*/

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeQeo_nativeCancelRegistration(JNIEnv* env, jobject thiz)
{
    qeo_log_d("You are now in %s", __func__);

    qeo_retcode_t rc = QEO_OK;
    do {

        if (_qeo_sec_context == 0){
            qeo_log_e("Context was not set ?!");
            rc = QEO_EFAIL;
            break;
        }

        qeo_util_retcode_t retval = qeo_platform_cancel_registration(_qeo_sec_context);

        if (QEO_UTIL_OK != retval) {
            qeo_log_e("Failed to cancel registration @ jni platform API with context = %"PRIxPTR" (%d)", _qeo_sec_context, retval);
            rc = QEO_EFAIL;
            break;
        }

    } while (0);

    return (jint)rc;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeQeo_nativeConfigDeviceInfo(JNIEnv *env,
                                                                   jclass cl,
                                                                   jobjectArray devInfoArray,
                                                                   jlongArray devIdArray)
{
    if (NULL == devInfoArray) {
        qeo_log_e("Illegal NULL pointer provided for parameter devInfoArray in NativeQeo.nativeConfigDeviceInfo");
        return (jint)QEO_EINVAL;
    }

    if (NULL == devIdArray) {
        qeo_log_e("Illegal NULL pointer provided for parameter devIdArray in NativeQeo.nativeConfigDeviceInfo");
        return (jint)QEO_EINVAL;
    }

    qeo_retcode_t rc = QEO_OK;
    int64_t devIdParam[2];
    jlong* pDevIdArray = NULL;
    jsize stringCount = (*env)->GetArrayLength(env, devInfoArray);
    jsize longCount = (*env)->GetArrayLength(env, devIdArray);

    if (longCount != 2) {
        qeo_log_e("Invalid array length (%d) of devIdArray in NativeQeo.nativeConfigDeviceInfo", longCount);
        return (jint)QEO_EINVAL;
    }

    if (stringCount != 8) {
        qeo_log_e("Invalid array length (%d) of devInfoArray in NativeQeo.nativeConfigDeviceInfo", stringCount);
        return (jint)QEO_EINVAL;
    }

    jstring string[stringCount];
    const char* devInfoParam[stringCount];
    memset(devInfoParam, 0, sizeof(devInfoParam));


    do {
        qeo_retcode_t ret_array_fill = QEO_OK;
        for (int i=0; i<stringCount; i++) {
            string[i] = (jstring) (*env)->GetObjectArrayElement(env, devInfoArray, i);
            if (NULL != (*env)->ExceptionOccurred(env)) {
                qeo_log_e("Unexpected exception occurred from GetObjectArrayElement");
                (*env)->ExceptionDescribe(env);
                (*env)->ExceptionClear(env);
                ret_array_fill = QEO_EFAIL;
                break;
            }

            devInfoParam[i] = (*env)->GetStringUTFChars(env, string[i], NULL);
            if (NULL == devInfoParam[i]) {
                qeo_log_e("Failed to GetStringUTFChars from devInfoParam[i]");
                ret_array_fill = QEO_EFAIL;
                break;
            }
        }

        if (QEO_OK != ret_array_fill) {
            qeo_log_e("Failed to get all strings from array devInfoArray");
            rc = QEO_EFAIL;
            break;
        }

        pDevIdArray = (*env)->GetLongArrayElements(env, devIdArray, (void *) 0);
        if (NULL == pDevIdArray) {
            qeo_log_e("Failed to GetLongArrayElements on devIdArray");
            rc = QEO_EFAIL;
            break;
        }
        if (sizeof(pDevIdArray) < 2) {
            qeo_log_e("Invalid array length for pDevIdArray (%d)", sizeof(pDevIdArray));
            rc = QEO_EINVAL;
            break;
        }
        devIdParam[0] = (int64_t) pDevIdArray[0];
        devIdParam[1] = (int64_t) pDevIdArray[1];

        set_device_info(devInfoParam, devIdParam);
    } while (0);

    if (NULL != pDevIdArray) {
        (*env)->ReleaseLongArrayElements(env, devIdArray, pDevIdArray, 0);
    }

    for (int i=0; i<stringCount; i++) {
        if (NULL != devInfoParam[i]) {
            (*env)->ReleaseStringUTFChars(env, string[i], devInfoParam[i]);
        }
    }

    return (jint)rc;
}

