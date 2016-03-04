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

@interface CPTCalendarFormatter : NSNumberFormatter {
    @private
    NSDateFormatter *dateFormatter;
    NSDate *referenceDate;
    NSCalendar *referenceCalendar;
    NSCalendarUnit referenceCalendarUnit;
}

@property (nonatomic, readwrite, retain) NSDateFormatter *dateFormatter;
@property (nonatomic, readwrite, copy) NSDate *referenceDate;
@property (nonatomic, readwrite, copy) NSCalendar *referenceCalendar;
@property (nonatomic, readwrite, assign) NSCalendarUnit referenceCalendarUnit;

/// @name Initialization
/// @{
-(id)initWithDateFormatter:(NSDateFormatter *)aDateFormatter;
/// @}

@end
