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

/// @ingroup themeNames
/// @{
extern NSString *const kCPTDarkGradientTheme; ///< A graph theme with dark gray gradient backgrounds and light gray lines.
extern NSString *const kCPTPlainBlackTheme;   ///< A graph theme with black backgrounds and white lines.
extern NSString *const kCPTPlainWhiteTheme;   ///< A graph theme with white backgrounds and black lines.
extern NSString *const kCPTSlateTheme;        ///< A graph theme with colors that match the default iPhone navigation bar, toolbar buttons, and table views.
extern NSString *const kCPTStocksTheme;       ///< A graph theme with a gradient background and white lines.
/// @}

@class CPTGraph;
@class CPTPlotAreaFrame;
@class CPTAxisSet;
@class CPTMutableTextStyle;

@interface CPTTheme : NSObject<NSCoding> {
    @private
    Class graphClass;
}

@property (nonatomic, readwrite, retain) Class graphClass;

/// @name Theme Management
/// @{
+(void)registerTheme:(Class)themeClass;
+(NSArray *)themeClasses;
+(CPTTheme *)themeNamed:(NSString *)theme;
+(NSString *)name;
/// @}

/// @name Theme Usage
/// @{
-(void)applyThemeToGraph:(CPTGraph *)graph;
/// @}

@end

/** @category CPTTheme(AbstractMethods)
 *  @brief CPTTheme abstract methods—must be overridden by subclasses
 **/
@interface CPTTheme(AbstractMethods)

/// @name Theme Usage
/// @{
-(id)newGraph;

-(void)applyThemeToBackground:(CPTGraph *)graph;
-(void)applyThemeToPlotArea:(CPTPlotAreaFrame *)plotAreaFrame;
-(void)applyThemeToAxisSet:(CPTAxisSet *)axisSet;
/// @}

@end
