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
#import <Qeo/Qeo.h>

/**
 Class responsible of handling and managing the connection with the Background Notification Server (BGNS)
 */
@interface QeoBackgroundServiceManager : NSObject <QEOBackgroundNotificationServiceDelegate>

/**
 Initializer for the QeoBackgroundServiceManager
 
 @param pollingTime Repeat interval for the keep alive timer (Apple minimum is 600sec.)
 @param qeoAliveTime Active time (in sec.) of Qeo in the background on receival of a BGNS notification (max. 600sec.)
 @param onQeoDataAvailable Handler to be called when a BGNS notification is received (nil is allowed)
 */
-(instancetype)initWithKeepAlivePollingTime:(NSTimeInterval)pollingTime
                               qeoAliveTime:(NSTimeInterval)qeoAliveTime
                           onQeoDataHandler:(void(^)(void))onQeoDataAvailable;

// External Application triggers to be called from the App delegate
- (void)applicationDidEnterBackground:(UIApplication *)application;
- (void)applicationWillEnterForeground:(UIApplication *)application;

// Needs to be called once when the App is launched in the background but after the factory is registered
// to the background notification server. It will start the QeoBackgroundServiceManager in "Backgound
// Qeo active state", which will eventually go to the "Backgound Qeo inactive state" after a delay.
-(void)didLaunchInBackground;

@end
