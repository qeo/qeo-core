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

#include <forwarder.h>
#include <qeo/log.h>
#include <jni.h>
#include <stdio.h>


JNIEXPORT jint JNICALL Java_org_qeo_forwarder_internal_jni_NativeForwarder_nativeStartForwarder(JNIEnv *env, jclass cl)
{
    qeo_log_d("Starting forwarder from JNI");
    return (jint) forwarder_start();
}

JNIEXPORT jint JNICALL Java_org_qeo_forwarder_internal_jni_NativeForwarder_nativeStopForwarder(JNIEnv *env, jclass cl)
{
    qeo_log_d("Stopping forwarder from JNI");
    return (jint) forwarder_stop();
}

JNIEXPORT void JNICALL Java_org_qeo_forwarder_internal_jni_NativeForwarder_nativeConfigLocalPort(JNIEnv *env, jclass cl, int port)
{
    qeo_log_d("Configuring forwarder TCP port from JNI to %d", port);
    char portS[10];
    sprintf(portS, "%d", port);
    forwarder_config_local_port(portS);
}

JNIEXPORT void JNICALL Java_org_qeo_forwarder_internal_jni_NativeForwarder_nativeConfigPublicLocator(JNIEnv *env, jclass cl, jstring ip, jint port)
{
    const char * ipChar;
    ipChar = (*env)->GetStringUTFChars(env, ip, NULL);
    if (ipChar == NULL) {
        qeo_log_e("invalid ip address parameter");
        return;
    }
    qeo_log_d("Configuring forwarder public IP from JNI to %s", ipChar);
    forwarder_config_public_locator(ipChar, port);
    (*env)->ReleaseStringUTFChars(env, ip, ipChar);
}


