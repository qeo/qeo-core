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

@interface CPTColorSpace : NSObject<NSCoding> {
    @private
    CGColorSpaceRef cgColorSpace;
}

@property (nonatomic, readonly, assign) CGColorSpaceRef cgColorSpace;

/// @name Factory Methods
/// @{
+(CPTColorSpace *)genericRGBSpace;
/// @}

/// @name Initialization
/// @{
-(id)initWithCGColorSpace:(CGColorSpaceRef)colorSpace;
/// @}

@end
