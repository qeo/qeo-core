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

@class CPTAxis;

@interface CPTGridLines : CPTLayer {
    @private
    __cpt_weak CPTAxis *axis;
    BOOL major;
}

@property (nonatomic, readwrite, cpt_weak_property) __cpt_weak CPTAxis *axis;
@property (nonatomic, readwrite) BOOL major;

@end
