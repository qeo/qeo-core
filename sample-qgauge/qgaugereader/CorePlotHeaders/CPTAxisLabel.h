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

#import "CPTDefinitions.h"

@class CPTLayer;
@class CPTTextStyle;

@interface CPTAxisLabel : NSObject<NSCoding> {
    @private
    CPTLayer *contentLayer;
    CGFloat offset;
    CGFloat rotation;
    CPTAlignment alignment;
    NSDecimal tickLocation;
}

@property (nonatomic, readwrite, retain) CPTLayer *contentLayer;
@property (nonatomic, readwrite, assign) CGFloat offset;
@property (nonatomic, readwrite, assign) CGFloat rotation;
@property (nonatomic, readwrite, assign) CPTAlignment alignment;
@property (nonatomic, readwrite) NSDecimal tickLocation;

/// @name Initialization
/// @{
-(id)initWithText:(NSString *)newText textStyle:(CPTTextStyle *)style;
-(id)initWithContentLayer:(CPTLayer *)layer;
/// @}

/// @name Layout
/// @{
-(void)positionRelativeToViewPoint:(CGPoint)point forCoordinate:(CPTCoordinate)coordinate inDirection:(CPTSign)direction;
-(void)positionBetweenViewPoint:(CGPoint)firstPoint andViewPoint:(CGPoint)secondPoint forCoordinate:(CPTCoordinate)coordinate inDirection:(CPTSign)direction;
/// @}

@end
