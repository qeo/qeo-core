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

#ifndef QDM_STATEWITHUNBARRAYSOFUNBARRAYSKEYINT16_H_
#define QDM_STATEWITHUNBARRAYSOFUNBARRAYSKEYINT16_H_

#include <qeo/api.h>


DDS_SEQUENCE(unsigned char, org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyAliasToUnbArrayOfBoolean_t);
DDS_SEQUENCE(int8_t, org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyAliasToUnbArrayOfByte_t);
DDS_SEQUENCE(int16_t, org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyAliasToUnbArrayOfInt16_t);
DDS_SEQUENCE(int32_t, org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyAliasToUnbArrayOfInt32_t);
DDS_SEQUENCE(int64_t, org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyAliasToUnbArrayOfInt64_t);
DDS_SEQUENCE(float, org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyAliasToUnbArrayOfFloat32_t);
DDS_SEQUENCE(char *, org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyAliasToUnbArrayOfString_t);
DDS_SEQUENCE(org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyAliasToUnbArrayOfBoolean_t, org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyUnbArrayOfUnbArrayOfBoolean_seq);
DDS_SEQUENCE(org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyAliasToUnbArrayOfByte_t, org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyUnbArrayOfUnbArrayOfByte_seq);
DDS_SEQUENCE(org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyAliasToUnbArrayOfInt16_t, org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyUnbArrayOfUnbArrayOfInt16_seq);
DDS_SEQUENCE(org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyAliasToUnbArrayOfInt32_t, org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyUnbArrayOfUnbArrayOfInt32_seq);
DDS_SEQUENCE(org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyAliasToUnbArrayOfInt64_t, org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyUnbArrayOfUnbArrayOfInt64_seq);
DDS_SEQUENCE(org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyAliasToUnbArrayOfFloat32_t, org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyUnbArrayOfUnbArrayOfFloat32_seq);
DDS_SEQUENCE(org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyAliasToUnbArrayOfString_t, org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyUnbArrayOfUnbArrayOfString_seq);
/**
 * struct representing an event containing unbound arrays (sequences) of unbound arrays
 */
typedef struct {
    org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyUnbArrayOfUnbArrayOfBoolean_seq MyUnbArrayOfUnbArrayOfBoolean;
    org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyUnbArrayOfUnbArrayOfByte_seq MyUnbArrayOfUnbArrayOfByte;
    /**
     * [Key]
     */
    org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyUnbArrayOfUnbArrayOfInt16_seq MyUnbArrayOfUnbArrayOfInt16;
    org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyUnbArrayOfUnbArrayOfInt32_seq MyUnbArrayOfUnbArrayOfInt32;
    org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyUnbArrayOfUnbArrayOfInt64_seq MyUnbArrayOfUnbArrayOfInt64;
    org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyUnbArrayOfUnbArrayOfFloat32_seq MyUnbArrayOfUnbArrayOfFloat32;
    org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_MyUnbArrayOfUnbArrayOfString_seq MyUnbArrayOfUnbArrayOfString;
} org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_t;
extern const DDS_TypeSupport_meta org_qeo_test_StateWithUnbArraysOfUnbArraysKeyInt16_type[];


#endif /* QDM_STATEWITHUNBARRAYSOFUNBARRAYSKEYINT16_H_ */

