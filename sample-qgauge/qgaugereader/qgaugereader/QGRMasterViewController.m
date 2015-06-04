#import "QGRMasterViewController.h"
#import <Qeo/Qeo.h>

#import "QGRDetailViewController.h"
#import "QGauge_NetStatMessage.h"

@interface QGRMasterViewController ()
- (void)configureCell:(UITableViewCell *)cell atIndexPath:(NSIndexPath *)indexPath;

@end

@implementation QGRMasterViewController{
    
    
    BOOL _qeoInitialized;
}

- (void)awakeFromNib
{
    if ([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPad) {
        self.clearsSelectionOnViewWillAppear = NO;
        self.preferredContentSize = CGSizeMake(320.0, 600.0);
    }
    [super awakeFromNib];
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    _qeohandler=[[QGRQeoHandler alloc]init];
    _deviceList = [[NSMutableArray alloc] init];
  
    self.detailViewController = (QGRDetailViewController *)[[self.splitViewController.viewControllers lastObject] topViewController];
    
    /*Register masterview to listen to notifications related to Device publisher available/removed.*/
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(updateScreenWithDevices:)
                                                 name:@"addDeviceId" object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(updateScreenWithDevices:)
                                                 name:@"removeDeviceId" object:nil];
    
    
    
    
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

-(void)dealloc
{
    // Remove listeners
    
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}


- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    
    return [_deviceList count];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:@"Cell" forIndexPath:indexPath];
    [self configureCell:cell atIndexPath:indexPath];
    return cell;
}

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath
{
    // Return NO if you do not want the specified item to be editable.
    return NO;
}


- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath
{
    // The table view should not be re-orderable.
    return NO;
}


- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    if ([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPad) {
      NSManagedObject *object = [self.deviceList objectAtIndex:indexPath.row];
        org_qeo_system_DeviceInfo *d=( org_qeo_system_DeviceInfo *)object;
        self.detailViewController.detailItem=d.deviceId;
    }
}

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    if ([[segue identifier] isEqualToString:@"showDetail"]) {
        NSIndexPath *indexPath = [self.tableView indexPathForSelectedRow];
        NSManagedObject *object = [self.deviceList objectAtIndex:indexPath.row];
        org_qeo_system_DeviceInfo *d=( org_qeo_system_DeviceInfo *)object;
        [[segue destinationViewController] setDetailItem:d.deviceId];
        
    }
}

- (void)configureCell:(UITableViewCell *)cell atIndexPath:(NSIndexPath *)indexPath
{
    NSManagedObject *object=[self.deviceList objectAtIndex:indexPath.row];
    
    /*Shows the user friendly name for Devices publishing NetStatMessage. */
    org_qeo_system_DeviceInfo *d = (org_qeo_system_DeviceInfo *) object;
    cell.textLabel.text = d.userFriendlyName;
    
}

/*Refresh the visible list of devices publishing netstatmessage. */

-(void)updateScreenWithDevices:(NSNotification *)notification {
    
    if ([[notification name] isEqualToString:@"addDeviceId"])
    {
       [_deviceList addObject:[notification object]];
        NSLog (@"device is successfully added %lu!", (unsigned long)_deviceList.count);
       
    }
    else if([[notification name] isEqualToString:@"removeDeviceId"]){
        [_deviceList removeObject:[notification object]];
        NSLog (@"device is successfully removed %lu!", (unsigned long)_deviceList.count);
        
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        [self.tableView reloadData];
        
    });
}

-(void)updateScreenWithSpeedData {
    [self.detailViewController configureView];
}
@end
