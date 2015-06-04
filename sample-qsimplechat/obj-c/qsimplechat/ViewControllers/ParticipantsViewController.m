#import "ParticipantsViewController.h"
#import "TabControllerViewController.h"
#import "QSimpleChat_ChatParticipant.h"

#pragma mark Extension
@interface ParticipantsViewController ()<QEOStateChangeReaderDelegate>

@property (strong, nonatomic) QEOStateChangeReader *participantReader;
@property (strong, nonatomic) NSMutableDictionary *participants;
@property (strong, nonatomic) NSDictionary *emoijDictionary;

@end

#pragma mark -
@implementation ParticipantsViewController

#pragma mark Memory Management

- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
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
    
    NSString *emoijPlist = [[NSBundle mainBundle] pathForResource:@"emoij" ofType:@"plist"];
    self.emoijDictionary = [[NSDictionary alloc] initWithContentsOfFile:emoijPlist];
}

#pragma mark - ApplicationStateProtocol

-(void)willResign
{
    
}

-(void)onResetQeo
{
    self.participantReader = nil;
}

-(void)onQeoReady
{
    
}

#pragma mark - QEOStateChangeReaderDelegate

// Delegate method that will be called when a state changes for a state change reader
- (void)didReceiveStateChange:(QEOType *)state
                    forReader:(QEOStateChangeReader *)stateChangeReader {
    
    org_qeo_sample_simplechat_ChatParticipant *cp = (org_qeo_sample_simplechat_ChatParticipant *)state;
    
    [self.participants setObject:cp forKey:cp.name];
}


// Delegate method that will be called when a state is removed
- (void)didReceiveStateRemoval:(QEOType *)state
                     forReader:(QEOStateChangeReader *)stateChangeReader {
    
    org_qeo_sample_simplechat_ChatParticipant *cp = (org_qeo_sample_simplechat_ChatParticipant *)state;
    
    [self.participants removeObjectForKey:cp.name];
}


// Could be used to trigger the UI to update the changes when a burst of state updates is received
- (void)didFinishBurstForStateChangeReader:(QEOStateChangeReader *)stateChangeReader {
    
    dispatch_async(dispatch_get_main_queue(), ^{
    
        [[self tableView] reloadData];
    });
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{

    // Return the number of sections.
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{

    // Return the number of rows in the section.
    return [self.participants count];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSString *key = [self.participants allKeys][indexPath.row];
    org_qeo_sample_simplechat_ChatParticipant *cp = [self.participants objectForKey:key];
    
    static NSString *CellIdentifier = @"Cell";
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:CellIdentifier];
    if (cell == nil) {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle  reuseIdentifier:CellIdentifier];
    }
    
    cell.textLabel.text = cp.name;
    switch (cp.state){
        case ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_AVAILABLE:
            cell.detailTextLabel.text = [NSString stringWithFormat:@"%@ Available",[self.emoijDictionary objectForKey:@"available"]];
            cell.detailTextLabel.textColor = [UIColor colorWithRed:0.0 green:0.8 blue:0.0 alpha:1.0];
            break;
        case ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_AWAY:
            cell.detailTextLabel.text = [NSString stringWithFormat:@"%@ Away",[self.emoijDictionary objectForKey:@"away"]];
            cell.detailTextLabel.textColor = [UIColor redColor];
            break;
        case ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_BUSY:
            cell.detailTextLabel.text = [NSString stringWithFormat:@"%@ Busy",[self.emoijDictionary objectForKey:@"busy"]];
            cell.detailTextLabel.textColor = [UIColor orangeColor];
            break;
        case ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_IDLE:
            cell.detailTextLabel.text = [NSString stringWithFormat:@"%@ Idle",[self.emoijDictionary objectForKey:@"idle"]];
            cell.detailTextLabel.textColor = [UIColor purpleColor];
            break;
    }
    
    return cell;
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section
{
    // Table header content
    return @"Chat Participants";
}

- (CGFloat)tableView:(UITableView *)tableView heightForHeaderInSection:(NSInteger)section
{
    // Table header height
    return 50.0;
}

- (void)tableView:(UITableView *)tableView willDisplayHeaderView:(UIView *)view forSection:(NSInteger)section
{
    if([view isKindOfClass:[UITableViewHeaderFooterView class]]){
        
        UITableViewHeaderFooterView *tableViewHeaderFooterView = (UITableViewHeaderFooterView *) view;
        tableViewHeaderFooterView.textLabel.textColor = [UIColor blueColor];
        tableViewHeaderFooterView.textLabel.textAlignment = NSTextAlignmentCenter;
        tableViewHeaderFooterView.textLabel.font = [tableViewHeaderFooterView.textLabel.font fontWithSize:15.0];
    }
}

-(void)setupQeoCommunication {
    NSError* error = nil;
    self.participants = [[NSMutableDictionary alloc] init];
    TabControllerViewController *tc = (TabControllerViewController *)[self parentViewController];
    QEOFactory *factory = tc.factory;
    
    self.participantReader = [[QEOStateChangeReader alloc] initWithType:[org_qeo_sample_simplechat_ChatParticipant class]
                                                            factory:factory
                                                           delegate:self
                                                     entityDelegate:nil
                                                              error:&error];
    if (self.participantReader == nil){
        
        dispatch_async(dispatch_get_main_queue(), ^{
            
            // Show Alert box
            UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Qeo ChatParticipant Reader"
                                                            message:[error localizedDescription]
                                                           delegate:self
                                                  cancelButtonTitle:@"Cancel"
                                                  otherButtonTitles: nil];
            
            [alert show];
            
        });
        
        return;
    }
}

@end
