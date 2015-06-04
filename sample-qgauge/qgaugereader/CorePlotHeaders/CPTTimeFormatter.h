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

/// @file

@interface CPTTimeFormatter : NSNumberFormatter {
    @private
    NSDateFormatter *dateFormatter;
    NSDate *referenceDate;
}

@property (nonatomic, readwrite, retain) NSDateFormatter *dateFormatter;
@property (nonatomic, readwrite, copy) NSDate *referenceDate;

/// @name Initialization
/// @{
-(id)initWithDateFormatter:(NSDateFormatter *)aDateFormatter;
/// @}

@end
