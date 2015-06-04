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
#else
#endif
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <unistd.h>
#include <string.h>
#include <stdbool.h>
#include <assert.h>
#include <sys/stat.h>
#include <qeocore/api.h>
#include "org_qeo_jni_NativeQeo.h"
#include "org_qeo_jni_NativeData.h"
#include "org_qeo_jni_NativeType.h"
#include "org_qeo_jni_NativeTypeSupport.h"
#include "org_qeo_jni_NativeReader.h"
#include "org_qeo_jni_NativeWriter.h"
#include "jni_common_p.h"
#include "jni_platform_p.h"
#include <platform_api/platform_api.h>

#include <dds/dds_aux.h>
#include <qeo/util_error.h>
#include <qeo/platform.h>
#include <qeo/factory.h>
#include <qeocore/remote_registration.h>
#include <qeocore/identity.h>
#include <qeocore/config.h>
#include <qeo/device.h>
#include <semaphore.h>
#include <qeo/log.h>

#define log_printf
//#define log_printf qeo_log_d

typedef struct
{
    jobject object;
    jclass class;
    jmethodID onDataAvailable;
    jmethodID onPolicyUpdate;
    jmethodID onPolicyUpdateDone;
} rw_interposer_data_t;

/* Keep this lined up with org.qeo.internal.common.EntityType */
typedef enum {
    ETYPE_UNKNOWN,
    ETYPE_EVENT_DATA,
    ETYPE_STATE_DATA,
    ETYPE_STATE_UPDATE,
} entity_type_t;

typedef struct {
    bool success;
    bool interrupted;
    bool init_done;
    sem_t semaphore;
} qeofactory_userdata;


static int etype_to_eflags(entity_type_t etype)
{
    int flags = QEOCORE_EFLAG_NONE;

    switch (etype) {
        case ETYPE_UNKNOWN:
            break;
        case ETYPE_EVENT_DATA:
            flags = QEOCORE_EFLAG_EVENT_DATA;
            break;
        case ETYPE_STATE_DATA:
            flags = QEOCORE_EFLAG_STATE_DATA;
            break;
        case ETYPE_STATE_UPDATE:
            flags = QEOCORE_EFLAG_STATE_UPDATE;
            break;
    }
    return flags;
}

static JavaVM *_jvm;
static jclass _validator_clazz;

void java_init_done_cb_interposer(uintptr_t userdata, bool result);


//needed for qeo-java-forwarder to avoid symbols to be stripped out from qeoJNI.so. Don't remove
void *_qeocore_fwdfactory_set_public_locator = qeocore_fwdfactory_set_public_locator;

static qeo_loglvl_t logLevel = QEO_LOG_WARNING; //default level

#ifdef ANDROID
#include <android/log.h>
static int _lvl2prio[] = { ANDROID_LOG_DEBUG, ANDROID_LOG_INFO, ANDROID_LOG_WARN, ANDROID_LOG_ERROR };
#else
static const char *_lvl2str[] = { "D", "I", "W", "E" };
#endif

static void jni_logger(qeo_loglvl_t lvl, const char* fileName, const char* functionName, int lineNumber, const char *format, ...)
{
    if (lvl < logLevel) {
        //don't log this message
        return;
    }
    va_list args;

    va_start(args, format);
#ifdef ANDROID
    char buf[512];
    int len = 0;
    len = snprintf(buf, sizeof(buf),"%s:%s:%d - ", fileName, functionName, lineNumber);
    vsnprintf(buf + len, sizeof(buf) - len,  format, args);
    __android_log_print(_lvl2prio[lvl], "QeoNative", "%s", buf);
#else
    printf("%s - %s:%s:%d - ", _lvl2str[lvl], fileName, functionName, lineNumber);
    vprintf(format, args);
    printf("\n");
#endif
    va_end(args);
}

static jint thread_attach(JNIEnv **env, bool *attached)
{
    jint rc;

    *attached = false;
    rc = ((*_jvm)->GetEnv(_jvm, (void **)env, JNI_VERSION_1_4));
    if (JNI_EDETACHED == rc) {
#ifdef ANDROID
        rc = (*_jvm)->AttachCurrentThread(_jvm, env, NULL);
#else
        rc = (*_jvm)->AttachCurrentThread(_jvm, (void**)env, NULL);
#endif
        if (JNI_OK == rc) {
            *attached = true;
        }
    }
    return rc;
}

static int thread_detach(bool attached)
{
    /* if thread was detached when calling thread_attach, we detach again */
    if (attached) {
        (*_jvm)->DetachCurrentThread(_jvm);
    }
}

static qeo_util_retcode_t java_certificate_validator(qeo_der_certificate* certificate_data, int cert_count) {
    qeo_util_retcode_t rv = QEO_UTIL_EFAIL;
    JNIEnv *env;
    bool attached;

    qeo_log_i("java_certificate_validator validating certificate data %p (%d)", certificate_data, cert_count);
    if (JNI_OK == thread_attach(&env, &attached)) {
        //We are attached to the JVM and have a valid env pointer.
        jobjectArray rawData;
        jclass validatorClass;
        jmethodID id;
        int i;
        jclass byteArrayClass = (*env)->FindClass(env, "[B");

        if (byteArrayClass == NULL) {
            qeo_log_e("Failed to find class byte[]");
            goto out;
        }
        rawData = (*env)->NewObjectArray(env, cert_count, byteArrayClass, NULL);

        if (rawData == NULL) {
            qeo_log_e("failed to create byte[][] of size %d", cert_count);
            goto out;
        }

        //We put all data into the array we just created
        for (i = 0; i < cert_count; i++) {
            jbyteArray data = (*env)->NewByteArray(env, certificate_data[i].size);

            if (data == NULL) {
                qeo_log_e("Failed to create byte[] of size %d",certificate_data[i].size);
                goto out;
            }
            (*env)->SetByteArrayRegion(env, data, 0, certificate_data[i].size, (jbyte*) certificate_data[i].cert_data);
            if ((*env)->ExceptionCheck(env)) {
                qeo_log_e("Failed to copy certificate data into array");
                goto out;
            }
            (*env)->SetObjectArrayElement(env, rawData, i, data);
            if ((*env)->ExceptionCheck(env)) {
                qeo_log_e("Failed to store byte[] into array");
                goto out;
            }
            (*env)->DeleteLocalRef(env, data);
        }
        //Now call our java callback.
        if (_validator_clazz == NULL) {
            qeo_log_e("validator class was not set.");
            goto out;
        }
        id = (*env)->GetStaticMethodID(env, _validator_clazz, "validateCertificateChain", "([[B)V");
        if (id == NULL) {
            qeo_log_e("Failed to lookup validator methodID");
            goto out;
        }

        (*env)->CallStaticVoidMethod(env, _validator_clazz, id, rawData);

        if  (JNI_FALSE == (*env)->ExceptionCheck(env)) {
            rv = QEO_UTIL_OK;
        }
    }
out:
    if((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env); //clear the exception. Do not keep it hanging around...
    }
    thread_detach(attached);
    return rv;
}

static jclass _classNativeQeo;         // org.qeo.jni.NativeQeo
static jmethodID _methodDispatchWakeUp;// org.qeo.jni.NativeQeo.dispatchWakeUp
static jmethodID _methodOnBgnsConnected;// org.qeo.jni.NativeQeo.onBgnsConnected

static void on_bgns_wakeup(uintptr_t userdata, const char *type_name)
{
    JNIEnv *env;
    bool attached;

    /* notify Java */
    if (JNI_OK == thread_attach(&env, &attached)) {
        jstring jstr = (*env)->NewStringUTF(env, type_name);
        (*env)->CallStaticVoidMethod(env, _classNativeQeo, _methodDispatchWakeUp, jstr);
        (*env)->DeleteLocalRef(env, jstr);
    }
    if ((*env)->ExceptionCheck(env)) {
        qeo_log_e("Failed to call NativeQeo.dispatchWakeUp");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
    thread_detach(attached);
}
static void on_bgns_connect(uintptr_t userdata, int fd, bool connected)
{
    JNIEnv *env;
    bool attached;

    /* notify Java */
    if (JNI_OK == thread_attach(&env, &attached)) {
        (*env)->CallStaticVoidMethod(env, _classNativeQeo, _methodOnBgnsConnected, fd, connected);
    }
    if ((*env)->ExceptionCheck(env)) {
        qeo_log_e("Failed to call NativeQeo.onBgnsConnected");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
    thread_detach(attached);
}

static const qeo_bgns_listener_t _bgns_listener = {
    .on_wakeup = on_bgns_wakeup,
    .on_connect = on_bgns_connect
};

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv *env;
    _jvm = vm;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_4) != JNI_OK) {
        return (-1);
    }

    if (platform_jni_init() == false){
        return -1;
    }
    qeo_log_set_logger(jni_logger);
    qeo_platform_set_custom_certificate_validator(java_certificate_validator);

    /* initialize background notification infrastructure */
    qeo_bgns_register(&_bgns_listener, 0);
    _classNativeQeo = (*env)->FindClass(env, "org/qeo/jni/NativeQeo");
    if ((*env)->ExceptionCheck(env)) {
        return -1;
    }
    _classNativeQeo = (jclass)(*env)->NewGlobalRef(env, _classNativeQeo);
    _methodDispatchWakeUp = (*env)->GetStaticMethodID(env, _classNativeQeo,
                                                      "dispatchWakeUp", "(Ljava/lang/String;)V");
    _methodOnBgnsConnected = (*env)->GetStaticMethodID(env, _classNativeQeo,
                                                      "onBgnsConnected", "(IZ)V");
    if ((*env)->ExceptionCheck(env)) {
        return -1;
    }
    return (JNI_VERSION_1_4);
}

/**
 * This function is used to store a global reference to certificate validator class.
 */
JNIEXPORT void JNICALL Java_org_qeo_jni_NativeQeo_initCertValidator(JNIEnv *env, jclass cl, jclass validator_clazz)
{
    _validator_clazz = (*env)->NewGlobalRef(env, validator_clazz);
}

/*
 * This function returns the platform specific storage location as a path.
 * If the path does not exist, it will create one with 0700 mode.
 */
static bool build_device_storage_path(const char *path)
{
    if (access(path, R_OK | W_OK) != 0) {
        qeo_log_i("Creating storage directory");
        if (mkdir(path, 0700) != 0) {
            qeo_log_e("Failed to create storage directory %s", path);
            return false;
        }
    }

    return true;
}

JNIEXPORT void JNICALL Java_org_qeo_jni_NativeQeo_nativeSetStoragePath(JNIEnv *env, jclass cl, jstring jpath)
{
    if (jpath == NULL) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/NullPointerException"), "path is null");
        return;
    }

    const char* path = (*env)->GetStringUTFChars(env, jpath, NULL);
    if (path) {
        const char * copy = strdup(path);
        if (copy) {
            if (build_device_storage_path(copy) == true) {
                qeo_log_i("Setting storage_path '%s'\r\n", copy);
                qeo_platform_set_device_storage_path(copy);
            }
        }
        (*env)->ReleaseStringUTFChars(env, jpath, path);
    }
}

/*
 * NativeQeo functions
 */
JNIEXPORT void JNICALL Java_org_qeo_jni_NativeQeo_bgnsSuspend(JNIEnv *env, jclass cl)
{
    qeo_bgns_suspend();
}

JNIEXPORT void JNICALL Java_org_qeo_jni_NativeQeo_bgnsResume(JNIEnv *env, jclass cl)
{
    qeo_bgns_resume();
}

JNIEXPORT jstring JNICALL Java_org_qeo_jni_NativeQeo_nativeGetVersionString(JNIEnv *env, jclass cl)
{
    const char *version = qeo_version_string();

    return (*env)->NewStringUTF(env, version);
}

JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeQeo_nativeOpen(JNIEnv *env, jclass cl, jint id)
{
    qeo_factory_t *factory = NULL;
    qeofactory_userdata *userdata = calloc(1, sizeof(qeofactory_userdata));

    do {
        if (NULL == userdata) {
            qeo_log_e("Can't alloc userdata");
            break;
        }
        userdata->init_done = false;

        if ((sem_init(&userdata->semaphore, 0, 0)) != 0){
            qeo_log_e("Failed to init semaphore");
            free(userdata);
            break;
        }

        factory = qeocore_factory_new((qeo_identity_t *)id);
        if (qeocore_factory_set_user_data(factory, (uintptr_t)userdata) != QEO_OK){
            qeo_log_e("Factory set user data failed");
            qeocore_factory_close(factory);
            factory = NULL;
            free(userdata);
            break;
        }
    } while (0);

    return (jlong) factory;
}

JNIEXPORT void JNICALL Java_org_qeo_jni_NativeQeo_nativeSetLogLevel(JNIEnv *env, jclass cl, jint level)
{
    logLevel = level;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeFactory_nativeGetNumFactories(JNIEnv *env, jclass cl)
{
    return qeocore_get_num_factories();
}

/*
 * Native pre-destroy. Should be called before factories are closed and hence DDS core thread is still running.
 */ 
JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeQeo_nativePreDestroy(JNIEnv *env, jclass cl)
{
    qeo_log_i("Native Qeo pre destroy");
    int rc = QEO_OK;
    if (DDS_parameter_unset("TCP_SEC_SERVER") != DDS_RETCODE_OK) {
        rc = QEO_EFAIL;
    }
    return rc;
}

/*
 * Native post-destroy. Should be called after factories are closed.
 */ 
JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeQeo_nativePostDestroy(JNIEnv *env, jclass cl)
{
    qeo_log_i("Native Qeo post destroy");
    fflush(stdout);
    return QEO_OK;
}

static void on_qeocore_on_factory_init_done(qeo_factory_t *factory, bool success);

static const qeocore_factory_listener_t _listener = {
    .on_factory_init_done = on_qeocore_on_factory_init_done
};

/*
 *  Internal function to create an interposer_data_t structure containing necessary JNI parameters.
 */
static qeo_retcode_t create_interposer(JNIEnv *env,
                                       jobject cbObj,
                                       jstring cbName,
                                       const char *cbSign,
                                       interposer_data_t **idata)
{
    const char *methodName = NULL;
    qeo_retcode_t rc = QEO_OK;
    assert(idata != NULL);

    do {
        *idata = (interposer_data_t *)calloc(1, sizeof(interposer_data_t));
        if (NULL == *idata) {
            qeo_log_e("Failed to allocate memory for idata structure");
            rc = QEO_ENOMEM;
            break;
        }
        methodName = (*env)->GetStringUTFChars(env, cbName, NULL);
        if (NULL == methodName) {
            qeo_log_e("Failed to GetStringUTFChars from method");
            rc = QEO_EFAIL;
            break;
        }
        (*idata)->object = (*env)->NewGlobalRef(env, cbObj);
        if (NULL == (*idata)->object) {
            qeo_log_e("Failed to create NewGlobalRef to call-back object");
            rc = QEO_ENOMEM;
            break;
        }
        (*idata)->class = (*env)->GetObjectClass(env, (*idata)->object);
        if (NULL == (*idata)->class) {
            qeo_log_e("Failed to GetObjectClass from call-back object");
            rc = QEO_EFAIL;
            break;
        }
        (*idata)->method = (*env)->GetMethodID(env, (*idata)->class, methodName, cbSign);
        if (NULL != (*env)->ExceptionOccurred(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
        if (NULL == (*idata)->method) {
            qeo_log_e("Failed to GetMethodID from call-back method %s with signature %s", cbName, cbSign);
            rc = QEO_EFAIL;
            break;
        }
        log_printf("Successfully filled in JNI structure for call-back %s with signature %s", cbName, cbSign);
        rc = QEO_OK;
    } while (0);
    if (NULL != methodName) {
        (*env)->ReleaseStringUTFChars(env, cbName, methodName);
    }
    if (QEO_OK != rc) {
        if (NULL != *idata) {
            if (NULL != (*idata)->object) {
                (*env)->DeleteGlobalRef(env, (*idata)->object);
            }
            free(*idata);
        }
        *idata = NULL;
    }
    return rc;
}

static void free_interpose_data(JNIEnv *env, interposer_data_t *idata)
{
    if (NULL != idata) {
        if (NULL != idata->object) {
            (*env)->DeleteGlobalRef(env, idata->object);
        }
        free(idata);
    }
}

/*
 *  Internal function to create an interposer_data_t structure containing necessary JNI parameters.
 */
static qeo_retcode_t create_rw_interposer(JNIEnv *env,
                                          jobject cbObj,
                                          jboolean setDataAvailableCb,
                                          jboolean setPolicyUpdateCb,
                                          rw_interposer_data_t **idata)
{
    qeo_retcode_t rc = QEO_OK;
    assert(idata != NULL);

    do {
        *idata = (rw_interposer_data_t *)calloc(1, sizeof(rw_interposer_data_t));
        if (NULL == *idata) {
            qeo_log_e("Failed to allocate memory for idata structure");
            rc = QEO_ENOMEM;
            break;
        }
        (*idata)->object = (*env)->NewGlobalRef(env, cbObj);
        if (NULL == (*idata)->object) {
            qeo_log_e("Failed to create NewGlobalRef to call-back object");
            rc = QEO_ENOMEM;
            break;
        }
        (*idata)->class = (*env)->GetObjectClass(env, (*idata)->object);
        if (NULL == (*idata)->class) {
            qeo_log_e("Failed to GetObjectClass from call-back object");
            rc = QEO_EFAIL;
            break;
        }
        if (setDataAvailableCb) {
            (*idata)->onDataAvailable = (*env)->GetMethodID(env, (*idata)->class, "onDataAvailable", "(J)V");
            if (NULL != (*env)->ExceptionOccurred(env)) {
                (*env)->ExceptionDescribe(env);
                (*env)->ExceptionClear(env);
            }
            if (NULL == (*idata)->onDataAvailable) {
                qeo_log_e("Failed to GetMethodID for onDataAvailable");
                rc = QEO_EFAIL;
                break;
            }
        }
        if (setPolicyUpdateCb) {
            (*idata)->onPolicyUpdate = (*env)->GetMethodID(env, (*idata)->class, "onPolicyUpdate", "(J)Z");
            if (NULL != (*env)->ExceptionOccurred(env)) {
                (*env)->ExceptionDescribe(env);
                (*env)->ExceptionClear(env);
            }
            if (NULL == (*idata)->onPolicyUpdate) {
                qeo_log_e("Failed to GetMethodID for onPolicyUpdate");
                rc = QEO_EFAIL;
                break;
            }
            (*idata)->onPolicyUpdateDone = (*env)->GetMethodID(env, (*idata)->class, "onPolicyUpdateDone", "()V");
            if (NULL != (*env)->ExceptionOccurred(env)) {
                (*env)->ExceptionDescribe(env);
                (*env)->ExceptionClear(env);
            }
            if (NULL == (*idata)->onPolicyUpdateDone) {
                qeo_log_e("Failed to GetMethodID for onPolicyUpdateDone");
                rc = QEO_EFAIL;
                break;
            }
        }
        log_printf("Successfully filled in JNI structure for reader/writer call-backs");
        rc = QEO_OK;
    } while (0);
    if (QEO_OK != rc) {
        if (NULL != *idata) {
            if (NULL != (*idata)->object) {
                (*env)->DeleteGlobalRef(env, (*idata)->object);
            }
            free(*idata);
        }
        *idata = NULL;
    }
    return rc;
}

static void free_rw_interpose_data(JNIEnv *env, rw_interposer_data_t *idata)
{
    if (NULL != idata) {
        if (NULL != idata->object) {
            (*env)->DeleteGlobalRef(env, idata->object);
        }
        free(idata);
    }
}

/* Implementation more or less copied from qeo-c/src/factory.c to cope with two temporary limitations:
 *  - Multirealm is already in qeo-c-core but we don't expose it to the applications (now just take the first).
 *  - qeocore_factory_init has become an asynchronous function but towards the applications we currently block.
 *      This will probably change in the future for GUI applications.
 */
JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeQeo_nativeInit(JNIEnv *env, jclass class, jobject cbObj, jlong jfactory, jstring otpCb, jstring updateCb, jstring initDoneCb)
{
    log_printf("You are now in NativeQeo.nativeInit");

    if (NULL == cbObj) {
        qeo_log_e("Illegal NULL pointer provided for parameter cbObj in NativeQeo.nativeInit");
        return (jint)QEO_EINVAL;
    }
    if (NULL == initDoneCb) {
        qeo_log_e("Illegal NULL pointer provided for parameter initDoneCb in NativeQeo.nativeInit");
        return (jint)QEO_EINVAL;
    }

    qeo_factory_t *factory = (qeo_factory_t *)jfactory;
    qeofactory_userdata *userdata = NULL;
    int s;
    interposer_data_t* idata_init_done_cb = NULL;
    interposer_data_t* idata_otp_cb = NULL;
    interposer_data_t* idata_update_cb = NULL;

    qeo_retcode_t rc = QEO_OK;

    qeocore_factory_get_user_data((qeo_factory_t *)factory, (uintptr_t *)&userdata);
    if (userdata == NULL) {
        qeo_log_e("Userdata is NULL, you probably did not call nativeOpen()");
        return (jint)QEO_EFAIL;
    }

    do {
        if (otpCb != NULL && updateCb != NULL) {
            // OTP Call-back registration
            rc = create_interposer(env, cbObj, otpCb, "()V", &idata_otp_cb);
            if (QEO_OK != rc) {
                break;
            }
            // Status Update Call-back registration
            rc = create_interposer(env, cbObj, updateCb, "(Ljava/lang/String;Ljava/lang/String;)V", &idata_update_cb);
            if (QEO_OK != rc) {
                break;
            }

            // For Android, we need to register the OTP and StatusUpdate call-backs towards the platform API.
            bool retval = platform_jni_set_interposer(_jvm, idata_otp_cb, idata_update_cb);
            if (retval == false) {
                qeo_log_e("Failed to register OTP and StatusUpdate call-back @ platform jni API");
                rc = QEO_EFAIL;
                break;
            }
        }

        // InitDone Call-back registration
        rc = create_interposer(env, cbObj, initDoneCb, "(Z)V", &idata_init_done_cb);
        if (QEO_OK != rc) {
            break;
        }

        // Start initializing Qeo
        qeo_retcode_t res_factory_init = qeocore_factory_init(factory, &_listener);
        if (res_factory_init != QEO_OK ) {
            qeo_log_e("Factory init failed");
            rc = res_factory_init;
            break;
        }

        /* wait until the mutex is unlocked through the callback ! */
        while ((s = sem_wait(&userdata->semaphore)) == -1) {
            if (errno == EINTR) {
                rc = QEO_OK;
            } else {
                qeo_log_e("sem_wait return with unexpected error: %s", strerror(errno));
                rc = QEO_EFAIL;
                break;
            }
        }
        rc = QEO_OK;

    } while (0);

    sem_destroy(&userdata->semaphore);

    log_printf("Going to call java InitDone call-back method");
    if (idata_init_done_cb != NULL) {
        java_init_done_cb_interposer((uintptr_t)idata_init_done_cb, userdata->success);
    }

    if (rc != QEO_OK || userdata->interrupted) {
        qeocore_factory_set_user_data(factory, (uintptr_t)NULL);
        qeocore_factory_close(factory);
        factory = NULL;
        free(userdata);
    }
            
    if (otpCb != NULL && updateCb != NULL) {
        platform_jni_clear_interposer();
    }

    if (idata_otp_cb != NULL) {
        free(idata_otp_cb);
    }
    if (idata_update_cb != NULL) {
        free(idata_update_cb);
    }
    if (idata_init_done_cb != NULL) {
        free(idata_init_done_cb);
    }

    return (jint)rc;
}

JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeFactory_nativeGetRealmId(JNIEnv *env, jclass cl, jlong factory)
{
    return qeocore_factory_get_realm_id((qeo_factory_t *)factory);
}

JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeFactory_nativeGetUserId(JNIEnv *env, jclass cl, jlong factory)
{
    return qeocore_factory_get_user_id((qeo_factory_t *)factory);
}

JNIEXPORT jstring JNICALL Java_org_qeo_jni_NativeFactory_nativeGetRealmUrl(JNIEnv *env, jclass cl, jlong factory)
{
    jstring urlJ = NULL;
    const char * url;

    url = qeocore_factory_get_realm_url((qeo_factory_t *)factory);
    if (url != NULL) {
        urlJ = (*env)->NewStringUTF(env, url);
    }
    return urlJ;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeQeo_nativeRefreshPolicy(JNIEnv *env, jclass cl, jlong factory)
{
    return (jint)qeocore_factory_refresh_policy((qeo_factory_t *)factory);
}

JNIEXPORT void JNICALL Java_org_qeo_jni_NativeQeo_nativeClose(JNIEnv *env, jclass cl, jlong factory)
{
    qeofactory_userdata *userdata = NULL;

    qeocore_factory_get_user_data((qeo_factory_t *)factory, (uintptr_t *)&userdata);
    if (userdata != NULL) {
        free(userdata);
    }
    qeocore_factory_close((qeo_factory_t *)factory);
}

JNIEXPORT void JNICALL Java_org_qeo_jni_NativeQeo_nativeInterrupt(JNIEnv *env, jclass cl, jlong factory)
{
    qeofactory_userdata *userdata = NULL;

    qeocore_factory_get_user_data((qeo_factory_t *)factory, (uintptr_t *)&userdata);
    if (userdata != NULL && !userdata->init_done) {
        userdata->interrupted = true;
        log_printf("init interrupted");
        sem_post(&userdata->semaphore);
    }
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeQeo_nativeSetUdpMode(JNIEnv *env,
                                                                   jclass cl,
                                                                   jboolean enabled)
{
    int rc = QEO_EFAIL;
    if (DDS_RETCODE_OK == DDS_parameter_set("UDP_MODE", (enabled ? "enabled" : "disabled"))) {
        rc = QEO_OK;
    }

    return (jint)rc;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeQeo_nativeSetDdsParameter(JNIEnv *env,
                                                                   jclass cl,
                                                                   jstring key,
                                                                   jstring value)
{
#ifdef DEBUG
    //only allow setting of DDS parameters in DEBUG build
    const char* keyStr = (*env)->GetStringUTFChars(env, key, 0);
    const char* valueStr = (*env)->GetStringUTFChars(env, value, 0);
    qeo_log_i("Setting DDS parameter %s=%s", keyStr, valueStr);
    int rc = QEO_EFAIL;
    if (DDS_RETCODE_OK == DDS_parameter_set(keyStr, valueStr)) {
        rc = QEO_OK;
    }

    (*env)->ReleaseStringUTFChars(env, key, keyStr);
    (*env)->ReleaseStringUTFChars(env, value, valueStr);
    return (jint)rc;
#else
    return (jint)QEO_EFAIL;
#endif
}

//set QEO parameter from JNI
JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeQeo_nativeSetQeoParameter(JNIEnv *env,
                                                                   jclass cl,
                                                                   jstring key,
                                                                   jstring value)
{
    if (NULL == key || NULL ==value) {
        qeo_log_e("Illegal NULL pointer provided");
        return (jint)QEO_EINVAL;
    }
    const char* keyStr = (*env)->GetStringUTFChars(env, key, 0);
    if (NULL == keyStr) {
        qeo_log_e("Failed to GetStringUTFChars from keyStr");
        return (jint)QEO_EFAIL;
    }
    const char* valueStr = (*env)->GetStringUTFChars(env, value, 0);
    if (NULL == valueStr) {
        qeo_log_e("Failed to GetStringUTFChars from keyStr");
        return (jint)QEO_EFAIL;
    }
    qeo_log_i("Setting Qeo parameter %s=%s", keyStr, valueStr);
    int rc = QEO_EFAIL;
    rc = qeocore_parameter_set(keyStr, valueStr);

    (*env)->ReleaseStringUTFChars(env, key, keyStr);
    (*env)->ReleaseStringUTFChars(env, value, valueStr);
    return (jint)rc;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeQeo_nativeConfigTcpServer(JNIEnv *env,
                                                                        jclass cl,
                                                                        jlong factory,
                                                                        jstring tcp_server)
{
    if (NULL == tcp_server) {
        qeo_log_e("Illegal NULL pointer provided for parameter interfaces in NativeQeo.nativeConfigTcpServer");
        return (jint)QEO_EINVAL;
    }

    const char* tcpServerStr = (*env)->GetStringUTFChars(env, tcp_server, 0);
    if (NULL == tcpServerStr) {
        qeo_log_e("Failed to GetStringUTFChars from tcpServerStr");
        return (jint)QEO_EFAIL;
    }

    qeo_retcode_t rc = qeocore_factory_set_tcp_server((qeo_factory_t *)factory, tcpServerStr);
    if (NULL != tcpServerStr) {
        (*env)->ReleaseStringUTFChars(env, tcp_server, tcpServerStr);
    }
    return (jint)rc;
}

/*
 * NativeSecurity functions
 */
JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeSecurity_nativeNoSecurity(JNIEnv *env,
                                                                        jclass cl)
{
    return (jint) qeocore_parameter_set("DDS_NO_SECURITY", "1");
}

JNIEXPORT jbyteArray JNICALL Java_org_qeo_jni_NativeSecurity_nativeEncryptOtc(JNIEnv *env,
                                                                             jclass cl,
                                                                             jstring otc,
                                                                             jstring key)
{
    jbyteArray encryptedOtc = NULL;
    unsigned char *encrypted = NULL;
    int len = 0;
    const char* otcStr = (*env)->GetStringUTFChars(env, otc, 0);
    const char* keyStr = (*env)->GetStringUTFChars(env, key, 0);

    do {
        if (NULL == otcStr) {
            qeo_log_e("Failed to GetStringUTFChars from otcStr");
            break;
        }
        qeo_log_d("Encrypting otc %s", otcStr);
        if (NULL == keyStr) {
            qeo_log_e("Failed to GetStringUTFChars from keyStr");
            break;
        }

        len = qeocore_remote_registration_encrypt_otc(otcStr, keyStr, &encrypted);
        if (len == -1) {
            qeo_log_w("Failed to encrypt otc");
            break;
        }

        encryptedOtc = (*env)->NewByteArray(env, len);
        if (NULL == encryptedOtc) {
            qeo_log_e("Failed to create ByteArray value in NativeSecurity.nativeEncryptOtc");
            break;
        }
        if (len > 0) {
            (*env)->SetByteArrayRegion(env, encryptedOtc, 0, len, (jbyte *)encrypted);
            if (NULL != (*env)->ExceptionOccurred(env)) {
                qeo_log_e("Unexpected exception occurred in NativeSecurity.nativeEncryptOtc SetByteArrayRegion");
                (*env)->ExceptionDescribe(env);
                (*env)->ExceptionClear(env);
            }
        }
    } while (0);

    if (NULL != otcStr) {
        (*env)->ReleaseStringUTFChars(env, otc, otcStr);
    }
    if (NULL != keyStr) {
        (*env)->ReleaseStringUTFChars(env, key, keyStr);
    }
    qeocore_remote_registration_free_encrypted_otc(encrypted);

    return encryptedOtc;
}

JNIEXPORT jstring JNICALL Java_org_qeo_jni_NativeSecurity_nativeDecryptOtc(JNIEnv *env,
                                                                             jclass cl,
                                                                             jbyteArray encryptedOtcArray)
{
    int len = (*env)->GetArrayLength(env, encryptedOtcArray);
    unsigned char * encryptedOtc = malloc(sizeof(unsigned char) * len);
    char * otc;
    jstring otcString = NULL;

    do {
        if (len <= 0) {
            qeo_log_e("invalid OTC array");
            break;
        }
        
        (*env)->GetByteArrayRegion(env, encryptedOtcArray, 0, len, encryptedOtc);
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in NativeSecurity.nativeDecryptOtc GetByteArrayRegion");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }

        otc = qeocore_remote_registration_decrypt_otc(encryptedOtc, len);
        if (NULL == otc) {
            qeo_log_w("Failed to decrypt otc");
            break;
        }
        otcString = (*env)->NewStringUTF(env, otc);
        free(otc);

        if (NULL == otcString) {
            qeo_log_e("Failed to create otcString");
            break;
        }
    } while (0);

    free(encryptedOtc);

    return otcString;
}

JNIEXPORT jstring JNICALL Java_org_qeo_jni_NativeSecurity_nativeGetPublicKey(JNIEnv *env,
                                                                             jclass cl)
{
    char *pem = NULL;
    jstring pem_string = NULL;

    do {
        pem = qeocore_remote_registration_get_pub_key_pem();
        if (NULL == pem) {
            qeo_log_w("Failed to get public key");
            break;
        }

        pem_string = (*env)->NewStringUTF(env, pem);
        free(pem);

    } while (0);

    return pem_string;
}

/*
 * NativeData functions
 */
JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeData_nativeNewWriterData(JNIEnv *env, jclass cl, jlong writer)
{
    return (jlong)qeocore_writer_data_new((const qeocore_writer_t *)writer);
}

JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeData_nativeNewReaderData(JNIEnv *env, jclass cl, jlong reader)
{
    return (jlong)qeocore_reader_data_new((const qeocore_reader_t *)reader);
}

JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeData_nativeGetMemberData(JNIEnv *env, jclass cl, jlong data, jint id)
{
    qeocore_data_t *member = NULL;
    qeo_retcode_t ret = qeocore_data_get_member((qeocore_data_t *)data, (qeocore_member_id_t)id, &member);
    return (jlong)member;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeData_nativeSetMemberData(JNIEnv *env,
                                                                       jclass cl,
                                                                       jlong data,
                                                                       jint id,
                                                                       jlong member)
{
    //First cast member to pointer, only then pass it along. 
    //This is needed for 32 bit systems to avoid endianess issues.
    void* value = (void *) member;
    return (jint)qeocore_data_set_member((qeocore_data_t *)data, (qeocore_member_id_t)id, &value);
}

JNIEXPORT void JNICALL Java_org_qeo_jni_NativeData_nativeDeleteData(JNIEnv *env, jclass cl, jlong data)
{
    qeocore_data_free((qeocore_data_t *)data);
}

JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeData_nativeGetSequence(JNIEnv *env, jclass cl, jlong data, jint size)
{
    qeo_sequence_t *sequence = calloc(1, sizeof(qeo_sequence_t));
    qeo_retcode_t ret = QEO_OK;

    if (size == 0) {
        ret = qeocore_data_sequence_get((qeocore_data_t *)data, sequence, 0, QEOCORE_SIZE_UNLIMITED);
    }
    else {
        ret = qeocore_data_sequence_new((qeocore_data_t *)data, sequence, size);
    }
    if (ret != QEO_OK) {
        free(sequence);
        sequence = NULL;
    }
    return (jlong)sequence;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeData_nativeGetSequenceSize(JNIEnv *env, jclass cl, jlong sequence)
{
    return (jint)DDS_SEQ_LENGTH(*((qeo_sequence_t *)sequence));
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeData_nativeSetSequence(JNIEnv *env, jclass cl, jlong data, jlong sequence)
{
    return (jint)qeocore_data_sequence_set((qeocore_data_t *)data, (qeo_sequence_t *)sequence, 0);
}

JNIEXPORT void JNICALL Java_org_qeo_jni_NativeData_nativeDeleteSequence(JNIEnv *env, jclass cl, jlong data, jlong sequence)
{
    qeocore_data_sequence_free((qeocore_data_t *)data, (qeo_sequence_t *)sequence);
    free((qeo_sequence_t *)sequence);
}

JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeData_nativeGetSequenceElement(JNIEnv *env, jclass cl, jlong sequence, jint index)
{
    return (jlong)((qeocore_data_t **) DDS_SEQ_DATA(*((qeo_sequence_t *) sequence)))[index];
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeData_nativeGetStatus(JNIEnv *env, jclass cl, jlong data)
{
    return (jint)qeocore_data_get_status((const qeocore_data_t *)data);
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeData_nativeGetInstanceHandle(JNIEnv *env, jclass cl, jlong data)
{
    return (jint)qeocore_data_get_instance_handle((const qeocore_data_t *)data);
}

/*
 * NativeType functions
 */
JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeType_nativeCreateType(JNIEnv *env, jclass cl, jstring s, jintArray result)
{
    qeo_retcode_t rc = QEO_OK;
    qeocore_type_t* type = NULL;
    const char* str = NULL;

    do {
        if (NULL == result) {
            rc = QEO_EINVAL;
            qeo_log_e("Illegal NULL pointer provided for parameter result in NativeType.nativeCreateType");
            return (jlong)NULL;
        }

        if (NULL == s) {
            qeo_log_e("Illegal NULL pointer provided for parameter s in NativeType.nativeCreateType");
            rc = QEO_EINVAL;
            break;
        }

        str = (*env)->GetStringUTFChars(env, s, 0);
        if (NULL == str) {
            qeo_log_e("Failed to GetStringUTFChars from s");
            rc = QEO_EFAIL;
            break;
        }

        type = qeocore_type_struct_new(str);
        if (NULL == type) {
            qeo_log_e("Failed to create new structure for type %s", str);
            rc = QEO_EFAIL;
            break;
        }

        log_printf("Successfully created type with name = %s", str);
    } while (0);

    (*env)->SetIntArrayRegion(env, result, 0, 1, (jint *)&rc);
    if (NULL != (*env)->ExceptionOccurred(env)) {
        qeo_log_e("Unexpected exception occurred in NativeType.nativeCreateType SetIntArrayRegion");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }

    if (NULL != str) {
        (*env)->ReleaseStringUTFChars(env, s, str);
    }

    return (jlong)type;
}

JNIEXPORT void JNICALL Java_org_qeo_jni_NativeType_nativeDeleteType(JNIEnv *env, jclass cl, jlong type)
{
    qeocore_type_free((qeocore_type_t *)type);
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeType_nativeAddElement(JNIEnv *env,
                                                                    jclass cl,
                                                                    jlong container,
                                                                    jlong member,
                                                                    jstring s,
                                                                    jintArray id,
                                                                    jboolean key)
{
    qeo_retcode_t rc = QEO_OK;
    jboolean isCopy;
    qeocore_member_id_t member_id;
    const char *name = NULL;

    if (NULL == s) {
        qeo_log_e("Illegal NULL pointer provided for parameter s in NativeType.nativeAddElement");
        return (jint)QEO_EINVAL;
    }

    if (NULL == id) {
        qeo_log_e("Illegal NULL pointer provided for parameter id in NativeType.nativeAddElement");
        return (jint)QEO_EINVAL;
    }

    name = (*env)->GetStringUTFChars(env, s, 0);
    if (NULL == name) {
        qeo_log_e("Failed to GetStringUTFChars from s");
        return (jint)QEO_EFAIL;
    }

    (*env)->GetIntArrayRegion(env, id, 0, 1, (jint*)&member_id);
    if (NULL != (*env)->ExceptionOccurred(env)) {
        qeo_log_e("Unexpected exception occurred in NativeType.nativeDeleteType GetIntArrayRegion");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }

    rc = qeocore_type_struct_add((qeocore_type_t *)container, (qeocore_type_t *)member, name, &member_id,
                                                (char)key == 1 ? QEOCORE_FLAG_KEY : QEOCORE_FLAG_NONE);

    (*env)->SetIntArrayRegion(env, id, 0, 1, (jint *)&member_id);
    if (NULL != (*env)->ExceptionOccurred(env)) {
        qeo_log_e("Unexpected exception occurred in NativeType.nativeDeleteType SetIntArrayRegion");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }

    if (NULL != name) {
        (*env)->ReleaseStringUTFChars(env, s, name);
    }
    return (jint)rc;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeType_nativeRegisterType(JNIEnv *env,
                                                                      jclass cl,
                                                                      jlong factory,
                                                                      jlong type,
                                                                      jstring s)
{
    qeo_retcode_t rc = QEO_OK;
    const char *name = NULL;
    if (NULL == s) {
        qeo_log_e("Illegal NULL pointer provided for parameter s in NativeType.nativeRegisterType");
        return (jint)QEO_EINVAL;
    }

    name = (*env)->GetStringUTFChars(env, s, 0);
    if (NULL == name) {
        qeo_log_e("Failed to GetStringUTFChars from s");
        return (jint)QEO_EFAIL;
    }

    rc = qeocore_type_register((qeo_factory_t *) factory, (qeocore_type_t *)type, name);
    if (NULL != name) {
        (*env)->ReleaseStringUTFChars(env, s, name);
    }
    return (jint)rc;
}

/*
 * NativeTypeSupport functions
 */
JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeNewString(JNIEnv *env, jclass cl, jint size)
{
    return (jlong)qeocore_type_string_new((size_t)size);
}

JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeNewByte(JNIEnv *env, jclass cl)
{
    return (jlong)qeocore_type_primitive_new(QEOCORE_TYPECODE_INT8);
}

JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeNewShort(JNIEnv *env, jclass cl)
{
    return (jlong)qeocore_type_primitive_new(QEOCORE_TYPECODE_INT16);
}

JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeNewInteger(JNIEnv *env, jclass cl)
{
    return (jlong)qeocore_type_primitive_new(QEOCORE_TYPECODE_INT32);
}

JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeNewLong(JNIEnv *env, jclass cl)
{
    return (jlong)qeocore_type_primitive_new(QEOCORE_TYPECODE_INT64);
}

JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeNewFloat(JNIEnv *env, jclass cl)
{
    return (jlong)qeocore_type_primitive_new(QEOCORE_TYPECODE_FLOAT32);
}

JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeNewBoolean(JNIEnv *env, jclass cl)
{
    return (jlong)qeocore_type_primitive_new(QEOCORE_TYPECODE_BOOLEAN);
}

JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeNewSequence(JNIEnv *env,
                                                                             jclass cl,
                                                                             jlong member)
{
    return (jlong)qeocore_type_sequence_new((qeocore_type_t *)member);
}

JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeNewEnumeration(JNIEnv *env,
                                                                                jclass cl,
                                                                                jstring name,
                                                                                jobjectArray constants)
{
    qeocore_type_t *enum_type = NULL;
    qeocore_enum_constants_t enum_consts = DDS_SEQ_INITIALIZER(qeocore_enum_constant_t);
    jsize len = (*env)->GetArrayLength(env, constants);

    if (DDS_RETCODE_OK == dds_seq_require(&enum_consts, len)) {
        char *enum_name = (char*)(*env)->GetStringUTFChars(env, name, 0);

        if (NULL != enum_name) {
            int i;

            for (i = 0; i < len; i++) {
                jstring stringValue = (jstring) (*env)->GetObjectArrayElement(env, constants, i);
                DDS_SEQ_ITEM(enum_consts, i).name = (char*)(*env)->GetStringUTFChars(env, stringValue, 0);
            }
            enum_type = qeocore_type_enum_new(enum_name, &enum_consts);
            /* clean up sequence and Java strings */
            (*env)->ReleaseStringUTFChars(env, name, enum_name);
            for (i = 0; i < len; i++) {
                if (NULL != DDS_SEQ_ITEM(enum_consts, i).name) {
                    jstring stringValue = (jstring) (*env)->GetObjectArrayElement(env, constants, i);
                    (*env)->ReleaseStringUTFChars(env, stringValue, DDS_SEQ_ITEM(enum_consts, i).name);
                }
            }
        }
        dds_seq_cleanup(&enum_consts);
    }
    return (jlong)enum_type;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeSetString(JNIEnv *env,
                                                                          jclass cl,
                                                                          jlong data,
                                                                          jint id,
                                                                          jstring value)
{
    qeo_retcode_t rc = QEO_OK;
    const char* str = NULL;
    if (NULL == value) {
        qeo_log_e("Illegal NULL pointer provided for parameter value in NativeTypeSupport.nativeSetString");
        return (jint)QEO_EINVAL;
    }

    str = (*env)->GetStringUTFChars(env, value, 0);
    if (NULL == str) {
        qeo_log_e("Failed to GetStringUTFChars from value");
        return (jint)QEO_EFAIL;
    }

    rc = qeocore_data_set_member((qeocore_data_t *)data, (qeocore_member_id_t)id, &str);
    if (NULL != str) {
        (*env)->ReleaseStringUTFChars(env, value, str);
    }
    return (jint)rc;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeSetByte(JNIEnv *env,
                                                                        jclass cl,
                                                                        jlong data,
                                                                        jint id,
                                                                        jbyte value)
{
    return (jint)qeocore_data_set_member((qeocore_data_t *)data, (qeocore_member_id_t)id, &value);
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeSetShort(JNIEnv *env,
                                                                         jclass cl,
                                                                         jlong data,
                                                                         jint id,
                                                                         jshort value)
{
    return (jint)qeocore_data_set_member((qeocore_data_t *)data, (qeocore_member_id_t)id, &value);
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeSetInteger(JNIEnv *env,
                                                                           jclass cl,
                                                                           jlong data,
                                                                           jint id,
                                                                           jint value)
{
    return (jint)qeocore_data_set_member((qeocore_data_t *)data, (qeocore_member_id_t)id, &value);
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeSetLong(JNIEnv *env,
                                                                        jclass cl,
                                                                        jlong data,
                                                                        jint id,
                                                                        jlong value)
{
    return (jint)qeocore_data_set_member((qeocore_data_t *)data, (qeocore_member_id_t)id, &value);
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeSetFloat(JNIEnv *env,
                                                                         jclass cl,
                                                                         jlong data,
                                                                         jint id,
                                                                         jfloat value)
{
    return (jint)qeocore_data_set_member((qeocore_data_t *)data, (qeocore_member_id_t)id, &value);
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeSetBoolean(JNIEnv *env,
                                                                           jclass cl,
                                                                           jlong data,
                                                                           jint id,
                                                                           jboolean value)
{
    return (jint)qeocore_data_set_member((qeocore_data_t *)data, (qeocore_member_id_t)id, &value);
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeSetStringSequence(JNIEnv *env,
                                                                               jclass cl,
                                                                               jlong data,
                                                                               jobjectArray value,
                                                                               jint length)
{
    if (value == NULL) {
        qeo_log_e("Illegal NULL pointer provided for parameter value in NativeTypeSupport.nativeSetStringSequence");
        return (jint)QEO_EINVAL;
    }

    qeo_sequence_t sequence;
    qeo_retcode_t res = QEO_OK;

    do {
        DDS_SEQ_LENGTH(sequence) = length;
        res = qeocore_data_sequence_new((qeocore_data_t *)data, &sequence, DDS_SEQ_LENGTH(sequence));
        if (QEO_OK != res) {
            break;
        }
        for (int i = 0; i < length; i++) {
            jstring stringValue = (jstring) (*env)->GetObjectArrayElement(env, value, i);
            ((char**)DDS_SEQ_DATA(sequence))[i] = (char *) (*env)->GetStringUTFChars(env, stringValue, 0);
        }
        res = qeocore_data_sequence_set((qeocore_data_t *)data, &sequence, 0);
        for (int i = 0; i < length; i++) {
            jstring stringValue = (jstring) (*env)->GetObjectArrayElement(env, value, i);
            (*env)->ReleaseStringUTFChars(env, stringValue, ((char**)DDS_SEQ_DATA(sequence))[i]);
        }
        free(DDS_SEQ_DATA(sequence));
    } while (0);

    return (jint)res;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeSetByteSequence(JNIEnv *env,
                                                                             jclass cl,
                                                                             jlong data,
                                                                             jbyteArray value,
                                                                             jint length)
{
    qeo_sequence_t sequence;
    qeo_retcode_t rc = QEO_OK;

    if (value == NULL) {
        qeo_log_e("Illegal NULL pointer provided for parameter value in NativeTypeSupport.nativeSetByteSequence");
        return (jint)QEO_EINVAL;
    }

    do {
        DDS_SEQ_INIT(sequence);
        DDS_SEQ_MAXIMUM(sequence) = DDS_SEQ_LENGTH(sequence) = length;
        DDS_SEQ_DATA(sequence) = (*env)->GetByteArrayElements(env, value, NULL);
        if (NULL == DDS_SEQ_DATA(sequence)) {
            qeo_log_e("Failed to GetByteArrayElements from jbyteArray value in NativeTypeSupport.nativeSetByteSequence");
            rc = QEO_EFAIL;
            break;
        }

        rc = qeocore_data_sequence_set((qeocore_data_t *)data, &sequence, 0);
        //free the buffer. Don't copy possible changes to the sequence back to java
        (*env)->ReleaseByteArrayElements(env, value, DDS_SEQ_DATA(sequence), JNI_ABORT);
    } while (0);

    return (jint)rc;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeSetShortSequence(JNIEnv *env,
                                                                              jclass cl,
                                                                              jlong data,
                                                                              jshortArray value,
                                                                              jint length)
{
    qeo_sequence_t sequence;
    qeo_retcode_t rc = QEO_OK;

    if (value == NULL) {
        qeo_log_e("Illegal NULL pointer provided for parameter value in NativeTypeSupport.nativeSetShortSequence");
        return (jint)QEO_EINVAL;
    }

    do {
        DDS_SEQ_INIT(sequence);
        DDS_SEQ_MAXIMUM(sequence) = DDS_SEQ_LENGTH(sequence) = length;
        DDS_SEQ_DATA(sequence) = (*env)->GetShortArrayElements(env, value, NULL);
        if (NULL == DDS_SEQ_DATA(sequence)) {
            qeo_log_e("Failed to GetShortArrayElements from jshortArray value in NativeTypeSupport.nativeSetShortSequence");
            rc = QEO_EFAIL;
            break;
        }

        rc = qeocore_data_sequence_set((qeocore_data_t *)data, &sequence, 0);
        //free the buffer. Don't copy possible changes to the sequence back to java
        (*env)->ReleaseShortArrayElements(env, value, DDS_SEQ_DATA(sequence), JNI_ABORT);
    } while (0);

    return (jint)rc;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeSetIntegerSequence(JNIEnv *env,
                                                                                jclass cl,
                                                                                jlong data,
                                                                                jintArray value,
                                                                                jint length)
{
    qeo_sequence_t sequence;
    qeo_retcode_t rc = QEO_OK;

    if (value == NULL) {
        qeo_log_e("Illegal NULL pointer provided for parameter value in NativeTypeSupport.nativeSetIntegerSequence");
        return (jint)QEO_EINVAL;
    }

    do {
        DDS_SEQ_INIT(sequence);
        DDS_SEQ_MAXIMUM(sequence) = DDS_SEQ_LENGTH(sequence) = length;
        DDS_SEQ_DATA(sequence) = (*env)->GetIntArrayElements(env, value, NULL);
        if (NULL == DDS_SEQ_DATA(sequence)) {
            qeo_log_e("Failed to GetIntArrayElements from jintArray value in NativeTypeSupport.nativeSetIntegerSequence");
            rc = QEO_EFAIL;
            break;
        }

        rc = qeocore_data_sequence_set((qeocore_data_t *)data, &sequence, 0);
        //free the buffer. Don't copy possible changes to the sequence back to java
        (*env)->ReleaseIntArrayElements(env, value, DDS_SEQ_DATA(sequence), JNI_ABORT);
    } while (0);

    return (jint)rc;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeSetLongSequence(JNIEnv *env,
                                                                             jclass cl,
                                                                             jlong data,
                                                                             jlongArray value,
                                                                             jint length)
{
    qeo_sequence_t sequence;
    qeo_retcode_t rc = QEO_OK;
    if (value == NULL) {
        qeo_log_e("Illegal NULL pointer provided for parameter value in NativeTypeSupport.nativeSetLongSequence");
        return (jint)QEO_EINVAL;
    }

    do {
        DDS_SEQ_INIT(sequence);
        DDS_SEQ_MAXIMUM(sequence) = DDS_SEQ_LENGTH(sequence) = length;
        DDS_SEQ_DATA(sequence) = (*env)->GetLongArrayElements(env, value, NULL);
        if (NULL == DDS_SEQ_DATA(sequence)) {
            qeo_log_e("Failed to GetLongArrayElements from jlongArray value in NativeTypeSupport.nativeSetLongSequence");
            rc = QEO_EFAIL;
            break;
        }

        rc = qeocore_data_sequence_set((qeocore_data_t *)data, &sequence, 0);
        //free the buffer. Don't copy possible changes to the sequence back to java
        (*env)->ReleaseLongArrayElements(env, value, DDS_SEQ_DATA(sequence), JNI_ABORT);
    } while (0);

    return (jint)rc;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeSetFloatSequence(JNIEnv *env,
                                                                              jclass cl,
                                                                              jlong data,
                                                                              jfloatArray value,
                                                                              jint length)
{
    qeo_sequence_t sequence;
    qeo_retcode_t rc = QEO_OK;
    if (value == NULL) {
        qeo_log_e("Illegal NULL pointer provided for parameter value in NativeTypeSupport.nativeSetFloatSequence");
        return (jint)QEO_EINVAL;
    }

    do {
        DDS_SEQ_INIT(sequence);
        DDS_SEQ_MAXIMUM(sequence) = DDS_SEQ_LENGTH(sequence) = length;
        DDS_SEQ_DATA(sequence) = (*env)->GetFloatArrayElements(env, value, NULL);
        if (NULL == DDS_SEQ_DATA(sequence)) {
            qeo_log_e("Failed to GetFloatArrayElements from jfloatArray value in NativeTypeSupport.nativeSetFloatSequence");
            rc = QEO_EFAIL;
            break;
        }

        rc = qeocore_data_sequence_set((qeocore_data_t *)data, &sequence, 0);
        //free the buffer. Don't copy possible changes to the sequence back to java
        (*env)->ReleaseFloatArrayElements(env, value, DDS_SEQ_DATA(sequence), JNI_ABORT);
    } while (0);

    return (jint)rc;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeSetBooleanSequence(JNIEnv *env,
                                                                                jclass cl,
                                                                                jlong data,
                                                                                jbooleanArray value,
                                                                                jint length)
{
    qeo_sequence_t sequence;
    qeo_retcode_t rc = QEO_OK;
    if (value == NULL) {
        qeo_log_e("Illegal NULL pointer provided for parameter value in NativeTypeSupport.nativeSetBooleanSequence");
        return (jint)QEO_EINVAL;
    }

    do {
        DDS_SEQ_INIT(sequence);
        DDS_SEQ_MAXIMUM(sequence) = DDS_SEQ_LENGTH(sequence) = length;
        DDS_SEQ_DATA(sequence) = (*env)->GetBooleanArrayElements(env, value, NULL);
        if (NULL == DDS_SEQ_DATA(sequence)) {
            qeo_log_e("Failed to GetBooleanArrayElements from jbooleanArray value in NativeTypeSupport.nativeSetBooleanSequence");
            rc = QEO_EFAIL;
            break;
        }

        rc = qeocore_data_sequence_set((qeocore_data_t *)data, &sequence, 0);
        //free the buffer. Don't copy possible changes to the sequence back to java
        (*env)->ReleaseBooleanArrayElements(env, value, DDS_SEQ_DATA(sequence), JNI_ABORT);
    } while (0);

    return (jint)rc;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeGetString(JNIEnv *env,
                                                                          jclass cl,
                                                                          jlong data,
                                                                          jint id,
                                                                          jobjectArray value)
{
    if (value == NULL) {
        qeo_log_e("Illegal NULL pointer provided for parameter value in NativeTypeSupport.nativeGetString");
        return (jint)QEO_EINVAL;
    }

    char *_value = NULL;
    jstring __value = NULL;
    qeo_retcode_t rc = QEO_OK;

    do {
        rc = qeocore_data_get_member((qeocore_data_t *)data, (qeocore_member_id_t)id, &_value);
        if (QEO_OK != rc) {
            qeo_log_e("Failed to get data member");
            break;
        }

        __value = (*env)->NewStringUTF(env, _value);
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetString NewStringUTF");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
        if (NULL == __value) {
            qeo_log_e("Failed to NewStringUTF from _value");
            rc = QEO_EFAIL;
            break;
        }

        (*env)->SetObjectArrayElement(env, value, 0, __value);
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetString SetObjectArrayElement");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    } while (0);

    return (jint)rc;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeGetByte(JNIEnv *env,
                                                                        jclass cl,
                                                                        jlong data,
                                                                        jint id,
                                                                        jbyteArray value)
{
    if (value == NULL) {
        qeo_log_e("Illegal NULL pointer provided for parameter value in NativeTypeSupport.nativeGetByte");
        return (jint)QEO_EINVAL;
    }

    unsigned char _value;
    qeo_retcode_t rc = QEO_OK;

    rc = qeocore_data_get_member((qeocore_data_t *)data, (qeocore_member_id_t)id, &_value);
    if (QEO_OK != rc) {
        qeo_log_e("Failed to get data member");
        return (jint)rc;
    }

    (*env)->SetByteArrayRegion(env, value, 0, 1, (jbyte *)&_value);
    if (NULL != (*env)->ExceptionOccurred(env)) {
        qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetByte SetByteArrayRegion");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
    return (jint)rc;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeGetShort(JNIEnv *env,
                                                                         jclass cl,
                                                                         jlong data,
                                                                         jint id,
                                                                         jshortArray value)
{
    if (value == NULL) {
        qeo_log_e("Illegal NULL pointer provided for parameter value in NativeTypeSupport.nativeGetShort");
        return (jint)QEO_EINVAL;
    }

    int16_t _value;
    qeo_retcode_t rc = QEO_OK;

    rc = qeocore_data_get_member((qeocore_data_t *)data, (qeocore_member_id_t)id, &_value);
    if (QEO_OK != rc) {
        qeo_log_e("Failed to get data member");
        return (jint)rc;
    }

    (*env)->SetShortArrayRegion(env, value, 0, 1, (jshort *)&_value);
    if (NULL != (*env)->ExceptionOccurred(env)) {
        qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetShort SetShortArrayRegion");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
    return (jint)rc;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeGetInteger(JNIEnv *env,
                                                                           jclass cl,
                                                                           jlong data,
                                                                           jint id,
                                                                           jintArray value)
{
    if (value == NULL) {
        qeo_log_e("Illegal NULL pointer provided for parameter value in NativeTypeSupport.nativeGetInteger");
        return (jint)QEO_EINVAL;
    }

    int32_t _value;
    qeo_retcode_t rc = QEO_OK;

    rc = qeocore_data_get_member((qeocore_data_t *)data, (qeocore_member_id_t)id, &_value);
    if (QEO_OK != rc) {
        qeo_log_e("Failed to get data member");
        return (jint)rc;
    }

    (*env)->SetIntArrayRegion(env, value, 0, 1, (jint *)&_value);
    if (NULL != (*env)->ExceptionOccurred(env)) {
        qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetInteger SetIntArrayRegion");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
    return (jint)rc;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeGetLong(JNIEnv *env,
                                                                        jclass cl,
                                                                        jlong data,
                                                                        jint id,
                                                                        jlongArray value)
{
    if (value == NULL) {
        qeo_log_e("Illegal NULL pointer provided for parameter value in NativeTypeSupport.nativeGetLong");
        return (jint)QEO_EINVAL;
    }

    int64_t _value;
    qeo_retcode_t rc = QEO_OK;

    rc = qeocore_data_get_member((qeocore_data_t *)data, (qeocore_member_id_t)id, &_value);
    if (QEO_OK != rc) {
        qeo_log_e("Failed to get data member");
        return (jint)rc;
    }

    (*env)->SetLongArrayRegion(env, value, 0, 1, (jlong *)&_value);
    if (NULL != (*env)->ExceptionOccurred(env)) {
        qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetLong SetLongArrayRegion");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
    return (jint)rc;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeGetFloat(JNIEnv *env,
                                                                         jclass cl,
                                                                         jlong data,
                                                                         jint id,
                                                                         jfloatArray value)
{
    if (value == NULL) {
        qeo_log_e("Illegal NULL pointer provided for parameter value in NativeTypeSupport.nativeGetFloat");
        return (jint)QEO_EINVAL;
    }

    float _value;
    qeo_retcode_t rc = QEO_OK;

    rc = qeocore_data_get_member((qeocore_data_t *)data, (qeocore_member_id_t)id, &_value);
    if (QEO_OK != rc) {
        qeo_log_e("Failed to get data member");
        return (jint)rc;
    }

    (*env)->SetFloatArrayRegion(env, value, 0, 1, (jfloat *)&_value);
    if (NULL != (*env)->ExceptionOccurred(env)) {
        qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetFloat SetFloatArrayRegion");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
    return (jint)rc;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeGetBoolean(JNIEnv *env,
                                                                           jclass cl,
                                                                           jlong data,
                                                                           jint id,
                                                                           jbooleanArray value)
{
    if (value == NULL) {
        qeo_log_e("Illegal NULL pointer provided for parameter value in NativeTypeSupport.nativeGetBoolean");
        return (jint)QEO_EINVAL;
    }

    int8_t _value;
    qeo_retcode_t rc = QEO_OK;

    rc = qeocore_data_get_member((qeocore_data_t *)data, (qeocore_member_id_t)id, &_value);
    if (QEO_OK != rc) {
        qeo_log_e("Failed to get data member");
        return (jint)rc;
    }

    (*env)->SetBooleanArrayRegion(env, value, 0, 1, (jboolean *)&_value);
    if (NULL != (*env)->ExceptionOccurred(env)) {
        qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetBoolean SetBooleanArrayRegion");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
    return (jint)rc;
}

JNIEXPORT jobjectArray JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeGetStringSequence(JNIEnv *env,
                                                                                       jclass cl,
                                                                                       jlong data)
{
    qeo_sequence_t sequence;
    jobjectArray value = NULL;
    qeo_retcode_t ret = QEO_OK;

    jclass stringClass = (*env)->FindClass(env, "java/lang/String");
    if (NULL != (*env)->ExceptionOccurred(env)) {
        qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetStringSequence FindClass");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }

    if (NULL == stringClass) {
        qeo_log_e("Failed to FindClass java/lang/String");
    	return NULL;
    }

    ret = qeocore_data_sequence_get((qeocore_data_t *)data, &sequence, 0, QEOCORE_SIZE_UNLIMITED);
    if (QEO_OK != ret) {
        return NULL;
    }

    value = (*env)->NewObjectArray(env, DDS_SEQ_LENGTH(sequence), stringClass, NULL);
    if (NULL != (*env)->ExceptionOccurred(env)) {
        qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetStringSequence NewObjectArray");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
    if (NULL == value) {
        qeo_log_e("Failed to create ObjectArray value in NativeTypeSupport.nativeGetStringSequence");
        return NULL;
    }

    for (int i = 0; i < DDS_SEQ_LENGTH(sequence); i++) {
        jstring stringValue = (*env)->NewStringUTF(env, ((char **)DDS_SEQ_DATA(sequence))[i]);
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetStringSequence NewStringUTF");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
        if (NULL == stringValue) {
            qeo_log_e("Failed to NewStringUTF for stringValue");
            break;
        }

        (*env)->SetObjectArrayElement(env, value, i, stringValue);
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetStringSequence SetObjectArrayElement");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }

        (*env)->DeleteLocalRef(env, stringValue);
    }
    qeocore_data_sequence_free((qeocore_data_t *)data, &sequence);
    return value;
}

JNIEXPORT jbyteArray JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeGetByteSequence(JNIEnv *env,
                                                                                   jclass cl,
                                                                                   jlong data)
{
    qeo_sequence_t sequence;
    jbyteArray value = NULL;
    qeo_retcode_t ret = QEO_OK;

    ret = qeocore_data_sequence_get((qeocore_data_t *)data, &sequence, 0, QEOCORE_SIZE_UNLIMITED);
    if (QEO_OK != ret) {
        return NULL;
    }

    value = (*env)->NewByteArray(env, DDS_SEQ_LENGTH(sequence));
    if (NULL == value) {
        qeo_log_e("Failed to create ByteArray value in NativeTypeSupport.nativeGetByteSequence");
        return NULL;
    }
    if (DDS_SEQ_LENGTH(sequence) > 0) {
        (*env)->SetByteArrayRegion(env, value, 0, DDS_SEQ_LENGTH(sequence), (jbyte *)DDS_SEQ_DATA(sequence));
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetByteSequence SetByteArrayRegion");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }
    qeocore_data_sequence_free((qeocore_data_t *)data, &sequence);
    return value;
}

JNIEXPORT jshortArray JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeGetShortSequence(JNIEnv *env,
                                                                                     jclass cl,
                                                                                     jlong data)
{
    qeo_sequence_t sequence;
    jshortArray value = NULL;
    qeo_retcode_t ret = QEO_OK;

    ret = qeocore_data_sequence_get((qeocore_data_t *)data, &sequence, 0, QEOCORE_SIZE_UNLIMITED);
    if (QEO_OK != ret) {
        return NULL;
    }

    value = (*env)->NewShortArray(env, DDS_SEQ_LENGTH(sequence));
    if (NULL == value) {
        qeo_log_e("Failed to create ShortArray value in NativeTypeSupport.nativeGetShortSequence");
        return NULL;
    }
    if (DDS_SEQ_LENGTH(sequence) > 0) {
        (*env)->SetShortArrayRegion(env, value, 0, DDS_SEQ_LENGTH(sequence), (jshort *)DDS_SEQ_DATA(sequence));
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetShortSequence SetShortArrayRegion");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }
    qeocore_data_sequence_free((qeocore_data_t *)data, &sequence);
    return value;
}

JNIEXPORT jintArray JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeGetIntegerSequence(JNIEnv *env,
                                                                                     jclass cl,
                                                                                     jlong data)
{
    qeo_sequence_t sequence;
    jintArray value = NULL;
    qeo_retcode_t ret = QEO_OK;

    ret = qeocore_data_sequence_get((qeocore_data_t *)data, &sequence, 0, QEOCORE_SIZE_UNLIMITED);
    if (QEO_OK != ret) {
        return NULL;
    }

    value = (*env)->NewIntArray(env, DDS_SEQ_LENGTH(sequence));
    if (NULL == value) {
        qeo_log_e("Failed to create IntArray value in NativeTypeSupport.nativeGetIntegerSequence");
        return NULL;
    }
    if (DDS_SEQ_LENGTH(sequence) > 0) {
        (*env)->SetIntArrayRegion(env, value, 0, DDS_SEQ_LENGTH(sequence), (jint *)DDS_SEQ_DATA(sequence));
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetIntegerSequence SetIntArrayRegion");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }
    qeocore_data_sequence_free((qeocore_data_t *)data, &sequence);
    return value;
}

JNIEXPORT jlongArray JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeGetLongSequence(JNIEnv *env,
                                                                                   jclass cl,
                                                                                   jlong data)
{
    qeo_sequence_t sequence;
    jlongArray value = NULL;
    qeo_retcode_t ret = QEO_OK;

    ret = qeocore_data_sequence_get((qeocore_data_t *)data, &sequence, 0, QEOCORE_SIZE_UNLIMITED);
    if (QEO_OK != ret) {
        return NULL;
    }

    value = (*env)->NewLongArray(env, DDS_SEQ_LENGTH(sequence));
    if (NULL == value) {
        qeo_log_e("Failed to create LongArray value in NativeTypeSupport.nativeGetLongSequence");
        return NULL;
    }
    if (DDS_SEQ_LENGTH(sequence) > 0) {
        (*env)->SetLongArrayRegion(env, value, 0, DDS_SEQ_LENGTH(sequence), (jlong *)DDS_SEQ_DATA(sequence));
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetLongSequence SetLongArrayRegion");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }
    qeocore_data_sequence_free((qeocore_data_t *)data, &sequence);
    return value;
}

JNIEXPORT jfloatArray JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeGetFloatSequence(JNIEnv *env,
                                                                                     jclass cl,
                                                                                     jlong data)
{
    qeo_sequence_t sequence;
    jfloatArray value = NULL;
    qeo_retcode_t ret = QEO_OK;

    ret = qeocore_data_sequence_get((qeocore_data_t *)data, &sequence, 0, QEOCORE_SIZE_UNLIMITED);
    if (QEO_OK != ret) {
        return NULL;
    }

    value = (*env)->NewFloatArray(env, DDS_SEQ_LENGTH(sequence));
    if (NULL == value) {
        qeo_log_e("Failed to create FloatArray value in NativeTypeSupport.nativeGetFloatSequence");
        return NULL;
    }
    if (DDS_SEQ_LENGTH(sequence) > 0) {
        (*env)->SetFloatArrayRegion(env, value, 0, DDS_SEQ_LENGTH(sequence), (jfloat *)DDS_SEQ_DATA(sequence));
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetFloatSequence SetFloatArrayRegion");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }
    qeocore_data_sequence_free((qeocore_data_t *)data, &sequence);
    return value;
}

JNIEXPORT jbooleanArray JNICALL Java_org_qeo_jni_NativeTypeSupport_nativeGetBooleanSequence(JNIEnv *env,
                                                                                        jclass cl,
                                                                                        jlong data)
{
    qeo_sequence_t sequence;
    jbooleanArray value = NULL;
    qeo_retcode_t ret = QEO_OK;

    ret = qeocore_data_sequence_get((qeocore_data_t *)data, &sequence, 0, QEOCORE_SIZE_UNLIMITED);
    if (QEO_OK != ret) {
        return NULL;
    }

    value = (*env)->NewBooleanArray(env, DDS_SEQ_LENGTH(sequence));
    if (NULL == value) {
        qeo_log_e("Failed to create BooleanArray value in NativeTypeSupport.nativeGetBooleanSequence");
        return NULL;
    }
    if (DDS_SEQ_LENGTH(sequence) > 0) {
        (*env)->SetBooleanArrayRegion(env, value, 0, DDS_SEQ_LENGTH(sequence), (jboolean *)DDS_SEQ_DATA(sequence));
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in NativeTypeSupport.nativeGetBooleanSequence SetBooleanArrayRegion");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }
    qeocore_data_sequence_free((qeocore_data_t *)data, &sequence);
    return value;
}

/*
 * NativeReader functions
 */
void java_exit(void)
{
    int getEnvStat = 0;

    if (_jvm) {
        getEnvStat = (*_jvm)->DetachCurrentThread(_jvm);
        log_printf("JNI exit called\n");
    }
}

static JNIEnv *attachThreadAndGetEnv(void)
{
    JNIEnv *env = NULL;
    int rc = JNI_ERR;

    if (_jvm) {
        rc = (*_jvm)->GetEnv(_jvm, (void **)&env, JNI_VERSION_1_4);
        if (rc == JNI_EDETACHED) {
#ifdef ANDROID
            rc = (*_jvm)->AttachCurrentThread(_jvm, &env, NULL);
#else
            rc = (*_jvm)->AttachCurrentThread(_jvm, (void**)&env, NULL);
#endif
            qeocore_atexit(java_exit);
            if (rc < 0)
                abort();
        }
    }
    if (JNI_OK != rc) {
        fprintf(stderr, "Could not get JNI environment for call-back: %d\n", rc);
        qeo_log_e("Could not get JNI environment for call-back: %d", rc);
    }
    return env;
}

static void java_on_data_available_interposer(const qeocore_reader_t *reader,
                                              const qeocore_data_t *data,
                                              uintptr_t userdata)
{
    JNIEnv *env = NULL;

    env = attachThreadAndGetEnv();
    if (NULL != env) {
        rw_interposer_data_t *idata = (rw_interposer_data_t *)userdata;

        log_printf("On Data Available call-back called");
        (*env)->CallVoidMethod(env, idata->object, idata->onDataAvailable, (jlong)data);
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in CallVoidMethod onDataAvailable");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }
}

static qeo_policy_perm_t java_on_policy_update_interposer(const qeo_policy_identity_t *identity,
                                                          uintptr_t userdata)
{
    JNIEnv *env = NULL;
    jboolean allow = false;

    env = attachThreadAndGetEnv();
    if (NULL != env) {
        rw_interposer_data_t *idata = (rw_interposer_data_t *)userdata;

        log_printf("On Update Policy call-back called");
        if (NULL == identity) {
            (*env)->CallVoidMethod(env, idata->object, idata->onPolicyUpdateDone);
            if (NULL != (*env)->ExceptionOccurred(env)) {
                qeo_log_e("Unexpected exception occurred in CallVoidMethod onPolicyUpdateDone");
                (*env)->ExceptionDescribe(env);
                (*env)->ExceptionClear(env);
            }
        }
        else {
            jlong uid = qeo_policy_identity_get_uid(identity);

            allow = (*env)->CallBooleanMethod(env, idata->object, idata->onPolicyUpdate, uid);
            if (NULL != (*env)->ExceptionOccurred(env)) {
                qeo_log_e("Unexpected exception occurred in CallBooleanMethod onPolicyUpdate");
                (*env)->ExceptionDescribe(env);
                (*env)->ExceptionClear(env);
            }
        }
    }
    return (allow ? QEO_POLICY_ALLOW : QEO_POLICY_DENY);
}

static qeo_policy_perm_t java_on_reader_policy_update_interposer(const qeocore_reader_t *reader,
                                                                 const qeo_policy_identity_t *identity,
                                                                 uintptr_t userdata)
{
    return java_on_policy_update_interposer(identity, userdata);
}

static qeo_policy_perm_t java_on_writer_policy_update_interposer(const qeocore_writer_t *writer,
                                                                 const qeo_policy_identity_t *identity,
                                                                 uintptr_t userdata)
{
    return java_on_policy_update_interposer(identity, userdata);
}

void java_init_done_cb_interposer(uintptr_t userdata, bool result)
{
    JNIEnv *env = NULL;
    int getEnvStat = JNI_ERR;
    jboolean res = (result) ? JNI_TRUE : JNI_FALSE;

    if (_jvm) {
        getEnvStat = (*_jvm)->GetEnv(_jvm, (void **)&env, JNI_VERSION_1_4);
    }
    if (getEnvStat == JNI_OK) {
        interposer_data_t *idata = (interposer_data_t *)userdata;

        log_printf("Init Done call-back called");
        (*env)->CallVoidMethod(env, idata->object, idata->method, res);
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in CallVoidMethod init_done_cb");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }
    else {
        fprintf(stderr, "Could not get JNI environment for Init Done call-back: %d\n", getEnvStat);
        qeo_log_e("Could not get JNI environment for Init Done call-back: %d", getEnvStat);
    }

}

static void on_qeocore_on_factory_init_done(qeo_factory_t *factory, bool success)
{
    qeofactory_userdata *userdata = NULL;

    qeocore_factory_get_user_data(factory, (uintptr_t *)&userdata);
    if (userdata != NULL) {
        userdata->init_done = true;
        userdata->success = success;
        log_printf("init done");
        sem_post(&userdata->semaphore);
    }
}

JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeReader_nativeOpen(JNIEnv *env,
                                                                 jobject obj,
                                                                 jlong factory,
                                                                 jint readerType,
                                                                 jlong type,
                                                                 jstring n,
                                                                 jboolean setDataAvailableCb,
                                                                 jboolean setPolicyUpdateCb,
                                                                 jintArray result)
{
    qeo_retcode_t rc = QEO_OK;
    const char *method = NULL;
    const char *name = NULL;
    qeocore_reader_t *reader = NULL;
    rw_interposer_data_t *idata = NULL;

    if (NULL == result) {
        rc = QEO_EINVAL;
        qeo_log_e("Illegal NULL pointer provided for parameter result in NativeReader.nativeOpen");
        return (jlong)NULL;
    }

    if (NULL == n) {
        rc = QEO_EINVAL;
        qeo_log_e("Illegal NULL pointer provided for parameter n in NativeReader.nativeOpen");
        (*env)->SetIntArrayRegion(env, result, 0, 1, (jint *)&rc);
        if (NULL != (*env)->ExceptionOccurred(env)) {
            qeo_log_e("Unexpected exception occurred in NativeReader.nativeOpen SetIntArrayRegion");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
        return (jlong)NULL;
    }

    do {
        qeocore_reader_listener_t listener = {0};

        name = (*env)->GetStringUTFChars(env, n, NULL);
        if (NULL == name) {
            qeo_log_e("Failed to GetStringUTFChars from name");
            rc = QEO_EFAIL;
            break;
        }
        if (setDataAvailableCb || setPolicyUpdateCb) {
            rc = create_rw_interposer(env, obj, setDataAvailableCb, setPolicyUpdateCb, &idata);
            if (QEO_OK != rc) {
                break;
            }
            if (setDataAvailableCb) {
                listener.on_data = java_on_data_available_interposer;
            }
            if (setPolicyUpdateCb) {
                listener.on_policy_update = java_on_reader_policy_update_interposer;
            }
            listener.userdata = (uintptr_t)idata;
        }
        log_printf("Going to create reader");
        reader = qeocore_reader_open((qeo_factory_t *)factory, (qeocore_type_t *)type, name,
                                     QEOCORE_EFLAG_ENABLE | etype_to_eflags(readerType), &listener, &rc);

        if (NULL == reader) {
            if (rc == QEO_OK) {
                //don't overwrite returncode if already set
                rc = QEO_EFAIL;
            }
            break;
        }
    } while (0);

    if (rc != QEO_OK) {
        free_rw_interpose_data(env, idata);
    }

    if (NULL != name) {
        (*env)->ReleaseStringUTFChars(env, n, name);
    }

    // Set the result in input parameter result.
    (*env)->SetIntArrayRegion(env, result, 0, 1, (jint *)&rc);
    if (NULL != (*env)->ExceptionOccurred(env)) {
        qeo_log_e("Unexpected exception occurred in NativeReader.nativeOpen SetIntArrayRegion result");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }

    log_printf("Reader created");
    return (jlong)reader;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeReader_nativeRead(JNIEnv *env,
                                                                jobject obj,
                                                                jlong reader,
                                                                jint instanceHandle,
                                                                jlong nativeData)
{
    qeo_retcode_t ret = 0;
    qeocore_filter_t filter = { };

    filter.instance_handle = instanceHandle;
    log_printf("read\n");
    ret = qeocore_reader_read((const qeocore_reader_t*)reader, &filter, (qeocore_data_t *)nativeData);
    log_printf("read done\n");

    return (jint)ret;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeReader_nativeTake(JNIEnv *env,
                                                                jobject obj,
                                                                jlong reader,
                                                                jint instanceHandle,
                                                                jlong nativeData)
{
    qeo_retcode_t ret = 0;
    qeocore_filter_t filter = { };

    filter.instance_handle = instanceHandle;
    log_printf("take\n");
    ret = qeocore_reader_take((const qeocore_reader_t*)reader, &filter, (qeocore_data_t *)nativeData);
    log_printf("take done\n");

    return (jint)ret;
}

JNIEXPORT void JNICALL Java_org_qeo_jni_NativeReader_nativeClose(JNIEnv *env, jobject obj, jlong reader)
{
    interposer_data_t *idata = NULL;

    log_printf("going to close reader\n");
    idata = (interposer_data_t *)qeocore_reader_get_userdata((qeocore_reader_t *)reader);
    qeocore_reader_close((qeocore_reader_t *)reader);
    if (NULL != idata) {
        free_interpose_data(env, idata);
    }
    log_printf("reader closed\n");
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeReader_nativeUpdatePolicy(JNIEnv *env, jobject obj, jlong reader)
{
    return (jint)qeocore_reader_policy_update((const qeocore_reader_t*)reader);
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeReader_nativeSetBGNS(JNIEnv *env, jobject obj, jlong reader, jboolean enabled)
{
    return (jint)qeocore_reader_bgns_notify((qeocore_reader_t*)reader, JNI_TRUE == enabled);
}

/*
 * NativeWriter functions
 */
JNIEXPORT jlong JNICALL Java_org_qeo_jni_NativeWriter_nativeOpen(JNIEnv *env,
                                                                 jobject obj,
                                                                 jlong factory,
                                                                 jint writerType,
                                                                 jlong type,
                                                                 jstring s,
                                                                 jboolean setPolicyUpdateCb,
                                                                 jintArray result)
{
    const char *name = NULL;
    qeo_retcode_t rc = QEO_OK;
    qeocore_writer_t *writer = NULL;
    rw_interposer_data_t *idata = NULL;

    if (NULL == result) {
        qeo_log_e("Illegal NULL pointer provided for parameter result in NativeWriter.nativeOpen");
        return (jlong)NULL;
    }

    do {
        qeocore_writer_listener_t listener = {0};

        if (NULL == s) {
            rc = QEO_EINVAL;
            qeo_log_e("Illegal NULL pointer provided for parameter s in NativeWriter.nativeOpen");
            break;
        }
        name = (*env)->GetStringUTFChars(env, s, 0);
        if (NULL == name) {
            qeo_log_e("Failed to GetStringUTFChars from s");
            rc = QEO_EFAIL;
            break;
        }
        if (setPolicyUpdateCb) {
            rc = create_rw_interposer(env, obj, false, setPolicyUpdateCb, &idata);
            if (QEO_OK != rc) {
                break;
            }
            listener.on_policy_update = java_on_writer_policy_update_interposer;
            listener.userdata = (uintptr_t)idata;
        }
        log_printf("Going to create writer");
        writer = qeocore_writer_open((qeo_factory_t *) factory, (qeocore_type_t *)type, name,
                                     QEOCORE_EFLAG_ENABLE | etype_to_eflags(writerType), &listener, &rc);
        if (NULL == writer) {
            qeo_log_e("Failed to open writer");
            if (rc == QEO_OK) {
                //don't overwrite returncode if already set
                rc = QEO_EFAIL;
            }
            break;
        }
    } while (0);

    (*env)->SetIntArrayRegion(env, result, 0, 1, (jint *)&rc);
    if (NULL != (*env)->ExceptionOccurred(env)) {
        qeo_log_e("Unexpected exception occurred in NativeWriter.nativeOpen SetIntArrayRegion result");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }

    if (rc != QEO_OK) {
        free_rw_interpose_data(env, idata);
    }

    if (NULL != name) {
        (*env)->ReleaseStringUTFChars(env, s, name);
    }

    log_printf("Writer created");
    return (jlong)writer;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeWriter_nativeWrite(JNIEnv *env, jclass cl, jlong writer, jlong data)
{
    log_printf("write\n");
    return (jint)qeocore_writer_write((const qeocore_writer_t *)writer, (qeocore_data_t *)data);
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeWriter_nativeRemove(JNIEnv *env, jclass cl, jlong writer, jlong data)
{
    log_printf("remove\n");
    return (jint)qeocore_writer_remove((const qeocore_writer_t *)writer, (qeocore_data_t *)data);
}

JNIEXPORT void JNICALL Java_org_qeo_jni_NativeWriter_nativeClose(JNIEnv *env, jclass cl, jlong writer)
{
    log_printf("going to close writer\n");
    qeocore_writer_close((qeocore_writer_t *)writer);
    log_printf("writer closed\n");
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeWriter_nativeUpdatePolicy(JNIEnv *env, jclass cl, jlong writer)
{
    return (jint)qeocore_writer_policy_update((const qeocore_writer_t*)writer);
}

