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
#import "QSimpleChat_ChatParticipant.h"

/**
 * A simple chat message.
 */
@interface org_qeo_sample_simplechat_ChatMessage : QEOType

  /**
   * The user sending the message.
   */
  @property (strong,nonatomic) NSString * from;
  /**
   * The message.
   */
  @property (strong,nonatomic) NSString * message;
  /**
   * A 8 bit number
   */
  @property (nonatomic) int8_t bytenumber;
  /**
   * A 16 bit number
   */
  @property (nonatomic) int16_t int16number;
  /**
   * A 32 bit number
   */
  @property (nonatomic) int32_t int32number;
  /**
   * A 64 bit number
   */
  @property (nonatomic) int64_t int64number;
  /**
   * A 32 bit float number
   */
  @property (nonatomic) float floatnumber;
  /**
   * A boolean
   */
  @property (nonatomic) BOOL somebool;
  /**
   * Some external structure
   */
  @property (strong,nonatomic) org_qeo_UUID * UUID;
  /**
   * Array of NSNumber (int32_t)
   * Some sequence of numbers
   */
  @property (strong,nonatomic) NSArray * numbersequence;
  /**
   * Array of NSString
   * Some sequence of strings
   */
  @property (strong,nonatomic) NSArray * stringsequence;
  /**
   * Array of org_qeo_UUID
   * Some sequence of UUID
   */
  @property (strong,nonatomic) NSArray * uuidsequence;

  /**
   * enum org_qeo_sample_simplechat_ChatState_t
   */
  @property (assign,nonatomic) org_qeo_sample_simplechat_ChatState_t myEnum;

@end


