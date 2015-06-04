/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "QSimpleChat_ChatParticipant.h"


const DDS_TypeSupport_meta _org_qeo_sample_simplechat_ChatState_type[] = {
    { .tc = CDR_TYPECODE_ENUM, .name = "org.qeo.sample.simplechat.ChatState", .nelem = 4 },
    { .name = "AVAILABLE", .label = ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_AVAILABLE },
    { .name = "IDLE", .label = ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_IDLE },
    { .name = "BUSY", .label = ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_BUSY },
    { .name = "AWAY", .label = ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_AWAY },
};

extern const DDS_TypeSupport_meta _org_qeo_sample_simplechat_ChatState_type[]; 

const DDS_TypeSupport_meta _org_qeo_sample_simplechat_ChatParticipant_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.sample.simplechat.ChatParticipant", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 2 },
    { .tc = CDR_TYPECODE_CSTRING, .name = "name", .flags = TSMFLAG_DYNAMIC|TSMFLAG_KEY },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "state", .tsm = _org_qeo_sample_simplechat_ChatState_type },
};


@implementation org_qeo_sample_simplechat_ChatParticipant
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_sample_simplechat_ChatParticipant_type;
}
@end

