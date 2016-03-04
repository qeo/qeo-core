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

#include "QNote_Wall.h"

const DDS_TypeSupport_meta org_qeo_sample_note_Wall_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.sample.note.Wall", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 2, .size = sizeof(org_qeo_sample_note_Wall_t) },  
    { .tc = CDR_TYPECODE_LONG, .name = "id", .flags = TSMFLAG_KEY, .offset = offsetof(org_qeo_sample_note_Wall_t, id) },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "description_str", .flags = TSMFLAG_DYNAMIC, .offset = offsetof(org_qeo_sample_note_Wall_t, description), .size = 0 },  
};
