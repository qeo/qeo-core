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

/* dcps_priv.h -- Private data of the DCPS layer. */

#ifndef __dcps_priv_h_
#define	__dcps_priv_h_

#define DCPS_BUILTIN_READERS	/* Define this for the DCPS Builtin Readers. */
/*#define DUMP_DDATA		** Define to dump rxed/txed dynamic data. */
/*#define TRACE_DELETE_CONTAINED** Define to trace deletion of contained data.*/

#ifdef TRACE_DELETE_CONTAINED
#define	dtrc_printf(s,n)	printf (s, n)
#else
#define	dtrc_printf(s,n)
#endif


enum mem_block_en {
	MB_SAMPLE_INFO,		/* DCPS SampleInfo. */
	MB_WAITSET,		/* DCPS WaitSet. */
	MB_STATUS_COND,		/* DCPS StatusCondition. */
	MB_READ_COND,		/* DCPS ReadCondition. */
	MB_QUERY_COND,		/* DCPS QueryCondition. */
	MB_GUARD_COND,		/* DCPS GuardCondition. */
	MB_TOPIC_WAIT,		/* Topic Wait context. */

	MB_END
};

extern MEM_DESC_ST 	dcps_mem_blocks [MB_END];  /* Memory used by DCPS. */
extern const char	*dcps_mem_names [];
extern unsigned		dcps_entity_count;

StatusCondition_t *dcps_new_status_condition (void);

void dcps_delete_status_condition (StatusCondition_t *cp);


Strings_t *dcps_new_str_pars (DDS_StringSeq *pars, int *error);

#define	dcps_free_str_pars	strings_delete

int dcps_update_str_pars (Strings_t **sp, DDS_StringSeq *pars);

int dcps_get_str_pars (DDS_StringSeq *pars, Strings_t *sp);

unsigned dcps_skip_mask (DDS_SampleStateMask   sample_states,
			 DDS_ViewStateMask     view_states,
			 DDS_InstanceStateMask instance_states);

#endif	/* !__dcps_priv_h_ */


