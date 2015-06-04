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

#include "EventWithUnbArraysOfPrimitives.h"

const DDS_TypeSupport_meta org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.test.EventWithUnbArraysOfPrimitivesStruct", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 8, .size = sizeof(org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_t) },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfBoolean", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_t, MyUnbArrayOfBoolean) },
    { .tc = CDR_TYPECODE_BOOLEAN },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfByte", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_t, MyUnbArrayOfByte) },
    { .tc = CDR_TYPECODE_OCTET },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfByte1", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_t, MyUnbArrayOfByte1) },
    { .tc = CDR_TYPECODE_OCTET },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfInt16", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_t, MyUnbArrayOfInt16) },
    { .tc = CDR_TYPECODE_SHORT },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfInt32", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_t, MyUnbArrayOfInt32) },
    { .tc = CDR_TYPECODE_LONG },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfInt64", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_t, MyUnbArrayOfInt64) },
    { .tc = CDR_TYPECODE_LONGLONG },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfFloat32", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_t, MyUnbArrayOfFloat32) },
    { .tc = CDR_TYPECODE_FLOAT },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfString", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_t, MyUnbArrayOfString) },
    { .tc = CDR_TYPECODE_CSTRING, .size = 0 },
};

