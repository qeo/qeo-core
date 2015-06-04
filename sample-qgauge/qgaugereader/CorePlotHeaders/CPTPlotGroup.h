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

#import "CPTLayer.h"

@class CPTPlot;

@interface CPTPlotGroup : CPTLayer {
}

/// @name Adding and Removing Plots
/// @{
-(void)addPlot:(CPTPlot *)plot;
-(void)removePlot:(CPTPlot *)plot;
-(void)insertPlot:(CPTPlot *)plot atIndex:(NSUInteger)idx;
/// @}

@end
