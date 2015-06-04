#import "QGRDetailViewController.h"
#import "QGRIfaceSpeedData.h"
@interface QGRDetailViewController ()
@property (strong, nonatomic) UIPopoverController *masterPopoverController;
-(void)refreshData:(NSNotification *)notification;
@end

@implementation QGRDetailViewController

{
    BOOL _isVisible;
    UIActivityIndicatorView *spinner;
   
}
#pragma mark - Managing the detail item

- (void)setDetailItem:(id)newDetailItem
{
    if (_detailItem != newDetailItem) {
        _detailItem = newDetailItem;
        
        // Update the view.
        [self configureView];
    }
    
    if (self.masterPopoverController != nil) {
        [self.masterPopoverController dismissPopoverAnimated:YES];
    }
}

/* Refreshed the list of interface details for selected device.*/
-(void)refreshData:(NSNotification *)notification{
    
    __weak QGRDetailViewController* me = self;
    __block NSNotification *not = notification;
   
    if ([[notification name] isEqualToString:@"speedData"])
    {
        dispatch_async(dispatch_get_main_queue(), ^{
            NSDictionary *_nsmCache=[not userInfo];
            
            if (me.detailItem) {
                me.detailDescriptionLabel.text = [me.detailItem description];
            }
            
            [_speedData removeAllObjects];
            for(NSString *key in _nsmCache){
                QGRIfaceSpeedData *d=[_nsmCache objectForKey:key];
                if([d.deviceId isEqual:self.detailItem]){
                    [_speedData setObject:d forKey:key];
                }
            }
            if(_isVisible){
                
                [spinner stopAnimating];
                [self.tableView reloadData];
            }
         
            _nsmCache = nil;
            not = nil;
        });
        
    }
    else if ([[notification name] isEqualToString:@"RemoveIface"]){
       
        dispatch_async(dispatch_get_main_queue(), ^{
            
            NSArray *obj=[[not userInfo] valueForKey:@"RemoveIface"];
            for(NSString* key in obj){
                [_speedData removeObjectForKey:key];
            }
            if(_isVisible){
                 [self.tableView reloadData];
            }
            not = nil;
        });
      
    }
    
}
- (void)configureView
{
    // Update the user interface for the detail item.
    if (self.detailItem) {
        self.detailDescriptionLabel.text = [self.detailItem description];
    }
    
}

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.
    
    
    self.tableView.delegate = self;
    self.tableView.dataSource = self;
    _speedData = [[NSMutableDictionary alloc] init];
   
    [self configureView];
    
     /*Register detailviewcontroller to listen to notifications related to new data available/removed interafce.*/
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(refreshData:)
                                                 name:@"speedData" object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(refreshData:)
                                                 name:@"RemoveIface" object:nil];
    
    //initialize graphviewcontroller
    
    self.graphViewController=[[QGRGraphViewController alloc ]init];
    [self addChildViewController:self.graphViewController];
    
    //spinner to show progress bar going on while updating the dictionary data.
    spinner = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleGray];
    spinner.center = CGPointMake(160, 240);
    spinner.hidesWhenStopped = YES;
    [self.view addSubview:spinner];
    [spinner startAnimating];
    
}
- (void)viewWillDisappear:(BOOL)animated{
    [super viewWillDisappear:animated];
    _isVisible=NO;
}

- (void)viewWillAppear:(BOOL)animated{
    [super viewWillAppear:animated];
    _isVisible=YES;
}
#pragma mark - UIViewController lifecycle methods
-(void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    _isVisible=YES;
    
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Split view

- (void)splitViewController:(UISplitViewController *)splitController willHideViewController:(UIViewController *)viewController withBarButtonItem:(UIBarButtonItem *)barButtonItem forPopoverController:(UIPopoverController *)popoverController
{
    barButtonItem.title = NSLocalizedString(@"Master", @"Master");
    [self.navigationItem setLeftBarButtonItem:barButtonItem animated:YES];
    self.masterPopoverController = popoverController;
}

- (void)splitViewController:(UISplitViewController *)splitController willShowViewController:(UIViewController *)viewController invalidatingBarButtonItem:(UIBarButtonItem *)barButtonItem
{
    // Called when the view is shown again in the split view, invalidating the button and popover controller.
    [self.navigationItem setLeftBarButtonItem:nil animated:YES];
    self.masterPopoverController = nil;
}

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    if ([[segue identifier] isEqualToString:@"showGraph"]) {
        NSIndexPath *indexPath = [self.tableView indexPathForSelectedRow];
        //Pass key to graphview instead of interface name to avoid collision between same name interfaces of different devices.
        [[segue destinationViewController] setSelectedIface:[[self.speedData allKeys] objectAtIndex:indexPath.row]];

    }
}

-(void)dealloc
{
    // Remove listeners
    
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

-(NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    return 1;
}

-(NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return _speedData.count;
    
}

-(CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
    return 75;
}

-(UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    
   
    static NSString *cellIdentifier = @"customCellID";
   
    QGRCustomCell *tablecell = (QGRCustomCell *)[tableView dequeueReusableCellWithIdentifier:cellIdentifier];
    
    NSArray *keys=[_speedData allKeys];
    NSString *key=[keys objectAtIndex:indexPath.row];
    QGRIfaceSpeedData *value=[_speedData objectForKey:key];
    tablecell.ifaceLabel.text=value.ifName;
    
       tablecell.inBytesLabel.text=[NSString stringWithFormat:@"%lld", value.kbytesIn ];
       tablecell.outBytesaceLabel.text=[NSString stringWithFormat:@"%lld",value.kbytesOut];

     return tablecell;
    
}

@end
