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

@interface CPTConstraints : NSObject<NSCoding, NSCopying> {
}

/// @name Factory Methods
/// @{
+(CPTConstraints *)constraintWithLowerOffset:(CGFloat)newOffset;
+(CPTConstraints *)constraintWithUpperOffset:(CGFloat)newOffset;
+(CPTConstraints *)constraintWithRelativeOffset:(CGFloat)newOffset;
/// @}

/// @name Initialization
/// @{
-(id)initWithLowerOffset:(CGFloat)newOffset;
-(id)initWithUpperOffset:(CGFloat)newOffset;
-(id)initWithRelativeOffset:(CGFloat)newOffset;
/// @}

@end

/** @category CPTConstraints(AbstractMethods)
 *  @brief CPTConstraints abstract methods—must be overridden by subclasses
 **/
@interface CPTConstraints(AbstractMethods)

/// @name Comparison
/// @{
-(BOOL)isEqualToConstraint:(CPTConstraints *)otherConstraint;
/// @}

/// @name Position
/// @{
-(CGFloat)positionForLowerBound:(CGFloat)lowerBound upperBound:(CGFloat)upperBound;
/// @}

@end
