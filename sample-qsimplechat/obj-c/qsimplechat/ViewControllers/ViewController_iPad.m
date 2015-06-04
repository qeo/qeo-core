#import "ViewController_iPad.h"

#pragma mark - Extension

// Extension: add private functionality
@interface ViewController_iPad () <UITextFieldDelegate>

// UI prperties connected to the storyboard controls
@property (weak, nonatomic) IBOutlet UITextField *userName;
@property (weak, nonatomic) IBOutlet UITextField *userInput;
@property (weak, nonatomic) IBOutlet UIButton *sendMessage;
@property (weak, nonatomic) IBOutlet UITextView *history;

@end

@implementation ViewController_iPad

#pragma mark - UITextFieldDelegate

// Called when the enter button on keyboard is pressed
- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    
    // Get Qeo representation of the UI info
    org_qeo_sample_simplechat_ChatMessage *cm = [self retrieveUserInput];
    
    // send it to the Qeo layer
    [self sendMessage:cm];
    
    return NO;
}

#pragma mark - object memory management

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
    
    _history.text = @"";
}

#pragma mark - Life Cycle

- (void)viewDidLoad
{
    [super viewDidLoad];
	
    // initialize UI cntrols
    _userName.text = [NSString stringWithFormat:@"%s",[[UIDevice currentDevice].name UTF8String]];
    _history.text = @"";
    
    // set delegates
    _userName.delegate = self;
    _userInput.delegate = self;
}

#pragma mark - User interaction

// Button Send pressed
- (IBAction)publishMessage:(id)sender {
    
    // Get Qeo representation of the UI info
    org_qeo_sample_simplechat_ChatMessage *cm = [self retrieveUserInput];
    
    // send it to the Qeo layer
    [self sendMessage:cm];
}

#pragma mark - Base class overrides

// Collects UI info from the user
-(org_qeo_sample_simplechat_ChatMessage *)retrieveUserInput {
    
    // Create message container
    org_qeo_sample_simplechat_ChatMessage *cm = [[org_qeo_sample_simplechat_ChatMessage alloc] init];
    
    // store UI info
    cm.from = _userName.text;
    cm.message = _userInput.text;
    
    // cleanup UI
    _userInput.text = @"";
    
    // Remove keyboard
    [_userInput resignFirstResponder];
    [_userName resignFirstResponder];
    
    return cm;
}

// Update UI with content of Qeo message
-(void)updateScreenWithChatMessage {
    
    // Don't update UI when not visibles
    if ((YES == [self isViewLoaded]) && (nil != self.view.window)) {
        
        // Join all message info
        NSString* newContent = [self.messageLIFO componentsJoinedByString:@"\n "];
        
        // Update UI
        _history.text = newContent;
    }
}

@end
