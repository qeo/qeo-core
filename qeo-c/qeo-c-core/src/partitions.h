/*
 * Copyright (c) 2014 - Qeo LLC
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

#ifndef PARTITIONS_H_
#define PARTITIONS_H_

#include <dds/dds_dcps.h>
#include <qeocore/api.h>
#include "core.h"

/**
 * When no partition strings are present in the sequence, add the one that
 * makes the entity disabled.
 */
qeo_retcode_t partition_validate_disabled(DDS_StringSeq *partitions,
                                          bool is_writer);

qeo_retcode_t partition_update_reader(const qeocore_reader_t *reader);
qeo_retcode_t partition_update_writer(const qeocore_writer_t *writer);

#endif /* PARTITIONS_H_ */
