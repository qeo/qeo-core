/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "EventWithNestedStruct.h"

extern const DDS_TypeSupport_meta _org_qeo_test_MyStructWithPrimitives_type[]; 
extern const DDS_TypeSupport_meta _org_qeo_DeviceId_type[]; 

const DDS_TypeSupport_meta _org_qeo_test_EventWithNestedStruct_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.test.EventWithNestedStruct", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 9 },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "MyStructWithPrimitives", .tsm = _org_qeo_test_MyStructWithPrimitives_type },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "MyDeviceId", .tsm = _org_qeo_DeviceId_type },
    { .tc = CDR_TYPECODE_BOOLEAN, .name = "MyBoolean" },
    { .tc = CDR_TYPECODE_OCTET, .name = "MyByte" },
    { .tc = CDR_TYPECODE_SHORT, .name = "MyInt16" },
    { .tc = CDR_TYPECODE_LONG, .name = "MyInt32" },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "MyInt64" },
    { .tc = CDR_TYPECODE_FLOAT, .name = "MyFloat32" },
    { .tc = CDR_TYPECODE_CSTRING, .name = "MyString", .flags = TSMFLAG_DYNAMIC },
};


@implementation org_qeo_test_EventWithNestedStruct
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_test_EventWithNestedStruct_type;
}
@end

