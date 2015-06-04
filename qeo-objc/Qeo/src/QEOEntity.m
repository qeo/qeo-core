#import "QEOEntity.h"
#import "QEOType.h"
#import "qeo-tsm-to-dynamic/tsm-to-dynamic.h"
#import "qeo/log.h"
#import "QEOPolicyIdentity.h"

@interface QEOEntity()
-(qeo_policy_perm_t)onPolicyUpdate:(const qeo_policy_identity_t *)identity;
@end

@implementation QEOEntity
{
    Class _qeoTypeIvar;
}
/*#######################################################################
 # C - IMPLEMENTATION                                                   #
 ########################################################################*/

qeo_policy_perm_t writer_policy_update_callback(const qeocore_writer_t *writer,
                                                const qeo_policy_identity_t *identity,
                                                uintptr_t userdata)
{
    QEOEntity *entity = (__bridge QEOEntity*)(void *)userdata;
    return [entity onPolicyUpdate:identity];
    
}


qeo_policy_perm_t reader_policy_update_callback(const qeocore_reader_t *reader,
                                                const qeo_policy_identity_t *identity,
                                                uintptr_t userdata)
{
    QEOEntity *entity = (__bridge QEOEntity*)(void *)userdata;
    return [entity onPolicyUpdate:identity];
}


/*#######################################################################
 # OBJECTIVE-C IMPLEMENTATION                                           #
 ########################################################################*/

- (instancetype)init
{
    // Do not allow this initialiser to ever succeed! factories and qeotypes are mandatory!
    return nil;
}

- (instancetype)initWithType:(Class)qeoType
                     factory:(QEOFactory *) factory
              entityDelegate:(id <QEOEntityDelegate>)entityDelegate
                       error:(NSError **)error;
{
    // Do something more generic with error handling
    if (error) {
        NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"OK"};
        *error = [[NSError alloc]initWithDomain:@"org.qeo" code:QEO_OK userInfo:userInfo];
    }
    
    if (qeoType == nil || factory == nil) {
        if (error) {
             NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Please provide the right arguments"};
             *error = [[NSError alloc]initWithDomain:@"org.qeo" code:QEO_EINVAL userInfo:userInfo];
        }
        return nil;
    }
    
    /* have a look at objc runtime if this does not work... */
    if (![qeoType respondsToSelector: @selector(getMetaType)]) {
        if (error) {
            NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Please supply the correct type"};
            *error = [[NSError alloc]initWithDomain:@"org.qeo" code:QEO_EINVAL userInfo:userInfo];
        }
        
        qeo_log_e("Not the right type");
        return nil;
    }
    
    
    self = [super init];
    if (self) {
        _qeoTypeIvar = qeoType;
        self.factory = factory;
        self.entityDelegate = entityDelegate;
        _tsm = [qeoType getMetaType];
        qeo_retcode_t ret;
        
        if ((ret = qeo_register_tsm(factory.factory, self.tsm , &_internalQeoType, &_typeInfo)) != QEO_OK) {
            if (error) {
                NSDictionary *userInfo = @{NSLocalizedDescriptionKey: @"Could not register type"};
                *error = [[NSError alloc]initWithDomain:@"org.qeo" code:ret userInfo:userInfo];
            }

            qeo_log_e("Could not register tsm (ret = %d)", ret);
            return nil;
        }
        
    }
    return self;
}


- (qeo_policy_perm_t)onPolicyUpdate:(const qeo_policy_identity_t *)identity
{
    if (self.entityDelegate == nil || ![self.entityDelegate respondsToSelector: @selector(allowAccessForEntity:identity:)]) {
        qeo_log_d("Ignoring because no suitable delegate available");
        return QEO_POLICY_ALLOW;
    }
    
    QEOPolicyIdentity *pi = [[QEOPolicyIdentity alloc] initWithCPolicyIdentity:identity];
    
    if ([_entityDelegate allowAccessForEntity:self identity:pi] == YES) {
        return QEO_POLICY_ALLOW;
    } else {
        return QEO_POLICY_DENY;
    }
    
}

- (BOOL)updatePolicyWithError:(NSError **)error
{
    /* Not used --> overriden in derived classes */
    return NO;
}

- (void)dealloc
{
    qeo_unregister_tsm(&_typeInfo);
}

-(Class) qeoType {
    return _qeoTypeIvar;
}
@end
