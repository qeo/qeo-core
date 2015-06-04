/*
 * Copyright (c) 2015 - Qeo LLC
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

#import <Foundation/Foundation.h>
#import <Qeo/Qeo.h>
#import "qeo.h"

@interface com_technicolor_wifidoctor_accesspoint_StationStats : QEOType

  /**
   * MAC address associated with station
   */
  @property (strong,nonatomic) NSString * MACAddress;
  /**
   * expressed in Mbps
   */
  @property (nonatomic) int32_t maxPhyRate;
  /**
   * expressed in dBm
   */
  @property (nonatomic) int32_t RSSIuplink;
  @property (nonatomic) float avgSpatialStreamsUplink;
  @property (nonatomic) float avgSpatialStreamsDownlink;
  @property (nonatomic) int32_t trainedPhyRateUplink;
  @property (nonatomic) int32_t trainedPhyRateDownlink;
  @property (nonatomic) int32_t dataRateUplink;
  @property (nonatomic) int32_t dataRateDownlink;
  @property (nonatomic) int32_t pctPowerSave;

@end

@interface com_technicolor_wifidoctor_accesspoint_BSSID : QEOType

  /**
   * [Key]
   * ID of the TestRequest for which these stats are published. A value of 0 indicates these are passive monitoring stats, not associated with a specific test request
   */
  @property (nonatomic) int32_t testId;
  /**
   * [Key]
   * MAC address associated with BSSID
   */
  @property (strong,nonatomic) NSString * MACAddress;
  /**
   * Reference to the Radio object this BSSID belongs to.
   */
  @property (nonatomic) int32_t radio;
  /**
   * Integer percentage
   */
  @property (nonatomic) int8_t mediumBusyIBSS;
  /**
   * Integer percentage
   */
  @property (nonatomic) int8_t mediumBusyOBSS;
  /**
   * Array of com_technicolor_wifidoctor_accesspoint_StationStats
   * statistics per associated station
   */
  @property (strong,nonatomic) NSArray * stationStats;

@end

@interface com_technicolor_wifidoctor_station_Statistics : QEOType

  /**
   * [Key]
   * ID of the TestRequest for which these stats are published. A value of 0 indicates these are passive monitoring stats, not associated with a specific test request
   */
  @property (nonatomic) int32_t testId;
  /**
   * [Key]
   */
  @property (strong,nonatomic) NSString * MACAddress;
  /**
   * Reference to the Radio object representing the station.
   */
  @property (nonatomic) int32_t radio;
  /**
   * expressed in dBm
   */
  @property (nonatomic) int32_t RSSIdownlink;

@end

@interface com_technicolor_wifidoctor_Radio : QEOType

  /**
   * [Key]
   * ID of the TestRequest for which these stats are published. A value of 0 indicates these are passive monitoring stats, not associated with a specific test request
   */
  @property (nonatomic) int32_t testId;
  /**
   * [Key]
   * 	 ID of the wifi radio. Basically a random number, assumed to be unique over the whole Qeo realm. 	 In the future, we'd probably use a UUID here but for the POC that's a bit overkill. 	
   */
  @property (nonatomic) int32_t id;
  /**
   * 	 Qeo Device ID of the device this radio belongs to. 	 Useful in the case of multiple devices that play the Access Point role within one realm. 	 Qeo provides a built-in function to retrieve this DeviceID. 	
   */
  @property (strong,nonatomic) org_qeo_DeviceId * device;
  /**
   * in MHz
   */
  @property (nonatomic) int32_t frequency;
  /**
   * Integer percentage. For Station radios, this value is probably meaningless and would be 0.
   */
  @property (nonatomic) int8_t mediumBusy;

@end

@interface com_technicolor_wifidoctor_ScanListEntry : QEOType

  @property (strong,nonatomic) NSString * BSSID;
  @property (strong,nonatomic) NSString * SSID;
  @property (strong,nonatomic) NSString * capabilities;
  /**
   * in MHz
   */
  @property (nonatomic) int32_t frequency;
  /**
   * in dBm
   */
  @property (nonatomic) int32_t level;

@end

@interface com_technicolor_wifidoctor_ScanList : QEOType

  /**
   * [Key]
   * the radio that published this scan list (can be either AP or STA)
   */
  @property (nonatomic) int32_t radio;
  /**
   * Array of com_technicolor_wifidoctor_ScanListEntry
   * the scan list entries
   */
  @property (strong,nonatomic) NSArray * list;
  /**
   * seconds since Jan 1, 1970
   */
  @property (nonatomic) int64_t timestamp;

@end

/**
 * Trigger a new scan and publication of the new scan list. ScanList entries from a previous scan for this radio will be disposed as the new list is published.
 */
@interface com_technicolor_wifidoctor_ScanListRequest : QEOType

  @property (nonatomic) int32_t radio;

@end

/**
 * A coordinator (typically the WifiDr Android app on the STA, but not necessarily) publishes a TestRequest to trigger a test between an AP and a STA. As long as the TestRequest instance lives, the test is 	 relevant and will be (eventually) carried out, or the results will 	 remain available. When the TestRequest is removed, all other traces 	 of the test (test states, results) will be removed as well.
 */
@interface com_technicolor_wifidoctor_TestRequest : QEOType

  /**
   * [Key]
   */
  @property (nonatomic) int32_t id;
  /**
   * MAC address of the transmitting node for this test
   */
  @property (strong,nonatomic) NSString * tx;
  /**
   * MAC address of the receiving node for this test
   */
  @property (strong,nonatomic) NSString * rx;
  /**
   * The test type. This is a poor man's substitute for an enumeration. Possible values are: 0: PING test 1: TX test
   */
  @property (nonatomic) int32_t type;
  /**
   * Ping parameter (1 < = x < = 15)
   */
  @property (nonatomic) int32_t count;
  /**
   * Ping parameter (0 < = x < = 20000)
   */
  @property (nonatomic) int32_t size;
  /**
   * Ping parameter (100 < = x < = 1000000)
   */
  @property (nonatomic) int32_t interval;
  /**
   * Ping parameter (1 < = x)
   */
  @property (nonatomic) int32_t timeout;
  /**
   * TX test parameter (0 < = x < = 86400)
   */
  @property (nonatomic) int32_t duration;
  /**
   * TX test parameter (64 < = x < = 2346)
   */
  @property (nonatomic) int32_t packetSize;
  /**
   * TX test parameter. Enum with possible values: 0 = AUTO 1 = CCK 2 = OFDMLEGACY 3 = OFDMMCS
   */
  @property (nonatomic) int32_t modulation;
  /**
   * TX test parameter (-1 < = x < = 32)
   */
  @property (nonatomic) int32_t rateIndex;
  /**
   * TX test parameter (0 < = x < = 15)
   */
  @property (nonatomic) int8_t priority;
  /**
   * TX test parameter
   */
  @property (nonatomic) BOOL AMPDU;

@end

@interface com_technicolor_wifidoctor_TestState : QEOType

  /**
   * [Key]
   * id of the corresponding TestRequest
   */
  @property (nonatomic) int32_t id;
  /**
   * [Key]
   * MAC address of the test participant publishing this test state
   */
  @property (strong,nonatomic) NSString * participant;
  /**
   * This should be an enum really. Possible values: 0 = QUEUED: acknowledge we've seen the test request, but it is not yet ready for execution 1 = WILLING: RX node indicates it is ready to participate in the test, waits for a COMMIT from the TX node before starting 2 = COMMIT: TX node indicates it is committed to starting the test, waits for RX node to go to TESTING before actually starting 3 = TESTING: test ongoing (for both RX and TX node) 4 = DONE: test is finished, results will be published 5 = REJECTED: node is unwilling to perform this test for some reason For tests where both TX and RX node are WifiDr-capable, we assume the following sequence of states: Coordinator TX node RX node --------------------------------------------------------- publish TestRequest QUEUED QUEUED v v WILLING COMMIT v v TESTING TESTING v v DONE v read TX node results DONE read RX node results remove TestRequest v v remove TestState remove TestState For "blind" tests (where the RX node is not WifiDr-capable), we assume the following sequence of states: Coordinator TX node ----------------------------------------- publish TestRequest QUEUED v TESTING v v DONE read TX node results remove TestRequest v remove TestState
   */
  @property (nonatomic) int32_t state;

@end


