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

#import <Foundation/Foundation.h>
#import <Qeo/Qeo.h>
#import "qeo_types.h"


/**
 * NetStatMessage represents different statistics of a network interface.
 */
@interface org_qeo_sample_gauge_NetStatMessage : QEOType

  /**
   * [Key]
   * The DeviceId of the host of this network interface.
   */
  @property (strong,nonatomic) org_qeo_system_DeviceId * deviceId;
  /**
   * [Key]
   * The name of the network interface.
   */
  @property (strong,nonatomic) NSString * ifName;
  /**
   * The number of bytes received.
   */
  @property (nonatomic) int64_t bytesIn;
  /**
   * The number of packets received.
   */
  @property (nonatomic) int64_t packetsIn;
  /**
   * The number of bytes transmitted.
   */
  @property (nonatomic) int64_t bytesOut;
  /**
   * The number of packets transmitted.
   */
  @property (nonatomic) int64_t packetsOut;
  /**
   * The timestamp (in milliseconds) at which this NetStatMessage was updated.
   */
  @property (nonatomic) int64_t timestamp;

-(NSString*)getKeyString;
@end


