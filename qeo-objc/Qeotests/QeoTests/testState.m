/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "testState.h"

extern const DDS_TypeSupport_meta _org_qeo_system_DeviceId_type[]; 
extern const DDS_TypeSupport_meta _org_qeo_UUID_type[]; 
extern const DDS_TypeSupport_meta _org_qeo_UUID_type[]; 

const DDS_TypeSupport_meta _org_qeo_test_TestState_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.test.TestState", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 12 },  
    { .tc = CDR_TYPECODE_TYPEREF, .name = "deviceId", .flags = TSMFLAG_KEY, .tsm = _org_qeo_system_DeviceId_type },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "manufacturer", .flags = TSMFLAG_DYNAMIC },  
    { .tc = CDR_TYPECODE_OCTET, .name = "bytenumber" },  
    { .tc = CDR_TYPECODE_SHORT, .name = "int16number" },  
    { .tc = CDR_TYPECODE_LONG, .name = "int32number" },  
    { .tc = CDR_TYPECODE_LONGLONG, .name = "int64number" },  
    { .tc = CDR_TYPECODE_FLOAT, .name = "floatnumber" },  
    { .tc = CDR_TYPECODE_BOOLEAN, .name = "somebool" },  
    { .tc = CDR_TYPECODE_TYPEREF, .name = "UUID", .tsm = _org_qeo_UUID_type },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "numbersequence", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },  
    { .tc = CDR_TYPECODE_LONG },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "stringsequence", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },  
    { .tc = CDR_TYPECODE_CSTRING },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "uuidsequence", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },  
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = _org_qeo_UUID_type },  
};


@implementation org_qeo_test_TestState
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_test_TestState_type;
}

@end

