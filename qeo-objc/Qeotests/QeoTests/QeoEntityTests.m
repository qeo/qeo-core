#import <XCTest/XCTest.h>
#import <Qeo/Qeo.h>

#import "QDMCombination.h"
#import "QeoCredentialsHandler.h"

@interface customDelegate1:NSObject<QEOEntityDelegate>
@end

@implementation customDelegate1
- (BOOL)allowAccessForEntity:(QEOEntity *)entity
                    identity:(QEOPolicyIdentity *)identity{
    if(![identity.userId isEqual :entity.factory.identity.userId] && [entity.qeoType isSubclassOfClass:[TestState class] ]){
        
         NSLog(@"customDelegate1-Not me %@",identity.userId);
        return YES;
    }
   else
   {
       NSLog(@" customDelegate1 -me %@",identity.userId);
       return  NO;
   }
}

@end

@interface customDelegate2:NSObject<QEOEntityDelegate>
@end

@implementation customDelegate2
- (BOOL)allowAccessForEntity:(QEOEntity *)entity
                    identity:(QEOPolicyIdentity *)identity{
    if([identity.userId isEqual :entity.factory.identity.userId] && [entity.qeoType isSubclassOfClass:[TestState class] ] )
    {
       NSLog(@" customDelegate2-me %@",identity.userId);
        return YES;
    }
    else {
        NSLog(@"customDelegate2- Not me %@",identity.userId);
        return NO;
    }
    
}

@end
@interface QeoEntityTests : XCTestCase <QEOStateReaderDelegate>
@property (nonatomic, strong) QEOFactory *factory;

@end

@implementation QeoEntityTests
QEOStateWriter *stateWriter;
QEOStateReader *stateReader;
customDelegate1 *cd1;
customDelegate2 *cd2;
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
    XCTAssertNotNil(self.factory, @"Factory initialisation should have succeeded");
    NSError *error;
    // Very simple, should never succeed!
    QEOEntity *entity = [[QEOEntity alloc]init];
    XCTAssertNil(entity, @"Should always return nil");
    
    /*Create new custom delegates to change .
     
     */
    cd1=[[customDelegate1 alloc]init];
    cd2=[[customDelegate2 alloc]init];
    
    
    stateWriter = [[QEOStateWriter alloc]initWithType:[TestState class]
                                              factory:_factory
                                       entityDelegate:nil
                                                error:&error];
    
    test = [[TestState alloc]init];
    test.deviceId = [[DeviceId alloc]init];
    test.deviceId.upper = 90220;
    test.deviceId.lower = 797204;
    test.manufacturer = @"technicolor";
    test.bytenumber = 255;
    test.int16number = INT16_MAX;
    test.int32number = INT32_MAX;
    test.int64number = INT64_MAX;
    test.floatnumber = MAXFLOAT;
    test.somebool = YES;
    test.UUID = [[UUID alloc]init];
    test.UUID.upper = 0;
    test.UUID.lower = 0;
    test.numbersequence = @[@0, @1, @2, @3];
    test.stringsequence = @[@"a", @"b", @"c", @"d"];
    test.uuidsequence = @[[[UUID alloc]init], [[UUID alloc]init], [[UUID alloc]init]];
    ((UUID *)test.uuidsequence[0]).upper = 0;
    ((UUID *)test.uuidsequence[0]).lower = 0;
    ((UUID *)test.uuidsequence[1]).upper = 1;
    ((UUID *)test.uuidsequence[1]).lower = 1;
    ((UUID *)test.uuidsequence[2]).upper = 2;
    ((UUID *)test.uuidsequence[2]).lower = 2;
    
    
    stateReader=[[QEOStateReader alloc]initWithType:[TestState class] factory:_factory delegate:self entityDelegate:nil error:nil];
    
    
}

- (void)tearDown
{
    // Put teardown code here; it will be run once, after the last test case.
    stateReader.entityDelegate=nil;
    stateWriter.entityDelegate=nil;
    cd1=nil;
    cd2=nil;
    test=nil;
    testRead=nil;
    stateReader=nil;
    stateWriter=nil;
    self.factory = nil;
    [super tearDown];
}

//Write to and read form yourself.
- (void)testWritetoandReadYourIdentity
{
    
    stateWriter.entityDelegate=cd2;
    stateReader.entityDelegate=cd2;
    [stateWriter write:test withError:nil];
    
    [stateReader updatePolicyWithError:nil];
    [stateWriter updatePolicyWithError:nil];
    
    [stateReader enumerateInstancesUsingBlock:^void (QEOType *qeoType, BOOL *cont) {
        
        testRead=(TestState *)qeoType;
        
    }];
    XCTAssertEqual(testRead.deviceId.upper, test.deviceId.upper, @"TestState reader-writer Deviceid.upper Objects should be same");
    XCTAssertEqual(testRead.deviceId.lower, test.deviceId.lower, @"TestState reader-writer Deviceid.lower Objects should be same");
}

//Write to yourself, read for public
- (void)testWritetoYourIdentityAndReadPublic
{
    
    stateWriter.entityDelegate=cd2;
    stateReader.entityDelegate=cd1;
   
    [stateWriter updatePolicyWithError:nil];
    [stateWriter write:test withError:nil];
    [stateReader updatePolicyWithError:nil];
    
    [stateReader enumerateInstancesUsingBlock:^void (QEOType *qeoType, BOOL *cont) {
        
        testRead=(TestState *)qeoType;
        
    }];
    XCTAssertNotEqual(testRead.deviceId.upper, test.deviceId.upper, @"TestState reader-writer Deviceid Objects should not be same");
    XCTAssertNil(testRead, @"You should not be able to read message.");
}

//Write to public, read for yourself
- (void)testWritetoPublicAndReadYourIdentity
{
    stateWriter.entityDelegate=cd1;
    stateReader.entityDelegate=cd2;
    [stateWriter updatePolicyWithError:nil];
    [stateWriter write:test withError:nil];
    [stateReader updatePolicyWithError:nil];
    
    [stateReader enumerateInstancesUsingBlock:^void (QEOType *qeoType, BOOL *cont) {
        
        testRead=(TestState *)qeoType;
        
    }];
    
    XCTAssertNotEqual(testRead.deviceId.upper, test.deviceId.upper, @"TestState reader-writer Deviceid Objects should not be same");
    XCTAssertNil(testRead, @"You should not be able to read message.");
}

//Write to and read from public
- (void)testWritetoAndReadPublic
{
    stateWriter.entityDelegate=cd1;
   
    stateReader.entityDelegate=cd1;
    [stateWriter updatePolicyWithError:nil];
    [stateReader updatePolicyWithError:nil];
    
     BOOL flag=[stateWriter write:test withError:nil];
    NSLog(@"falg value-: %d", flag);
    
       sleep(1);
       [stateReader enumerateInstancesUsingBlock:^void (QEOType *qeoType, BOOL *cont) {
        
        NSLog(@" bad test: %@", qeoType.description);
        testRead=(TestState *)qeoType;
        
    }];
    
    XCTAssertNotEqual(testRead.deviceId.upper, test.deviceId.upper, @"TestState reader-writer Deviceid Objects should not be same");
    XCTAssertNil(testRead, @"No messages avilabale to read as you have no permission to write");
}

@end
