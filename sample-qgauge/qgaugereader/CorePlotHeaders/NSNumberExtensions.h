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

/** @category NSNumber(CPTExtensions)
 *  @brief Core Plot extensions to NSNumber.
 **/
@interface NSNumber(CPTExtensions)

+(NSNumber *)numberWithCGFloat:(CGFloat)number;

-(CGFloat)cgFloatValue;
-(id)initWithCGFloat:(CGFloat)number;

-(NSDecimalNumber *)decimalNumber;

@end
