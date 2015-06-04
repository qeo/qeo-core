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

@interface CPTImage : NSObject<NSCoding, NSCopying> {
    @private
    CGImageRef image;
    CGFloat scale;
    BOOL tiled;
    BOOL tileAnchoredToContext;
}

@property (nonatomic, readwrite, assign) CGImageRef image;
@property (nonatomic, readwrite, assign) CGFloat scale;
@property (nonatomic, readwrite, assign, getter = isTiled) BOOL tiled;
@property (nonatomic, readwrite, assign) BOOL tileAnchoredToContext;
@property (nonatomic, readonly, getter = isOpaque) BOOL opaque;

/// @name Factory Methods
/// @{
+(CPTImage *)imageWithCGImage:(CGImageRef)anImage scale:(CGFloat)newScale;
+(CPTImage *)imageWithCGImage:(CGImageRef)anImage;
+(CPTImage *)imageForPNGFile:(NSString *)path;
/// @}

/// @name Initialization
/// @{
-(id)initWithCGImage:(CGImageRef)anImage scale:(CGFloat)newScale;
-(id)initWithCGImage:(CGImageRef)anImage;
-(id)initForPNGFile:(NSString *)path;
/// @}

/// @name Drawing
/// @{
-(void)drawInRect:(CGRect)rect inContext:(CGContextRef)context;
/// @}

@end
