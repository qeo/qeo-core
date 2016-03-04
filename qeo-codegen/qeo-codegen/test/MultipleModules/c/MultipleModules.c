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

#include "MultipleModules.h"

const DDS_TypeSupport_meta module_first_MyStructWithPrimitives_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "module.first.MyStructWithPrimitives", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 7, .size = sizeof(module_first_MyStructWithPrimitives_t) },
    { .tc = CDR_TYPECODE_BOOLEAN, .name = "MyBoolean", .offset = offsetof(module_first_MyStructWithPrimitives_t, MyBoolean) },
    { .tc = CDR_TYPECODE_OCTET, .name = "MyByte", .offset = offsetof(module_first_MyStructWithPrimitives_t, MyByte) },
    { .tc = CDR_TYPECODE_SHORT, .name = "MyInt16", .offset = offsetof(module_first_MyStructWithPrimitives_t, MyInt16) },
    { .tc = CDR_TYPECODE_LONG, .name = "MyInt32", .offset = offsetof(module_first_MyStructWithPrimitives_t, MyInt32) },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "MyInt64", .offset = offsetof(module_first_MyStructWithPrimitives_t, MyInt64) },
    { .tc = CDR_TYPECODE_FLOAT, .name = "MyFloat32", .offset = offsetof(module_first_MyStructWithPrimitives_t, MyFloat32) },
    { .tc = CDR_TYPECODE_CSTRING, .name = "MyString", .flags = TSMFLAG_DYNAMIC, .offset = offsetof(module_first_MyStructWithPrimitives_t, MyString), .size = 0 },
};


const DDS_TypeSupport_meta module_first_Class1_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "module.first.Class1", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 3, .size = sizeof(module_first_Class1_t) },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "upper", .offset = offsetof(module_first_Class1_t, upper) },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfStructWithPrimitives", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(module_first_Class1_t, MyUnbArrayOfStructWithPrimitives) },
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = module_first_MyStructWithPrimitives_type },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "lower", .offset = offsetof(module_first_Class1_t, lower) },
};


const DDS_TypeSupport_meta module_second_MyStructWithPrimitives_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "module.second.MyStructWithPrimitives", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 7, .size = sizeof(module_second_MyStructWithPrimitives_t) },
    { .tc = CDR_TYPECODE_BOOLEAN, .name = "MyBoolean", .offset = offsetof(module_second_MyStructWithPrimitives_t, MyBoolean) },
    { .tc = CDR_TYPECODE_OCTET, .name = "MyByte", .offset = offsetof(module_second_MyStructWithPrimitives_t, MyByte) },
    { .tc = CDR_TYPECODE_SHORT, .name = "MyInt16", .offset = offsetof(module_second_MyStructWithPrimitives_t, MyInt16) },
    { .tc = CDR_TYPECODE_LONG, .name = "MyInt32", .offset = offsetof(module_second_MyStructWithPrimitives_t, MyInt32) },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "MyInt64", .offset = offsetof(module_second_MyStructWithPrimitives_t, MyInt64) },
    { .tc = CDR_TYPECODE_FLOAT, .name = "MyFloat32", .offset = offsetof(module_second_MyStructWithPrimitives_t, MyFloat32) },
    { .tc = CDR_TYPECODE_CSTRING, .name = "MyString", .flags = TSMFLAG_DYNAMIC, .offset = offsetof(module_second_MyStructWithPrimitives_t, MyString), .size = 0 },
};


const DDS_TypeSupport_meta module_second_Class1_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "module.second.Class1", .flags = TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 2, .size = sizeof(module_second_Class1_t) },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "upper2", .offset = offsetof(module_second_Class1_t, upper2) },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "lower2", .offset = offsetof(module_second_Class1_t, lower2) },
};

