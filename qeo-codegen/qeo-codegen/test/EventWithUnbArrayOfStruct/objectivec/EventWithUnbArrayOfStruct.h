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
#import "StructWithPrimitives.h"

/**
 * struct representing an event containing an unbound array (sequence) of a struct
 */
@interface org_qeo_test_EventWithUnbArrayOfStruct : QEOType

  /**
   * Array of org_qeo_test_MyStructWithPrimitives
   */
  @property (strong,nonatomic) NSArray * MyUnbArrayOfStructWithPrimitives;

@end


