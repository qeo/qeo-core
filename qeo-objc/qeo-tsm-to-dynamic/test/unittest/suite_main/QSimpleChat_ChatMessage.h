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

#ifndef QDM_QSIMPLECHAT_CHATMESSAGE_H_
#define QDM_QSIMPLECHAT_CHATMESSAGE_H_

#include <qeo/types.h>
#include "qeo_types.h"

#include "cwifi.h"
#include "myenum.h"

DDS_SEQUENCE(int8_t, org_qeo_sample_simplechat_ChatMessage_extraInfo_seq);
DDS_SEQUENCE(my_enum_t, myenum_seq);
/**
 * A simple chat message.
 */
typedef struct {
    /**
     * The user sending the message.
     */
    char * from;

    org_qeo_UUID_t fromExtra;

    /**
     * The message.
     */
    char * message;
    
    org_qeo_sample_simplechat_ChatMessage_extraInfo_seq extraInfo;

    org_qeo_wifi_ScanList_list_seq list;

    my_enum_t maincolor;

    myenum_seq colorlist; 

} org_qeo_sample_simplechat_ChatMessage_t;
extern const DDS_TypeSupport_meta org_qeo_sample_simplechat_ChatMessage_type[];


#endif /* QDM_QSIMPLECHAT_CHATMESSAGE_H_ */

