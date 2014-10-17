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

#include "EventWithUnbArrayOfStruct.h"

const DDS_TypeSupport_meta org_qeo_test_EventWithUnbArrayOfStruct_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.test.EventWithUnbArrayOfStruct", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 1, .size = sizeof(org_qeo_test_EventWithUnbArrayOfStruct_t) },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfStructWithPrimitives", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_EventWithUnbArrayOfStruct_t, MyUnbArrayOfStructWithPrimitives) },
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = org_qeo_test_MyStructWithPrimitives_type },
};

