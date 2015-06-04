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

@class CPTColor;

@interface CPTShadow : NSObject<NSCoding, NSCopying, NSMutableCopying> {
    @private
    CGSize shadowOffset;
    CGFloat shadowBlurRadius;
    CPTColor *shadowColor;
}

@property (nonatomic, readonly, assign) CGSize shadowOffset;
@property (nonatomic, readonly, assign) CGFloat shadowBlurRadius;
@property (nonatomic, readonly, retain) CPTColor *shadowColor;

/// @name Factory Methods
/// @{
+(id)shadow;
/// @}

/// @name Drawing
/// @{
-(void)setShadowInContext:(CGContextRef)context;
/// @}

@end
