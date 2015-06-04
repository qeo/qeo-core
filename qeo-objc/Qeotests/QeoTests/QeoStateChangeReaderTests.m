#import <XCTest/XCTest.h>

#import <Qeo/Qeo.h>

#import "QDMCombination.h"
#import "QeoCredentialsHandler.h"

@interface QeoStateChangeReaderTests : XCTestCase <QEOStateChangeReaderDelegate, QEOEntityDelegate>

@property (nonatomic, strong) QEOFactory *factory;

@end

@implementation QeoStateChangeReaderTests

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
    XCTAssertNotNil(self.factory, @"Factory creation should have succeeded");
}

- (void)tearDown
{
    // Put teardown code here; it will be run once, after the last test case.
    self.factory = nil;
    [super tearDown];
}

- (BOOL) isValidStateChangeReader:(QEOStateChangeReader *)stateChangeReader
{
    return YES;
}

- (void)testStateChangeReaderCreation
{
    
    NSError *error;
    QEOStateChangeReader *stateChangeReader;
    
    stateChangeReader = [[QEOStateChangeReader alloc]init];
    XCTAssertNil(stateChangeReader, @"default initialiser should fail here");
    
    stateChangeReader = [[QEOStateChangeReader alloc]initWithType:nil
                                                          factory:self.factory
                                                         delegate:self
                                                   entityDelegate:self
                                                            error:&error];
    XCTAssertNil(stateChangeReader, @"Statechange reader creation should fail here");
    XCTAssertNotNil(error, @"The error should not be nil here");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"the domain should be correct");
    XCTAssertTrue([error code] != 0, @"state change reader creation should have failed, so code is not 0");
    stateChangeReader = nil;
    error = nil;
    
    stateChangeReader = [[QEOStateChangeReader alloc]initWithType:[NSString class]
                                                          factory:self.factory
                                                         delegate:self
                                                   entityDelegate:self
                                                            error:&error];
    XCTAssertNil(stateChangeReader, @"Statechange reader creation should fail here");
    XCTAssertNotNil(error, @"The error should not be nil here");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"the domain should be correct");
    XCTAssertTrue([error code] != 0, @"state change reader creation should have failed, so code is not 0");
    stateChangeReader = nil;
    error = nil;
    
    stateChangeReader = [[QEOStateChangeReader alloc]initWithType:[DeviceInfo class]
                                                          factory:nil
                                                         delegate:self
                                                   entityDelegate:self
                                                            error:&error];
    XCTAssertNil(stateChangeReader, @"Statechange reader creation should fail here");
    XCTAssertNotNil(error, @"The error should not be nil here");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"the domain should be correct");
    XCTAssertTrue([error code] != 0, @"state change reader creation should have failed, so code is not 0");
    stateChangeReader = nil;
    error = nil;
    
    stateChangeReader = [[QEOStateChangeReader alloc]initWithType:[DeviceInfo class]
                                                          factory:self.factory
                                                         delegate:nil
                                                   entityDelegate:self
                                                            error:&error];
    
    XCTAssertNotNil(stateChangeReader, @"state change reader creation should have succeeded");
    XCTAssertTrue([self isValidStateChangeReader:stateChangeReader], @"state change reader should be valid");
    
    XCTAssertNotNil(error, @"The error should not be nil here");
    XCTAssertNil(stateChangeReader.delegate, @"delegate should be correct");
    XCTAssertEqual(stateChangeReader.entityDelegate, self, @"entity delegate should be correct");
    XCTAssertEqual(stateChangeReader.factory, self.factory, @"factory should be correct");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"the domain should be correct");
    XCTAssertTrue([error code] == 0, @"state change reader creation should have succeeded, so code is 0");
    sleep(1); /* REMOVE THIS SLEEP WHEN DE3418 IS FIXED */
    stateChangeReader = nil;
    error = nil;

   
    
    stateChangeReader = [[QEOStateChangeReader alloc]initWithType:[DeviceInfo class]
                                                          factory:self.factory
                                                         delegate:self
                                                   entityDelegate:nil
                                                            error:&error];
    XCTAssertNotNil(stateChangeReader, @"state change reader creation should have succeeded");
    XCTAssertTrue([self isValidStateChangeReader:stateChangeReader], @"state change reader should be valid");
    XCTAssertNotNil(error, @"The error should not be nil here");
    XCTAssertEqual(stateChangeReader.delegate, self, @"delegate should be correct");
    XCTAssertNil(stateChangeReader.entityDelegate, @"entity delegate should be correct");
    XCTAssertEqual(stateChangeReader.factory, self.factory, @"factory should be correct");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"the domain should be correct");
    XCTAssertTrue([error code] == 0, @"state change reader creation should have succeeded, so code is 0");
    sleep(1);/* REMOVE THIS SLEEP WHEN DE3418 IS FIXED */

    stateChangeReader = nil;
    error = nil;
    
    stateChangeReader = [[QEOStateChangeReader alloc]initWithType:[DeviceInfo class]
                                                          factory:self.factory
                                                         delegate:self
                                                   entityDelegate:self
                                                            error:nil];
    XCTAssertNotNil(stateChangeReader, @"state change reader creation should have succeeded");
    XCTAssertTrue([self isValidStateChangeReader:stateChangeReader], @"state change reader should be valid");
    XCTAssertEqual(stateChangeReader.delegate, self, @"delegate should be correct");
    XCTAssertEqual(stateChangeReader.entityDelegate, self, @"entity delegate should be correct");
    XCTAssertEqual(stateChangeReader.factory, self.factory, @"factory should be correct");
    sleep(1);/* REMOVE THIS SLEEP WHEN DE3418 IS FIXED */

    stateChangeReader = nil;
    error = nil;
        
    stateChangeReader = [[QEOStateChangeReader alloc]initWithType:[DeviceInfo class]
                                                          factory:self.factory
                                                         delegate:self
                                                   entityDelegate:self
                                                            error:&error];
    XCTAssertNotNil(stateChangeReader, @"state change reader creation should have succeeded");
    XCTAssertTrue([self isValidStateChangeReader:stateChangeReader], @"state change reader should be valid");
    XCTAssertNotNil(error, @"The error should not be nil here");
    XCTAssertEqual(stateChangeReader.delegate, self, @"delegate should be correct");
    XCTAssertEqual(stateChangeReader.entityDelegate, self, @"entity delegate should be correct");
    XCTAssertEqual(stateChangeReader.factory, self.factory, @"factory should be correct");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"the domain should be correct");
    XCTAssertTrue([error code] == 0, @"state change reader creation should have succeeded, so code is 0");
    sleep(1);/* REMOVE THIS SLEEP WHEN DE3418 IS FIXED */

    stateChangeReader = nil;
    error = nil;
}

@end

