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

#import "CPTAnnotationHostLayer.h"

@class CPTLineStyle;
@class CPTFill;

@interface CPTBorderedLayer : CPTAnnotationHostLayer {
    @private
    CPTLineStyle *borderLineStyle;
    CPTFill *fill;
    BOOL inLayout;
}

/// @name Drawing
/// @{
@property (nonatomic, readwrite, copy) CPTLineStyle *borderLineStyle;
@property (nonatomic, readwrite, copy) CPTFill *fill;
/// @}

/// @name Layout
/// @{
@property (nonatomic, readwrite) BOOL inLayout;
/// @}

/// @name Drawing
/// @{
-(void)renderBorderedLayerAsVectorInContext:(CGContextRef)context;
/// @}

@end
