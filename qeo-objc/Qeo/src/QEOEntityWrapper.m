#import "QEOEntityWrapper.h"
#import "QEOType.h"
#import "qeo/log.h"

@implementation QEOEntityWrapper

-(instancetype)init
{
    // Don't allow the default constructor
    return nil;
}

-(instancetype)initWithEntity:(QEOEntity *)entity
{
    if ((nil == entity) || (nil == entity.qeoType)) {
        qeo_log_e("invalid entity");
        return nil;
    }
    const DDS_TypeSupport_meta *metaType = [entity.qeoType getMetaType];
    if (NULL == metaType) {
        qeo_log_e("invalid entity metatype (NULL)");
        return nil;
    }

    self = [super init];
    if (self) {
        _qeoTypeName = [NSString stringWithUTF8String:metaType[0].name];
        if ([_qeoTypeName isEqualToString:@""]) {
            qeo_log_e("invalid entity metatype name (empty)");
            return nil;
        }
        qeo_log_d("entity with type name: %s",[_qeoTypeName UTF8String]);
        _entity = entity;
    }
    return self;
}

@end
