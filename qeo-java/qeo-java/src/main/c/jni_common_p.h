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

#ifndef JNI_COMMON_P_H_
#define JNI_COMMON_P_H_

/*#######################################################################
 # HEADER (INCLUDE) SECTION                                              #
 ########################################################################*/
#include <jni.h>

/*#######################################################################
 # TYPES SECTION                                                         #
 ########################################################################*/
typedef struct
{
    jobject object;
    jclass class;
    jmethodID method;
} interposer_data_t;

/*#######################################################################
 # PUBLIC FUNCTION DECLARATION                                        #
 ########################################################################*/
#endif
