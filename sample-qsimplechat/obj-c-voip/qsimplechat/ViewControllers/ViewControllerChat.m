#import "ViewControllerChat.h"
#import "TabControllerViewController.h"
#import <Qeo/Qeo.h>

#pragma mark - Defines
#define MAX_NUM_OF_ELEMENTS_IN_HISTORY 50

#pragma mark - Extension
@interface ViewControllerChat () <UITextFieldDelegate,QEOEventReaderDelegate,UIAlertViewDelegate>

#pragma mark storyboard controls
@property (weak, nonatomic) IBOutlet UITextField *userName;
@property (weak, nonatomic) IBOutlet UITextView *history;
@property (weak, nonatomic) IBOutlet UIButton *send;
@property (weak, nonatomic) IBOutlet UITextField *userInput;
@property (weak, nonatomic) IBOutlet UISegmentedControl *state;
- (IBAction)userNameChanged:(UITextField *)sender;
- (IBAction)stateUpdated:(UISegmentedControl *)sender;

#pragma mark private members and methods
@property (strong) NSMutableArray* messageLIFO; //atomic
@property (nonatomic,strong) QEOEventReader *eventReader;
@property (nonatomic,strong) QEOEventWriter *eventWriter;
@property (nonatomic,strong) QEOStateWriter *participantWriter;
@property (nonatomic,strong) UIAlertView *alert;
@property (nonatomic,strong) org_qeo_sample_simplechat_ChatParticipant *previousChatParticipant;
-(void)sendMessage:(org_qeo_sample_simplechat_ChatMessage *) chatMessage;
-(org_qeo_sample_simplechat_ChatMessage *)retrieveUserInput;
@end

#pragma mark -
@implementation ViewControllerChat

#pragma mark Object memory management

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
    }
    
    return self;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    
    // Remove event messages to free up memory
    [self.messageLIFO removeAllObjects];
    
    // Set control content to a small string 
    self.history.text = @"";
}

#pragma mark - Life Cycle

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    // initialize UI cntrols
    self.userName.text = [NSString stringWithFormat:@"%s",[[UIDevice currentDevice].name UTF8String]];
    self.history.text = @"";
    
    // set delegates
    self.userName.delegate = self;
    self.userInput.delegate = self;
    
    self.messageLIFO = [[NSMutableArray alloc] initWithCapacity:MAX_NUM_OF_ELEMENTS_IN_HISTORY];
}

- (void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
    
    // Refresh screen with lates content
    [self updateScreenWithChatMessage];
}

#pragma mark - QEOEventReaderDelegate

// Callback from Qeo layer: event received
- (void)didReceiveEvent:(QEOType *)event
              forReader:(QEOEventReader *)eventReader
{
    // Cast to the model type
    org_qeo_sample_simplechat_ChatMessage *cm = (org_qeo_sample_simplechat_ChatMessage *)event;
    NSString* message = [NSString stringWithFormat:@"\n%@: \n%@ ",cm.from, cm.message];
    
    // Add event to the message LIFO
    [self.messageLIFO insertObject:message atIndex:0];
    
    if (MAX_NUM_OF_ELEMENTS_IN_HISTORY < [self.messageLIFO count]) {
        // drop first element
        [self.messageLIFO removeLastObject];
    }
}

// Callback from Qeo layer: no more events in Qeo queue
- (void)didFinishBurstForEventReader:(QEOEventReader *)reader
{
    NSLog(@"%s",__FUNCTION__);
    
    // Update the received info on the UI (UI updates must be done on the main thread)
    [self performSelectorOnMainThread:@selector(updateScreenWithChatMessage)
                           withObject:nil
                        waitUntilDone:NO];
}

#pragma mark - UIAlertViewDelegate

// Delegate method to handle button clicks from the UI alert
- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    
    if (buttonIndex != [alertView cancelButtonIndex])
    {
        if (1 < [alertView numberOfButtons]) {
            TabControllerViewController *tc = (TabControllerViewController *)[self parentViewController];
            if (0 == buttonIndex) {
                
                // Remote registration Accept pressed
                if (nil != tc.context) {
                    [tc.context remoteRegistrationConfirmation:YES];
                }
            } else {
                
                // Remote registration Deny pressed
                if (nil != tc.context) {
                    [tc.context remoteRegistrationConfirmation:NO];
                }
            }
            
        } else {
            
            // Retry pressed, delay needed to allow alert view to close properly before showing the
            // registration dialog again.
            // Too soon gives: "Warning: Attempt to dismiss from view controller"
            [self performSelector:@selector(setupQeoCommunication) withObject:nil afterDelay:0.7];
        }
    }
}

- (void)alertView:(UIAlertView *)alertView didDismissWithButtonIndex:(NSInteger)buttonIndex {
    
    // cleanup
    self.alert = nil;
}

#pragma mark - UITextFieldDelegate

// Called when the enter button on keyboard is pressed
- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    
    // Get Qeo representation of the UI info
    org_qeo_sample_simplechat_ChatMessage *cm = [self retrieveUserInput];
    
    // Send it to the Qeo layer
    [self sendMessage:cm];
    
    return NO;
}

#pragma mark - User interaction

// Called when send button is pressed
- (IBAction)publishMessage:(id)sender {
    
    NSLog(@"%s",__FUNCTION__);
    
    // Get Qeo representation of the UI info
    org_qeo_sample_simplechat_ChatMessage *cm = [self retrieveUserInput];
    
    // send it to the Qeo layer
    [self sendMessage:cm];
}

- (IBAction)userNameChanged:(UITextField *)sender {
    org_qeo_sample_simplechat_ChatParticipant *cp = [[org_qeo_sample_simplechat_ChatParticipant alloc] init];
    cp.name = sender.text;
    cp.state = (int)[self.state selectedSegmentIndex];
    
    [self updateParticipant:cp];
    
}

- (IBAction)stateUpdated:(UISegmentedControl *)sender {
    
    org_qeo_sample_simplechat_ChatParticipant *cp = [[org_qeo_sample_simplechat_ChatParticipant alloc] init];
    cp.name = self.userName.text;
    cp.state = (int)[sender selectedSegmentIndex];

    [self updateParticipant:cp];
}

#pragma mark - Qeo message handling

// Collects UI info from the user
-(org_qeo_sample_simplechat_ChatMessage *)retrieveUserInput {
    
    // Create message container
    org_qeo_sample_simplechat_ChatMessage *cm = [[org_qeo_sample_simplechat_ChatMessage alloc] init];
    
    // store UI info
    cm.from = self.userName.text;
    cm.message = self.userInput.text;
    
    // cleanup UI
    self.userInput.text = @"";
    
    // Remove keyboard
    [self.userInput resignFirstResponder];
    [self.userName resignFirstResponder];
    
    return cm;
}


// Update UI with content of Qeo message
-(void)updateScreenWithChatMessage {
    
    // Don't update UI when not visible
    if ((YES == [self isViewLoaded]) && (nil != self.view.window)) {
        
        // Join all message info
        NSString* newContent = [self.messageLIFO componentsJoinedByString:@"\n "];
        
        // Update UI
        self.history.text = newContent;
    }
}

-(void)sendMessage:(org_qeo_sample_simplechat_ChatMessage *) chatMessage {
    
    if (nil == self.eventWriter || nil == chatMessage){
        return;
    }
    
    // Send message and currentUserName to qeo
    NSError *error;
    [self.eventWriter write:chatMessage withError:&error];
}

// Publishes state of current participant (removes any previous state)
-(void)updateParticipant:(org_qeo_sample_simplechat_ChatParticipant *) chatParticipant {
    
    if (nil == self.participantWriter || nil == chatParticipant){
        return;
    }
    
    NSError *error;
    if (self.previousChatParticipant != nil && ![self.previousChatParticipant.name isEqualToString:chatParticipant.name]){
        [self.participantWriter remove:self.previousChatParticipant withError:&error];
    }
    
    [self.participantWriter write:chatParticipant withError:&error];
    self.previousChatParticipant = chatParticipant;
}

#pragma mark - ApplicationStateProtocol

-(void)setupQeoCommunication {
    NSLog(@"%s",__FUNCTION__);
    NSError* error = nil;
    TabControllerViewController *tc = (TabControllerViewController *)[self parentViewController];
    QEOFactory *factory = tc.factory;
    
    if (nil == factory){
        if (UIApplicationStateBackground == [UIApplication sharedApplication].applicationState) {
            // No UI allowed in background
            NSLog(@"%s FACTORY NIL",__FUNCTION__);
            return;
        }

        dispatch_async(dispatch_get_main_queue(), ^{
            
            // Show Alert box
            self.alert = [[UIAlertView alloc] initWithTitle:@"Qeo Event Reader"
                                                    message:[error localizedDescription]
                                                   delegate:self
                                          cancelButtonTitle:@"Cancel"
                                          otherButtonTitles: nil];
            
            [self.alert show];
            
        });
        
        return;
    }
    
    self.eventReader = [[QEOEventReader alloc] initWithType:[org_qeo_sample_simplechat_ChatMessage class]
                                                    factory:factory
                                                   delegate:self
                                             entityDelegate:nil
                                                      error:&error];
    if (nil == _eventReader){
        if (UIApplicationStateBackground == [UIApplication sharedApplication].applicationState) {
            // No UI allowed in background
            NSLog(@"%s EVENT READER NIL",__FUNCTION__);
            return;
        }

        dispatch_async(dispatch_get_main_queue(), ^{
            
            // Show Alert box
            self.alert = [[UIAlertView alloc] initWithTitle:@"Qeo Event Writer"
                                                    message:[error localizedDescription]
                                                   delegate:self
                                          cancelButtonTitle:@"Cancel"
                                          otherButtonTitles: nil];
            
            [self.alert show];
            
        });
        
        return;
    }
    
    self.eventWriter = [[QEOEventWriter alloc] initWithType:[org_qeo_sample_simplechat_ChatMessage class]
                                                    factory:factory
                                             entityDelegate:nil
                                                      error:&error];
    if (nil == _eventWriter){
        if (UIApplicationStateBackground == [UIApplication sharedApplication].applicationState) {
            // No UI allowed in background
            NSLog(@"%s EVENT WRITER NIL",__FUNCTION__);
            return;
        }

        dispatch_async(dispatch_get_main_queue(), ^{
            
            // Show Alert box
            self.alert = [[UIAlertView alloc] initWithTitle:@"Qeo ChatParticipant Writer"
                                                    message:[error localizedDescription]
                                                   delegate:self
                                          cancelButtonTitle:@"Cancel"
                                          otherButtonTitles: nil];
            
            [self.alert show];
            
        });
        
        
        return;
    }
    
    
    self.participantWriter = [[QEOStateWriter alloc] initWithType:[org_qeo_sample_simplechat_ChatParticipant class]
                                                      factory:factory
                                               entityDelegate:nil
                                                        error:&error];
    if (self.participantWriter == nil){
        if (UIApplicationStateBackground == [UIApplication sharedApplication].applicationState) {
            // No UI allowed in background
            NSLog(@"%s PARTICIPANT WRITER NIL",__FUNCTION__);
            return;
        }
        
        dispatch_async(dispatch_get_main_queue(), ^{
            
            // Show Alert box
            self.alert = [[UIAlertView alloc] initWithTitle:@"Qeo ChatParticipant Writer"
                                                    message:[error localizedDescription]
                                                   delegate:self
                                          cancelButtonTitle:@"Cancel"
                                          otherButtonTitles: nil];
            
            [self.alert show];
            
        });
        
        return;
    }
    
    [self onQeoReady];
}

-(void)onQeoReady {
    NSLog(@"%s",__FUNCTION__);
    org_qeo_sample_simplechat_ChatParticipant *cp = [[org_qeo_sample_simplechat_ChatParticipant alloc] init];
    cp.name = self.userName.text;
    cp.state = (int)[self.state selectedSegmentIndex];
    
    NSError *error;
    if (self.previousChatParticipant != nil && ![self.previousChatParticipant.name isEqualToString:cp.name]){
        [self.participantWriter remove:self.previousChatParticipant withError:&error];
    }
    
    [self.participantWriter write:cp withError:&error];
    self.previousChatParticipant = cp;
}

// Called when going to inactive mode
-(void)willResign
{
    NSLog(@"%s",__FUNCTION__);
    if (nil != self.alert) {
        [self.alert dismissWithClickedButtonIndex:0 animated:NO];
    }
}

// Called when user has indicated the qeo credentials have to be cleared
-(void)onResetQeo
{
    // Close readers/writers and factory
    self.eventReader = nil;
    self.eventWriter = nil;
    self.participantWriter = nil;
}

@end
