/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "EventWithNestedStructInSameQDM.h"


const DDS_TypeSupport_meta _org_qeo_test_MyInnerStructWithPrimitives_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.test.MyInnerStructWithPrimitives", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 7 },
    { .tc = CDR_TYPECODE_BOOLEAN, .name = "MyBoolean" },
    { .tc = CDR_TYPECODE_OCTET, .name = "MyByte" },
    { .tc = CDR_TYPECODE_SHORT, .name = "MyInt16" },
    { .tc = CDR_TYPECODE_LONG, .name = "MyInt32" },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "MyInt64" },
    { .tc = CDR_TYPECODE_FLOAT, .name = "MyFloat32" },
    { .tc = CDR_TYPECODE_CSTRING, .name = "MyString", .flags = TSMFLAG_DYNAMIC },
};

extern const DDS_TypeSupport_meta _org_qeo_test_MyInnerStructWithPrimitives_type[]; 

const DDS_TypeSupport_meta _org_qeo_test_EventWithNestedStruct_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.test.EventWithNestedStruct", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 8 },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "MyStructWithPrimitives", .tsm = _org_qeo_test_MyInnerStructWithPrimitives_type },
    { .tc = CDR_TYPECODE_BOOLEAN, .name = "MyBoolean" },
    { .tc = CDR_TYPECODE_OCTET, .name = "MyByte" },
    { .tc = CDR_TYPECODE_SHORT, .name = "MyInt16" },
    { .tc = CDR_TYPECODE_LONG, .name = "MyInt32" },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "MyInt64" },
    { .tc = CDR_TYPECODE_FLOAT, .name = "MyFloat32" },
    { .tc = CDR_TYPECODE_CSTRING, .name = "MyString", .flags = TSMFLAG_DYNAMIC },
};


@implementation org_qeo_test_MyInnerStructWithPrimitives
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_test_MyInnerStructWithPrimitives_type;
}
@end

@implementation org_qeo_test_EventWithNestedStruct
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_test_EventWithNestedStruct_type;
}
@end

