#import "QEORegistrationViewController.h"
#import "qeo/log.h"
#import "QEOPlatform.h"
#import "QEOFactoryContext.h"


@interface QEORegistrationViewController ()


@end

@implementation QEORegistrationViewController


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
    [super viewDidLoad];
	
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    
}


-(void)registerToQeoWith:(NSString*)otc url:(NSString*)url {
    
    qeo_log_d("%s, found OTC:%@, found URL: %@",__FUNCTION__,otc,url);
    
    [self.context registerToQeoWith:otc url:url];
    
}

-(void)cancelRegistration {
    qeo_log_d("cancelled registration process");
    
    [self.context cancelRegistration];
}

@end
