/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "QGauge_NetStatMessage.h"

extern const DDS_TypeSupport_meta _org_qeo_system_DeviceId_type[]; 

const DDS_TypeSupport_meta _org_qeo_sample_gauge_NetStatMessage_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.sample.gauge.NetStatMessage", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 7 },  
    { .tc = CDR_TYPECODE_TYPEREF, .name = "deviceId", .flags = TSMFLAG_KEY, .tsm = _org_qeo_system_DeviceId_type },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "ifName", .flags = TSMFLAG_DYNAMIC|TSMFLAG_KEY },  
    { .tc = CDR_TYPECODE_LONGLONG, .name = "bytesIn" },  
    { .tc = CDR_TYPECODE_LONGLONG, .name = "packetsIn" },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "bytesOut" },  
    { .tc = CDR_TYPECODE_LONGLONG, .name = "packetsOut" },  
    { .tc = CDR_TYPECODE_LONGLONG, .name = "timestamp" },  
};


@implementation org_qeo_sample_gauge_NetStatMessage
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_sample_gauge_NetStatMessage_type;
}

-(NSString*)getKeyString{
    return [NSString stringWithFormat:@"%luuld", (unsigned long)[self hash]];
}
@end

