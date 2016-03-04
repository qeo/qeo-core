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

#import "CPTMutableNumericData.h"
#import "CPTNumericDataType.h"

/** @category CPTMutableNumericData(TypeConversion)
 *  @brief Type conversion methods for CPTMutableNumericData.
 **/
@interface CPTMutableNumericData(TypeConversion)

/// @name Data Format
/// @{
@property (readwrite, assign) CPTNumericDataType dataType;
@property (readwrite, assign) CPTDataTypeFormat dataTypeFormat;
@property (readwrite, assign) size_t sampleBytes;
@property (readwrite, assign) CFByteOrder byteOrder;
/// @}

/// @name Type Conversion
/// @{
-(void)convertToType:(CPTDataTypeFormat)newDataType sampleBytes:(size_t)newSampleBytes byteOrder:(CFByteOrder)newByteOrder;
/// @}

@end
