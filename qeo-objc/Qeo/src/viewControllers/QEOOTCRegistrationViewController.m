#import "QEOOTCRegistrationViewController.h"
#import "QEOContainerViewController.h"
#import "qeo/log.h"

@interface QEOOTCRegistrationViewController () <UITextFieldDelegate>

@property (weak, nonatomic) IBOutlet UITextField *otc;
@property (weak, nonatomic) IBOutlet UITextField *url;
@property (weak, nonatomic) IBOutlet UIButton *switchToQRCodeButton;
@property (weak, nonatomic) IBOutlet UIButton *validateButton;

- (IBAction)validationPressed:(UIButton *)sender;
- (IBAction)cancelPressed:(UIButton *)sender;
- (IBAction)switchToQRCodeRegistration:(UIButton *)sender;

@end

@implementation QEOOTCRegistrationViewController

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
    // Dispose of any resources that can be recreated.
}


- (void)viewDidLoad
{
    [super viewDidLoad];
    
    self.view.autoresizesSubviews = YES;
    
    _url.text = @"https://join.qeo.org";
    
    // QR Code scanning is not supported in the simulator
    NSString *model = [[UIDevice currentDevice] model];
    if ([model rangeOfString:@"Simulator" options:NSCaseInsensitiveSearch].location != NSNotFound){
        _switchToQRCodeButton.enabled = NO;
    } else {
        _switchToQRCodeButton.enabled = YES;
    }
    
    // set delegates
    _otc.delegate = self;
    _url.delegate = self;
	
    qeo_log_d("%s",__FUNCTION__);
}

-(void)viewWillAppear:(BOOL)animated {
    
    // At startup UI is default in portrait mode
    // If you do manual layout of controls you need to adjust the frame to landscape here manually
    if (UIInterfaceOrientationIsLandscape([UIApplication sharedApplication].statusBarOrientation)) {
    
        CGRect frame = self.view.frame;
        if (frame.size.height > frame.size.width) {
            // FIX: for view to lanscape size before we get to viewDidLayoutSubviews
            CGFloat height = frame.size.height;
            frame.size.height = frame.size.width - 48 /* label + spacing */;
            frame.size.width  = height;
            self.view.frame = frame;
        }
    
    }
}

-(void)viewDidLayoutSubviews {
    
    if (UIInterfaceOrientationIsLandscape([UIApplication sharedApplication].statusBarOrientation)) {
        
        // For iPhone:
        if (UI_USER_INTERFACE_IDIOM() != UIUserInterfaceIdiomPad) {
            // Position button
            CGRect validateButtonframe = _validateButton.frame;
            CGRect switchButtonFrame   = _switchToQRCodeButton.frame;
            
            // Allign buttons on height
            switchButtonFrame.origin.y = validateButtonframe.origin.y;
            
            _switchToQRCodeButton.frame = switchButtonFrame;
        }
    }
}

-(void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration {
    
    // BUG: frame is not updated after rotation, bounds position is ok
    //      force frame size before rotation starts to fix it
    self.view.frame = self.parentViewController.view.frame;
    
    [super willAnimateRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event {
    UITouch * touch = [touches anyObject];
    if(touch.phase == UITouchPhaseBegan) {
        // Remove keyboard
        [_otc resignFirstResponder];
        [_url resignFirstResponder];
    }
}

// Called when the enter button on keyboard is pressed
- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    
    // Remove keyboard
    [_otc resignFirstResponder];
    [_url resignFirstResponder];
    
    QEOContainerViewController* parent = (QEOContainerViewController*)self.parentViewController;
    [parent registerToQeoWith:_otc.text url:_url.text];
    
    return NO;
}

- (IBAction)validationPressed:(UIButton *)sender {
    
    // Remove keyboard
    [_otc resignFirstResponder];
    [_url resignFirstResponder];
    
    QEOContainerViewController* parent = (QEOContainerViewController*)self.parentViewController;
    [parent registerToQeoWith:_otc.text url:_url.text];
}

- (IBAction)cancelPressed:(UIButton *)sender {
    
    // Remove keyboard
    [_otc resignFirstResponder];
    [_url resignFirstResponder];
    
    QEOContainerViewController* parent = (QEOContainerViewController*)self.parentViewController;
    [parent cancelRegistration];
}

- (IBAction)switchToQRCodeRegistration:(UIButton *)sender {
    
    QEOContainerViewController* parent = (QEOContainerViewController*)self.parentViewController;
    [parent swapViewControllers];
}

@end
