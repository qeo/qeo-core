#import "QEOPolicyIdentity.h"

@interface QEOPolicyIdentity()

@end

@implementation QEOPolicyIdentity

-(instancetype)initWithCPolicyIdentity:(const qeo_policy_identity_t *)policy_identity
{
    if (policy_identity == NULL) {
        return nil;
    }
    
    self = [super init];
    if (self){
        _userId = [NSNumber numberWithLongLong:qeo_policy_identity_get_uid(policy_identity)];
    }
    
    return self;
}
@end
