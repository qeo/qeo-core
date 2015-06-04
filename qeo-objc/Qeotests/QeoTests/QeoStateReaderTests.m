#import <XCTest/XCTest.h>

#import <Qeo/Qeo.h>

#import "QDMCombination.h"
#import "QeoCredentialsHandler.h"

@interface QeoStateReaderTests : XCTestCase <QEOStateReaderDelegate, QEOEntityDelegate>

@property (nonatomic, strong) QEOFactory *factory;

@end

@implementation QeoStateReaderTests

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
    
    XCTAssertNotNil(self.factory, @"factory creation should have succeeded here");
}

- (void)tearDown
{
    // Put teardown code here; it will be run once, after the last test case.
    self.factory = nil;
    [super tearDown];
}

- (BOOL)isValidStateReader:(QEOStateReader *)stateReader
{
    return YES;
}

- (void)testStateReaderCreation
{
    NSError *error;
    QEOStateReader *stateReader;
    
    stateReader = [[QEOStateReader alloc]init];
    XCTAssertNil(stateReader, @"state reader creation should fail with default initialiser");
    stateReader = nil;
    error = nil;
    
    stateReader = [[QEOStateReader alloc]initWithType:nil
                                              factory:self.factory
                                             delegate:self
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNil(stateReader, @"State reader creation should fail here");
    XCTAssertNotNil(error, @"error should return a valid error");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"the domain should be correct");
    XCTAssertTrue([error code] != 0, @"The error code should not be 0 if something went wrong");
    stateReader = nil;
    error = nil;
    
    stateReader = [[QEOStateReader alloc]initWithType:[NSString class]
                                              factory:self.factory
                                             delegate:self
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNil(stateReader, @"State reader creation should fail here");
    XCTAssertNotNil(error, @"error should return a valid error");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"the domain should be correct");
    XCTAssertTrue([error code] != 0, @"The error code should not be 0 if something went wrong");
    stateReader = nil;
    error = nil;
    
    stateReader = [[QEOStateReader alloc]initWithType:[TestState class]
                                              factory:nil
                                             delegate:self
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNil(stateReader, @"State reader creation should fail here");
    XCTAssertNotNil(error, @"error should return a valid error");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"the domain should be correct");
    XCTAssertTrue([error code] != 0, @"The error code should not be 0 if something went wrong");
    stateReader = nil;
    error = nil;
    
    stateReader = [[QEOStateReader alloc]initWithType:[TestState class]
                                              factory:self.factory
                                             delegate:nil
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNotNil(stateReader, @"State reader creation should succeed here");
    XCTAssertTrue([self isValidStateReader:stateReader], @"should return a valid state reader");
    XCTAssertEqual(stateReader.factory, self.factory, @"The factory should be correct");
    XCTAssertNil(stateReader.delegate, @"The delegate should be correct");
    XCTAssertEqual(stateReader.entityDelegate, self, @"the entity delegate should be correct");
    XCTAssertNotNil(error, @"error should return a valid error");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"the domain should be correct");
    XCTAssertTrue([error code] == 0, @"The error code should not be 0 if something went wrong");
    stateReader = nil;
    error = nil;
    
    stateReader = [[QEOStateReader alloc]initWithType:[TestState class]
                                              factory:self.factory
                                             delegate:self
                                       entityDelegate:nil
                                                error:&error];
    XCTAssertNotNil(stateReader, @"State reader creation should succeed here");
    XCTAssertTrue([self isValidStateReader:stateReader], @"should return a valid state reader");
    XCTAssertEqual(stateReader.factory, self.factory, @"The factory should be correct");
    XCTAssertEqual(stateReader.delegate, self, @"The delegate should be correct");
    XCTAssertNil(stateReader.entityDelegate, @"the entity delegate should be correct");
    XCTAssertNotNil(error, @"error should return a valid error");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"the domain should be correct");
    XCTAssertTrue([error code] == 0, @"The error code should not be 0 if something went wrong");
    stateReader = nil;
    error = nil;
    
    stateReader = [[QEOStateReader alloc]initWithType:[TestState class]
                                              factory:self.factory
                                             delegate:self
                                       entityDelegate:self
                                                error:nil];
    XCTAssertNotNil(stateReader, @"State reader creation should succeed here");
    XCTAssertTrue([self isValidStateReader:stateReader], @"should return a valid state reader");
    XCTAssertEqual(stateReader.factory, self.factory, @"The factory should be correct");
    XCTAssertEqual(stateReader.delegate, self, @"The delegate should be correct");
    XCTAssertEqual(stateReader.entityDelegate, self, @"the entity delegate should be correct");
    stateReader = nil;
    error = nil;
    
    stateReader = [[QEOStateReader alloc]initWithType:[TestState class]
                                              factory:self.factory
                                             delegate:self
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNotNil(stateReader, @"State reader creation should succeed here");
    XCTAssertTrue([self isValidStateReader:stateReader], @"should return a valid state reader");
    XCTAssertEqual(stateReader.factory, self.factory, @"The factory should be correct");
    XCTAssertEqual(stateReader.delegate, self, @"The delegate should be correct");
    XCTAssertEqual(stateReader.entityDelegate, self, @"the entity delegate should be correct");
    XCTAssertNotNil(error, @"error should return a valid error");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"the domain should be correct");
    XCTAssertTrue([error code] == 0, @"The error code should not be 0 if something went wrong");
    stateReader = nil;
    error = nil;    
}

@end
