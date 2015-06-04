#import "QEOFactoryContext.h"
#import <UIKit/UIKit.h>
#import "QEOPlatform.h"
#import "qeo/log.h"
#import "qeo/error.h"
#import <qeo/util_error.h>
#import "QEOFactory.h"
#import "QEORegistrationViewController.h"
#import "limits.h"

@interface QEOFactoryContext() <UIAlertViewDelegate>
@property (nonatomic,weak,readwrite) QEOFactory* factory;

- (void)invokeRegistrationDialog;
- (UIViewController *)topMostController;
- (UIViewController *)topViewControllerWithRootViewController:(UIViewController *)root; // helper
+ (NSBundle *)frameworkBundle;
@end

@implementation QEOFactoryContext
{
    qeo_platform_security_context_t _sec_context;
    QEORegistrationViewController* _modalVC;
}

#pragma mark - memory management

- (instancetype)initWithFactory:(QEOFactory *)factory
{
    self = [super init];
    if (!self) {
        return self;
    }

    self.factory = factory;
    
    // Get the platform
    QEOPlatform *platform = [QEOPlatform sharedInstance];
    
    // Set Factory context as responsible object for registration
    platform.factoryContext = self;
    
    return self;
}

- (void)dealloc {
    
    // cleanup
    _factory = nil;
    _sec_context = 0;
    
    // Get the platform
    QEOPlatform *platform = [QEOPlatform sharedInstance];
    
    platform.factoryContext = nil;
}

#pragma mark - QEOFactoryContextDelegate protocol

// For local registration
- (BOOL)performRegisterWithOtc:(NSString *)otc
                           url:(NSURL *)url
                         error:(NSError **)error
{
    QEOPlatform *platform = [QEOPlatform sharedInstance];
    
    if ((nil == platform.factoryContext) || (0 == _sec_context)) {
        
        qeo_log_e("Not permitted sec context = %d",_sec_context);
        
        // Not permitted
        if (error != nil){
            NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Registation: operation not allowed"};
            *error = [[NSError alloc]initWithDomain:@"org.qeo" code:QEO_EFAIL userInfo:userInfo];
        }
        return NO;
    }
    
    if ((nil == otc) || (YES ==[otc isEqualToString:@""])){
        
        if (error != nil){
            NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Could not start registration: empty otc not allowed"};
            *error = [[NSError alloc]initWithDomain:@"org.qeo" code:QEO_EFAIL userInfo:userInfo];
        }
        
        // Stop registration process
        qeo_util_retcode_t cancelResult = qeo_platform_cancel_registration(_sec_context);
        
        // cleanup
        _sec_context = 0;
        platform.factoryContext = nil;
        
        if (QEO_UTIL_OK != cancelResult) {
            qeo_log_e("qeo_platform_cancel_registration failed with result = %d",cancelResult);
        }
        return NO;
    }
    
    if (nil == url) {
        
        if (error != nil){
            NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Could not start registration: empty url not allowed"};
            *error = [[NSError alloc]initWithDomain:@"org.qeo" code:QEO_EFAIL userInfo:userInfo];
        }
        
        // Stop registration process
        qeo_util_retcode_t cancelResult = qeo_platform_cancel_registration(_sec_context);
        
        // cleanup
        _sec_context = 0;
        platform.factoryContext = nil;
        
        if (QEO_UTIL_OK != cancelResult) {
            qeo_log_e("qeo_platform_cancel_registration failed with result = %d",cancelResult);
        }
        return NO;
    }
    
    // call qeo-c-util verification method !!
    qeo_util_retcode_t result = qeo_platform_set_otc_url(_sec_context,
                                                         [[otc stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]] UTF8String],
                                                         [[[url absoluteString] stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]] UTF8String]);
    
    if (QEO_UTIL_OK != result) {
        
        if (error != nil){
            NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Could not start registration: internal error"};
            *error = [[NSError alloc]initWithDomain:@"org.qeo" code:result userInfo:userInfo];
        }
        
        // cleanup
        _sec_context = 0;
        platform.factoryContext = nil;
        
        return NO;
    }
    
    // Only good casses pass this point
    if (error != nil) {
        NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"OK"};
        *error = [[NSError alloc]initWithDomain:@"org.qeo" code:QEO_OK userInfo:userInfo];
    }
    return YES;
}

// For remote registration
- (BOOL)requestRemoteRegistrationAs:(NSString *)name
                            timeout:(NSNumber *)timeout
{
    if ((nil == name) || (YES ==[name isEqualToString:@""])){
        
        qeo_log_e("requestRemoteRegistrationAs:timeout: App name is empty");
        return NO;
    }
    
    if (0 == _sec_context) {
        qeo_log_e("requestRemoteRegistrationAs:timeout: Security context missing");
        return NO;
    }
    
    // call qeo-c-util remote registration method !!
    qeo_util_retcode_t result = qeo_platform_set_remote_registration_params(_sec_context,
                                                                            [name UTF8String],
                                                                            (nil==timeout)? 0:[timeout unsignedLongValue]);
    
    if (QEO_UTIL_OK != result) {
        
        qeo_log_e("requestRemoteRegistrationAs:timeout: remote registration request failed");
        return NO;
    }
    return YES;
}

// Only makes sence for remote registration
- (void)remoteRegistrationConfirmation:(BOOL)accept
{
    // call qeo-c-util to confirm/reject remote registration
    qeo_util_retcode_t result = qeo_platform_confirm_remote_registration_credentials(_sec_context,
                                                                                     (YES == accept) ? true : false);
    
    
    if (QEO_UTIL_OK != result) {
        qeo_log_e("native confirmation failed, result: %d",result);
        
        // Stop registration process
        qeo_util_retcode_t result = qeo_platform_cancel_registration(_sec_context);
        
        if (QEO_UTIL_OK != result) {
            qeo_log_e("qeo_platform_cancel_registration failed with result = %d",result);
        }
        
        QEOPlatform *platform = [QEOPlatform sharedInstance];
        
        // cleanup
        _sec_context = 0;
        platform.factoryContext = nil;
    }
}

// Cancels a remote/local registration
- (void)cancelRegistration
{
    QEOPlatform *platform = [QEOPlatform sharedInstance];
    
    if ((nil == platform.factoryContext) || (0 == _sec_context)) {
        qeo_log_e("Not permitted sec context = %d",_sec_context);
        // Not permitted
        return;
    }
    
    // Stop registration process
    qeo_util_retcode_t result = qeo_platform_cancel_registration(_sec_context);
    
    if (QEO_UTIL_OK != result) {
        qeo_log_e("qeo_platform_cancel_registration failed with result = %d",result);
    }
    
    // cleanup
    _sec_context = 0;
    platform.factoryContext = nil;
}


#pragma mark - public methods

- (void)closeContext {
    
    // cleanup
    _factory = nil;
    _sec_context = 0;
    
    if (nil != _modalVC) {
        [self closeRegistrationDialog];
    }
    
    QEOPlatform *platform = [QEOPlatform sharedInstance];
    platform.factoryContext = nil;
}

-(void)registerToQeoWith:(NSString*)otc url:(NSString*)url {
    
    qeo_log_d("Found OTC:%@, found URL: %@",otc,url);
    
    // call qeo-c-util verification method !!
    qeo_platform_set_otc_url(_sec_context,
                             [[otc stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]] UTF8String],
                             [[url stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]] UTF8String]);
    
}

- (qeo_util_retcode_t)registrationParametersNeeded:(qeo_platform_security_context_t)sec_context
{
    if (UIApplicationStateBackground == [UIApplication sharedApplication].applicationState) {
        // No UI allowed in background, default behaviour: deny
        return QEO_UTIL_EFAIL;
    }
    if (nil == self.factory) {
        qeo_log_e("No factory ");
        
        QEOPlatform *platform = [QEOPlatform sharedInstance];
        
        // cleanup
        _sec_context = 0;
        platform.factoryContext = nil;
        
        return QEO_UTIL_EFAIL;
    }
    
    // Store native context
    _sec_context = sec_context;
    
    if (nil == self.factory.delegate) {
        
        // Get the App name
        NSDictionary *info = [[NSBundle mainBundle] infoDictionary];
        NSString *appName = [info objectForKey:@"CFBundleDisplayName"];
        
        // Always send out a remote registration request
        [self requestRemoteRegistrationAs:appName
                                  timeout:[NSNumber numberWithLong:INT_MAX]];
        
        // Use Registration view from our framework
        // Put method call on main thread queue
        [self performSelectorOnMainThread:@selector(invokeRegistrationDialog)
                               withObject:nil
                            waitUntilDone:NO];
    } else {
        
        // Use external Registration view
        [self.factory.delegate registrationParametersNeeded:self];
    }
        
    return QEO_UTIL_OK;
}

- (void)security_status_update:(qeo_platform_security_context_t)sec_context
                         state:(qeo_platform_security_state)state
                        reason:(qeo_platform_security_state_reason)reason
{
    /* Nothing special done here */
}

// To trigger developer to check the credentials and to accept/reject the registration
- (qeo_util_retcode_t)remoteRegistrationConfirmationNeeded:(qeo_platform_security_context_t)sec_context
                                                 realmName:(NSString *)realmName
                                                       url:(NSURL *)url
{
    if (UIApplicationStateBackground == [UIApplication sharedApplication].applicationState) {
        // No UI allowed in background, default behaviour: deny
        return QEO_UTIL_EFAIL;
    }

    QEOPlatform *platform = [QEOPlatform sharedInstance];
    
    if (nil == self.factory) {
        qeo_log_e("No factory ");
        
        // cleanup
        _sec_context = 0;
        platform.factoryContext = nil;
        
        return QEO_UTIL_EFAIL;
    }
    
    // Store native context
    _sec_context = sec_context;
    
    if ((nil == self.factory.delegate) || ![self.factory.delegate respondsToSelector: @selector(remoteRegistrationConfirmationNeeded:realmName:url:)]) {
        
        dispatch_async(dispatch_get_main_queue(), ^{
            // Show Alert box (local object)
            UIAlertView* alert = [[UIAlertView alloc] initWithTitle:@"Remote Registration"
                                                            message:[NSString stringWithFormat:@"Qeo Credentials offered for Realm: %@ and Url: %@", realmName, [url absoluteString]]
                                                           delegate:self
                                                  cancelButtonTitle:nil
                                                  otherButtonTitles: @"Accept",@"Deny",nil];
            
            [alert show];
        });
        return QEO_UTIL_OK;
        
    } else {
        
        // Forward
        [self.factory.delegate remoteRegistrationConfirmationNeeded:self realmName:realmName url:url];
    }
    
    return QEO_UTIL_OK;
}

#pragma mark - UIAlertViewDelegate

// Delegate method to handle button clicks from the UI alert
- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    
    if (buttonIndex != [alertView cancelButtonIndex])
	{
        if (0 == buttonIndex) {
            
            // Remote registration Accept pressed
            [self remoteRegistrationConfirmation:YES];
        } else {
            
            // Remote registration Deny pressed
            [self remoteRegistrationConfirmation:NO];
        }
    }
}

#pragma mark - Registration dialog

-(UIViewController *) topViewControllerWithRootViewController:(UIViewController *)root {
    UIViewController *topController = root;
    
    while (topController.presentedViewController) {
        topController = topController.presentedViewController;
    }
    
    if ([topController isKindOfClass:[UINavigationController class]]) {
        // Check for nested sub hierarchies
        UIViewController *visibleOnNavigationStack = ((UINavigationController *)topController).visibleViewController;
        if (nil != visibleOnNavigationStack) {
            topController = [self topViewControllerWithRootViewController:visibleOnNavigationStack];
        }
    }
    else if ([topController isKindOfClass:[UITabBarController class]]) {
        // Check for nested sub hierarchies
        UITabBarController* tabBarController = (UITabBarController*)topController;
        topController = [self topViewControllerWithRootViewController:tabBarController.selectedViewController];
    }
    else if (topController.presentedViewController) {
        // Check for nested sub hierarchies
        UIViewController* presentedViewController = topController.presentedViewController;
        topController = [self topViewControllerWithRootViewController:presentedViewController];
    }
    
    return (topController != root ? topController : nil);
}

-(UIViewController *)topMostController
{
    // Start at the bottem of the hierarchy (=root)
    UIViewController *topController = [UIApplication sharedApplication].keyWindow.rootViewController;
    UIViewController *next = nil;
    
    // Run through the hierarchy
    while ((next = [self topViewControllerWithRootViewController:topController]) != nil) {
        topController = next;
    }
    
    return topController;
}

-(void)invokeRegistrationDialog {
    
    qeo_log_d("%s",__FUNCTION__);
    
    // Load correct storyboard from disk
    UIStoryboard *storyboard = nil;
    if ( UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad ) {
        
        storyboard = [UIStoryboard storyboardWithName:@"QEORegistration_iPad" bundle:[QEOFactoryContext frameworkBundle]];
    } else {
        
        storyboard = [UIStoryboard storyboardWithName:@"QEORegistration_iPhone" bundle:[QEOFactoryContext frameworkBundle]];
    }
    
    // Create registration view and make it modal
    _modalVC = [storyboard instantiateViewControllerWithIdentifier:@"RegistationView"];
    [_modalVC setModalPresentationStyle:UIModalPresentationFullScreen];
    [_modalVC setModalTransitionStyle:UIModalTransitionStyleCoverVertical];
    
    // Set callback reference
    _modalVC.context = self;
    
    // Get current active view controller to launch our registration view from
    [[self topMostController] presentViewController:_modalVC
                                           animated:YES
                                         completion:nil];
}

-(void)closeRegistrationDialog {
    qeo_log_d("%s",__FUNCTION__);
    
    if (nil != _modalVC) {
        // Close registration dialog, wait until its done before proceding (sync)
        dispatch_sync(dispatch_get_main_queue(), ^{
            [[_modalVC presentingViewController] dismissViewControllerAnimated:YES completion:nil];
            _modalVC = nil;
        });
    }
}

+ (NSBundle *)frameworkBundle {
    static NSBundle* frameworkBundle = nil;
    static dispatch_once_t predicate;
    dispatch_once(&predicate, ^{
        NSString* mainBundlePath = [[NSBundle mainBundle] resourcePath];
        NSString* frameworkBundlePath = [mainBundlePath stringByAppendingPathComponent:@"Qeo.bundle"];
        frameworkBundle = [NSBundle bundleWithPath:frameworkBundlePath];
        if (nil == frameworkBundle) {
            NSLog(@"MISSING QEO BUNDLE !!!!!!!!!!");
        }
        if (NO == frameworkBundle.loaded) {
            if (NO == [frameworkBundle load]) {
                NSLog(@"CANNOT LOAD THE QEO BUNDLE !!!!!!!!!!");
            }
        }
    });
    return frameworkBundle;
}



@end
