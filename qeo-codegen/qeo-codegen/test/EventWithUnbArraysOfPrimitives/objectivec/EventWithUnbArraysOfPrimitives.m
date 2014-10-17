/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "EventWithUnbArraysOfPrimitives.h"


const DDS_TypeSupport_meta _org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.test.EventWithUnbArraysOfPrimitivesStruct", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 8 },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfBoolean", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_BOOLEAN },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfByte", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_OCTET },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfByte1", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_OCTET },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfInt16", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_SHORT },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfInt32", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_LONG },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfInt64", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_LONGLONG },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfFloat32", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_FLOAT },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfString", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_CSTRING },
};


@implementation org_qeo_test_EventWithUnbArraysOfPrimitivesStruct
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_type;
}
@end

