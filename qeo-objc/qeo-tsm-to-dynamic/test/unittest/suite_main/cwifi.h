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

#ifndef QDM_CWIFI_H_
#define QDM_CWIFI_H_

#include <qeo/types.h>
#include "qeo_types.h"


typedef struct {
    /**
     * [Key] ID of the wifi radio.
     */
    org_qeo_UUID_t id;
    /**
     * 			Qeo Device ID of the device this radio belongs to. 			Useful in the case of multiple devices that play the Access Point role within one realm. 			Qeo provides a built-in function to retrieve this DeviceID. 	
     */
    org_qeo_system_DeviceId_t device;
    /**
     * 			enumeration, possible values: 1-14 (2.4GHz channels) and 			36,40,44,48,52,56,60,64,100,104,108,112,116,120,124,128,132,136,140,149,153,157,161 (5GHz channels). 		
     */
    int32_t channel;
    /**
     * 			802.11standard, MIMOconfiguration TX x RX:SS, SignalBandwidth (MHz), Supported Bands, SGIcapable (y or no). 			E.g. "a/b/g/n,2x3:2,20/40,2/5,y". 		
     */
    char * capabilities;
} org_qeo_wifi_Radio_t;
extern const DDS_TypeSupport_meta org_qeo_wifi_Radio_type[];

typedef struct {
    /**
     * [Key] MAC address associated with the interface. If the interface is of type "access point", this is the BSSID. If the interface is of type "client", this is the client's MAC address.
     */
    char * MACAddress;
    /**
     * Reference to the Radio object his Interface is attached to.
     */
    org_qeo_UUID_t radio;
    /**
     * For access points, their network name. For clients, the network they are attached to.
     */
    char * SSID;
    /**
     * enum. Values are 0 = access point 1 = client (station)
     */
    int32_t type;
    /**
     * 			Is this interface currently enabled?
     */
    qeo_boolean_t enabled;
} org_qeo_wifi_Interface_t;
extern const DDS_TypeSupport_meta org_qeo_wifi_Interface_type[];

/**
 * Note: seperate topic to allow for specific eventing (new STA associated).
 */
typedef struct {
    /**
     * [Key] the station's MAC address
     */
    char * MACAddress;
    /**
     * [Key] 			BSSID the station is associated with. Refers to Interface.MACAddress. 			Note: key, since STA can connect (at different time) to different BSSIDs.
     */
    char * BSSID;
    /**
     * 			802.11standard, MIMOconfiguration TX x RX:SS, SignalBandwidth, Supported Bands, SGIcapable. 			E.g. "a/b/g/n,2x3:2,20/40,2/5,y".
     */
    char * capabilities;
    /**
     * is this station currently associated?
     */
    qeo_boolean_t associated;
    /**
     * timestamp (seconds since 1970) when the station was last seen as associated
     */
    int64_t lastSeen;
    /**
     * in kbps
     */
    int32_t maxNegotiatedPhyRate;
} org_qeo_wifi_AssociatedStation_t;
extern const DDS_TypeSupport_meta org_qeo_wifi_AssociatedStation_type[];

typedef struct {
    char * BSSID;
    char * SSID;
    /**
     * enum. Same values as Radio.channel
     */
    int32_t channel;
    /**
     * in dBm
     */
    int32_t RSSI;
} org_qeo_wifi_ScanListEntry_t;
extern const DDS_TypeSupport_meta org_qeo_wifi_ScanListEntry_type[];

DDS_SEQUENCE(org_qeo_wifi_ScanListEntry_t, org_qeo_wifi_ScanList_list_seq);
/**
 * 		To be published at start-up, periodically(period left free; 		for AP after period scans as defined in ACS config), on request. 	
 */
typedef struct {
    /**
     * [Key] the radio that published this scan list (can be either AP or STA)
     */
    org_qeo_UUID_t radio;
    /**
     * the scan list entries
     */
    org_qeo_wifi_ScanList_list_seq list;
    /**
     * seconds since Jan 1, 1970
     */
    int64_t timestamp;
} org_qeo_wifi_ScanList_t;
extern const DDS_TypeSupport_meta org_qeo_wifi_ScanList_type[];

/**
 * 		Trigger a new scan and publication of the new scan list. ScanList entries from a previous scan for this radio will be removed as the new list is published. 		Important to note that a scan on AP side interrupts service (for a few seconds). 		Please forget about the previous remark that a scan triggers a new AutoChannelSelection: 		it is possible to scan without triggering AutoChannelSelection.
 */
typedef struct {
    org_qeo_UUID_t radio;
} org_qeo_wifi_ScanRequest_t;
extern const DDS_TypeSupport_meta org_qeo_wifi_ScanRequest_type[];


#endif /* QDM_CWIFI_H_ */

