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
 * Possible chat participant states.
 */
typedef NS_ENUM(int32_t, org_qeo_sample_simplechat_ChatState_t) {
  /**
   * The user is available for chatting.
   */
  ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_AVAILABLE,
  /**
   * The user is idle.
   */
  ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_IDLE,
  /**
   * The user is busy and will not respond to messages.
   */
  ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_BUSY,
  /**
   * The user is unavailable.
   */
  ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_AWAY
};

/**
 * A participant in a chat session.
 */
@interface org_qeo_sample_simplechat_ChatParticipant : QEOType

  /**
   * [Key]
   * The name of the participant.
   */
  @property (strong,nonatomic) NSString * name;
  /**
   * The state of the participant.
   */
  @property (assign,nonatomic) org_qeo_sample_simplechat_ChatState_t state;

@end


