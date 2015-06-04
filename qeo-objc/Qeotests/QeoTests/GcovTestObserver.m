#import <XCTest/XCTest.h>

@interface GcovTestObserver : XCTestObserver

@end

@implementation GcovTestObserver

#ifdef DEBUG
+ (void)load {
    [[NSUserDefaults standardUserDefaults] setValue:@"XCTestLog,GcovTestObserver"
                                             forKey:@"XCTestObserverClass"];
}
#endif
- (void)stopObserving
{
    [super stopObserving];
    extern void __gcov_flush(void);
    __gcov_flush();
}

@end
