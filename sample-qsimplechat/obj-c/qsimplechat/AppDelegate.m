#import "AppDelegate.h"
#import "TabControllerViewController.h"
#import <Qeo/Qeo.h>

#pragma mark -

@implementation AppDelegate

#pragma mark UIApplicationDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    //---------------------------------------------------------
    // Check if User has reset the Qeo credentials
    //---------------------------------------------------------
    NSUserDefaults *standardUserDefaults = [NSUserDefaults standardUserDefaults];
    if(nil == [standardUserDefaults objectForKey:@"reset_Qeo"]) {
        
        // Set the application defaults
        [standardUserDefaults setObject:@"NO" forKey:@"reset_Qeo"];
        [standardUserDefaults synchronize];
    }
    return YES;
}

- (void)applicationWillResignActive:(UIApplication *)application
{
    TabControllerViewController* root = (TabControllerViewController*)self.window.rootViewController;
    [root willResign];
}

- (void)applicationDidEnterBackground:(UIApplication *)application
{
    // Put Qeo to deep sleep
    [QEOFactory suspendQeoCommunication];
}

- (void)applicationWillEnterForeground:(UIApplication *)application
{
    // Wakeup Qeo
    [QEOFactory resumeQeoCommunication];
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
    TabControllerViewController* root = (TabControllerViewController*)self.window.rootViewController;
    [root willContinue];
}

- (void)applicationWillTerminate:(UIApplication *)application
{
   
}

@end
