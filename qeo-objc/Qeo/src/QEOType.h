/*
 * Copyright (c) 2015 - Qeo LLC
 *
 * The source code form of this Qeo Open Source Project component is subject
 * to the terms of the Clear BSD license.
 *
 * You can redistribute it and/or modify it under the terms of the Clear BSD
 * License (http://directory.fsf.org/wiki/License:ClearBSD). See LICENSE file
 * for more details.
 *
 * The Qeo Open Source Project also includes third party Open Source Software.
 * See LICENSE file for more details.
 */

#import <Foundation/Foundation.h>
#import "Qeo.h"
#import "qeocore/api.h"
#import <qeo-c-core/dds/dds_tsm.h>
#import "QEOEntity.h"

@interface QEOType()
+(const DDS_TypeSupport_meta *)getMetaType;
-(BOOL)marshallToData:(qeocore_data_t *)data withTypeInfo:(qeo_tsm_dynamic_type_hndl_t)typeInfo;
-(BOOL)marshallOnlyKeyToData:(qeocore_data_t *)data withTypeInfo:(qeo_tsm_dynamic_type_hndl_t)typeInfo;
-(instancetype)initFromData:(const qeocore_data_t *)data withTypeInfo:(qeo_tsm_dynamic_type_hndl_t)typeInfo;
-(instancetype)initOnlyKeyFromData:(const qeocore_data_t *)data withTypeInfo:(qeo_tsm_dynamic_type_hndl_t)typeInfo;

@end
