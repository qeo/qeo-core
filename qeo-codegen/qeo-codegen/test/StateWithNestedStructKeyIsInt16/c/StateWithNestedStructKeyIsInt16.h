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

/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#ifndef QDM_STATEWITHNESTEDSTRUCTKEYISINT16_H_
#define QDM_STATEWITHNESTEDSTRUCTKEYISINT16_H_

#include <qeo/types.h>
#include "StructWithPrimitives.h"
#include "qeo.h"

#ifdef __cplusplus
extern "C"
{
#endif


/**
 * struct representing an event containing a nested struct
 */
typedef struct {
    qeo_boolean_t MyBoolean;
    int8_t MyByte;
  /**
   * [Key]
   */
    int16_t MyInt16;
    int32_t MyInt32;
    int64_t MyInt64;
    float MyFloat32;
    char * MyString;
    org_qeo_test_MyStructWithPrimitives_t MyStructWithPrimitives;
    org_qeo_DeviceId_t MyDeviceId;
} org_qeo_test_StateWithNestedStructKeyIsInt16_t;
extern const DDS_TypeSupport_meta org_qeo_test_StateWithNestedStructKeyIsInt16_type[];

#ifdef __cplusplus
}
#endif

#endif /* QDM_STATEWITHNESTEDSTRUCTKEYISINT16_H_ */

