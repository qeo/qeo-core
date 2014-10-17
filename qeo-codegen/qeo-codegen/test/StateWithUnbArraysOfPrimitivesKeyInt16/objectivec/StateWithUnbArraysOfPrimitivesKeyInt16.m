/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "StateWithUnbArraysOfPrimitivesKeyInt16.h"


const DDS_TypeSupport_meta _org_qeo_test_StateWithUnbArraysOfPrimitivesKeyInt16_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.test.StateWithUnbArraysOfPrimitivesKeyInt16", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 7 },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfBoolean", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_BOOLEAN },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfByte", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_OCTET },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfInt16", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE },
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


@implementation org_qeo_test_StateWithUnbArraysOfPrimitivesKeyInt16
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_test_StateWithUnbArraysOfPrimitivesKeyInt16_type;
}
@end

