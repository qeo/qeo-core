/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "enums.h"


const DDS_TypeSupport_meta _org_qeo_test_EnumName_type[] = {
    { .tc = CDR_TYPECODE_ENUM, .name = "org.qeo.test.EnumName", .nelem = 2 },
    { .name = "ENUM1", .label = ORG_QEO_TEST_ENUMNAME_ENUM1 },
    { .name = "ENUM2", .label = ORG_QEO_TEST_ENUMNAME_ENUM2 },
};


const DDS_TypeSupport_meta _org_qeo_test_EnumNameBis_type[] = {
    { .tc = CDR_TYPECODE_ENUM, .name = "org.qeo.test.EnumNameBis", .nelem = 2 },
    { .name = "ENUM1BIS", .label = ORG_QEO_TEST_ENUMNAMEBIS_ENUM1BIS },
    { .name = "ENUM2BIS", .label = ORG_QEO_TEST_ENUMNAMEBIS_ENUM2BIS },
};

extern const DDS_TypeSupport_meta _org_qeo_test_EnumName_type[]; 

const DDS_TypeSupport_meta _org_qeo_test_MyStructWithEnums_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.test.MyStructWithEnums", .flags = TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 4 },
    { .tc = CDR_TYPECODE_BOOLEAN, .name = "MyBoolean" },
    { .tc = CDR_TYPECODE_OCTET, .name = "MyByte" },
    { .tc = CDR_TYPECODE_SHORT, .name = "MyInt16" },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "MyEnum", .tsm = _org_qeo_test_EnumName_type },
};

extern const DDS_TypeSupport_meta _org_qeo_test_EnumNameBis_type[]; 

const DDS_TypeSupport_meta _org_qeo_test_MyStructWithEnumsBis_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.test.MyStructWithEnumsBis", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 3 },
    { .tc = CDR_TYPECODE_FLOAT, .name = "MyFloat32" },
    { .tc = CDR_TYPECODE_CSTRING, .name = "MyString", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "MyEnumBis", .tsm = _org_qeo_test_EnumNameBis_type },
};


@implementation org_qeo_test_MyStructWithEnums
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_test_MyStructWithEnums_type;
}
@end

@implementation org_qeo_test_MyStructWithEnumsBis
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_test_MyStructWithEnumsBis_type;
}
@end

