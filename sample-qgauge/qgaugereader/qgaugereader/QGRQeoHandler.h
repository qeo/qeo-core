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
#import <Qeo/Qeo.h>
#import "QGauge_NetStatMessage.h"
#import "qeo_DeviceInfo.h"
#import "QGRIfaceSpeedData.h"
#import "QGRCachedGraphData.h"
@interface QGRQeoHandler : NSObject <QEOStateReaderDelegate,QEOStateChangeReaderDelegate,UIAlertViewDelegate>

//Manages all QEO communication(initialization of QEO factory, state readers.)
-(void)setupQeoCommunication;

// Will be called when the App enters the background
-(void)willResign:(NSNotification *)notification;

// Will be called when the App resumes in the foreground
-(void)willContinue:(NSNotification *)notification;

//Calculates traffic speed of different network interafce.
- (QGRIfaceSpeedData *)computeSpeed:(org_qeo_sample_gauge_NetStatMessage *)msg;

//Timer method to refrsh UI periodically.
-(void)getQEOUpdate:(NSTimer *)timer;

@property (strong, nonatomic) NSManagedObjectContext *managedObjectContext;
@property (strong, nonatomic) NSMutableArray *deviceList;
@property (strong, nonatomic) NSMutableArray *deviceInfo;
@property (strong, nonatomic) NSMutableDictionary *nsmCache;
@property (strong, nonatomic) NSMutableDictionary *speedData;
@property (strong, nonatomic) NSMutableDictionary *calculatedData;
@property (strong, nonatomic) NSMutableDictionary *ifacePlottedData;
@property BOOL qeoRequestStarted; // atomic

@end
