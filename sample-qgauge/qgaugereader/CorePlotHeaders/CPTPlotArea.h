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
#import "CPTGraph.h"
#import "CPTLayer.h"

@class CPTAxis;
@class CPTAxisLabelGroup;
@class CPTAxisSet;
@class CPTGridLineGroup;
@class CPTPlotGroup;
@class CPTLineStyle;
@class CPTFill;

@interface CPTPlotArea : CPTAnnotationHostLayer {
    @private
    CPTGridLineGroup *minorGridLineGroup;
    CPTGridLineGroup *majorGridLineGroup;
    CPTAxisSet *axisSet;
    CPTPlotGroup *plotGroup;
    CPTAxisLabelGroup *axisLabelGroup;
    CPTAxisLabelGroup *axisTitleGroup;
    CPTFill *fill;
    NSArray *topDownLayerOrder;
    CPTGraphLayerType *bottomUpLayerOrder;
    BOOL updatingLayers;
}

/// @name Layers
/// @{
@property (nonatomic, readwrite, retain) CPTGridLineGroup *minorGridLineGroup;
@property (nonatomic, readwrite, retain) CPTGridLineGroup *majorGridLineGroup;
@property (nonatomic, readwrite, retain) CPTAxisSet *axisSet;
@property (nonatomic, readwrite, retain) CPTPlotGroup *plotGroup;
@property (nonatomic, readwrite, retain) CPTAxisLabelGroup *axisLabelGroup;
@property (nonatomic, readwrite, retain) CPTAxisLabelGroup *axisTitleGroup;
/// @}

/// @name Layer Ordering
/// @{
@property (nonatomic, readwrite, retain) NSArray *topDownLayerOrder;
/// @}

/// @name Decorations
/// @{
@property (nonatomic, readwrite, copy) CPTLineStyle *borderLineStyle;
@property (nonatomic, readwrite, copy) CPTFill *fill;
/// @}

/// @name Axis Set Layer Management
/// @{
-(void)updateAxisSetLayersForType:(CPTGraphLayerType)layerType;
-(void)setAxisSetLayersForType:(CPTGraphLayerType)layerType;
-(unsigned)sublayerIndexForAxis:(CPTAxis *)axis layerType:(CPTGraphLayerType)layerType;
/// @}

@end
