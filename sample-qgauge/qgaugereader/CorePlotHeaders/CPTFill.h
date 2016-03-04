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

@class CPTGradient;
@class CPTImage;
@class CPTColor;

@interface CPTFill : NSObject<NSCopying, NSCoding> {
}

/// @name Factory Methods
/// @{
+(CPTFill *)fillWithColor:(CPTColor *)aColor;
+(CPTFill *)fillWithGradient:(CPTGradient *)aGradient;
+(CPTFill *)fillWithImage:(CPTImage *)anImage;
/// @}

/// @name Initialization
/// @{
-(id)initWithColor:(CPTColor *)aColor;
-(id)initWithGradient:(CPTGradient *)aGradient;
-(id)initWithImage:(CPTImage *)anImage;
/// @}

@end

/** @category CPTFill(AbstractMethods)
 *  @brief CPTFill abstract methods—must be overridden by subclasses
 **/
@interface CPTFill(AbstractMethods)

@property (nonatomic, readonly, getter = isOpaque) BOOL opaque;

/// @name Drawing
/// @{
-(void)fillRect:(CGRect)rect inContext:(CGContextRef)context;
-(void)fillPathInContext:(CGContextRef)context;
/// @}

@end
