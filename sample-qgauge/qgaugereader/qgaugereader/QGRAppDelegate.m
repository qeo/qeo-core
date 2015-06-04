#import "QGRAppDelegate.h"

#import "QGRMasterViewController.h"

@implementation QGRAppDelegate



- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    
    NSUserDefaults *standardUserDefaults = [NSUserDefaults standardUserDefaults];
    if(nil == [standardUserDefaults objectForKey:@"reset_Qeo"]) {
        
        // Set the application defaults
        [standardUserDefaults setObject:@"NO" forKey:@"reset_Qeo"];
        [standardUserDefaults synchronize];
    }

    
    // Override point for customization after application launch.
    if ([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPad) {
        UISplitViewController *splitViewController = (UISplitViewController *)self.window.rootViewController;
        UINavigationController *navigationController = [splitViewController.viewControllers lastObject];
        splitViewController.delegate = (id)navigationController.topViewController;
        
        UINavigationController *masterNavigationController = splitViewController.viewControllers[0];
        QGRMasterViewController *controller = (QGRMasterViewController *)masterNavigationController.topViewController;
        
    } else {
        UINavigationController *navigationController = (UINavigationController *)self.window.rootViewController;
        QGRMasterViewController *controller = (QGRMasterViewController *)navigationController.topViewController;
        
    }
    return YES;
}
							
- (void)applicationWillResignActive:(UIApplication *)application
{
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
        [[NSNotificationCenter defaultCenter] postNotificationName:@"pauseApp" object:nil userInfo:nil];
    
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
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    [[NSNotificationCenter defaultCenter] postNotificationName:@"resumeApp" object:nil userInfo:nil];
    
}

- (void)applicationWillTerminate:(UIApplication *)application
{
    // Saves changes in the application's managed object context before the application terminates.
   
}


@end
