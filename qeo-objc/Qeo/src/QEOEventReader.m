#import "QEOEventReader.h"
#import "qeo/log.h"
#import "qeocore/api.h"
#import "QEOType.h"


@interface QEOEventReader()
-(void)onData:(const qeocore_data_t *)data;
-(void)onNoMoreData;

@end

@implementation QEOEventReader
{
    qeocore_reader_t *eventReader;
}

/*#######################################################################
 # C - IMPLEMENTATION                                        #
 ########################################################################*/


static void on_data_available(const qeocore_reader_t *reader,
                              const qeocore_data_t *data,
                              uintptr_t userdata)
{
    QEOEventReader *objcreader = (__bridge QEOEventReader*)(void *)userdata;
    
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
                    delegate:(id <QEOEventReaderDelegate>)delegate
              entityDelegate:(id <QEOEntityDelegate>)entityDelegate
                       error:(NSError **)error;
{
  
    self = [super initWithType:qeoType
                     factory:factory
              entityDelegate:entityDelegate
                       error:error];
    
    if (self) {
        self.delegate = delegate;
        const DDS_TypeSupport_meta *tsm = (const DDS_TypeSupport_meta *)[qeoType getMetaType];
        qeo_retcode_t ret;
        
        const qeocore_reader_listener_t rlistener = {
            .on_data = on_data_available,
            .on_policy_update = reader_policy_update_callback,
            .userdata = (uintptr_t)self
        };
        
        eventReader = qeocore_reader_open(factory.factory,
                                          self.internalQeoType,
                                          tsm->name,
                                          QEOCORE_EFLAG_EVENT_DATA | QEOCORE_EFLAG_ENABLE,
                                          &rlistener,
                                          &ret);
        qeocore_type_free(self.internalQeoType);
        if (ret != QEO_OK){
            if (error) {
                NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Could not make eventreader"};
                *error = [[NSError alloc]initWithDomain:@"org.qeo" code:ret userInfo:userInfo];
            }
            qeo_log_e("Could not make eventreader (%d)", ret);
            
            return nil;
        }
    }
    
    return self;
}

- (void)onData:(const qeocore_data_t *)data
{
    if (self.delegate == nil || ![self.delegate respondsToSelector: @selector(didReceiveEvent:forReader:)]) {
        qeo_log_d("Ignoring because no suitable delegate available");
        return;
    }
    
    QEOType *obj = [[self.qeoType alloc] initFromData:data withTypeInfo:super.typeInfo];
    if (obj == nil){
        qeo_log_e("Could not unmarshal");
        return;
    }
    
    [_delegate didReceiveEvent:obj forReader:self];
}

- (void)onNoMoreData
{
    if (_delegate == nil || ![_delegate respondsToSelector: @selector(didFinishBurstForEventReader:)]) {
        qeo_log_d("Ignoring because no suitable delegate available");
        return;
    }
    
    [_delegate didFinishBurstForEventReader:self];
}


- (BOOL)updatePolicyWithError:(NSError **)error
{
    qeo_retcode_t ret = qeocore_reader_policy_update(eventReader);
   
    if (ret == QEO_OK){
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
    qeocore_reader_close(eventReader);
}


@end
