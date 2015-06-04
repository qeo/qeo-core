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

/* This file defines structure-QGRCustomCell used to display in/out data transferred for selected interface. */
#import <UIKit/UIKit.h>

@interface QGRCustomCell : UITableViewCell

@property (weak, nonatomic) IBOutlet UILabel *ifaceLabel;
@property (weak, nonatomic) IBOutlet UILabel *inBytesLabel;
@property (weak, nonatomic) IBOutlet UILabel *outBytesaceLabel;
@end
