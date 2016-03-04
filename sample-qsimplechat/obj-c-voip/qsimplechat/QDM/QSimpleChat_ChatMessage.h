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

@end


