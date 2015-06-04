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

/* dd_log.h -- DDS Discovery logging utility functions. */

#ifndef __dd_log_h_
#define	__dd_log_h_

#include <stdio.h>
#include "dds/dds_dcps.h"
#include "dds/dds_aux.h"

unsigned discovery_log_enable (DDS_DomainParticipant part,
			       FILE                  *f,
			       unsigned              mask);

/* Enable discovery logging for the given entities (mask), logging the output
   to the specified file. */

void discovery_log_disable (unsigned handle);

/* Disable discovery logging. */

#endif /* __dd_log_h */

