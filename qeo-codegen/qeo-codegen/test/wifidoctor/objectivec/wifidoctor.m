/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#define XTYPES_USED
#import <Qeo/dds/dds_tsm.h>
#import "wifidoctor.h"


const DDS_TypeSupport_meta _com_technicolor_wifidoctor_accesspoint_StationStats_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.accesspoint.StationStats", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 10 },
    { .tc = CDR_TYPECODE_CSTRING, .name = "MACAddress", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_LONG, .name = "maxPhyRate" },
    { .tc = CDR_TYPECODE_LONG, .name = "RSSIuplink" },
    { .tc = CDR_TYPECODE_FLOAT, .name = "avgSpatialStreamsUplink" },
    { .tc = CDR_TYPECODE_FLOAT, .name = "avgSpatialStreamsDownlink" },
    { .tc = CDR_TYPECODE_LONG, .name = "trainedPhyRateUplink" },
    { .tc = CDR_TYPECODE_LONG, .name = "trainedPhyRateDownlink" },
    { .tc = CDR_TYPECODE_LONG, .name = "dataRateUplink" },
    { .tc = CDR_TYPECODE_LONG, .name = "dataRateDownlink" },
    { .tc = CDR_TYPECODE_LONG, .name = "pctPowerSave" },
};

extern const DDS_TypeSupport_meta _com_technicolor_wifidoctor_accesspoint_StationStats_type[]; 

const DDS_TypeSupport_meta _com_technicolor_wifidoctor_accesspoint_BSSID_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.accesspoint.BSSID", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 6 },
    { .tc = CDR_TYPECODE_LONG, .name = "testId", .flags = TSMFLAG_KEY },
    { .tc = CDR_TYPECODE_CSTRING, .name = "MACAddress", .flags = TSMFLAG_DYNAMIC|TSMFLAG_KEY },
    { .tc = CDR_TYPECODE_LONG, .name = "radio" },
    { .tc = CDR_TYPECODE_OCTET, .name = "mediumBusyIBSS" },
    { .tc = CDR_TYPECODE_OCTET, .name = "mediumBusyOBSS" },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "stationStats", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = _com_technicolor_wifidoctor_accesspoint_StationStats_type },
};


const DDS_TypeSupport_meta _com_technicolor_wifidoctor_station_Statistics_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.station.Statistics", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 4 },
    { .tc = CDR_TYPECODE_LONG, .name = "testId", .flags = TSMFLAG_KEY },
    { .tc = CDR_TYPECODE_CSTRING, .name = "MACAddress", .flags = TSMFLAG_DYNAMIC|TSMFLAG_KEY },
    { .tc = CDR_TYPECODE_LONG, .name = "radio" },
    { .tc = CDR_TYPECODE_LONG, .name = "RSSIdownlink" },
};

extern const DDS_TypeSupport_meta _org_qeo_DeviceId_type[]; 

const DDS_TypeSupport_meta _com_technicolor_wifidoctor_Radio_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.Radio", .flags = TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 5 },
    { .tc = CDR_TYPECODE_LONG, .name = "testId", .flags = TSMFLAG_KEY },
    { .tc = CDR_TYPECODE_LONG, .name = "id", .flags = TSMFLAG_KEY },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "device", .tsm = _org_qeo_DeviceId_type },
    { .tc = CDR_TYPECODE_LONG, .name = "frequency" },
    { .tc = CDR_TYPECODE_OCTET, .name = "mediumBusy" },
};


const DDS_TypeSupport_meta _com_technicolor_wifidoctor_ScanListEntry_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.ScanListEntry", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 5 },
    { .tc = CDR_TYPECODE_CSTRING, .name = "BSSID", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "SSID", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "capabilities", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_LONG, .name = "frequency" },
    { .tc = CDR_TYPECODE_LONG, .name = "level" },
};

extern const DDS_TypeSupport_meta _com_technicolor_wifidoctor_ScanListEntry_type[]; 

const DDS_TypeSupport_meta _com_technicolor_wifidoctor_ScanList_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.ScanList", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 3 },
    { .tc = CDR_TYPECODE_LONG, .name = "radio", .flags = TSMFLAG_KEY },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "list", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE },
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = _com_technicolor_wifidoctor_ScanListEntry_type },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "timestamp" },
};


const DDS_TypeSupport_meta _com_technicolor_wifidoctor_ScanListRequest_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.ScanListRequest", .flags = TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 1 },
    { .tc = CDR_TYPECODE_LONG, .name = "radio" },
};


const DDS_TypeSupport_meta _com_technicolor_wifidoctor_TestRequest_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.TestRequest", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 14 },
    { .tc = CDR_TYPECODE_LONG, .name = "id", .flags = TSMFLAG_KEY },
    { .tc = CDR_TYPECODE_CSTRING, .name = "tx", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_CSTRING, .name = "rx", .flags = TSMFLAG_DYNAMIC },
    { .tc = CDR_TYPECODE_LONG, .name = "type" },
    { .tc = CDR_TYPECODE_LONG, .name = "count" },
    { .tc = CDR_TYPECODE_LONG, .name = "size" },
    { .tc = CDR_TYPECODE_LONG, .name = "interval" },
    { .tc = CDR_TYPECODE_LONG, .name = "timeout" },
    { .tc = CDR_TYPECODE_LONG, .name = "duration" },
    { .tc = CDR_TYPECODE_LONG, .name = "packetSize" },
    { .tc = CDR_TYPECODE_LONG, .name = "modulation" },
    { .tc = CDR_TYPECODE_LONG, .name = "rateIndex" },
    { .tc = CDR_TYPECODE_OCTET, .name = "priority" },
    { .tc = CDR_TYPECODE_BOOLEAN, .name = "AMPDU" },
};


const DDS_TypeSupport_meta _com_technicolor_wifidoctor_TestState_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.TestState", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 3 },
    { .tc = CDR_TYPECODE_LONG, .name = "id", .flags = TSMFLAG_KEY },
    { .tc = CDR_TYPECODE_CSTRING, .name = "participant", .flags = TSMFLAG_DYNAMIC|TSMFLAG_KEY },
    { .tc = CDR_TYPECODE_LONG, .name = "state" },
};


@implementation com_technicolor_wifidoctor_accesspoint_StationStats
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _com_technicolor_wifidoctor_accesspoint_StationStats_type;
}
@end

@implementation com_technicolor_wifidoctor_accesspoint_BSSID
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _com_technicolor_wifidoctor_accesspoint_BSSID_type;
}
@end

@implementation com_technicolor_wifidoctor_station_Statistics
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _com_technicolor_wifidoctor_station_Statistics_type;
}
@end

@implementation com_technicolor_wifidoctor_Radio
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _com_technicolor_wifidoctor_Radio_type;
}
@end

@implementation com_technicolor_wifidoctor_ScanListEntry
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _com_technicolor_wifidoctor_ScanListEntry_type;
}
@end

@implementation com_technicolor_wifidoctor_ScanList
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _com_technicolor_wifidoctor_ScanList_type;
}
@end

@implementation com_technicolor_wifidoctor_ScanListRequest
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _com_technicolor_wifidoctor_ScanListRequest_type;
}
@end

@implementation com_technicolor_wifidoctor_TestRequest
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _com_technicolor_wifidoctor_TestRequest_type;
}
@end

@implementation com_technicolor_wifidoctor_TestState
{
}

+ (const DDS_TypeSupport_meta *)getMetaType {
    return _com_technicolor_wifidoctor_TestState_type;
}
@end

