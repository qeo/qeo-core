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

#ifndef PARTITION_STRING_LIST_NODE_H_
#define PARTITION_STRING_LIST_NODE_H_

/*#######################################################################
# HEADER (INCLUDE) SECTION                                               #
########################################################################*/
#include <inttypes.h>
#include <qeo/error.h>
#include <qeocore/api.h>
#include "policy_identity.h"
#include "utlist.h"


/*#######################################################################
# TYPES SECTION                                                         #
########################################################################*/
#define PARTITION_STRING_SELECTOR_READ 0x01
#define PARTITION_STRING_SELECTOR_NOT_READ 0x02
#define PARTITION_STRING_SELECTOR_WRITE 0x04
#define PARTITION_STRING_SELECTOR_NOT_WRITE 0x08
#define PARTITION_STRING_SELECTOR_ALL (PARTITION_STRING_SELECTOR_READ | PARTITION_STRING_SELECTOR_NOT_READ | PARTITION_STRING_SELECTOR_WRITE | PARTITION_STRING_SELECTOR_NOT_WRITE)

struct partition_string_list_node {
    unsigned fine_grained : 1;
    qeo_policy_identity_t id;
    const char *partition_string;
    struct partition_string_list_node *next;
};

/*########################################################################
#                       API FUNCTION SECTION                             #
########################################################################*/

#endif
