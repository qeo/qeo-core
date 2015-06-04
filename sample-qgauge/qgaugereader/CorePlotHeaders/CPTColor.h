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

@interface CPTColor : NSObject<NSCopying, NSCoding> {
    @private
    CGColorRef cgColor;
}

@property (nonatomic, readonly, assign) CGColorRef cgColor;
@property (nonatomic, readonly, getter = isOpaque) BOOL opaque;

/// @name Factory Methods
/// @{
+(CPTColor *)clearColor;
+(CPTColor *)whiteColor;
+(CPTColor *)lightGrayColor;
+(CPTColor *)grayColor;
+(CPTColor *)darkGrayColor;
+(CPTColor *)blackColor;
+(CPTColor *)redColor;
+(CPTColor *)greenColor;
+(CPTColor *)blueColor;
+(CPTColor *)cyanColor;
+(CPTColor *)yellowColor;
+(CPTColor *)magentaColor;
+(CPTColor *)orangeColor;
+(CPTColor *)purpleColor;
+(CPTColor *)brownColor;

+(CPTColor *)colorWithCGColor:(CGColorRef)newCGColor;
+(CPTColor *)colorWithComponentRed:(CGFloat)red green:(CGFloat)green blue:(CGFloat)blue alpha:(CGFloat)alpha;
+(CPTColor *)colorWithGenericGray:(CGFloat)gray;
/// @}

/// @name Initialization
/// @{
-(id)initWithCGColor:(CGColorRef)cgColor;
-(id)initWithComponentRed:(CGFloat)red green:(CGFloat)green blue:(CGFloat)blue alpha:(CGFloat)alpha;

-(CPTColor *)colorWithAlphaComponent:(CGFloat)alpha;
/// @}

@end
