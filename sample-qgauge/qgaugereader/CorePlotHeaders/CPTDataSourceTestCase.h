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

#import "CPTTestCase.h"

#import "CPTPlot.h"

@class CPTMutablePlotRange;

@interface CPTDataSourceTestCase : CPTTestCase<CPTPlotDataSource> {
    @private
    NSArray *xData, *yData;
    NSMutableArray *plots;

    NSUInteger nRecords;
}

@property (copy, readwrite) NSArray *xData;
@property (copy, readwrite) NSArray *yData;
@property (assign, readwrite) NSUInteger nRecords;
@property (retain, readonly) CPTMutablePlotRange *xRange;
@property (retain, readonly) CPTMutablePlotRange *yRange;
@property (retain, readwrite) NSMutableArray *plots;

-(void)buildData;

-(void)addPlot:(CPTPlot *)newPlot;

@end
