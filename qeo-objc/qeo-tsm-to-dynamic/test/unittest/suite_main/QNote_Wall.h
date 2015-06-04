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

#ifndef QDM_QNOTE_WALL_H_
#define QDM_QNOTE_WALL_H_

#include <qeo/types.h>


typedef struct {
    /**
     * [Key] Unique ID of the wall. This is a randomly generated number by the Wall.
     */
    int32_t id;
    /**
     * Description of the wall (e.g kitchen wall)
     */
    char * description;
} org_qeo_sample_note_Wall_t;
extern const DDS_TypeSupport_meta org_qeo_sample_note_Wall_type[];


#endif /* QDM_QNOTE_WALL_H_ */

