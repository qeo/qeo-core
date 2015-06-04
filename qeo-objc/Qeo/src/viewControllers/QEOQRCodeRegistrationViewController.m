#import "QEOQRCodeRegistrationViewController.h"
#import "QEOContainerViewController.h"
#import "qeo/log.h"

@interface QEOQRCodeRegistrationViewController ()
@property (weak, nonatomic) IBOutlet UIView *preview;
@property (weak, nonatomic) IBOutlet UIButton *switchToOTCButton;
@property (weak, nonatomic) IBOutlet UIButton *cancelButton;
@property (weak, nonatomic) IBOutlet UILabel *invalidQRCodeLabel;

@property (nonatomic, strong) AVCaptureSession *captureSession;
@property (nonatomic, strong) AVCaptureVideoPreviewLayer *videoPreviewLayer;

-(IBAction)switchToOTCRegistration:(UIButton *)sender;
-(IBAction)cancelRegistration:(UIButton *)sender;
-(void)startCapture;
-(void)stopCapture;
-(void)hideErrorLabel;
-(void)showErrorLabel;

@end

@implementation QEOQRCodeRegistrationViewController
{
    NSTimer* myTimer;
}


#pragma mark - Memory management

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

#pragma mark - View life cycle

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    _invalidQRCodeLabel.hidden = YES;
   	_captureSession = nil;
    [self startCapture];
}

-(void)viewDidLayoutSubviews {
        
    // Available height
    CGFloat maxHeight = self.view.bounds.size.height;
    CGFloat maxWidth  = self.view.bounds.size.width;
    
    CGRect newPreviewFrame;
    CGFloat maxAllowedHeight;
    CGFloat maxAllowedWidth;
    CGFloat xPosOffset;
    CGFloat newWidth;
    CGFloat newHeight;
    
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
        maxAllowedHeight = 840.0;
        maxAllowedWidth = 680.0;
        xPosOffset = 40.0;
    }
    else {
        xPosOffset = 20.0;
        maxAllowedHeight = 350.0;
        maxAllowedWidth = 280.0;
    }
    
    UIInterfaceOrientation deviceOrientation = [UIApplication sharedApplication].statusBarOrientation;
    
    if (UIInterfaceOrientationIsLandscape(deviceOrientation)) {
        
        // Position/resize preview frame
        newWidth  = (maxHeight - xPosOffset)*1.3;
        newHeight = maxHeight - xPosOffset;
        newPreviewFrame = CGRectMake(xPosOffset,
                                     0.0,
                                     (newWidth <  maxAllowedWidth)?newWidth:maxAllowedWidth,
                                     (newHeight < maxAllowedHeight)?newHeight:maxAllowedHeight);
        self.preview.frame = newPreviewFrame;
        
        // Position switch button
        CGRect controlFrame = self.switchToOTCButton.frame;
        CGFloat spacer = (maxWidth - newPreviewFrame.size.width - xPosOffset - controlFrame.size.width)/2;
        controlFrame.origin.x = xPosOffset + newPreviewFrame.size.width + spacer;
        
        if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
            // move up
            controlFrame.origin.y = controlFrame.origin.y - 60;
        }
        self.switchToOTCButton.frame = controlFrame;
        
        // Position cancel button
        controlFrame = self.cancelButton.frame;
        spacer = (maxWidth - newPreviewFrame.size.width - xPosOffset - controlFrame.size.width)/2;
        controlFrame.origin.x = xPosOffset + newPreviewFrame.size.width + spacer;
        self.cancelButton.frame = controlFrame;
        
        // Position of the error label
        controlFrame = self.invalidQRCodeLabel.frame;
        spacer = (maxWidth - newPreviewFrame.size.width - xPosOffset - controlFrame.size.width)/2;
        controlFrame.origin.x = xPosOffset + newPreviewFrame.size.width + spacer;
        
        if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
            // move up
            controlFrame.origin.y = controlFrame.origin.y - 60;
        }
        self.invalidQRCodeLabel.frame = controlFrame;
        
        // Position/resize/orientate video layer
        if (nil != _videoPreviewLayer) {
            [_videoPreviewLayer setFrame:_preview.layer.bounds];
            
            if (deviceOrientation == UIInterfaceOrientationLandscapeLeft) {
                [_videoPreviewLayer.connection setVideoOrientation:AVCaptureVideoOrientationLandscapeLeft];
            } else {
                [_videoPreviewLayer.connection setVideoOrientation:AVCaptureVideoOrientationLandscapeRight];
            }
        }
    } else {
        // Keep autolayout positions of the controls
        // only the preview and video layer needs resizing.
        
        // Position/resize preview frame
        newWidth  = maxWidth - (2*xPosOffset);
        newHeight = (maxWidth - (2*xPosOffset))*1.3;
        newPreviewFrame = CGRectMake(xPosOffset,
                                     0.0,
                                     (newWidth <  maxAllowedWidth)?newWidth:maxAllowedWidth,
                                     (newHeight < maxAllowedHeight)?newHeight:maxAllowedHeight);
        self.preview.frame = newPreviewFrame;
        
        // Position/resize/orientate video layer
        if (nil != _videoPreviewLayer) {
            [_videoPreviewLayer setFrame:_preview.layer.bounds];
            [_videoPreviewLayer.connection setVideoOrientation:AVCaptureVideoOrientationPortrait];
        }
    }
}


-(void)viewDidDisappear:(BOOL)animated {
    [self stopCapture];
}

-(void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration {
    
    // BUG: frame is not updated after rotation, bounds position is ok
    //      force frame size before rotation starts to fix it
    self.view.frame = self.parentViewController.view.frame;
    
    [super willAnimateRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];
}


#pragma mark - AVCaptureMetadataOutputObjectsDelegate

-(void)captureOutput:(AVCaptureOutput *)captureOutput didOutputMetadataObjects:(NSArray *)metadataObjects fromConnection:(AVCaptureConnection *)connection {
    if (metadataObjects != nil && [metadataObjects count] > 0) {
        
        AVMetadataMachineReadableCodeObject *metadataObj = [metadataObjects objectAtIndex:0];
        if ([[metadataObj type] isEqualToString:AVMetadataObjectTypeQRCode]) {
            
            // Split the found value on char ";"
            // Qeo QR codes contain 2 values in the form of: OTC;URL
            NSArray* metaData = [[metadataObj stringValue] componentsSeparatedByString: @";"];
            
            if (metaData.count >= 2){
                [self performSelectorOnMainThread:@selector(hideErrorLabel) withObject:nil waitUntilDone:NO];
                
                // Detected a possible valid Qeo QR code
                QEOContainerViewController* parent = (QEOContainerViewController*)self.parentViewController;
                [parent registerToQeoWith:(NSString*)[metaData objectAtIndex:0] url:(NSString*)[metaData objectAtIndex:1]];
            
                // Stop scanning
                [self performSelectorOnMainThread:@selector(stopCapture) withObject:nil waitUntilDone:NO];
                
            } else {
                
                [self performSelectorOnMainThread:@selector(showErrorLabel) withObject:nil waitUntilDone:NO];
            }
        }
    }
}


#pragma mark - Caputre

- (void)startCapture {
    NSError *error;
    AVCaptureDevice *captureDevice = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    AVCaptureDeviceInput *input = [AVCaptureDeviceInput deviceInputWithDevice:captureDevice error:&error];
    
    if (!input) {
        //TODO
        qeo_log_d("%@", [error localizedDescription]);
        
        return;
    }
    
    _captureSession = [[AVCaptureSession alloc] init];
    [_captureSession addInput:input];
    AVCaptureMetadataOutput *captureMetadataOutput = [[AVCaptureMetadataOutput alloc] init];
    [_captureSession addOutput:captureMetadataOutput];
    
    // We must take a serial dispatch queue
    dispatch_queue_t dispatchQueue = dispatch_queue_create("backgroundQueue", NULL);
    [captureMetadataOutput setMetadataObjectsDelegate:self queue:dispatchQueue];
    
    // We are only interested in QR metadata
    [captureMetadataOutput setMetadataObjectTypes:[NSArray arrayWithObject:AVMetadataObjectTypeQRCode]];
    
    _videoPreviewLayer = [[AVCaptureVideoPreviewLayer alloc] initWithSession:_captureSession];
    [_videoPreviewLayer setVideoGravity:AVLayerVideoGravityResizeAspectFill];
    [_videoPreviewLayer setFrame:_preview.layer.bounds];
    [_preview.layer addSublayer:_videoPreviewLayer];
    
    [_captureSession startRunning];
}

-(void)stopCapture {
    [_captureSession stopRunning];
    _captureSession = nil;
    [_videoPreviewLayer removeFromSuperlayer];
}

-(void)hideErrorLabel {
    _invalidQRCodeLabel.hidden = YES;
}

-(void)showErrorLabel {
    
    [myTimer invalidate];
    myTimer = nil;
    
    _invalidQRCodeLabel.hidden = NO;
    
    myTimer = [NSTimer scheduledTimerWithTimeInterval:(.3f)
                                               target:self
                                             selector:@selector(hideErrorLabel)
                                             userInfo:nil
                                              repeats:NO];
}


#pragma mark - Switch

- (IBAction)switchToOTCRegistration:(UIButton *)sender {
    
    QEOContainerViewController* parent = (QEOContainerViewController*)self.parentViewController;
    [parent swapViewControllers];
}

- (IBAction)cancelRegistration:(UIButton *)sender {
    
    QEOContainerViewController* parent = (QEOContainerViewController*)self.parentViewController;
    [parent cancelRegistration];
}

@end
