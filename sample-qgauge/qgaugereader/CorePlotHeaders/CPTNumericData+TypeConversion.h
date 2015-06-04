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

#import "CPTNumericData.h"
#import "CPTNumericDataType.h"

/** @category CPTNumericData(TypeConversion)
 *  @brief Type conversion methods for CPTNumericData.
 **/
@interface CPTNumericData(TypeConversion)

/// @name Type Conversion
/// @{
-(CPTNumericData *)dataByConvertingToDataType:(CPTNumericDataType)newDataType;

-(CPTNumericData *)dataByConvertingToType:(CPTDataTypeFormat)newDataType sampleBytes:(size_t)newSampleBytes byteOrder:(CFByteOrder)newByteOrder;
/// @}

/// @name Data Conversion Utilities
/// @{
-(void)convertData:(NSData *)sourceData dataType:(CPTNumericDataType *)sourceDataType toData:(NSMutableData *)destData dataType:(CPTNumericDataType *)destDataType;
-(void)swapByteOrderForData:(NSMutableData *)sourceData sampleSize:(size_t)sampleSize;
/// @}

@end
