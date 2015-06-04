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
#import "qeo-tsm-to-dynamic/tsm-to-dynamic.h"
#import "QEOType.h"
#import <qeocore/api.h>
#import "QEOFactory.h"

@interface QEOEntity()

@property (readwrite, strong, nonatomic) QEOFactory *factory;
@property (readonly, nonatomic) qeocore_type_t *internalQeoType;
@property (readonly, nonatomic) qeo_tsm_dynamic_type_hndl_t typeInfo;
@property (readonly, nonatomic) const DDS_TypeSupport_meta *tsm;

- (instancetype)initWithType:(Class)qeoType
                     factory:(QEOFactory *) factory
              entityDelegate:(id <QEOEntityDelegate>)entityDelegate
                       error:(NSError **)error;

@end

qeo_policy_perm_t reader_policy_update_callback(const qeocore_reader_t *reader,
                                                const qeo_policy_identity_t *identity,
                                                uintptr_t userdata);
qeo_policy_perm_t writer_policy_update_callback(const qeocore_writer_t *writer,
                                                const qeo_policy_identity_t *identity,
                                                uintptr_t userdata);