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


#include "myenum.h"

const DDS_TypeSupport_meta my_enum_type[] = {
    { .tc = CDR_TYPECODE_ENUM, .name = "my_enum", .flags = TSMFLAG_MUTABLE, .nelem = 3, .size = sizeof(my_enum_t) },  
    { .name = "RED", .label = 0 },
    { .name = "GREEN", .label = 1 },
    { .name = "BLUE", .label = 2 },
};
