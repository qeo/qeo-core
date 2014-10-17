/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "enumsMultipleModules.h"


const DDS_TypeSupport_meta _org_qeo_test_EnumName_type[] = {
    { .tc = CDR_TYPECODE_ENUM, .name = "org.qeo.test.EnumName", .nelem = 2 },
    { .name = "ENUM1", .label = ORG_QEO_TEST_ENUMNAME_ENUM1 },
    { .name = "ENUM2", .label = ORG_QEO_TEST_ENUMNAME_ENUM2 },
};


const DDS_TypeSupport_meta _org_qeo_test_EnumNameBis_type[] = {
    { .tc = CDR_TYPECODE_ENUM, .name = "org.qeo.test.EnumNameBis", .nelem = 2 },
    { .name = "ENUM1", .label = ORG_QEO_TEST_ENUMNAMEBIS_ENUM1 },
    { .name = "ENUM2", .label = ORG_QEO_TEST_ENUMNAMEBIS_ENUM2 },
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
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.test.MyStructWithEnumsBis", .flags = TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 3 },
    { .tc = CDR_TYPECODE_LONG, .name = "MyInt32" },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "MyInt64" },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "MyEnumBis", .tsm = _org_qeo_test_EnumNameBis_type },
};


const DDS_TypeSupport_meta _org_qeo_testo_EnumName_type[] = {
    { .tc = CDR_TYPECODE_ENUM, .name = "org.qeo.testo.EnumName", .nelem = 2 },
    { .name = "ENUM3", .label = ORG_QEO_TESTO_ENUMNAME_ENUM3 },
    { .name = "ENUM4", .label = ORG_QEO_TESTO_ENUMNAME_ENUM4 },
};


const DDS_TypeSupport_meta _org_qeo_testo_EnumNameBis_type[] = {
    { .tc = CDR_TYPECODE_ENUM, .name = "org.qeo.testo.EnumNameBis", .nelem = 2 },
    { .name = "ENUM1", .label = ORG_QEO_TESTO_ENUMNAMEBIS_ENUM1 },
    { .name = "ENUM2", .label = ORG_QEO_TESTO_ENUMNAMEBIS_ENUM2 },
};

extern const DDS_TypeSupport_meta _org_qeo_testo_EnumName_type[]; 

const DDS_TypeSupport_meta _org_qeo_testo_MyStructWithEnums_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.testo.MyStructWithEnums", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 3 },
    { .tc = CDR_TYPECODE_FLOAT, .name = "MyFloat32" },
    { .tc = CDR_TYPECODE_CSTRING, .name = "MyString", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "MyEnum", .tsm = _org_qeo_testo_EnumName_type },
};

extern const DDS_TypeSupport_meta _org_qeo_testo_EnumNameBis_type[]; 

const DDS_TypeSupport_meta _org_qeo_testo_MyStructWithEnumsBis_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.testo.MyStructWithEnumsBis", .flags = TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 4 },
    { .tc = CDR_TYPECODE_BOOLEAN, .name = "MyBooleanBis" },
    { .tc = CDR_TYPECODE_OCTET, .name = "MyByteBis" },
    { .tc = CDR_TYPECODE_SHORT, .name = "MyInt16Bis" },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "MyEnumBis", .tsm = _org_qeo_testo_EnumNameBis_type },
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

@implementation org_qeo_testo_MyStructWithEnums
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_testo_MyStructWithEnums_type;
}
@end

@implementation org_qeo_testo_MyStructWithEnumsBis
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_testo_MyStructWithEnumsBis_type;
}
@end

