/*
 * Copyright (c) 2016 - Qeo LLC
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

#include "cwifi.h"

const DDS_TypeSupport_meta org_qeo_wifi_Radio_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.wifi.Radio", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 4, .size = sizeof(org_qeo_wifi_Radio_t) },  
    { .tc = CDR_TYPECODE_TYPEREF, .name = "id", .flags = TSMFLAG_KEY, .offset = offsetof(org_qeo_wifi_Radio_t, id), .tsm = org_qeo_UUID_type },  
    { .tc = CDR_TYPECODE_TYPEREF, .name = "device", .offset = offsetof(org_qeo_wifi_Radio_t, device), .tsm = org_qeo_system_DeviceId_type },  
    { .tc = CDR_TYPECODE_LONG, .name = "channel", .offset = offsetof(org_qeo_wifi_Radio_t, channel) },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "capabilities", .flags = TSMFLAG_DYNAMIC, .offset = offsetof(org_qeo_wifi_Radio_t, capabilities), .size = 0 },  
};

const DDS_TypeSupport_meta org_qeo_wifi_Interface_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.wifi.Interface", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 5, .size = sizeof(org_qeo_wifi_Interface_t) },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "MACAddress", .flags = TSMFLAG_DYNAMIC|TSMFLAG_KEY, .offset = offsetof(org_qeo_wifi_Interface_t, MACAddress), .size = 0 },  
    { .tc = CDR_TYPECODE_TYPEREF, .name = "radio", .offset = offsetof(org_qeo_wifi_Interface_t, radio), .tsm = org_qeo_UUID_type },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "SSID", .flags = TSMFLAG_DYNAMIC, .offset = offsetof(org_qeo_wifi_Interface_t, SSID), .size = 0 },  
    { .tc = CDR_TYPECODE_LONG, .name = "type", .offset = offsetof(org_qeo_wifi_Interface_t, type) },  
    { .tc = CDR_TYPECODE_BOOLEAN, .name = "enabled", .offset = offsetof(org_qeo_wifi_Interface_t, enabled) },  
};

const DDS_TypeSupport_meta org_qeo_wifi_AssociatedStation_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.wifi.AssociatedStation", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 6, .size = sizeof(org_qeo_wifi_AssociatedStation_t) },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "MACAddress", .flags = TSMFLAG_DYNAMIC|TSMFLAG_KEY, .offset = offsetof(org_qeo_wifi_AssociatedStation_t, MACAddress), .size = 0 },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "BSSID", .flags = TSMFLAG_DYNAMIC|TSMFLAG_KEY, .offset = offsetof(org_qeo_wifi_AssociatedStation_t, BSSID), .size = 0 },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "capabilities", .flags = TSMFLAG_DYNAMIC, .offset = offsetof(org_qeo_wifi_AssociatedStation_t, capabilities), .size = 0 },  
    { .tc = CDR_TYPECODE_BOOLEAN, .name = "associated", .offset = offsetof(org_qeo_wifi_AssociatedStation_t, associated) },  
    { .tc = CDR_TYPECODE_LONGLONG, .name = "lastSeen", .offset = offsetof(org_qeo_wifi_AssociatedStation_t, lastSeen) },  
    { .tc = CDR_TYPECODE_LONG, .name = "maxNegotiatedPhyRate", .offset = offsetof(org_qeo_wifi_AssociatedStation_t, maxNegotiatedPhyRate) },  
};

const DDS_TypeSupport_meta org_qeo_wifi_ScanListEntry_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.wifi.ScanListEntry", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 4, .size = sizeof(org_qeo_wifi_ScanListEntry_t) },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "BSSID", .flags = TSMFLAG_DYNAMIC, .offset = offsetof(org_qeo_wifi_ScanListEntry_t, BSSID), .size = 0 },  
    { .tc = CDR_TYPECODE_CSTRING, .name = "SSID", .flags = TSMFLAG_DYNAMIC, .offset = offsetof(org_qeo_wifi_ScanListEntry_t, SSID), .size = 0 },  
    { .tc = CDR_TYPECODE_LONG, .name = "channel", .offset = offsetof(org_qeo_wifi_ScanListEntry_t, channel) },  
    { .tc = CDR_TYPECODE_LONG, .name = "RSSI", .offset = offsetof(org_qeo_wifi_ScanListEntry_t, RSSI) },  
};

const DDS_TypeSupport_meta org_qeo_wifi_ScanList_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.wifi.ScanList", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_KEY|TSMFLAG_MUTABLE, .nelem = 3, .size = sizeof(org_qeo_wifi_ScanList_t) },  
    { .tc = CDR_TYPECODE_TYPEREF, .name = "radio", .flags = TSMFLAG_KEY, .offset = offsetof(org_qeo_wifi_ScanList_t, radio), .tsm = org_qeo_UUID_type },  
    { .tc = CDR_TYPECODE_SEQUENCE, .name = "list", .flags = TSMFLAG_DYNAMIC|TSMFLAG_GENID|TSMFLAG_MUTABLE, .offset = offsetof(org_qeo_wifi_ScanList_t, list) },  
    { .tc = CDR_TYPECODE_TYPEREF, .tsm = org_qeo_wifi_ScanListEntry_type },  
    { .tc = CDR_TYPECODE_LONGLONG, .name = "timestamp", .offset = offsetof(org_qeo_wifi_ScanList_t, timestamp) },  
};

const DDS_TypeSupport_meta org_qeo_wifi_ScanRequest_type[] = {
    { .tc = CDR_TYPECODE_STRUCT, .name = "org.qeo.wifi.ScanRequest", .flags = TSMFLAG_GENID|TSMFLAG_MUTABLE, .nelem = 1, .size = sizeof(org_qeo_wifi_ScanRequest_t) },  
    { .tc = CDR_TYPECODE_TYPEREF, .name = "radio", .offset = offsetof(org_qeo_wifi_ScanRequest_t, radio), .tsm = org_qeo_UUID_type },  
};
