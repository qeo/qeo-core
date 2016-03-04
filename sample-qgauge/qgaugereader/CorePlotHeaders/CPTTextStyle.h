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

#include "CPTTextStylePlatformSpecific.h"

@class CPTColor;

@interface CPTTextStyle : NSObject<NSCoding, NSCopying, NSMutableCopying> {
    @protected
    NSString *fontName;
    CGFloat fontSize;
    CPTColor *color;
    CPTTextAlignment textAlignment;
    NSLineBreakMode lineBreakMode;
}

@property (readonly, copy, nonatomic) NSString *fontName;
@property (readonly, assign, nonatomic) CGFloat fontSize;
@property (readonly, copy, nonatomic) CPTColor *color;
@property (readonly, assign, nonatomic) CPTTextAlignment textAlignment;
@property (readonly, assign, nonatomic) NSLineBreakMode lineBreakMode;

/// @name Factory Methods
/// @{
+(id)textStyle;
/// @}

@end

#pragma mark -

/** @category CPTTextStyle(CPTPlatformSpecificTextStyleExtensions)
 *  @brief Platform-specific extensions to CPTTextStyle.
 **/
@interface CPTTextStyle(CPTPlatformSpecificTextStyleExtensions)

@property (readonly, copy, nonatomic) NSDictionary *attributes;

/// @name Factory Methods
/// @{
+(id)textStyleWithAttributes:(NSDictionary *)attributes;
/// @}

@end

#pragma mark -

/** @category NSString(CPTTextStyleExtensions)
 *  @brief NSString extensions for drawing styled text.
 **/
@interface NSString(CPTTextStyleExtensions)

/// @name Measurement
/// @{
-(CGSize)sizeWithTextStyle:(CPTTextStyle *)style;
/// @}

/// @name Drawing
/// @{
-(void)drawInRect:(CGRect)rect withTextStyle:(CPTTextStyle *)style inContext:(CGContextRef)context;
/// @}

@end
