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

#ifndef QDM_STATEWITHPRIMITIVESKEYISBYTE_H_
#define QDM_STATEWITHPRIMITIVESKEYISBYTE_H_

#include <qeo/types.h>

#ifdef __cplusplus
extern "C"
{
#endif


/**
 * This struct represents a state containing only primitive types.
 */
typedef struct {
    qeo_boolean_t MyBoolean;
  /**
   * [Key]
   */
    int8_t MyByte;
    int16_t MyInt16;
    int32_t MyInt32;
    int64_t MyInt64;
    float MyFloat32;
    char * MyString;
} org_qeo_test_StateWithPrimitivesKeyIsByte_t;
extern const DDS_TypeSupport_meta org_qeo_test_StateWithPrimitivesKeyIsByte_type[];

#ifdef __cplusplus
}
#endif

#endif /* QDM_STATEWITHPRIMITIVESKEYISBYTE_H_ */

