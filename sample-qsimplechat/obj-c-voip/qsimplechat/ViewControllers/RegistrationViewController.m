#import "RegistrationViewController.h"
#import <Qeo/qeo.h>
#import "limits.h"

#pragma mark Extension
@interface RegistrationViewController () <UITextFieldDelegate>
@property (weak, nonatomic) IBOutlet UITextField *myOTCField;
@property (weak, nonatomic) IBOutlet UITextField *myURLField;
@property (weak, nonatomic) IBOutlet UIButton *remoteRegistrationButton;

@end

#pragma mark -
@implementation RegistrationViewController

#pragma mark Memory management

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
    
    self.myURLField.text = @"http://join.qeo.org";
    self.myURLField.delegate = self;
    self.myOTCField.delegate = self;
    [self.remoteRegistrationButton setTitle:@"Remote Registration" forState:UIControlStateNormal];
    [self.remoteRegistrationButton setTitleColor:[UIColor blueColor] forState:UIControlStateNormal];
    self.remoteRegistrationButton.enabled = YES;
}

#pragma mark - UITextFieldDelegate

// Called when the enter button on keyboard is pressed
- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    
    // Remove Registration UI
    [[self presentingViewController] dismissViewControllerAnimated:YES completion:nil];
    
    BOOL result = [self.context performRegisterWithOtc:self.myOTCField.text
                                                   url:[[NSURL alloc] initWithString: self.myURLField.text]
                                                 error:nil];
    
    NSLog(@"%s  result: %d , otc: %@, url: %@",__FUNCTION__,result,self.myOTCField.text,self.myURLField.text);
    
    return NO;
}

#pragma mark - QEO related

- (IBAction)cancelRegistration:(UIButton *)sender {
    
        // Remove Registration UI
    [[self presentingViewController] dismissViewControllerAnimated:YES completion:nil];
    
    [self.context cancelRegistration];
}

- (IBAction)registerToQeo:(UIButton *)sender {
    
    // Remove Registration UI
    [[self presentingViewController] dismissViewControllerAnimated:YES completion:nil];
    
    BOOL result = [self.context performRegisterWithOtc:self.myOTCField.text
                                                   url:[[NSURL alloc] initWithString: self.myURLField.text]
                                                 error:nil];
    
    NSLog(@"%s  result: %d , otc: %@, url: %@",__FUNCTION__,result,self.myOTCField.text,self.myURLField.text);
}

- (IBAction)setupRemoteRegistration:(UIButton *)sender {
    
    // Check the user defaults, the user may have given a special timeout value
    // in the General settings of the App.
    NSUserDefaults *standardUserDefaults = [NSUserDefaults standardUserDefaults];
    [standardUserDefaults synchronize];
    
    NSString *strTimeout = [standardUserDefaults stringForKey:@"reg_remote"];
    long long timeout;
    
    if ((nil == strTimeout) || (YES == [strTimeout isEqualToString:@""])) {
        
        // Take endless time
        timeout = INT_MAX;
    } else {
        
        // Extract timeout value
        timeout = [strTimeout longLongValue];
    }
    
    [self.remoteRegistrationButton setTitle:@"Remote Started ... " forState:UIControlStateNormal];
    [self.remoteRegistrationButton setTitleColor:[UIColor redColor] forState:UIControlStateNormal];
    self.remoteRegistrationButton.enabled = NO;
    
    // Wait max timeout in seconds
    [self.context requestRemoteRegistrationAs:@"My Customized Simple Chat App" timeout:[NSNumber numberWithLongLong:timeout]];
}

- (void)resetControls {
    
    self.myURLField.text = @"http://join.qeo.org";
    self.myOTCField.text = @"";
    [self.remoteRegistrationButton setTitle:@"Remote Registration" forState:UIControlStateNormal];
    [self.remoteRegistrationButton setTitleColor:[UIColor blueColor] forState:UIControlStateNormal];
    self.remoteRegistrationButton.enabled = YES;
}

@end
