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

#import <Foundation/Foundation.h>

#import "qeo/factory.h"
#import "Qeo.h"

@interface QEOFactory()
@property (nonatomic, readonly) qeo_factory_t *factory;
@property (nonatomic, weak) id <QEOFactoryDelegate> delegate;

// BGNS: Only for StateReaders and StateChangeReaders
-(void)registerBGNSEnabledEntity:(QEOEntity *)reader;
-(void)unregisterBGNSEnabledEntity:(QEOEntity *)reader;
@end
