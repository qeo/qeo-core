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

#include "StateWithUnbArraysOfUnbArraysKeyInt16.h"

const DDS_TypeSupport_meta org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.test.StateWithUnbArraysOfUnbArraysKeyInt16", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 7, .size = sizeof(org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_t) },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfUnbArrayOfBoolean", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_t, MyUnbArrayOfUnbArrayOfBoolean) },  
    { .tc = CDR_TYPECODE_SEQUENCE },  
    { .tc = CDR_TYPECODE_BOOLEAN },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfUnbArrayOfByte", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_t, MyUnbArrayOfUnbArrayOfByte) },  
    { .tc = CDR_TYPECODE_SEQUENCE },  
    { .tc = CDR_TYPECODE_OCTET },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfUnbArrayOfInt16", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_t, MyUnbArrayOfUnbArrayOfInt16) },  
    { .tc = CDR_TYPECODE_SEQUENCE },  
    { .tc = CDR_TYPECODE_SHORT },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfUnbArrayOfInt32", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_t, MyUnbArrayOfUnbArrayOfInt32) },  
    { .tc = CDR_TYPECODE_SEQUENCE },  
    { .tc = CDR_TYPECODE_LONG },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfUnbArrayOfInt64", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_t, MyUnbArrayOfUnbArrayOfInt64) },  
    { .tc = CDR_TYPECODE_SEQUENCE },  
    { .tc = CDR_TYPECODE_LONGLONG },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfUnbArrayOfFloat32", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_t, MyUnbArrayOfUnbArrayOfFloat32) },  
    { .tc = CDR_TYPECODE_SEQUENCE },  
    { .tc = CDR_TYPECODE_FLOAT },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfUnbArrayOfString", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_t, MyUnbArrayOfUnbArrayOfString) },  
    { .tc = CDR_TYPECODE_SEQUENCE },  
    { .tc = CDR_TYPECODE_CSTRING, .size = 0 },  
};
