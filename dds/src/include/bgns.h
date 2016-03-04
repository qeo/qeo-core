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

/* bgns.h -- Interface to the Background Notification Server functionality.  */

#ifndef __bgns_h_
#define	__bgns_h_

#include "dds/dds_aux.h"
#include "dds_data.h"

int bgns_init (unsigned min_cx, unsigned max_cx);

/* Initialize the BGNS service with the given parameters. */

int bgns_start_server (unsigned         port,
		       int              ipv6,
		       int              secure,
		       unsigned         domain_id,
		       DDS_ReturnCode_t *error);

/* Start the background notification server.  Returns a non-0 handle if
   successful. */

int bgns_start_client (const char       *server,
		       int              ipv6,
		       int              secure,
		       unsigned         domain_id,
		       DDS_ReturnCode_t *error);

/* Start a background notification service client. */

int bgns_stop (unsigned handle);

/* Stop the background notification server. */

void bgns_activate (Domain_t *dp);

/* Called automatically from DCPS when a domain is created. */

void bgns_deactivate (Domain_t *dp);

/* Called automatically from DCPS when a domain is closed. */

void bgns_dispose (Domain_t *dp);

/* Called when a domain is going to delete all its entities. */

void bgns_final (void);

/* Final cleanup. */

void bgns_register_wakeup (DDS_Activities_on_wakeup fct);

/* Register a callback function to be called on wakeup. */

void bgns_register_info (DDS_Activities_on_client_change fct);

/* Register a callback function to be called on client changes. */

void bgns_dump (void);

/* Trace/Debug only: dump BGNS data. */
 
#endif /* !__bgns_h_ */
