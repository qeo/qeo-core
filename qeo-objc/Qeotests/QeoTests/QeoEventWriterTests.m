#import <XCTest/XCTest.h>
#import <Qeo/Qeo.h>

#import "QDMCombination.h"
#import "QeoCredentialsHandler.h"

@interface QeoEventWriterTests : XCTestCase <QEOEntityDelegate>

@property (strong, nonatomic) QEOFactory *factory;

@end

@implementation QeoEventWriterTests

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

- (BOOL)isValidEventWriter:(QEOEventWriter *)eventWriter
{
    return YES;
}

- (void)testEventWriterCreation
{
    QEOEventWriter *eventWriter;
    NSError *error;

    eventWriter = [[QEOEventWriter alloc]init];
    XCTAssertNil(eventWriter, @"EventWriter creation with default initialiser should fail");
    
    eventWriter = [[QEOEventWriter alloc]initWithType:nil
                                              factory:self.factory
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNil(eventWriter, @"eventWriter creation should fail here");
    XCTAssertNotNil(error, @"Error return code should not be nil");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertNotEqual([error code], 0, @"Return code should not be 0 if event writer was not created successfully");
    
    eventWriter = [[QEOEventWriter alloc]initWithType:[ChatMessage class]
                                              factory:nil
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNil(eventWriter, @"eventWriter creation should fail here");
    XCTAssertNotNil(error, @"Error return code should not be nil");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertNotEqual([error code], 0, @"Return code should be 0 if event writer was created successfully");
    
    eventWriter = [[QEOEventWriter alloc]initWithType:[ChatMessage class]
                                              factory:self.factory
                                       entityDelegate:nil
                                                error:&error];
    XCTAssertNotNil(eventWriter, @"eventWriter creation should succeed here");
    XCTAssertEqualObjects(eventWriter.factory, self.factory, @"factory should be the same here");
    XCTAssertEqualObjects(eventWriter.entityDelegate, nil, @"Entity should be the same as the input");
    XCTAssertNotNil(error, @"Error return code should not be nil");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertTrue(error.code == 0, @"Return code should be 0 if event writer was created successfully");
    XCTAssertTrue([self isValidEventWriter:eventWriter], @"Eventwriter should be a valid eventwriter");
    
    eventWriter = [[QEOEventWriter alloc]initWithType:[ChatMessage class]
                                              factory:self.factory
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNotNil(eventWriter, @"eventWriter creation should succeed here");
    XCTAssertEqualObjects(eventWriter.factory, self.factory, @"factory should be the same here");
    XCTAssertEqualObjects(eventWriter.entityDelegate, self, @"Entity should be the same as the input");
    XCTAssertTrue([self isValidEventWriter:eventWriter], @"Eventwriter should be a valid eventwriter");
    
    eventWriter = [[QEOEventWriter alloc]initWithType:[ChatMessage class]
                                              factory:self.factory
                                       entityDelegate:self
                                                error:&error];
    XCTAssertNotNil(eventWriter, @"eventWriter creation should succeed here");
    XCTAssertEqualObjects(eventWriter.factory, self.factory, @"factory should be the same here");
    XCTAssertEqualObjects(eventWriter.entityDelegate, self, @"Entity should be the same as the input");
    XCTAssertNotNil(error, @"Error return code should not be nil");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Error Domain should be Qeo");
    XCTAssertTrue(error.code == 0, @"Return code should be 0 if event writer was created successfully");
    XCTAssertTrue([self isValidEventWriter:eventWriter], @"Eventwriter should be a valid eventwriter");

    eventWriter = nil;
}

@end
