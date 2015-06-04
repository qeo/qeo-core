#import "QEOIdentity.h"
#import "qeocore/identity.h"
#import "qeo/log.h"
#import "qeo/types.h"
#import "QEOPlatform.h"

@interface QEOIdentity()

@end

@implementation QEOIdentity

- (instancetype) init;
{
    return [super init];
}

+ (NSArray *) retrieveQeoIdentities
{
    qeo_identity_t *identities = NULL;
    unsigned int length = 0;
    NSMutableArray *identities_array = nil;

    do {
        qeo_retcode_t retcode = qeocore_get_identities(&identities, &length);
        if (retcode != QEO_OK) {
            qeo_log_e("Could not retrieve identities: %d" ,retcode);
            return nil;
        }
        
        if (length == 0) {
            qeo_log_d("No credentials present..");
            break;
        }
        
        identities_array = [[NSMutableArray alloc] initWithCapacity:[@(length) integerValue]];
        for (int i = 0; i < length; ++i) {
            QEOIdentity *identity = [[QEOIdentity alloc] init];
            identity.realmId = @(identities[i].realm_id);
            identity.deviceId = @(identities[i].device_id);
            identity.userId = @(identities[i].user_id);
            identity.url = [[NSURL alloc]initWithString:@(identities[i].url)];
        
            if ([identity isValid] == NO) {
                NSLog(@"Identity not valid..? (%@ %@ %@ %@)", identity.realmId, identity.deviceId, identity.userId, identity.url);
                continue;
            }
            
            [identities_array addObject:identity];
        }
        
        qeo_log_d("Number of identities: %d", [identities_array count]);
    } while (0);
    
    qeocore_free_identities(&identities, length);
    
    if ([identities_array count] == 0){
        return nil;
    }
    
    return identities_array;
}

+ (void)clearQeoIdentities
{
    [QEOPlatform clearQeoIdentities];
}

- (BOOL)isOpen
{
    if (_realmId == nil && _deviceId == nil && _userId == nil && _url == nil) {
        return YES;
    }
    
    return NO;
}

-(BOOL) isValid
{
    /* New registration process */
    if ([self isOpen] == YES){
        return YES;
    }
    
    if (_realmId != nil && _userId != nil && _deviceId != nil && _url != nil){
        return YES;
    }
    
    return NO;
}

@end
