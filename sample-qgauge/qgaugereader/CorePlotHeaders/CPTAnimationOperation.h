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

#import "CPTAnimation.h"
#import "CPTDefinitions.h"

@class CPTAnimationPeriod;

@interface CPTAnimationOperation : NSObject {
    @private
    CPTAnimationPeriod *period;
    CPTAnimationCurve animationCurve;

    id boundObject;
    SEL boundGetter;
    SEL boundSetter;

    __cpt_weak NSObject<CPTAnimationDelegate> *delegate;
}

/// @name Animation Timing
/// @{
@property (nonatomic, retain) CPTAnimationPeriod *period;
@property (nonatomic, assign) CPTAnimationCurve animationCurve;
/// @}

/// @name Animated Property
/// @{
@property (nonatomic, retain) id boundObject;
@property (nonatomic) SEL boundGetter;
@property (nonatomic) SEL boundSetter;
/// @}

/// @name Delegate
/// @{
@property (nonatomic, cpt_weak_property) __cpt_weak NSObject<CPTAnimationDelegate> *delegate;
/// @}

@end
