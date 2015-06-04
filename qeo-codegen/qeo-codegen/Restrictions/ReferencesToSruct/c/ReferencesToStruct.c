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

#include "ReferencesToStruct.h"

const DDS_TypeSupport_meta org_qeo_test_RefsToStruct_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.test.RefsToStruct", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 4, .size = sizeof(org_qeo_test_RefsToStruct_t) },  
    { .tc = CDR_TYPECODE_TYPEREF, .name = "MyUnbArrayOfDeviceId1", .offset = offsetof(org_qeo_test_RefsToStruct_t, MyUnbArrayOfDeviceId1), .tsm = org_qeo_DeviceId_type },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfDeviceId2", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_RefsToStruct_t, MyUnbArrayOfDeviceId2) },  
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = org_qeo_DeviceId_type },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfDeviceId3", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_RefsToStruct_t, MyUnbArrayOfDeviceId3) },  
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = org_qeo_DeviceId_type },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfDeviceId4", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_test_RefsToStruct_t, MyUnbArrayOfDeviceId4) },  
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = org_qeo_DeviceId_type },  
};
