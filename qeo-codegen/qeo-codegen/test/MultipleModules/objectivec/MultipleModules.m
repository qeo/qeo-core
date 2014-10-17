/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "MultipleModules.h"


const DDS_TypeSupport_meta _module_first_MyStructWithPrimitives_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "module.first.MyStructWithPrimitives", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 7 },
    { .tc = CDR_TYPECODE_BOOLEAN, .name = "MyBoolean" },
    { .tc = CDR_TYPECODE_OCTET, .name = "MyByte" },
    { .tc = CDR_TYPECODE_SHORT, .name = "MyInt16" },
    { .tc = CDR_TYPECODE_LONG, .name = "MyInt32" },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "MyInt64" },
    { .tc = CDR_TYPECODE_FLOAT, .name = "MyFloat32" },
    { .tc = CDR_TYPECODE_CSTRING, .name = "MyString", .flags = TSMFLAG_DYNAMIC },
};

extern const DDS_TypeSupport_meta _module_first_MyStructWithPrimitives_type[]; 

const DDS_TypeSupport_meta _module_first_Class1_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "module.first.Class1", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 3 },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "upper" },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfStructWithPrimitives", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = _module_first_MyStructWithPrimitives_type },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "lower" },
};


const DDS_TypeSupport_meta _module_second_MyStructWithPrimitives_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "module.second.MyStructWithPrimitives", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 7 },
    { .tc = CDR_TYPECODE_BOOLEAN, .name = "MyBoolean" },
    { .tc = CDR_TYPECODE_OCTET, .name = "MyByte" },
    { .tc = CDR_TYPECODE_SHORT, .name = "MyInt16" },
    { .tc = CDR_TYPECODE_LONG, .name = "MyInt32" },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "MyInt64" },
    { .tc = CDR_TYPECODE_FLOAT, .name = "MyFloat32" },
    { .tc = CDR_TYPECODE_CSTRING, .name = "MyString", .flags = TSMFLAG_DYNAMIC },
};


const DDS_TypeSupport_meta _module_second_Class1_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "module.second.Class1", .flags = TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 2 },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "upper2" },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "lower2" },
};


@implementation module_first_MyStructWithPrimitives
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _module_first_MyStructWithPrimitives_type;
}
@end

@implementation module_first_Class1
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _module_first_Class1_type;
}
@end

@implementation module_second_MyStructWithPrimitives
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _module_second_MyStructWithPrimitives_type;
}
@end

@implementation module_second_Class1
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _module_second_Class1_type;
}
@end

