#import "QeoBackgroundServiceManager.h"
#import <ifaddrs.h>
#import <arpa/inet.h>

//-------------------------------------------------
// States and events of the internal State Machine
//-------------------------------------------------
typedef enum : NSUInteger {
    ForegroundState,
    BackgroundQeoActiveState,
    BackgroundQeoInactiveState,
    BackgroundKeepAliveState
} QeoAplicationStates;

typedef enum : NSUInteger {
    MoveToForegroundEvent,
    MoveToBackgroundEvent,
    QeoDataReceivedEvent,
    QeoDataReceivedEndEvent,
    KeepAliveEvent,
    KeepAliveEndEvent
} QeoApplicationEvents;

//-------------------------------------------------
// Private methods and properties
//-------------------------------------------------
#pragma mark Private extension

@interface QeoBackgroundServiceManager ()

@property (assign) QeoAplicationStates qeoAppState;
@property (strong) dispatch_queue_t  backgroundTaskQueue;
@property (strong) NSString *backgroundIPAddress;

@property (assign) UIBackgroundTaskIdentifier keepAliveBackgroundTaskId;
@property (strong) dispatch_source_t keepAliveTimer;
@property (assign) NSTimeInterval keepAlivePollingTime;
@property (assign) BOOL keepAliveTimerSuspended;

@property (assign) UIBackgroundTaskIdentifier qeoDataNotificationBackgroundTaskId;
@property (strong) dispatch_source_t qeoDataNotificationTimer;
@property (assign) NSTimeInterval qeoDataNotificationTimeout;
@property (assign) BOOL qeoDataNotificationTimerSuspended;
@property (copy) void (^onQeoDataAvailable)(void);

-(void)setupKeepAliveHandler;
-(void)startKeepAliveTimer;
-(void)stopKeepAliveRelatedTasks;
-(void)startQeoDataNotificationTimer;
-(void)stopQeoDataNotificationRelatedTasks;
-(void)changeStateToForeground;
-(void)changeStateToBackgroundQeoInactive;
-(void)changeStateToBackgroundQeoActive;
-(void)changeStateToBackgroundKeepAlive;
-(void)changeQeoApplicationState:(QeoAplicationStates)currentState forEvent:(QeoApplicationEvents)event;

@end

#pragma mark -
@implementation QeoBackgroundServiceManager


#pragma mark Memory Management

-(instancetype)init
{
    // Disable default initializer 
    return nil;
}

-(instancetype)initWithKeepAlivePollingTime:(NSTimeInterval)pollingTime
                               qeoAliveTime:(NSTimeInterval)qeoAliveTime
                           onQeoDataHandler:(void(^)(void))onQeoDataAvailable
{
    NSLog(@"%s",__FUNCTION__);
    
    self = [super init];
    if (self) {
        self.qeoDataNotificationBackgroundTaskId = UIBackgroundTaskInvalid;
        self.keepAliveBackgroundTaskId = UIBackgroundTaskInvalid;
        self.backgroundTaskQueue = dispatch_queue_create("queue.to.serialize.background.tasks", NULL);

        //----------------------------------
        // Validate input
        //----------------------------------
        if (600 > pollingTime) {
            NSLog(@"** WARNING: Keep alive polling time cannot be less then 600 sec (= 10 min.)");
            self.keepAlivePollingTime = 600;
        } else {
            self.keepAlivePollingTime = pollingTime;
        }

        if (0 == qeoAliveTime) {
            NSLog(@"** WARNING: Qeo Alive time cannot be set to zero, default to 30 sec.");
            self.qeoDataNotificationTimeout = 30;
        } else if (600 < qeoAliveTime) {
            NSLog(@"** WARNING: Qeo Alive time cannot be more then 600 sec (= 10 min.)");
            self.qeoDataNotificationTimeout = 595;
        } else {
            self.qeoDataNotificationTimeout = qeoAliveTime;
        }
        
        self.onQeoDataAvailable = onQeoDataAvailable;

        //----------------------------------
        // Setup keep alive timer
        //----------------------------------
        self.keepAliveTimer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER,
                                                     0,
                                                     0,
                                                     self.backgroundTaskQueue);
        if (NULL == self.keepAliveTimer) {
            return nil;
        }

        // Set timer values
        dispatch_source_set_timer(self.keepAliveTimer,
                                  dispatch_time(DISPATCH_TIME_NOW, self.qeoDataNotificationTimeout * NSEC_PER_SEC),
                                  DISPATCH_TIME_FOREVER,
                                  0);
        
        self.keepAliveTimerSuspended = TRUE;

        // Attach handler
        dispatch_source_set_event_handler(self.keepAliveTimer, ^{
            
            dispatch_async(self.backgroundTaskQueue, ^{
                NSLog(@"Keep alive timer timeout handler called !!!!");
                [self changeQeoApplicationState:self.qeoAppState forEvent:KeepAliveEndEvent];
            });
        });
        
        //----------------------------------
        // Setup Qeo Data Notification timer
        //----------------------------------
        self.qeoDataNotificationTimer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER,
                                                               0,
                                                               0,
                                                               self.backgroundTaskQueue);
        
        if (NULL == self.qeoDataNotificationTimer) {
            return nil;
        }
        
        // Set timer values
        dispatch_source_set_timer(self.qeoDataNotificationTimer,
                                  dispatch_time(DISPATCH_TIME_NOW, self.qeoDataNotificationTimeout * NSEC_PER_SEC),
                                  DISPATCH_TIME_FOREVER,
                                  0);
        
        self.qeoDataNotificationTimerSuspended = TRUE;

        // Attach handler
        dispatch_source_set_event_handler(self.qeoDataNotificationTimer, ^{
            
            dispatch_async(self.backgroundTaskQueue, ^{
                NSLog(@"Qeo Data Notification timer timeout handler called !!!!");
                [self changeQeoApplicationState:self.qeoAppState forEvent:QeoDataReceivedEndEvent];
            });
        });

        //----------------------------------
        // Set initial state
        //----------------------------------
        if (UIApplicationStateBackground == [UIApplication sharedApplication].applicationState) {
            self.qeoAppState = BackgroundQeoActiveState;
            // Note: this initial state requires a call to didLaunchInBackground method once
            //       after the factory is registered to the BGNS.
        } else {
            self.qeoAppState = ForegroundState;
            [[UIApplication sharedApplication] clearKeepAliveTimeout];
        }
    }
    return self;
}

-(void)dealloc
{
    NSLog(@"%s",__FUNCTION__);
    
    if(NULL != self.keepAliveTimer)
    {
        // Timer Must be active before you can cancel it
        // otherwise you get EXC_BAD_INSTRUCTION
        [self startKeepAliveTimer];
        dispatch_source_cancel(self.keepAliveTimer);
        self.keepAliveTimer = NULL;
    }
    if(NULL != self.qeoDataNotificationTimer)
    {
        // Timer Must be active before you can cancel it
        // otherwise you get EXC_BAD_INSTRUCTION
        [self startQeoDataNotificationTimer];
        dispatch_source_cancel(self.qeoDataNotificationTimer);
        self.qeoDataNotificationTimer = NULL;
    }
    
    if (UIBackgroundTaskInvalid != self.keepAliveBackgroundTaskId) {
        [[UIApplication sharedApplication] endBackgroundTask:self.keepAliveBackgroundTaskId];
        self.keepAliveBackgroundTaskId = UIBackgroundTaskInvalid;
    }
    
    if (UIBackgroundTaskInvalid != self.qeoDataNotificationBackgroundTaskId) {
        [[UIApplication sharedApplication] endBackgroundTask:self.qeoDataNotificationBackgroundTaskId];
        self.qeoDataNotificationBackgroundTaskId = UIBackgroundTaskInvalid;
    }
}

#pragma mark - QEOBackgroundNotificationServiceDelegate

// External event from Qeo Background Server
- (void)qeoDataAvailableNotificationReceivedForEntities:(NSArray *)readers
{
    NSLog(@"%s",__FUNCTION__);
    [self changeQeoApplicationState:self.qeoAppState forEvent:QeoDataReceivedEvent];
}

#pragma mark - External Application events

- (void)applicationWillEnterForeground:(UIApplication *)application
{
    NSLog(@"%s",__FUNCTION__);
    
    [self changeQeoApplicationState:self.qeoAppState forEvent:MoveToForegroundEvent];
}

- (void)applicationDidEnterBackground:(UIApplication *)application
{
    NSLog(@"%s",__FUNCTION__);
    
    [self changeQeoApplicationState:self.qeoAppState forEvent:MoveToBackgroundEvent];
}

- (void)didLaunchInBackground
{
    NSLog(@"%s",__FUNCTION__);

    self.qeoAppState = BackgroundQeoActiveState;

    dispatch_async(self.backgroundTaskQueue, ^{

        // 1. Create Task ID
        self.qeoDataNotificationBackgroundTaskId = [[UIApplication sharedApplication] beginBackgroundTaskWithExpirationHandler:^{
            [self changeStateToBackgroundQeoInactive];
        }];

        // 2. Reset Keep Alive related tasks
        [self stopKeepAliveRelatedTasks];

        // 3. Start timer
        [self startQeoDataNotificationTimer];
    });
}

#pragma mark - State Machine Events

-(void)changeStateToForeground
{
    NSLog(@"%s",__FUNCTION__);
    
    dispatch_async(self.backgroundTaskQueue, ^{
        
        // 1. Reset Keep Alive handler
        [[UIApplication sharedApplication] clearKeepAliveTimeout];
        
        // 2. Reset Keep Alive related tasks
        [self stopKeepAliveRelatedTasks];
        
        // 3. Reset Qeo Data Notification tasks
        [self stopQeoDataNotificationRelatedTasks];
        
        // 4. Wakeup Qeo
        [QEOFactory resumeQeoCommunication];
        
        // 5. Update new state
        self.qeoAppState = ForegroundState;
    });
}

-(void)changeStateToBackgroundQeoInactive
{
    NSLog(@"%s",__FUNCTION__);
    
    dispatch_async(self.backgroundTaskQueue, ^{
        
        // 1. Suspend Qeo
        [QEOFactory suspendQeoCommunication];
        
        // Order sensitive
        if (BackgroundKeepAliveState == self.qeoAppState) {
            
            // 2. Update new state
            self.qeoAppState = BackgroundQeoInactiveState;
            
            // 3. Reset Qeo Data Notification tasks
            [self stopQeoDataNotificationRelatedTasks];
            
            // 4. Reset Keep Alive related tasks
            [self stopKeepAliveRelatedTasks];
            
        } else {
            
            // 2. Update new state
            self.qeoAppState = BackgroundQeoInactiveState;
        
            // 3. Reset Keep Alive related tasks
            [self stopKeepAliveRelatedTasks];
        
            // 4. Reset Qeo Data Notification tasks
            [self stopQeoDataNotificationRelatedTasks];
        }
    });
}

-(void)changeStateToBackgroundQeoActive
{
    NSLog(@"%s",__FUNCTION__);
    
    dispatch_async(self.backgroundTaskQueue, ^{
        
        // 1. Create Task ID
        self.qeoDataNotificationBackgroundTaskId = [[UIApplication sharedApplication] beginBackgroundTaskWithExpirationHandler:^{
                [self changeStateToBackgroundQeoInactive];
            }];
        
        // 2. Reset Keep Alive related tasks
        [self stopKeepAliveRelatedTasks];
        
        // 3. Start timer
        [self startQeoDataNotificationTimer];
        
        // 4. Wakeup Qeo
        [QEOFactory resumeQeoCommunication];
        
        // 5. Update new state
        self.qeoAppState = BackgroundQeoActiveState;
        
        // 6. Notify
        if (nil != self.onQeoDataAvailable) {
            self.onQeoDataAvailable();
        }
    });
}

-(void)changeStateToBackgroundKeepAlive
{
    NSLog(@"%s",__FUNCTION__);
    
    dispatch_async(self.backgroundTaskQueue, ^{
        
        // 1. Create Task ID
        self.keepAliveBackgroundTaskId = [[UIApplication sharedApplication]
                                          beginBackgroundTaskWithExpirationHandler:^{
                                              [self changeStateToBackgroundQeoInactive];
                                          }];
        
        // 2. Start Keep alive timer
        [self startKeepAliveTimer];
        
        // 3. Wakeup Qeo
        [QEOFactory resumeQeoCommunication];
        
        // 4. Reset Qeo Data Notification tasks
        [self stopQeoDataNotificationRelatedTasks];
        
        // 5. Update new state
        self.qeoAppState = BackgroundKeepAliveState;
    });
}

#pragma mark - State Machine

-(void)changeQeoApplicationState:(QeoAplicationStates)currentState forEvent:(QeoApplicationEvents)event
{
    NSLog(@"%s",__FUNCTION__);
    
    //-----------------------------------
    // Handle events on the state machine
    //-----------------------------------
    switch (currentState) {
        case ForegroundState:
        {
            switch (event) {
                case MoveToBackgroundEvent:
                {
                    [self setupKeepAliveHandler];
                    [self changeStateToBackgroundQeoInactive];
                    break;
                }
                    
                default:
                    break;
            }
            break;
        }
        case BackgroundQeoActiveState:
        {
            switch (event) {
                case MoveToForegroundEvent:
                {
                    [self changeStateToForeground];
                    break;
                }
                case QeoDataReceivedEndEvent:
                {
                    [self changeStateToBackgroundQeoInactive];
                    break;
                }
                    
                default:
                    break;
            }
            break;
        }
        case BackgroundQeoInactiveState:
        {
            switch (event) {
                case MoveToForegroundEvent:
                {
                    [self changeStateToForeground];
                    break;
                }
                case QeoDataReceivedEvent:
                {
                    [self changeStateToBackgroundQeoActive];
                    break;
                }
                case KeepAliveEvent:
                {
                    [self changeStateToBackgroundKeepAlive];
                    break;
                }
                    
                default:
                    break;
            }
            break;
        }
        case BackgroundKeepAliveState:
        {
            switch (event) {
                case MoveToForegroundEvent:
                {
                    [self changeStateToForeground];
                    break;
                }
                case KeepAliveEndEvent:
                {
                    [self changeStateToBackgroundQeoInactive];
                    break;
                }
                    
                default:
                    break;
            }
            break;
        }
            
        default:
            break;
    }
}

#pragma mark - private helper methods

-(void)startKeepAliveTimer
{
    NSLog(@"%s",__FUNCTION__);
    if (YES == self.keepAliveTimerSuspended) {
        // Update start time
        dispatch_source_set_timer(self.keepAliveTimer,
                                  dispatch_time(DISPATCH_TIME_NOW, self.qeoDataNotificationTimeout * NSEC_PER_SEC),
                                  DISPATCH_TIME_FOREVER,
                                  0);
        
        // Start timer
        dispatch_resume(self.keepAliveTimer);
        self.keepAliveTimerSuspended = NO;
    }
}

-(void)stopKeepAliveRelatedTasks
{
    NSLog(@"%s",__FUNCTION__);
    if (NO == self.keepAliveTimerSuspended) {
        dispatch_suspend(self.keepAliveTimer);
        self.keepAliveTimerSuspended = YES;
    }
    if (UIBackgroundTaskInvalid != self.keepAliveBackgroundTaskId) {
        [[UIApplication sharedApplication] endBackgroundTask:self.keepAliveBackgroundTaskId];
        self.keepAliveBackgroundTaskId = UIBackgroundTaskInvalid;
    }
}

-(void)startQeoDataNotificationTimer
{
    NSLog(@"%s",__FUNCTION__);
    if (YES == self.qeoDataNotificationTimerSuspended) {
        // Update start time
        dispatch_source_set_timer(self.qeoDataNotificationTimer,
                                  dispatch_time(DISPATCH_TIME_NOW, self.qeoDataNotificationTimeout * NSEC_PER_SEC),
                                  DISPATCH_TIME_FOREVER,
                                  0);
        
        // Start timer
        dispatch_resume(self.qeoDataNotificationTimer);
        self.qeoDataNotificationTimerSuspended = NO;
    }
}

-(void)stopQeoDataNotificationRelatedTasks
{
    NSLog(@"%s",__FUNCTION__);
    if (NO == self.qeoDataNotificationTimerSuspended) {
        dispatch_suspend(self.qeoDataNotificationTimer);
        self.qeoDataNotificationTimerSuspended = YES;
    }
    if (UIBackgroundTaskInvalid != self.qeoDataNotificationBackgroundTaskId) {
        [[UIApplication sharedApplication] endBackgroundTask:self.qeoDataNotificationBackgroundTaskId];
        self.qeoDataNotificationBackgroundTaskId = UIBackgroundTaskInvalid;
    }
}

-(void)setupKeepAliveHandler
{
    NSLog(@"%s",__FUNCTION__);
    
    // Setup handler
    [[UIApplication sharedApplication] setKeepAliveTimeout:self.keepAlivePollingTime handler:^{
        
        NSLog(@"Keep alive handler callback !!!!");
        [self changeQeoApplicationState:self.qeoAppState forEvent:KeepAliveEvent];
    }];
}

@end
