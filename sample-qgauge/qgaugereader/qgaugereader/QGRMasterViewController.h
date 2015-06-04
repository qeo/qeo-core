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

#import <UIKit/UIKit.h>

@class QGRDetailViewController;
@class CustomViewController;
#import "QGRQEoHandler.h"
#import "QGauge_NetStatMessage.h"

@interface QGRMasterViewController : UITableViewController

@property (strong, nonatomic) QGRDetailViewController *detailViewController;
@property (strong, nonatomic) CustomViewController *customTableviewController;
@property (strong, nonatomic) QGRQeoHandler *qeohandler;
@property (strong, nonatomic) NSMutableArray *deviceList;

//Updates the UI with latest data available.
-(void)updateScreenWithDevices:(org_qeo_sample_gauge_NetStatMessage *) message;
@end
