#import "AppDelegate.h"
#import "TabControllerViewController.h"
#import "QeoBackgroundServiceManager.h"

@implementation AppDelegate

#pragma mark UIApplicationDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    // Give permission to use local notifications and sounds (simulator)
    if ([application respondsToSelector:@selector(registerUserNotificationSettings:)])
    {
        // Running on iOS 8+
        UIUserNotificationSettings *settings = [UIUserNotificationSettings settingsForTypes:(UIUserNotificationTypeAlert|UIUserNotificationTypeSound) categories:nil];
        [application registerUserNotificationSettings:settings];
    }
    else // Running on iOS 7.x
    {
        UIRemoteNotificationType myTypes = UIRemoteNotificationTypeAlert | UIRemoteNotificationTypeSound;
        [application registerForRemoteNotificationTypes:myTypes];
    }

    //---------------------------------------------------------
    // callback handler in case BGNS notifies the App
    //---------------------------------------------------------
    typedef void (^onQeoDataHandler)(void);
    onQeoDataHandler onQeoData = ^{
        // Remove old local notifications
        [[UIApplication sharedApplication] cancelAllLocalNotifications];
        
        // Inform user with new local notification:
        if (UIApplicationStateActive != [UIApplication sharedApplication].applicationState) {
            
            // Create notification
            UILocalNotification* notif = [[UILocalNotification alloc] init];
            if (nil != notif) {
                notif.repeatInterval = 0;
                notif.alertBody = @"New Qeo data available";
                notif.alertAction = @"slide to open App";
                notif.hasAction = YES;
                notif.soundName = @"ReceivedMessage.caf";
                
                [[UIApplication sharedApplication]  presentLocalNotificationNow:notif];
            }
        }
    };

    // Create BGNS Manager
    self.qeoBackgroundServiceManager =
      [[QeoBackgroundServiceManager alloc]initWithKeepAlivePollingTime:600         // Apple minimum = 600 sec
                                                          qeoAliveTime:60         // Max. 600 sec, take a small margin
                                                      onQeoDataHandler:onQeoData];

    //---------------------------------------------------------
    // Check if in which state the App was started
    //---------------------------------------------------------
    if (UIApplicationStateBackground == application.applicationState) {
        NSLog(@"%s: App started in background",__FUNCTION__);

        // App is not notified via the Appdelegate about a auto-launch into the background.
        // => i.e. rebooting the device or when the App has crashed/killed by the OS and then auto-restarted.
        // Note: App is never auto-restarted when the user explicitally has forced close the App.
        //
        // Create Factory, Readers/Writers and register to BGNS in the background (if possible)
        TabControllerViewController* root = (TabControllerViewController*)self.window.rootViewController;
        [root didLaunchInBackground];
    } else {
        NSLog(@"%s: App started in foreground",__FUNCTION__);

        // Check if User has reset the Qeo credentials
        NSUserDefaults *standardUserDefaults = [NSUserDefaults standardUserDefaults];
        if(nil == [standardUserDefaults objectForKey:@"reset_Qeo"]) {

            // Reset the application defaults
            [standardUserDefaults setObject:@"NO" forKey:@"reset_Qeo"];
            [standardUserDefaults synchronize];
        }

        // App is started in the foreground: Clear out all old notifications
        [application cancelAllLocalNotifications];
    }
    return YES;
}

- (void)applicationWillResignActive:(UIApplication *)application
{
    NSLog(@"%s",__FUNCTION__);
    TabControllerViewController* root = (TabControllerViewController*)self.window.rootViewController;
    [root willResign];
}

- (void)applicationDidEnterBackground:(UIApplication *)application
{
    NSLog(@"%s",__FUNCTION__);
    // Notify the BGNS Manager that the App is in background mode
    [self.qeoBackgroundServiceManager applicationDidEnterBackground:application];
}

- (void)applicationWillEnterForeground:(UIApplication *)application
{
    NSLog(@"%s",__FUNCTION__);
    // Notify the BGNS Manager that the App is coming to the foreground
    [self.qeoBackgroundServiceManager applicationWillEnterForeground:application];
    
    // Clear out all old notifications
    [application cancelAllLocalNotifications];
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
    NSLog(@"%s",__FUNCTION__);
    TabControllerViewController* root = (TabControllerViewController*)self.window.rootViewController;
    [root willContinue];
}

- (void)applicationWillTerminate:(UIApplication *)application
{
    NSLog(@"%s",__FUNCTION__);
}

@end
