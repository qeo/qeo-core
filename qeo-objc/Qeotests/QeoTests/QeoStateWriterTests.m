#import <XCTest/XCTest.h>
#import <Qeo/Qeo.h>

#import "QDMCombination.h"
#import "QeoCredentialsHandler.h"

@interface QeoStateWriterTests : XCTestCase <QEOEntityDelegate>

@property (nonatomic, strong) QEOFactory *factory;

@end

@implementation QeoStateWriterTests

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
    XCTAssertNotNil(self.factory, @"Factory initialisation should have succeeded");
}

- (void)tearDown
{
    // Put teardown code here; it will be run once, after the last test case.
    self.factory = nil;
    [super tearDown];
}

- (BOOL)isValidStateWriter:(QEOStateWriter *)stateWriter
{
       return YES;
}

- (void)testStateWriterCreation
{
    NSError *error;
    
    QEOStateWriter *stateWriter = [[QEOStateWriter alloc]init];
    XCTAssertNil(stateWriter, @"Default initialiser should fail here");

    stateWriter = [[QEOStateWriter alloc]initWithType:nil
                                              factory:self.factory
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNil(stateWriter, @"Statewriter creation should fail here");
    XCTAssertNotNil(error, @"error should be provided");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Domain of the error should be org.qeo");
    XCTAssertTrue([error code] != 0, @"The error code should not be zero in this case");
    
    stateWriter = [[QEOStateWriter alloc]initWithType:[@"Wrong Type definition" class]
                                              factory:self.factory
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNil(stateWriter, @"Statewriter creation should fail here");
    XCTAssertNotNil(error, @"error should be provided");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Domain of the error should be org.qeo");
    XCTAssertTrue([error code] != 0, @"The error code should not be zero in this case");
    
    stateWriter = [[QEOStateWriter alloc]initWithType:[TestState class]
                                              factory:nil
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNil(stateWriter, @"Statewriter creation should fail here");
    XCTAssertNotNil(error, @"error should be provided");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Domain of the error should be org.qeo");
    XCTAssertTrue([error code] != 0, @"The error code should not be zero in this case");
    
    stateWriter = [[QEOStateWriter alloc]initWithType:[TestState class]
                                              factory:nil
                                       entityDelegate:self
                                                error:nil];
    XCTAssertNil(stateWriter, @"Statewriter creation should fail here");
    
    stateWriter = [[QEOStateWriter alloc]initWithType:[TestState class]
                                              factory:self.factory
                                       entityDelegate:nil
                                                error:&error];
    XCTAssertNotNil(stateWriter, @"Statewriter creation should succeed here");
    XCTAssertTrue([self isValidStateWriter:stateWriter], @"should be a valid stateWriter!");
    XCTAssertNil(stateWriter.entityDelegate, @"entity delegate should be nil here");
    XCTAssertNotNil(error, @"error should be provided");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Domain of the error should be org.qeo");
    XCTAssertTrue([error code] == 0, @"The error code should not be zero in this case");
    
    stateWriter = [[QEOStateWriter alloc]initWithType:[TestState class]
                                              factory:self.factory
                                       entityDelegate:self
                                                error:nil];
    XCTAssertNotNil(stateWriter, @"Statewriter creation should succeed here");
    XCTAssertTrue([self isValidStateWriter:stateWriter], @"should be a valid stateWriter!");
    XCTAssertEqual(stateWriter.entityDelegate, self, @"the entity delegate should be the same");
    XCTAssertNotNil(error, @"error should be provided");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Domain of the error should be org.qeo");
    XCTAssertTrue([error code] == 0, @"The error code should not be zero in this case");

    stateWriter = nil;
}

@end
