/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "QSimpleChat_ChatMessage.h"


const DDS_TypeSupport_meta _org_qeo_sample_simplechat_ChatMessage_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.sample.simplechat.ChatMessage", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 2 },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "from", .flags = TSMFLAG_DYNAMIC },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "message", .flags = TSMFLAG_DYNAMIC },  
};


@implementation org_qeo_sample_simplechat_ChatMessage
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _org_qeo_sample_simplechat_ChatMessage_type;
}

@end

