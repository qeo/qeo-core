/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "EventWithUnbArrayOfStruct.h"

extern const DDS_TypeSupport_meta _org_qeo_test_MyStructWithPrimitives_type[]; 

const DDS_TypeSupport_meta _org_qeo_test_EventWithUnbArrayOfStruct_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.test.EventWithUnbArrayOfStruct", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 1 },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "MyUnbArrayOfStructWithPrimitives", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = _org_qeo_test_MyStructWithPrimitives_type },
};


@implementation org_qeo_test_EventWithUnbArrayOfStruct
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_test_EventWithUnbArrayOfStruct_type;
}
@end

