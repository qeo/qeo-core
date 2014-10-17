/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "TypeWithRecurringStructs.h"


const DDS_TypeSupport_meta _org_qeo_dynamic_qdm_test_Substruct1_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.dynamic.qdm.test.Substruct1", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 2 },
    { .tc = CDR_TYPECODE_CSTRING, .name = "msubstring", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_LONG, .name = "msubint32" },
};

extern const DDS_TypeSupport_meta _org_qeo_dynamic_qdm_test_Substruct1_type[]; 

const DDS_TypeSupport_meta _org_qeo_dynamic_qdm_test_Substruct2_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.dynamic.qdm.test.Substruct2", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 3 },
    { .tc = CDR_TYPECODE_SHORT, .name = "msubshort" },
    { .tc = CDR_TYPECODE_CSTRING, .name = "msubstring", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "msubstruct1", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = _org_qeo_dynamic_qdm_test_Substruct1_type },
};

extern const DDS_TypeSupport_meta _org_qeo_dynamic_qdm_test_Substruct2_type[]; 
extern const DDS_TypeSupport_meta _org_qeo_dynamic_qdm_test_Substruct1_type[]; 

const DDS_TypeSupport_meta _org_qeo_dynamic_qdm_test_Substruct3_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.dynamic.qdm.test.Substruct3", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 4 },
    { .tc = CDR_TYPECODE_CSTRING, .name = "msubstring", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "msubstruct2", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = _org_qeo_dynamic_qdm_test_Substruct2_type },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "msubstruct1", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = _org_qeo_dynamic_qdm_test_Substruct1_type },
    { .tc = CDR_TYPECODE_FLOAT, .name = "msubfloat" },
};

extern const DDS_TypeSupport_meta _org_qeo_dynamic_qdm_test_Substruct1_type[]; 
extern const DDS_TypeSupport_meta _org_qeo_dynamic_qdm_test_Substruct3_type[]; 
extern const DDS_TypeSupport_meta _org_qeo_dynamic_qdm_test_Substruct2_type[]; 

const DDS_TypeSupport_meta _org_qeo_dynamic_qdm_test_House_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.dynamic.qdm.test.House", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 5 },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "msubstruct1", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = _org_qeo_dynamic_qdm_test_Substruct1_type },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "msubstruct3", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = _org_qeo_dynamic_qdm_test_Substruct3_type },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "msubstruct2", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = _org_qeo_dynamic_qdm_test_Substruct2_type },
    { .tc = CDR_TYPECODE_FLOAT, .name = "mfloat32" },
    { .tc = CDR_TYPECODE_CSTRING, .name = "mstring", .flags = TSMFLAG_DYNAMIC },
};


@implementation org_qeo_dynamic_qdm_test_Substruct1
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_dynamic_qdm_test_Substruct1_type;
}
@end

@implementation org_qeo_dynamic_qdm_test_Substruct2
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_dynamic_qdm_test_Substruct2_type;
}
@end

@implementation org_qeo_dynamic_qdm_test_Substruct3
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_dynamic_qdm_test_Substruct3_type;
}
@end

@implementation org_qeo_dynamic_qdm_test_House
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_dynamic_qdm_test_House_type;
}
@end

