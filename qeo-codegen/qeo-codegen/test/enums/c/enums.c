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

#include "enums.h"

const DDS_TypeSupport_meta org_qeo_test_EnumName_type[] = {
    { .tc = CDR_TYPECODE_ENUM, .name = "org.qeo.test.EnumName", .nelem = 2 },
    { .name = "ENUM1", .label = ORG_QEO_TEST_ENUMNAME_ENUM1 },
    { .name = "ENUM2", .label = ORG_QEO_TEST_ENUMNAME_ENUM2 },
};


const DDS_TypeSupport_meta org_qeo_test_EnumNameBis_type[] = {
    { .tc = CDR_TYPECODE_ENUM, .name = "org.qeo.test.EnumNameBis", .nelem = 2 },
    { .name = "ENUM1BIS", .label = ORG_QEO_TEST_ENUMNAMEBIS_ENUM1BIS },
    { .name = "ENUM2BIS", .label = ORG_QEO_TEST_ENUMNAMEBIS_ENUM2BIS },
};


const DDS_TypeSupport_meta org_qeo_test_MyStructWithEnums_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.test.MyStructWithEnums", .flags = TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 4, .size = sizeof(org_qeo_test_MyStructWithEnums_t) },
    { .tc = CDR_TYPECODE_BOOLEAN, .name = "MyBoolean", .offset = offsetof(org_qeo_test_MyStructWithEnums_t, MyBoolean) },
    { .tc = CDR_TYPECODE_OCTET, .name = "MyByte", .offset = offsetof(org_qeo_test_MyStructWithEnums_t, MyByte) },
    { .tc = CDR_TYPECODE_SHORT, .name = "MyInt16", .offset = offsetof(org_qeo_test_MyStructWithEnums_t, MyInt16) },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "MyEnum", .offset = offsetof(org_qeo_test_MyStructWithEnums_t, MyEnum), .tsm = org_qeo_test_EnumName_type },
};


const DDS_TypeSupport_meta org_qeo_test_MyStructWithEnumsBis_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.test.MyStructWithEnumsBis", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 3, .size = sizeof(org_qeo_test_MyStructWithEnumsBis_t) },
    { .tc = CDR_TYPECODE_FLOAT, .name = "MyFloat32", .offset = offsetof(org_qeo_test_MyStructWithEnumsBis_t, MyFloat32) },
    { .tc = CDR_TYPECODE_CSTRING, .name = "MyString", .flags = TSMFLAG_DYNAMIC, .offset = offsetof(org_qeo_test_MyStructWithEnumsBis_t, MyString), .size = 0 },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "MyEnumBis", .offset = offsetof(org_qeo_test_MyStructWithEnumsBis_t, MyEnumBis), .tsm = org_qeo_test_EnumNameBis_type },
};

