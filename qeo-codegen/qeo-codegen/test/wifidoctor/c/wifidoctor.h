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

#ifndef QDM_WIFIDOCTOR_H_
#define QDM_WIFIDOCTOR_H_

#include <qeo/types.h>
#include "qeo.h"

#ifdef __cplusplus
extern "C"
{
#endif


typedef struct {
  /**
   * MAC address associated with station
   */
    char * MACAddress;
  /**
   * expressed in Mbps
   */
    int32_t maxPhyRate;
  /**
   * expressed in dBm
   */
    int32_t RSSIuplink;
    float avgSpatialStreamsUplink;
    float avgSpatialStreamsDownlink;
    int32_t trainedPhyRateUplink;
    int32_t trainedPhyRateDownlink;
    int32_t dataRateUplink;
    int32_t dataRateDownlink;
    int32_t pctPowerSave;
} com_technicolor_wifidoctor_accesspoint_StationStats_t;
extern const DDS_TypeSupport_meta com_technicolor_wifidoctor_accesspoint_StationStats_type[];

DDS_SEQUENCE(com_technicolor_wifidoctor_accesspoint_StationStats_t, com_technicolor_wifidoctor_accesspoint_BSSID_stationStats_seq);
typedef struct {
  /**
   * [Key]
   * ID of the TestRequest for which these stats are published. A value of 0 indicates these are passive monitoring stats, not associated with a specific test request
   */
    int32_t testId;
  /**
   * [Key]
   * MAC address associated with BSSID
   */
    char * MACAddress;
  /**
   * Reference to the Radio object this BSSID belongs to.
   */
    int32_t radio;
  /**
   * Integer percentage
   */
    int8_t mediumBusyIBSS;
  /**
   * Integer percentage
   */
    int8_t mediumBusyOBSS;
  /**
   * statistics per associated station
   */
    com_technicolor_wifidoctor_accesspoint_BSSID_stationStats_seq stationStats;
} com_technicolor_wifidoctor_accesspoint_BSSID_t;
extern const DDS_TypeSupport_meta com_technicolor_wifidoctor_accesspoint_BSSID_type[];

typedef struct {
  /**
   * [Key]
   * ID of the TestRequest for which these stats are published. A value of 0 indicates these are passive monitoring stats, not associated with a specific test request
   */
    int32_t testId;
  /**
   * [Key]
   */
    char * MACAddress;
  /**
   * Reference to the Radio object representing the station.
   */
    int32_t radio;
  /**
   * expressed in dBm
   */
    int32_t RSSIdownlink;
} com_technicolor_wifidoctor_station_Statistics_t;
extern const DDS_TypeSupport_meta com_technicolor_wifidoctor_station_Statistics_type[];

typedef struct {
  /**
   * [Key]
   * ID of the TestRequest for which these stats are published. A value of 0 indicates these are passive monitoring stats, not associated with a specific test request
   */
    int32_t testId;
  /**
   * [Key]
   * 	 ID of the wifi radio. Basically a random number, assumed to be unique over the whole Qeo realm. 	 In the future, we'd probably use a UUID here but for the POC that's a bit overkill. 	
   */
    int32_t id;
  /**
   * 	 Qeo Device ID of the device this radio belongs to. 	 Useful in the case of multiple devices that play the Access Point role within one realm. 	 Qeo provides a built-in function to retrieve this DeviceID. 	
   */
    org_qeo_DeviceId_t device;
  /**
   * in MHz
   */
    int32_t frequency;
  /**
   * Integer percentage. For Station radios, this value is probably meaningless and would be 0.
   */
    int8_t mediumBusy;
} com_technicolor_wifidoctor_Radio_t;
extern const DDS_TypeSupport_meta com_technicolor_wifidoctor_Radio_type[];

typedef struct {
    char * BSSID;
    char * SSID;
    char * capabilities;
  /**
   * in MHz
   */
    int32_t frequency;
  /**
   * in dBm
   */
    int32_t level;
} com_technicolor_wifidoctor_ScanListEntry_t;
extern const DDS_TypeSupport_meta com_technicolor_wifidoctor_ScanListEntry_type[];

DDS_SEQUENCE(com_technicolor_wifidoctor_ScanListEntry_t, com_technicolor_wifidoctor_ScanList_list_seq);
typedef struct {
  /**
   * [Key]
   * the radio that published this scan list (can be either AP or STA)
   */
    int32_t radio;
  /**
   * the scan list entries
   */
    com_technicolor_wifidoctor_ScanList_list_seq list;
  /**
   * seconds since Jan 1, 1970
   */
    int64_t timestamp;
} com_technicolor_wifidoctor_ScanList_t;
extern const DDS_TypeSupport_meta com_technicolor_wifidoctor_ScanList_type[];

/**
 * Trigger a new scan and publication of the new scan list. ScanList entries from a previous scan for this radio will be disposed as the new list is published.
 */
typedef struct {
    int32_t radio;
} com_technicolor_wifidoctor_ScanListRequest_t;
extern const DDS_TypeSupport_meta com_technicolor_wifidoctor_ScanListRequest_type[];

/**
 * A coordinator (typically the WifiDr Android app on the STA, but not necessarily) publishes a TestRequest to trigger a test between an AP and a STA. As long as the TestRequest instance lives, the test is 	 relevant and will be (eventually) carried out, or the results will 	 remain available. When the TestRequest is removed, all other traces 	 of the test (test states, results) will be removed as well.
 */
typedef struct {
  /**
   * [Key]
   */
    int32_t id;
  /**
   * MAC address of the transmitting node for this test
   */
    char * tx;
  /**
   * MAC address of the receiving node for this test
   */
    char * rx;
  /**
   * The test type. This is a poor man's substitute for an enumeration. Possible values are: 0: PING test 1: TX test
   */
    int32_t type;
  /**
   * Ping parameter (1 < = x < = 15)
   */
    int32_t count;
  /**
   * Ping parameter (0 < = x < = 20000)
   */
    int32_t size;
  /**
   * Ping parameter (100 < = x < = 1000000)
   */
    int32_t interval;
  /**
   * Ping parameter (1 < = x)
   */
    int32_t timeout;
  /**
   * TX test parameter (0 < = x < = 86400)
   */
    int32_t duration;
  /**
   * TX test parameter (64 < = x < = 2346)
   */
    int32_t packetSize;
  /**
   * TX test parameter. Enum with possible values: 0 = AUTO 1 = CCK 2 = OFDMLEGACY 3 = OFDMMCS
   */
    int32_t modulation;
  /**
   * TX test parameter (-1 < = x < = 32)
   */
    int32_t rateIndex;
  /**
   * TX test parameter (0 < = x < = 15)
   */
    int8_t priority;
  /**
   * TX test parameter
   */
    qeo_boolean_t AMPDU;
} com_technicolor_wifidoctor_TestRequest_t;
extern const DDS_TypeSupport_meta com_technicolor_wifidoctor_TestRequest_type[];

typedef struct {
  /**
   * [Key]
   * id of the corresponding TestRequest
   */
    int32_t id;
  /**
   * [Key]
   * MAC address of the test participant publishing this test state
   */
    char * participant;
  /**
   * This should be an enum really. Possible values: 0 = QUEUED: acknowledge we've seen the test request, but it is not yet ready for execution 1 = WILLING: RX node indicates it is ready to participate in the test, waits for a COMMIT from the TX node before starting 2 = COMMIT: TX node indicates it is committed to starting the test, waits for RX node to go to TESTING before actually starting 3 = TESTING: test ongoing (for both RX and TX node) 4 = DONE: test is finished, results will be published 5 = REJECTED: node is unwilling to perform this test for some reason For tests where both TX and RX node are WifiDr-capable, we assume the following sequence of states: Coordinator TX node RX node --------------------------------------------------------- publish TestRequest QUEUED QUEUED v v WILLING COMMIT v v TESTING TESTING v v DONE v read TX node results DONE read RX node results remove TestRequest v v remove TestState remove TestState For "blind" tests (where the RX node is not WifiDr-capable), we assume the following sequence of states: Coordinator TX node ----------------------------------------- publish TestRequest QUEUED v TESTING v v DONE read TX node results remove TestRequest v remove TestState
   */
    int32_t state;
} com_technicolor_wifidoctor_TestState_t;
extern const DDS_TypeSupport_meta com_technicolor_wifidoctor_TestState_type[];

#ifdef __cplusplus
}
#endif

#endif /* QDM_WIFIDOCTOR_H_ */

