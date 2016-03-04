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

/* di_darwin -- Dynamic IP address handler for Mac OSX. */

#include <stdio.h>
#include "dds/dds_error.h"
#include "log.h"
#include "error.h"
#include "timer.h"
#include "di_data.h"
#include <netinet/in.h>

#ifndef DI_POLL_DELTA
#define	DI_POLL_DELTA	(10 * TICKS_PER_SEC)	/* IP address change polls. */
#endif

typedef struct ip_ctrl_st {
	unsigned char	*ipa;
	unsigned	*n;
	unsigned	max;
	Scope_t		min_scope;
	Scope_t		max_scope;
	DI_NOTIFY	fct;
	unsigned char	*prev_ipa;
	unsigned	prev_n;
} IP_CTRL;

static IP_CTRL	ipv4;
#ifdef DDS_IPV6
static IP_CTRL	ipv6;
#endif
static Timer_t	poll_timer;

/* di_sys_init -- Initialize dynamic IP handling. */

int di_sys_init (void)
{
	ipv4.fct = NULL;
#ifdef DDS_IPV6
	ipv6.fct = NULL;
#endif
	tmr_init (&poll_timer, "DynIP.poll");

	return (DDS_RETCODE_OK);
}

/* di_event -- Event handler for changes. */

static void di_event (uintptr_t user)
{
	ARG_NOT_USED (user)

	/*log_printf (RTPS_ID, 0, "DynIP: Event.\r\n");*/
	if (ipv4.fct) {
		/*log_printf (RTPS_ID, 0, "DynIP: Check IPv4 addresses (save %u bytes).\r\n", 
						*ipv4.n * OWN_IPV4_SIZE);*/
		memcpy (ipv4.prev_ipa, ipv4.ipa, *ipv4.n * OWN_IPV4_SIZE);
		ipv4.prev_n = *ipv4.n;
		*ipv4.n = sys_own_ipv4_addr (ipv4.ipa, ipv4.max * OWN_IPV4_SIZE,
					     ipv4.min_scope, ipv4.max_scope, 0);
		if (*ipv4.n != ipv4.prev_n ||
		    memcmp (ipv4.ipa, ipv4.prev_ipa, ipv4.prev_n * OWN_IPV4_SIZE)) {
			/*log_printf (RTPS_ID, 0, "DynIP: IPv4 address changes!\r\n");*/
			(*ipv4.fct) ();
		}
	}
#ifdef DDS_IPV6
	if (ipv6.fct) {
		/*log_printf (RTPS_ID, 0, "DynIP: Check IPv6 addresses (save %u bytes).\r\n",
						*ipv6.n * OWN_IPV6_SIZE);*/
		memcpy (ipv6.prev_ipa, ipv6.ipa, *ipv6.n * OWN_IPV6_SIZE);
		ipv6.prev_n = *ipv6.n;
		*ipv6.n = sys_own_ipv6_addr (ipv6.ipa, ipv6.max * OWN_IPV6_SIZE,
					     ipv6.min_scope, ipv6.max_scope, 0);
		if (*ipv6.n != ipv6.prev_n ||
		    memcmp (ipv6.ipa, ipv6.prev_ipa, ipv6.prev_n * OWN_IPV6_SIZE)) {
			/*log_printf (RTPS_ID, 0, "DynIP: IPv6 address changes!\r\n");*/
			(*ipv6.fct) ();
		}
	}
#endif
	tmr_start (&poll_timer, DI_POLL_DELTA, 0, di_event);
}

/* di_sys_attach -- Attach the event handler for the given family. */

int di_sys_attach (unsigned      family,
		   unsigned char *ipa,
		   unsigned      *n,
		   unsigned      max,
		   Scope_t       min_scope,
		   Scope_t       max_scope,
		   DI_NOTIFY     fct)
{
	if (family == AF_INET) {
		ipv4.ipa = ipa;
		ipv4.n = n;
		ipv4.max = max;
		ipv4.min_scope = min_scope;
		ipv4.max_scope = max_scope;
		ipv4.fct = fct;
		ipv4.prev_ipa = xmalloc (max * OWN_IPV4_SIZE);
	}
#ifdef DDS_IPV6
	else if (family == AF_INET6) {
		ipv6.ipa = ipa;
		ipv6.n = n;
		ipv6.max = max;
		ipv6.min_scope = min_scope;
		ipv6.max_scope = max_scope;
		ipv6.fct = fct;
		ipv6.prev_ipa = xmalloc (max * OWN_IPV6_SIZE);
	}
#endif
	else
		return (DDS_RETCODE_BAD_PARAMETER);

	di_event (0);

	return (DDS_RETCODE_OK);
}

/* di_sys_detach -- Detach the event handler from the given family. */

int di_sys_detach (unsigned family)
{
	if (family == AF_INET) {
		ipv4.fct = NULL;
		xfree (ipv4.prev_ipa);
	}
#ifdef DDS_IPV6
	else if (family == AF_INET6) {
		ipv6.fct = NULL;
		xfree (ipv6.prev_ipa);
	}
#endif
	else
		return (DDS_RETCODE_BAD_PARAMETER);

	if (!ipv4.fct
#ifdef DDS_IPV6
	              && !ipv6.fct
#endif
	                          ) {
		log_printf (RTPS_ID, 0, "di_sys_detach: timer stopped!\r\n");
		tmr_stop (&poll_timer);
	}

	return (DDS_RETCODE_OK);
}

/* di_sys_final -- Finalize all event handling and cleanup. */

void di_sys_final (void)
{
	ipv4.fct = NULL;
#ifdef DDS_IPV6
	ipv6.fct = NULL;
#endif
}

void di_sys_check (void)
{
	di_event (0);
}

