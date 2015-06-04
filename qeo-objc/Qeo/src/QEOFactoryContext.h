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

#import <Foundation/Foundation.h>
#import "Qeo.h"
#import "platform_api.h"

@class QEOFactory;

@interface QEOFactoryContext : NSObject <QEOFactoryContextDelegate>

- (instancetype)initWithFactory:(QEOFactory *)factory;

- (void)registerToQeoWith:(NSString*)otc url:(NSString*)url;
- (void)cancelRegistration;
- (void)closeRegistrationDialog;
- (void)closeContext;

- (qeo_util_retcode_t)registrationParametersNeeded:(qeo_platform_security_context_t)sec_context;

- (void)security_status_update:(qeo_platform_security_context_t)sec_context
                         state:(qeo_platform_security_state)state
                        reason:(qeo_platform_security_state_reason)reason;

- (qeo_util_retcode_t)remoteRegistrationConfirmationNeeded:(qeo_platform_security_context_t)sec_context
                                                 realmName:(NSString *)realmName
                                                       url:(NSURL *)url;

@end
