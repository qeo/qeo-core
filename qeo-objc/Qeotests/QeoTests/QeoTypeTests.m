#import <XCTest/XCTest.h>
#import <Qeo/Qeo.h>
#import "QDMCombination.h"
#import "QeoCredentialsHandler.h"
@interface QeoTypeTests : XCTestCase<QEOStateReaderDelegate>
@property (nonatomic, strong) QEOFactory *factory;

@end

@implementation QeoTypeTests
TestState *test;
TestState *testRead;

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
    self.factory = [[QEOFactory alloc]init];
}

- (void)tearDown
{
    // Put teardown code here; it will be run once, after the last test case.
   
    self.factory = nil;
    [super tearDown];
}

- (void)testType
{
    // Very simple, should never succeed
    QEOType *qeoType = [[QEOType alloc]init];
    XCTAssertNil(qeoType, @"Qeo type is an abstract class and should not be allowed to be created");
}

- (void)testisEqualAndHashType
{
    // Very simple, should never succeed
    QEOType *qeoType = [[QEOType alloc]init];
    XCTAssertNil(qeoType, @"Qeo type is an abstract class and should not be allowed to be created");
    
    QEOStateWriter *stateWriter;
    QEOStateReader *stateReader;
    NSError *error;
    stateWriter = [[QEOStateWriter alloc]initWithType:[TestState class]
                                              factory:_factory
                                       entityDelegate:nil
                                                error:&error];
    
    test = [[TestState alloc]init];
    test.deviceId = [[DeviceId alloc]init];
    test.deviceId.upper = 90220;
    test.deviceId.lower = 797204;
    test.manufacturer=@"testManufacturer";
    
    [stateWriter write:test withError:nil];
    
    stateReader=[[QEOStateReader alloc]initWithType:[TestState class] factory:_factory delegate:self entityDelegate:nil error:nil];
    
    sleep(1);
    
    [stateReader enumerateInstancesUsingBlock:^void (QEOType *qeoType, BOOL *cont) {
        
        testRead=(TestState *)qeoType;
        
    }];
    
    BOOL temp= [test isEqual:testRead];
    BOOL hashvalue=[test hash]==[testRead hash];
    XCTAssertTrue(temp, "The two objecst must be equal");
    XCTAssertTrue(hashvalue, "hash values must be equal");
    
    test.deviceId.upper = 90221;
    test.deviceId.lower = 74567;
    test.manufacturer=@"testManufacturer1";
    
    
    temp= [test.deviceId isEqual:testRead.deviceId];
    XCTAssertFalse(temp, "deviceid for 2 objects must not be equal ");
    temp= [test isEqual:testRead];
    XCTAssertFalse(temp, "objects must not be equal ");
    hashvalue=[test hash]==[testRead hash];
    XCTAssertFalse(hashvalue, "hash values must not be equal");
    
    testRead = nil;
    test = nil;
    stateReader=nil;
    stateWriter=nil;
}


@end
