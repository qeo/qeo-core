/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "StateWithNestedStructKeyIsStruct.h"

extern const DDS_TypeSupport_meta _org_qeo_test_MyStructWithPrimitives_type[]; 

const DDS_TypeSupport_meta _org_qeo_test_StateWithNestedStructKeyIsStruct_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.test.StateWithNestedStructKeyIsStruct", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 8 },
    { .tc = CDR_TYPECODE_BOOLEAN, .name = "MyBoolean" },
    { .tc = CDR_TYPECODE_OCTET, .name = "MyByte" },
    { .tc = CDR_TYPECODE_SHORT, .name = "MyInt16" },
    { .tc = CDR_TYPECODE_LONG, .name = "MyInt32" },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "MyInt64" },
    { .tc = CDR_TYPECODE_FLOAT, .name = "MyFloat32" },
    { .tc = CDR_TYPECODE_CSTRING, .name = "MyString", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "MyStructWithPrimitives", .flags = TSMFLAG_KEY, .tsm = _org_qeo_test_MyStructWithPrimitives_type },
};


@implementation org_qeo_test_StateWithNestedStructKeyIsStruct
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_test_StateWithNestedStructKeyIsStruct_type;
}
@end

