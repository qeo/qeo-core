#import <XCTest/XCTest.h>
#import <Qeo/Qeo.h>
#import "QDMCombination.h"
#import "QeoCredentialsHandler.h"

@interface QeoFactoryTests : XCTestCase <QEOEntityDelegate, QEOEventReaderDelegate, QEOStateReaderDelegate, QEOStateChangeReaderDelegate>

@end

@implementation QeoFactoryTests

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

- (void)testFactoryCreation
{
    NSError *error;
    QEOFactory *factory;
    QEOFactory *factoryBis;
    NSArray *identityList;
    
    factory = [[QEOFactory alloc]init];
    XCTAssertNotNil(factory, @"Default factory creation should succeed here");
    
    // Factory always has a valid identity
    XCTAssertNotNil(factory.identity, @"Every factory has a valid identity");
    XCTAssertNotEqualObjects(factory.identity.realmId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.userId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.deviceId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.url, @"", @"Closed domain should have a valid URL");
   
    // Second factory creation should fail
    factoryBis = [[QEOFactory alloc]init];
    XCTAssertNil(factoryBis, @"Second factory creation should fail at the moment");
    factoryBis = [[QEOFactory alloc]initWithError:nil];
    XCTAssertNil(factoryBis, @"Second factory creation should fail at the moment");
    factoryBis = [[QEOFactory alloc]initWithError:&error];
    XCTAssertNil(factoryBis, @"Second factory creation should fail at the moment");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertNotEqual(error.code, 0, @"Error code should not be 0 if factory creation fails");
    
    // After release of the factory new factories should be creatable
    factory = nil;
    factory = [[QEOFactory alloc]init];
    XCTAssertNotNil(factory, @"Default factory creation should succeed here");
    
    // Second factory creation should fail
    factoryBis = [[QEOFactory alloc]init];
    XCTAssertNil(factoryBis, @"Second factory creation should fail at the moment");
    factoryBis = [[QEOFactory alloc]initWithError:nil];
    XCTAssertNil(factoryBis, @"Second factory creation should fail at the moment");
    factoryBis = [[QEOFactory alloc]initWithError:&error];
    XCTAssertNil(factoryBis, @"Second factory creation should fail at the moment");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertNotEqual(error.code, 0, @"Error code should not be 0 if factory creation fails");
    
    // After release of the factory new factories should be creatable
    factory = nil;
    factory = [[QEOFactory alloc]initWithError:nil];
    XCTAssertNotNil(factory, @"Factory creation should succeed here");
    XCTAssertNotNil(factory.identity, @"Every factory has a valid identity");
    XCTAssertNotEqualObjects(factory.identity.realmId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.userId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.deviceId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.url, @"", @"Closed domain should have a valid URL");
    
    factory = nil;
    factory = [[QEOFactory alloc]initWithError:&error];
    XCTAssertNotNil(factory, @"Factory creation should succeed here");
    XCTAssertNotNil(factory.identity, @"Every factory has a valid identity");
    XCTAssertNotEqualObjects(factory.identity.realmId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.userId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.deviceId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.url, @"", @"Closed domain should have a valid URL");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertTrue(error.code == 0, @"Error code should be 0 if factory creation succeeds");
    
    // What if error domain is not nil?
    error = [NSError errorWithDomain:@"WrongDomain" code:666 userInfo:nil];
    factory = nil;
    factory = [[QEOFactory alloc]initWithError:&error];
    XCTAssertNotNil(factory, @"Factory creation should succeed here");
    XCTAssertNotNil(factory.identity, @"Every factory has a valid identity");
    XCTAssertNotEqualObjects(factory.identity.realmId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.userId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.deviceId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.url, @"", @"Closed domain should have a valid URL");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertTrue(error.code == 0, @"Error code should be 0 if factory creation succeeds");
    
    factoryBis = [[QEOFactory alloc]init];
    XCTAssertNil(factoryBis, @"Second factory creation should fail at the moment");
    factoryBis = [[QEOFactory alloc]initWithError:nil];
    XCTAssertNil(factoryBis, @"Second factory creation should fail at the moment");
    factoryBis = [[QEOFactory alloc]initWithError:&error];
    XCTAssertNil(factoryBis, @"Second factory creation should fail at the moment");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertNotEqual(error.code, 0, @"Error code should not be 0 if factory creation fails");
    
    // Factory creation with identity tests
    
    // Closed domain factory
    factory = nil;
    factory = [[QEOFactory alloc]initWithQeoIdentity:nil error:&error];
    XCTAssertNotNil(factory, @"Factory creation should succeed here");
    XCTAssertNotNil(factory.identity, @"Every factory has a valid identity");
    XCTAssertNotEqualObjects(factory.identity.realmId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.userId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.deviceId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.url, @"", @"Closed domain should have a valid URL");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertTrue(error.code == 0, @"Error code should be 0 if factory creation succeeds");
    
    factory = nil;
    factory = [[QEOFactory alloc]initWithQeoIdentity:nil error:nil];
    XCTAssertNotEqualObjects(factory.identity.realmId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.userId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.deviceId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.url, @"", @"Closed domain should have a valid URL");
    XCTAssertNotNil(factory, @"Factory creation should succeed here");
    XCTAssertNotNil(factory.identity, @"Every factory has a valid identity");
    
    identityList = [QEOIdentity retrieveQeoIdentities];
    XCTAssertNotEqual(identityList.count, 0, @"The identity list should not be empty");
    
    factory = nil;
    factory = [[QEOFactory alloc]initWithQeoIdentity:[identityList lastObject] error:&error];
    XCTAssertNotEqualObjects(factory.identity.realmId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.userId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.deviceId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.url, @"", @"Closed domain should have a valid URL");
    XCTAssertNotNil(factory, @"Factory creation should succeed here");
    XCTAssertNotNil(factory.identity, @"Every factory has a valid identity");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertTrue(error.code == 0, @"Error code should be 0 if factory creation succeeds");
    
    factory = nil;
    factory = [[QEOFactory alloc]initWithQeoIdentity:[identityList lastObject] error:nil];
    XCTAssertNotEqualObjects(factory.identity.realmId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.userId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.deviceId, @0, @"Closed domain should not have 0 as realm ID");
    XCTAssertNotEqualObjects(factory.identity.url, @"", @"Closed domain should have a valid URL");
    XCTAssertNotNil(factory, @"Factory creation should succeed here");
    XCTAssertNotNil(factory.identity, @"Every factory has a valid identity");
    
    // Open domain factory
    factory = nil;
    factory = [[QEOFactory alloc]initWithQeoIdentity:[[QEOIdentity alloc]init] error:&error];
    XCTAssertNotNil(factory, @"Open domain factory creation should succeed here");
    XCTAssertNotNil(factory.identity, @"Every factory has a valid identity");
    XCTAssertEqualObjects(factory.identity.realmId, nil, @"Open domain should have 0 as realm ID");
    XCTAssertEqualObjects(factory.identity.userId, nil, @"Open domain should have 0 as realm ID");
    XCTAssertEqualObjects(factory.identity.deviceId, nil, @"Open domain should have 0 as realm ID");
    XCTAssertEqualObjects(factory.identity.url, nil, @"Open domain has no need for the URL");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertTrue(error.code == 0, @"Error code should be 0 if factory creation succeeds");
    
    factory = nil;
    factory = [[QEOFactory alloc]initWithQeoIdentity:[[QEOIdentity alloc]init] error:nil];
    XCTAssertNotNil(factory, @"Open domain factory creation should succeed here");
    XCTAssertNotNil(factory.identity, @"Every factory has a valid identity");
    XCTAssertEqualObjects(factory.identity.realmId, nil, @"Open domain should have 0 as realm ID");
    XCTAssertEqualObjects(factory.identity.userId, nil, @"Open domain should have 0 as realm ID");
    XCTAssertEqualObjects(factory.identity.deviceId, nil, @"Open domain should have 0 as realm ID");
    XCTAssertEqualObjects(factory.identity.url, nil, @"Open domain has no need for the URL");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertTrue(error.code == 0, @"Error code should be 0 if factory creation succeeds");
    
    error = nil;
    factoryBis = nil;
    factoryBis = [[QEOFactory alloc]initWithQeoIdentity:[[QEOIdentity alloc]init] error:&error];
    XCTAssertEqualObjects(factoryBis, nil, @"Second Open domain factory creation should not succeed");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertFalse(error.code == 0, @"Error code should not be 0 if factory creation failed");
    
    factory = nil;
    factoryBis = nil;
}

- (void)testFactoryCreationStressTest
{
    QEOFactory *factory;
    int nbOfIterations = 20;
    
    for (int index = 0; index < nbOfIterations; index++) {
        factory = [[QEOFactory alloc]init];
        XCTAssertNotNil(factory, @"factory creation should always succeed here");
        factory = nil;
        factory = [[QEOFactory alloc]initWithQeoIdentity:[[QEOIdentity alloc]init] error:nil];
        XCTAssertNotNil(factory, @"factory creation should always succeed here");
        factory = nil;
    }
}

- (void)testOpenFactoryCreation
{
    NSError *error;
    QEOFactory *factory;
    QEOEventReader *eventReader;
    QEOEventWriter *eventWriter;
    QEOStateReader *stateReader;
    QEOStateChangeReader *stateChangeReader;
    QEOStateWriter *stateWriter;
    
        // This contains an empty identity that will create an open domain factory
    factory = [[QEOFactory alloc]initWithQeoIdentity:[[QEOIdentity alloc]init] error:&error];
    XCTAssertNotNil(factory, @"The open factory creation should succeed here");
    XCTAssertNotNil(error, @"the error code should not be nil here");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"error domain should be correct");
    XCTAssertTrue(error.code == 0, @"The error code should be 0 if the factory creation succeeded");
    // Normal reader and writer creation should not fail
    eventReader = [[QEOEventReader alloc]initWithType:[ChatMessage class]
                                              factory:factory
                                             delegate:self
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNotNil(eventReader, @"The event reader creation failed for normal readers and writers on the open domain");
    XCTAssertNotNil(error, @"The error message should not be nil");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"error domain should be correct");
    XCTAssertTrue(error.code == 0, @"The error code should be 0 if the eventReader creation succeeded");
    eventWriter = [[QEOEventWriter alloc]initWithType:[ChatMessage class]
                                              factory:factory
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNotNil(eventWriter, @"The event writer creation should not fail for normal readers and writers on the open domain");
    XCTAssertNotNil(error, @"The error message should not be nil");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"error domain should be correct");
    XCTAssertTrue(error.code == 0, @"The error code should be 0 if the eventWriter creation succeeded");
    stateReader = [[QEOStateReader alloc]initWithType:[DeviceInfo class]
                                              factory:factory
                                             delegate:self
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNotNil(stateReader, @"The state reader should not be nil here");
    XCTAssertNotNil(error, @"the error code should not be nil here");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"error domain should be correct");
    XCTAssertTrue(error.code == 0, @"The error code should be 0 if the stateReader creation succeeded");

    stateChangeReader = [[QEOStateChangeReader alloc]initWithType:[DeviceInfo class]
                                                          factory:factory
                                                         delegate:self
                                                   entityDelegate:self
                                                            error:&error];
    XCTAssertNotNil(stateChangeReader, @"the state reader should be nil");
    XCTAssertNotNil(error, @"the error object should not be nil here");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"error domain should be correct");
    XCTAssertTrue(error.code == 0, @"The error code should be 0 if the stateChangeReader creation succeeded");
    
    stateWriter = [[QEOStateWriter alloc]initWithType:[DeviceInfo class]
                                              factory:factory
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNotNil(stateWriter, @"The statewriter should not be nil");
    XCTAssertNotNil(error, @"the error object should not be nil here");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"error domain should be correct");
    XCTAssertTrue(error.code == 0, @"The error code should be 0 if the statewriter creation succeeded");

    stateReader = [[QEOStateReader alloc]initWithType:[RegistrationCredentials class]
                                              factory:factory
                                             delegate:self
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNotNil(stateReader, @"State reader creation should succeed here");
    XCTAssertNotNil(error, @"the error object should not be nil here");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"error domain should be correct");
    XCTAssertTrue(error.code == 0, @"The error code should be 0 if the stateReader creation succeeded");
    
    stateChangeReader = [[QEOStateChangeReader alloc]initWithType:[RegistrationCredentials class]
                                                          factory:factory
                                                         delegate:self
                                                   entityDelegate:self
                                                            error:&error];
    XCTAssertNotNil(stateChangeReader, @"the state change reader should not be nil");
    XCTAssertNotNil(error, @"the error object should not be nil here");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"error domain should be correct");
    XCTAssertTrue(error.code == 0, @"The error code should be 0 if the stateChangeReader creation succeeded");

    stateWriter = [[QEOStateWriter alloc]initWithType:[RegistrationRequest class]
                                              factory:factory
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNotNil(stateWriter, @"The state writer should not be nil here");
    XCTAssertNotNil(error, @"the error object should not be nil here");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"error domain should be correct");
    XCTAssertTrue(error.code == 0, @"The error code should be 0 if the stateWriter creation succeeded");

    error = nil;
    factory = nil;
    eventReader = nil;
    eventWriter = nil;
    stateReader = nil;
    stateChangeReader = nil;
    stateWriter = nil;
}

@end
