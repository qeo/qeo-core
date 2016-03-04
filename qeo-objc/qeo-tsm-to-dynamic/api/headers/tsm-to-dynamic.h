/*
 * Copyright (c) 2016 - Qeo LLC
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

#ifndef QEO_TSM_TO_DYNAMIC_H_
#define QEO_TSM_TO_DYNAMIC_H_

#include <qeo/error.h>
#include <qeocore/api.h>
#include <qeocore/dyntype.h>


#ifdef __cplusplus
extern "C"
{
#endif

#define QEO_T2D_FLAGS_KEY           0x00001
#define QEO_T2D_FLAGS_NON_KEY       0x00002
#define QEO_T2D_FLAGS_ALL           (QEO_T2D_FLAGS_KEY | QEO_T2D_FLAGS_NON_KEY)

typedef uint32_t qeo_t2d_flags_t;

typedef union qeo_t2d_types_u qeo_t2d_types_t;
typedef struct qeo_t2d_marshal_cbs_s qeo_t2d_marshal_cbs_t;
typedef struct qeo_t2d_unmarshal_cbs_s qeo_t2d_unmarshal_cbs_t;

union qeo_t2d_types_u {
    qeo_boolean_t bool_val;
    int8_t        char_val;
    int16_t       short_val;
    int32_t       long_val;
    int64_t       longlong_val;
    float         float_val;
    char         *string_val;
    qeo_enum_value_t enum_val;

    struct seq_data_s {
        uintptr_t     seq_ref; 
        size_t        seq_size;
        const qeo_t2d_marshal_cbs_t *mcbs;
        const qeo_t2d_unmarshal_cbs_t *ucbs;
    } seq;

    struct {
        uintptr_t     ref;
        const char   *name;
        const qeo_t2d_marshal_cbs_t *mcbs;
        const qeo_t2d_unmarshal_cbs_t *ucbs;
    } typeref;

};

typedef bool (*marshal_get_val_cb)(uintptr_t in_data, const char *name, CDR_TypeCode_t type, qeo_t2d_types_t *value);
typedef bool (*marshal_get_seq_val_cb)(uintptr_t in_data, int index, CDR_TypeCode_t type, qeo_t2d_types_t *value);

struct qeo_t2d_marshal_cbs_s {
    marshal_get_val_cb get_val_cb;
    marshal_get_seq_val_cb   get_seq_val_cb;
} ;


typedef bool (*unmarshal_set_val_cb)(uintptr_t out_data, const char *name, CDR_TypeCode_t type, qeo_t2d_types_t *value);
typedef bool (*unmarshal_set_seq_val_cb)(uintptr_t out_data, int index, CDR_TypeCode_t type, qeo_t2d_types_t *value);


struct qeo_t2d_unmarshal_cbs_s {
    unmarshal_set_val_cb set_val_cb;
    unmarshal_set_seq_val_cb set_seq_val_cb;

};

typedef bool (*walk_tsm_cb)(uintptr_t userdata, const char *name, CDR_TypeCode_t type);

typedef struct qeo_tsm_dynamic_type_s *qeo_tsm_dynamic_type_hndl_t;

qeo_retcode_t qeo_register_tsm(qeo_factory_t *factory, const DDS_TypeSupport_meta tsm[], qeocore_type_t **qeotype, qeo_tsm_dynamic_type_hndl_t *types);
qeo_retcode_t qeo_unregister_tsm(qeo_tsm_dynamic_type_hndl_t *types);


qeo_retcode_t qeo_walk_tsm_for_marshal(qeo_tsm_dynamic_type_hndl_t types, const DDS_TypeSupport_meta tsm[], uintptr_t in_data, qeocore_data_t *out_data, qeo_t2d_flags_t flags, const qeo_t2d_marshal_cbs_t *mcbs);
qeo_retcode_t qeo_walk_tsm_for_unmarshal(qeo_tsm_dynamic_type_hndl_t types, const DDS_TypeSupport_meta tsm[], const qeocore_data_t *in_data, uintptr_t out_data,  qeo_t2d_flags_t flags, const qeo_t2d_unmarshal_cbs_t *ucbs);

qeo_retcode_t qeo_walk_tsm_generic(const DDS_TypeSupport_meta tsm[], uintptr_t userdata, qeo_t2d_flags_t flags, walk_tsm_cb cb);


#ifdef __cplusplus
}
#endif

#endif
