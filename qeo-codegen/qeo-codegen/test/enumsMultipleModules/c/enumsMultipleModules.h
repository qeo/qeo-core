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

#ifndef QDM_ENUMSMULTIPLEMODULES_H_
#define QDM_ENUMSMULTIPLEMODULES_H_

#include <qeo/types.h>

#ifdef __cplusplus
extern "C"
{
#endif


typedef enum {
 ORG_QEO_TEST_ENUMNAME_ENUM1,
 ORG_QEO_TEST_ENUMNAME_ENUM2
} org_qeo_test_EnumName_t;
extern const DDS_TypeSupport_meta org_qeo_test_EnumName_type[];

typedef enum {
 ORG_QEO_TEST_ENUMNAMEBIS_ENUM1,
 ORG_QEO_TEST_ENUMNAMEBIS_ENUM2
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
    int32_t MyInt32;
    int64_t MyInt64;
    org_qeo_test_EnumNameBis_t MyEnumBis;
} org_qeo_test_MyStructWithEnumsBis_t;
extern const DDS_TypeSupport_meta org_qeo_test_MyStructWithEnumsBis_type[];

typedef enum {
 ORG_QEO_TESTO_ENUMNAME_ENUM3,
 ORG_QEO_TESTO_ENUMNAME_ENUM4
} org_qeo_testo_EnumName_t;
extern const DDS_TypeSupport_meta org_qeo_testo_EnumName_type[];

typedef enum {
 ORG_QEO_TESTO_ENUMNAMEBIS_ENUM1,
 ORG_QEO_TESTO_ENUMNAMEBIS_ENUM2
} org_qeo_testo_EnumNameBis_t;
extern const DDS_TypeSupport_meta org_qeo_testo_EnumNameBis_type[];

/**
 * Struct containing enums.
 */
typedef struct {
    float MyFloat32;
    char * MyString;
    org_qeo_testo_EnumName_t MyEnum;
} org_qeo_testo_MyStructWithEnums_t;
extern const DDS_TypeSupport_meta org_qeo_testo_MyStructWithEnums_type[];

/**
 * Struct containing enums.
 */
typedef struct {
    qeo_boolean_t MyBooleanBis;
    int8_t MyByteBis;
    int16_t MyInt16Bis;
    org_qeo_testo_EnumNameBis_t MyEnumBis;
} org_qeo_testo_MyStructWithEnumsBis_t;
extern const DDS_TypeSupport_meta org_qeo_testo_MyStructWithEnumsBis_type[];

#ifdef __cplusplus
}
#endif

#endif /* QDM_ENUMSMULTIPLEMODULES_H_ */

