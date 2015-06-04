/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "qeo_RegistrationRequest.h"

extern const DDS_TypeSupport_meta _org_qeo_system_DeviceId_type[]; 

const DDS_TypeSupport_meta _org_qeo_system_RegistrationRequest_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.system.RegistrationRequest", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 10 },  
    { .tc = CDR_TYPECODE_TYPEREF, .name = "deviceId", .flags = TSMFLAG_KEY, .tsm = _org_qeo_system_DeviceId_type },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "rsaPublicKey", .flags = TSMFLAG_DYNAMIC|TSMFLAG_KEY },  
    { .tc = CDR_TYPECODE_SHORT, .name = "version", .flags = TSMFLAG_KEY },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "manufacturer", .flags = TSMFLAG_DYNAMIC|TSMFLAG_KEY },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "modelName", .flags = TSMFLAG_DYNAMIC|TSMFLAG_KEY },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "userFriendlyName", .flags = TSMFLAG_DYNAMIC|TSMFLAG_KEY },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "userName", .flags = TSMFLAG_DYNAMIC|TSMFLAG_KEY },  
    { .tc = CDR_TYPECODE_SHORT, .name = "registrationStatus" },  
    { .tc = CDR_TYPECODE_SHORT, .name = "errorCode" },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "errorMessage", .flags = TSMFLAG_DYNAMIC },  
};


@implementation org_qeo_system_RegistrationRequest
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_system_RegistrationRequest_type;
}

@end

