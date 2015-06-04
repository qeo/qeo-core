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

#import "CPTBorderedLayer.h"

@class CPTAxisSet;
@class CPTPlotGroup;
@class CPTPlotArea;

@interface CPTPlotAreaFrame : CPTBorderedLayer {
    @private
    CPTPlotArea *plotArea;
}

@property (nonatomic, readonly, retain) CPTPlotArea *plotArea;
@property (nonatomic, readwrite, retain) CPTAxisSet *axisSet;
@property (nonatomic, readwrite, retain) CPTPlotGroup *plotGroup;

@end
