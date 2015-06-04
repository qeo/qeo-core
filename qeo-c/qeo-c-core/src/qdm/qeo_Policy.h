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

#ifndef QDM_QEO_POLICY_H_
#define QDM_QEO_POLICY_H_

#include <qeo/types.h>

#ifdef __cplusplus
extern "C"
{
#endif


/**
 * The currently enforced policy
 */
typedef struct {
  /**
   * [Key]
   * Policy Sequence Nr.
   */
    int64_t seqnr;
  /**
   * Policy content.
   */
    char * content;
} org_qeo_system_Policy_t;
extern const DDS_TypeSupport_meta org_qeo_system_Policy_type[];

#ifdef __cplusplus
}
#endif

#endif /* QDM_QEO_POLICY_H_ */

