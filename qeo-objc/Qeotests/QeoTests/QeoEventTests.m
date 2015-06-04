#import <XCTest/XCTest.h>
#import <Qeo/Qeo.h>

#import "QDMCombination.h"
#import "QeoCredentialsHandler.h"

@interface QeoEventTests : XCTestCase <QEOEntityDelegate, QEOEventReaderDelegate>
@property (strong, nonatomic) QEOFactory *factory;

@property (strong, nonatomic) QEOEventReader *reader;
// Array that will contain all received events, set this to nil to clean up all stuff
@property (strong, nonatomic) NSMutableArray *eventArray;
@property (assign, nonatomic) NSUInteger receivedEndBurstEvents;

@property (strong, nonatomic) QEOEventWriter *writer;

@end

@implementation QeoEventTests

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
    self.reader = [[QEOEventReader alloc]initWithType:[ChatMessage class]
                                              factory:self.factory
                                             delegate:self
                                       entityDelegate:self
                                                error:nil];
    self.eventArray = [[NSMutableArray alloc]init];
    self.receivedEndBurstEvents = 0;
    self.writer = [[QEOEventWriter alloc]initWithType:[ChatMessage class]
                                              factory:self.factory
                                       entityDelegate:self
                                                error:nil];
}

- (void)tearDown
{
    // Put teardown code here; it will be run once, after the last test case.
    self.eventArray = nil;
    self.receivedEndBurstEvents = 0;
    self.reader = nil;
    self.writer = nil;
    self.factory = nil;
    
    [super tearDown];
}

// Delegate method for allowing access to a policy identity, the entity is a reader/writer
- (BOOL)allowAccessForEntity:(QEOEntity *)entity
                    identity:(QEOPolicyIdentity *)identity
{
    // TODO implement
    //XCTFail(@"Not yet implemented");
    
    return YES;
}

// Delegate method that will be called for every event received on an event reader
- (void)didReceiveEvent:(ChatMessage *)event
              forReader:(QEOEventReader *)eventReader
{
    XCTAssertEqualObjects(eventReader, self.reader, @"The reader here should be the same as one created by the test initialisation");
    // Add the received event in te received event array
    [self.eventArray addObject:event];
    
}

// Could be used as trigger for the UI to refresh the event info when there is a burst of events received
- (void)didFinishBurstForEventReader:(QEOEventReader *)reader
{
    self.receivedEndBurstEvents++;
}


- (void)testEventFunctionality
{
    NSError *error;
    UUID *uuid;
    ChatMessage *chatMessage;
    ChatMessage *receivedChatMessage;
    BOOL success = NO;
    
    XCTAssertNotNil(self.factory, @"Initialisation should have succeeded properly");
    XCTAssertNotNil(self.reader, @"Initialisation should have succeeded properly");
    XCTAssertNotNil(self.writer, @"Initialisation should have succeeded properly");
    XCTAssertNotNil(self.eventArray, @"Initialisation should have succeeded properly");
    
    uuid = [[UUID alloc]init];

    // Start writing bad stuff to the writer
    success = [self.writer write:uuid
                       withError:&error];
    XCTAssertTrue(success == NO, "Writing should have failed here");
    XCTAssertNotNil(error, @"Error code should not be nil here");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Domain should be Qeo");
    XCTAssertTrue([error code] != 0, @"write statement should have failed because of incorrect QeoType");
    
    // check that nothing has been received by the reader
    XCTAssertTrue([self.eventArray count] == 0, @"The array should be empty here");
    XCTAssertTrue(self.receivedEndBurstEvents == 0, @"The number of received burst events should be 0");
    
    // write nil object
    success = [self.writer write:nil withError:&error];
    XCTAssertTrue(success == NO, "Writing should have failed here");
    XCTAssertNotNil(error, @"Error code should not be nil here");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Domain should be Qeo");
    XCTAssertTrue([error code] != 0, @"write statement should have failed because of incorrect QeoType");
    
    // check that nothing has been received by the reader
    XCTAssertTrue([self.eventArray count] == 0, @"The array should be empty here");
    XCTAssertTrue(self.receivedEndBurstEvents == 0, @"The number of received burst events should be 0");

    chatMessage = [[ChatMessage alloc]init];
    chatMessage.from = @"Sammy Tanghe";
    chatMessage.message = @"hello world";
    chatMessage.bytenumber = 255;
    chatMessage.int16number = INT16_MAX;
    chatMessage.int32number = INT32_MAX;
    chatMessage.int64number = INT64_MAX;
    chatMessage.floatnumber = 1.0/9.0;
    chatMessage.somebool = YES;
    chatMessage.UUID = [[UUID alloc]init];
    chatMessage.UUID.upper = 90210;
    chatMessage.UUID.lower = 797204;
    chatMessage.numbersequence = @[@0, @1, @2];
    chatMessage.stringsequence = @[@"a", @"b", @"c"];
    chatMessage.uuidsequence = @[[[UUID alloc]init], [[UUID alloc]init]];
    ((UUID*)chatMessage.uuidsequence[0]).upper = 0;
    ((UUID*)chatMessage.uuidsequence[0]).lower = 0;
    ((UUID*)chatMessage.uuidsequence[1]).upper = 1;
    ((UUID*)chatMessage.uuidsequence[1]).lower = 1;
    chatMessage.myEnum = ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_BUSY;
    success = [self.writer write:chatMessage withError:&error];
    XCTAssertTrue(success == YES, "Writing should have succeeded here");
    XCTAssertNotNil(error, @"Error code should not nil here");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Domain should be Qeo");
    XCTAssertTrue([error code] == 0, @"write statement should have failed succeeded");
    
    // check that object has been received by the reader
    sleep(1);
    XCTAssertTrue([self.eventArray count] == 1, @"The array should not be empty here");
    //XCTAssertEqualObjects(chatMessage, self.eventArray[0], @"The received object should be the same as the sent object");
    // Check manually for now
    receivedChatMessage = [self.eventArray count] ? (ChatMessage *) self.eventArray[0] : nil;
    XCTAssertNotNil(receivedChatMessage, @"We should have received a chat message");
    XCTAssertEqualObjects(chatMessage.from, receivedChatMessage.from, @"The from should be the same");
    XCTAssertEqualObjects(chatMessage.message, receivedChatMessage.message, @"The message should be the same");
    XCTAssertTrue(chatMessage.bytenumber == receivedChatMessage.bytenumber, @"The byte number should be the same");
    XCTAssertTrue(chatMessage.int16number == receivedChatMessage.int16number, @"The int16number should be the same");
    XCTAssertTrue(chatMessage.int32number == receivedChatMessage.int32number, @"The int32number should be the same");
    XCTAssertTrue(chatMessage.int64number == receivedChatMessage.int64number, @"The int64number should be the same");
    XCTAssertTrue(chatMessage.floatnumber == receivedChatMessage.floatnumber, @"The floatnumber should be the same");
    XCTAssertTrue(chatMessage.somebool == receivedChatMessage.somebool, @"The somebool should be the same");
    XCTAssertEqualObjects(chatMessage.UUID, receivedChatMessage.UUID, @"The UUID.upper should be the same");
    XCTAssertEqualObjects(chatMessage.numbersequence, receivedChatMessage.numbersequence, @"the numbersequence should be the same");
    XCTAssertEqualObjects(chatMessage.stringsequence, receivedChatMessage.stringsequence, @"the numbersequence should be the same");
    XCTAssertEqualObjects(chatMessage.uuidsequence, receivedChatMessage.uuidsequence, @"the uuidsequence should be the same");
    XCTAssertEqual(chatMessage.myEnum, receivedChatMessage.myEnum, @"enum should be equal");
    XCTAssertTrue(self.receivedEndBurstEvents == 1, @"The number of received burst events should not be 0");
    XCTAssertEqualObjects(chatMessage, receivedChatMessage, @"The chatmessage should be the same");
    // Clear all elements from the list
    [self.eventArray removeAllObjects];
    self.receivedEndBurstEvents = 0;
    
    chatMessage = [[ChatMessage alloc]init];
    chatMessage.from = nil;
    chatMessage.message = @"";
    chatMessage.bytenumber = 255;
    chatMessage.int16number = INT16_MAX;
    chatMessage.int32number = INT32_MAX;
    chatMessage.int64number = INT64_MAX;
    chatMessage.floatnumber = 1.0/9.0;
    chatMessage.somebool = YES;
    chatMessage.UUID = [[UUID alloc]init];
    chatMessage.UUID.upper = 90210;
    chatMessage.UUID.lower = 797204;
    chatMessage.numbersequence = @[@0, @1, @2];
    chatMessage.stringsequence = @[@"a", @"b", @"c"];
    chatMessage.uuidsequence = @[[[UUID alloc]init], [[UUID alloc]init]];
    ((UUID*)chatMessage.uuidsequence[0]).upper = 0;
    ((UUID*)chatMessage.uuidsequence[0]).lower = 0;
    ((UUID*)chatMessage.uuidsequence[1]).upper = 1;
    ((UUID*)chatMessage.uuidsequence[1]).lower = 1;
    chatMessage.myEnum = ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_AWAY;
    success = [self.writer write:chatMessage withError:&error];
    XCTAssertTrue(success == YES, "Writing should have succeeded here");
    XCTAssertNotNil(error, @"Error code should not nil here");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Domain should be Qeo");
    XCTAssertTrue([error code] == 0, @"write statement should have failed succeeded");
    
    // check that object has been received by the reader
    sleep(1);
    XCTAssertTrue([self.eventArray count] == 1, @"The array should not be empty here");
    //XCTAssertEqualObjects(chatMessage, self.eventArray[0], @"The received object should be the same as the sent object");
    // Check manually for now
    receivedChatMessage = [self.eventArray count] ? (ChatMessage *) self.eventArray[0] : nil;
    XCTAssertNotNil(receivedChatMessage, @"We should have received a chat message");
    XCTAssertEqualObjects(@"", receivedChatMessage.from, @"The from should be the same");
    XCTAssertEqualObjects(chatMessage.message, receivedChatMessage.message, @"The message should be the same");
    XCTAssertTrue(chatMessage.bytenumber == receivedChatMessage.bytenumber, @"The byte number should be the same");
    XCTAssertTrue(chatMessage.int16number == receivedChatMessage.int16number, @"The int16number should be the same");
    XCTAssertTrue(chatMessage.int32number == receivedChatMessage.int32number, @"The int32number should be the same");
    XCTAssertTrue(chatMessage.int64number == receivedChatMessage.int64number, @"The int64number should be the same");
    XCTAssertTrue(chatMessage.floatnumber == receivedChatMessage.floatnumber, @"The floatnumber should be the same");
    XCTAssertTrue(chatMessage.somebool == receivedChatMessage.somebool, @"The somebool should be the same");
    XCTAssertEqualObjects(chatMessage.UUID, receivedChatMessage.UUID, @"The UUID.upper should be the same");
    XCTAssertEqualObjects(chatMessage.numbersequence, receivedChatMessage.numbersequence, @"the numbersequence should be the same");
    XCTAssertEqualObjects(chatMessage.stringsequence, receivedChatMessage.stringsequence, @"the numbersequence should be the same");
    XCTAssertEqualObjects(chatMessage.uuidsequence, receivedChatMessage.uuidsequence, @"the uuidsequence should be the same");
    XCTAssertEqual(chatMessage.myEnum, receivedChatMessage.myEnum, @"enum should be equal");

    XCTAssertTrue(self.receivedEndBurstEvents == 1, @"The number of received burst events should not be 0");
    // Clear all elements from the list
    [self.eventArray removeAllObjects];
    self.receivedEndBurstEvents = 0;

    chatMessage = [[ChatMessage alloc]init];
    chatMessage.from = @"Sammy Tanghe";
    chatMessage.message = @"hello world";
    chatMessage.bytenumber = 0;
    chatMessage.int16number = -INT16_MAX;
    chatMessage.int32number = -INT32_MAX;
    chatMessage.int64number = -INT64_MAX;
    chatMessage.floatnumber = -1.0/9.0;
    chatMessage.somebool = NO;
    chatMessage.UUID = [[UUID alloc]init];
    chatMessage.UUID.upper = 90210;
    chatMessage.UUID.lower = 797204;
    chatMessage.numbersequence = @[@0, @1, @2];
    chatMessage.stringsequence = @[@"a", @"b", @"c"];
    chatMessage.uuidsequence = @[[[UUID alloc]init], [[UUID alloc]init]];
    ((UUID*)chatMessage.uuidsequence[0]).upper = 0;
    ((UUID*)chatMessage.uuidsequence[0]).lower = 0;
    ((UUID*)chatMessage.uuidsequence[1]).upper = 1;
    ((UUID*)chatMessage.uuidsequence[1]).lower = 1;
    chatMessage.myEnum = ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_IDLE;
    success = [self.writer write:chatMessage withError:&error];
    XCTAssertTrue(success == YES, "Writing should have succeeded here");
    XCTAssertNotNil(error, @"Error code should not nil here");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Domain should be Qeo");
    XCTAssertTrue([error code] == 0, @"write statement should have failed succeeded");
    
    // check that object has been received by the reader
    sleep(1);
    XCTAssertTrue([self.eventArray count] == 1, @"The array should not be empty here");
    //XCTAssertEqualObjects(chatMessage, self.eventArray[0], @"The received object should be the same as the sent object");
    // Check manually for now
    receivedChatMessage = [self.eventArray count] ? (ChatMessage *) self.eventArray[0] : nil;
    XCTAssertNotNil(receivedChatMessage, @"We should have received a chat message");
    XCTAssertEqualObjects(chatMessage.from, receivedChatMessage.from, @"The from should be the same");
    XCTAssertEqualObjects(chatMessage.message, receivedChatMessage.message, @"The message should be the same");
    XCTAssertTrue(chatMessage.bytenumber == receivedChatMessage.bytenumber, @"The byte number should be the same");
    XCTAssertTrue(chatMessage.int16number == receivedChatMessage.int16number, @"The int16number should be the same");
    XCTAssertTrue(chatMessage.int32number == receivedChatMessage.int32number, @"The int32number should be the same");
    XCTAssertTrue(chatMessage.int64number == receivedChatMessage.int64number, @"The int64number should be the same");
    XCTAssertTrue(chatMessage.floatnumber == receivedChatMessage.floatnumber, @"The floatnumber should be the same");
    XCTAssertTrue(chatMessage.somebool == receivedChatMessage.somebool, @"The somebool should be the same");
    XCTAssertEqualObjects(chatMessage.UUID, receivedChatMessage.UUID, @"The UUID.upper should be the same");
    XCTAssertEqualObjects(chatMessage.numbersequence, receivedChatMessage.numbersequence, @"the numbersequence should be the same");
    XCTAssertEqualObjects(chatMessage.stringsequence, receivedChatMessage.stringsequence, @"the numbersequence should be the same");
    XCTAssertEqualObjects(chatMessage.uuidsequence, receivedChatMessage.uuidsequence, @"the uuidsequence should be the same");
    XCTAssertEqualObjects(chatMessage, receivedChatMessage, @"The chatmessage should be the same");
    XCTAssertEqual(chatMessage.myEnum, receivedChatMessage.myEnum, @"enum should be equal");

    XCTAssertTrue(self.receivedEndBurstEvents == 1, @"The number of received burst events should not be 0");
    // Clear all elements from the list
    [self.eventArray removeAllObjects];
    self.receivedEndBurstEvents = 0;
    
    chatMessage = [[ChatMessage alloc]init];
    chatMessage.from = @"Sammy Tanghe";
    chatMessage.message = @"hello world";
    chatMessage.bytenumber = 255;
    chatMessage.int16number = INT16_MAX;
    chatMessage.int32number = INT32_MAX;
    chatMessage.int64number = INT64_MAX;
    chatMessage.floatnumber = 1.0/9.0;
    chatMessage.somebool = YES;
    chatMessage.UUID = nil;
    chatMessage.numbersequence = @[@0, @1, @2];
    chatMessage.stringsequence = @[@"a", @"b", @"c"];
    chatMessage.uuidsequence = @[[[UUID alloc]init], [[UUID alloc]init]];
    ((UUID*)chatMessage.uuidsequence[0]).upper = 0;
    ((UUID*)chatMessage.uuidsequence[0]).lower = 0;
    ((UUID*)chatMessage.uuidsequence[1]).upper = 1;
    ((UUID*)chatMessage.uuidsequence[1]).lower = 1;
    chatMessage.myEnum = ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_IDLE;

    success = [self.writer write:chatMessage withError:&error];
    XCTAssertTrue(success == YES, "Writing should have succeeded here");
    XCTAssertNotNil(error, @"Error code should not nil here");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Domain should be Qeo");
    XCTAssertTrue([error code] == 0, @"write statement should have failed succeeded");
    
    // check that object has been received by the reader
    sleep(1);
    XCTAssertTrue([self.eventArray count] == 1, @"The array should not be empty here");
    //XCTAssertEqualObjects(chatMessage, self.eventArray[0], @"The received object should be the same as the sent object");
    // Check manually for now
    receivedChatMessage = [self.eventArray count] ? (ChatMessage *) self.eventArray[0] : nil;
    XCTAssertNotNil(receivedChatMessage, @"We should have received a chat message");
    XCTAssertEqualObjects(chatMessage.from, receivedChatMessage.from, @"The from should be the same");
    XCTAssertEqualObjects(chatMessage.message, receivedChatMessage.message, @"The message should be the same");
    XCTAssertTrue(chatMessage.bytenumber == receivedChatMessage.bytenumber, @"The byte number should be the same");
    XCTAssertTrue(chatMessage.int16number == receivedChatMessage.int16number, @"The int16number should be the same");
    XCTAssertTrue(chatMessage.int32number == receivedChatMessage.int32number, @"The int32number should be the same");
    XCTAssertTrue(chatMessage.int64number == receivedChatMessage.int64number, @"The int64number should be the same");
    XCTAssertTrue(chatMessage.floatnumber == receivedChatMessage.floatnumber, @"The floatnumber should be the same");
    XCTAssertTrue(chatMessage.somebool == receivedChatMessage.somebool, @"The somebool should be the same");
    XCTAssertEqualObjects(receivedChatMessage.UUID, [[UUID alloc]init], @"The UUID.upper should be the same");
    XCTAssertEqualObjects(chatMessage.numbersequence, receivedChatMessage.numbersequence, @"the numbersequence should be the same");
    XCTAssertEqualObjects(chatMessage.stringsequence, receivedChatMessage.stringsequence, @"the numbersequence should be the same");
    XCTAssertEqualObjects(chatMessage.uuidsequence, receivedChatMessage.uuidsequence, @"the uuidsequence should be the same");
    XCTAssertEqual(chatMessage.myEnum, receivedChatMessage.myEnum, @"enum should be equal");

    XCTAssertTrue(self.receivedEndBurstEvents == 1, @"The number of received burst events should not be 0");
    // Clear all elements from the list
    [self.eventArray removeAllObjects];
    self.receivedEndBurstEvents = 0;
    
    chatMessage = [[ChatMessage alloc]init];
    chatMessage.from = @"Sammy Tanghe";
    chatMessage.message = @"hello world";
    chatMessage.bytenumber = 255;
    chatMessage.int16number = INT16_MAX;
    chatMessage.int32number = INT32_MAX;
    chatMessage.int64number = INT64_MAX;
    chatMessage.floatnumber = 1.0/9.0;
    chatMessage.somebool = YES;
    chatMessage.UUID = [[UUID alloc]init];
    chatMessage.UUID.upper = 90210;
    chatMessage.UUID.lower = 797204;
    chatMessage.numbersequence = nil;
    chatMessage.stringsequence = @[@"a", @"b", @"c"];
    chatMessage.uuidsequence = @[[[UUID alloc]init], [[UUID alloc]init]];
    ((UUID*)chatMessage.uuidsequence[0]).upper = 0;
    ((UUID*)chatMessage.uuidsequence[0]).lower = 0;
    ((UUID*)chatMessage.uuidsequence[1]).upper = 1;
    ((UUID*)chatMessage.uuidsequence[1]).lower = 1;
    chatMessage.myEnum = ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_IDLE;

    success = [self.writer write:chatMessage withError:&error];
    XCTAssertTrue(success == YES, "Writing should have succeeded here");
    XCTAssertNotNil(error, @"Error code should not nil here");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Domain should be Qeo");
    XCTAssertTrue([error code] == 0, @"write statement should have failed succeeded");
    
    // check that object has been received by the reader
    sleep(1);
    XCTAssertTrue([self.eventArray count] == 1, @"The array should not be empty here");
    //XCTAssertEqualObjects(chatMessage, self.eventArray[0], @"The received object should be the same as the sent object");
    // Check manually for now
    receivedChatMessage = [self.eventArray count] ? (ChatMessage *) self.eventArray[0] : nil;
    XCTAssertNotNil(receivedChatMessage, @"We should have received a chat message");
    XCTAssertEqualObjects(chatMessage.from, receivedChatMessage.from, @"The from should be the same");
    XCTAssertEqualObjects(chatMessage.message, receivedChatMessage.message, @"The message should be the same");
    XCTAssertTrue(chatMessage.bytenumber == receivedChatMessage.bytenumber, @"The byte number should be the same");
    XCTAssertTrue(chatMessage.int16number == receivedChatMessage.int16number, @"The int16number should be the same");
    XCTAssertTrue(chatMessage.int32number == receivedChatMessage.int32number, @"The int32number should be the same");
    XCTAssertTrue(chatMessage.int64number == receivedChatMessage.int64number, @"The int64number should be the same");
    XCTAssertTrue(chatMessage.floatnumber == receivedChatMessage.floatnumber, @"The floatnumber should be the same");
    XCTAssertTrue(chatMessage.somebool == receivedChatMessage.somebool, @"The somebool should be the same");
    XCTAssertEqualObjects(chatMessage.UUID, receivedChatMessage.UUID, @"The UUID.upper should be the same");
    XCTAssertEqualObjects(@[], receivedChatMessage.numbersequence, @"the numbersequence should be the same");
    XCTAssertEqualObjects(chatMessage.stringsequence, receivedChatMessage.stringsequence, @"the numbersequence should be the same");
    XCTAssertEqualObjects(chatMessage.uuidsequence, receivedChatMessage.uuidsequence, @"the uuidsequence should be the same");
    XCTAssertEqual(chatMessage.myEnum, receivedChatMessage.myEnum, @"enum should be equal");

    XCTAssertTrue(self.receivedEndBurstEvents == 1, @"The number of received burst events should not be 0");
    // Clear all elements from the list
    [self.eventArray removeAllObjects];
    self.receivedEndBurstEvents = 0;
    
    chatMessage = [[ChatMessage alloc]init];
    chatMessage.from = @"Sammy Tanghe";
    chatMessage.message = @"hello world";
    chatMessage.bytenumber = 255;
    chatMessage.int16number = INT16_MAX;
    chatMessage.int32number = INT32_MAX;
    chatMessage.int64number = INT64_MAX;
    chatMessage.floatnumber = 1.0/9.0;
    chatMessage.somebool = YES;
    chatMessage.UUID = [[UUID alloc]init];
    chatMessage.UUID.upper = 90210;
    chatMessage.UUID.lower = 797204;
    chatMessage.numbersequence = @[@0, @1, @2];
    chatMessage.stringsequence = nil;
    chatMessage.uuidsequence = @[[[UUID alloc]init], [[UUID alloc]init]];
    ((UUID*)chatMessage.uuidsequence[0]).upper = 0;
    ((UUID*)chatMessage.uuidsequence[0]).lower = 0;
    ((UUID*)chatMessage.uuidsequence[1]).upper = 1;
    ((UUID*)chatMessage.uuidsequence[1]).lower = 1;
    chatMessage.myEnum = ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_IDLE;

    success = [self.writer write:chatMessage withError:&error];
    XCTAssertTrue(success == YES, "Writing should have succeeded here");
    XCTAssertNotNil(error, @"Error code should not nil here");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Domain should be Qeo");
    XCTAssertTrue([error code] == 0, @"write statement should have failed succeeded");
    
    // check that object has been received by the reader
    sleep(1);
    XCTAssertTrue([self.eventArray count] == 1, @"The array should not be empty here");
    //XCTAssertEqualObjects(chatMessage, self.eventArray[0], @"The received object should be the same as the sent object");
    // Check manually for now
    receivedChatMessage = [self.eventArray count] ? (ChatMessage *) self.eventArray[0] : nil;
    XCTAssertNotNil(receivedChatMessage, @"We should have received a chat message");
    XCTAssertEqualObjects(chatMessage.from, receivedChatMessage.from, @"The from should be the same");
    XCTAssertEqualObjects(chatMessage.message, receivedChatMessage.message, @"The message should be the same");
    XCTAssertTrue(chatMessage.bytenumber == receivedChatMessage.bytenumber, @"The byte number should be the same");
    XCTAssertTrue(chatMessage.int16number == receivedChatMessage.int16number, @"The int16number should be the same");
    XCTAssertTrue(chatMessage.int32number == receivedChatMessage.int32number, @"The int32number should be the same");
    XCTAssertTrue(chatMessage.int64number == receivedChatMessage.int64number, @"The int64number should be the same");
    XCTAssertTrue(chatMessage.floatnumber == receivedChatMessage.floatnumber, @"The floatnumber should be the same");
    XCTAssertTrue(chatMessage.somebool == receivedChatMessage.somebool, @"The somebool should be the same");
    XCTAssertEqualObjects(chatMessage.UUID, receivedChatMessage.UUID, @"The UUID.upper should be the same");
    XCTAssertEqualObjects(chatMessage.numbersequence, receivedChatMessage.numbersequence, @"the numbersequence should be the same");
    XCTAssertEqualObjects(@[], receivedChatMessage.stringsequence, @"the numbersequence should be the same");
    XCTAssertEqualObjects(chatMessage.uuidsequence, receivedChatMessage.uuidsequence, @"the uuidsequence should be the same");
    XCTAssertEqual(chatMessage.myEnum, receivedChatMessage.myEnum, @"enum should be equal");

    XCTAssertTrue(self.receivedEndBurstEvents == 1, @"The number of received burst events should not be 0");
    // Clear all elements from the list
    [self.eventArray removeAllObjects];
    self.receivedEndBurstEvents = 0;
    
    chatMessage = [[ChatMessage alloc]init];
    chatMessage.from = @"Sammy Tanghe";
    chatMessage.message = @"hello world";
    chatMessage.bytenumber = 255;
    chatMessage.int16number = INT16_MAX;
    chatMessage.int32number = INT32_MAX;
    chatMessage.int64number = INT64_MAX;
    chatMessage.floatnumber = 1.0/9.0;
    chatMessage.somebool = YES;
    chatMessage.UUID = [[UUID alloc]init];
    chatMessage.UUID.upper = 90210;
    chatMessage.UUID.lower = 797204;
    chatMessage.numbersequence = @[@0, @1, @2];
    chatMessage.stringsequence = @[@"a", @"b", @"c"];
    chatMessage.uuidsequence = nil;
    chatMessage.myEnum = ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_IDLE;

    success = [self.writer write:chatMessage withError:&error];
    XCTAssertTrue(success == YES, "Writing should have succeeded here");
    XCTAssertNotNil(error, @"Error code should not nil here");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Domain should be Qeo");
    XCTAssertTrue([error code] == 0, @"write statement should have failed succeeded");
    
    // check that object has been received by the reader
    sleep(1);
    XCTAssertTrue([self.eventArray count] == 1, @"The array should not be empty here");
    //XCTAssertEqualObjects(chatMessage, self.eventArray[0], @"The received object should be the same as the sent object");
    // Check manually for now
    receivedChatMessage = [self.eventArray count] ? (ChatMessage *) self.eventArray[0] : nil;
    XCTAssertNotNil(receivedChatMessage, @"We should have received a chat message");
    XCTAssertEqualObjects(chatMessage.from, receivedChatMessage.from, @"The from should be the same");
    XCTAssertEqualObjects(chatMessage.message, receivedChatMessage.message, @"The message should be the same");
    XCTAssertTrue(chatMessage.bytenumber == receivedChatMessage.bytenumber, @"The byte number should be the same");
    XCTAssertTrue(chatMessage.int16number == receivedChatMessage.int16number, @"The int16number should be the same");
    XCTAssertTrue(chatMessage.int32number == receivedChatMessage.int32number, @"The int32number should be the same");
    XCTAssertTrue(chatMessage.int64number == receivedChatMessage.int64number, @"The int64number should be the same");
    XCTAssertTrue(chatMessage.floatnumber == receivedChatMessage.floatnumber, @"The floatnumber should be the same");
    XCTAssertTrue(chatMessage.somebool == receivedChatMessage.somebool, @"The somebool should be the same");
    XCTAssertEqualObjects(chatMessage.UUID, receivedChatMessage.UUID, @"The UUID.upper should be the same");
    XCTAssertEqualObjects(chatMessage.numbersequence, receivedChatMessage.numbersequence, @"the numbersequence should be the same");
    XCTAssertEqualObjects(chatMessage.stringsequence, receivedChatMessage.stringsequence, @"the numbersequence should be the same");
    XCTAssertTrue([receivedChatMessage.uuidsequence count] == 0, @"the uuidsequence should be the empty");
    XCTAssertEqual(chatMessage.myEnum, receivedChatMessage.myEnum, @"enum should be equal");

    XCTAssertTrue(self.receivedEndBurstEvents == 1, @"The number of received burst events should not be 0");
    // Clear all elements from the list
    [self.eventArray removeAllObjects];
    self.receivedEndBurstEvents = 0;

}

- (void)testBurstEventFunctionality
{
    ChatMessage *chatMessage;
    ChatMessage *receivedChatMessage;
    
    XCTAssertNotNil(self.factory, @"Initialisation should have succeeded properly");
    XCTAssertNotNil(self.reader, @"Initialisation should have succeeded properly");
    XCTAssertNotNil(self.writer, @"Initialisation should have succeeded properly");
    XCTAssertNotNil(self.eventArray, @"Initialisation should have succeeded properly");

    chatMessage = [[ChatMessage alloc]init];
    chatMessage.from = @"Sammy Tanghe";
    chatMessage.message = @"hello world";
    chatMessage.bytenumber = 255;
    chatMessage.int16number = INT16_MAX;
    chatMessage.int32number = INT32_MAX;
    chatMessage.int64number = INT64_MAX;
    chatMessage.floatnumber = 1.0/9.0;
    chatMessage.somebool = YES;
    chatMessage.UUID = [[UUID alloc]init];
    chatMessage.UUID.upper = 90210;
    chatMessage.UUID.lower = 797204;
    chatMessage.numbersequence = @[@0, @1, @2];
    chatMessage.stringsequence = @[@"a", @"b", @"c"];
    chatMessage.uuidsequence = @[[[UUID alloc]init], [[UUID alloc]init]];
    ((UUID*)chatMessage.uuidsequence[0]).upper = 0;
    ((UUID*)chatMessage.uuidsequence[0]).lower = 0;
    ((UUID*)chatMessage.uuidsequence[1]).upper = 1;
    ((UUID*)chatMessage.uuidsequence[1]).lower = 1;
    chatMessage.myEnum = ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_IDLE;

    for (int index = 0; index < 100; index++) {
        chatMessage.UUID.lower = index;
        [self.writer write:chatMessage withError:nil];
    }
    
    // Wait for all messages to arrive
    sleep(1);
    
    XCTAssertTrue([self.eventArray count] == 100, @"The array should not be empty here");
    for (int index = 0; index < 100; index++) {
        receivedChatMessage = [self.eventArray count] && [self.eventArray count] > index ? (ChatMessage *) self.eventArray[index] : nil;
        XCTAssertEqualObjects(chatMessage.from, receivedChatMessage.from, @"The from should be the same");
        XCTAssertEqualObjects(chatMessage.message, receivedChatMessage.message, @"The message should be the same");
        XCTAssertTrue(chatMessage.bytenumber == receivedChatMessage.bytenumber, @"The byte number should be the same");
        XCTAssertTrue(chatMessage.int16number == receivedChatMessage.int16number, @"The int16number should be the same");
        XCTAssertTrue(chatMessage.int32number == receivedChatMessage.int32number, @"The int32number should be the same");
        XCTAssertTrue(chatMessage.int64number == receivedChatMessage.int64number, @"The int64number should be the same");
        XCTAssertTrue(chatMessage.floatnumber == receivedChatMessage.floatnumber, @"The floatnumber should be the same");
        XCTAssertTrue(chatMessage.somebool == receivedChatMessage.somebool, @"The somebool should be the same");
        XCTAssertTrue(receivedChatMessage.UUID.lower == index, @"The UUID.upper should be the same");
        XCTAssertEqualObjects(chatMessage.numbersequence, receivedChatMessage.numbersequence, @"the numbersequence should be the same");
        XCTAssertEqualObjects(chatMessage.stringsequence, receivedChatMessage.stringsequence, @"the numbersequence should be the same");
        XCTAssertEqualObjects(chatMessage.uuidsequence, receivedChatMessage.uuidsequence, @"the uuidsequence should be teh same");
        XCTAssertEqual(chatMessage.myEnum, receivedChatMessage.myEnum, @"enum should be equal");

    }
    
    XCTAssertTrue(self.receivedEndBurstEvents > 0 && self.receivedEndBurstEvents <= 100,
                  @"The number of received burst events should be between 0 and 100 and is currently %luu",(unsigned long) (unsigned long)self.receivedEndBurstEvents);
}

- (void)testExceptionalTypeBehavior
{
    ChatMessage *chatMessage;
    ChatMessage *receivedChatMessage;
    NSMutableString *tempString;
    NSError *error;
    BOOL success = NO;
    
    XCTAssertNotNil(self.factory, @"Initialisation should have succeeded properly");
    XCTAssertNotNil(self.reader, @"Initialisation should have succeeded properly");
    XCTAssertNotNil(self.writer, @"Initialisation should have succeeded properly");
    XCTAssertNotNil(self.eventArray, @"Initialisation should have succeeded properly");
    
    // Long string
    chatMessage = [[ChatMessage alloc]init];
    chatMessage.from = @"Sammy Tanghe";
    chatMessage.message = @"hello world";
    chatMessage.bytenumber = 255;
    chatMessage.int16number = INT16_MAX;
    chatMessage.int32number = INT32_MAX;
    chatMessage.int64number = INT64_MAX;
    chatMessage.floatnumber = NAN;
    chatMessage.somebool = YES;
    chatMessage.UUID = [[UUID alloc]init];
    chatMessage.UUID.upper = 90210;
    chatMessage.UUID.lower = 797204;
    chatMessage.numbersequence = @[@-0, @-1, @-2];
    chatMessage.stringsequence = @[@"一個敏捷的棕毛狐狸躍過那隻懶狗", @"b", @"c"];
    chatMessage.uuidsequence = @[[[UUID alloc]init], [[UUID alloc]init]];
    ((UUID*)chatMessage.uuidsequence[0]).upper = 0;
    ((UUID*)chatMessage.uuidsequence[0]).lower = 0;
    ((UUID*)chatMessage.uuidsequence[1]).upper = 1;
    ((UUID*)chatMessage.uuidsequence[1]).lower = 1;
    chatMessage.myEnum = ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_IDLE;

    tempString = [[NSMutableString alloc]initWithCapacity:128*1024];
    for (int index = 0; index < 128*1024; index++) {
        [tempString appendString:@"a"];
    }
    chatMessage.message = tempString;
    success = [self.writer write:chatMessage withError:&error];
    XCTAssertTrue(success == YES, "Writing should have succeeded here");
    XCTAssertNotNil(error, @"Error code should not nil here");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Domain should be Qeo");
    XCTAssertTrue([error code] == 0, @"write statement should have failed succeeded");
    
    sleep(1);
    
    XCTAssertTrue([self.eventArray count] == 1, @"The array should not be empty here");
    //XCTAssertEqualObjects(chatMessage, self.eventArray[0], @"The received object should be the same as the sent object");
    // Check manually for now
    receivedChatMessage = [self.eventArray count] ? (ChatMessage *) self.eventArray[0] : nil;
    XCTAssertNotNil(receivedChatMessage, @"We should have received a chat message");
    XCTAssertEqualObjects(chatMessage.from, receivedChatMessage.from, @"The from should be the same");
    XCTAssertEqualObjects(chatMessage.message, receivedChatMessage.message, @"The message should be the same");
    XCTAssertTrue(chatMessage.bytenumber == receivedChatMessage.bytenumber, @"The byte number should be the same");
    XCTAssertTrue(chatMessage.int16number == receivedChatMessage.int16number, @"The int16number should be the same");
    XCTAssertTrue(chatMessage.int32number == receivedChatMessage.int32number, @"The int32number should be the same");
    XCTAssertTrue(chatMessage.int64number == receivedChatMessage.int64number, @"The int64number should be the same");
    XCTAssertTrue(isnan(receivedChatMessage.floatnumber) , @"The floatnumber should be the same");
    XCTAssertTrue(chatMessage.somebool == receivedChatMessage.somebool, @"The somebool should be the same");
    XCTAssertEqualObjects(chatMessage.UUID, receivedChatMessage.UUID, @"The UUID.upper should be the same");
    XCTAssertEqualObjects(chatMessage.numbersequence, receivedChatMessage.numbersequence, @"the numbersequence should be the same");
    XCTAssertEqualObjects(chatMessage.stringsequence, receivedChatMessage.stringsequence, @"the numbersequence should be the same");
    XCTAssertEqualObjects(chatMessage.uuidsequence, receivedChatMessage.uuidsequence, @"the uuidsequence should be teh same");
    XCTAssertEqual(chatMessage.myEnum, receivedChatMessage.myEnum, @"enum should be equal");
    XCTAssertTrue(self.receivedEndBurstEvents == 1, @"The number of received burst events should not be 0");
    
    // Clear all elements from the list
    [self.eventArray removeAllObjects];
    self.receivedEndBurstEvents = 0;

    // Chinese Characters
    chatMessage = [[ChatMessage alloc]init];
    chatMessage.from = @"一個敏捷的棕毛狐狸躍過那隻懶狗";
    chatMessage.message = @"一个敏捷的棕毛狐狸跃过那只懒狗";
    chatMessage.bytenumber = 255;
    chatMessage.int16number = INT16_MAX;
    chatMessage.int32number = INT32_MAX;
    chatMessage.int64number = INT64_MAX;
    chatMessage.floatnumber = NAN;
    chatMessage.somebool = YES;
    chatMessage.UUID = [[UUID alloc]init];
    chatMessage.UUID.upper = 90210;
    chatMessage.UUID.lower = 797204;
    chatMessage.numbersequence = @[@0, @1, @2];
    chatMessage.stringsequence = @[@"a", @"b", @"c"];
    chatMessage.uuidsequence = @[[[UUID alloc]init], [[UUID alloc]init]];
    ((UUID*)chatMessage.uuidsequence[0]).upper = 0;
    ((UUID*)chatMessage.uuidsequence[0]).lower = 0;
    ((UUID*)chatMessage.uuidsequence[1]).upper = 1;
    ((UUID*)chatMessage.uuidsequence[1]).lower = 1;
    chatMessage.myEnum = ORG_QEO_SAMPLE_SIMPLECHAT_CHATSTATE_IDLE;

    success = [self.writer write:chatMessage withError:&error];
    XCTAssertTrue(success == YES, "Writing should have succeeded here");
    XCTAssertNotNil(error, @"Error code should not nil here");
    XCTAssertEqualObjects([error domain], @"org.qeo", @"Domain should be Qeo");
    XCTAssertTrue([error code] == 0, @"write statement should have failed succeeded");
    
    // check that object has been received by the reader
    sleep(1);
    XCTAssertTrue([self.eventArray count] == 1, @"The array should not be empty here");
    //XCTAssertEqualObjects(chatMessage, self.eventArray[0], @"The received object should be the same as the sent object");
    // Check manually for now
    receivedChatMessage = [self.eventArray count] ? (ChatMessage *) self.eventArray[0] : nil;
    XCTAssertNotNil(receivedChatMessage, @"We should have received a chat message");
    XCTAssertEqualObjects(chatMessage.from, receivedChatMessage.from, @"The from should be the same");
    XCTAssertEqualObjects(chatMessage.message, receivedChatMessage.message, @"The message should be the same");
    XCTAssertTrue(chatMessage.bytenumber == receivedChatMessage.bytenumber, @"The byte number should be the same");
    XCTAssertTrue(chatMessage.int16number == receivedChatMessage.int16number, @"The int16number should be the same");
    XCTAssertTrue(chatMessage.int32number == receivedChatMessage.int32number, @"The int32number should be the same");
    XCTAssertTrue(chatMessage.int64number == receivedChatMessage.int64number, @"The int64number should be the same");
    XCTAssertTrue(isnan(receivedChatMessage.floatnumber), @"The floatnumber should be the same");
    XCTAssertTrue(chatMessage.somebool == receivedChatMessage.somebool, @"The somebool should be the same");
    XCTAssertEqualObjects(chatMessage.UUID, receivedChatMessage.UUID, @"The UUID.upper should be the same");
    XCTAssertEqualObjects(chatMessage.numbersequence, receivedChatMessage.numbersequence, @"the numbersequence should be the same");
    XCTAssertEqualObjects(chatMessage.stringsequence, receivedChatMessage.stringsequence, @"the numbersequence should be the same");
    XCTAssertEqualObjects(chatMessage.uuidsequence, receivedChatMessage.uuidsequence, @"the uuidsequence should be teh same");
    XCTAssertEqual(chatMessage.myEnum, receivedChatMessage.myEnum, @"enum should be equal");
    XCTAssertTrue(self.receivedEndBurstEvents == 1, @"The number of received burst events should not be 0");
    
    // Clear all elements from the list
    [self.eventArray removeAllObjects];
    self.receivedEndBurstEvents = 0;
}

@end
