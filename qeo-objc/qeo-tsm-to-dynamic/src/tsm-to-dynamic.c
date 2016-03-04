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


/*#######################################################################
 # HEADER (INCLUDE) SECTION                                              #
 ########################################################################*/
#include "tsm-to-dynamic.h"
#include "qeocore/api.h"
#include "qeocore/dyntype.h"
#include "dds/dds_seq.h"
#include "qeo/log.h"
#include <stdio.h>
#include <stdlib.h>
#include <utlist.h>

#include <assert.h>
/*#######################################################################
 # TYPES SECTION                                                         #
 ########################################################################*/
#ifdef XTYPES_USED
#define	LONGDOUBLE
#endif

struct qeo_tsm_dynamic_type_s {
    const DDS_TypeSupport_meta *tsm; /* can not be NULL, points to tsm[0] */
    qeocore_member_id_t *members; /* array with exactly tsm->nelem members */
    struct qeo_tsm_dynamic_type_s *next;
};

/*#######################################################################
 # STATIC FUNCTION DECLARATION                                           #
 ########################################################################*/
static qeocore_type_t *register_struct(qeo_factory_t *factory, const DDS_TypeSupport_meta *tsm, qeo_tsm_dynamic_type_hndl_t *types);
static qeocore_type_t *register_sequence(qeo_factory_t *factory, const DDS_TypeSupport_meta *tsm, qeo_tsm_dynamic_type_hndl_t *types);
static qeocore_type_t *register_tsm(qeo_factory_t *factory, const DDS_TypeSupport_meta *tsm, qeo_tsm_dynamic_type_hndl_t *types);
static qeocore_type_t *register_enum(qeo_factory_t *factory, const DDS_TypeSupport_meta *tsm, qeo_tsm_dynamic_type_hndl_t *types);
static bool process_field(const DDS_TypeSupport_meta *tsm, qeo_t2d_flags_t flags);

static qeo_retcode_t walk_struct_for_marshaling(const DDS_TypeSupport_meta tsm[], qeo_tsm_dynamic_type_hndl_t types, uintptr_t in_data, qeocore_data_t *out_data, qeo_t2d_flags_t flags, const qeo_t2d_marshal_cbs_t *mcbs);
static qeo_retcode_t walk_sequence_for_marshaling(const DDS_TypeSupport_meta tsm[], qeo_tsm_dynamic_type_hndl_t types, const struct seq_data_s *seq_data, qeocore_data_t *out_data, qeo_t2d_flags_t flags, const qeo_t2d_marshal_cbs_t *mcbs);
static qeo_retcode_t walk_struct_for_unmarshaling(const DDS_TypeSupport_meta tsm[], qeo_tsm_dynamic_type_hndl_t types, const qeocore_data_t *in_data, uintptr_t out_data, qeo_t2d_flags_t flags, const qeo_t2d_unmarshal_cbs_t *ucbs);
static qeo_retcode_t walk_sequence_for_unmarshaling(const DDS_TypeSupport_meta tsm[], qeo_tsm_dynamic_type_hndl_t types, qeo_sequence_t *seq, uintptr_t out_data, qeo_t2d_flags_t flags, const qeo_t2d_unmarshal_cbs_t *ucbs);
/*#######################################################################
 # STATIC VARIABLE SECTION                                               #
 ########################################################################*/
/*#######################################################################
 # STATIC FUNCTION IMPLEMENTATION                                        #
 ########################################################################*/
static qeocore_type_t *register_sequence(qeo_factory_t *factory, const DDS_TypeSupport_meta *tsm, qeo_tsm_dynamic_type_hndl_t *types){

    qeocore_type_t *seq_type = register_tsm(factory, tsm, types);

    if (seq_type == NULL){
        return NULL;
    }
    
    qeocore_type_t *t = qeocore_type_sequence_new(seq_type);
    qeocore_type_free(seq_type);
    return t;
    
}

static qeocore_type_t *register_enum(qeo_factory_t *factory, const DDS_TypeSupport_meta *tsm, qeo_tsm_dynamic_type_hndl_t *types){

    const DDS_TypeSupport_meta *tsm_it = &tsm[1];
    qeocore_enum_constants_t vals = DDS_SEQ_INITIALIZER(qeocore_enum_constant_t);
    qeocore_type_t *enum_type;
    DDS_ReturnCode_t ddsret;

    if ((ddsret = dds_seq_require(&vals, tsm->nelem)) != DDS_RETCODE_OK){
        qeo_log_e("dds_seq_require failed (%d)", ddsret);
        return NULL;
    }
    
    for (int elem = 0;  elem < tsm->nelem; ++elem, ++tsm_it ){
        DDS_SEQ_ITEM(vals, elem).name = (char *)tsm_it->name;
    }

    enum_type = qeocore_type_enum_new(tsm->name, &vals);
    if (enum_type == NULL){
        qeo_log_e("Cannot register enum");
    }
    dds_seq_cleanup(&vals);
    
    return enum_type;
}

static qeocore_type_t *register_typeref(qeo_factory_t *factory, const DDS_TypeSupport_meta *tsm, qeo_tsm_dynamic_type_hndl_t *types){

    if (tsm->tc == CDR_TYPECODE_STRUCT){
        return register_struct(factory, tsm, types);

    } else if (tsm->tc == CDR_TYPECODE_ENUM){
        return register_enum(factory, tsm, types);

    } else {

        assert(false);
        return NULL;
    }

    return NULL;
}


static qeocore_type_t *register_tsm(qeo_factory_t *factory, const DDS_TypeSupport_meta *tsm, qeo_tsm_dynamic_type_hndl_t *types){

    switch (tsm->tc){
        case CDR_TYPECODE_SHORT:
            return qeocore_type_primitive_new(QEOCORE_TYPECODE_INT16);
        case CDR_TYPECODE_LONG:
            return qeocore_type_primitive_new(QEOCORE_TYPECODE_INT32);
        case CDR_TYPECODE_LONGLONG:
            return qeocore_type_primitive_new(QEOCORE_TYPECODE_INT64);
        case CDR_TYPECODE_FLOAT:
            return qeocore_type_primitive_new(QEOCORE_TYPECODE_FLOAT32);
        case CDR_TYPECODE_BOOLEAN:
            return qeocore_type_primitive_new(QEOCORE_TYPECODE_BOOLEAN);
        case CDR_TYPECODE_OCTET:
            return qeocore_type_primitive_new(QEOCORE_TYPECODE_INT8);
        case CDR_TYPECODE_CSTRING:
            return qeocore_type_string_new(0);
        case CDR_TYPECODE_TYPEREF:
            {
                qeocore_type_t *t = register_typeref(factory, tsm->tsm, types);
                if (t == NULL){
                    qeo_log_e("Cannot register typeref");
                    return NULL;
                }
                
                return t;
            }
            break;
        case CDR_TYPECODE_SEQUENCE:
            return register_sequence(factory, &tsm[1], types); /* we have to look beyond */


        case CDR_TYPECODE_TYPE:
        case CDR_TYPECODE_ENUM:
        case CDR_TYPECODE_STRUCT:
        case CDR_TYPECODE_UNKNOWN:
        case CDR_TYPECODE_USHORT:
        case CDR_TYPECODE_ULONG:
        case CDR_TYPECODE_ULONGLONG:
        case CDR_TYPECODE_DOUBLE:
#ifdef LONGDOUBLE
        case CDR_TYPECODE_LONGDOUBLE:
#endif
        case CDR_TYPECODE_FIXED:
        case CDR_TYPECODE_CHAR:
        case CDR_TYPECODE_WCHAR:
        case CDR_TYPECODE_WSTRING:
        case CDR_TYPECODE_UNION:
        case CDR_TYPECODE_ARRAY:
        case CDR_TYPECODE_MAX:
            qeo_log_e("This type (%d) is currently not supported", tsm->tc);
            return NULL;

    }

    return NULL;


}

static qeocore_type_t *register_struct(qeo_factory_t *factory, const DDS_TypeSupport_meta *tsm, qeo_tsm_dynamic_type_hndl_t *types){

    assert(tsm->tc == CDR_TYPECODE_STRUCT);

    qeo_retcode_t ret;
    qeocore_type_t *struct_type = NULL;

    struct_type = qeocore_type_struct_new(tsm->name);
    if (struct_type == NULL){
        qeo_log_e("Could not register new struct (%s)", tsm->name);
        return NULL;
    }

    qeo_tsm_dynamic_type_hndl_t entry = calloc(1, sizeof(*entry));
    if (entry == NULL){
        return NULL;
    }
    entry->members = malloc(sizeof(qeocore_member_id_t) * tsm->nelem);
    if (entry->members == NULL){
        return NULL;
    }
    entry->tsm = tsm;

    const DDS_TypeSupport_meta *tsm_it = &tsm[1];
    
    for (int elem = 0;  elem < tsm->nelem; ++elem, ++tsm_it ){

        qeocore_type_t *mtype = register_tsm(factory, tsm_it, types);
        if (mtype == NULL){
            qeo_log_e("Could not register member %s", tsm_it->name);
            return NULL;
        }

        //qeo_log_d("adding member %s", tsm_it->name);
        entry->members[elem] = QEOCORE_MEMBER_ID_DEFAULT;
        if ((ret = qeocore_type_struct_add(struct_type, mtype, tsm_it->name, &entry->members[elem], (tsm_it->flags & TSMFLAG_KEY) ? QEOCORE_FLAG_KEY : QEOCORE_FLAG_NONE)) != QEO_OK){
            qeo_log_e("qeocore_type_struct_add failed (%s.%s) %d", tsm->name, tsm_it->name, ret);
            return NULL;
        }

        qeocore_type_free(mtype);

        if (tsm_it->tc == CDR_TYPECODE_SEQUENCE){
            ++tsm_it;
        }
    }

    ret = qeocore_type_register(factory, struct_type, tsm[0].name);
    if (ret != QEO_OK){
        qeo_log_e("qeocore_type_register failed with %s", tsm[0].name);
        qeocore_type_free(struct_type);
        return NULL;
    }
        
    LL_APPEND(*types, entry);
    

    return struct_type;
}

static bool process_field(const DDS_TypeSupport_meta *tsm, qeo_t2d_flags_t flags){

    return ((tsm->flags & TSMFLAG_KEY) && (flags & QEO_T2D_FLAGS_KEY)) ||
    (!(tsm->flags & TSMFLAG_KEY) && (flags & QEO_T2D_FLAGS_NON_KEY));

}

static qeo_retcode_t walk_sequence_for_marshaling(const DDS_TypeSupport_meta tsm[], qeo_tsm_dynamic_type_hndl_t types, const struct seq_data_s *seq_data, qeocore_data_t *out_data, qeo_t2d_flags_t flags, const qeo_t2d_marshal_cbs_t *mcbs){

    qeo_t2d_types_t val = {};
    qeo_sequence_t seq;
    qeocore_data_sequence_new(out_data, &seq, seq_data->seq_size);
    dds_seq_reset(&seq); /* set length to 0 */
                    

    switch(tsm->tc){
        case CDR_TYPECODE_SHORT:
        case CDR_TYPECODE_LONG:
        case CDR_TYPECODE_LONGLONG:
        case CDR_TYPECODE_FLOAT:
        case CDR_TYPECODE_BOOLEAN:
        case CDR_TYPECODE_OCTET:
        case CDR_TYPECODE_CSTRING:
            for (int j = 0; j < seq_data->seq_size; ++j){
                if (mcbs->get_seq_val_cb(seq_data->seq_ref, j, tsm->tc, &val) == false){
                    qeo_log_e("Could not get val");
                    return QEO_EFAIL;
                }
                if (dds_seq_append(&seq, &val) != DDS_RETCODE_OK){
                    qeo_log_e("Could not append to dds sequence");
                    return QEO_EFAIL;
                }
            }
            break;

        case CDR_TYPECODE_TYPEREF:
            if (tsm->tsm[0].tc == CDR_TYPECODE_STRUCT){
                for (int j = 0; j < seq_data->seq_size; ++j){
                    const qeo_t2d_marshal_cbs_t *mcbs2 = mcbs;
                    if (mcbs->get_seq_val_cb(seq_data->seq_ref, j, tsm->tsm[0].tc, &val) == false){
                        qeo_log_e("Could not get val");
                        return QEO_EFAIL;
                    }

                    if (val.typeref.mcbs != NULL){
                        mcbs2 = val.typeref.mcbs;
                    }

                    qeocore_data_t *elemdata = ((qeocore_data_t **)DDS_SEQ_DATA(seq))[j]; 
                    if (walk_struct_for_marshaling(tsm->tsm, types, val.typeref.ref, elemdata, flags, mcbs2) != QEO_OK){
                        qeo_log_e("Could not marshal struct in sequence");
                        return QEO_EFAIL;
                    }

                    if (dds_seq_append(&seq, &elemdata) != DDS_RETCODE_OK){
                        qeo_log_e("Could not append to dds sequence");
                        return QEO_EFAIL;
                    }
                }
            } else if (tsm->tsm[0].tc == CDR_TYPECODE_ENUM){
                for (int j = 0; j < seq_data->seq_size; ++j){
                    if (mcbs->get_seq_val_cb(seq_data->seq_ref, j, tsm->tsm[0].tc, &val) == false){
                        qeo_log_e("Could not get val");
                        return QEO_EFAIL;
                    }
                    if (dds_seq_append(&seq, &val) != DDS_RETCODE_OK){
                        qeo_log_e("Could not append to dds sequence");
                        return QEO_EFAIL;
                    }
                }
            } else {
                qeo_log_e("typeref to type %d not supported", tsm->tsm[0].tc);
                return QEO_EINVAL;
            }
            break;
        case CDR_TYPECODE_SEQUENCE:
            qeo_log_e("Sequences of sequences are currently not supported");
            return QEO_EINVAL;
            /* If you want to support this, make sure tsm_it in walk_struct is updated appropriately */
#if 0
            for (int j = 0; j < seq_data->seq_size; ++j){
                if (mcbs->get_seq_val_cb(seq_data->seq_ref, j, tsm->tc, &val) == false){
                    qeo_log_e("Could not get val");
                    return QEO_EFAIL;
                }

                if (val.seq.mcbs != NULL){
                    mcbs2 = val.seq.mcbs;
                }

                qeocore_data_t *elemdata = ((qeocore_data_t **)DDS_SEQ_DATA(seq))[j]; 
                if (walk_sequence_for_marshaling(tsm+1, factory, &val.seq, elemdata, flags, mcbs2) != QEO_OK){
                    qeo_log_e("Could not marshal struct in sequence");
                    return QEO_EFAIL;
                }

                if (dds_seq_append(&seq, &elemdata) != DDS_RETCODE_OK){
                    qeo_log_e("Could not append to dds sequence");
                    return QEO_EFAIL;
                }
            }
#endif
            break;


        case CDR_TYPECODE_TYPE:
        case CDR_TYPECODE_ENUM:
        case CDR_TYPECODE_STRUCT:
        case CDR_TYPECODE_UNKNOWN:
        case CDR_TYPECODE_USHORT:
        case CDR_TYPECODE_ULONG:
        case CDR_TYPECODE_ULONGLONG:
        case CDR_TYPECODE_DOUBLE:
#ifdef LONGDOUBLE
        case CDR_TYPECODE_LONGDOUBLE:
#endif
        case CDR_TYPECODE_FIXED:
        case CDR_TYPECODE_CHAR:
        case CDR_TYPECODE_WCHAR:
        case CDR_TYPECODE_WSTRING:
        case CDR_TYPECODE_UNION:
        case CDR_TYPECODE_ARRAY:
        case CDR_TYPECODE_MAX:
                qeo_log_e("This type (%d) for member %s is currently not supported %d", tsm->tc, "(sequence)");
                return QEO_EINVAL;
    }
                    
    qeocore_data_sequence_set(out_data, &seq, 0);
    qeocore_data_sequence_free(out_data, &seq);

    return QEO_OK;
}

static qeo_retcode_t walk_struct_for_marshaling(const DDS_TypeSupport_meta tsm[], qeo_tsm_dynamic_type_hndl_t types, uintptr_t in_data, qeocore_data_t *out_data, qeo_t2d_flags_t flags, const qeo_t2d_marshal_cbs_t *mcbs){

    
    qeo_retcode_t ret = QEO_OK;
    qeo_tsm_dynamic_type_hndl_t entry;

    LL_SEARCH_SCALAR(types, entry, tsm, tsm);

    if (entry == NULL){
        qeo_log_e("Could not find type %s", tsm->name);
        return QEO_EINVAL;
    }
    
    const DDS_TypeSupport_meta *tsm_it = &tsm[1];
    for (int elem = 0; elem < tsm->nelem; ++elem, ++tsm_it ){
        const qeo_t2d_marshal_cbs_t *mcbs2 = mcbs;
        qeocore_member_id_t member = entry->members[elem];
        qeo_t2d_types_t val = {};
        if (process_field(tsm_it, flags) == false){
            continue;
        }

        switch (tsm_it->tc){
            case CDR_TYPECODE_SHORT:
            case CDR_TYPECODE_LONG:
            case CDR_TYPECODE_LONGLONG:
            case CDR_TYPECODE_FLOAT:
            case CDR_TYPECODE_BOOLEAN:
            case CDR_TYPECODE_OCTET:
            case CDR_TYPECODE_CSTRING:
                {
                    if (mcbs2->get_val_cb(in_data, tsm_it->name, tsm_it->tc, &val) == false){
                        qeo_log_e("could not get val type");
                        ret = QEO_EFAIL;
                        break;

                    }
                    if ((ret = qeocore_data_set_member(out_data, member, (void *)&val)) != QEO_OK){
                        qeo_log_e("could not set member (%s)", tsm_it->name);
                        break;
                    }
                }
                break;
            case CDR_TYPECODE_TYPEREF:
                {
                    qeocore_data_t *subdata;
                    if (tsm_it->tsm[0].tc == CDR_TYPECODE_STRUCT){
                        if ((ret = qeocore_data_get_member(out_data, member, &subdata)) != QEO_OK){
                            qeo_log_e("could not get member (%s)", tsm_it->name);
                            break;
                        }

                        do {
                            if (mcbs->get_val_cb(in_data, tsm_it->name, tsm_it->tsm[0].tc, &val) == false){
                                qeo_log_e("could not get reference to typeref");
                                ret = QEO_EFAIL;
                                break;
                            }

                            if (val.typeref.mcbs != NULL){
                                mcbs2 = val.typeref.mcbs;
                            }

                            if ((ret = walk_struct_for_marshaling(tsm_it->tsm, types, val.typeref.ref, subdata, flags, mcbs2)) != QEO_OK){
                                break;
                            }

                            if ((ret = qeocore_data_set_member(out_data, member, &subdata)) != QEO_OK){
                                qeo_log_e("Could not set member");
                                break;
                            }

                        } while (0);

                        qeocore_data_free(subdata);
                    } else if (tsm_it->tsm[0].tc == CDR_TYPECODE_ENUM){
                        if (mcbs->get_val_cb(in_data, tsm_it->name, tsm_it->tsm[0].tc, &val) == false){
                            qeo_log_e("could not get val type");
                            ret = QEO_EFAIL;
                            break;

                        }
                        if ((ret = qeocore_data_set_member(out_data, member, (void *)&val)) != QEO_OK){
                            qeo_log_e("could not set member (%s)", tsm_it->name);
                            break;
                        }
                    } else {
                        qeo_log_e("typeref to type %d not supported", tsm_it->tsm[0].tc);
                        ret = QEO_EINVAL;
                        break;
                    }

                    if (ret != QEO_OK){
                        break;
                    }

                }
                break;
            case CDR_TYPECODE_SEQUENCE:
                {
                    qeocore_data_t *subdata;
                    if ((ret = qeocore_data_get_member(out_data, member, &subdata)) != QEO_OK){
                        qeo_log_e("could not get member");
                        break;
                    }

                    do {
                        if (mcbs2->get_val_cb(in_data, tsm_it->name, tsm_it->tc, &val) == false){
                            qeo_log_e("Could not get val");
                            ret = QEO_EFAIL;
                            break;
                        }

                        if (val.typeref.mcbs != NULL){
                            mcbs2 = val.seq.mcbs;
                        }

                        if ((ret = walk_sequence_for_marshaling(tsm_it + 1, types, &val.seq, subdata, flags, mcbs2)) != QEO_OK){
                            qeo_log_e("Could not walk sequence");
                            break;
                        }

                        if ((ret = qeocore_data_set_member(out_data, member, &subdata)) != QEO_OK){
                            qeo_log_e("Could not set member");
                            break;
                        }

                        
                        ret = QEO_OK;
                    } while (0);

                    qeocore_data_free(subdata);

                    if (ret != QEO_OK){
                        break;
                    }

                    ++tsm_it;
                }

                break;

            case CDR_TYPECODE_TYPE:
            case CDR_TYPECODE_ENUM:
            case CDR_TYPECODE_STRUCT:
            case CDR_TYPECODE_UNKNOWN:
            case CDR_TYPECODE_USHORT:
            case CDR_TYPECODE_ULONG:
            case CDR_TYPECODE_ULONGLONG:
            case CDR_TYPECODE_DOUBLE:
#ifdef LONGDOUBLE
            case CDR_TYPECODE_LONGDOUBLE:
#endif
            case CDR_TYPECODE_FIXED:
            case CDR_TYPECODE_CHAR:
            case CDR_TYPECODE_WCHAR:
            case CDR_TYPECODE_WSTRING:
            case CDR_TYPECODE_UNION:
            case CDR_TYPECODE_ARRAY:
            case CDR_TYPECODE_MAX:
                qeo_log_e("This type (%d) for member %s is currently not supported", tsm_it->tc, tsm_it->name);
                ret = QEO_EINVAL;

        }
        
        if (ret != QEO_OK){
            break;
        }
    }


    return ret;
}

static qeo_retcode_t walk_sequence_for_unmarshaling(const DDS_TypeSupport_meta tsm[], qeo_tsm_dynamic_type_hndl_t types, qeo_sequence_t *seq, uintptr_t out_data, qeo_t2d_flags_t flags, const qeo_t2d_unmarshal_cbs_t *ucbs){
    size_t len = DDS_SEQ_LENGTH(*seq);
                    
    qeo_retcode_t ret = QEO_OK;

    for (int j = 0; j < len; ++j){
        switch(tsm->tc){
            case CDR_TYPECODE_SHORT:
            case CDR_TYPECODE_LONG:
            case CDR_TYPECODE_LONGLONG:
            case CDR_TYPECODE_FLOAT:
            case CDR_TYPECODE_BOOLEAN:
            case CDR_TYPECODE_OCTET:
            case CDR_TYPECODE_CSTRING:
            {
                /* We cannot use DDS_SEQ_ITEM_PTR as pointer arithmetic does not work on void* 
                 * (in fact, only GCC complains about it, clang is 'fine' with it for some reason)  */
                uintptr_t ptr = (uintptr_t)(DDS_SEQ_DATA(*seq) + (ptrdiff_t)(j * DDS_SEQ_ELEM_SIZE(*seq)));
                if (ucbs->set_seq_val_cb(out_data, j, tsm->tc, (qeo_t2d_types_t *)ptr) == false){
                    qeo_log_e("Could not get val");
                    return QEO_EFAIL;
                }
                break;
            }
            case CDR_TYPECODE_TYPEREF:
                {
                    if (tsm->tsm[0].tc == CDR_TYPECODE_STRUCT){
                        const qeo_t2d_unmarshal_cbs_t *ucbs2 = ucbs;
                        qeo_t2d_types_t val = {};
                        val.typeref.name = tsm->tsm[0].name;
                        qeocore_data_t *subdata = ((qeocore_data_t **)DDS_SEQ_DATA(*seq))[j];

                        if (ucbs->set_seq_val_cb(out_data, j, tsm->tsm[0].tc, &val) == false){
                            qeo_log_e("could not set member");
                            return QEO_EFAIL;
                        }

                        if (val.typeref.ucbs != NULL){
                            ucbs2 = val.typeref.ucbs;
                        }

                        if ((ret = walk_struct_for_unmarshaling(tsm->tsm, types, subdata, val.typeref.ref, flags, ucbs2)) != QEO_OK){
                            qeo_log_e("could not walk struct for unmarshal");
                            return ret;
                        }
                    } else if (tsm->tsm[0].tc == CDR_TYPECODE_ENUM){
                        /* We cannot use DDS_SEQ_ITEM_PTR as pointer arithmetic does not work on void* 
                         * (in fact, only GCC complains about it, clang is 'fine' with it for some reason)  */
                        uintptr_t ptr = (uintptr_t)(DDS_SEQ_DATA(*seq) + (ptrdiff_t)(j * DDS_SEQ_ELEM_SIZE(*seq)));
                        if (ucbs->set_seq_val_cb(out_data, j, tsm->tsm[0].tc, (qeo_t2d_types_t *)ptr) == false){
                            qeo_log_e("Could not get val");
                            return QEO_EFAIL;
                        }
                        break;

                    } else {
                        qeo_log_e("typeref to type %d not supported", tsm->tsm[0].tc);
                        return QEO_EINVAL;
                    }

                }
                break;
            case CDR_TYPECODE_SEQUENCE:
                qeo_log_e("Sequences of sequences are currently not supported");
                return QEO_EINVAL;
                break;

            case CDR_TYPECODE_TYPE:
            case CDR_TYPECODE_ENUM:
            case CDR_TYPECODE_STRUCT:
            case CDR_TYPECODE_UNKNOWN:
            case CDR_TYPECODE_USHORT:
            case CDR_TYPECODE_ULONG:
            case CDR_TYPECODE_ULONGLONG:
            case CDR_TYPECODE_DOUBLE:
#ifdef LONGDOUBLE
            case CDR_TYPECODE_LONGDOUBLE:
#endif
            case CDR_TYPECODE_FIXED:
            case CDR_TYPECODE_CHAR:
            case CDR_TYPECODE_WCHAR:
            case CDR_TYPECODE_WSTRING:
            case CDR_TYPECODE_UNION:
            case CDR_TYPECODE_ARRAY:
            case CDR_TYPECODE_MAX:
                qeo_log_e("This type (%d) for member %s is currently not supported %d", tsm->tc, "(sequence)");
                return QEO_EINVAL;
        }
    }
                    
    return QEO_OK;

}

static qeo_retcode_t walk_struct_for_unmarshaling(const DDS_TypeSupport_meta tsm[], qeo_tsm_dynamic_type_hndl_t types, const qeocore_data_t *in_data, uintptr_t out_data, qeo_t2d_flags_t flags, const qeo_t2d_unmarshal_cbs_t *ucbs){

    qeo_retcode_t ret = QEO_OK;
    qeo_tsm_dynamic_type_hndl_t entry;

    LL_SEARCH_SCALAR(types, entry, tsm, tsm);

    if (entry == NULL){
        qeo_log_e("Could not find type %s", tsm->name);
        return QEO_EINVAL;
    }

    const DDS_TypeSupport_meta *tsm_it = &tsm[1];
    for (int elem = 0; elem < tsm->nelem; ++elem, ++tsm_it ){
        qeo_t2d_types_t val = {};
        qeocore_member_id_t member = entry->members[elem];

        if (process_field(tsm_it, flags) == false){
            continue;
        }

        switch (tsm_it->tc){
            case CDR_TYPECODE_SHORT:
            case CDR_TYPECODE_LONG:
            case CDR_TYPECODE_LONGLONG:
            case CDR_TYPECODE_FLOAT:
            case CDR_TYPECODE_BOOLEAN:
            case CDR_TYPECODE_OCTET:
            case CDR_TYPECODE_CSTRING:
                {
                    if ((ret = qeocore_data_get_member(in_data, member, &val)) != QEO_OK){
                        qeo_log_e("could not get member (%s)", tsm_it->name);
                        break;
                    }

                    if (ucbs->set_val_cb(out_data, tsm_it->name, tsm_it->tc, &val) == false){
                        qeo_log_e("could not set member");
                        break;
                    }
					if (CDR_TYPECODE_CSTRING == tsm_it->tc) {
						free(val.string_val);
					}
                }
                break;
            case CDR_TYPECODE_TYPEREF:
                {
                    if (tsm_it->tsm[0].tc == CDR_TYPECODE_STRUCT){
                        qeocore_data_t *subdata;
                        const qeo_t2d_unmarshal_cbs_t *ucbs2 = ucbs;

                        if ((ret = qeocore_data_get_member(in_data, member, &subdata)) != QEO_OK){
                            qeo_log_e("could not get member");
                            break;
                        }

                        val.typeref.name = tsm_it->tsm[0].name;
                        if (ucbs->set_val_cb(out_data, tsm_it->name, tsm_it->tsm[0].tc, &val) == false){
                            qeo_log_e("could not set member");
                            break;
                        }

                        if (val.typeref.ucbs != NULL){
                            ucbs2 = val.typeref.ucbs;
                        }

                        if ((ret = walk_struct_for_unmarshaling(tsm_it->tsm, types, subdata, val.typeref.ref, flags, ucbs2)) != QEO_OK){
                            qeo_log_e("could not walk struct for unmarshal");
                            break;
                        }

                        qeocore_data_free(subdata);
                    } else if (tsm_it->tsm[0].tc == CDR_TYPECODE_ENUM){
                        if ((ret = qeocore_data_get_member(in_data, member, &val)) != QEO_OK){
                            qeo_log_e("could not get member (%s)", tsm_it->name);
                            break;
                        }

                        if (ucbs->set_val_cb(out_data, tsm_it->name, tsm_it->tsm[0].tc, &val) == false){
                            qeo_log_e("could not set member");
                            break;
                        }
                    } else {
                        qeo_log_e("typeref to type %d not supported", tsm_it->tsm[0].tc);
                        return QEO_EINVAL;
                    }
                }
                break;
            case CDR_TYPECODE_SEQUENCE:
                {
                    qeocore_data_t *subdata;
                    const qeo_t2d_unmarshal_cbs_t *ucbs2 = ucbs;
                    qeo_sequence_t seq;

                    if ((ret = qeocore_data_get_member(in_data, member, &subdata)) != QEO_OK){
                        qeo_log_e("could not get member");
                        break;
                    }

                    if ((ret = qeocore_data_sequence_get(subdata, &seq, 0, QEOCORE_SIZE_UNLIMITED)) != QEO_OK){
                        qeo_log_e("could not get sequence");
                        break;
                    }
                    val.seq.seq_size = DDS_SEQ_LENGTH(seq);

                    if (ucbs2->set_val_cb(out_data, tsm_it->name, tsm_it->tc, &val) == false){
                        qeo_log_e("could not set member");
                        ret = QEO_EFAIL;
                        break;
                    }

                    if (val.seq.ucbs != NULL){
                        ucbs2 = val.seq.ucbs;
                    }

                    if ((ret = walk_sequence_for_unmarshaling(tsm_it+1, types, &seq, val.seq.seq_ref, flags, ucbs2)) != QEO_OK){
                        qeo_log_e("could not unmarshal sequence");
                        break;
                    }

                    if ((ret = qeocore_data_sequence_free(subdata, &seq)) != QEO_OK){
                        qeo_log_e("could not free sequence");
                        break;
                    }

                    qeocore_data_free(subdata);
                    ++tsm_it;

                }

                break;

            case CDR_TYPECODE_TYPE:
            case CDR_TYPECODE_ENUM:
            case CDR_TYPECODE_STRUCT:
            case CDR_TYPECODE_UNKNOWN:
            case CDR_TYPECODE_USHORT:
            case CDR_TYPECODE_ULONG:
            case CDR_TYPECODE_ULONGLONG:
            case CDR_TYPECODE_DOUBLE:
#ifdef LONGDOUBLE
            case CDR_TYPECODE_LONGDOUBLE:
#endif
            case CDR_TYPECODE_FIXED:
            case CDR_TYPECODE_CHAR:
            case CDR_TYPECODE_WCHAR:
            case CDR_TYPECODE_WSTRING:
            case CDR_TYPECODE_UNION:
            case CDR_TYPECODE_ARRAY:
            case CDR_TYPECODE_MAX:
                qeo_log_e("This type (%d) for member %s is currently not supported %d", tsm_it->tc, tsm_it->name);
                ret = QEO_EINVAL;
                break;

        }

        if (ret != QEO_OK){
            break;
        }
    }

    return ret;

}

/*#######################################################################
 # PUBLIC FUNCTION IMPLEMENTATION                                        #
 ########################################################################*/
qeo_retcode_t qeo_register_tsm(qeo_factory_t *factory, const DDS_TypeSupport_meta tsm[], qeocore_type_t **qeotype, qeo_tsm_dynamic_type_hndl_t *types){

    if (factory == NULL || tsm == NULL || qeotype == NULL || types == NULL){
        return QEO_EINVAL;
    }

    if (tsm[0].tc != CDR_TYPECODE_STRUCT){
        qeo_log_e("First entry should be struct (is now %d) (did you set XTYPES_USED ?)", tsm[0].tc);
        return QEO_EINVAL;
    }
    
    *types = NULL;
    if ((*qeotype = register_struct(factory, tsm, types)) == NULL){
        qeo_unregister_tsm(types);
        qeo_log_e("Could not register struct");
        return QEO_EINVAL;
    }
    

    return QEO_OK;
}

qeo_retcode_t qeo_unregister_tsm(qeo_tsm_dynamic_type_hndl_t *types){

    qeo_tsm_dynamic_type_hndl_t el;
    qeo_tsm_dynamic_type_hndl_t tmp;

    if (*types == NULL || *types == NULL){
        return QEO_EINVAL;
    }

    LL_FOREACH_SAFE(*types, el, tmp){
        LL_DELETE(*types, el);
        free(el->members);
        free(el);
    }
    *types = NULL;
    
    return QEO_OK;
}

qeo_retcode_t qeo_walk_tsm_for_marshal(qeo_tsm_dynamic_type_hndl_t types, const DDS_TypeSupport_meta tsm[], uintptr_t in_data, qeocore_data_t *out_data, qeo_t2d_flags_t flags, const qeo_t2d_marshal_cbs_t *mcbs){

    if (types == NULL || tsm == NULL || mcbs == NULL){
        return QEO_EINVAL;
    }

    assert(tsm[0].tc == CDR_TYPECODE_STRUCT);

    return walk_struct_for_marshaling(tsm, types, in_data, out_data, flags, mcbs);
}

qeo_retcode_t qeo_walk_tsm_for_unmarshal(qeo_tsm_dynamic_type_hndl_t types, const DDS_TypeSupport_meta tsm[], const qeocore_data_t *in_data, uintptr_t out_data, qeo_t2d_flags_t flags, const qeo_t2d_unmarshal_cbs_t *ucbs){
    
    
    if (types == NULL || tsm == NULL || ucbs == NULL){
        qeo_log_e("Invalid arguments");
        return QEO_EINVAL;
    }

    assert(tsm[0].tc == CDR_TYPECODE_STRUCT);

    return walk_struct_for_unmarshaling(tsm, types, in_data, out_data, flags, ucbs);



}

qeo_retcode_t qeo_walk_tsm_generic(const DDS_TypeSupport_meta tsm[], uintptr_t userdata, qeo_t2d_flags_t flags, walk_tsm_cb cb){


    qeo_retcode_t ret = QEO_OK;
    if (tsm == NULL || cb == NULL){
        return QEO_EINVAL;
    }

    const DDS_TypeSupport_meta *tsm_it = &tsm[1];
    bool cont = true;
    for (int elem = 0; elem < tsm->nelem && cont; ++elem, ++tsm_it ){
        if (process_field(tsm_it, flags) == false){
            continue;
        }

        CDR_TypeCode_t tc = tsm_it->tc;
        switch (tsm_it->tc){
            case CDR_TYPECODE_TYPEREF:
                tc = tsm_it->tsm[0].tc;
            case CDR_TYPECODE_SHORT:
            case CDR_TYPECODE_LONG:
            case CDR_TYPECODE_LONGLONG:
            case CDR_TYPECODE_FLOAT:
            case CDR_TYPECODE_BOOLEAN:
            case CDR_TYPECODE_OCTET:
            case CDR_TYPECODE_CSTRING:
            case CDR_TYPECODE_SEQUENCE:
                cont = cb(userdata, tsm_it->name, tc); 
                if (tc == CDR_TYPECODE_SEQUENCE){
                    ++tsm_it;
                }
                break;

            case CDR_TYPECODE_TYPE:
            case CDR_TYPECODE_ENUM:
            case CDR_TYPECODE_STRUCT:
            case CDR_TYPECODE_UNKNOWN:
            case CDR_TYPECODE_USHORT:
            case CDR_TYPECODE_ULONG:
            case CDR_TYPECODE_ULONGLONG:
            case CDR_TYPECODE_DOUBLE:
#ifdef LONGDOUBLE
            case CDR_TYPECODE_LONGDOUBLE:
#endif
            case CDR_TYPECODE_FIXED:
            case CDR_TYPECODE_CHAR:
            case CDR_TYPECODE_WCHAR:
            case CDR_TYPECODE_WSTRING:
            case CDR_TYPECODE_UNION:
            case CDR_TYPECODE_ARRAY:
            case CDR_TYPECODE_MAX:
                qeo_log_e("This type (%d) for member %s is currently not supported", tsm_it->tc, tsm_it->name);
                ret = QEO_EINVAL;

        }
        
        if (ret != QEO_OK){
            break;
        }

    }

    return ret;
}
