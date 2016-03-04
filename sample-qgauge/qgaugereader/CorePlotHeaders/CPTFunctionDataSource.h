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

#import "CPTPlot.h"

@class CPTMutablePlotRange;
@class CPTPlotRange;
@class CPTPlotSpace;

/// @file

/**
 *  @brief A function called to generate plot data in a CPTFunctionDataSource datasource.
 **/
typedef double (*CPTDataSourceFunction)(double);

@interface CPTFunctionDataSource : NSObject<CPTPlotDataSource> {
    @private
    __cpt_weak CPTPlot *dataPlot;
    CPTDataSourceFunction dataSourceFunction;
    CGFloat resolution;
    double cachedStep;
    NSUInteger dataCount;
    NSUInteger cachedCount;
    CPTMutablePlotRange *cachedPlotRange;
    CPTPlotRange *dataRange;
}

@property (nonatomic, readonly) CPTDataSourceFunction dataSourceFunction;
@property (nonatomic, readonly, cpt_weak_property) __cpt_weak CPTPlot *dataPlot;

@property (nonatomic, readwrite) CGFloat resolution;
@property (nonatomic, readwrite, retain) CPTPlotRange *dataRange;

/// @name Factory Methods
/// @{
+(id)dataSourceForPlot:(CPTPlot *)plot withFunction:(CPTDataSourceFunction)function;
/// @}

/// @name Initialization
/// @{
-(id)initForPlot:(CPTPlot *)plot withFunction:(CPTDataSourceFunction)function;
/// @}

@end
