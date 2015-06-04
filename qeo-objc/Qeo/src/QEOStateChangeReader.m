#import "QEOStateChangeReader.h"
#import "qeo/log.h"
#import "qeocore/api.h"
#import "QEOType.h"

@interface QEOStateChangeReader()
-(void)onData:(const qeocore_data_t *)data;
-(void)onRemove:(const qeocore_data_t *)data;
-(void)onNoMoreData;
@end

@implementation QEOStateChangeReader
{
    qeocore_reader_t *stateChangeReader;
}
/*#######################################################################
 # C - IMPLEMENTATION                                                   #
 ########################################################################*/

static void on_data_available(const qeocore_reader_t *reader,
                              const qeocore_data_t *data,
                              uintptr_t userdata)
{
    QEOStateChangeReader *objcreader = (__bridge QEOStateChangeReader*)(void *)userdata;
    
    switch (qeocore_data_get_status(data)) {
        case QEOCORE_NOTIFY:
            qeo_log_d("Notify received");
            break;
        case QEOCORE_DATA:
        {
            qeo_log_d("Data received");
            [objcreader onData:data];
        }
            break;
        case QEOCORE_NO_MORE_DATA:
        {
            qeo_log_d("No more data received");
            [objcreader onNoMoreData];
        }
            break;
        case QEOCORE_REMOVE:
            qeo_log_d("remove received");
            [objcreader onRemove:data];
            break;
        case QEOCORE_ERROR:
            qeo_log_e("no callback called due to prior error");
            break;
    }
}



/*#######################################################################
 # OBJECTIVE-C IMPLEMENTATION                                        #
 ########################################################################*/
- (instancetype)init
{
    // Don't allow the default constructor
    return nil;
}

- (instancetype)initWithType:(Class)qeoType
                     factory:(QEOFactory *)factory
                    delegate:(id <QEOStateChangeReaderDelegate>) delegate
              entityDelegate:(id <QEOEntityDelegate>)entityDelegate
                       error:(NSError **)error
{
    self = [super initWithType:qeoType
                       factory:factory
                entityDelegate:entityDelegate
                         error:error];
    
    if (self) {
        self.delegate = delegate;
        const DDS_TypeSupport_meta *tsm = (const DDS_TypeSupport_meta *)[qeoType getMetaType];
        qeo_retcode_t ret;
        
        const qeocore_reader_listener_t _rlistener = {
            .on_data = on_data_available,
            .on_policy_update = reader_policy_update_callback,
            .userdata = (uintptr_t)self
        };
        
        stateChangeReader = qeocore_reader_open(factory.factory,
                                          self.internalQeoType,
                                          tsm->name,
                                          QEOCORE_EFLAG_STATE_DATA | QEOCORE_EFLAG_ENABLE,
                                          &_rlistener,
                                          &ret);
        if (ret != QEO_OK) {
            if (error) {
                NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Could not make statechangereader"};
                *error = [[NSError alloc]initWithDomain:@"org.qeo" code:ret userInfo:userInfo];
            }
            qeo_log_e("Could not make statechangereader (%d)", ret);
            return nil;
        }
    }
    
    return self;
}

- (void)setBackgroundServiceNotification:(BOOL)backgroundServiceNotification
{
    qeo_log_d("%s",__FUNCTION__);

    qeo_retcode_t result = qeocore_reader_bgns_notify(stateChangeReader, (bool)backgroundServiceNotification);
    if (QEO_OK != result) {
        qeo_log_e("Could not alter the StateChangeReader for background service notifications");
        return;
    }
    _backgroundServiceNotification = backgroundServiceNotification;

    if (YES == backgroundServiceNotification) {
        [self.factory registerBGNSEnabledEntity:self];

        // Verify Voip setting
        NSArray *backgroundModes = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"UIBackgroundModes"];
        if ((nil == backgroundModes) || (NO == [backgroundModes containsObject:@"voip"])){
            // Missing Voip in App main plist file
            qeo_log_e("*** WARNING: VoIP feature not present as Background mode, check your App plist file");
        }
    } else {
        [self.factory unregisterBGNSEnabledEntity:self];
    }
}

- (void)onData:(const qeocore_data_t *)data
{
    if (_delegate == nil || ![_delegate respondsToSelector: @selector(didReceiveStateChange:forReader:)]){
        qeo_log_d("Ignoring because no suitable delegate available");
        return;
    }
    
    QEOType *obj = [[self.qeoType alloc] initFromData:data withTypeInfo:super.typeInfo];
    if (obj == nil){
        qeo_log_e("Could not unmarshal");
        return;
    }
    
    [_delegate didReceiveStateChange:obj forReader:self];
}

- (void)onNoMoreData
{
    if (_delegate == nil || ![_delegate respondsToSelector: @selector(didFinishBurstForStateChangeReader:)]) {
        qeo_log_d("Ignoring because no suitable delegate available");
        return;
    }
    
    [_delegate didFinishBurstForStateChangeReader:self];
}

- (void)onRemove:(const qeocore_data_t *)data
{
    if (_delegate == nil || ![_delegate respondsToSelector: @selector(didReceiveStateRemoval:forReader:)]){
        qeo_log_d("Ignoring because no delegate available");
        return;
    }
    
    QEOType *obj = [[self.qeoType alloc] initOnlyKeyFromData:data withTypeInfo:super.typeInfo];
    if (obj == nil) {
        qeo_log_e("Could not unmarshal");
        return;
    }
    
    [_delegate didReceiveStateRemoval:obj forReader:self];
}


- (BOOL)updatePolicyWithError:(NSError **)error
{
    qeo_retcode_t ret = qeocore_reader_policy_update(stateChangeReader);
    
    if (ret == QEO_OK) {
        if (error) {
            NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"OK"};
            *error = [[NSError alloc]initWithDomain:@"org.qeo" code:ret userInfo:userInfo];
        }
        
        return YES;
    } else {
        if (error) {
            NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Could not trigger policy update"};
            *error = [[NSError alloc]initWithDomain:@"org.qeo" code:ret userInfo:userInfo];
        }
        
        return NO;
    }
}


- (void)dealloc
{
    [self.factory unregisterBGNSEnabledEntity:self];
    qeocore_reader_close(stateChangeReader);
}

@end
