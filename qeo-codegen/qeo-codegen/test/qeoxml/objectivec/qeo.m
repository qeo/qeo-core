/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "qeo.h"


const DDS_TypeSupport_meta _org_qeo_DeviceId_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.DeviceId", .flags = TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 2 },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "upper" },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "lower" },
};


@implementation org_qeo_DeviceId
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_DeviceId_type;
}
@end

