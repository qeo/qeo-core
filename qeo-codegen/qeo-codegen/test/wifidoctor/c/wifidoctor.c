/*
 * Copyright (c) 2014 - Qeo LLC
 *
 * The source code form of this Qeo Open Source Project component is subject
 * to the terms of the Clear BSD license.
 *
 * You can redistribute it and/or modify it under the terms of the Clear BSD
 * License (http://directory.fsf.org/wiki/License:ClearBSD). See LICENSE file
 * for more details.
 *
 * The Qeo Open Source Project also includes third party Open Source Software.
 * See LICENSE file for more details.
 */

/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#include "wifidoctor.h"

const DDS_TypeSupport_meta com_technicolor_wifidoctor_accesspoint_StationStats_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.accesspoint.StationStats", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 10, .size = sizeof(com_technicolor_wifidoctor_accesspoint_StationStats_t) },
    { .tc = CDR_TYPECODE_CSTRING, .name = "MACAddress", .flags = TSMFLAG_DYNAMIC, .offset = offsetof(com_technicolor_wifidoctor_accesspoint_StationStats_t, MACAddress), .size = 0 },
    { .tc = CDR_TYPECODE_LONG, .name = "maxPhyRate", .offset = offsetof(com_technicolor_wifidoctor_accesspoint_StationStats_t, maxPhyRate) },
    { .tc = CDR_TYPECODE_LONG, .name = "RSSIuplink", .offset = offsetof(com_technicolor_wifidoctor_accesspoint_StationStats_t, RSSIuplink) },
    { .tc = CDR_TYPECODE_FLOAT, .name = "avgSpatialStreamsUplink", .offset = offsetof(com_technicolor_wifidoctor_accesspoint_StationStats_t, avgSpatialStreamsUplink) },
    { .tc = CDR_TYPECODE_FLOAT, .name = "avgSpatialStreamsDownlink", .offset = offsetof(com_technicolor_wifidoctor_accesspoint_StationStats_t, avgSpatialStreamsDownlink) },
    { .tc = CDR_TYPECODE_LONG, .name = "trainedPhyRateUplink", .offset = offsetof(com_technicolor_wifidoctor_accesspoint_StationStats_t, trainedPhyRateUplink) },
    { .tc = CDR_TYPECODE_LONG, .name = "trainedPhyRateDownlink", .offset = offsetof(com_technicolor_wifidoctor_accesspoint_StationStats_t, trainedPhyRateDownlink) },
    { .tc = CDR_TYPECODE_LONG, .name = "dataRateUplink", .offset = offsetof(com_technicolor_wifidoctor_accesspoint_StationStats_t, dataRateUplink) },
    { .tc = CDR_TYPECODE_LONG, .name = "dataRateDownlink", .offset = offsetof(com_technicolor_wifidoctor_accesspoint_StationStats_t, dataRateDownlink) },
    { .tc = CDR_TYPECODE_LONG, .name = "pctPowerSave", .offset = offsetof(com_technicolor_wifidoctor_accesspoint_StationStats_t, pctPowerSave) },
};


const DDS_TypeSupport_meta com_technicolor_wifidoctor_accesspoint_BSSID_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.accesspoint.BSSID", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 6, .size = sizeof(com_technicolor_wifidoctor_accesspoint_BSSID_t) },
    { .tc = CDR_TYPECODE_LONG, .name = "testId", .flags = TSMFLAG_KEY, .offset = offsetof(com_technicolor_wifidoctor_accesspoint_BSSID_t, testId) },
    { .tc = CDR_TYPECODE_CSTRING, .name = "MACAddress", .flags = TSMFLAG_DYNAMIC|TSMFLAG_KEY, .offset = offsetof(com_technicolor_wifidoctor_accesspoint_BSSID_t, MACAddress), .size = 0 },
    { .tc = CDR_TYPECODE_LONG, .name = "radio", .offset = offsetof(com_technicolor_wifidoctor_accesspoint_BSSID_t, radio) },
    { .tc = CDR_TYPECODE_OCTET, .name = "mediumBusyIBSS", .offset = offsetof(com_technicolor_wifidoctor_accesspoint_BSSID_t, mediumBusyIBSS) },
    { .tc = CDR_TYPECODE_OCTET, .name = "mediumBusyOBSS", .offset = offsetof(com_technicolor_wifidoctor_accesspoint_BSSID_t, mediumBusyOBSS) },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "stationStats", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(com_technicolor_wifidoctor_accesspoint_BSSID_t, stationStats) },
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = com_technicolor_wifidoctor_accesspoint_StationStats_type },
};


const DDS_TypeSupport_meta com_technicolor_wifidoctor_station_Statistics_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.station.Statistics", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 4, .size = sizeof(com_technicolor_wifidoctor_station_Statistics_t) },
    { .tc = CDR_TYPECODE_LONG, .name = "testId", .flags = TSMFLAG_KEY, .offset = offsetof(com_technicolor_wifidoctor_station_Statistics_t, testId) },
    { .tc = CDR_TYPECODE_CSTRING, .name = "MACAddress", .flags = TSMFLAG_DYNAMIC|TSMFLAG_KEY, .offset = offsetof(com_technicolor_wifidoctor_station_Statistics_t, MACAddress), .size = 0 },
    { .tc = CDR_TYPECODE_LONG, .name = "radio", .offset = offsetof(com_technicolor_wifidoctor_station_Statistics_t, radio) },
    { .tc = CDR_TYPECODE_LONG, .name = "RSSIdownlink", .offset = offsetof(com_technicolor_wifidoctor_station_Statistics_t, RSSIdownlink) },
};


const DDS_TypeSupport_meta com_technicolor_wifidoctor_Radio_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.Radio", .flags = TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 5, .size = sizeof(com_technicolor_wifidoctor_Radio_t) },
    { .tc = CDR_TYPECODE_LONG, .name = "testId", .flags = TSMFLAG_KEY, .offset = offsetof(com_technicolor_wifidoctor_Radio_t, testId) },
    { .tc = CDR_TYPECODE_LONG, .name = "id", .flags = TSMFLAG_KEY, .offset = offsetof(com_technicolor_wifidoctor_Radio_t, id) },
    { .tc = CDR_TYPECODE_TYPEREF, .name = "device", .offset = offsetof(com_technicolor_wifidoctor_Radio_t, device), .tsm = org_qeo_DeviceId_type },
    { .tc = CDR_TYPECODE_LONG, .name = "frequency", .offset = offsetof(com_technicolor_wifidoctor_Radio_t, frequency) },
    { .tc = CDR_TYPECODE_OCTET, .name = "mediumBusy", .offset = offsetof(com_technicolor_wifidoctor_Radio_t, mediumBusy) },
};


const DDS_TypeSupport_meta com_technicolor_wifidoctor_ScanListEntry_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.ScanListEntry", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 5, .size = sizeof(com_technicolor_wifidoctor_ScanListEntry_t) },
    { .tc = CDR_TYPECODE_CSTRING, .name = "BSSID", .flags = TSMFLAG_DYNAMIC, .offset = offsetof(com_technicolor_wifidoctor_ScanListEntry_t, BSSID), .size = 0 },
    { .tc = CDR_TYPECODE_CSTRING, .name = "SSID", .flags = TSMFLAG_DYNAMIC, .offset = offsetof(com_technicolor_wifidoctor_ScanListEntry_t, SSID), .size = 0 },
    { .tc = CDR_TYPECODE_CSTRING, .name = "capabilities", .flags = TSMFLAG_DYNAMIC, .offset = offsetof(com_technicolor_wifidoctor_ScanListEntry_t, capabilities), .size = 0 },
    { .tc = CDR_TYPECODE_LONG, .name = "frequency", .offset = offsetof(com_technicolor_wifidoctor_ScanListEntry_t, frequency) },
    { .tc = CDR_TYPECODE_LONG, .name = "level", .offset = offsetof(com_technicolor_wifidoctor_ScanListEntry_t, level) },
};


const DDS_TypeSupport_meta com_technicolor_wifidoctor_ScanList_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.ScanList", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 3, .size = sizeof(com_technicolor_wifidoctor_ScanList_t) },
    { .tc = CDR_TYPECODE_LONG, .name = "radio", .flags = TSMFLAG_KEY, .offset = offsetof(com_technicolor_wifidoctor_ScanList_t, radio) },
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "list", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(com_technicolor_wifidoctor_ScanList_t, list) },
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = com_technicolor_wifidoctor_ScanListEntry_type },
    { .tc = CDR_TYPECODE_LONGLONG, .name = "timestamp", .offset = offsetof(com_technicolor_wifidoctor_ScanList_t, timestamp) },
};


const DDS_TypeSupport_meta com_technicolor_wifidoctor_ScanListRequest_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.ScanListRequest", .flags = TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 1, .size = sizeof(com_technicolor_wifidoctor_ScanListRequest_t) },
    { .tc = CDR_TYPECODE_LONG, .name = "radio", .offset = offsetof(com_technicolor_wifidoctor_ScanListRequest_t, radio) },
};


const DDS_TypeSupport_meta com_technicolor_wifidoctor_TestRequest_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.TestRequest", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 14, .size = sizeof(com_technicolor_wifidoctor_TestRequest_t) },
    { .tc = CDR_TYPECODE_LONG, .name = "id", .flags = TSMFLAG_KEY, .offset = offsetof(com_technicolor_wifidoctor_TestRequest_t, id) },
    { .tc = CDR_TYPECODE_CSTRING, .name = "tx", .flags = TSMFLAG_DYNAMIC, .offset = offsetof(com_technicolor_wifidoctor_TestRequest_t, tx), .size = 0 },
    { .tc = CDR_TYPECODE_CSTRING, .name = "rx", .flags = TSMFLAG_DYNAMIC, .offset = offsetof(com_technicolor_wifidoctor_TestRequest_t, rx), .size = 0 },
    { .tc = CDR_TYPECODE_LONG, .name = "type", .offset = offsetof(com_technicolor_wifidoctor_TestRequest_t, type) },
    { .tc = CDR_TYPECODE_LONG, .name = "count", .offset = offsetof(com_technicolor_wifidoctor_TestRequest_t, count) },
    { .tc = CDR_TYPECODE_LONG, .name = "size", .offset = offsetof(com_technicolor_wifidoctor_TestRequest_t, size) },
    { .tc = CDR_TYPECODE_LONG, .name = "interval", .offset = offsetof(com_technicolor_wifidoctor_TestRequest_t, interval) },
    { .tc = CDR_TYPECODE_LONG, .name = "timeout", .offset = offsetof(com_technicolor_wifidoctor_TestRequest_t, timeout) },
    { .tc = CDR_TYPECODE_LONG, .name = "duration", .offset = offsetof(com_technicolor_wifidoctor_TestRequest_t, duration) },
    { .tc = CDR_TYPECODE_LONG, .name = "packetSize", .offset = offsetof(com_technicolor_wifidoctor_TestRequest_t, packetSize) },
    { .tc = CDR_TYPECODE_LONG, .name = "modulation", .offset = offsetof(com_technicolor_wifidoctor_TestRequest_t, modulation) },
    { .tc = CDR_TYPECODE_LONG, .name = "rateIndex", .offset = offsetof(com_technicolor_wifidoctor_TestRequest_t, rateIndex) },
    { .tc = CDR_TYPECODE_OCTET, .name = "priority", .offset = offsetof(com_technicolor_wifidoctor_TestRequest_t, priority) },
    { .tc = CDR_TYPECODE_BOOLEAN, .name = "AMPDU", .offset = offsetof(com_technicolor_wifidoctor_TestRequest_t, AMPDU) },
};


const DDS_TypeSupport_meta com_technicolor_wifidoctor_TestState_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "com.technicolor.wifidoctor.TestState", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 3, .size = sizeof(com_technicolor_wifidoctor_TestState_t) },
    { .tc = CDR_TYPECODE_LONG, .name = "id", .flags = TSMFLAG_KEY, .offset = offsetof(com_technicolor_wifidoctor_TestState_t, id) },
    { .tc = CDR_TYPECODE_CSTRING, .name = "participant", .flags = TSMFLAG_DYNAMIC|TSMFLAG_KEY, .offset = offsetof(com_technicolor_wifidoctor_TestState_t, participant), .size = 0 },
    { .tc = CDR_TYPECODE_LONG, .name = "state", .offset = offsetof(com_technicolor_wifidoctor_TestState_t, state) },
};

