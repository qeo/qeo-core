#import "platform_api.h"
#import "QEOPlatform.h"
#import <UIKit/UIKit.h>
#import "qeo/log.h"
#import "qeo/factory.h"
#import "QEOFactoryContext.h"
#include <qeo/platform.h>
#include <qeo/platform_security.h>
#include <dds/dds_aux.h>

#ifdef DEBUG
#import <ifaddrs.h>
#import <arpa/inet.h>
#endif

#pragma mark - Extension

@interface QEOPlatform ()

-(instancetype)initPlatform;
-(void)resetPlatform;
-(void)destroyPlatform;
-(char*)getPath;
#ifdef DEBUG
-(NSString *)getCurrentActiveIPAddress;
#endif

@end

#pragma mark - qeo-c-utils callbacks

// Multi-Ream: this should be done by the QEOFactoryContext object in the future
static qeo_util_retcode_t on_registration_params_needed(uintptr_t app_context,
                                                        qeo_platform_security_context_t sec_context){
    
    qeo_log_d("on_registration_params_needed CALLED !!!");
    
    QEOPlatform *platformInstance = [QEOPlatform sharedInstance];
    
    if (nil == platformInstance.factoryContext) {
        // Stop
        return QEO_UTIL_EFAIL;
    } else {
        return [platformInstance.factoryContext registrationParametersNeeded:sec_context];
    }
    
    return QEO_UTIL_OK;
}

// Multi-Ream:this should be done by the QEOFactoryContext object in the future
static void on_security_status_update_cb(uintptr_t app_context,
                                         qeo_platform_security_context_t sec_context,
                                         qeo_platform_security_state state,
                                         qeo_platform_security_state_reason reason){
    
    QEOPlatform *platformInstance = [QEOPlatform sharedInstance];
    
    if (nil == platformInstance.factoryContext) {
        return;
    } else {
        [platformInstance.factoryContext security_status_update:sec_context
                                                          state:state
                                                         reason:reason];
    }
}

static qeo_util_retcode_t on_platform_custom_certificate_validator_cb(qeo_der_certificate* certf, int size){
    
    qeo_log_d("iOS CERTIFICATE VALIDATION started");
    
    qeo_util_retcode_t returnValue = QEO_UTIL_EFAIL;
    NSMutableArray* certificateChain = [[NSMutableArray alloc]init];
    SecPolicyRef policyForSSLCertificateChains = NULL;
    SecTrustRef  trustManagementRef = NULL;
    
    do {
    
        // Step 1:
        // Convert array of raw "der"-certificate data into an array of iOS "SecCertificateRef" format
        for (int idx=0; idx<size; ++idx){
            NSData *rawDerCertificate = [NSData dataWithBytes:certf[idx].cert_data length:certf[idx].size];
            
            if(0 < [rawDerCertificate length]) {
                SecCertificateRef derCertificate = SecCertificateCreateWithData(NULL, (__bridge CFDataRef)rawDerCertificate);
                
                if(NULL != derCertificate) {
                    [certificateChain addObject:(__bridge id)derCertificate];
                } else {
                    qeo_log_e("CERTIFICATE Conversion to iOS format FAILED");
                    break;
                }
            } else {
                qeo_log_e("Provided der-CERTIFICATE EMPTY");
                break;
            }
        }
        
        // Step 2:
        // Create policy object for evaluating SSL certificate chains
        policyForSSLCertificateChains = SecPolicyCreateSSL(true, NULL);
        if (NULL == policyForSSLCertificateChains){
            qeo_log_e("SSL POLICY CREATION FAILED");
            break;
        }
        
        // Step 3:
        // Create a trust management object based on certificates and policies
        OSStatus result = SecTrustCreateWithCertificates((__bridge CFArrayRef)certificateChain,
                                                          policyForSSLCertificateChains,
                                                          &trustManagementRef);
        
        if (errSecSuccess != result || NULL == trustManagementRef) {
            qeo_log_e("iOS TRUST MANAGEMENT creation FAILED");
            break;
        }
        
        // Step 4:
        // Evaluate the certificate chain for the SSL policy
        SecTrustResultType resultType = 0;
        result = SecTrustEvaluate (trustManagementRef, &resultType);
        if (errSecSuccess != result || ((kSecTrustResultProceed != resultType) && (kSecTrustResultUnspecified != resultType))) {
            qeo_log_e("Certificate chian validation FAILED");
            break;
        }
    
        returnValue = QEO_UTIL_OK;
        
    } while (0);
    
    // Step 5
    // Cleanup of allocated resources
    if (NULL != trustManagementRef){
        CFRelease(trustManagementRef);
    }
    if (NULL != policyForSSLCertificateChains){
        CFRelease(policyForSSLCertificateChains);
    }
    for (int idx=0; idx<[certificateChain count]; ++idx){
        CFRelease((SecCertificateRef)certificateChain[idx]);
    }
    
    qeo_log_d("Result CERTIFICATE VALIDATION on iOS: %s",(QEO_UTIL_OK == returnValue)?[@"SUCCESS" UTF8String]:[@"FAILED" UTF8String]);
    return returnValue;
}

// Multi-Ream:this should be done by the QEOFactoryContext object in the future
static qeo_util_retcode_t on_remote_registration_confirmation_needed_cb(uintptr_t app_context,
                                                                        qeo_platform_security_context_t sec_context,
                                                                        const qeo_platform_security_remote_registration_credentials_t *rrcred) {
    
    QEOPlatform *platformInstance = [QEOPlatform sharedInstance];
    
    if (nil == platformInstance.factoryContext) {
        // Stop
        return QEO_UTIL_EFAIL;
    } else {
        return [platformInstance.factoryContext remoteRegistrationConfirmationNeeded:sec_context
                                                                           realmName:[NSString stringWithUTF8String:rrcred->realm_name]
                                                                                 url:[NSURL URLWithString:[NSString stringWithUTF8String:rrcred->url]]];
    }
    
    return QEO_UTIL_OK;
}

#pragma mark - QEOPlatform implementation

@implementation QEOPlatform
{
    char *_Path;
    qeo_platform_device_info *_DeviceInfo;
}

+(QEOPlatform *)sharedInstance
{
    qeo_log_d("%s",__FUNCTION__);
    
    static QEOPlatform *sharedInstance = nil;
    static dispatch_once_t onceToken = 0;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[super alloc] initPlatform];
    });
    
    if (nil != [sharedInstance getPath]) {
        qeo_log_d("Current Library Path: %s", [sharedInstance getPath]);
    } else {
        qeo_log_e("Current Library Path NULL !");
    }
    
    return sharedInstance;
}

-(char*) getPath {
    return _Path;
}

-(instancetype) initPlatform
{
    // Always print the Qeo version to console
    NSLog(@"Qeo version is %s",qeo_version_string());
    
    qeo_log_d("%s",__FUNCTION__);
    
    self = [super init];
    if (self) {

#ifdef DEBUG
        NSString* ipAddress = [self getCurrentActiveIPAddress];
        qeo_log_d("IP-Address: %s\r\n",[ipAddress UTF8String]);
#endif
        //=====================================================
        // Initialize qeo storage location

        NSDictionary *env = [[NSProcessInfo processInfo] environment];
        NSString *storageDir = [env valueForKey:@"QEO_STORAGE_DIR"];
        NSString *libraryPath;
        
        if (storageDir != nil && [storageDir length] != 0) {
            libraryPath = storageDir;
        }
        else {
            // Setup path to the ".qeo" directory in the App sandbox
            NSString *libraryDir = [NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, YES) lastObject];
            libraryPath = [libraryDir stringByAppendingPathComponent:@"/.qeo"];
        }
        
        // Check existance of ".qeo" directory
        NSFileManager* fileManager = [NSFileManager defaultManager];
        if (![fileManager fileExistsAtPath:libraryPath]) {
            
            qeo_log_e("Create .qeo direcotory");
            // Not existing: Create it !
            [fileManager createDirectoryAtPath:libraryPath withIntermediateDirectories:NO attributes:nil error:nil];
        }
        
        // Register the ".qeo" directory
        _Path = strdup([libraryPath UTF8String]);
        qeo_log_d("We use %s", _Path);
        qeo_platform_set_device_storage_path(_Path);
        
        //=====================================================
        // Initialize DDS logging
        DDS_parameter_set ("LOG_DIR", [NSTemporaryDirectory() UTF8String]);
        
        //=====================================================
        // Initialize Device information
        
        // Build device info
        _DeviceInfo = calloc(1, sizeof(qeo_platform_device_info));
        
        uuid_t uuid;
        [[UIDevice currentDevice].identifierForVendor getUUIDBytes:uuid];
        
        qeo_platform_device_id deviceId;
        memcpy(&deviceId.upperId, uuid, sizeof(deviceId.upperId));
        memcpy(&deviceId.lowerId, uuid + sizeof(deviceId.upperId), sizeof(deviceId.lowerId));
        _DeviceInfo->qeoDeviceId = deviceId;
        
        _DeviceInfo->manufacturer = strdup([[NSString stringWithFormat:@"Apple"] UTF8String]);
        _DeviceInfo->modelName = strdup([[UIDevice currentDevice].model UTF8String]);
        _DeviceInfo->productClass = NULL;
        _DeviceInfo->serialNumber = NULL;
        _DeviceInfo->hardwareVersion = strdup([[UIDevice currentDevice].model UTF8String]);
        NSString *softwareVersion = [NSString stringWithFormat:@"%@ %@",[UIDevice currentDevice].systemName,[UIDevice currentDevice].systemVersion];
        _DeviceInfo->softwareVersion = strdup([softwareVersion UTF8String]);
        _DeviceInfo->productClass = strdup([[UIDevice currentDevice].model UTF8String]);
        _DeviceInfo->userFriendlyName = strdup([[UIDevice currentDevice].name UTF8String]);
        _DeviceInfo->configURL = strdup([[NSString stringWithFormat:@""] UTF8String]);
        
        // Register the device info
        qeo_platform_set_device_info(_DeviceInfo);
        
        //=====================================================
        // Initialize qeo-c-util callbacks
        
        if (QEO_UTIL_OK != qeo_platform_set_custom_certificate_validator(on_platform_custom_certificate_validator_cb)){
            qeo_log_e("Could not set certificate validator in the platform layer");
            return nil;
        }
        
        // Multi-Realm: this should be done by the QEOFactoryContext object in the future
        qeo_platform_callbacks_t platform_callbacks;
        platform_callbacks.on_reg_params_needed = on_registration_params_needed;
        platform_callbacks.on_sec_update = on_security_status_update_cb;
        platform_callbacks.on_rr_confirmation_needed = on_remote_registration_confirmation_needed_cb;
        
        /* Setup platform security */
        if (QEO_UTIL_OK != qeo_platform_init((uintptr_t)(__bridge void *)self, &platform_callbacks)){
            qeo_log_e("Could not init platform layer");
            return nil;
        }
    }
    return self;
}

-(void)dealloc
{
    // Remove native callbacks
    qeo_platform_callbacks_t platform_callbacks = {};
    qeo_platform_init((uintptr_t)(__bridge void *)self, &platform_callbacks);
    
    // Remove device info
    qeo_platform_set_device_info(NULL);
    
    // Remove path
    qeo_platform_set_device_storage_path(NULL);

    free(_Path);
    free((void*)_DeviceInfo->manufacturer);
    free((void*)_DeviceInfo->modelName);
    free((void*)_DeviceInfo->hardwareVersion);
    free((void*)_DeviceInfo->softwareVersion);
    free((void*)_DeviceInfo->productClass);
    free((void*)_DeviceInfo->userFriendlyName);
    free((void*)_DeviceInfo->configURL);
    free((void*)_DeviceInfo);
}

-(void) resetPlatform {
    
    qeo_log_d("%s",__FUNCTION__);
    
    // Cleanup any reference in the native side
    [self destroyPlatform];
    
    [QEOPlatform clearQeoIdentities];
}

-(void) destroyPlatform {
    
    qeo_log_d("%s",__FUNCTION__);
    
    //=====================================================
    // Remove callbacks
    // Multi-Realm: this should be done by the QEOFactoryContext object in the future
    qeo_platform_callbacks_t platform_callbacks = {};
    qeo_platform_init((uintptr_t)(__bridge void *)self, &platform_callbacks);
    //=====================================================
    
    
    // Remove device info
    qeo_platform_set_device_info(NULL);
    
    // Remove path
    qeo_platform_set_device_storage_path(NULL);
}

#ifdef DEBUG
- (NSString *)getCurrentActiveIPAddress
{
    NSString *wifiAddress = nil;
    NSString *cellAddress = nil;
    struct ifaddrs *interfaces = NULL; /* root of linked list */
    
    // Get All available interfaces
    if (!getifaddrs(&interfaces)) {
        
        // Linked list walking pointer
        // initialze it with the root address
        struct ifaddrs *tmpInf = interfaces;
        
        while (NULL != tmpInf) {
            // Get the interface type
            sa_family_t sa_type = tmpInf->ifa_addr->sa_family;
            if(sa_type == AF_INET || sa_type == AF_INET6) {
                // Extract interface name and IP-address
                NSString *name = [NSString stringWithUTF8String:tmpInf->ifa_name];
                NSString *ipAddress = [NSString stringWithUTF8String:inet_ntoa(((struct sockaddr_in *)tmpInf->ifa_addr)->sin_addr)];
                //NSLog(@"name: %@, addr: %@",name,ipAddress);
                
                if (!([ipAddress isEqualToString:@"0.0.0.0"])) {
                    // Check precense of a Wifi Address
                    // Note: on iPhone we get 'en0'
                    //       on simulator we get 'en1'
                    if([name hasPrefix:@"en"]) {
                        wifiAddress = ipAddress;
                        NSLog(@"WiFi IP-address: %@, name: %@", ipAddress, name);
                    }
                    
                    // Check for Cellular Address
                    // Note: This address is always present if cellular network is present
                    //       Address precense is independant of having a valid Wifi connection or not
                    if([name hasPrefix:@"pdp_ip"]) {
                        cellAddress = ipAddress;
                        NSLog(@"Cellular IP-address: %@, name: %@", ipAddress, name);
                    }
                }
            }
            tmpInf = tmpInf->ifa_next;
        }
        // Free up interface resources
        freeifaddrs(interfaces);
    }
    // Prefer Wifi above cellular
    NSString *activeIPAddress = (nil != wifiAddress)? wifiAddress : cellAddress;
    return (nil != activeIPAddress)? activeIPAddress : @"0.0.0.0";
}
#endif

+(void)clearQeoIdentities {
    
    // reset registration responsiblities.
    [QEOPlatform sharedInstance].factoryContext = nil;
    
    // Check existance of ".qeo" directory
    NSFileManager* fileManager = [NSFileManager defaultManager];
    NSString *libraryDir = [NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, YES) lastObject];
    NSString *libraryPath = [libraryDir stringByAppendingPathComponent:@"/.qeo"];
    if (YES == [fileManager fileExistsAtPath:libraryPath]) {
        
        qeo_log_d("Delete .qeo direcotory");
        // Existing: Delete it !
        [fileManager removeItemAtPath:libraryPath error:nil];
        
        // Re-create it
        [fileManager createDirectoryAtPath:libraryPath withIntermediateDirectories:NO attributes:nil error:nil];
    }
}

@end
