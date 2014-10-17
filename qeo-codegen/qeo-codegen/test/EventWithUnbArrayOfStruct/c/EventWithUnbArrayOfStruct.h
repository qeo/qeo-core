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

/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

#ifndef QDM_EVENTWITHUNBARRAYOFSTRUCT_H_
#define QDM_EVENTWITHUNBARRAYOFSTRUCT_H_

#include <qeo/types.h>
#include "StructWithPrimitives.h"

#ifdef __cplusplus
extern "C"
{
#endif


DDS_SEQUENCE(org_qeo_test_MyStructWithPrimitives_t, org_qeo_test_EventWithUnbArrayOfStruct_MyUnbArrayOfStructWithPrimitives_seq);
/**
 * struct representing an event containing an unbound array (sequence) of a struct
 */
typedef struct {
    org_qeo_test_EventWithUnbArrayOfStruct_MyUnbArrayOfStructWithPrimitives_seq MyUnbArrayOfStructWithPrimitives;
} org_qeo_test_EventWithUnbArrayOfStruct_t;
extern const DDS_TypeSupport_meta org_qeo_test_EventWithUnbArrayOfStruct_type[];

#ifdef __cplusplus
}
#endif

#endif /* QDM_EVENTWITHUNBARRAYOFSTRUCT_H_ */

