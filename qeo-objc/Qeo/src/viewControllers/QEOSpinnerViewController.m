#import "QEOSpinnerViewController.h"
#import <QuartzCore/QuartzCore.h>
#import "qeo/log.h"

@interface QEOSpinnerViewController ()
@property (weak, nonatomic) IBOutlet UIView *spinnerBackground;

@end

@implementation QEOSpinnerViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
   qeo_log_d("%s",__FUNCTION__);
    
    [super viewDidLoad];
    
    self.view.autoresizesSubviews = YES;
    
    // border radius
    [_spinnerBackground.layer setCornerRadius:10.0f];
    
    // border
    [_spinnerBackground.layer setBorderColor:[UIColor lightGrayColor].CGColor];
    [_spinnerBackground.layer setBorderWidth:1.5f];
    
    // drop shadow
    [_spinnerBackground.layer setShadowColor:[UIColor blackColor].CGColor];
    [_spinnerBackground.layer setShadowOpacity:0.8];
    [_spinnerBackground.layer setShadowRadius:3.0];
    [_spinnerBackground.layer setShadowOffset:CGSizeMake(2.0, 2.0)];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

-(void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration {
    
    // BUG: frame is not updated after rotation, bounds position is ok
    //      force frame size before rotation starts to fix it
    self.view.frame = self.parentViewController.view.frame;
    
    [super willAnimateRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];
}


@end
