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

/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#ifndef QDM_EVENTWITHUNBARRAYSOFPRIMITIVES_H_
#define QDM_EVENTWITHUNBARRAYSOFPRIMITIVES_H_

#include <qeo/types.h>

#ifdef __cplusplus
extern "C"
{
#endif


DDS_SEQUENCE(qeo_boolean_t, org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_MyUnbArrayOfBoolean_seq);
DDS_SEQUENCE(int8_t, org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_MyUnbArrayOfByte_seq);
DDS_SEQUENCE(int8_t, org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_MyUnbArrayOfByte1_seq);
DDS_SEQUENCE(int16_t, org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_MyUnbArrayOfInt16_seq);
DDS_SEQUENCE(int32_t, org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_MyUnbArrayOfInt32_seq);
DDS_SEQUENCE(int64_t, org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_MyUnbArrayOfInt64_seq);
DDS_SEQUENCE(float, org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_MyUnbArrayOfFloat32_seq);
DDS_SEQUENCE(char *, org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_MyUnbArrayOfString_seq);
/**
 * struct representing an event containing unbound arrays (sequences) of primitives
 */
typedef struct {
    org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_MyUnbArrayOfBoolean_seq MyUnbArrayOfBoolean;
    org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_MyUnbArrayOfByte_seq MyUnbArrayOfByte;
    org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_MyUnbArrayOfByte1_seq MyUnbArrayOfByte1;
    org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_MyUnbArrayOfInt16_seq MyUnbArrayOfInt16;
    org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_MyUnbArrayOfInt32_seq MyUnbArrayOfInt32;
    org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_MyUnbArrayOfInt64_seq MyUnbArrayOfInt64;
    org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_MyUnbArrayOfFloat32_seq MyUnbArrayOfFloat32;
    org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_MyUnbArrayOfString_seq MyUnbArrayOfString;
} org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_t;
extern const DDS_TypeSupport_meta org_qeo_test_EventWithUnbArraysOfPrimitivesStruct_type[];

#ifdef __cplusplus
}
#endif

#endif /* QDM_EVENTWITHUNBARRAYSOFPRIMITIVES_H_ */

