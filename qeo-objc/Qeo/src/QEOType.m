#import "QEOType.h"
#import "qeo-tsm-to-dynamic/tsm-to-dynamic.h"
#import "QEOFactory.h"
#import "qeo/log.h"

@implementation QEOType
/*#######################################################################
 # C - IMPLEMENTATION                                                   #
 ########################################################################*/

struct hash_s
{
    NSUInteger hash;
    uintptr_t obj;
};

struct equal_s
{
    BOOL result;
    uintptr_t obj1;
    uintptr_t obj2;
};

struct description_s
{
    uintptr_t description; /* NSMutableString * */
    uintptr_t obj;
};

static bool get_val_cb(uintptr_t in_data, const char *name, CDR_TypeCode_t type, qeo_t2d_types_t *value)
{
    void *ptr = (void *)in_data;
    QEOType *qeoType = (__bridge QEOType *)ptr;
    bool retval = true;
    id val = [qeoType valueForKey:[NSString stringWithCString:name encoding:NSUTF8StringEncoding]];
    
    switch (type) {
        case CDR_TYPECODE_SHORT:
            value->short_val = [val shortValue];
            break;
        case CDR_TYPECODE_LONG:
            value->long_val = [val intValue];
            break;
        case CDR_TYPECODE_LONGLONG:
            value->longlong_val = [val longLongValue];
            break;
        case CDR_TYPECODE_FLOAT:
            value->float_val = [val floatValue];
            break;
        case CDR_TYPECODE_BOOLEAN:
            value->bool_val = [val boolValue];
            break;
        case CDR_TYPECODE_OCTET:
            value->char_val = [val unsignedCharValue];
            break;
        case CDR_TYPECODE_ENUM:
            value->enum_val = [val intValue];
            break;
        case CDR_TYPECODE_CSTRING:
            value->string_val = (char *)[val UTF8String];
            if (value->string_val == NULL){ /* DDS does not support NULL pointers ... */
                value->string_val = "";
            }
            break;
        case CDR_TYPECODE_SEQUENCE:{
            NSArray *array = (NSArray *)val;
            value->seq.seq_ref = (uintptr_t)(__bridge void *)array;
            value->seq.seq_size = [array count];
        }
            break;
        case CDR_TYPECODE_STRUCT:
        {
           
            value->typeref.ref = (uintptr_t)(__bridge void *)val;
        }
            break;
        default:
            qeo_log_e("not supported yet (%d) ", type);
            retval = false;
    }
    
    return retval;
}

static bool get_seq_val_cb(uintptr_t in_data, int index, CDR_TypeCode_t type, qeo_t2d_types_t *value)
{
    NSArray *array = (__bridge NSArray *)(void *)in_data;
    bool retval = true;
    
    switch (type){
        case CDR_TYPECODE_SHORT:
            value->short_val = [array[index] shortValue];
            break;
        case CDR_TYPECODE_LONG:
            value->long_val = [array[index] intValue];
            break;
        case CDR_TYPECODE_LONGLONG:
            value->longlong_val = [array[index] longLongValue];
            break;
        case CDR_TYPECODE_FLOAT:
            value->float_val = [array[index] floatValue];
            break;
        case CDR_TYPECODE_BOOLEAN:
            value->bool_val = [array[index] boolValue];
            break;
        case CDR_TYPECODE_OCTET:
            value->char_val = [array[index] unsignedCharValue];
            break;
        case CDR_TYPECODE_ENUM:
            value->enum_val = [array[index] intValue];
            break;
        case CDR_TYPECODE_CSTRING:
            value->string_val = (char *)[array[index] UTF8String];
            if (value->string_val == NULL){
                value->string_val = "";
            }
            value->string_val = strdup(value->string_val); /* special behaviour for sequences of strings */
            break;
        case CDR_TYPECODE_SEQUENCE:
        {
            NSArray *nested_array = [array[index] array];
            value->seq.seq_ref = (uintptr_t)(__bridge void *)nested_array;
            value->seq.seq_size = [nested_array count];
        }
            break;
        case CDR_TYPECODE_STRUCT:
        {
            value->typeref.ref = (uintptr_t)(__bridge void *)array[index];
        }
            break;
        default:
            qeo_log_e("not supported yet (%d) ", type);
            retval = false;
    }

    return retval;
}


const static qeo_t2d_marshal_cbs_t _mcbs =
{
    .get_val_cb = get_val_cb,
    .get_seq_val_cb = get_seq_val_cb
};

static char *make_type_name(const char *topic_name)
{
    char *type = strdup(topic_name);
    
    if (type == NULL){
        return NULL;
    }
    
    char *it = type;
    while (*it++ != '\0'){
        if (*it == '.'){
            *it = '_';
        }
    }
    
    return type;
}

static bool set_val_cb(uintptr_t out_data, const char *name, CDR_TypeCode_t type, qeo_t2d_types_t *value)
{
    void *ptr = (void *)out_data;
    QEOType *qeoType = (__bridge QEOType *)ptr;
    id val;
    
    switch (type) {
        case CDR_TYPECODE_SHORT:
            val = [NSNumber numberWithShort:value->short_val];
            break;
        case CDR_TYPECODE_LONG:
            val = [NSNumber numberWithInt:value->long_val];
            break;
        case CDR_TYPECODE_LONGLONG:
            val = [NSNumber numberWithLongLong:value->longlong_val];
            break;
        case CDR_TYPECODE_FLOAT:
            val = [NSNumber numberWithFloat:value->float_val];
            break;
        case CDR_TYPECODE_BOOLEAN:
            val = [NSNumber numberWithBool:value->bool_val];
            break;
        case CDR_TYPECODE_OCTET:
            val = [NSNumber numberWithChar:value->char_val];
            break;
        case CDR_TYPECODE_CSTRING:
            val = [NSString stringWithCString:value->string_val encoding:NSUTF8StringEncoding];
            break;
        case CDR_TYPECODE_ENUM:
            val = [NSNumber numberWithInt:value->enum_val];
            break;
        case CDR_TYPECODE_SEQUENCE:
        {
            NSMutableArray *array = [[NSMutableArray alloc] initWithCapacity:value->seq.seq_size];
            
            // Do not retain here, will be done later in "setValue:forKey:"
            value->seq.seq_ref = (uintptr_t)(__bridge void*)array;
            val = array;
        }
            break;
        case CDR_TYPECODE_STRUCT:
        {
            char *className = make_type_name(value->typeref.name);
            if (className == NULL){
                qeo_log_e("No memory");
                return false;
            }
            
            QEOType *newQeoType = [[NSClassFromString(@(className)) alloc] init];
            if (newQeoType == nil){
                free(className);
                qeo_log_e("Could not make object (%s)", className);
                return false;
            }
            free(className);
            // Do not retain here, will be done later in "setValue:forKey:"
            value->typeref.ref = (uintptr_t)(__bridge void*)newQeoType;
            val = newQeoType;
        }
            break;
        default:
            qeo_log_e("not supported yet (%d)", type);
            return false;
            
    }
    // Retains "newQeoType" automatically here
    [qeoType setValue: val forKey: [NSString stringWithCString: name encoding: NSUTF8StringEncoding]];
    
    return true;
}

static bool set_seq_val_cb(uintptr_t out_data, int index, CDR_TypeCode_t type, qeo_t2d_types_t *value)
{
    NSMutableArray *array = (__bridge NSMutableArray *)(void *)out_data;
    id val;
    
    switch (type){
        case CDR_TYPECODE_SHORT:
            val = [NSNumber numberWithShort:value->short_val];
            break;
        case CDR_TYPECODE_LONG:
            val = [NSNumber numberWithInt:value->long_val];
            break;
        case CDR_TYPECODE_LONGLONG:
            val = [NSNumber numberWithLongLong:value->longlong_val];
            break;
        case CDR_TYPECODE_FLOAT:
            val = [NSNumber numberWithFloat:value->float_val];
            break;
        case CDR_TYPECODE_BOOLEAN:
            val = [NSNumber numberWithBool:value->bool_val];
            break;
        case CDR_TYPECODE_OCTET:
            val = [NSNumber numberWithChar:value->char_val];
            break;
        case CDR_TYPECODE_ENUM:
            val = [NSNumber numberWithInt:value->enum_val];
            break;
        case CDR_TYPECODE_CSTRING:
            val = [NSString stringWithCString:value->string_val encoding:NSUTF8StringEncoding];
            break;
        case CDR_TYPECODE_SEQUENCE:
        {
            NSMutableArray *array = [[NSMutableArray alloc] initWithCapacity:value->seq.seq_size];
            
            // Do not retain here, will be done later in assignment of NSMutableArray
            value->seq.seq_ref = (uintptr_t)(__bridge void*)array;
            val = array;
        }
            break;
        case CDR_TYPECODE_STRUCT:
        {
            char *className = make_type_name(value->typeref.name);
            if (className == NULL){
                qeo_log_e("No memory");
                return false;
            }
            
            QEOType *newQeoType = [[NSClassFromString(@(className)) alloc] init];
            if (newQeoType == nil){
                free(className);
                qeo_log_e("Could not make object (%s)", className);
                return false;
            }
            
            free(className);
            // Do not retain here, will be done later in assignment of NSMutableArray
            value->typeref.ref = (uintptr_t)(__bridge void*)newQeoType;
            val = newQeoType;
        }
            break;
        default:
            qeo_log_e("not supported yet (%d)", type);
            return false;
    }
    
    // NSMutableArray retains automatically the assigned object
    array[index] = val;
    
    return true;
}


const static qeo_t2d_unmarshal_cbs_t _ucbs =
{
    .set_val_cb = set_val_cb,
    .set_seq_val_cb = set_seq_val_cb
};

static bool hash_cb(uintptr_t userdata, const char *name, CDR_TypeCode_t type)
{
    void *ptr = (void *)userdata;
    struct hash_s *hashdata = (struct hash_s *)ptr;
    QEOType *qeoType = (__bridge QEOType *)(void *)hashdata->obj;
    id val = [qeoType valueForKey:[NSString stringWithCString:name encoding:NSUTF8StringEncoding]];
    
    hashdata->hash *= 17;
    hashdata->hash += [val hash];
    
    return true;
}

static bool equals_cb(uintptr_t userdata, const char *name, CDR_TypeCode_t type)
{
    void *ptr = (void *)userdata;
    struct equal_s *equaldata = (struct equal_s *)ptr;
    QEOType *obj1 = (__bridge QEOType *)(void *)equaldata->obj1;
    QEOType *obj2 = (__bridge QEOType *)(void *)equaldata->obj2;
    id val1 = [obj1 valueForKey:[NSString stringWithCString:name encoding:NSUTF8StringEncoding]];
    id val2 = [obj2 valueForKey:[NSString stringWithCString:name encoding:NSUTF8StringEncoding]];
    
    if ([val1 isEqual:val2] == YES){
        equaldata->result = YES;
        return true;
    } else {
        equaldata->result = NO;
        return false;
    }
    
    return true;
}

static bool description_cb(uintptr_t userdata, const char *name, CDR_TypeCode_t type)
{
    void *ptr = (void *)userdata;
    struct description_s *descdata = (struct description_s *)ptr;
    QEOType *qeoType = (__bridge QEOType *)(void *)descdata->obj;

    NSMutableString *description = (__bridge NSMutableString *)(void *)descdata->description;
    
    id val = [qeoType valueForKey:[NSString stringWithCString:name encoding:NSUTF8StringEncoding]];
    
    [description appendFormat:@"%s: %@, ", name, [val description]];
    
    return true;
}

/*#######################################################################
 # OBJECTIVE-C IMPLEMENTATION                                        #
 ########################################################################*/

+ (const DDS_TypeSupport_meta *)getMetaType
{
    return nil;
}


- (instancetype)init
{
    /* Prevent init of baseclass */
     if ([self isMemberOfClass:[QEOType class]]){
         qeo_log_e("Not allowed to instantiate QEOType");
        return nil;
        
    }
         
    return [super init];
}

- (NSUInteger)hash
{
    qeo_retcode_t ret;
    qeo_t2d_flags_t flags;
    const DDS_TypeSupport_meta *tsm = [[self class] getMetaType];
    
    if (tsm[0].flags & TSMFLAG_KEY) {
        flags = QEO_T2D_FLAGS_KEY;
    } else {
        flags = QEO_T2D_FLAGS_ALL;
    }
    
    struct hash_s hash_data = {
        .hash = 1,
        .obj = (uintptr_t)self
    };
    
    if ((ret = qeo_walk_tsm_generic(tsm, (uintptr_t)&hash_data, flags, hash_cb)) != QEO_OK) {
        qeo_log_e("Could not calculate hash (%d)", ret);
        return 0;
    }
   
    return hash_data.hash;
}


- (BOOL)isEqual:(id)other
{
    if (other == self) {
        return YES;
    }
    
    if (!other || ![other isKindOfClass:[self class]]) {
        return NO;
    }
    
    qeo_retcode_t ret;
    qeo_t2d_flags_t flags;
    const DDS_TypeSupport_meta *tsm = [[self class] getMetaType];
    
    if (tsm[0].flags & TSMFLAG_KEY) {
        flags = QEO_T2D_FLAGS_KEY;
    } else {
        flags = QEO_T2D_FLAGS_ALL;
    }
    
    struct equal_s equals_data = {
        .result = false,
        .obj1 = (uintptr_t)self,
        .obj2 = (uintptr_t)other
    };
    
    if ((ret = qeo_walk_tsm_generic(tsm, (uintptr_t)&equals_data, flags, equals_cb)) != QEO_OK) {
        qeo_log_e("Could not determine equality (%d)", ret);
        return NO;
    }
    
    return equals_data.result;
}

- (NSString *)description {
    
    const DDS_TypeSupport_meta *tsm = [[self class] getMetaType];
    qeo_retcode_t ret;
    NSMutableString *description = [[NSMutableString alloc] init];
    
    [description appendString:@"{"];
    
    struct description_s desc_data = {
        .description = (uintptr_t)description,
        .obj = (uintptr_t)self
    };
    
    if ((ret = qeo_walk_tsm_generic(tsm, (uintptr_t)&desc_data, QEO_T2D_FLAGS_ALL, description_cb)) != QEO_OK) {
        qeo_log_e("Could not make description (%d)", ret);
        return nil;
    }
    
   [description appendString:@"}"];
    
    return description;
}


- (BOOL)marshallToData:(qeocore_data_t *)data
          withTypeInfo:(qeo_tsm_dynamic_type_hndl_t)typeInfo
             withFlags:(qeo_t2d_flags_t)flags
{
    const DDS_TypeSupport_meta *tsm = [[self class] getMetaType];
    
    if (qeo_walk_tsm_for_marshal(typeInfo, tsm, (uintptr_t)self, data, QEO_T2D_FLAGS_ALL, &_mcbs) != QEO_OK){
        qeo_log_e("Could not marshal..");
        return NO;
    }
    
    return YES;
}

- (BOOL)marshallOnlyKeyToData:(qeocore_data_t *)data
                 withTypeInfo:(qeo_tsm_dynamic_type_hndl_t)typeInfo
{
    return [self marshallToData:data withTypeInfo:typeInfo withFlags:QEO_T2D_FLAGS_KEY];
}

- (BOOL)marshallToData:(qeocore_data_t *)data
          withTypeInfo:(qeo_tsm_dynamic_type_hndl_t)typeInfo
{
    return [self marshallToData:data withTypeInfo:typeInfo withFlags:QEO_T2D_FLAGS_ALL];
}

- (instancetype)initFromData:(const qeocore_data_t *)data
                withTypeInfo:(qeo_tsm_dynamic_type_hndl_t)typeInfo
                   withFlags:(qeo_t2d_flags_t)flags
{
    self = [super init];
    if (self == nil) {
        return nil;
    }
    
    const DDS_TypeSupport_meta *tsm = [[self class] getMetaType];
    
    if (qeo_walk_tsm_for_unmarshal(typeInfo, tsm, data, (uintptr_t)self, QEO_T2D_FLAGS_ALL, &_ucbs) != QEO_OK) {
        qeo_log_e("Could not unmarshal");
        return nil;
    }
        
    return self;
}

-(instancetype)initFromData:(const qeocore_data_t *)data
               withTypeInfo:(qeo_tsm_dynamic_type_hndl_t)typeInfo
{
    return [self initFromData:data withTypeInfo:typeInfo withFlags:QEO_T2D_FLAGS_ALL];
}

-(instancetype)initOnlyKeyFromData:(const qeocore_data_t *)data
                      withTypeInfo:(qeo_tsm_dynamic_type_hndl_t)typeInfo
{
    return [self initFromData:data withTypeInfo:typeInfo withFlags:QEO_T2D_FLAGS_KEY];
}

@end
