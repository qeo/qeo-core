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

#ifndef QDM_STATEWITHUNBARRAYOFSTRUCTKEYISINT16_H_
#define QDM_STATEWITHUNBARRAYOFSTRUCTKEYISINT16_H_

#include <qeo/types.h>
#include "StructWithPrimitives.h"

#ifdef __cplusplus
extern "C"
{
#endif


DDS_SEQUENCE(org_qeo_test_MyStructWithPrimitives_t, org_qeo_test_StateWithUnbArrayOfStructKeyIsInt16_MyUnbArrayOfStructWithPrimitives_seq);
/**
 * struct representing a state containing an unbound array (sequence) of a struct
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
    org_qeo_test_StateWithUnbArrayOfStructKeyIsInt16_MyUnbArrayOfStructWithPrimitives_seq MyUnbArrayOfStructWithPrimitives;
} org_qeo_test_StateWithUnbArrayOfStructKeyIsInt16_t;
extern const DDS_TypeSupport_meta org_qeo_test_StateWithUnbArrayOfStructKeyIsInt16_type[];

#ifdef __cplusplus
}
#endif

#endif /* QDM_STATEWITHUNBARRAYOFSTRUCTKEYISINT16_H_ */

