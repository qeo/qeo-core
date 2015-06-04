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
#import "StructWithPrimitives.h"
#import "qeo.h"

/**
 * struct representing an event containing a nested struct
 */
@interface org_qeo_test_StateWithNestedStructKeyIsInt16 : QEOType

  @property (nonatomic) BOOL MyBoolean;
  @property (nonatomic) int8_t MyByte;
  /**
   * [Key]
   */
  @property (nonatomic) int16_t MyInt16;
  @property (nonatomic) int32_t MyInt32;
  @property (nonatomic) int64_t MyInt64;
  @property (nonatomic) float MyFloat32;
  @property (strong,nonatomic) NSString * MyString;
  @property (strong,nonatomic) org_qeo_test_MyStructWithPrimitives * MyStructWithPrimitives;
  @property (strong,nonatomic) org_qeo_DeviceId * MyDeviceId;

@end


