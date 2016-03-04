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

@class CPTPlotRange;
@class CPTFill;

@interface CPTLimitBand : NSObject<NSCoding, NSCopying> {
    @private
    CPTPlotRange *range;
    CPTFill *fill;
}

@property (nonatomic, readwrite, retain) CPTPlotRange *range;
@property (nonatomic, readwrite, retain) CPTFill *fill;

/// @name Factory Methods
/// @{
+(CPTLimitBand *)limitBandWithRange:(CPTPlotRange *)newRange fill:(CPTFill *)newFill;
/// @}

/// @name Initialization
/// @{
-(id)initWithRange:(CPTPlotRange *)newRange fill:(CPTFill *)newFill;
/// @}

@end
