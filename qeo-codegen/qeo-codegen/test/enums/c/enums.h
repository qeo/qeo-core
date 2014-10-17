/*
 * Copyright (c) 2014 - Qeo LLC
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

#ifndef QDM_ENUMS_H_
#define QDM_ENUMS_H_

#include <qeo/types.h>

#ifdef __cplusplus
extern "C"
{
#endif


/**
 * Enumeration doc
 */
typedef enum {
  /**
   * Enumeration value doc
   */
 ORG_QEO_TEST_ENUMNAME_ENUM1,
 ORG_QEO_TEST_ENUMNAME_ENUM2
} org_qeo_test_EnumName_t;
extern const DDS_TypeSupport_meta org_qeo_test_EnumName_type[];

typedef enum {
 ORG_QEO_TEST_ENUMNAMEBIS_ENUM1BIS,
 ORG_QEO_TEST_ENUMNAMEBIS_ENUM2BIS
} org_qeo_test_EnumNameBis_t;
extern const DDS_TypeSupport_meta org_qeo_test_EnumNameBis_type[];

/**
 * Struct containing enums.
 */
typedef struct {
    qeo_boolean_t MyBoolean;
    int8_t MyByte;
    int16_t MyInt16;
    org_qeo_test_EnumName_t MyEnum;
} org_qeo_test_MyStructWithEnums_t;
extern const DDS_TypeSupport_meta org_qeo_test_MyStructWithEnums_type[];

/**
 * Struct containing enums.
 */
typedef struct {
    float MyFloat32;
    char * MyString;
    org_qeo_test_EnumNameBis_t MyEnumBis;
} org_qeo_test_MyStructWithEnumsBis_t;
extern const DDS_TypeSupport_meta org_qeo_test_MyStructWithEnumsBis_type[];

#ifdef __cplusplus
}
#endif

#endif /* QDM_ENUMS_H_ */

