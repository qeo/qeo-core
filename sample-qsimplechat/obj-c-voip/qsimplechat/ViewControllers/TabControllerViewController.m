#import "TabControllerViewController.h"
#import "ApplicationStateProtocol.h"
#import "RegistrationViewController.h"
#import "AppDelegate.h"
#import "QeoBackgroundServiceManager.h"

#pragma mark Extension
@interface TabControllerViewController () <UIAlertViewDelegate,QEOFactoryDelegate>

@property BOOL qeoRequestStarted; // atomic
@property (nonatomic, readwrite) QEOFactory *factory;
@property (weak, nonatomic, readwrite) id<QEOFactoryContextDelegate> context;

- (void)setupQeoFactory;

@end

#pragma mark -
@implementation TabControllerViewController
{
    __weak RegistrationViewController *registrationDialog;
}

#pragma mark Memory Management

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
}

#pragma mark - Life Cycle

- (void)viewDidLoad
{
    [super viewDidLoad];
}

#pragma mark - Segue handling

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    if (YES == [[segue identifier] isEqualToString:@"registerSegue"])
    {
        // Get reference to the destination view controller
        registrationDialog = [segue destinationViewController];
        
        // Pass context object
        registrationDialog.context = self.context;
    }
}

#pragma mark - QEOFactoryDelegate

- (void)registrationParametersNeeded:(id<QEOFactoryContextDelegate>)context
{
    // Show custom dialog
    self.context = context;

    dispatch_async(dispatch_get_main_queue(), ^{
        
        // invoke registration dialog through the segue
        [self performSegueWithIdentifier:@"registerSegue" sender:nil];
        
    });
}

- (void)remoteRegistrationConfirmationNeeded:(id<QEOFactoryContextDelegate>)context
                                   realmName:(NSString *)realmName
                                         url:(NSURL *)url
{
    dispatch_async(dispatch_get_main_queue(), ^{
        // Show Alert box (local object)
        UIAlertView* alert = [[UIAlertView alloc] initWithTitle:@"Remote Registration"
                                                        message:[NSString stringWithFormat:@"Qeo Credentials offered for Realm: %@ and Url: %@", realmName, [url absoluteString]]
                                                       delegate:self
                                              cancelButtonTitle:nil
                                              otherButtonTitles: @"Accept",@"Deny",nil];
        
        [alert show];
    });
}

#pragma mark - public methods

// Will be called when the App is launched in the background
-(void)didLaunchInBackground
{
    NSLog(@"%s",__FUNCTION__);
    
    // Check the user defaults, the user may have set the reset the Qeo identities in
    // the General settings of the App.
    NSUserDefaults *standardUserDefaults = [NSUserDefaults standardUserDefaults];
    [standardUserDefaults synchronize];

    // Only if we do not need to reset identities we are going to try to construct the factory
    // The acual reset will be handled when the App moves to the foreground.
    if (NO == [standardUserDefaults boolForKey:@"reset_Qeo"]) {
        __weak TabControllerViewController* weakSelf = self;

        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{

            weakSelf.qeoRequestStarted = YES;

            // Try to create a factory: The Qeo framework will not display any UI in background mode
            // If the App is not registered it will return nil otherwise it will create the factory for the
            // registered realm
            weakSelf.factory = [[QEOFactory alloc] init];

            if (nil != weakSelf.factory) {
                // VoiP: Register the factory to the background notification server
                AppDelegate *appDelegate = (AppDelegate *)([UIApplication sharedApplication].delegate);
                weakSelf.factory.bgnsCallbackDelegate = appDelegate.qeoBackgroundServiceManager;

                weakSelf.qeoRequestStarted = NO;

                // Update other controllers about the factory
                NSArray *controllers = [weakSelf viewControllers];
                for (id<ApplicationStateProtocol> asp in controllers) {
                    [asp setupQeoCommunication];
                }

                // Move Qeo Background manager to the "Background Qeo active state"
                [appDelegate.qeoBackgroundServiceManager didLaunchInBackground];
            } else {
                weakSelf.qeoRequestStarted = NO;
            }
        });
    }
}

// Will be called when the App enters the background from Active or Inactive state
-(void)willResign
{
    // Forward to every tab
    NSArray *controllers = [self viewControllers];
    for (id<ApplicationStateProtocol> asp in controllers) {
        [asp willResign];
    }
}

// Will be called when the App resumes in the foreground
-(void)willContinue
{
    // Check the user defaults, the user may have set the reset the Qeo identities in
    // the General settings of the App.
    NSUserDefaults *standardUserDefaults = [NSUserDefaults standardUserDefaults];
    [standardUserDefaults synchronize];
    
    if (YES == [standardUserDefaults boolForKey:@"reset_Qeo"]) {
        // reset
        [QEOIdentity clearQeoIdentities];
        self.factory = nil;

        // Forward to every tab
        NSArray *controllers = [self viewControllers];
        for (id<ApplicationStateProtocol> asp in controllers) {
            [asp onResetQeo];
        }
        
        // reset the flag in the user defaults
        [standardUserDefaults setObject:@"NO" forKey:@"reset_Qeo"];
        [standardUserDefaults synchronize];
    }
    
    if ((nil == self.factory) && (NO == self.qeoRequestStarted)) {
        [self setupQeoFactory];
    }
}

#pragma mark - UIAlertViewDelegate

// Delegate method to handle button clicks from the UI alert
- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex
{
    if (buttonIndex != [alertView cancelButtonIndex])
	{
        if (1 < [alertView numberOfButtons]) {
            
            if (0 == buttonIndex) {
                
                // Remote registration Accept pressed
                if (nil != self.context) {
                    [self.context remoteRegistrationConfirmation:YES];
                }
            } else {
                
                // Remote registration Deny pressed
                if (nil != self.context) {
                    [self.context remoteRegistrationConfirmation:NO];
                    [registrationDialog performSelector:@selector(resetControls) withObject:nil];
                }
            }
            
        } else {
            
            // Close current request
            self.qeoRequestStarted = NO;
            
            // Retry pressed, delay needed to allow alert view to close properly before showing the
            // registration dialog again.
            // Too soon gives: "Warning: Attempt to dismiss from view controller"
            [self performSelector:@selector(setupQeoFactory) withObject:nil afterDelay:0.7];
        }
    }
}

#pragma mark - private methods

- (void)setupQeoFactory
{    
    __weak TabControllerViewController* weakSelf = self;

    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        
        weakSelf.qeoRequestStarted = YES;
     
        NSError* error = nil;
        
        // Check the user defaults, the user may have selected the custom registration
        // in the General settings of the App.
        NSUserDefaults *standardUserDefaults = [NSUserDefaults standardUserDefaults];
        [standardUserDefaults synchronize];
        
        if (YES == [standardUserDefaults boolForKey:@"reg_custom"]) {
            
            // Trigger Custom Developer registration UI
            weakSelf.factory = [[QEOFactory alloc] initWithFactoryDelegate:weakSelf error:&error];
        } else {
            
            // Trigger Framework registration UI
            weakSelf.factory = [[QEOFactory alloc] initWithError:&error];
        }
        
        // Close Registration dialog if still visible
        if (nil != registrationDialog) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [registrationDialog dismissViewControllerAnimated:YES completion:nil];
            });
        }
        
        if (nil == weakSelf.factory){
            
            dispatch_async(dispatch_get_main_queue(), ^{
                
                // Show Alert box
                UIAlertView *_alert = [[UIAlertView alloc] initWithTitle:@"Qeo Factory"
                                                                 message:[error localizedDescription]
                                                                delegate:weakSelf
                                                       cancelButtonTitle:nil
                                                       otherButtonTitles: @"Retry",nil];
                
                [_alert show];
                
            });
            weakSelf.qeoRequestStarted = NO;
            return;
        }

        // VoiP: Register the factory to the background notification server
        AppDelegate *appDelegate = (AppDelegate *)([UIApplication sharedApplication].delegate);
        weakSelf.factory.bgnsCallbackDelegate = appDelegate.qeoBackgroundServiceManager;
        
        weakSelf.qeoRequestStarted = NO;
        
        NSArray *controllers = [weakSelf viewControllers];
        for (id<ApplicationStateProtocol> asp in controllers) {
            [asp setupQeoCommunication];
        }
    });
}

@end
