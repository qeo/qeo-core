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

/*#######################################################################
#                       HEADER (INCLUDE) SECTION                        #
########################################################################*/

#include "core.h"
#include "core_util.h"
#include "partitions.h"
#include "policy.h"
#include "policy_cache.h"

/*#######################################################################
#                       TYPES SECTION                                   #
########################################################################*/

typedef struct {
    struct {
        unsigned is_writer : 1;
    } flags;
    union {
        const entity_t *entity;
        const qeocore_reader_t *reader;
        const qeocore_writer_t *writer;
    } e;
    DDS_StringSeq *seq;
    qeo_retcode_t rc;
} partitions_data_t;

/*#######################################################################
#                   STATIC FUNCTION DECLARATION                         #
########################################################################*/

/*#######################################################################
#                       STATIC VARIABLE SECTION                         #
########################################################################*/

static const char *_disabled_reader = "DISABLED_READER";
static const char *_disabled_writer = "DISABLED_WRITER";

/*#######################################################################
#                   STATIC FUNCTION IMPLEMENTATION                      #
########################################################################*/

static qeo_policy_perm_t call_on_update(partitions_data_t *data,
                                        qeo_policy_identity_t *id)
{
    qeo_policy_perm_t perm = QEO_POLICY_ALLOW;

    if (data->flags.is_writer) {
        if (NULL != data->e.writer->listener.on_policy_update) {
            perm = data->e.writer->listener.on_policy_update(data->e.writer, id, data->e.writer->listener.userdata);
        }
    }
    else {
        if (NULL != data->e.reader->listener.on_policy_update) {
            perm = data->e.reader->listener.on_policy_update(data->e.reader, id, data->e.reader->listener.userdata);
        }
    }
    return perm;
}

static void get_partitions_cb(qeo_security_policy_hndl policy,
                              uintptr_t cookie,
                              const char *topic_name,
                              unsigned int selector,
                              struct partition_string_list_node *list)
{
    partitions_data_t *data = (partitions_data_t *)cookie;

    if (QEO_OK == data->rc) {
        struct partition_string_list_node *item = NULL;

        LL_FOREACH(list, item) {
            qeo_policy_perm_t perm = QEO_POLICY_ALLOW;

            if (1 == item->fine_grained) {
                perm = call_on_update(data, &item->id);
            }
            if (QEO_POLICY_ALLOW == perm) {
                data->rc = ddsrc_to_qeorc(dds_seq_append(data->seq, (void*)&item->partition_string));
                if (QEO_OK != data->rc) {
                    break;
                }
            }
        }
    }
}

static qeo_retcode_t update_partition_seq(partitions_data_t *data)
{
    qeo_retcode_t rc = QEO_OK;
    unsigned int mask = (data->flags.is_writer ? PARTITION_STRING_SELECTOR_WRITE : PARTITION_STRING_SELECTOR_READ);

    /* wipe partitions */
    DDS_StringSeq__clear(data->seq);
    /* insert current partitions */
    rc = qeo_security_policy_get_partition_strings(data->e.entity->factory->qeo_pol, (uintptr_t)data,
                                                   DDS_Topic_get_name(data->e.entity->topic), mask,
                                                   get_partitions_cb);
    if (QEO_OK == rc) {
        rc = data->rc;
    }
    if (QEO_OK == rc) {
        rc = partition_validate_disabled(data->seq, data->flags.is_writer);
    }
    /* signal end-of-list */
    call_on_update(data, NULL);
    return rc;
}

/*#######################################################################
#                   PUBLIC FUNCTION IMPLEMENTATION                      #
########################################################################*/

qeo_retcode_t partition_validate_disabled(DDS_StringSeq *partitions,
                                          bool is_writer)
{
    qeo_retcode_t rc = QEO_OK;

    if (0 == DDS_SEQ_LENGTH(*partitions)) {
        const char *disabled = is_writer ? _disabled_writer : _disabled_reader;

        rc = ddsrc_to_qeorc(dds_seq_append(partitions, &disabled));
    }
    return rc;
}

qeo_retcode_t partition_update_reader(const qeocore_reader_t *reader)
{
    DDS_ReturnCode_t ddsrc = DDS_RETCODE_OK;
    qeo_retcode_t rc = QEO_OK;
    DDS_SubscriberQos qos;

    ddsrc = DDS_Subscriber_get_qos(reader->sub, &qos);
    qeo_log_dds_rc("DDS_Subscriber_get_qos", ddsrc);
    rc = ddsrc_to_qeorc(ddsrc);
    if (QEO_OK == rc) {
        partitions_data_t data;

        data.flags.is_writer = 0;
        data.e.reader = reader;
        data.seq = &qos.partition.name;
        data.rc = QEO_OK;
        rc = update_partition_seq(&data);
        if (QEO_OK == rc) {
            ddsrc = DDS_Subscriber_set_qos(reader->sub, &qos);
            qeo_log_dds_rc("DDS_Subscriber_set_qos", ddsrc);
            rc = ddsrc_to_qeorc(ddsrc);
        }
        DDS_StringSeq__clear(&qos.partition.name);
    }
    return rc;
}

qeo_retcode_t partition_update_writer(const qeocore_writer_t *writer)
{
    DDS_ReturnCode_t ddsrc = DDS_RETCODE_OK;
    qeo_retcode_t rc = QEO_OK;
    DDS_PublisherQos qos;

    ddsrc = DDS_Publisher_get_qos(writer->pub, &qos);
    qeo_log_dds_rc("DDS_Publisher_get_qos", ddsrc);
    rc = ddsrc_to_qeorc(ddsrc);
    if (QEO_OK == rc) {
        partitions_data_t data;

        data.flags.is_writer = 1;
        data.e.writer = writer;
        data.seq = &qos.partition.name;
        data.rc = QEO_OK;
        rc = update_partition_seq(&data);
        if (QEO_OK == rc) {
            ddsrc = DDS_Publisher_set_qos(writer->pub, &qos);
            qeo_log_dds_rc("DDS_Publisher_set_qos", ddsrc);
            rc = ddsrc_to_qeorc(ddsrc);
        }
        DDS_StringSeq__clear(&qos.partition.name);
    }
    return rc;
}

