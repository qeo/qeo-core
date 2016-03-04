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

#import "CPTDefinitions.h"

@class CPTPlot;
@class CPTTextStyle;

@interface CPTLegendEntry : NSObject<NSCoding> {
    @private
    __cpt_weak CPTPlot *plot;
    NSUInteger index;
    NSUInteger row;
    NSUInteger column;
    CPTTextStyle *textStyle;
}

/// @name Plot Info
/// @{
@property (nonatomic, readwrite, cpt_weak_property) __cpt_weak CPTPlot *plot;
@property (nonatomic, readwrite, assign) NSUInteger index;
/// @}

/// @name Formatting
/// @{
@property (nonatomic, readwrite, retain) CPTTextStyle *textStyle;
/// @}

/// @name Layout
/// @{
@property (nonatomic, readwrite, assign) NSUInteger row;
@property (nonatomic, readwrite, assign) NSUInteger column;
@property (nonatomic, readonly, assign) CGSize titleSize;
/// @}

/// @name Drawing
/// @{
-(void)drawTitleInRect:(CGRect)rect inContext:(CGContextRef)context scale:(CGFloat)scale;
/// @}

@end
