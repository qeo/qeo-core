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

#ifndef QDM_REFERENCESTOSTRUCT_H_
#define QDM_REFERENCESTOSTRUCT_H_

#include <qeo/api.h>
#include "qeo.h"

typedef org_qeo_DeviceId_t org_qeo_test_deviceIdAlias_t;
typedef org_qeo_DeviceId_t org_qeo_test_deviceIdAlias2_t;

DDS_SEQUENCE(org_qeo_DeviceId_t, org_qeo_test_ReferencesToStruct_MyUnbArrayOfDeviceId2_seq);
DDS_SEQUENCE(org_qeo_test_deviceIdAlias_t, org_qeo_test_ReferencesToStruct_MyUnbArrayOfDeviceId3_seq);
DDS_SEQUENCE(org_qeo_test_deviceIdAlias2_t, org_qeo_test_ReferencesToStruct_MyUnbArrayOfDeviceId4_seq);
typedef struct {
    /**
     * Regular reference to DeviceId
     */
    org_qeo_DeviceId_t MyUnbArrayOfDeviceId1;
    /**
     * Reference to sequence DeviceId
     */
    org_qeo_test_ReferencesToStruct_MyUnbArrayOfDeviceId2_seq MyUnbArrayOfDeviceId2;
    /**
     * Reference to sequence DeviceId, but trough typedef
     */
    org_qeo_test_ReferencesToStruct_MyUnbArrayOfDeviceId3_seq MyUnbArrayOfDeviceId3;
    /**
     * Reference to sequence DeviceId, but trough typedef -- reference to a different alias
     */
    org_qeo_test_ReferencesToStruct_MyUnbArrayOfDeviceId4_seq MyUnbArrayOfDeviceId4;
} org_qeo_test_RefsToStruct_t;
extern const DDS_TypeSupport_meta org_qeo_test_RefsToStruct_type[];


#endif /* QDM_REFERENCESTOSTRUCT_H_ */

