#import "QEOContainerViewController.h"
#import "QEORegistrationViewController.h"
#import "qeo/log.h"

#define SegueIdentifierOTC @"OTCSegue"
#define SegueIdentifierQRCode @"QRCodeSegue"
#define SegueIdentifierSpinner @"SpinnerSegue"

@interface QEOContainerViewController ()

@property (strong, nonatomic) NSString *currentSegueIdentifier;

- (void)swapFromViewController:(UIViewController *)fromViewController toViewController:(UIViewController *)toViewController;

@end

@implementation QEOContainerViewController


#pragma mark - memory management
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

#pragma mark - view life cycle
- (void)viewDidLoad
{
    [super viewDidLoad];
    
    self.view.autoresizesSubviews = YES;
    [self.view setAutoresizingMask:UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight];
	
    _currentSegueIdentifier = SegueIdentifierOTC;
    [self performSegueWithIdentifier:_currentSegueIdentifier sender:nil];
}

#pragma mark - Segue handling

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    if ([segue.identifier isEqualToString:SegueIdentifierOTC])
    {
        if (self.childViewControllers.count > 0) {
            [self swapFromViewController:[self.childViewControllers objectAtIndex:0] toViewController:segue.destinationViewController];
        }
        else {
            [self addChildViewController:segue.destinationViewController];
            ((UIViewController *)segue.destinationViewController).view.frame = CGRectMake(0, 0, self.view.frame.size.width, self.view.frame.size.height);
            [self.view addSubview:((UIViewController *)segue.destinationViewController).view];
            [segue.destinationViewController didMoveToParentViewController:self];
        }
    }
    else if ([segue.identifier isEqualToString:SegueIdentifierQRCode])
    {
        [self swapFromViewController:[self.childViewControllers objectAtIndex:0] toViewController:segue.destinationViewController];
    }
    else if ([segue.identifier isEqualToString:SegueIdentifierSpinner])
    {
        // Add new subview to container
        self.currentSegueIdentifier = SegueIdentifierSpinner;
        [self addChildViewController:segue.destinationViewController];
        ((UIViewController *)segue.destinationViewController).view.frame = CGRectMake(0, 0, self.view.frame.size.width, self.view.frame.size.height);
        [self.view addSubview:((UIViewController *)segue.destinationViewController).view];
        
        // Transition previous from container
        [[self.childViewControllers objectAtIndex:0] willMoveToParentViewController:nil];
        [[self.childViewControllers objectAtIndex:0] removeFromParentViewController];
        
        // Make new one front view controller
        [segue.destinationViewController didMoveToParentViewController:self];
    }
}

#pragma mark - Swapping child view controllers

- (void)swapFromViewController:(UIViewController *)fromViewController toViewController:(UIViewController *)toViewController
{
    toViewController.view.frame = CGRectMake(0, 0, self.view.frame.size.width, self.view.frame.size.height);
    
    [fromViewController willMoveToParentViewController:nil];
    [self addChildViewController:toViewController];
    [self transitionFromViewController:fromViewController toViewController:toViewController duration:1.0 options:UIViewAnimationOptionTransitionCrossDissolve animations:nil completion:^(BOOL finished) {
        [fromViewController removeFromParentViewController];
        [toViewController didMoveToParentViewController:self];
    }];
}

- (void)swapViewControllers
{
    self.currentSegueIdentifier = ([_currentSegueIdentifier isEqualToString:SegueIdentifierOTC]) ? SegueIdentifierQRCode : SegueIdentifierOTC;
    [self performSegueWithIdentifier:_currentSegueIdentifier sender:nil];
}

#pragma mark - Registration

-(void)registerToQeoWith:(NSString*)otc url:(NSString*)url {
    
    qeo_log_d("%s, found OTC:%@, found URL: %@",__FUNCTION__,otc,url);
    
    // Show spinner
    self.currentSegueIdentifier = SegueIdentifierSpinner;
    [self performSegueWithIdentifier:_currentSegueIdentifier sender:nil];
    
    
    QEORegistrationViewController* parent = (QEORegistrationViewController*)self.parentViewController;
    [parent registerToQeoWith:otc url:url];
}

-(void)cancelRegistration {
    qeo_log_d("cancelled registration process");
    QEORegistrationViewController* parent = (QEORegistrationViewController*)self.parentViewController;
    [parent cancelRegistration];
}

@end
