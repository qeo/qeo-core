#import "QEOStateReader.h"
#import "qeo/log.h"
#import "qeocore/api.h"
#import "qeocore/dyntype.h"
#import "QEOFactory.h"
#import "QEOEntity.h"
#import "QEOType.h"

@interface QEOStateReader()
-(void)onUpdate;
@end

@implementation QEOStateReader
{
    qeocore_reader_t *stateReader;
}
/*#######################################################################
  # C - IMPLEMENTATION                                                  #
  ########################################################################*/


static void on_data_available(const qeocore_reader_t *reader,
                              const qeocore_data_t *data,
                              uintptr_t userdata)
{
    QEOStateReader *objcreader = (__bridge QEOStateReader*)(void *)userdata;
    
    switch (qeocore_data_get_status(data)) {
        case QEOCORE_NOTIFY:
            qeo_log_d("Notify received");
            [objcreader onUpdate];
            break;
        case QEOCORE_DATA:
             qeo_log_d("Data received");
             break;
        case QEOCORE_NO_MORE_DATA:
             qeo_log_d("No more data received");
             break;
        case QEOCORE_REMOVE:
            qeo_log_d("remove received");
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
                    delegate:(id <QEOStateReaderDelegate>)delegate
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
        qeo_retcode_t ret = QEO_OK;
        
        const qeocore_reader_listener_t rlistener = {
            .on_data = on_data_available,
            .on_policy_update = reader_policy_update_callback,
            .userdata = (uintptr_t)self
        };
        
        stateReader = qeocore_reader_open(factory.factory,
                                          self.internalQeoType,
                                          tsm->name,
                                          QEOCORE_EFLAG_STATE_UPDATE | QEOCORE_EFLAG_ENABLE,
                                          &rlistener,
                                          &ret);
        qeocore_type_free(self.internalQeoType); 

        if (ret != QEO_OK){
            if (error) {
                NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Could not make statereader"};
                *error = [[NSError alloc]initWithDomain:@"org.qeo" code:ret userInfo:userInfo];
            }
            qeo_log_e("Could not make statereader (%d)", ret);
            return nil;
        }
    }
    
    return self;
}

- (void)setBackgroundServiceNotification:(BOOL)backgroundServiceNotification
{
    qeo_log_d("%s",__FUNCTION__);
    
    qeo_retcode_t result = qeocore_reader_bgns_notify(stateReader, (bool)backgroundServiceNotification);
    if (QEO_OK != result) {
        qeo_log_e("Could not alter the StateReader for background service notifications");
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

- (void)dealloc
{
    [self.factory unregisterBGNSEnabledEntity:self];
    qeocore_reader_close(stateReader);
}

- (void)enumerateInstancesUsingBlock:(void (^)(const QEOType *, BOOL *cont))iterationBlock
{
    qeocore_filter_t filter = {};
    qeocore_data_t *data = qeocore_reader_data_new(stateReader);
    BOOL cont = YES;
    
    while (cont == YES && qeocore_reader_read(stateReader, &filter, data) == QEO_OK) {
        QEOType *qeoType = [[self.qeoType alloc] initFromData:data withTypeInfo:super.typeInfo];
        if (qeoType != nil){
            iterationBlock(qeoType, &cont);
        }
        if (cont == YES) {
            filter.instance_handle = qeocore_data_get_instance_handle(data);
            qeocore_data_free(data);
            data = qeocore_reader_data_new(stateReader);
        }
    }
    
    qeocore_data_free(data);
}


- (NSUInteger)countByEnumeratingWithState:(NSFastEnumerationState *)state
                                  objects:(id __unsafe_unretained [])buffer
                                    count:(NSUInteger)len
{
    qeocore_filter_t filter = {};
    NSUInteger count = 0;
    
    if(state->state == 0) {
        state->mutationsPtr = &state->extra[0]; //unused
        state->extra[1] = 0;
    } else {
        filter.instance_handle = (unsigned int)state->extra[1];
    }
    
    state->itemsPtr = buffer;
    qeocore_data_t *data = qeocore_reader_data_new(stateReader);
    
    while (count < len && qeocore_reader_read(stateReader, &filter, data) == QEO_OK) {
        QEOType __autoreleasing *qeoType = [[self.qeoType alloc] initFromData:data withTypeInfo:super.typeInfo];
        if (qeoType != nil) {
            buffer[count] = qeoType;
            ++count;
            ++state->state;
        } else {
            qeo_log_e("Could not unmarshal");
        }
        /* Not happy to cast a pointer to an unsigned long...
         Pity they don't use uintptr_t..
         It will work in practice though */
        state->extra[1] = filter.instance_handle = qeocore_data_get_instance_handle(data);
        qeocore_data_free(data);
        data = qeocore_reader_data_new(stateReader);
    }

    qeocore_data_free(data);
    return count;
}

-(void)onUpdate
{
    if (_delegate == nil || ![_delegate respondsToSelector: @selector(didReceiveUpdateForStateReader:)]) {
        qeo_log_d("Ignoring because no suitable delegate available");
        return;
    }
    
    [_delegate didReceiveUpdateForStateReader:self];
}

- (BOOL)updatePolicyWithError:(NSError **)error
{
    qeo_retcode_t ret = qeocore_reader_policy_update(stateReader);
    
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


@end
