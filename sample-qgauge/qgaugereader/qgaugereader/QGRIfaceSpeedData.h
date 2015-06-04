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
#import "qeo_types.h"
@interface QGRIfaceSpeedData : NSObject
@property org_qeo_system_DeviceId *deviceId;
@property NSString *ifName;
@property int64_t kbytesIn;
@property int64_t kbytesOut;
@property int64_t pktsIn;
@property int64_t pktsOut;

@end
