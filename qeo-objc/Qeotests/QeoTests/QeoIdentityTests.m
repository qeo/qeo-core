#import <XCTest/XCTest.h>
#import <Qeo/Qeo.h>
#import "QeoCredentialsHandler.h"

@interface QeoIdentityTests : XCTestCase

@end

@implementation QeoIdentityTests

+ (void)setUp
{
    [QeoCredentialsHandler loadQeoCredentials];
}

+ (void)tearDown
{
    [QeoCredentialsHandler removeQeoCredentials];
}

- (void)setUp
{
    [super setUp];
    // Put setup code here; it will be run once, before the first test case.
}

- (void)tearDown
{
    // Put teardown code here; it will be run once, after the last test case.
    [super tearDown];
}

- (void)testIdentity
{
    // Default identity testen
    QEOIdentity *identity = [[QEOIdentity alloc]init];
    XCTAssertNotNil(identity, @"Identity creation succeed fail from application side");
    XCTAssertNil(identity.realmId, @"Default identity should return all nil values");
    XCTAssertNil(identity.deviceId, @"Default identity should return all nil values");
    XCTAssertNil(identity.userId, @"Default identity should return all nil values");
    XCTAssertNil(identity.url, @"Default identity should return all nil values");
    
    NSArray *qeoIdentities = [QEOIdentity retrieveQeoIdentities];
    XCTAssertNil(qeoIdentities, @"For now this test will only return nil, since it is not yet supported");
    XCTAssertTrue([qeoIdentities count] == 0, @"The array is nil or empty at the moment");
    
    for (NSObject *object in qeoIdentities) {
        if ([object isKindOfClass:[QEOIdentity class]] == NO) {
            XCTAssert(NO, @"Objects in qeoIdentities should be of QEOIdentity class");
        }
        XCTAssertNotNil(identity.realmId, @"Realm should not be nil");
        XCTAssertNotNil(identity.userId, @"UserId should not be nil");
        XCTAssertNotNil(identity.deviceId, @"Device should not be nil");
        XCTAssertNotNil(identity.url, @"URL should not be nil");
        XCTAssertNotEqual([identity.url absoluteString], @"", @"URL should not be empty");
    }
    identity = nil;
}

@end
