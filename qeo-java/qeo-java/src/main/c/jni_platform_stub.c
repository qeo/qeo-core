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
#include "org_qeo_jni_NativeQeo.h"
#include "jni_platform_p.h"
#include <qeocore/api.h>
#include <platform_api/platform_api.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <qeo/log.h>
#include <unistd.h>

/*#######################################################################
 # TYPES SECTION                                                         #
 ########################################################################*/
/*#######################################################################
 # STATIC FUNCTION DECLARATION                                           #
 ########################################################################*/
/*#######################################################################
 # STATIC VARIABLE SECTION                                               #
 ########################################################################*/
/*#######################################################################
 # STATIC FUNCTION IMPLEMENTATION                                        #
 ########################################################################*/
/*#######################################################################
 # PUBLIC FUNCTION IMPLEMENTATION                                        #
 ########################################################################*/
bool platform_jni_init(void){


    
/*  
 * call default implementation. 
 * Normally we should never have to explicitly call this function,
 * as default_impl_init() is marked as constructor. For some reason, when you really call the function from here,
 * default_impl_init() is executed at construction time... In other words: it gets executed twice. 
 * But when we don't call default_impl_init() here, the constructor does not seem to be called...
 *
 * Very strange. if you know more about this, feel free to contact me :) */
    extern void default_impl_init(void);


    default_impl_init();

    return true;

}

bool platform_jni_set_interposer(JavaVM *jvm, const interposer_data_t *idata_otc_cb, const interposer_data_t *idata_update_cb){
    return true;
}

void platform_jni_clear_interposer()
{
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeQeo_nativeSetRegistrationCredentials(JNIEnv* env, jobject thiz, jstring otc, jstring url)
{
    return QEO_OK;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeQeo_nativeCancelRegistration(JNIEnv* env, jobject thiz){

    return QEO_OK;
}

JNIEXPORT jint JNICALL Java_org_qeo_jni_NativeQeo_nativeConfigDeviceInfo(JNIEnv *env,
                                                                   jclass cl,
                                                                   jobjectArray devInfoArray,
                                                                   jlongArray devIdArray)
{
    return QEO_OK;
}
