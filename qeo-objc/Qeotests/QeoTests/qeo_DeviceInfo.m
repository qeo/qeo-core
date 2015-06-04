/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "qeo_DeviceInfo.h"


const DDS_TypeSupport_meta _org_qeo_system_RealmInfo_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.system.RealmInfo", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 4 },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "realmId" },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "deviceId" },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "userId" },
    { .tc = CDR_TYPECODE_CSTRING, .name = "url", .flags = TSMFLAG_DYNAMIC },
};

extern const DDS_TypeSupport_meta _org_qeo_system_DeviceId_type[]; 
extern const DDS_TypeSupport_meta _org_qeo_system_RealmInfo_type[]; 

const DDS_TypeSupport_meta _org_qeo_system_DeviceInfo_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.system.DeviceInfo", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 10 },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "deviceId", .flags = TSMFLAG_KEY, .tsm = _org_qeo_system_DeviceId_type },
    { .tc = CDR_TYPECODE_CSTRING, .name = "manufacturer", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "modelName", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "productClass", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "serialNumber", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "hardwareVersion", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "softwareVersion", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "userFriendlyName", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "configURL", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "realmInfo", .tsm = _org_qeo_system_RealmInfo_type },
};


@implementation org_qeo_system_RealmInfo
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_system_RealmInfo_type;
}
@end

@implementation org_qeo_system_DeviceInfo
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_system_DeviceInfo_type;
}
@end

