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

#import "CPTBorderedLayer.h"
#import "CPTTextStyle.h"

/// @file

extern const CGFloat kCPTTextLayerMarginWidth; ///< Margin width around the text.

@interface CPTTextLayer : CPTBorderedLayer {
    @private
    NSString *text;
    CPTTextStyle *textStyle;
    NSAttributedString *attributedText;
    CGSize maximumSize;
}

@property (readwrite, copy, nonatomic) NSString *text;
@property (readwrite, retain, nonatomic) CPTTextStyle *textStyle;
@property (readwrite, copy, nonatomic) NSAttributedString *attributedText;
@property (readwrite, nonatomic) CGSize maximumSize;

/// @name Initialization
/// @{
-(id)initWithText:(NSString *)newText;
-(id)initWithText:(NSString *)newText style:(CPTTextStyle *)newStyle;
-(id)initWithAttributedText:(NSAttributedString *)newText;
/// @}

/// @name Layout
/// @{
-(CGSize)sizeThatFits;
-(void)sizeToFit;
/// @}

@end
