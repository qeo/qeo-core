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

#import <UIKit/UIKit.h>
#import <Qeo/Qeo.h>

@interface TabControllerViewController : UITabBarController

@property (nonatomic, readonly) QEOFactory *factory;
@property (weak, nonatomic, readonly) id<QEOFactoryContextDelegate> context;

// Will be called when the App is launched in the background
-(void)didLaunchInBackground;

// Will be called when the App enters the background from Active or Inactive state
-(void)willResign;

// Will be called when the App resumes in the foreground
-(void)willContinue;

@end
