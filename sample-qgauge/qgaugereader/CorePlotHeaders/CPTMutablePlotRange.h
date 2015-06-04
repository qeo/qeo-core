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

#import "CPTPlotRange.h"

@interface CPTMutablePlotRange : CPTPlotRange {
}

/// @name Range Limits
/// @{
@property (nonatomic, readwrite) NSDecimal location;
@property (nonatomic, readwrite) NSDecimal length;
/// @}

/// @name Combining Ranges
/// @{
-(void)unionPlotRange:(CPTPlotRange *)otherRange;
-(void)intersectionPlotRange:(CPTPlotRange *)otherRange;
/// @}

/// @name Shifting Ranges
/// @{
-(void)shiftLocationToFitInRange:(CPTPlotRange *)otherRange;
-(void)shiftEndToFitInRange:(CPTPlotRange *)otherRange;
/// @}

/// @name Expanding/Contracting Ranges
/// @{
-(void)expandRangeByFactor:(NSDecimal)factor;
/// @}

@end
