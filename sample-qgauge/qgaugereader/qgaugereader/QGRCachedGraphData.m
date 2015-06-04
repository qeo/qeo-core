#import "QGRCachedGraphData.h"

@implementation QGRCachedGraphData

-(id)init
{
    self = [super init];
    if (self) {
        _inData = [[NSMutableArray alloc] init];
        _outData = [[NSMutableArray alloc] init];
       
    }
    return self;
    
}

@end
