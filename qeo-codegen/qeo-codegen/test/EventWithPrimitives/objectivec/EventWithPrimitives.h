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

/**
 * This struct represents an event containing only primitive types.
 */
@interface org_qeo_test_EventWithPrimitives : QEOType

  @property (nonatomic) BOOL MyBoolean;
  @property (nonatomic) int8_t MyByte;
  /**
   * This is an int16
   */
  @property (nonatomic) int16_t MyInt16;
  @property (nonatomic) int32_t MyInt32;
  /**
   * This is an int64
   */
  @property (nonatomic) int64_t MyInt64;
  @property (nonatomic) float MyFloat32;
  @property (strong,nonatomic) NSString * MyString;

@end


