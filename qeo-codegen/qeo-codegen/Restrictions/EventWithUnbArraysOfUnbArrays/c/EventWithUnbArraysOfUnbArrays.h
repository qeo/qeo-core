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

#ifndef QDM_EVENTWITHUNBARRAYSOFUNBARRAYS_H_
#define QDM_EVENTWITHUNBARRAYSOFUNBARRAYS_H_

#include <qeo/api.h>
#include "qeo.h"

typedef int8_t org_qeo_test_MyAliasToUnbArrayOfByte_t;
typedef int32_t org_qeo_test_MyAliasfInt32_t;
typedef char * org_qeo_test_MyAliasOfString_t;

DDS_SEQUENCE(unsigned char, org_qeo_test_EventWithUnbArraysOfUnbArrays_MyAliasToUnbArrayOfBoolean_t);
DDS_SEQUENCE(int16_t, org_qeo_test_EventWithUnbArraysOfUnbArrays_MyAliasToUnbArrayOfInt16_t);
DDS_SEQUENCE(int32_t, org_qeo_test_EventWithUnbArraysOfUnbArrays_MyAliasToUnbArrayOfInt32_t);
DDS_SEQUENCE(int64_t, org_qeo_test_EventWithUnbArraysOfUnbArrays_MyAliasToUnbArrayOfInt64_t);
DDS_SEQUENCE(float, org_qeo_test_EventWithUnbArraysOfUnbArrays_MyAliasToUnbArrayOfFloat32_t);
DDS_SEQUENCE(char *, org_qeo_test_EventWithUnbArraysOfUnbArrays_MyAliasToUnbArrayOfString_t);
DDS_SEQUENCE(org_qeo_test_MyAliasToUnbArrayOfByte_t, org_qeo_test_EventWithUnbArraysOfUnbArrays_MyUnbArrayOfUnbArrayOfByte_seq);
DDS_SEQUENCE(org_qeo_test_EventWithUnbArraysOfUnbArrays_MyAliasToUnbArrayOfInt16_t, org_qeo_test_EventWithUnbArraysOfUnbArrays_MyUnbArrayOfUnbArrayOfInt16_seq);
DDS_SEQUENCE(org_qeo_test_EventWithUnbArraysOfUnbArrays_MyAliasToUnbArrayOfInt32_t, org_qeo_test_EventWithUnbArraysOfUnbArrays_MyUnbArrayOfUnbArrayOfInt32_seq);
DDS_SEQUENCE(org_qeo_test_EventWithUnbArraysOfUnbArrays_MyAliasToUnbArrayOfInt64_t, org_qeo_test_EventWithUnbArraysOfUnbArrays_MyUnbArrayOfUnbArrayOfInt64_seq);
DDS_SEQUENCE(org_qeo_test_EventWithUnbArraysOfUnbArrays_MyAliasToUnbArrayOfFloat32_t, org_qeo_test_EventWithUnbArraysOfUnbArrays_MyUnbArrayOfUnbArrayOfFloat32_seq);
DDS_SEQUENCE(org_qeo_test_EventWithUnbArraysOfUnbArrays_MyAliasToUnbArrayOfString_t, org_qeo_test_EventWithUnbArraysOfUnbArrays_MyUnbArrayOfUnbArrayOfString_seq);
DDS_SEQUENCE(org_qeo_DeviceId_t, org_qeo_test_EventWithUnbArraysOfUnbArrays_MyUnbArrayOfDeviceId_seq);
/**
 * struct representing an event containing unbound arrays (sequences) of unbound arrays
 */
typedef struct {
    org_qeo_test_EventWithUnbArraysOfUnbArrays_MyAliasToUnbArrayOfBoolean_t MyUnbArrayOfUnbArrayOfBoolean;
    org_qeo_test_EventWithUnbArraysOfUnbArrays_MyUnbArrayOfUnbArrayOfByte_seq MyUnbArrayOfUnbArrayOfByte;
    org_qeo_test_EventWithUnbArraysOfUnbArrays_MyUnbArrayOfUnbArrayOfInt16_seq MyUnbArrayOfUnbArrayOfInt16;
    org_qeo_test_EventWithUnbArraysOfUnbArrays_MyUnbArrayOfUnbArrayOfInt32_seq MyUnbArrayOfUnbArrayOfInt32;
    org_qeo_test_EventWithUnbArraysOfUnbArrays_MyUnbArrayOfUnbArrayOfInt64_seq MyUnbArrayOfUnbArrayOfInt64;
    org_qeo_test_EventWithUnbArraysOfUnbArrays_MyUnbArrayOfUnbArrayOfFloat32_seq MyUnbArrayOfUnbArrayOfFloat32;
    org_qeo_test_EventWithUnbArraysOfUnbArrays_MyUnbArrayOfUnbArrayOfString_seq MyUnbArrayOfUnbArrayOfString;
    org_qeo_test_EventWithUnbArraysOfUnbArrays_MyUnbArrayOfDeviceId_seq MyUnbArrayOfDeviceId;
    org_qeo_test_MyAliasfInt32_t MyInt32;
    org_qeo_test_MyAliasOfString_t MyString;
} org_qeo_test_EventWithUnbArraysOfUnbArrays_t;
extern const DDS_TypeSupport_meta org_qeo_test_EventWithUnbArraysOfUnbArrays_type[];


#endif /* QDM_EVENTWITHUNBARRAYSOFUNBARRAYS_H_ */

