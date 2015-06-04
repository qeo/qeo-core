#import <XCTest/XCTest.h>
#import <Qeo/Qeo.h>

#import "QDMCombination.h"
#import "QeoCredentialsHandler.h"

@interface QeoEventReaderTests : XCTestCase <QEOEventReaderDelegate, QEOEntityDelegate>
@property QEOFactory *factory;

@end

@implementation QeoEventReaderTests

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
    
    // Create a default factory
    self.factory = [[QEOFactory alloc]init];
}

- (void)tearDown
{
    // Put teardown code here; it will be run once, after the last test case.
    self.factory = nil;
    
    [super tearDown];
}

- (BOOL)allowAccessForEntity:(QEOEntity *)entity
                    identity:(QEOPolicyIdentity *)identity
{
    return TRUE;
}

- (BOOL)isValidEventReader:(QEOEventReader *)eventReader
{
      return YES;
}

- (void)testEventReaderCreation
{
    QEOEventReader *eventReader;
    NSError *error;
    
    eventReader = [[QEOEventReader alloc]init];
    XCTAssertNil(eventReader, @"Event Reader creation should fail here!");

    eventReader = [[QEOEventReader alloc]initWithType:nil
                                              factory:self.factory
                                             delegate:self
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNil(eventReader, @"Event reader creation should fail here");
    XCTAssertEqual(error.domain, @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertFalse(error.code == 0, @"Error code should not be 0 if event reader creation fails");
    
    eventReader = [[QEOEventReader alloc]initWithType:[ChatMessage class]
                                              factory:nil
                                             delegate:self
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNil(eventReader, @"Event reader creation should fail here");
    XCTAssertEqual(error.domain, @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertFalse(error.code == 0, @"Error code should not be 0 if event reader creation fails");
    
    
    eventReader = [[QEOEventReader alloc]initWithType:[ChatMessage class]
                                              factory:self.factory
                                             delegate:nil
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNotNil(eventReader, @"Event reader creation should succeed here");
    XCTAssertEqual(eventReader.factory, self.factory, @"Factory should be self here");
    XCTAssertEqualObjects(eventReader.delegate, nil, @"Delegate should be nil here!");
    XCTAssertEqual(eventReader.entityDelegate, self, @"EntityDelegate should be self here!");
    XCTAssertEqual(error.domain, @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertTrue(error.code == 0, @"Error code should be 0 if event reader creation succeeds");
    XCTAssertTrue([self isValidEventReader:eventReader], @"Returned event reader is not a valid one");
    
    eventReader = [[QEOEventReader alloc]initWithType:[ChatMessage class]
                                              factory:self.factory
                                             delegate:self
                                       entityDelegate:nil
                                                error:&error];
    XCTAssertNotNil(eventReader, @"Event reader creation should succeed here");
    XCTAssertNotNil(eventReader, @"Event reader creation should succeed here");
    XCTAssertEqual(eventReader.factory, self.factory, @"Factory should be self here");
    XCTAssertEqual(eventReader.delegate, self, @"Delegate should be self here!");
    XCTAssertEqualObjects(eventReader.entityDelegate, nil, @"EntityDelegate should be nil here!");
    XCTAssertEqual(error.domain, @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertTrue(error.code == 0, @"Error code should be 0 if event reader creation succeeds");
    XCTAssertTrue([self isValidEventReader:eventReader], @"Returned event reader is not a valid one");
    
    eventReader = [[QEOEventReader alloc]initWithType:[ChatMessage class]
                                              factory:self.factory
                                             delegate:self
                                       entityDelegate:self
                                                error:nil];
    XCTAssertNotNil(eventReader, @"Event reader creation should succeed here");
    XCTAssertTrue([self isValidEventReader:eventReader], @"Returned event reader is not a valid one");
    XCTAssertNotNil(eventReader, @"Event reader creation should succeed here");
    XCTAssertEqual(eventReader.factory, self.factory, @"Factory should be self here");
    XCTAssertEqual(eventReader.delegate, self, @"Delegate should be self here!");
    XCTAssertEqual(eventReader.entityDelegate, self, @"EntityDelegate should be self here!");
    XCTAssertTrue([self isValidEventReader:eventReader], @"Returned event reader is not a valid one");
    
    eventReader = [[QEOEventReader alloc]initWithType:[ChatMessage class]
                                              factory:self.factory
                                             delegate:self
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNotNil(eventReader, @"Event reader creation should succeed here");
    XCTAssertNotNil(eventReader, @"Event reader creation should succeed here");
    XCTAssertEqual(eventReader.factory, self.factory, @"Factory should be self here");
    XCTAssertEqual(eventReader.delegate, self, @"Delegate should be self here!");
    XCTAssertEqual(eventReader.entityDelegate, self, @"EntityDelegate should be self here!");
    XCTAssertEqual(error.domain, @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertTrue(error.code == 0, @"Error code should be 0 if event reader creation succeeds");
    XCTAssertTrue([self isValidEventReader:eventReader], @"Returned event reader is not a valid one");

    eventReader = nil;
}

@end
