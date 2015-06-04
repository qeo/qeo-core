#import "QEOFactory.h"
#import "QEOIdentity.h"
#import "qeocore/api.h"
#import "qeo/log.h"
#import "QEOPlatform.h"
#import <qeo/util_error.h>
#import "QEOFactoryContext.h"
#import <UIKit/UIKit.h>
#import "dds_aux.h"
#import "QEOEntityWrapper.h"

#pragma mark Extension
@interface QEOFactory()

@property (readwrite, copy, nonatomic) QEOIdentity *identity;
@property (nonatomic,strong,readonly) QEOFactoryContext *context;
@property (nonatomic, strong) NSMutableArray *readerList;

- (BOOL)createNativeFactoryWithQeoIdentity:(QEOIdentity *)identity
                                     error:(NSError **)error;
- (BOOL)destroyVoipStream:(int)fileDescriptor;
- (BOOL)createVoipStream:(int)fileDescriptor;
@end

#pragma mark -
@implementation QEOFactory
{
    NSMutableDictionary *socketMap;
}

#pragma mark Memory management
- (instancetype)init
{
   return [self initWithError:nil];
}

- (instancetype)initWithError:(NSError **)error
{
     /* Currently retrieveQeoIdentities does not work
      * Uncomment when working on multirealm             */
     /*
        NSArray *identities   = [QEOIdentity retrieveQeoIdentities];
        QEOIdentity *identity = [identities count] > 0 ? identities[0] : nil;
      */
    QEOIdentity *identity = nil;
    return [self initWithQeoIdentity:identity error:error];
   
}

- (BOOL)createNativeFactoryWithQeoIdentity:(QEOIdentity *)identity
                                     error:(NSError **)error
{
    qeo_log_d("Going to create factory");
    
    if ([identity isOpen] == YES){
        _factory = qeo_factory_create_by_id(QEO_IDENTITY_OPEN);
        if (self.factory != nil){
            _identity = identity;
        }
        
    } else {
        // BGNS server only supported for private domain
        socketMap = [NSMutableDictionary dictionary];
        self.readerList = [NSMutableArray array];

        /* Currently qeo-c-core does not support real multirealm...*/
        _factory = qeo_factory_create_by_id(QEO_IDENTITY_DEFAULT);
        
        /* remove this hack when we support multirealm */
        if (self.factory != nil){
            _identity = [QEOIdentity retrieveQeoIdentities][0];
        }
    }
    
    if (self.factory == nil){
        if (error != nil){
            NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Could not create factory"};
            *error = [[NSError alloc]initWithDomain:@"org.qeo" code:QEO_EFAIL userInfo:userInfo];
        }
        return NO;
    }

    // Only good casses pass this point
    if (error != nil) {
        NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"OK"};
        *error = [[NSError alloc]initWithDomain:@"org.qeo" code:QEO_OK userInfo:userInfo];
    }
    return YES;
}

- (instancetype)initWithQeoIdentity:(QEOIdentity *)identity
                              error:(NSError **)error
{
    // Initialize platform first
    QEOPlatform *platform = [QEOPlatform sharedInstance];
    if (nil == platform) {
       if (error != nil) {
           NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Platform inialization failed"};
            *error = [[NSError alloc]initWithDomain:@"org.qeo" code:QEO_EFAIL userInfo:userInfo];
        }
        return nil;
    }
    
    qeo_log_d("ObjC platform (%d)",platform);
    
    // We currently do not support multiple Factories on private domains
    // We do allow it on open domain on iOS level, we depend on DDS to allow/disallow it on lower levels
    if (([identity isOpen] == NO) && (nil != platform.factoryContext)) {
        if (error != nil) {
            NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Multiple Factories not supported"};
            *error = [[NSError alloc]initWithDomain:@"org.qeo" code:QEO_EFAIL userInfo:userInfo];
        }
        return nil;
    }
    
    self = [super init];
    if (!self) {
        
        if (error != nil) {
            NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Factory internal error"};
            *error = [[NSError alloc]initWithDomain:@"org.qeo" code:QEO_EFAIL userInfo:userInfo];
        }
        return self;
    }
    
    // There are no callbacks on the open domain
    if ([identity isOpen] == NO) {
        // Create Factory context
        _context = [[QEOFactoryContext alloc]initWithFactory:self];
        qeo_log_d("ObjC factory (%d), ObjC factory context (%d)",self,_context);
    }
    
    // Create the native Qeo counterpart
    BOOL result = [self createNativeFactoryWithQeoIdentity:identity error:error];
    
    if (NO == result) {
        if (nil != _context) {
            [_context closeContext];
            _context = nil;
        }
        
        return nil;
    }
    
    // For open domain we do not have a registration dialog
    if (NO == [identity isOpen]) {
        
        [_context closeContext];
        _context = nil;
    }
    
    return self;
}

- (instancetype)initWithFactoryDelegate:(id <QEOFactoryDelegate>) delegate
                                  error:(NSError **)error
{
    if (nil != delegate) {
        
        // Initialize platform first
        QEOPlatform *platform = [QEOPlatform sharedInstance];
        if (nil == platform) {
            if (error != nil) {
                NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Platform inialization failed"};
                *error = [[NSError alloc]initWithDomain:@"org.qeo" code:QEO_EFAIL userInfo:userInfo];
            }
            return nil;
        }
        
        // We currently do not support multiple Factories on private domains
        if (nil != platform.factoryContext) {
            if (error != nil) {
                NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Multiple Factories not supported"};
                *error = [[NSError alloc]initWithDomain:@"org.qeo" code:QEO_EFAIL userInfo:userInfo];
            }
            return nil;
        }
        
        self = [super init];
        if (!self) {
            if (error != nil) {
                NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Factory internal error"};
                *error = [[NSError alloc]initWithDomain:@"org.qeo" code:QEO_EFAIL userInfo:userInfo];
            }
            return self;
        }
    
        // Store delegate
        self.delegate = delegate;
        
        // Create Factory context
        _context = [[QEOFactoryContext alloc]initWithFactory:self];
        
        // Create the native Qeo counterpart
        QEOIdentity *identity = nil;
        if (NO == [self createNativeFactoryWithQeoIdentity:identity error:error]) {
            
            [_context closeContext];
            _context = nil;
            return nil;
        }
        return self;
        
    } else {
    
        // call the default initializer with eror
        return [self initWithError:error];
    }
}

- (void)dealloc
{
    if (nil != _context) {
        [_context closeContext];
    }
    if (nil != socketMap) {
        // Private domain
        qeo_bgns_register(NULL,(uintptr_t)(__bridge void *)self);

        // Cleanup any open stream sockets
        if (0 != [socketMap count]) {
            qeo_log_d("Cleanup VoIP streams that are still open");

            // Some streams are still present => cleanup
            for (NSNumber *key in socketMap) {
                NSValue *streamWrapper = [socketMap objectForKey:key];
                if (nil != streamWrapper) {
                    CFReadStreamRef stream = (CFReadStreamRef)[streamWrapper pointerValue];
                    // Close and release stream
                    CFReadStreamClose(stream);
                    CFRelease(stream);
                }
            }
        }
    }
    qeo_factory_close(_factory);
    
}

#pragma mark - qeo-c-core callbacks

static void on_qeo_bgns_on_wakeup(uintptr_t userdata, const char *type_name) {
    qeo_log_d("type_name: %s",type_name);

    QEOFactory *factory = (__bridge QEOFactory*)(void *)userdata;
    if (nil == factory) {
        qeo_log_e("Could not retrieve a valid factory object");
        return;
    }

    // Call delegate
    __strong id<QEOBackgroundNotificationServiceDelegate> delegate = factory.bgnsCallbackDelegate;
    if (nil != delegate) {
        
        // Find all Readers associated with the given topic
        NSMutableArray *readers = [NSMutableArray array];
        if (type_name != NULL) {
            // Type_name can be null if the BGNS could not create the local readers.
            // It will cause a wake-up event and the application will need to go back to sleep after a little while
            NSString *qeoTypeName = [NSString stringWithUTF8String:type_name];
            
            NSMutableIndexSet *indexesToDelete = [NSMutableIndexSet indexSet];
            for (NSUInteger currentIndex = 0;currentIndex < factory.readerList.count;++currentIndex) {
                QEOEntityWrapper *wrapper = [factory.readerList objectAtIndex:currentIndex];
                if (YES == [wrapper.qeoTypeName isEqualToString:qeoTypeName]) {
                    // strongify first
                    __strong QEOEntity *entity = wrapper.entity;
                    if (nil != entity) {
                        [readers addObject:entity];
                    } else {
                        [indexesToDelete addIndex:currentIndex];
                    }
                }
            }
            // Remove invalid entities
            if (0 < indexesToDelete.count) {
                [factory.readerList removeObjectsAtIndexes:indexesToDelete];
            }
        }
        [delegate qeoDataAvailableNotificationReceivedForEntities:readers];
    }
}

static void on_qeo_bgns_on_connect(uintptr_t userdata, int fd, bool connected) {
    qeo_log_d("filedescriptor %d, connected: %d",fd,connected);

    QEOFactory *factory = (__bridge QEOFactory*)(void *)userdata;
    if (nil == factory) {
        qeo_log_e("Could not retrieve a valid factory object");
        return;
    }

    if (true == connected){
        [factory createVoipStream:fd];
    } else {
        [factory destroyVoipStream:fd];
    }
}

#pragma mark - Background handling

+ (void)suspendQeoCommunication
{
    qeo_bgns_suspend();
    NSLog(@"%s QEO IN SUSPEND MODE",__FUNCTION__);
}

+ (void)resumeQeoCommunication
{
    qeo_bgns_resume();
    NSLog(@"%s QEO IN ACTIVE MODE",__FUNCTION__);
}

- (void)setBgnsCallbackDelegate:(id<QEOBackgroundNotificationServiceDelegate>)bgnsCallbackDelegate
{
    qeo_log_d("%s",__FUNCTION__);

    if (nil == socketMap) {
        // Ignore => socketMap is only nil for public domains, BGNS is only supported for private domains
        return;
    }
    _bgnsCallbackDelegate = bgnsCallbackDelegate;
    if (nil == bgnsCallbackDelegate) {
        // unregister BGNS
        qeo_bgns_register(NULL,(uintptr_t)(__bridge void *)self);
        return;
    }

    NSArray *backgroundModes = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"UIBackgroundModes"];
    if ((nil == backgroundModes) || (NO == [backgroundModes containsObject:@"voip"])){
        // Missing Voip in App main plist file
        qeo_log_e("*** WARNING: VoIP feature not present as Background mode, check your App plist file");
    }
    
    // Initialize qeo-c-core Background Notification Service callbacks
    qeo_bgns_listener_t bgns_callbacks;
    bgns_callbacks.on_wakeup = on_qeo_bgns_on_wakeup;
    bgns_callbacks.on_connect= on_qeo_bgns_on_connect;

    // Register callbacks
    qeo_bgns_register(&bgns_callbacks, (uintptr_t)(__bridge void *)self);
}

- (BOOL)createVoipStream:(int)fileDescriptor
{
    // Check if file descripter is already present
    NSValue *streamWrapper = [socketMap objectForKey:[NSNumber numberWithInt:fileDescriptor]];
    if (nil != streamWrapper) {
        qeo_log_d("File descriptor %d already present",fileDescriptor);
        return YES;
    }

    // Create a stream
    // Note: It is sufficient to create only a read stream, the VoIP properties
    //       are only applied to a read stream and never to a write stream.
    //       Therefore we limit ourselves here to read streams. The actual data
    //       that passes through the socket is handled completely in DDS.
    //       Because of that we do not need to add the read stream to the runloop
    //       of the App.
    CFReadStreamRef stream;
    CFStreamCreatePairWithSocket(kCFAllocatorDefault, fileDescriptor, &stream, NULL);

    // Set VoIP properties on the stream
    if (!stream ||
        CFReadStreamSetProperty(stream,
                                kCFStreamNetworkServiceType,
                                kCFStreamNetworkServiceTypeVoIP) != TRUE ||
        // Auto-close my native socket
        CFReadStreamSetProperty(stream,
                                kCFStreamPropertyShouldCloseNativeSocket,
                                kCFBooleanTrue) != TRUE ||
        // open stream
        CFReadStreamOpen(stream) != TRUE)
    {
        NSLog(@"Failed to configure TCP transport for VoIP usage. Background mode will not be supported.");

        CFRelease(stream);
        stream = NULL;
        return NO;
    }

    // Store stream and associated fileDescriptor in dictionary
    streamWrapper = [NSValue valueWithPointer:stream];
    [socketMap setObject:streamWrapper forKey:[NSNumber numberWithInt:fileDescriptor]];
    NSLog(@"Created and stored a new TCP VoIP stream for file descriptor: %d",fileDescriptor);
    return YES;
}

- (BOOL)destroyVoipStream:(int)fileDescriptor
{
    qeo_log_d("File descriptor: %d",fileDescriptor);

    // Lookup file descriptor in map
    NSValue *streamWrapper = [socketMap objectForKey:[NSNumber numberWithInt:fileDescriptor]];

    if (nil != streamWrapper) {
        CFReadStreamRef stream = (CFReadStreamRef)[streamWrapper pointerValue];

        // Close and release stream
        CFReadStreamClose(stream);
        CFRelease(stream);
        stream = NULL;

        // Remove it from dictionary
        [socketMap removeObjectForKey:[NSNumber numberWithInt:fileDescriptor]];

        qeo_log_d("TCP VoIP stream closed for file descriptor: %d, released and removed from storage",fileDescriptor);
    } else {
        qeo_log_d("File descriptor %d not found",fileDescriptor);
    }

    return YES;
}

-(void)registerBGNSEnabledEntity:(QEOEntity *)reader
{
    qeo_log_d("%s",__FUNCTION__);

    if ((nil != self.readerList) && (nil != reader)) {
        // Wrap weak entity
        QEOEntityWrapper *wrapper = [[QEOEntityWrapper alloc]initWithEntity:reader];

        if (nil != wrapper) {
            // Add new wrapped entity
            [self.readerList addObject:wrapper];
            qeo_log_d("Added entity");
        } else {
            qeo_log_e("Could not wrap entity, qeo type has invalid structure ??");
        }
    }
}

-(void)unregisterBGNSEnabledEntity:(QEOEntity *)reader
{
    qeo_log_d("%s",__FUNCTION__);

    if ((nil != self.readerList) && (nil != reader)) {
        for (NSUInteger currentIndex = 0;currentIndex < self.readerList.count;++currentIndex) {
            QEOEntityWrapper *wrapper = [self.readerList objectAtIndex:currentIndex];
            if (wrapper.entity == reader) {
                [self.readerList removeObjectAtIndex:currentIndex];
                qeo_log_d("Removed entity");
                break;
            }
        }
    }
}

@end
