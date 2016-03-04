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

/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#ifndef QDM_TYPEWITHRECURRINGSTRUCTS_H_
#define QDM_TYPEWITHRECURRINGSTRUCTS_H_

#include <qeo/types.h>

#ifdef __cplusplus
extern "C"
{
#endif


typedef struct {
    char * msubstring;
    int32_t msubint32;
} org_qeo_dynamic_qdm_test_Substruct1_t;
extern const DDS_TypeSupport_meta org_qeo_dynamic_qdm_test_Substruct1_type[];

DDS_SEQUENCE(org_qeo_dynamic_qdm_test_Substruct1_t, org_qeo_dynamic_qdm_test_Substruct2_msubstruct1_seq);
typedef struct {
    int16_t msubshort;
    char * msubstring;
    org_qeo_dynamic_qdm_test_Substruct2_msubstruct1_seq msubstruct1;
} org_qeo_dynamic_qdm_test_Substruct2_t;
extern const DDS_TypeSupport_meta org_qeo_dynamic_qdm_test_Substruct2_type[];

DDS_SEQUENCE(org_qeo_dynamic_qdm_test_Substruct2_t, org_qeo_dynamic_qdm_test_Substruct3_msubstruct2_seq);
DDS_SEQUENCE(org_qeo_dynamic_qdm_test_Substruct1_t, org_qeo_dynamic_qdm_test_Substruct3_msubstruct1_seq);
typedef struct {
    char * msubstring;
    org_qeo_dynamic_qdm_test_Substruct3_msubstruct2_seq msubstruct2;
    org_qeo_dynamic_qdm_test_Substruct3_msubstruct1_seq msubstruct1;
    float msubfloat;
} org_qeo_dynamic_qdm_test_Substruct3_t;
extern const DDS_TypeSupport_meta org_qeo_dynamic_qdm_test_Substruct3_type[];

DDS_SEQUENCE(org_qeo_dynamic_qdm_test_Substruct1_t, org_qeo_dynamic_qdm_test_House_msubstruct1_seq);
DDS_SEQUENCE(org_qeo_dynamic_qdm_test_Substruct3_t, org_qeo_dynamic_qdm_test_House_msubstruct3_seq);
DDS_SEQUENCE(org_qeo_dynamic_qdm_test_Substruct2_t, org_qeo_dynamic_qdm_test_House_msubstruct2_seq);
typedef struct {
    org_qeo_dynamic_qdm_test_House_msubstruct1_seq msubstruct1;
    org_qeo_dynamic_qdm_test_House_msubstruct3_seq msubstruct3;
    org_qeo_dynamic_qdm_test_House_msubstruct2_seq msubstruct2;
    float mfloat32;
    char * mstring;
} org_qeo_dynamic_qdm_test_House_t;
extern const DDS_TypeSupport_meta org_qeo_dynamic_qdm_test_House_type[];

#ifdef __cplusplus
}
#endif

#endif /* QDM_TYPEWITHRECURRINGSTRUCTS_H_ */

