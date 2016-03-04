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

/* ri_bgcp.h -- Defines the interface to the background notification service. */

#ifndef __ri_bgcp_h_
#define __ri_bgcp_h_

#include "dds/dds_aux.h"
#include "ri_data.h"
#include "ri_bgcp.h"

#define	BG_MAX_CLIENTS	4

extern int bgcp_v4_active;
extern int bgcp_v6_active;

extern IP_CX	*bgv4_server;
extern IP_CX	*bgv6_server;
extern IP_CX	*bg_client [BG_MAX_CLIENTS];

/* The background notification service consists of two distinct layers.
   The first, i.e. the lower layer is responsible for client-server association,
   and the second, i.e. the higher layer translates user requests to lower-layer
   requests/events.
   This separation allows to have either standalone communication to a dedicated
   server or to piggy-back the service over an existing TCP control channel. */

int bgcp_init (unsigned min_cx, unsigned max_cx);

/* Initialize the background notification service. */

void bgcp_reset (void);

/* Should be called whenever the protocols is restarted. */

void bgcp_final (void);

/* Final call to close down all background notification functionalities. */

void bgcp_domain_active (unsigned domain_id);

/* A domain participant was created for the given domain. */

void bgcp_domain_inactive (unsigned domain_id);

/* A domain participant was deleted for the given domain. */


/* Higher layer requests.
   ---------------------- */

typedef void (*SRN_MATCH) (int          enable,
			   uintptr_t    user,
			   GuidPrefix_t *peer,
			   void         **pctxt);

/* Match/unmatch connection callback function. */

typedef void (*SRN_WAKEUP) (uintptr_t     user,
			    const char    *topic_name,
			    const char    *type_name,
			    unsigned char prefix [12]);

/* Wakeup indication from server. */

typedef int (*SRN_NOTIFY) (int        enable,
			   uintptr_t  user,
			   uintptr_t  pctxt,
			   const char *topic,
			   const char *type,
			   uintptr_t  *cookie);

/* Ask server to enable/disable a topic/type notification. If successful, the
   server will return a non-0 result and set the cookie.  If not, the request
   will be declined and 0 is returned. */

typedef void (*SRN_SUSPEND) (int       enable,
			     uintptr_t user,
			     void      *cx);

/* Callback to specify that a peer went to sleep. */


/* 1. Client side functionality: 
   - - - - - - - - - - - - - - - */

int bgcp_start_client (RTPS_TCP_RSERV   *dest,
		       unsigned         domain_id,
		       SRN_MATCH        match_fct,
		       SRN_WAKEUP	wakeup_fct,
		       uintptr_t        user,
		       DDS_ReturnCode_t *error);

/* Start a notification client for the given destination and domain or piggy-
   backed on the RTPS/TCP control channel (dest == NULL).  This function may
   be called multiple times for different arguments.
   If successful, a handle (> 0) will be returned, otherwise *error will be
   set to the real error that occured. */

int bgcp_notify (unsigned   domain_id,
		 const char *topic_name,
		 const char *type_name,
		 uintptr_t   user);

/* Request to enable notifications on a specific Topic/Type in a domain.
   The names can contain standard wildcards ('*' and '?').
   This function should be used by a client to register its topics of interest.
   The connection to the server doesn't need to be up yet.  Remote registration
   will be done automatically once the link is established and repeated if 
   necessary if links go down and up again.
   The user parameter is used whenever a wakeup is triggered to notify which
   topic got data. */

int bgcp_unnotify (unsigned    domain_id,
		   const char *topic_name,
		   const char *type_name);

/* Request to stop notifications on the given Topic/Type in a domain. */

int bgcp_suspending (void);

/* Should be used by the client to notify it is going into suspend mode. */

int bgcp_resuming (void);

/* Should be used by the client to notify that it is active again. */

void bgcp_register_connect (DDS_Activities_on_connected fct);

/* Register a callback function for fd notification. */


/* 2. Server side functionality:
   - - - - - - - - - - - - - - - */

int bgcp_start_server (unsigned         port,
		       int              ipv6,
		       int              secure,
		       unsigned         domain_id,
		       SRN_MATCH        match_fct,
		       SRN_NOTIFY       notify_fct,
		       SRN_SUSPEND	suspend_fct,
		       uintptr_t        user,
		       DDS_ReturnCode_t *error);

/* Start a notification server for the given port number, IP family and
   secure mode in a given domain.  If port == 0, the server will attach
   to the current control channels and will handle requests from there.
   If non-0, a dedicated server will be used.  If successful, a handle
   (>= 0) will be returned. */

int bgcp_match (void *cx, const char *topic_name, const char *type_name);

/* Return a non-0 result if a topic/type name matches the list of requested
   notifications on a server for a specific client.  Should be called in the
   suspend_fct callback to check each client topic in order to create all the
   requested proxy readers. */

int bgcp_wakeup (void         *cx,
		 const char   *topic_name,
		 const char   *type_name,
		 GuidPrefix_t *src);

/* Wake up a remotely suspended client due to data being available on the given
   topic/type. */



/* 3. Common functions:
   - - - - - - - - - - */

int bgcp_stop (int handle);

/* Stop a previously created client or server. */

void bgcp_dump (void);

/* T&D: dump BGCP data. */

#endif /* !__ri_bgcp_h_ */
