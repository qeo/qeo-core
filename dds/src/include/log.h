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

#ifndef __log_h_
#define __log_h_

#define	LOG_DEF_ID	0	/* Default logging id. */
#define	LOG_MAX_ID	31	/* Maximum logging id. */

#define	PROF_ID		0	/* Profiling logging. */
#define	DOM_ID		1	/* Domain logging. */
#define	POOL_ID		2	/* Pool logging. */
#define	STR_ID		3	/* String pool logging. */
#define	LOC_ID		4	/* Locator pool logging. */
#define	TMR_ID		5	/* Timer pool logging. */
#define	DB_ID		6	/* Buffer pool logging. */
#define	THREAD_ID	7	/* Thread logging. */
#define	SOCK_ID		8	/* Socket logging. */
#define	IP_ID		9	/* IP transport logging. */
#define	TCPS_ID		10	/* TCP protocol logging. */
#define	BGNS_ID		11	/* BGNS protocol logging. */
#define	CACHE_ID	12	/* Cache logging. */
#define	QOS_ID		13	/* QoS pool logging. */
#define	XTYPES_ID	14	/* X-Types extensions logging. */
#define	RTPS_ID		15	/* RTPS protocol logging. */
#define	DISC_ID		16	/* Discovery logging. */
#define	SPDP_ID		17	/* SPDP protocol logging. */
#define	SEDP_ID		18	/* SEDP protocol logging. */
#define	DCPS_ID		19	/* DCPS protocol logging. */
#define	SEC_ID		20	/* Security logging. */
#define	DDS_ID		21	/* General DDS logging. */

#define	INFO_ID		22	/* DDS error info logging. */
#define	USER_ID		23	/* User-level logging offset. */

extern const char *log_id_str [];	/* Id strings. */
extern const char **log_fct_str [];	/* Function strings. */

#endif /* __log_h_ */

