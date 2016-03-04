/*
 * Copyright (c) 2016 - Qeo LLC
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

#include "QSimpleChat_ChatMessage.h"
#include "cwifi.h"
#include "myenum.h"

const DDS_TypeSupport_meta org_qeo_sample_simplechat_ChatMessage_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.sample.simplechat.ChatMessage", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 7, .size = sizeof(org_qeo_sample_simplechat_ChatMessage_t) },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "from", .flags = TSMFLAG_DYNAMIC, .offset = offsetof(org_qeo_sample_simplechat_ChatMessage_t, from), .size = 0 },  
    { .tc = CDR_TYPECODE_TYPEREF, .name = "fromExtra", .offset = offsetof(org_qeo_sample_simplechat_ChatMessage_t, fromExtra), .tsm = org_qeo_UUID_type },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "message", .flags = TSMFLAG_DYNAMIC, .offset = offsetof(org_qeo_sample_simplechat_ChatMessage_t, message), .size = 0 },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "extraInfo", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_sample_simplechat_ChatMessage_t, extraInfo) },  
    { .tc = CDR_TYPECODE_OCTET },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "list", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_sample_simplechat_ChatMessage_t, list) },  
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = org_qeo_wifi_ScanListEntry_type },  
    { .tc = CDR_TYPECODE_TYPEREF, .name = "maincolor", .offset = offsetof(org_qeo_sample_simplechat_ChatMessage_t, maincolor), .tsm = my_enum_type },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "colorlist", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_sample_simplechat_ChatMessage_t, list) },  
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = my_enum_type },  
};
