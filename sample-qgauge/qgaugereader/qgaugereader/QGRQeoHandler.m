#import "QGRQeoHandler.h"

@implementation QGRQeoHandler
{
    QEOFactory     *_factory;
    QEOStateReader *_stateReader;
    QGRIfaceSpeedData *_uiRefreshData;
    QEOStateChangeReader *_deviceInfoReader;
    
    NSTimer *timer;
    BOOL _isAvailable;
    // Popup
    UIAlertView* _alert;
}
/*Initializes the data structures used .*/
-(id)init
{
    self = [super init];
    if (self) {
        self.qeoRequestStarted=NO;
        _deviceList = [[NSMutableArray alloc] init];
        self.deviceInfo = [[NSMutableArray alloc] init];
        _calculatedData=[[NSMutableDictionary alloc] init];
        _nsmCache = [[NSMutableDictionary alloc] init];
        _speedData = [[NSMutableDictionary alloc] init];
        _isAvailable=YES;
        _ifacePlottedData=[[NSMutableDictionary alloc] init];
        
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(willResign:)
                                                     name:@"pauseApp" object:nil];
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(willContinue:)
                                                     name:@"resumeApp" object:nil];
        
        
    }
    return self;
    
}

/*Computes speed for each interafce received over QEO based on timestamp.*/
- (QGRIfaceSpeedData *)computeSpeed:(org_qeo_sample_gauge_NetStatMessage *)nsm{
    
    
    QGRIfaceSpeedData *data=[[QGRIfaceSpeedData alloc] init];
    org_qeo_sample_gauge_NetStatMessage *prevNsm = [_nsmCache objectForKey:nsm.getKeyString];
    
    if (prevNsm!=nil){
        if (nsm.timestamp == prevNsm.timestamp) {
            return nil;
        }
        
        NSNumber *diffTime = [NSNumber numberWithFloat:((nsm.timestamp - prevNsm.timestamp ) / 1000000000)];
        
        /*
         Fill the data object with calculated info and send around UI.
         */
        [data setIfName:nsm.ifName];
        [data setDeviceId:nsm.deviceId];
        
        if(diffTime !=nil && [diffTime intValue] !=0){
            
            double temp=[diffTime doubleValue];
            [data setKbytesIn:(nsm.bytesIn  - prevNsm.bytesIn ) / (temp*1024)];
            [data setKbytesOut:(nsm.bytesOut  - prevNsm.bytesOut ) / (temp*1024)];
            [data setPktsIn:(nsm.packetsIn  - prevNsm.packetsIn ) / temp];
        }
        
        
    }
    
    [_nsmCache setObject:nsm forKey:nsm.getKeyString];
    
    return data;
}

/*Callback received from QEO for statechangereader.*/
- (void)didReceiveStateChange:(QEOType *)state
                    forReader:(QEOStateChangeReader *)stateChangeReader
{
    org_qeo_system_DeviceInfo *di=(org_qeo_system_DeviceInfo *) state;
    
    if(![_deviceInfo containsObject:di]){
        [_deviceInfo addObject:di];
    }
}

/*Initalizes QEO factory adn state readers. */
-(void)setupQeoCommunication {
    
    NSLog(@"%s",__FUNCTION__);
    
   self.qeoRequestStarted = YES;
    // Put actions on background task
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
       
        NSError *error;
        _factory = [[QEOFactory alloc] initWithError:&error];
        
        
        if (_factory == nil){
            NSLog(@"Could not make factory");
            dispatch_async(dispatch_get_main_queue(), ^{
                
                // Show Alert box
                _alert = [[UIAlertView alloc] initWithTitle:@"Qeo" message:[error localizedDescription] delegate:self cancelButtonTitle:nil otherButtonTitles: @"Retry",nil];
                
                [_alert show];
                
            });
            self.qeoRequestStarted = NO;
            return;
        }
        
        _stateReader=[[QEOStateReader alloc]initWithType:[org_qeo_sample_gauge_NetStatMessage class] factory:_factory delegate:self entityDelegate:nil error:&error];
        
        if(_stateReader==nil){
            NSLog(@"Could not make state Reader");
            // Show Alert box
            _alert = [[UIAlertView alloc] initWithTitle:@"Qeo" message:[error localizedDescription] delegate:self cancelButtonTitle:@"Cancel" otherButtonTitles: nil];
            
            [_alert show];
            self.qeoRequestStarted = NO;
            return;
        }
        
        _deviceInfoReader=[[QEOStateChangeReader alloc] initWithType:[org_qeo_system_DeviceInfo class] factory:_factory delegate:self entityDelegate:nil error:&error];
        
        if(_deviceInfoReader==nil){
            NSLog(@"Could not make StateChangeReader for DeviceInfo");
            // Show Alert box
            _alert = [[UIAlertView alloc] initWithTitle:@"Qeo" message:[error localizedDescription] delegate:self cancelButtonTitle:@"Cancel" otherButtonTitles: nil];
            
            [_alert show];
            self.qeoRequestStarted = NO;
            return;
        }
        
        dispatch_async(dispatch_get_main_queue(), ^{
            
            timer = [NSTimer scheduledTimerWithTimeInterval: 1.0
                                                     target: self
                                                   selector:@selector(getQEOUpdate:)
                                                   userInfo: nil repeats:YES];
            
            NSRunLoop *runloop = [NSRunLoop currentRunLoop];
            [runloop addTimer:timer forMode:NSDefaultRunLoopMode];
            
            
        });
        self.qeoRequestStarted = NO;
    });
}

/*Timer fucntion to update UI after every 1 second inetrval.*/
-(void) getQEOUpdate:(NSTimer *)timer{
    //    timer.
  
    @synchronized(self){
        if(_isAvailable){
            
            NSMutableDictionary *tempmap=[[NSMutableDictionary alloc] initWithDictionary:_nsmCache] ;
            NSMutableArray *ifacesToRemove=[[NSMutableArray alloc] init];
            [_stateReader enumerateInstancesUsingBlock:^void (QEOType *qeoType, BOOL *cont) {
                org_qeo_sample_gauge_NetStatMessage *nsm = ( org_qeo_sample_gauge_NetStatMessage *)qeoType;
                
                /* Fill new devicewriter available. */
                
                
                if(![_deviceList containsObject:nsm.deviceId]){
                    
                    [_deviceList addObject:nsm.deviceId];
                    //send notification to masterview
                    org_qeo_system_DeviceInfo *deviceInfo;
                    for(org_qeo_system_DeviceInfo *d in _deviceInfo){
                        if([d.deviceId isEqual:nsm.deviceId]){
                            deviceInfo=d;
                        }
                    }
                    if(deviceInfo!=nil && deviceInfo.deviceId!=nil)
                        [[NSNotificationCenter defaultCenter] postNotificationName:@"addDeviceId" object:deviceInfo
                                                                          userInfo:nil];
                }
                
                
                /* Save calculated data to send around in notification. */
                
                _uiRefreshData=[self computeSpeed:nsm];
                if(_uiRefreshData!=nil){
                    [_calculatedData setObject:_uiRefreshData forKey:nsm.getKeyString];
                }
                
                /*Temporay data structure used to do intermediate operations(deleting removed interfaces and writers,adding new interfaces and writers). */
                
                if([tempmap objectForKey:nsm.getKeyString]){
                    [tempmap removeObjectForKey:nsm.getKeyString];
                }
                
            }];
            
            /*Send notification to update ui with new data and refresh grpah.*/
            if(_calculatedData.count!=0){
                
                for(NSString* key in _calculatedData){
                    QGRIfaceSpeedData *temp=[_calculatedData objectForKey:key];
                    
                    if(temp!=nil && (temp.deviceId!=nil && temp.ifName!=nil)){
                        QGRCachedGraphData *data;
                        if([_ifacePlottedData objectForKey:key]){
                            data=[_ifacePlottedData objectForKey:key];
                        }
                        
                        else {
                            data=[[QGRCachedGraphData alloc]init];
                        }
                        
                        [data.inData addObject:[NSNumber numberWithLongLong:temp.kbytesIn]];
                        [data.outData addObject:[NSNumber numberWithLongLong:temp.kbytesOut]];
                        [_ifacePlottedData setObject:data forKey:key];
                    }
                }
                /*Send notification to refresh detail view with updated interface data.*/
                NSDictionary *speeddata = [NSDictionary dictionaryWithDictionary:_calculatedData];
                [[NSNotificationCenter defaultCenter] postNotificationName:@"speedData" object:nil userInfo:speeddata];
                
                /*Send notification to plot graph.*/
                NSDictionary *graphData = [NSDictionary dictionaryWithDictionary:_ifacePlottedData];
                [[NSNotificationCenter defaultCenter] postNotificationName:@"graphData" object:nil userInfo:graphData];
            }
            
            org_qeo_system_DeviceId *tempkey;
            
            for(NSString *key in [tempmap allKeys]){
                QGRIfaceSpeedData *tempdata=[_nsmCache objectForKey:key];
                [ifacesToRemove addObject:tempdata.ifName];
                tempkey=tempdata.deviceId;
                [_nsmCache removeObjectForKey:key];
                if([_deviceList containsObject:tempdata.deviceId]){
                    [_deviceList removeObject:tempdata.deviceId];
                    
                    org_qeo_system_DeviceInfo *deviceInfo;
                    for(org_qeo_system_DeviceInfo *d in _deviceInfo){
                        if([d.deviceId isEqual:tempdata.deviceId]){
                            deviceInfo=d;
                        }
                    }
                    /*Send notification to update masterview for remove device writers.*/
                    [[NSNotificationCenter defaultCenter] postNotificationName:@"removeDeviceId" object:deviceInfo userInfo:nil];
                }
                
            }
            if(ifacesToRemove.count!=0){
                NSDictionary *removedData = [NSDictionary dictionaryWithObject:ifacesToRemove forKey:@"RemoveIface"];
                
                [[NSNotificationCenter defaultCenter] postNotificationName:@"RemoveIface" object:nil userInfo:removedData];
                
            }
            
            
            //Clean up after use.
            [_calculatedData removeAllObjects];
            tempmap=nil;
            ifacesToRemove=nil;
            tempkey=nil;
            
        }
        
    }
    
}
/*Callback received from QEO for statereader.*/
-(void)didReceiveUpdateForStateReader:
(QEOStateReader *)stateReader
{
    @synchronized(self){
        _isAvailable =YES;
        _stateReader=stateReader;
        
    }
    
}

/*Resumes QEO communication if application is coming to foreground.*/
-(void)willContinue:(NSNotification *)notification
{
    NSLog(@"%s",__FUNCTION__);
    
    // Check the user defaults, the user may have set the reset the Qeo identities in
    // the General settings of the App.
    NSUserDefaults *standardUserDefaults = [NSUserDefaults standardUserDefaults];
    [standardUserDefaults synchronize];
    
    if (YES == [standardUserDefaults boolForKey:@"reset_Qeo"]) {
        // reset
        [QEOIdentity clearQeoIdentities];
        
        // reset the flag in the user defaults
        [standardUserDefaults setObject:@"NO" forKey:@"reset_Qeo"];
        [standardUserDefaults synchronize];
        
        //Close readers/writers  and factory
        _factory = nil;
        _stateReader = nil;
        _deviceInfoReader = nil;
    }
    if ((nil == _factory)&& (NO == self.qeoRequestStarted)) {
        // re-init Qeo
        [self setupQeoCommunication];
    } else if (nil != _factory){
        // Qeo was still running */
        dispatch_async(dispatch_get_main_queue(), ^{
            
            timer = [NSTimer scheduledTimerWithTimeInterval: 1.0
                                                     target: self
                                                   selector:@selector(getQEOUpdate:)
                                                   userInfo: nil repeats:YES];
            
            NSRunLoop *runloop = [NSRunLoop currentRunLoop];
            [runloop addTimer:timer forMode:NSDefaultRunLoopMode];
            
            
        });

    }
    
}

/*Cleans up QEO resources used if application is going to background.*/
-(void)willResign:(NSNotification *)notification
{
    NSLog(@"%s",__FUNCTION__);
    if (nil != _alert) {
        [_alert dismissWithClickedButtonIndex:0 animated:NO];
    }
    
    [timer invalidate];
    timer=nil;
    
}

#pragma mark - UIAlertViewDelegate

// Delegate method to handle button clicks from the UI alert
- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    
    if (buttonIndex != [alertView cancelButtonIndex])
    {
        // Retry pressed, delay needed to allow alert view to close properly before showing the
        // registration dialog again.
        // Too soon gives: "Warning: Attempt to dismiss from view controller"
        [self performSelector:@selector(setupQeoCommunication) withObject:nil afterDelay:0.7];
    }
}

- (void)alertView:(UIAlertView *)alertView didDismissWithButtonIndex:(NSInteger)buttonIndex {
    
    // cleanup
    _alert = nil;
}
@end
