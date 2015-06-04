#import "QEOEventWriter.h"
#import "qeo/log.h"
#import "qeocore/api.h"
#import "QEOFactory.h"
#import "QEOEntity.h"


@interface QEOEventWriter()
{
    
}
@end

@implementation QEOEventWriter
{
    qeocore_writer_t *eventWriter;
}
/*#######################################################################
 # C - IMPLEMENTATION                                                   #
 ########################################################################*/


/*#######################################################################
 # OBJECTIVE-C IMPLEMENTATION                                           #
 ########################################################################*/
- (instancetype)init
{
    // Don't allow the default constructor
    return nil;
}



- (instancetype)initWithType:(Class)qeoType
                     factory:(QEOFactory *)factory
              entityDelegate:(id <QEOEntityDelegate>)entityDelegate
                       error:(NSError **)error
{
    self = [super initWithType:qeoType
                       factory:factory
                entityDelegate:entityDelegate
                         error:error];
    if (self) {
        const DDS_TypeSupport_meta *tsm = (const DDS_TypeSupport_meta *)[qeoType getMetaType];
        qeo_retcode_t ret;
        
        const qeocore_writer_listener_t w_listener = {
            .on_policy_update = writer_policy_update_callback,
            .userdata = (uintptr_t)self
        };
        
        eventWriter = qeocore_writer_open(factory.factory,
                                          self.internalQeoType,
                                          tsm->name,
                                          QEOCORE_EFLAG_EVENT_DATA | QEOCORE_EFLAG_ENABLE,
                                          &w_listener, 
                                          &ret);
        qeocore_type_free(self.internalQeoType);

        if (ret != QEO_OK){
            if (error) {
                NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Could not make eventwriter"};
                *error = [[NSError alloc]initWithDomain:@"org.qeo" code:ret userInfo:userInfo];
            }
            qeo_log_e("Could not make eventwriter (%d)", ret);
            
            return nil;
        }
    }
    
    return self;
}

- (BOOL)write:(QEOType *)object
    withError:(NSError **)error
{
    BOOL retval = NO;
    
    qeo_retcode_t ret = QEO_OK;
    qeocore_data_t *sample = qeocore_writer_data_new(eventWriter);
    NSString *errorInfo = @"OK";
    do {
        
        if (sample == NULL) {
            errorInfo = @"Could not create new sample";
            ret = QEO_EFAIL;
            break;
        }
        
        if (![object isKindOfClass:super.qeoType]) {
            errorInfo = [NSString stringWithFormat:@"Object was not of type %@", [super.qeoType description]];
            ret = QEO_EINVAL;
            break;
        }
        
        if ([object marshallToData:sample withTypeInfo:super.typeInfo] == NO) {
            errorInfo = @"Could not marshal sample";
            ret = QEO_EFAIL;
            break;
        }
        
        ret = qeocore_writer_write(eventWriter, sample);
        if (ret != QEO_OK) {
            errorInfo = @"Could not write sample";
            break;
        } 
        
        retval = YES;
    } while (0);
    
    if (retval == NO) {
        qeo_log_e("Error: %s", [errorInfo UTF8String]);
    }
    
    if (error) {
        NSDictionary *userInfo = @{NSLocalizedDescriptionKey: errorInfo};
        *error = [[NSError alloc]initWithDomain:@"org.qeo" code:ret userInfo:userInfo];
    }

    
    qeocore_data_free(sample);
    
    return retval;
}

- (BOOL)updatePolicyWithError:(NSError **)error
{
    qeo_retcode_t ret = qeocore_writer_policy_update(eventWriter);
    
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
    qeocore_writer_close(eventWriter);
}

@end
