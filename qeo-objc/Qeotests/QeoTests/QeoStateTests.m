#import <XCTest/XCTest.h>

#import <Qeo/Qeo.h>

#import "QDMCombination.h"
#import "QeoCredentialsHandler.h"

@interface QeoStateTests : XCTestCase <QEOEntityDelegate, QEOStateReaderDelegate, QEOStateChangeReaderDelegate>

@property (nonatomic, strong) QEOFactory *factory;
@property (nonatomic, strong) QEOStateWriter *stateWriter;
@property (nonatomic, strong) NSMutableArray *writtenStates;
@property (nonatomic, strong) QEOStateReader *stateReader;
@property (nonatomic, assign) int updatesReceived;
@property (nonatomic, strong) QEOStateChangeReader *stateChangeReader;
@property (nonatomic, assign) int burstsReceived;
@property (nonatomic, strong) NSMutableArray *receivedStates;

@end

@implementation QeoStateTests

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
    XCTAssertNotNil(self.factory, @"factory creation should have succeeded");
    self.stateWriter = [[QEOStateWriter alloc]initWithType:[TestState class]
                                                   factory:self.factory
                                            entityDelegate:self
                                                     error:nil];
    XCTAssertNotNil(self.stateWriter, @"statewriter creation should have succeeded");
    self.writtenStates = [[NSMutableArray alloc]init];
    XCTAssertNotNil(self.writtenStates, @"written states array creation should have succeeded");
    self.stateReader = [[QEOStateReader alloc]initWithType:[TestState class]
                                                   factory:self.factory
                                                  delegate:self
                                            entityDelegate:self
                                                     error:nil];
    XCTAssertNotNil(self.stateReader, @"state reader creation should have succeeded");
    self.updatesReceived = 0;
    self.stateChangeReader = [[QEOStateChangeReader  alloc]initWithType:[TestState class]
                                                                factory:self.factory
                                                               delegate:self
                                                         entityDelegate:self
                                                                  error:nil];
    XCTAssertNotNil(self.stateChangeReader, @"state change reader creation should have succeeded");
    self.receivedStates = [[NSMutableArray alloc]init];
    XCTAssertNotNil(self.receivedStates, @"received states array creation should have succeeded");
    self.burstsReceived = 0;
}

- (void)tearDown
{
    // Put teardown code here; it will be run once, after the last test case.
    self.receivedStates = nil;
    self.stateChangeReader = nil;
    self.stateReader = nil;
    self.writtenStates = nil;
    self.stateWriter = nil;
    self.factory = nil;
    self.updatesReceived = 0;
    self.burstsReceived = 0;
    [super tearDown];
}

#pragma mark - Helper functions implementation
- (int) getIndexInArray:(NSArray *)array
                forState:(QEOType *)state
{
    int index = -1;
    
    // TODO: hash and equal are not yet implemented!
    // Assume for now that the qeotype is of type DeviceInfo and that the keys should be the same in the equals method
    for (int i = 0; i < [array count]; i++) {
        TestState *testState = [array objectAtIndex:i];
        if (testState.deviceId.upper == ((TestState *)state).deviceId.upper &&
            testState.deviceId.lower == ((TestState *)state).deviceId.lower) {
            index = i;
            break;
        }
    }
    
    return index;
}

- (BOOL)isType:(TestState *)type
       equalTo:(TestState *)otherType
{
    // equal and hash is not yet implemented, assume the types are deviceInfo for now!
    if (((TestState *)type).deviceId.upper != ((TestState *)otherType).deviceId.upper ||
        ((TestState *)type).deviceId.lower != ((TestState *)otherType).deviceId.lower ||
        ![((TestState *)type).manufacturer isEqualToString:((TestState *)otherType).manufacturer] ||
        ((TestState *)type).bytenumber != ((TestState *)otherType).bytenumber ||
        ((TestState *)type).int16number != ((TestState *)otherType).int16number ||
        ((TestState *)type).int32number != ((TestState *)otherType).int32number ||
        ((TestState *)type).int64number != ((TestState *)otherType).int64number ||
        ((TestState *)type).floatnumber != ((TestState *)otherType).floatnumber ||
        ((TestState *)type).UUID.upper != ((TestState *)otherType).UUID.upper ||
        ((TestState *)type).UUID.lower != ((TestState *)otherType).UUID.lower ||
        ![((TestState *)type).numbersequence isEqual:((TestState *)otherType).numbersequence] ||
        // ![((TestState *)type).uuidsequence isEqual:((TestState *)otherType).uuidsequence] ||
        ![((TestState *)type).stringsequence isEqual:((TestState *)otherType).stringsequence]) {
        return NO;
    }
    else {
        return YES;
    }
}

- (BOOL)write:(QEOType *)object
    withError:(NSError **)error
{
    BOOL success = [self.stateWriter write:object withError:error];
    
    if (success == YES) {
        int index = [self getIndexInArray:self.writtenStates forState:object];
        if (index < 0) {
            // Add new element in the list
            [self.writtenStates addObject:object];
        }
        else {
            // Update element in the list
            [self.writtenStates replaceObjectAtIndex:index withObject:object];
        }
    }
    
    return success;
}

- (BOOL)remove:(QEOType *)object
     withError:(NSError **)error
{
    BOOL success = [self.stateWriter remove:object withError:error];
    
    if (success == YES) {
        int index = [self getIndexInArray:self.writtenStates forState:object];
        if (index < 0) {
            XCTFail(@" The object should be in the list here");
        }
        else {
            [self.writtenStates removeObjectAtIndex:index];
        }
    }
    
    return success;
}

#pragma mark - Delegate implementation
- (BOOL)allowAccessForEntity:(QEOEntity *)entity
                    identity:(QEOPolicyIdentity *)identity
{
    return TRUE;
}

- (void)didReceiveUpdateForStateReader:(QEOStateReader *)stateReader
{
    self.updatesReceived++;
}

- (void)didReceiveStateChange:(QEOType *)state
                    forReader:(QEOStateChangeReader *)stateChangeReader
{
    int index = [self getIndexInArray:self.receivedStates forState:state];
    
    if (index < 0) {
        // add the new element in the list
        [self.receivedStates addObject:state];
    }
    else {
        // update the element in the list
        [self.receivedStates replaceObjectAtIndex:index withObject:state];
    }
}

- (void)didFinishBurstForStateChangeReader:(QEOStateChangeReader *)stateChangeReader
{
    self.burstsReceived++;
}

- (void)didReceiveStateRemoval:(QEOType *)state
                     forReader:(QEOStateChangeReader *)stateChangeReader
{
    int index = [self getIndexInArray:self.receivedStates forState:state];
    
    if (index < 0) {
        XCTFail(@"object should always be in the list!");
    }
    else {
        [self.receivedStates removeObjectAtIndex:index];
    }
}

#pragma mark - Test implementation

- (void)testWriterFailures
{
    BOOL result = NO;
    NSError *error;
    
    result = [self write:nil withError:&error];
    XCTAssertTrue(result == NO, @"writing of the sample should have failed");
    XCTAssertNotNil(error, @"The error value should not be nil");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"The domain of the error is not correct");
    XCTAssertTrue([error code] != 0, @"The error code should not be 0 if something went wrong");
    error = nil;
    
    // write incorrect type
    result =[self write:[[DeviceId alloc]init] withError:&error];
    XCTAssertTrue(result == NO, @"writing of the sample should have failed");
    XCTAssertNotNil(error, @"The error value should not be nil");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"The domain of the error is not correct");
    XCTAssertTrue([error code] != 0, @"The error code should not be 0 if something went wrong");
    error = nil;
   
    result = [self write:nil withError:nil];
    XCTAssertTrue(result == NO, @"writing of the sample should have failed");

    XCTAssertTrue([self.writtenStates count] == 0, @"No sample should have been written");
    XCTAssertTrue([self.receivedStates count] == 0, @"No sample should have been received");
}

- (void)testWriterSuccess
{
    BOOL result = NO;
    NSError *error;
    TestState *testState;
    
    testState = [[TestState alloc]init];
    testState.deviceId = [[DeviceId alloc]init];
    testState.deviceId.upper = 90210;
    testState.deviceId.lower = 797204;
    testState.manufacturer = @"technicolor";
    testState.bytenumber = 255;
    testState.int16number = INT16_MAX;
    testState.int32number = INT32_MAX;
    testState.int64number = INT64_MAX;
    testState.floatnumber = MAXFLOAT;
    testState.somebool = YES;
    testState.UUID = [[UUID alloc]init];
    testState.UUID.upper = 0;
    testState.UUID.lower = 0;
    testState.numbersequence = @[@0, @1, @2, @3];
    testState.stringsequence = @[@"a", @"b", @"c", @"d"];
    testState.uuidsequence = @[[[UUID alloc]init], [[UUID alloc]init], [[UUID alloc]init]];
    ((UUID *)testState.uuidsequence[0]).upper = 0;
    ((UUID *)testState.uuidsequence[0]).lower = 0;
    ((UUID *)testState.uuidsequence[1]).upper = 1;
    ((UUID *)testState.uuidsequence[1]).lower = 1;
    ((UUID *)testState.uuidsequence[2]).upper = 2;
    ((UUID *)testState.uuidsequence[2]).lower = 2;
    result = [self write:testState withError:&error];
    XCTAssertTrue(result == YES, @"The state should have been written successfully");
    XCTAssertNotNil(error, @"The error should not be nil here");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"The domain should be correct");
    XCTAssertTrue(error.code == 0, @"The error should be 0 if the write succeeded");
    XCTAssertTrue([self.writtenStates count] == 1, @"one state should have been written");
    XCTAssertTrue([self isType:[self.writtenStates lastObject] equalTo:testState] == YES, @"the state should be the same");
    
    // Wait until the state has been received
    sleep(1);
    XCTAssertTrue([self.receivedStates count] == 1, @"one state should have been received");
    XCTAssertTrue([self isType:[self.receivedStates lastObject] equalTo:testState] == YES, @"the received state should be the same");
    XCTAssertTrue(self.updatesReceived == 1, @"One update should have been received");
    XCTAssertTrue(self.burstsReceived == 1, @"One burst event should have been received");
    
    // update the state with some changes
    self.updatesReceived = 0;
    self.burstsReceived = 0;
    testState.manufacturer = @"Technicolor";
    testState.bytenumber = 0;
    testState.int16number = INT16_MIN;
    testState.int32number = INT32_MIN;
    testState.int64number = INT64_MIN;
    testState.floatnumber = -MAXFLOAT;
    testState.somebool = YES;
    testState.UUID = nil;
    testState.numbersequence = @[];
    testState.stringsequence = @[];
    testState.uuidsequence = @[];
    result = [self write:testState withError:&error];
    XCTAssertTrue(result == YES, @"The state should have been written successfully");
    XCTAssertNotNil(error, @"The error should not be nil here");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"The domain should be correct");
    XCTAssertTrue(error.code == 0, @"The error should be 0 if the write succeeded");
    XCTAssertTrue([self.writtenStates count] == 1, @"one state should have been written");
    XCTAssertTrue([self isType:[self.writtenStates lastObject] equalTo:testState] == YES, @"the state should be the same");
    
    sleep(1);
    XCTAssertTrue([self.receivedStates count] == 1, @"one state should have been received");
    XCTAssertTrue([self isType:[self.receivedStates lastObject] equalTo:testState] == YES, @"the received state should be the same");
    XCTAssertTrue(self.updatesReceived == 1, @"One update should have been received");
    XCTAssertTrue(self.burstsReceived == 1, @"One burst event should have been received");
    
    // update the state with some changes
    self.updatesReceived = 0;
    self.burstsReceived = 0;
    testState.manufacturer = nil;
    testState.bytenumber = 0;
    testState.int16number = INT16_MIN;
    testState.int32number = INT32_MIN;
    testState.int64number = INT64_MIN;
    testState.floatnumber = NAN;
    testState.somebool = NO;
    testState.UUID = nil;
    testState.numbersequence = nil;
    testState.stringsequence = nil;
    testState.uuidsequence = nil;
    result = [self write:testState withError:&error];
    XCTAssertTrue(result == YES, @"The state should have been written successfully");
    XCTAssertNotNil(error, @"The error should not be nil here");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"The domain should be correct");
    XCTAssertTrue(error.code == 0, @"The error should be 0 if the write succeeded");
    XCTAssertTrue([self.writtenStates count] == 1, @"one state should have been written");
    
    sleep(1);
    XCTAssertTrue([self.receivedStates count] == 1, @"one state should have been received");
    XCTAssertEqualObjects(((TestState *)[self.receivedStates lastObject]).manufacturer, @"", @"Manufacturer should be the same");
    XCTAssertTrue(((TestState *)[self.receivedStates lastObject]).bytenumber == testState.bytenumber, @"same bytenumber");
    XCTAssertTrue(((TestState *)[self.receivedStates lastObject]).int16number == testState.int16number, @"same bytenumber");
    XCTAssertTrue(((TestState *)[self.receivedStates lastObject]).int32number == testState.int32number, @"same bytenumber");
    XCTAssertTrue(((TestState *)[self.receivedStates lastObject]).int64number == testState.int64number, @"same bytenumber");
    XCTAssertTrue(isnan(((TestState *)[self.receivedStates lastObject]).floatnumber) == YES, @"same float");
    XCTAssertTrue(((TestState *)[self.receivedStates lastObject]).somebool == testState.somebool, @"same bool");
    XCTAssertEqualObjects(((TestState *)[self.receivedStates lastObject]).UUID , [[UUID alloc]init], @"Manufacturer should be the same");
    XCTAssertEqualObjects(((TestState *)[self.receivedStates lastObject]).numbersequence , @[], @"Manufacturer should be the same");
    XCTAssertEqualObjects(((TestState *)[self.receivedStates lastObject]).stringsequence , @[], @"Manufacturer should be the same");
    XCTAssertEqualObjects(((TestState *)[self.receivedStates lastObject]).uuidsequence , @[], @"Manufacturer should be the same");
    XCTAssertTrue(self.updatesReceived == 1, @"One update should have been received");
    XCTAssertTrue(self.burstsReceived == 1, @"One burst event should have been received");
    
    // Remove the state
    self.updatesReceived = 0;
    self.burstsReceived = 0;
    testState.deviceId = [[DeviceId alloc]init];
    testState.deviceId.upper = 90210;
    testState.deviceId.lower = 797204;
    result = [self remove:testState withError:&error];
    XCTAssertTrue(result == YES, @"The removal of the state should have succeeded");
    XCTAssertNotNil(error, @"The error should not be nil here");
    XCTAssertEqualObjects(error.domain, @"org.qeo", @"The domain should be correct");
    XCTAssertTrue(error.code == 0, @"The error should be 0 if the write succeeded");
    XCTAssertTrue([self.writtenStates count] == 0, @"0 states should have been written");
    
    sleep(1);
    XCTAssertTrue([self.receivedStates count] == 0, @"0 states should have been received");
    XCTAssertTrue(self.updatesReceived == 1, @"One update should have been received");
    XCTAssertTrue(self.burstsReceived == 1, @"One burst event should have been received");
    self.updatesReceived = 0;
    self.burstsReceived = 0;
}

- (void)testMultipleStates
{
    BOOL result = NO;
    NSError *error;
    TestState *testState;
    int nbOfStates = 100;
    
    // publish many states at once
    for (int index = 0; index < nbOfStates; index++) {
        testState = [[TestState alloc]init];
        testState.deviceId = [[DeviceId alloc]init];
        testState.deviceId.upper = index;
        testState.deviceId.lower = index;
        testState.manufacturer = [[NSString alloc]initWithFormat:@"index%d", index];
        testState.bytenumber = index;
        testState.int16number = index;
        testState.int32number = index;
        testState.int64number = index;
        testState.floatnumber = index;
        testState.somebool = YES;
        testState.UUID = [[UUID alloc]init];
        testState.UUID.upper = index;
        testState.UUID.lower = index;
        testState.numbersequence = @[[[NSNumber alloc]initWithInt:index]];
        testState.stringsequence = @[@"a", @"b", @"c", @"d"];
        testState.uuidsequence = @[[[UUID alloc]init], [[UUID alloc]init], [[UUID alloc]init]];
        ((UUID *)testState.uuidsequence[0]).upper = index;
        ((UUID *)testState.uuidsequence[0]).lower = index;
        ((UUID *)testState.uuidsequence[1]).upper = index;
        ((UUID *)testState.uuidsequence[1]).lower = index;
        ((UUID *)testState.uuidsequence[2]).upper = index;
        ((UUID *)testState.uuidsequence[2]).lower = index;
        result = [self write:testState withError:&error];
        XCTAssertTrue(result, @"writing should have succeeded");
        XCTAssertNotNil(error, @"the error code should not be nil");
        XCTAssertEqualObjects([error domain], @"org.qeo", @"the domain should be correct");
        XCTAssertTrue([error code] == 0, @"the error code should not be 0 because the write succeeded");
        XCTAssertTrue([self.writtenStates count] == index + 1, @"There should be one elemtn in there");
    }
    sleep(1);
    // check if all states have been received (in the right order)
    XCTAssertTrue([self.writtenStates count] == nbOfStates, @"%d states should be written", nbOfStates);
    XCTAssertTrue([self.receivedStates count] == nbOfStates, @"%d states should be received", nbOfStates);
    XCTAssertTrue(self.updatesReceived > 0 && self.updatesReceived <= nbOfStates, @"One update should have been received");
    XCTAssertTrue(self.burstsReceived > 0 && self.burstsReceived <= nbOfStates, @"One burst event should have been received");
    for (int index = 0; index < nbOfStates; index++) {
        XCTAssertTrue([self isType:self.writtenStates[index] equalTo:self.receivedStates[index]],
                      @"The states should be the same");
    }
    self.updatesReceived = 0;
    self.burstsReceived = 0;
    
    // Update many states at once
    for (int index = 0; index < nbOfStates; index++) {
        testState = [[TestState alloc]init];
        testState.deviceId = [[DeviceId alloc]init];
        testState.deviceId.upper = index;
        testState.deviceId.lower = index;
        testState.manufacturer = [[NSString alloc]initWithFormat:@"Index%d", index];
        testState.bytenumber = 0;
        testState.int16number = 0;
        testState.int32number = 0;
        testState.int64number = 0;
        testState.floatnumber = 0;
        testState.somebool = NO;
        testState.UUID = [[UUID alloc]init];
        testState.UUID.upper = 0;
        testState.UUID.lower = 0;
        testState.numbersequence = @[[[NSNumber alloc]initWithInt:index]];
        testState.stringsequence = @[@"q", @"w", @"e", @"r"];
        testState.uuidsequence = @[[[UUID alloc]init], [[UUID alloc]init], [[UUID alloc]init]];
        ((UUID *)testState.uuidsequence[0]).upper = 0;
        ((UUID *)testState.uuidsequence[0]).lower = 0;
        ((UUID *)testState.uuidsequence[1]).upper = 0;
        ((UUID *)testState.uuidsequence[1]).lower = 0;
        ((UUID *)testState.uuidsequence[2]).upper = 0;
        ((UUID *)testState.uuidsequence[2]).lower = 0;
        result = [self write:testState withError:&error];
        XCTAssertTrue(result, @"writing should have succeeded");
        XCTAssertNotNil(error, @"the error code should not be nil");
        XCTAssertEqualObjects([error domain], @"org.qeo", @"the domain should be correct");
        XCTAssertTrue([error code] == 0, @"the error code should not be 0 because the write succeeded");
    }
    sleep(1);
    // check if all states have been received (in the right order)
    XCTAssertTrue([self.writtenStates count] == nbOfStates, @"%d states should be written", nbOfStates);
    XCTAssertTrue([self.receivedStates count] == nbOfStates, @"%d states should be received", nbOfStates);
    XCTAssertTrue(self.updatesReceived > 0 && self.updatesReceived <= nbOfStates, @"One update should have been received");
    XCTAssertTrue(self.burstsReceived > 0 && self.burstsReceived <= nbOfStates, @"One burst event should have been received");
    for (int index = 0; index < nbOfStates; index++) {
        XCTAssertTrue([self isType:self.writtenStates[index] equalTo:self.receivedStates[index]],
                      @"The states should be the same");
    }
    self.updatesReceived = 0;
    self.burstsReceived = 0;
    
    // Remove many states at once
    for (int index = 0; index < nbOfStates; index++) {
        testState = [[TestState alloc]init];
        testState.deviceId = [[DeviceId alloc]init];
        testState.deviceId.upper = index;
        testState.deviceId.lower = index;
        result = [self remove:testState withError:&error];
        XCTAssertTrue(result, @"writing should have succeeded");
        XCTAssertNotNil(error, @"the error code should not be nil");
        XCTAssertEqualObjects([error domain], @"org.qeo", @"the domain should be correct");
        XCTAssertTrue([error code] == 0, @"the error code should not be 0 because the write succeeded");
        XCTAssertTrue([self.writtenStates count] == (nbOfStates - (index + 1)), @"There should be one elemtn in there");
    }
    sleep(1);
    // check if all states have been received (in the right order)
    XCTAssertTrue([self.writtenStates count] == 0, @"%d states should be written", nbOfStates);
    XCTAssertTrue([self.receivedStates count] == 0, @"%d states should be received", nbOfStates);
    XCTAssertTrue(self.updatesReceived > 0 && self.updatesReceived <= nbOfStates, @"One update should have been received");
    XCTAssertTrue(self.burstsReceived > 0 && self.burstsReceived <= nbOfStates, @"One burst event should have been received");
    self.updatesReceived = 0;
    self.burstsReceived = 0;
    
}

- (void)testFastIterators
{
    BOOL result = NO;
    NSError *error;
    TestState *testState;
    int index = 0;
    int nbOfStates = 100;
    
    // Check with reader iterators if states can be retrieved correctly
    for (DeviceInfo *devInfo in self.stateReader) {
        XCTFail(@"This branch should never be hit, because there are no objects written yet: %@",devInfo.userFriendlyName);
    }
    
    // publish many states at once
    for (int index = 0; index < nbOfStates; index++) {
        testState = [[TestState alloc]init];
        testState.deviceId = [[DeviceId alloc]init];
        testState.deviceId.upper = index;
        testState.deviceId.lower = index;
        testState.manufacturer = [[NSString alloc]initWithFormat:@"index%d", index];
        testState.bytenumber = index;
        testState.int16number = index;
        testState.int32number = index;
        testState.int64number = index;
        testState.floatnumber = index;
        testState.somebool = YES;
        testState.UUID = [[UUID alloc]init];
        testState.UUID.upper = index;
        testState.UUID.lower = index;
        testState.numbersequence = @[[[NSNumber alloc]initWithInt:index]];
        testState.stringsequence = @[@"a", @"b", @"c", @"d"];
        testState.uuidsequence = @[[[UUID alloc]init], [[UUID alloc]init], [[UUID alloc]init]];
        ((UUID *)testState.uuidsequence[0]).upper = index;
        ((UUID *)testState.uuidsequence[0]).lower = index;
        ((UUID *)testState.uuidsequence[1]).upper = index;
        ((UUID *)testState.uuidsequence[1]).lower = index;
        ((UUID *)testState.uuidsequence[2]).upper = index;
        ((UUID *)testState.uuidsequence[2]).lower = index;
        result = [self write:testState withError:&error];
        XCTAssertTrue(result, @"writing should have succeeded");
        XCTAssertNotNil(error, @"the error code should not be nil");
        XCTAssertEqualObjects([error domain], @"org.qeo", @"the domain should be correct");
        XCTAssertTrue([error code] == 0, @"the error code should not be 0 because the write succeeded");
        XCTAssertTrue([self.writtenStates count] == index + 1, @"There should be one elemtn in there");
    }
    sleep(1);
    
    XCTAssertTrue([self.writtenStates count] == nbOfStates, @"%d states should be written", nbOfStates);
    XCTAssertTrue([self.receivedStates count] == nbOfStates, @"%d states should be received", nbOfStates);
    XCTAssertTrue(self.updatesReceived > 0 && self.updatesReceived <= nbOfStates, @"One update should have been received");
    XCTAssertTrue(self.burstsReceived > 0 && self.burstsReceived <= nbOfStates, @"One burst event should have been received");

    // Check with reader iterators if states can be retrieved correctly
    index = 0;
    for (TestState *ts in self.stateReader) {
        XCTAssertNotNil(ts, @"The received QeoType should not be nil");
        XCTAssertTrue([ts isKindOfClass:[TestState class]], @"The returned object should be of class DeviceInfo here");
        XCTAssertTrue([self isType:ts equalTo:self.writtenStates[index]] == YES, @"The received objects in the iterator \
                      should be the same as the ones that are written (and in the same order even)");
        index++;
        XCTAssertTrue(index <= nbOfStates , @"");
    }
    XCTAssertTrue(index == nbOfStates, @"There should be 100 elements in the list");
    
}

- (void)testBlockIterator
{
    BOOL result = NO;
    NSError *error;
    TestState *testState;
    int nbOfStates = 100;
    int __block index = 0;
    
    // publish many states at once
    // publish many states at once
    for (int index = 0; index < nbOfStates; index++) {
        testState = [[TestState alloc]init];
        testState.deviceId = [[DeviceId alloc]init];
        testState.deviceId.upper = index;
        testState.deviceId.lower = index;
        testState.manufacturer = [[NSString alloc]initWithFormat:@"index%d", index];
        testState.bytenumber = index;
        testState.int16number = index;
        testState.int32number = index;
        testState.int64number = index;
        testState.floatnumber = index;
        testState.somebool = YES;
        testState.UUID = [[UUID alloc]init];
        testState.UUID.upper = index;
        testState.UUID.lower = index;
        testState.numbersequence = @[[[NSNumber alloc]initWithInt:index]];
        testState.stringsequence = @[@"a", @"b", @"c", @"d"];
        testState.uuidsequence = @[[[UUID alloc]init], [[UUID alloc]init], [[UUID alloc]init]];
        ((UUID *)testState.uuidsequence[0]).upper = index;
        ((UUID *)testState.uuidsequence[0]).lower = index;
        ((UUID *)testState.uuidsequence[1]).upper = index;
        ((UUID *)testState.uuidsequence[1]).lower = index;
        ((UUID *)testState.uuidsequence[2]).upper = index;
        ((UUID *)testState.uuidsequence[2]).lower = index;
        result = [self write:testState withError:&error];
        XCTAssertTrue(result, @"writing should have succeeded");
        XCTAssertNotNil(error, @"the error code should not be nil");
        XCTAssertEqualObjects([error domain], @"org.qeo", @"the domain should be correct");
        XCTAssertTrue([error code] == 0, @"the error code should not be 0 because the write succeeded");
        XCTAssertTrue([self.writtenStates count] == index + 1, @"There should be one elemtn in there");
    }
    sleep(1);
    XCTAssertTrue([self.writtenStates count] == nbOfStates, @"%d states should be written", nbOfStates);
    XCTAssertTrue([self.receivedStates count] == nbOfStates, @"%d states should be received", nbOfStates);
    XCTAssertTrue(self.updatesReceived > 0 && self.updatesReceived <= nbOfStates, @"One update should have been received");
    XCTAssertTrue(self.burstsReceived > 0 && self.burstsReceived <= nbOfStates, @"One burst event should have been received");
    
    // All objects have been received, now start iterating
    index = 0;
    [self.stateReader enumerateInstancesUsingBlock:^(QEOType *ts, BOOL *cont) {
        // Continue till the end
        XCTAssertNotNil(ts, @"the returned devInfo should not be nil");
        XCTAssertTrue(cont != nil, @"the returned continue pointer should not be nil");
        XCTAssertTrue(*cont, @"The default return value should be YES");
        *cont = YES;
        
        XCTAssertTrue([ts isKindOfClass:[testState class]], @"The class should be of type DeviceInfo");
        XCTAssertTrue([self isType:(TestState *)ts equalTo:self.writtenStates[index]], @"The written objects should be the \
                      same and be in the same order");
        XCTAssertTrue(index < nbOfStates, @"There should never be more then 100 states here");
        
        index++;
    }];
    
    XCTAssertTrue(index == 100, @"exactly 100 elements should have been received");
    
    // All objects have been received, now start iterating, but now only for the first 50 ones
    index = 0;
    [self.stateReader enumerateInstancesUsingBlock:^(QEOType *ts, BOOL *cont) {
        // Continue till the end
        XCTAssertNotNil(ts, @"the returned devInfo should not be nil");
        XCTAssertTrue(cont != nil, @"the returned continue pointer should not be nil");
        XCTAssertTrue(*cont, @"The default return value should be YES");
        if (index < 49) {
            *cont = YES;
        } else {
            *cont = NO;
        }
        
        XCTAssertTrue([ts isKindOfClass:[testState class]], @"The class should be of type DeviceInfo");
        XCTAssertTrue([self isType:(TestState *)ts equalTo:self.writtenStates[index]], @"The written objects should be the \
                      same and be in the same order");
        XCTAssertTrue(index < nbOfStates, @"There should never be more then 100 states here");
        
        index++;
    }];
    
    XCTAssertTrue(index == 50, @"exactly 50 elements should have been received");
    
    // All objects have been received, now start iterating
    index = 0;
    [self.stateReader enumerateInstancesUsingBlock:^(QEOType *ts, BOOL *cont) {
        // Continue till the end
        XCTAssertNotNil(ts, @"the returned devInfo should not be nil");
        XCTAssertTrue(cont != nil, @"the returned continue pointer should not be nil");
        XCTAssertTrue(*cont, @"The default return value should be YES");
        *cont = YES;
        
        XCTAssertTrue([ts isKindOfClass:[testState class]], @"The class should be of type DeviceInfo");
        // should always be 0 because we have removed all previous ones
        XCTAssertTrue([self isType:(TestState *)ts equalTo:self.writtenStates[0]] == YES, @"The written objects should be the \
                      same and be in the same order");
        XCTAssertTrue(index < nbOfStates, @"There should never be more then 100 states here");
        
        // Now dispose the object
        XCTAssertTrue([self remove:ts withError:nil], @"This removal should have succeeded");
        
        index++;
    }];
    
    sleep(1);
    
    XCTAssertTrue([self.writtenStates count] == 0, @"All states should have been removed");
    XCTAssertTrue([self.receivedStates count] == 0, @"All states should have been removed");

}

@end
