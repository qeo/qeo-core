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

#ifndef _JNI_PLATFORM_P_H_
#define _JNI_PLATFORM_P_H_

/*#######################################################################
 # HEADER (INCLUDE) SECTION                                              #
 ########################################################################*/
#include "jni_common_p.h"
#include <stdbool.h>

/*#######################################################################
 # TYPES SECTION                                                         #
 ########################################################################*/
/*#######################################################################
 # PUBLIC FUNCTION DECLARATION                                        #
 ########################################################################*/
bool platform_jni_init(void);
bool platform_jni_set_interposer(JavaVM *jvm, const interposer_data_t *idata_otc_cb, const interposer_data_t *idata_update_cb);
void platform_jni_clear_interposer();

#endif
