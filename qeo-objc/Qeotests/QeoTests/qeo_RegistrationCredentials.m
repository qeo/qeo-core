/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "qeo_RegistrationCredentials.h"

extern const DDS_TypeSupport_meta _org_qeo_system_DeviceId_type[]; 

const DDS_TypeSupport_meta _org_qeo_system_RegistrationCredentials_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.system.RegistrationCredentials", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 5 },  
    { .tc = CDR_TYPECODE_TYPEREF, .name = "deviceId", .flags = TSMFLAG_KEY, .tsm = _org_qeo_system_DeviceId_type },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "requestRSAPublicKey", .flags = TSMFLAG_DYNAMIC|TSMFLAG_KEY },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "encryptedOtc", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },  
    { .tc = CDR_TYPECODE_OCTET },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "url", .flags = TSMFLAG_DYNAMIC },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "realmName", .flags = TSMFLAG_DYNAMIC },  
};


@implementation org_qeo_system_RegistrationCredentials
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_system_RegistrationCredentials_type;
}

@end

