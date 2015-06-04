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

//
#include <stdint.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdio.h>
#include <unistd.h>


#include <qeo/factory.h> 
#include <qeo/log.h> 

#include "unittest/unittest.h"
#include "QSimpleChat_ChatMessage.h"
#include "QNote_Wall.h"
#include "qeo_DeviceInfo.h"

#include "tsm-to-dynamic.h"
#include "cwifi.h"

struct hash_s {
    uint64_t hash;
    uintptr_t dataptr;
};

static qeo_factory_t *_factory;
static qeo_tsm_dynamic_type_hndl_t _types;
/* ===[ public API tests ]=================================================== */

START_TEST(wifi_datamodel_begintoend)
{
    qeocore_type_t *type[6];
    qeo_tsm_dynamic_type_hndl_t types[6];
    int i = 0;
    
    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_wifi_Radio_type, &type[i], &types[i]), QEO_OK);
    ck_assert_int_ne(type, NULL);
    ck_assert_int_ne(types, NULL);
    ++i;
    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_wifi_Interface_type, &type[i], &types[i]), QEO_OK);
    ck_assert_int_ne(type, NULL);
    ck_assert_int_ne(types, NULL);
    ++i;
    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_wifi_AssociatedStation_type, &type[i], &types[i]), QEO_OK);
    ck_assert_int_ne(type, NULL);
    ck_assert_int_ne(types, NULL);
    ++i;
    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_wifi_ScanListEntry_type, &type[i], &types[i]), QEO_OK);
    ck_assert_int_ne(type, NULL);
    ck_assert_int_ne(types, NULL);
    ++i;
    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_wifi_ScanList_type, &type[i], &types[i]), QEO_OK);
    ck_assert_int_ne(type, NULL);
    ck_assert_int_ne(types, NULL);
    ++i;
    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_wifi_ScanRequest_type, &type[i], &types[i]), QEO_OK);
    ck_assert_int_ne(type, NULL);
    ck_assert_int_ne(types, NULL);

    i = 0;
                                               
    ck_assert_int_eq(qeo_unregister_tsm(&types[i]), QEO_OK);
    qeocore_type_free(type[i]);
    ++i;
    ck_assert_int_eq(qeo_unregister_tsm(&types[i]), QEO_OK);
    qeocore_type_free(type[i]);
    ++i;
    ck_assert_int_eq(qeo_unregister_tsm(&types[i]), QEO_OK);
    qeocore_type_free(type[i]);
    ++i;
    ck_assert_int_eq(qeo_unregister_tsm(&types[i]), QEO_OK);
    qeocore_type_free(type[i]);
    ++i;
    ck_assert_int_eq(qeo_unregister_tsm(&types[i]), QEO_OK);
    qeocore_type_free(type[i]);
    ++i;
    ck_assert_int_eq(qeo_unregister_tsm(&types[i]), QEO_OK);
    qeocore_type_free(type[i]);
    ++i;
    
}
END_TEST

START_TEST(wifi_datamodel_endtobegin)
{
    qeocore_type_t *type;
    qeo_tsm_dynamic_type_hndl_t types;
    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_wifi_ScanRequest_type, &type, &types), QEO_OK);
    ck_assert_int_ne(type, NULL);
    ck_assert_int_ne(types, NULL);
    ck_assert_int_eq(qeo_unregister_tsm(&types), QEO_OK);
    qeocore_type_free(type);
    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_wifi_ScanList_type, &type, &types), QEO_OK);
    ck_assert_int_ne(type, NULL);
    ck_assert_int_ne(types, NULL);
    ck_assert_int_eq(qeo_unregister_tsm(&types), QEO_OK);
    qeocore_type_free(type);
    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_wifi_ScanListEntry_type, &type, &types), QEO_OK);
    ck_assert_int_ne(type, NULL);
    ck_assert_int_ne(types, NULL);
    ck_assert_int_eq(qeo_unregister_tsm(&types), QEO_OK);
    qeocore_type_free(type);
    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_wifi_AssociatedStation_type, &type, &types), QEO_OK);
    ck_assert_int_ne(type, NULL);
    ck_assert_int_ne(types, NULL);
    ck_assert_int_eq(qeo_unregister_tsm(&types), QEO_OK);
    qeocore_type_free(type);
    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_wifi_Interface_type, &type, &types), QEO_OK);
    ck_assert_int_ne(type, NULL);
    ck_assert_int_ne(types, NULL);
    ck_assert_int_eq(qeo_unregister_tsm(&types), QEO_OK);
    qeocore_type_free(type);
    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_wifi_Radio_type, &type, &types), QEO_OK);
    ck_assert_int_ne(type, NULL);
    ck_assert_int_ne(types, NULL);
    qeocore_type_free(type);
    ck_assert_int_eq(qeo_unregister_tsm(&types), QEO_OK);
    
}
END_TEST


/*#######################################################################
 # uuid                                        #
 ########################################################################*/
static bool qeo_UUID_get_val_cb(uintptr_t in_data, const char *name, CDR_TypeCode_t type, qeo_t2d_types_t *value){

    const org_qeo_UUID_t *uuid = (const org_qeo_UUID_t *)in_data;
    if (strcmp(name, "lower") == 0){
        value->longlong_val = uuid->lower;
    } else if (strcmp(name, "upper") == 0){
        value->longlong_val = uuid->upper;

    } else {
        return false;
    }

    return true;
}

static bool qeo_UUID_set_val_cb(uintptr_t out_data, const char *name, CDR_TypeCode_t type, qeo_t2d_types_t *value /* cast me */){

    org_qeo_UUID_t *uuid = (org_qeo_UUID_t *)out_data;

    if (strcmp(name, "lower") == 0){
        uuid->lower = value->longlong_val;
    } else if (strcmp(name, "upper") == 0){
        uuid->upper = value->longlong_val;
    } else {
        return false;
    }

    return true;
}

const static qeo_t2d_unmarshal_cbs_t _uuid_ucbs = {
    .set_val_cb = qeo_UUID_set_val_cb,
};

const static qeo_t2d_marshal_cbs_t _uuid_mcbs = {
    .get_val_cb = qeo_UUID_get_val_cb,
};
/*#######################################################################
 # scanlist                                        #
 ########################################################################*/
static bool wifi_ScanList_get_val_cb(uintptr_t in_data, const char *name, CDR_TypeCode_t type, qeo_t2d_types_t *value){ 

    const org_qeo_wifi_ScanListEntry_t *slentry = (const org_qeo_wifi_ScanListEntry_t *)in_data;
    if (strcmp(name, "BSSID") == 0){
        value->string_val = slentry->BSSID;
    } else if (strcmp(name, "SSID") == 0){
        value->string_val = slentry->SSID;
    } else if (strcmp(name, "channel") == 0){
        value->long_val = slentry->channel;
    } else if (strcmp(name, "RSSI") == 0){
        value->long_val = slentry->RSSI;
    } else {
        qeo_log_e("unknown field %s", name);
        return false;
    }

    return true;

}

static bool wifi_ScanList_set_val_cb(uintptr_t out_data, const char *name, CDR_TypeCode_t type, qeo_t2d_types_t *value /* cast me */){

    org_qeo_wifi_ScanListEntry_t *slentry = (org_qeo_wifi_ScanListEntry_t *)out_data;


    if (strcmp(name, "BSSID") == 0){
        slentry->BSSID = strdup(value->string_val);
    } else if (strcmp(name, "SSID") == 0){
        slentry->SSID = strdup(value->string_val);
    } else if (strcmp(name, "channel") == 0){
        slentry->channel = value->long_val;
    } else if (strcmp(name, "RSSI") == 0){
        slentry->RSSI = value->long_val;
    } else {
        qeo_log_e("unknown field %s", name);
        return false;
    }

    return true;

}

const static qeo_t2d_marshal_cbs_t _sl_mcbs = {
    .get_val_cb = wifi_ScanList_get_val_cb,
};

const static qeo_t2d_unmarshal_cbs_t _sl_ucbs = {
    .set_val_cb = wifi_ScanList_set_val_cb,
};

/*#######################################################################
 # simplechat                                        #
 ########################################################################*/
static bool simplechat_ChatMessage_list_get_seq_val_cb(uintptr_t in_data, int index, CDR_TypeCode_t type, qeo_t2d_types_t *value){

    const org_qeo_wifi_ScanList_list_seq *listEntry = (const org_qeo_wifi_ScanList_list_seq *)in_data;
    value->typeref.ref = (uintptr_t)(DDS_SEQ_ITEM_PTR(*listEntry, index));
    value->typeref.mcbs = &_sl_mcbs;

    return true;

}


static bool simplechat_ChatMessage_list_set_seq_val_cb(uintptr_t out_data, int index, CDR_TypeCode_t type, qeo_t2d_types_t *value){

    org_qeo_wifi_ScanList_list_seq *listEntry = (org_qeo_wifi_ScanList_list_seq *)out_data;
    
    switch(type){
        case CDR_TYPECODE_STRUCT:
            ck_assert_str_eq(value->typeref.name, org_qeo_wifi_ScanListEntry_type[0].name);
            value->typeref.ref = (uintptr_t)(DDS_SEQ_ITEM_PTR(*listEntry, index));
            value->typeref.ucbs = &_sl_ucbs;

        break;
        default:
            qeo_log_e("Unsupported type %d", type);
            return false;

    }

    return true;
}

const static qeo_t2d_marshal_cbs_t _sc_list_mcbs = {
    .get_seq_val_cb = simplechat_ChatMessage_list_get_seq_val_cb,
};

const static qeo_t2d_unmarshal_cbs_t _sc_list_ucbs = {
    .set_seq_val_cb = simplechat_ChatMessage_list_set_seq_val_cb,
};

static bool simplechat_ChatMessage_extraInfo_get_seq_val_cb(uintptr_t in_data, int index, CDR_TypeCode_t type, qeo_t2d_types_t *value){

    const org_qeo_sample_simplechat_ChatMessage_extraInfo_seq *extraInfo = (const org_qeo_sample_simplechat_ChatMessage_extraInfo_seq *)in_data;
    value->char_val = ((int8_t *)DDS_SEQ_DATA(*extraInfo))[index];

    return true;
}

static bool simplechat_ChatMessage_extraInfo_set_seq_val_cb(uintptr_t out_data, int index, CDR_TypeCode_t type, qeo_t2d_types_t *value){

    DDS_VoidPtrSeq *seq = (DDS_VoidPtrSeq *)out_data;
    
    switch(type){
        case CDR_TYPECODE_OCTET:
            ((int8_t *)DDS_SEQ_DATA(*seq))[index] = value->char_val;

        break;
        default:
            qeo_log_e("Unsupported type %d", type);
            return false;

    }

    return true;
}

const static qeo_t2d_marshal_cbs_t _sc_extraInfo_mcbs = {
    .get_seq_val_cb = simplechat_ChatMessage_extraInfo_get_seq_val_cb,
};

const static qeo_t2d_unmarshal_cbs_t _sc_extraInfo_ucbs = {
    .set_seq_val_cb = simplechat_ChatMessage_extraInfo_set_seq_val_cb,
};

static bool simplechat_ChatMessage_colorlist_get_seq_val_cb(uintptr_t in_data, int index, CDR_TypeCode_t type, qeo_t2d_types_t *value){

    const myenum_seq *colorlist = (const myenum_seq *)in_data;
    value->enum_val = ((qeo_enum_value_t *)DDS_SEQ_DATA(*colorlist))[index];

    return true;
}

static bool simplechat_ChatMessage_colorlist_set_seq_val_cb(uintptr_t out_data, int index, CDR_TypeCode_t type, qeo_t2d_types_t *value){

    DDS_VoidPtrSeq *seq = (DDS_VoidPtrSeq *)out_data;
    
    switch(type){
        case CDR_TYPECODE_ENUM:
            ((qeo_enum_value_t *)DDS_SEQ_DATA(*seq))[index] = value->enum_val;

        break;
        default:
            qeo_log_e("Unsupported type %d", type);
            return false;

    }

    return true;
}

const static qeo_t2d_marshal_cbs_t _sc_colorlist_mcbs = {
    .get_seq_val_cb = simplechat_ChatMessage_colorlist_get_seq_val_cb,
};

const static qeo_t2d_unmarshal_cbs_t _sc_colorlist_ucbs = {
    .set_seq_val_cb = simplechat_ChatMessage_colorlist_set_seq_val_cb,
};

static bool simplechat_ChatMessage_get_val_cb(uintptr_t in_data, const char *name, CDR_TypeCode_t type, qeo_t2d_types_t *value){

    const org_qeo_sample_simplechat_ChatMessage_t *chat_msg = (const org_qeo_sample_simplechat_ChatMessage_t *)in_data;
    if (strcmp(name, "from") == 0){
        value->string_val = chat_msg->from;
    } else if (strcmp(name, "fromExtra") == 0){
        value->typeref.ref = (uintptr_t)&chat_msg->fromExtra;
        value->typeref.mcbs = &_uuid_mcbs;
    } else if (strcmp(name, "message") == 0){
        value->string_val = chat_msg->message;
    } else if (strcmp(name, "extraInfo") == 0){
        value->seq.seq_ref = (uintptr_t)&chat_msg->extraInfo;
        value->seq.seq_size = DDS_SEQ_LENGTH(chat_msg->extraInfo);
        value->seq.mcbs = &_sc_extraInfo_mcbs;
    } else if (strcmp(name, "list") == 0){
        value->seq.seq_ref = (uintptr_t)&chat_msg->list;
        value->seq.seq_size = DDS_SEQ_LENGTH(chat_msg->list);
        value->seq.mcbs = &_sc_list_mcbs;
    } else if (strcmp(name, "maincolor") == 0){
        ck_assert_int_eq(type, CDR_TYPECODE_ENUM);
        value->enum_val = chat_msg->maincolor;
    } else if (strcmp(name, "colorlist") == 0){
        ck_assert_int_eq(type, CDR_TYPECODE_SEQUENCE);
        value->seq.seq_ref = (uintptr_t)&chat_msg->colorlist;
        value->seq.seq_size = DDS_SEQ_LENGTH(chat_msg->colorlist);
        value->seq.mcbs = &_sc_colorlist_mcbs;

    } else {
        qeo_log_e("unknown field %s", name);
        return false;
    }

    return true;
}



static bool simplechat_ChatMessage_set_val_cb(uintptr_t out_data, const char *name, CDR_TypeCode_t type, qeo_t2d_types_t *value /* cast me */){


    org_qeo_sample_simplechat_ChatMessage_t *chat_msg = (org_qeo_sample_simplechat_ChatMessage_t *)out_data;
    if (strcmp(name, "from") == 0){
        chat_msg->from = strdup(value->string_val);
    } else if (strcmp(name, "fromExtra") == 0){
        ck_assert_str_eq(value->typeref.name, org_qeo_UUID_type[0].name);
        value->typeref.ref = (uintptr_t)&chat_msg->fromExtra;
        value->typeref.ucbs = &_uuid_ucbs;
    } else if (strcmp(name, "message") == 0){
        chat_msg->message = strdup(value->string_val);
    } else if (strcmp(name, "extraInfo") == 0){
        value->seq.seq_ref = (uintptr_t)&chat_msg->extraInfo;
        value->seq.ucbs = &_sc_extraInfo_ucbs;
        DDS_SEQ_INIT(chat_msg->extraInfo);
        ck_assert_int_eq(dds_seq_require(&chat_msg->extraInfo,value->seq.seq_size), DDS_RETCODE_OK);
        printf("allocating extraInfo sequence with size %zu\n", value->seq.seq_size);
    } else if (strcmp(name, "list") == 0){
        value->seq.seq_ref = (uintptr_t)&chat_msg->list;
        value->seq.ucbs = &_sc_list_ucbs;
        DDS_SEQ_INIT(chat_msg->list);
        ck_assert_int_eq(dds_seq_require(&chat_msg->list,value->seq.seq_size), DDS_RETCODE_OK);
        printf("allocating list sequence with size %zu\n", value->seq.seq_size);
    } else if (strcmp(name, "maincolor") == 0){
        chat_msg->maincolor = value->enum_val;
    } else if (strcmp(name, "colorlist") == 0){
        value->seq.seq_ref = (uintptr_t)&chat_msg->colorlist;
        value->seq.ucbs = &_sc_colorlist_ucbs;
        DDS_SEQ_INIT(chat_msg->colorlist);
        ck_assert_int_eq(dds_seq_require(&chat_msg->colorlist,value->seq.seq_size), DDS_RETCODE_OK);
        printf("allocating colorlist sequence with size %zu\n", value->seq.seq_size);

    } else {
        qeo_log_e("unknown field %s", name);
        return false;
    }

    return true;

}

const static qeo_t2d_unmarshal_cbs_t _sc_ucbs = {
    .set_val_cb = simplechat_ChatMessage_set_val_cb,
    .set_seq_val_cb = NULL,
};

static org_qeo_sample_simplechat_ChatMessage_t _rcvd_chatmsg;
static void on_sc_data_available(const qeocore_reader_t *reader,
                                            const qeocore_data_t *data,
                                            uintptr_t userdata){

    
    qeo_retcode_t ret = QEO_OK;
    switch (qeocore_data_get_status(data)) {
        case QEOCORE_NOTIFY:
            qeo_log_d("Notify received");
            break;
        case QEOCORE_DATA:
            qeo_log_d("Data received");
            ret = qeo_walk_tsm_for_unmarshal(_types, org_qeo_sample_simplechat_ChatMessage_type, data, (uintptr_t)&_rcvd_chatmsg, QEO_T2D_FLAGS_ALL, &_sc_ucbs);
            break;
        case QEOCORE_NO_MORE_DATA:
            qeo_log_d("No more data received");
            break;
        case QEOCORE_REMOVE:
            qeo_log_d("remove received");
            break;
        case QEOCORE_ERROR:
            qeo_log_e("no callback called due to prior error");
            break;
    }
    ck_assert_int_eq(ret, QEO_OK);

}

const static qeo_t2d_marshal_cbs_t _sc_mcbs = {
    .get_val_cb = simplechat_ChatMessage_get_val_cb,
    .get_seq_val_cb = NULL
};

const static qeocore_reader_listener_t _sc_rlistener = {
    .on_data = on_sc_data_available,
    .on_policy_update = NULL,
    .userdata = 0xdeadbabe
};

/*#######################################################################
 # associated station #
 ########################################################################*/

static bool associatedStation_get_val_cb(uintptr_t in_data, const char *name, CDR_TypeCode_t type, qeo_t2d_types_t *value){

    const org_qeo_wifi_AssociatedStation_t *as = (const org_qeo_wifi_AssociatedStation_t *)in_data;
    if (strcmp(name, "MACAddress") == 0){
        value->string_val = as->MACAddress;
    } else if (strcmp(name, "BSSID") == 0){
        value->string_val = as->BSSID;
    } else if (strcmp(name, "capabilities") == 0){
        value->string_val = as->capabilities;
    } else if (strcmp(name, "associated") == 0){
        value->bool_val = as->associated;
    }
     else if (strcmp(name, "lastSeen") == 0){
        value->longlong_val = as->lastSeen;
    }
     else if (strcmp(name, "maxNegotiatedPhyRate") == 0){
        value->long_val = as->maxNegotiatedPhyRate;
    } else {
        qeo_log_e("unknown field %s", name);
        return false;
    }

    return true;
}

static bool associatedStation_set_val_cb(uintptr_t out_data, const char *name, CDR_TypeCode_t type, qeo_t2d_types_t *value /* cast me */){

    org_qeo_wifi_AssociatedStation_t *as = (org_qeo_wifi_AssociatedStation_t *)out_data;
    if (strcmp(name, "MACAddress") == 0){
        as->MACAddress = strdup(value->string_val);
    } else if (strcmp(name, "BSSID") == 0){
        as->BSSID = strdup(value->string_val);
    } else if (strcmp(name, "capabilities") == 0){
        as->capabilities = strdup(value->string_val);
    } else if (strcmp(name, "associated") == 0){
        as->associated = value->bool_val;
    }
     else if (strcmp(name, "lastSeen") == 0){
        as->lastSeen = value->longlong_val;
    }
     else if (strcmp(name, "maxNegotiatedPhyRate") == 0){
        as->maxNegotiatedPhyRate = value->long_val;
    } else {
        qeo_log_e("unknown field %s", name);
        return false;
    }

    return true;
}

const static qeo_t2d_unmarshal_cbs_t _as_ucbs = {
    .set_val_cb = associatedStation_set_val_cb,
};

static org_qeo_wifi_AssociatedStation_t _rcvd_as;
static void on_as_data_available(const qeocore_reader_t *reader,
                                            const qeocore_data_t *data,
                                            uintptr_t userdata){

    
    qeo_retcode_t ret = QEO_OK;
    switch (qeocore_data_get_status(data)) {
        case QEOCORE_NOTIFY:
            qeo_log_d("Notify received");
            break;
        case QEOCORE_DATA:
            qeo_log_d("Data received");
            ret = qeo_walk_tsm_for_unmarshal(_types, org_qeo_wifi_AssociatedStation_type, data, (uintptr_t)&_rcvd_as, QEO_T2D_FLAGS_ALL, &_as_ucbs);
            break;
        case QEOCORE_NO_MORE_DATA:
            qeo_log_d("No more data received");
            break;
        case QEOCORE_REMOVE:
            qeo_log_d("remove received");
            break;
        case QEOCORE_ERROR:
            qeo_log_e("no callback called due to prior error");
            break;
    }
    ck_assert_int_eq(ret, QEO_OK);

}

const static qeo_t2d_marshal_cbs_t _as_mcbs = {
    .get_val_cb = associatedStation_get_val_cb
};

const static qeocore_reader_listener_t _as_rlistener = {
    .on_data = on_as_data_available,
    .on_policy_update = NULL,
    .userdata = 0xdeadbabe
};
/*#######################################################################
 # DeviceId
 ########################################################################*/
static bool qeo_deviceId_get_val_cb(uintptr_t in_data, const char *name, CDR_TypeCode_t type, qeo_t2d_types_t *value){

    const org_qeo_system_DeviceId_t *uuid = (const org_qeo_system_DeviceId_t *)in_data;
    if (strcmp(name, "lower") == 0){
        value->longlong_val = uuid->lower;
    } else if (strcmp(name, "upper") == 0){
        value->longlong_val = uuid->upper;

    } else {
        return false;
    }

    return true;
}

static bool qeo_deviceId_set_val_cb(uintptr_t out_data, const char *name, CDR_TypeCode_t type, qeo_t2d_types_t *value /* cast me */){

    org_qeo_system_DeviceId_t *uuid = (org_qeo_system_DeviceId_t *)out_data;

    if (strcmp(name, "lower") == 0){
        uuid->lower = value->longlong_val;
    } else if (strcmp(name, "upper") == 0){
        uuid->upper = value->longlong_val;
    } else {
        return false;
    }

    return true;
}

const static qeo_t2d_unmarshal_cbs_t _deviceid_ucbs = {
    .set_val_cb = qeo_deviceId_set_val_cb,
};

const static qeo_t2d_marshal_cbs_t _deviceid_mcbs = {
    .get_val_cb = qeo_deviceId_get_val_cb,
};


/*#######################################################################
 # DeviceInfo
 ########################################################################*/
static bool deviceinfo_get_val_cb(uintptr_t in_data, const char *name, CDR_TypeCode_t type, qeo_t2d_types_t *value){

    const org_qeo_system_DeviceInfo_t *di = (const org_qeo_system_DeviceInfo_t *)in_data;
    if (strcmp(name, "deviceId") == 0){
        value->typeref.ref = (uintptr_t)&di->deviceId;
        value->typeref.mcbs = &_deviceid_mcbs;
    } else if (strcmp(name, "manufacturer") == 0){
        value->string_val = di->manufacturer;
    } else if (strcmp(name, "modelName") == 0){
        value->string_val = di->modelName;
    } else if (strcmp(name, "productClass") == 0){
        value->string_val = di->productClass;
    }
     else if (strcmp(name, "serialNumber") == 0){
        value->string_val = di->serialNumber;
    }
     else if (strcmp(name, "hardwareVersion") == 0){
        value->string_val = di->hardwareVersion;
    }
     else if (strcmp(name, "softwareVersion") == 0){
        value->string_val = di->softwareVersion;
    }
     else if (strcmp(name, "userFriendlyName") == 0){
        value->string_val = di->userFriendlyName;
    } else if (strcmp(name, "configURL") == 0){
        value->string_val = di->configURL;
    } else {
        qeo_log_e("unknown field %s", name);
        return false;
    }

    return true;
}

static bool deviceInfo_set_val_cb(uintptr_t out_data, const char *name, CDR_TypeCode_t type, qeo_t2d_types_t *value /* cast me */){

    org_qeo_system_DeviceInfo_t *di = (org_qeo_system_DeviceInfo_t *)out_data;
    if (strcmp(name, "deviceId") == 0){
        value->typeref.ref = (uintptr_t)&di->deviceId;
        value->typeref.ucbs = &_deviceid_ucbs;
        
    } else if (strcmp(name, "manufacturer") == 0){
        di->manufacturer = strdup(value->string_val);
    } else if (strcmp(name, "modelName") == 0){
        di->modelName = strdup(value->string_val);
    } else if (strcmp(name, "productClass") == 0){
        di->productClass = strdup(value->string_val);
    }
     else if (strcmp(name, "serialNumber") == 0){
        di->serialNumber = strdup(value->string_val);
    }
     else if (strcmp(name, "hardwareVersion") == 0){
        di->hardwareVersion = strdup(value->string_val);
    }
     else if (strcmp(name, "softwareVersion") == 0){
        di->softwareVersion = strdup(value->string_val);
    }
     else if (strcmp(name, "userFriendlyName") == 0){
        di->userFriendlyName = strdup(value->string_val);
    }
     else if (strcmp(name, "configURL") == 0){
        di->configURL = strdup(value->string_val);
    
    } else {
        qeo_log_e("unknown field %s", name);
        return false;
    }

    return true;
}

const static qeo_t2d_unmarshal_cbs_t _di_ucbs = {
    .set_val_cb = deviceInfo_set_val_cb,
};

static org_qeo_system_DeviceInfo_t _rcvd_di;
static void on_di_data_available(const qeocore_reader_t *reader,
                                            const qeocore_data_t *data,
                                            uintptr_t userdata){

    
    qeo_retcode_t ret = QEO_OK;
    switch (qeocore_data_get_status(data)) {
        case QEOCORE_NOTIFY:
            qeo_log_d("Notify received");
            break;
        case QEOCORE_DATA:
            qeo_log_d("Data received");
            ret = qeo_walk_tsm_for_unmarshal(_types, org_qeo_system_DeviceInfo_type, data, (uintptr_t)&_rcvd_di, QEO_T2D_FLAGS_ALL, &_di_ucbs);
            break;
        case QEOCORE_NO_MORE_DATA:
            qeo_log_d("No more data received");
            break;
        case QEOCORE_REMOVE:
            qeo_log_d("remove received");
            break;
        case QEOCORE_ERROR:
            qeo_log_e("no callback called due to prior error");
            break;
    }
    ck_assert_int_eq(ret, QEO_OK);

}

const static qeo_t2d_marshal_cbs_t _di_mcbs = {
    .get_val_cb = deviceinfo_get_val_cb
};

const static qeocore_reader_listener_t _di_rlistener = {
    .on_data = on_di_data_available,
    .on_policy_update = NULL,
    .userdata = 0xdeadbabe
};

static bool di_hash_cb(uintptr_t userdata, const char *name, CDR_TypeCode_t type){

    struct hash_s *hash = (struct hash_s *)userdata;
    const org_qeo_system_DeviceInfo_t *di = (const org_qeo_system_DeviceInfo_t *)hash->dataptr;

    if (strcmp(name, "deviceId") == 0){
        hash->hash *= 17; 
        hash->hash += di->deviceId.upper; 
        hash->hash *= 31; 
        hash->hash += di->deviceId.lower; 
    }

    return true;
}

/*#######################################################################
 # TESTS
 ########################################################################*/

START_TEST(simplechat)
{
    int i;
    qeo_retcode_t ret;
    qeocore_type_t *type;
    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_sample_simplechat_ChatMessage_type, &type, &_types), QEO_OK);
    ck_assert_int_ne(type, NULL);
    ck_assert_int_ne(_types, NULL);
    printf("End of registering\n");

    int8_t byte_array[] = { 3, 1 ,4, 1, 5  };

    my_enum_t enum_array[] = { BLUE, GREEN, RED };

    org_qeo_wifi_ScanListEntry_t scanlist_array[] = {
        {
            .BSSID = "00:11:22:33:44:55",
            .SSID = "QeoTestHades",
            .channel = 1,
            .RSSI = -45
        },
        {
            .BSSID = "00:a1:b2:c3:d4:e5",
            .SSID = "QeoTestLoki",
            .channel = 2,
            .RSSI = -85
        },
    };

    org_qeo_sample_simplechat_ChatMessage_t chat_msg = {
        .from = "me",
        .fromExtra.lower = 101,
        .fromExtra.upper = 434,
        .message = "hello world",
        .maincolor = GREEN
    };

    DDS_SEQ_INIT(chat_msg.extraInfo);
    dds_seq_from_array(&chat_msg.extraInfo, byte_array, sizeof(byte_array) / sizeof(byte_array[0]));


    DDS_SEQ_INIT(chat_msg.list);
    dds_seq_from_array(&chat_msg.list, scanlist_array, sizeof(scanlist_array) / sizeof(scanlist_array[0]));

    DDS_SEQ_INIT(chat_msg.colorlist);
    dds_seq_from_array(&chat_msg.colorlist, enum_array, sizeof(enum_array) / sizeof(enum_array[0]));

    /* reading */
    qeocore_reader_t *event_reader = qeocore_reader_open(_factory,
            type,
            org_qeo_sample_simplechat_ChatMessage_type[0].name,
            QEOCORE_EFLAG_EVENT_DATA | QEOCORE_EFLAG_ENABLE,
            &_sc_rlistener,
            &ret);
    ck_assert_int_ne(event_reader, NULL);

    /* writing */
    qeocore_writer_t *event_writer = qeocore_writer_open(_factory,
            type,
            org_qeo_sample_simplechat_ChatMessage_type[0].name,
            QEOCORE_EFLAG_EVENT_DATA | QEOCORE_EFLAG_ENABLE,
            NULL, /* FIX ME */
            &ret);
    qeocore_type_free(type);
    ck_assert_int_ne(event_writer, NULL);
    ck_assert_int_eq(ret, QEO_OK);

    qeocore_data_t *sample = qeocore_writer_data_new(event_writer);
    ck_assert_int_eq(qeo_walk_tsm_for_marshal(_types, org_qeo_sample_simplechat_ChatMessage_type, (uintptr_t)&chat_msg, sample, QEO_T2D_FLAGS_ALL, &_sc_mcbs), QEO_OK); 

    ck_assert_int_eq(qeocore_writer_write(event_writer, sample), QEO_OK);
    qeocore_data_free(sample);


    sleep(1);
    org_qeo_wifi_ScanListEntry_t *entry;
    DDS_SEQ_FOREACH_ENTRY (chat_msg.list, i, entry){
        ck_assert_str_eq(entry->BSSID, DDS_SEQ_ITEM_PTR(_rcvd_chatmsg.list,i)->BSSID);
        ck_assert_str_eq(entry->SSID, DDS_SEQ_ITEM_PTR(_rcvd_chatmsg.list,i)->SSID);
        ck_assert_int_eq(entry->channel, DDS_SEQ_ITEM_PTR(_rcvd_chatmsg.list,i)->channel);
        ck_assert_int_eq(entry->RSSI, DDS_SEQ_ITEM_PTR(_rcvd_chatmsg.list,i)->RSSI);
    }

    ck_assert_str_eq(_rcvd_chatmsg.from, chat_msg.from);
    ck_assert_str_eq(_rcvd_chatmsg.message, chat_msg.message);
    fail_unless(dds_seq_equal(&_rcvd_chatmsg.extraInfo, &chat_msg.extraInfo), "sequences not equal");
    ck_assert_int_eq(_rcvd_chatmsg.fromExtra.lower, chat_msg.fromExtra.lower);
    ck_assert_int_eq(_rcvd_chatmsg.fromExtra.upper, chat_msg.fromExtra.upper);
    ck_assert_int_eq(_rcvd_chatmsg.maincolor, chat_msg.maincolor);
    fail_unless(dds_seq_equal(&_rcvd_chatmsg.colorlist, &chat_msg.colorlist), "sequences not equal");


    qeocore_writer_close(event_writer);
    qeocore_reader_close(event_reader);

    ck_assert_int_eq(qeo_unregister_tsm(&_types), QEO_OK);
}
END_TEST

START_TEST(wifiassociatedstation)
{
    qeo_retcode_t ret;
    qeocore_type_t *type;
    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_wifi_AssociatedStation_type, &type, &_types), QEO_OK);
    ck_assert_int_ne(type, NULL);
    ck_assert_int_ne(_types, NULL);

    org_qeo_wifi_AssociatedStation_t as = {
        .MACAddress = "00:11:22:33:44:55",
        .BSSID = "test",
        .capabilities = "all",
        .associated = true,
        .lastSeen = 123,
        .maxNegotiatedPhyRate = 34
    };

    /* reading */
    qeocore_reader_t *state_reader = qeocore_reader_open(_factory,
            type,
            org_qeo_wifi_AssociatedStation_type[0].name,
            QEOCORE_EFLAG_STATE_DATA | QEOCORE_EFLAG_ENABLE,
            &_as_rlistener,
            &ret);
    ck_assert_int_ne(state_reader, NULL);

    /* writing */
    qeocore_writer_t *state_writer = qeocore_writer_open(_factory,
            type,
            org_qeo_wifi_AssociatedStation_type[0].name,
            QEOCORE_EFLAG_STATE_DATA | QEOCORE_EFLAG_ENABLE,
            NULL, /* FIX ME */
            &ret);
    ck_assert_int_ne(state_writer, NULL);
    ck_assert_int_eq(ret, QEO_OK);
    qeocore_type_free(type);

    qeocore_data_t *sample = qeocore_writer_data_new(state_writer);
    ck_assert_int_eq(qeo_walk_tsm_for_marshal(_types, org_qeo_wifi_AssociatedStation_type, (uintptr_t)&as, sample, QEO_T2D_FLAGS_ALL, &_as_mcbs), QEO_OK); 

    ck_assert_int_eq(qeocore_writer_write(state_writer, sample), QEO_OK);
    qeocore_data_free(sample);


    sleep(1);
    ck_assert_str_eq(_rcvd_as.MACAddress, as.MACAddress);
    ck_assert_str_eq(_rcvd_as.BSSID, as.BSSID);
    ck_assert_str_eq(_rcvd_as.capabilities, as.capabilities);
    ck_assert_int_eq(_rcvd_as.associated, as.associated);
    ck_assert_int_eq(_rcvd_as.lastSeen, as.lastSeen);
    ck_assert_int_eq(_rcvd_as.maxNegotiatedPhyRate, as.maxNegotiatedPhyRate);


    qeocore_writer_close(state_writer);
    qeocore_reader_close(state_reader);
    ck_assert_int_eq(qeo_unregister_tsm(&_types), QEO_OK);
}
END_TEST

START_TEST(deviceinfo)
{
    qeo_retcode_t ret;
    qeocore_type_t *type;
    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_system_DeviceInfo_type, &type, &_types), QEO_OK);
    ck_assert_int_ne(type, NULL);
    ck_assert_int_ne(_types, NULL);

    org_qeo_system_DeviceInfo_t di = {
        .deviceId.lower = 101,
        .deviceId.upper = 101,
        .manufacturer = "acme",
        .modelName = "mymodel",
        .productClass = "tablet",
        .serialNumber = "919191",
        .hardwareVersion = "v1.0",
        .softwareVersion = "v2.0",
        .userFriendlyName = "my cool device",
        .configURL = "http://"
    };

    org_qeo_system_DeviceInfo_t dieq = {
        .deviceId.lower = 101,
        .deviceId.upper = 101,
        .manufacturer = "acme",
        .modelName = "mymodel",
        .productClass = "tablet",
        .serialNumber = "919191",
        .hardwareVersion = "v1.0",
        .softwareVersion = "v2.0",
        .userFriendlyName = "my cool device",
        .configURL = "http://"
    };

    org_qeo_system_DeviceInfo_t dineq = {
        .deviceId.lower = 102, /* different */
        .deviceId.upper = 101,
        .manufacturer = "acme",
        .modelName = "mymodel",
        .productClass = "tablet",
        .serialNumber = "919191",
        .hardwareVersion = "v1.0",
        .softwareVersion = "v2.0",
        .userFriendlyName = "my cool device",
        .configURL = "http://"
    };

    /* reading */
    qeocore_reader_t *state_reader = qeocore_reader_open(_factory,
            type,
            org_qeo_system_DeviceInfo_type[0].name,
            QEOCORE_EFLAG_STATE_DATA | QEOCORE_EFLAG_ENABLE,
            &_di_rlistener,
            &ret);
    ck_assert_int_ne(state_reader, NULL);

    /* writing */
    qeocore_writer_t *state_writer = qeocore_writer_open(_factory,
            type,
            org_qeo_system_DeviceInfo_type[0].name,
            QEOCORE_EFLAG_STATE_DATA | QEOCORE_EFLAG_ENABLE,
            NULL, /* FIX ME */
            &ret);
    ck_assert_int_ne(state_writer, NULL);
    ck_assert_int_eq(ret, QEO_OK);
    qeocore_type_free(type);

    qeocore_data_t *sample = qeocore_writer_data_new(state_writer);
    ck_assert_int_eq(qeo_walk_tsm_for_marshal(_types, org_qeo_system_DeviceInfo_type, (uintptr_t)&di, sample, QEO_T2D_FLAGS_ALL, &_di_mcbs), QEO_OK); 

    ck_assert_int_eq(qeocore_writer_write(state_writer, sample), QEO_OK);
    qeocore_data_free(sample);


    sleep(1);
    ck_assert_int_eq(_rcvd_di.deviceId.lower, di.deviceId.lower);
    ck_assert_int_eq(_rcvd_di.deviceId.upper, di.deviceId.upper);
    ck_assert_str_eq(_rcvd_di.manufacturer, di.manufacturer);
    ck_assert_str_eq(_rcvd_di.productClass, di.productClass);
    ck_assert_str_eq(_rcvd_di.serialNumber, di.serialNumber);
    ck_assert_str_eq(_rcvd_di.hardwareVersion, di.hardwareVersion);
    ck_assert_str_eq(_rcvd_di.softwareVersion, di.softwareVersion);
    ck_assert_str_eq(_rcvd_di.userFriendlyName, di.userFriendlyName);
    ck_assert_str_eq(_rcvd_di.configURL, di.configURL);


    struct hash_s hashdi = {
        .hash = 1,
        .dataptr = (uintptr_t)&di
    };
    struct hash_s hashdieq = {
        .hash = 1,
        .dataptr = (uintptr_t)&dieq
    };   
    struct hash_s hashneq = {
        .hash = 1,
        .dataptr = (uintptr_t)&dineq
    };
    struct hash_s hashrcvdeq = {
        .hash = 1,
        .dataptr = (uintptr_t)&_rcvd_di
    };
    ret = qeo_walk_tsm_generic(org_qeo_system_DeviceInfo_type, (uintptr_t)&hashdi, QEO_T2D_FLAGS_KEY, di_hash_cb);
    ck_assert_int_eq(ret, QEO_OK);
    ret = qeo_walk_tsm_generic(org_qeo_system_DeviceInfo_type, (uintptr_t)&hashdieq, QEO_T2D_FLAGS_KEY, di_hash_cb);
    ck_assert_int_eq(ret, QEO_OK);
    ret = qeo_walk_tsm_generic(org_qeo_system_DeviceInfo_type, (uintptr_t)&hashneq, QEO_T2D_FLAGS_KEY, di_hash_cb);
    ck_assert_int_eq(ret, QEO_OK);
    ret = qeo_walk_tsm_generic(org_qeo_system_DeviceInfo_type, (uintptr_t)&hashrcvdeq, QEO_T2D_FLAGS_KEY, di_hash_cb);
    ck_assert_int_eq(ret, QEO_OK);

    ck_assert_int_eq(hashdi.hash, hashdieq.hash);
    ck_assert_int_eq(hashdi.hash, hashrcvdeq.hash);
    ck_assert_int_ne(hashdi.hash, hashneq.hash);

    qeocore_writer_close(state_writer);
    qeocore_reader_close(state_reader);
    ck_assert_int_eq(qeo_unregister_tsm(&_types), QEO_OK);
}
END_TEST


START_TEST(wall)
{
    qeocore_type_t *type;
    qeo_tsm_dynamic_type_hndl_t types;
    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_sample_note_Wall_type, &type, &types), QEO_OK);
    qeocore_type_free(type);
    ck_assert_int_ne(type, NULL);
    ck_assert_int_ne(types, NULL);
    ck_assert_int_eq(qeo_unregister_tsm(&types), QEO_OK);
    ck_assert_int_eq(types, NULL);
}
END_TEST

START_TEST(scanlist){

    qeocore_type_t *type;
    qeo_tsm_dynamic_type_hndl_t types;

    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_wifi_ScanList_type, &type, &types), QEO_OK);
    qeocore_type_free(type);
    ck_assert_int_eq(qeo_unregister_tsm(&types), QEO_OK);


}
END_TEST

START_TEST(deviceinfood){

    qeocore_type_t *type;
    qeo_tsm_dynamic_type_hndl_t types;
    ck_assert_int_eq(qeo_register_tsm(_factory, org_qeo_system_DeviceInfo_type, &type, &types), QEO_EINVAL);
    ck_assert_int_eq(type, NULL);
    ck_assert_int_eq(types, NULL);

}
END_TEST
/* ===[ test setup ]========================================================= */

static singleTestCaseInfo tests[] =
{
    /* public API */
    { .name = "simplechat", .function = simplechat },
    { .name = "wall", .function = wall },
    { .name = "scanlist", .function = scanlist },
    { .name = "wifi endtobegin", .function = wifi_datamodel_endtobegin },
    { .name = "wifi begintoend", .function = wifi_datamodel_begintoend },
    { .name = "deviceinfo", .function = deviceinfo },
    { .name = "associatedstation", .function = wifiassociatedstation },
    {NULL},
};

static singleTestCaseInfo odtests[] =
{
    /* public API */
    { .name = "deviceinfood", .function = deviceinfood },
    {NULL},
};

static void create_factory(){

    _factory = qeo_factory_create();
    sleep(1); /* to prevent interleaved traces */

}

static void create_odfactory(){

    _factory = qeo_factory_create_by_id(QEO_IDENTITY_OPEN);
    sleep(1); /* to prevent interleaved traces */

}

static void close_factory(){

    qeo_factory_close(_factory);
    printf("-----------------------------\r\n");

}

void register_type_support_tests(Suite *s)
{
    TCase *tc = tcase_create("normal tests");
    tcase_addtests(tc, tests);
    tcase_add_checked_fixture(tc, create_factory, close_factory);
    suite_add_tcase (s, tc);

    tc = tcase_create("open domain tests");
    tcase_addtests(tc, odtests);
    tcase_add_checked_fixture(tc, create_odfactory, close_factory);
    suite_add_tcase (s, tc);
}

static testCaseInfo testcases[] =
{
    { .register_testcase = register_type_support_tests, .name = "data" },
    {NULL}
};

static testSuiteInfo testsuite =
{
        .name = "LOG",
        .desc = "LOG tests",
};

/* called before every test case starts */
static void init_tcase(void)
{

}

/* called after every test case finishes */
static void fini_tcase(void)
{

}

__attribute__((constructor))
void my_init(void)
{
    register_testsuite(&testsuite, testcases, init_tcase, fini_tcase);
}
