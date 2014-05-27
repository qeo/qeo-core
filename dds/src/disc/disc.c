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

/* disc.c -- Implements the SPDP and SEDP discovery protocols which are used to
	     discover new DDS participants and endpoints. */

#include <stdio.h>
#include <stdlib.h>
#ifdef _WIN32
#include "win.h"
#else
#include <unistd.h>
#include <arpa/inet.h>
#endif
#include "sys.h"
#include "log.h"
#include "str.h"
#include "error.h"
#include "skiplist.h"
#include "pool.h"
#include "timer.h"
#include "debug.h"
#include "guid.h"
#include "parse.h"
#include "pl_cdr.h"
#include "rtps.h"
#include "uqos.h"
#include "dds.h"
#include "guard.h"
#include "dcps.h"
#ifdef DDS_SECURITY
#include "security.h"
#endif
#ifdef DDS_TYPECODE
#include "vtc.h"
#endif
#ifdef DDS_FORWARD
#include "rtps_fwd.h"
#endif
#include "disc.h"

static int		disc_log;		/* Genaral Discovery logging. */

#ifdef RTPS_USED

static int		spdp_log;		/* SPDP event logging. */

/*#define TOPIC_DISCOVERY	** Optional Topic Discovery. */
/*#define DISC_MSG_DUMP		** Dump Participant messages. */
/*#define DISC_EV_TRACE		** Trace Discovery events. */

#if !defined (SIMULATION) && !defined (STATIC_DISCOVERY)
#define	SIMPLE_DISCOVERY
#endif

#define	LEASE_DELTA		10	/* # of seconds extra for lease time-out. */

static DMATCHFCT	notify_match;		/* Match notification. */
static DUMATCHFCT	notify_unmatch;		/* Unmatch notification. */
static DUMDONEFCT	notify_done;		/* Done notification. */

#ifdef SIMPLE_DISCOVERY

static int		sedp_log;		/* SEDP event logging. */

static DDS_TypeSupport	dds_participant_msg_ts;

static const DDS_TypeSupport_meta dds_participant_msg_tsm [] = {
	{ CDR_TYPECODE_STRUCT, 3, "ParticipantMessageData", sizeof (ParticipantMessageData), 0, 3, 0, NULL },
	{ CDR_TYPECODE_ARRAY, 1, "participantGuidPrefix", 0, 0, sizeof (GuidPrefix_t), 0, NULL },
	{ CDR_TYPECODE_OCTET, 0, NULL, 0, 0, 0, 0, NULL },
	{ CDR_TYPECODE_ARRAY, 1, "kind", 0, offsetof (ParticipantMessageData, kind), 4, 0, NULL },
	{ CDR_TYPECODE_OCTET, 0, NULL, 0, 0, 0, 0, NULL },
	{ CDR_TYPECODE_SEQUENCE, 2, "data", 0, offsetof (ParticipantMessageData, data), 0, 0, NULL },
	{ CDR_TYPECODE_OCTET, 0, NULL, 0, 0, 0, 0, NULL } 
};
#endif /* SIMPLE_DISCOVERY */

/* disc_register -- Register discovery notification functions. */

void disc_register (DMATCHFCT  n_match,
		    DUMATCHFCT n_unmatch,
		    DUMDONEFCT n_done)
{
	notify_match = n_match;
	notify_unmatch = n_unmatch;
	notify_done = n_done;
}

#ifdef SIMPLE_DISCOVERY

typedef struct change_data_t {
	ChangeKind_t	kind;
	void		*data;
	int		is_new;
	InstanceHandle	h;
	handle_t	writer;
	FTime_t		time;
} ChangeData_t;

/* disc_get_data -- Get a discovery protocol message from the history cache. */

static DDS_ReturnCode_t disc_get_data (Reader_t *rp, ChangeData_t *c)
{
	Change_t	*cp;
	void		*auxp;
	unsigned	nchanges;
	DDS_ReturnCode_t error;

	nchanges = 1;
	error = hc_get_data (rp->r_cache, &nchanges, &cp, SKM_READ, 0, 0, 1);
	if (error)
		return (error);

	if (!nchanges)
		return (DDS_RETCODE_NO_DATA);

	c->kind   = cp->c_kind;
	c->is_new = cp->c_vstate == NEW;
	c->h      = cp->c_handle;
	c->writer = cp->c_writer;
	c->time   = cp->c_time;
	if (cp->c_kind == ALIVE) {
		c->data = dcps_get_cdata (NULL, cp,
					  rp->r_topic->type->type_support, 0,
					  &error, &auxp);
		if (error)
			log_printf (DISC_ID, 0, "disc_get_data({%u}) returned error %d!\r\n",
					       rp->r_handle, error);
	}
	else {
		c->data = NULL;
		error = DDS_RETCODE_OK;
	}
	hc_change_free (cp);
	return (error);
}

/* add_locators -- Add locators to an endpoint. */

static void add_locators (LocalEndpoint_t     *ep,
			  int	              writer,
			  const LocatorList_t uc_locs,
			  const LocatorList_t mc_locs,
			  const LocatorList_t dst_locs)
{
	LocatorRef_t	*rp;
	LocatorNode_t	*np;
	int		error;

	foreach_locator (uc_locs, rp, np) {
		error = rtps_endpoint_add_locator (ep, 0, &np->locator);
		if (error)
			fatal_printf ("SPDP: can't add endpoint unicast destination!");
	}
	foreach_locator (mc_locs, rp, np) {
		error = rtps_endpoint_add_locator (ep, 1, &np->locator);
		if (error)
	    		fatal_printf ("SPDP: can't add endpoint multicast destination!");
	}
	if (writer)
		foreach_locator (dst_locs, rp, np) {
			if (!lock_take (((Writer_t *) ep)->w_lock)) {
				error = rtps_reader_locator_add ((Writer_t *) ep, np, 0, 1);
				lock_release (((Writer_t *) ep)->w_lock);
				if (error)
					fatal_printf ("SPDP: can't add writer destination!");
			}
		}
}

/* create_builtin_endpoint -- Create a built-in endpoint.
			      On entry/exit: no locks taken. */

static int create_builtin_endpoint (Domain_t            *dp,
				    BUILTIN_INDEX       index,
				    int                 push_mode,
				    int                 stateful,
				    const Duration_t    *resend_per,
				    const LocatorList_t uc_locs,
				    const LocatorList_t mc_locs,
				    const LocatorList_t dst_locs)
{
	unsigned		type;
	int			writer, error;
	static const Builtin_Type_t builtin_types [] = {
		BT_Participant,
		BT_Participant,
		BT_Publication,
		BT_Publication,
		BT_Subscription,
		BT_Subscription,
		0, 0, /* Proxy */
		0, 0, /* State */
		0, 0, /* Message */
		BT_Topic,
		BT_Topic
	};
	static const size_t	builtin_dlen [] = {
		sizeof (Participant_t *),
		sizeof (SPDPdiscoveredParticipantData),
		sizeof (Writer_t *),
		sizeof (DiscoveredWriterData),
		sizeof (Reader_t *),
		sizeof (DiscoveredReaderData),
		0, 0, /* Proxy */
		0, 0, /* State */
		sizeof (ParticipantMessageData),
		sizeof (ParticipantMessageData),
		sizeof (Topic_t *),
		sizeof (DiscoveredTopicData)
	};
	static const size_t	builtin_klen [] = {
		sizeof (GuidPrefix_t),
		sizeof (GuidPrefix_t),
		sizeof (GUID_t),
		sizeof (GUID_t),
		sizeof (GUID_t),
		sizeof (GUID_t),
		0, 0, /* Proxy */
		0, 0, /* State */
		sizeof (GUID_t),
		sizeof (GUID_t),
		0,
		0
	};
	TypeSupport_t		*ts;
	PL_TypeSupport		*plp;
	Endpoint_t		*ep;
	TopicType_t		*typep;
	Topic_t			*tp;
	const EntityId_t	*eid;
	Writer_t		*wp;
	Reader_t		*rp;
	Cache_t			cache;
	DDS_DataReaderQos	rqos;
	DDS_DataWriterQos	wqos;

	if (lock_take (dp->lock))
		return (DDS_RETCODE_BAD_PARAMETER);

	typep = type_lookup (dp, rtps_builtin_type_names [index]);
	if (!typep) {
		typep = type_create (dp, rtps_builtin_type_names [index], NULL);
		if (!typep)
			fatal_printf ("create_builtin_endpoint: can't create builtin type (%s/%s)!",
				rtps_builtin_topic_names [index],
				rtps_builtin_type_names [index]);

		typep->flags |= EF_BUILTIN | EF_LOCAL;
		ts = xmalloc (sizeof (TypeSupport_t) + sizeof (PL_TypeSupport));
		if (!ts)
			fatal_printf ("create_builtin_endpoint: out of memory for type_support!");

		memset (ts, 0, sizeof (TypeSupport_t) + sizeof (PL_TypeSupport));
		plp = (PL_TypeSupport *) (ts + 1);
		ts->ts_name = rtps_builtin_type_names [index];
		ts->ts_prefer = MODE_PL_CDR;
		ts->ts_keys = 1;
		ts->ts_dynamic = 1;
#ifdef DDS_TYPECODE
		ts->ts_origin = TSO_Builtin;
#endif
		ts->ts_fksize = 1;
		ts->ts_length = builtin_dlen [index];
		ts->ts_mkeysize = builtin_klen [index];
		plp->builtin = 1;
		plp->type = builtin_types [index];
#ifdef XTYPES_USED
		plp->xtype = NULL;
#endif
		ts->ts_pl = plp;
		typep->type_support = ts;
	}
	tp = topic_lookup (&dp->participant, rtps_builtin_topic_names [index]);
	if (!tp) {
		tp = topic_create (&dp->participant,
				   NULL,
				   rtps_builtin_topic_names [index],
				   rtps_builtin_type_names [index], NULL);
		if (!tp)
			fatal_printf ("create_builtin_endpoint: can't create builtin topic (%s/%s)!",
				rtps_builtin_topic_names [index],
				rtps_builtin_type_names [index]);

		tp->entity.flags |= EF_BUILTIN;
	}
	eid = &rtps_builtin_eids [index];
	type = eid->id [ENTITY_KIND_INDEX] & ENTITY_KIND_MINOR;
	writer = (type == ENTITY_KIND_WRITER_KEY || type == ENTITY_KIND_WRITER);
	lock_take (tp->lock);
	ep = endpoint_create (&dp->participant,
			      writer ? (void *) dp->builtin_publisher : 
				       (void *) dp->builtin_subscriber,
			      eid, NULL);
	if (!ep)
		fatal_printf ("create_builtin_endpoint: can't create builtin endpoint (%s/%s)!",
			rtps_builtin_topic_names [index],
			rtps_builtin_type_names [index]);

	ep->entity.flags |= EF_BUILTIN;
	ep->topic = tp;
	if (ep->entity.type == ET_WRITER) {
		wp = (Writer_t *) ep;
#ifdef RW_LOCKS
		lock_take (wp->w_lock);
#endif
		wp->w_publisher->nwriters++;
		wp->w_next = tp->writers;
		tp->writers = &wp->w_ep;
		wqos = qos_def_writer_qos;
			wqos.durability.kind = DDS_TRANSIENT_LOCAL_DURABILITY_QOS;
			if (stateful)
				wqos.reliability.kind = DDS_RELIABLE_RELIABILITY_QOS;
		ep->qos = qos_writer_new (&wqos);

		/* Create writer endpoint. */
		wp->w_rtps = NULL;
		memset (&wp->w_listener, 0, sizeof (Writer_t) -
					       offsetof (Writer_t, w_listener));

		/* Create a history cache. */
		wp->w_cache = cache = hc_new ((LocalEndpoint_t *) ep, 0);

		/* Create RTPS Writer context. */
		error = rtps_writer_create (wp, push_mode, stateful,
					    NULL, NULL, NULL, resend_per);
#ifdef RW_LOCKS
	    	lock_release (wp->w_lock);
#endif
	}
	else {
		rp = (Reader_t *) ep;
#ifdef RW_LOCKS
		lock_take (rp->r_lock);
#endif
		rp->r_subscriber->nreaders++;
		rp->r_next = tp->readers;
		tp->readers = &rp->r_ep;
		rqos = qos_def_reader_qos;
			rqos.durability.kind = DDS_TRANSIENT_LOCAL_DURABILITY_QOS;
			if (stateful)
				rqos.reliability.kind = DDS_RELIABLE_RELIABILITY_QOS;
		ep->qos = qos_reader_new (&rqos);

		/* Create reader endpoint. */
		rp->r_rtps = NULL;
		qos_init_time_based_filter (&rp->r_time_based_filter);
		qos_init_reader_data_lifecycle (&rp->r_data_lifecycle);

		/* Create a history cache. */
		rp->r_cache = cache = hc_new ((LocalEndpoint_t *) ep, 0);

		/* Add RTPS Reader context. */
		error = rtps_reader_create (rp, stateful,
					    NULL, NULL);
#ifdef RW_LOCKS
	    	lock_release (rp->r_lock);
#endif
	}
	lock_release (tp->lock);
	if (error || !ep->qos) {
		fatal_printf ("create_builtin_endpoint: can't create built-in endpoint (%u)!", index);
		lock_release (dp->lock);
		return (error);
	}
	dp->participant.p_builtins |= 1 << index;
	dp->participant.p_builtin_ep [index] = ep;
	add_locators ((LocalEndpoint_t *) dp->participant.p_builtin_ep [index],
					writer, uc_locs, mc_locs, dst_locs);
	lock_release (dp->lock);
	return (DDS_RETCODE_OK);
}

/* disable_builtin_writer -- Disable notifications on builtin writers (and
			     purge outstanding notifications). On entry:
			     domain_lock and global_lock taken. */

static void disable_builtin_writer (Writer_t *wp)
{
	Topic_t		*tp;

	tp = wp->w_topic;
	if (lock_take (tp->lock))
		return;

#ifdef RW_LOCKS
	if (lock_take (wp->w_lock))
		goto done;
#endif

	/* Turn off notifications */
	hc_request_notification (wp->w_cache, NULL, (uintptr_t) 0);

	/* Purge remaining notification */
	while (!dds_purge_notifications ((Entity_t *) wp, DDS_ALL_STATUS, 1)) {

		/* Listener is still running, need to block until it is done! */
		/* Release all locks: */
#ifdef RW_LOCKS
		lock_release (wp->w_lock);
#endif
		lock_release (tp->lock);
		lock_release (tp->domain->lock);

		/* Wait */
		dds_wait_listener ((Entity_t *) wp);

		/* Take all locks back: */
		lock_take (tp->domain->lock);
		lock_take (tp->lock);
#ifdef RW_LOCKS
		lock_take (wp->w_lock);
#endif
	}

#ifdef RW_LOCKS
	lock_release (wp->w_lock);
    done:
#endif
	lock_release (tp->lock);
}

/* delete_builtin_writer -- Delete all writer-specific builtin endpoint data.
			    On entry: DP and global lock taken. */

static void delete_builtin_writer (Writer_t *wp)
{
	Endpoint_t	*ep, *prev_ep;
	Topic_t		*tp;

	tp = wp->w_topic;
	if (lock_take (tp->lock))
		return;

#ifdef RW_LOCKS
	if (lock_take (wp->w_lock))
		goto done;
#endif

	/* Decrease # of writers of builtin publisher. */
	wp->w_publisher->nwriters--;

	/* Remove writer from topic endpoints list. */
	for (ep = tp->writers, prev_ep = NULL;
	     ep;
	     prev_ep = ep, ep = ep->next)
		if (ep == &wp->w_ep) {

			/* Remove RTPS writer. */
			rtps_writer_delete (wp);

			/* Remove from topic list. */
			if (prev_ep)
				prev_ep->next = ep->next;
			else
				tp->writers = ep->next;
			break;
		}

#ifdef RW_LOCKS
	lock_release (wp->w_lock);

    done:
#endif
	lock_release (tp->lock);
}

/* disable_builtin_reader -- Disable notifications on builtin reader (and
			     purge outstanding notifications). On entry:
			     domain_lock and global_lock taken. */

static void disable_builtin_reader (Reader_t *rp)
{
	Topic_t		*tp;

	tp = rp->r_topic;
	if (lock_take (tp->lock))
		return;

#ifdef RW_LOCKS
	if (lock_take (rp->r_lock))
		goto done;
#endif

	/* Turn off notifications */
	hc_request_notification (rp->r_cache, NULL, (uintptr_t) 0);

	/* Purge remaining notification */
	while (!dds_purge_notifications ((Entity_t *) rp, DDS_ALL_STATUS, 1)) {

		/* Listener is still running, need to block until it is done! */
		/* Release all locks: */
#ifdef RW_LOCKS
		lock_release (rp->r_lock);
#endif
		lock_release (tp->lock);
		lock_release (tp->domain->lock);

		/* Wait */
		dds_wait_listener ((Entity_t *) rp);

		/* Take all locks back: */
		lock_take (tp->domain->lock);
		lock_take (tp->lock);
#ifdef RW_LOCKS
		lock_take (rp->r_lock);
#endif
	}

#ifdef RW_LOCKS
	lock_release (rp->r_lock);

done:
#endif
	lock_release (tp->lock);
}

/* delete_builtin_reader -- Delete all reader-specific builtin endpoint data. */

static void delete_builtin_reader (Reader_t *rp)
{
	Endpoint_t	*ep, *prev_ep;
	Topic_t		*tp;

	tp = rp->r_topic;
	if (lock_take (tp->lock))
		return;

#ifdef RW_LOCKS
	if (lock_take (rp->r_lock))
		goto done;
#endif

	/* Decrease # of readers of builtin subscriber. */
	rp->r_subscriber->nreaders--;

	/* Remove writer from topic endpoints list. */
	for (ep = tp->readers, prev_ep = NULL;
	     ep;
	     prev_ep = ep, ep = ep->next)
		if (ep == &rp->r_ep) {

			/* Remove RTPS Reader. */
			rtps_reader_delete (rp);

			/* Remove from topic list. */
			if (prev_ep)
				prev_ep->next = ep->next;
			else
				tp->readers = ep->next;
			break;
		}
#ifdef RW_LOCKS
	lock_release (rp->r_lock);

    done:
#endif
	lock_release (tp->lock);
}


/* disable_builtin_endpoint -- Turn of notifications on a builtin endpoint, and
			       purge outstanding notifications. On entry/exit:
			       domain and global lock taken. */

static void disable_builtin_endpoint (Domain_t *dp, BUILTIN_INDEX index)
{
	LocalEndpoint_t	*ep = (LocalEndpoint_t *) dp->participant.p_builtin_ep [index];
	
	if (ep->ep.entity.type == ET_WRITER)
		disable_builtin_writer ((Writer_t *) ep);
	else
		disable_builtin_reader ((Reader_t *) ep);
}

/* delete_builtin_endpoint -- Remove a builtin endpoint.
			      On entry/exit: domain and global lock taken. */

static void delete_builtin_endpoint (Domain_t *dp, BUILTIN_INDEX index)
{
	LocalEndpoint_t	*ep = (LocalEndpoint_t *) dp->participant.p_builtin_ep [index];
	TopicType_t	*typep;
	Topic_t		*tp;

	/* Delete Reader/Writer specific data. */
	if (ep->ep.entity.type == ET_WRITER)
		delete_builtin_writer ((Writer_t *) ep);
	else
		delete_builtin_reader ((Reader_t *) ep);

	/* Delete Locator lists. */
	if (ep->ep.ucast)
		locator_list_delete_list (&ep->ep.ucast);
	if (ep->ep.mcast)
		locator_list_delete_list (&ep->ep.mcast);

	/* Delete QoS parameters. */
	if (ep->ep.qos)
		qos_free (ep->ep.qos);

	/* Delete History Cache. */
	hc_free (ep->cache);

	/* Delete Typesupport if last one. */
	tp = ep->ep.topic;
	lock_take (tp->lock);
	typep = tp->type;
	if (typep->nrefs == 1) {
		xfree ((void *) typep->type_support);
		typep->type_support = NULL;
	}

	/* Delete domain endpoint. */
	ep->ep.topic = NULL;
	endpoint_delete (&dp->participant, &ep->ep);

	/* Delete topic (frees/destroys topic lock). */
	topic_delete (&dp->participant, tp, NULL, NULL);

	/* Builtin has disappeared. */
	dp->participant.p_builtins &= ~(1 << index);
	dp->participant.p_builtin_ep [index] = NULL;

	/* lock_release (dp->lock); */
}

#endif /* SIMPLE_DISCOVERY */

/* disc_new_match -- A match between one of our endpoints and a remote
		     endpoint was detected.
		     On entry/exit: DP, TP, R/W locked. */

static void disc_new_match (LocalEndpoint_t *ep, Endpoint_t *peer_ep)
{
	Writer_t	*wp;
	Reader_t	*rp;
	int		e;

	if (disc_log)
		log_printf (DISC_ID, 0, "Discovery: Match detected!\r\n");

	e = (*notify_match) (ep, peer_ep);
	if (entity_writer (entity_type (&ep->ep.entity))) {
		if (e) {
			wp = (Writer_t *) ep;
			rtps_matched_reader_add (wp, (DiscoveredReader_t *) peer_ep);
			dcps_publication_match (wp, 1, peer_ep);
			liveliness_enable (&wp->w_ep, peer_ep);
			deadline_enable (&wp->w_ep, peer_ep);
		}
	}
	else {
		if (e) {
			rp = (Reader_t *) ep;
			hc_rem_writer_add (rp->r_cache, peer_ep->entity.handle);
			rtps_matched_writer_add (rp, (DiscoveredWriter_t *) peer_ep);
			dcps_subscription_match (rp, 1, peer_ep);
			liveliness_enable (peer_ep, &rp->r_ep);
			deadline_enable (peer_ep, &rp->r_ep);
			lifespan_enable (peer_ep, &rp->r_ep);
		}
	}
}

#ifdef SIMPLE_DISCOVERY

/* disc_end_match -- A match between one of our endpoints and a remote
		     endpoint was removed.
		     On entry/exit: DP, TP, R/W locked. */

static void disc_end_match (LocalEndpoint_t *ep, Endpoint_t *peer_ep)
{
	Writer_t	*wp;
	Reader_t	*rp;
	int		e;

	if (disc_log)
		log_printf (DISC_ID, 0, "Discovery: Match removed!\r\n");

	e = (*notify_unmatch) (ep, peer_ep);
	if (entity_writer (entity_type (&ep->ep.entity))) {
		wp = (Writer_t *) ep;
		liveliness_disable (&wp->w_ep, peer_ep);
		deadline_disable (&wp->w_ep, peer_ep);
		rtps_matched_reader_remove (wp, (DiscoveredReader_t *) peer_ep);
		dcps_publication_match (wp, 0, peer_ep);
		if (e)
			(*notify_done) (ep);
	}
	else {
		rp = (Reader_t *) ep;
		lifespan_disable (peer_ep, &rp->r_ep);
		deadline_disable (peer_ep, &rp->r_ep);
		liveliness_disable (peer_ep, &rp->r_ep);
		rtps_matched_writer_remove (rp, (DiscoveredWriter_t *) peer_ep);
		hc_rem_writer_removed (rp->r_cache, peer_ep->entity.handle);
		dcps_subscription_match (rp, 0, peer_ep);
		if (e)
			(*notify_done) (ep);
	}
}

#endif

typedef struct match_data_st {
	LocatorList_t	cur_uc;
	LocatorList_t	cur_mc;
	LocatorList_t	res_uc;
	Endpoint_t	*ep;
	const char	*names [2];
	unsigned	nmatches;
	int		qos;
	const EntityId_t *eid;
} MatchData_t;

typedef enum {
	EI_NEW,
	EI_UPDATE,
	EI_DELETE
} InfoType_t;

static void dr2dt (DiscoveredTopicQos *qp, const DiscoveredReaderData *info)
{
	qp->durability = info->qos.durability;
	qp->durability_service = qos_def_topic_qos.durability_service;
	qp->deadline = info->qos.deadline;
	qp->latency_budget = info->qos.latency_budget;
	qp->liveliness = info->qos.liveliness;
	qp->reliability = info->qos.reliability;
	qp->transport_priority = qos_def_topic_qos.transport_priority;
	qp->lifespan = qos_def_topic_qos.lifespan;
	qp->destination_order = info->qos.destination_order;
	qp->history = qos_def_topic_qos.history;
	qp->resource_limits = qos_def_topic_qos.resource_limits;
	qp->ownership = info->qos.ownership;
	qp->topic_data = str_ref (info->qos.topic_data);
}

/* user_topic_notify -- Notify a discovered Topic to discovery listeners. */

static void user_topic_notify (Topic_t *tp, int new1)
{
	Domain_t	*dp;
	KeyHash_t	hash;
	Reader_t	*rp;
	Cache_t		cache;
	Change_t	*cp;
	InstanceHandle	h;
	int		error;

	dp = tp->domain;
	rp = dp->builtin_readers [BT_Topic];
	if (!rp)
		return;

	cp = hc_change_new ();
	if (!cp)
		goto notif_err;

	lock_take (rp->r_lock);
	cache = rp->r_cache;
	cp->c_writer = cp->c_handle = tp->entity.handle;
	if (new1) {
		topic_key_from_name (str_ptr (tp->name),
				     str_len (tp->name) - 1,
				     str_ptr (tp->type->type_name),
				     str_len (tp->type->type_name) - 1,
				     &hash);
		memset (&hash.hash [sizeof (GuidPrefix_t)], 0, sizeof (EntityId_t));
		if (hc_lookup_hash (cache, &hash, hash.hash,
		    		     sizeof (DDS_BuiltinTopicKey_t),
				     &h, 0, 0, NULL) &&
		    h != cp->c_handle)
			hc_inst_free (cache, h); /* Remove lingering items. */
		hc_lookup_hash (cache, &hash, hash.hash, 
				sizeof (DDS_BuiltinTopicKey_t),
				&cp->c_handle,
				LH_ADD_SET_H, 0, NULL);
	}
 	cp->c_kind = ALIVE;
 	cp->c_data = cp->c_xdata;
 	cp->c_length = sizeof (cp->c_writer);
 	memcpy (cp->c_xdata, &cp->c_writer, sizeof (cp->c_writer));
	error = hc_add_inst (cache, cp, NULL, 0);
	if (!error)
		tp->entity.flags |= EF_CACHED;
	lock_release (rp->r_lock);
	if (!error)
		return;

    notif_err:
	warn_printf ("Discovered topic notification failed!");
}

#ifdef SIMPLE_DISCOVERY

static void user_notify_delete (Domain_t *dp,
				Builtin_Type_t type,
				InstanceHandle h)
{
	Reader_t	  *rp;
	Cache_t		  cache;
	Change_t	  *cp;
	int		  error;
	static const char *btype_str [] = {
		"participant", "writer", "reader", "topic"
	};

	rp = dp->builtin_readers [type];
	if (!rp)
		return;

	cp = hc_change_new ();
	if (!cp)
		return;

	lock_take (rp->r_lock);
	cache = rp->r_cache;
	cp->c_writer = cp->c_handle = h;
	cp->c_kind = NOT_ALIVE_UNREGISTERED;
	cp->c_data = NULL;
	cp->c_length = 0;
	error = hc_add_inst (cache, cp, NULL, 0);
	lock_release (rp->r_lock);
	if (!error)
		return;

	warn_printf ("Deletion of discovered %s notification failed! (%d)", 
							btype_str [type], error);
}

#endif
 
#ifdef DDS_TYPECODE

/* tc_typesupport -- When a new type is discovered, this function must be used
		     to create the typesupport in order to have a real type. */
		     
static TypeSupport_t *tc_typesupport (unsigned char *tc, const char *name)
{
	TypeSupport_t	*ts;
	char		*cp;
	unsigned	name_len;
	VTC_Header_t	*vh;

	name_len = strlen (name) + 1;
	ts = xmalloc (sizeof (TypeSupport_t) + name_len);
	if (!ts)
		return (NULL);

	memset (ts, 0, sizeof (TypeSupport_t));
	cp = (char *) (ts + 1);
	memcpy (cp, name, name_len);
	ts->ts_name = cp;
	ts->ts_prefer = MODE_V_TC;
	ts->ts_origin = TSO_Typecode;
	ts->ts_users = 1;
	vh = (VTC_Header_t *) tc;
	vh->nrefs_ext++;
	ts->ts_vtc = vh;
	return (ts);
}

/*#define LOG_TC_UNIQUE*/
#ifdef LOG_TC_UNIQUE
#define	tcu_print(s)		dbg_printf (s)
#define	tcu_print1(s,a1)	dbg_printf (s,a1)
#define	tcu_print2(s,a1,a2)	dbg_printf (s,a1,a2)
#define	tcu_print3(s,a1,a2,a3)	dbg_printf (s,a1,a2,a3)
#else
#define	tcu_print(s)
#define	tcu_print1(s,a1)
#define	tcu_print2(s,a1,a2)
#define	tcu_print3(s,a1,a2,a3)
#endif

/* tc_unique -- Return unique typecode data by comparing the proposed data
		with the data of other topic endpoints.  If alternative
		typecode data is found, the proposed data is released and the
		existing data is reused. */

static unsigned char *tc_unique (Topic_t       *tp,
				 Endpoint_t    *ep, 
				 unsigned char *tc,
				 int           *incompatible)
{
	VTC_Header_t	*hp;
	unsigned char	*ntc, *xtc;
	Endpoint_t	*xep;
	int		same;

	ntc = NULL;
	tcu_print2 ("tc_unique(@%p, %p):", ep, tc);
	*incompatible = 0;
	do {
		if (tp->type->type_support) {
			if (tp->type->type_support->ts_prefer >= MODE_V_TC) {
				tcu_print2 ("{T:%p*%u}", tp->type->type_support->ts_vtc,
						 tp->type->type_support->ts_vtc->nrefs_ext);
				if (vtc_equal ((unsigned char *) tp->type->type_support->ts_vtc, tc)) {
					ntc = (unsigned char *) tp->type->type_support->ts_vtc;
					break;
				}
			}
			else {
				if (!vtc_compatible (tp->type->type_support,
						     tc,
						     &same))
					*incompatible = 1;
				else if (same) {
					tcu_print (" use ~0!\r\n");
					xfree (tc);
					return (TC_IS_TS);
				}
			}
		}
		for (xep = tp->writers; xep; xep = xep->next) {
			if (xep == ep || !entity_discovered (xep->entity.flags))
				continue;

			xtc = ((DiscoveredWriter_t *) xep)->dw_tc;
			if (!xtc || xtc == TC_IS_TS)
				continue;

			tcu_print3 ("{W%p:%p*%u}", xep, xtc, ((VTC_Header_t *) xtc)->nrefs_ext);
			if (vtc_equal (xtc, tc)) {
				ntc = xtc;
				break;
			}
		}
		if (ntc)
			break;

		for (xep = tp->readers; xep; xep = xep->next) {
			if (xep == ep || !entity_discovered (xep->entity.flags))
				continue;

			xtc = ((DiscoveredReader_t *) xep)->dr_tc;
			if (!xtc || xtc == TC_IS_TS)
				continue;

			tcu_print3 ("{R%p:%p*%u}", xep, xtc, ((VTC_Header_t *) xtc)->nrefs_ext);
			if (vtc_equal (xtc, tc)) {
				ntc = xtc;
				break;
			}
		}
	}
	while (0);
	if (ntc) {
		xtc = ntc;
		hp = (VTC_Header_t *) ntc;
		hp->nrefs_ext++;
		xfree (tc);
	}
	else
		xtc = tc;
	tcu_print1 (" use %p!\r\n", xtc);
	return (xtc);
}

/* tc_update -- Attempts to update the Typecode of a Discovered Reader. */

static int tc_update (Endpoint_t    *ep,
		      unsigned char **ep_tc,
		      unsigned char **new_tc,
		      int           *incompatible)
{
	TopicType_t	*ttp = ep->topic->type;
	TypeSupport_t	*tsp = (TypeSupport_t *) ttp->type_support;

	tcu_print2 ("tc_update(@%p,%p)", *ep, *new_tc);
	*incompatible = 0;
	if (!tsp) {
		tsp = tc_typesupport (*ep_tc, str_ptr (ttp->type_name));
		if (!tsp)
			return (0);

		*ep_tc = *new_tc;
		tcu_print (" - new TS\r\n");
	}
	else if (!*ep_tc || *ep_tc == TC_IS_TS)
		*ep_tc = tc_unique (ep->topic, ep, *new_tc, incompatible);
	else if (vtc_equal (*ep_tc, *new_tc)) {	/* Same as previous -- just reuse previous. */
		xfree (*new_tc);
		tcu_print (" - same\r\n");
	}
	else if (tsp->ts_prefer >= MODE_V_TC &&	/* Typesupport *is* endpoint type? */
	         tsp->ts_vtc == (VTC_Header_t *) *ep_tc) {
		if ((tsp->ts_vtc->nrefs_ext & NRE_NREFS) == 2) { /* No one else using it? */
			xfree (*ep_tc);	/* -> then just update it in both locations. */
			*ep_tc = NULL;
			tsp->ts_vtc = NULL;
			tcu_print (" - replace\r\n");
			*ep_tc = tc_unique (ep->topic, ep, *new_tc, incompatible);
			tsp->ts_vtc = (VTC_Header_t *) *ep_tc;
			tsp->ts_vtc->nrefs_ext++;
		}
		else {				/* Multiple endpoints using old typecode! */
			tsp->ts_vtc->nrefs_ext--;
			*ep_tc = NULL;
			tcu_print (" - update(1)\r\n");
			*ep_tc = tc_unique (ep->topic, ep, *new_tc, incompatible);
		}
	}
	else {
		vtc_free (*ep_tc);
		*ep_tc = NULL;
		tcu_print (" - update(2)\r\n");
		*ep_tc = tc_unique (ep->topic, ep, *new_tc, incompatible);
	}
	*new_tc = NULL;
	return (1);
}

#endif /* DDS_TYPECODE */

/* add_dr_topic -- Add a new topic, based on discovered reader info. */

static Topic_t *add_dr_topic (Participant_t        *pp,
			      Topic_t              *tp,
			      DiscoveredReaderData *info,
			      DiscoveredTopicQos   *qos,
			      int                  ignored)
{
	int	new;

	tp = topic_create (pp,
			   tp,
			   str_ptr (info->topic_name),
			   str_ptr (info->type_name),
			   &new);
	if (!tp || ignored)
		return (tp);

	if (!new || tp->nrrefs > 1 || tp->nlrefs) {
		if (!new &&
		    pp->p_domain->builtin_readers [BT_Topic] && 
		    (tp->entity.flags & EF_REMOTE) != 0)
			user_topic_notify (tp, 0);
		else if (new)
			tp->entity.flags |= EF_NOT_IGNORED;
		return (tp);
	}
	tp->qos = qos_disc_topic_new (qos);
	if (!tp->qos) {
		topic_delete (pp, tp, NULL, NULL);
		return (NULL);
	}
	/*dbg_printf ("!!Qos_disc_topic_new (%p) for %s/%s,\r\n", tp->qos,
			str_ptr (info->topic_name), str_ptr (info->type_name));*/
	tp->entity.flags |= EF_NOT_IGNORED;

	/* Deliver topic info to user topic reader. */
	if ((tp->entity.flags & EF_LOCAL) == 0 &&
	     pp->p_domain->builtin_readers [BT_Topic])
		user_topic_notify (tp, 1);

	return (tp);
}

#ifdef SIMPLE_DISCOVERY

/* update_dr_topic -- Update topic QoS, based on discovered reader info. */

static int update_dr_topic_qos (Topic_t *tp, const DiscoveredReaderData *info)
{
	DiscoveredTopicQos	qos_data;
	int			error;

	/* Locally created topic info has precedence: don't update if local. */
	if ((tp->entity.flags & EF_LOCAL) == 0) {
		dr2dt (&qos_data, info);
		error = qos_disc_topic_update (&tp->qos, &qos_data);
		if (!error && tp->domain->builtin_readers [BT_Topic])
			user_topic_notify (tp, 0);
	}
	return (0);
}

#endif /* SIMPLE_DISCOVERY */

static void dw2dt (DiscoveredTopicQos *qp, const DiscoveredWriterData *info)
{
	qp->durability = info->qos.durability;
	qp->durability_service = info->qos.durability_service;
	qp->deadline = info->qos.deadline;
	qp->latency_budget = info->qos.latency_budget;
	qp->liveliness = info->qos.liveliness;
	qp->reliability = info->qos.reliability;
	qp->transport_priority = qos_def_topic_qos.transport_priority;
	qp->lifespan = info->qos.lifespan;
	qp->destination_order = info->qos.destination_order;
	qp->history = qos_def_topic_qos.history;
	qp->resource_limits = qos_def_topic_qos.resource_limits;
	qp->ownership = info->qos.ownership;
	qp->topic_data = str_ref (info->qos.topic_data);
}

/* add_dw_topic -- Add a new topic, based on discovered writer info. */

static Topic_t *add_dw_topic (Participant_t        *pp,
			      Topic_t              *tp,
			      DiscoveredWriterData *info,
			      DiscoveredTopicQos   *qos,
			      int                  ignored)
{
	int	new;

	tp = topic_create (pp,
			   tp,
			   str_ptr (info->topic_name),
			   str_ptr (info->type_name),
			   &new);
	if (!tp || ignored)
		return (tp);

	if (!new || tp->nrrefs > 1 || tp->nlrefs) {
		if (!new &&
		    pp->p_domain->builtin_readers [BT_Topic] &&
		    (tp->entity.flags & EF_REMOTE) != 0)
			user_topic_notify (tp, 0);
		else if (new)
			tp->entity.flags |= EF_NOT_IGNORED;
		return (tp);
	}
	tp->qos = qos_disc_topic_new (qos);
	if (!tp->qos) {
		topic_delete (pp, tp, NULL, NULL);
		return (NULL);
	}
	tp->entity.flags |= EF_NOT_IGNORED;

	/* Deliver topic info to user topic reader. */
	if (pp->p_domain->builtin_readers [BT_Topic])
		user_topic_notify (tp, 1);

	return (tp);
}

#ifdef SIMPLE_DISCOVERY

/* update_dw_topic -- Update topic QoS, based on discovered writer info. */

static int update_dw_topic_qos (Topic_t *tp, const DiscoveredWriterData *info)
{
	DiscoveredTopicQos	qos_data;
	int			error;

	/* Locally created topic info has precedence: don't update if local. */
	if ((tp->entity.flags & EF_LOCAL) == 0) {
		dw2dt (&qos_data, info);
		error = qos_disc_topic_update (&tp->qos, &qos_data);
		if (!error && tp->domain->builtin_readers [BT_Topic])
			user_topic_notify (tp, 0);
	}
	return (0);
}

#endif

#define	locator_list_swap(l1,l2,t)	t = l1; l1 = l2; l2 = t

/* user_reader_notify -- Notify a discovered Reader to discovery listeners. */

static void user_reader_notify (DiscoveredReader_t *rp, int new1)
{
	Domain_t	*dp;
	KeyHash_t	hash;
	Reader_t	*nrp;
	Cache_t		cache;
	Change_t	*cp;
	InstanceHandle	h;
	int		error;

	dp = rp->dr_participant->p_domain;
	nrp = dp->builtin_readers [BT_Subscription];
	if (!nrp)
		return;

	cp = hc_change_new ();
	if (!cp)
		goto notif_err;

	memcpy (hash.hash, &rp->dr_participant->p_guid_prefix, 
						sizeof (GuidPrefix_t) - 4);
	memcpy (&hash.hash [sizeof (GuidPrefix_t) - 4], &rp->dr_entity_id,
							sizeof (EntityId_t));
	memset (&hash.hash [12], 0, 4); 
	lock_take (nrp->r_lock);
	cache = nrp->r_cache;
	cp->c_writer = cp->c_handle = rp->dr_handle;
	if (new1 &&
	    !hc_lookup_hash (cache, &hash, hash.hash,
	    		     sizeof (DDS_BuiltinTopicKey_t),
			     &h, 0, 0, NULL) &&
	    h != cp->c_handle)
		hc_inst_free (cache, h);
	hc_lookup_hash (cache, &hash, hash.hash, sizeof (DDS_BuiltinTopicKey_t),
				&cp->c_handle, (new1) ? LH_ADD_SET_H : 0, 0, NULL);
	cp->c_kind = ALIVE;
	cp->c_data = cp->c_xdata;
	cp->c_length = sizeof (cp->c_writer);
	memcpy (cp->c_xdata, &cp->c_writer, sizeof (cp->c_writer));
	error = hc_add_inst (cache, cp, NULL, 0);
	if (!error)
		rp->dr_flags |= EF_CACHED;
	lock_release (nrp->r_lock);
	if (!error)
		return;

    notif_err:
	warn_printf ("Discovered reader notification failed!");
}

#define	local_active(fh) (((fh) & (EF_LOCAL | EF_ENABLED)) == (EF_LOCAL | EF_ENABLED))
#define	remote_active(fh) (((fh) & (EF_LOCAL | EF_NOT_IGNORED)) == EF_NOT_IGNORED)

/* disc_subscription_add -- Add a Discovered Reader.
			    On entry/exit: DP locked. */

static int disc_subscription_add (Participant_t        *pp,
				  DiscoveredReader_t   *drp,
				  const UniQos_t       *qp,
				  Topic_t              *tp,
				  Writer_t             *wp,
				  DiscoveredReaderData *info)
{
	Endpoint_t		*ep;
	Topic_t			*ptp;
	int			new_topic = 0, ret = DDS_RETCODE_OK, ignored;
	DDS_QOS_POLICY_ID	qid;
	DiscoveredTopicQos	qos_data;
	int			incompatible = 0;

	ignored = 0;
	ptp = topic_lookup (pp, str_ptr (info->topic_name));
	if (!ptp) {
		/*log_printf (DISC_ID, 0, "Discovery: add_dr_topic (%s)\r\n", str_ptr (info->topic_name));*/
		dr2dt (&qos_data, info);
#ifdef DDS_SECURITY
		if (pp->p_domain->security &&
		    check_peer_topic (pp->p_permissions,
				      str_ptr (info->topic_name),
				      &qos_data) != DDS_RETCODE_OK) {
			ignored = 1;
			endpoint_delete (pp, &drp->dr_ep);
			return (DDS_RETCODE_ACCESS_DENIED);
		}
#endif
		tp = add_dr_topic (pp, tp, info, &qos_data, ignored);
		if (!tp || entity_ignored (tp->entity.flags)) {
			drp->dr_flags &= ~EF_NOT_IGNORED;
			return (DDS_RETCODE_OUT_OF_RESOURCES);
		}
		new_topic = 1;
	}
#ifdef DDS_TYPECODE
	if (info->typecode) {
		if (!tp->type->type_support) {
			tp->type->type_support = tc_typesupport (info->typecode,
						 str_ptr (tp->type->type_name));
			drp->dr_tc = info->typecode;
		}
		else
			drp->dr_tc = tc_unique (tp, &drp->dr_ep,
						info->typecode, &incompatible);
		info->typecode = NULL;
	}
	else
		drp->dr_tc = NULL;
#endif
	if (lock_take (tp->lock)) {
		warn_printf ("disc_subscription_add: topic lock error");
		return (DDS_RETCODE_BAD_PARAMETER);
	}
	if (!new_topic)
		tp->nrrefs++;
	drp->dr_topic = tp;

#ifdef DDS_SECURITY
	if (!ignored &&
	    pp->p_domain->security) {
		info->qos.partition = qp->partition;
		info->qos.topic_data = qp->topic_data;
		info->qos.user_data = qp->user_data;
		info->qos.group_data = qp->group_data;
		
		if (check_peer_reader (pp->p_permissions,
				   str_ptr (info->topic_name),
				   &info->qos) != DDS_RETCODE_OK)
			ignored = 1;
		info->qos.partition = NULL;
		info->qos.topic_data = NULL;
		info->qos.user_data = NULL;
		info->qos.group_data = NULL;
	}
#endif
	if (ignored) {
		drp->dr_qos = NULL;
		drp->dr_ucast = NULL;
		drp->dr_mcast = NULL;
		drp->dr_flags &= ~EF_NOT_IGNORED;
		drp->dr_content_filter = NULL;
		ret = DDS_RETCODE_ACCESS_DENIED;
		goto done;
	}
	drp->dr_qos = qos_add (qp);
	if (!drp->dr_qos) {
		drp->dr_flags &= ~EF_NOT_IGNORED;
		ret = DDS_RETCODE_OUT_OF_RESOURCES;
		goto done;
	}
	drp->dr_ucast = info->proxy.ucast;
	info->proxy.ucast = NULL;
	drp->dr_mcast = info->proxy.mcast;
	info->proxy.mcast = NULL;
	if (info->proxy.exp_il_qos)
		drp->dr_flags |= EF_INLINE_QOS;

	drp->dr_time_based_filter = info->qos.time_based_filter;
	drp->dr_content_filter = info->filter;
	info->filter = NULL;
	if (drp->dr_content_filter) {
			ret = sql_parse_filter (tp->type->type_support,
						str_ptr (drp->dr_content_filter->filter.expression),
						&drp->dr_content_filter->program);
			bc_cache_init (&drp->dr_content_filter->cache);
			if (!ret)
				drp->dr_flags |= EF_FILTERED;
			else
				ret = DDS_RETCODE_OK;
	}
	drp->dr_flags |= EF_NOT_IGNORED;
	if (pp->p_domain->builtin_readers [BT_Subscription])
		user_reader_notify (drp, 1);

	/* Check if not ignored. */
	if ((drp->dr_flags & EF_NOT_IGNORED) == 0)
		goto done;

	/* Hook into Topic Readers list. */
	drp->dr_next = tp->readers;
	tp->readers = &drp->dr_ep;

	/* Check for matching local endpoints. */
	if (incompatible) {
		dcps_inconsistent_topic (tp);
		goto done;
	}
	for (ep = tp->writers; ep; ep = ep->next) {
		if (!local_active (ep->entity.flags))
			continue;

#ifndef RW_TOPIC_LOCK
		if (lock_take (((Writer_t *) ep)->w_lock)) {
			warn_printf ("disc_subscription_add: writer lock error");
			continue;
		}
#endif
		if ((Writer_t *) ep == wp)
			disc_new_match ((LocalEndpoint_t *) ep, &drp->dr_ep);
		else if (!qos_same_partition (ep->u.publisher->qos.partition,
					         drp->dr_qos->qos.partition))
			dcps_offered_incompatible_qos ((Writer_t *) ep, DDS_PARTITION_QOS_POLICY_ID);
		else if (!qos_match (qos_ptr (ep->qos), &ep->u.publisher->qos,
			     	    qp, NULL, &qid))
			dcps_offered_incompatible_qos ((Writer_t *) ep, qid);
		else
			disc_new_match ((LocalEndpoint_t *) ep, &drp->dr_ep);
#ifndef RW_TOPIC_LOCK
		lock_release (((Writer_t *) ep)->w_lock);
#endif
	}

    done:
	lock_release (tp->lock);
	return (ret);
}

/* disc_remote_reader_add -- Add a new discovered Reader as if it was discovered
			     via a Discovery protocol.
			     On entry/exit: no locks taken. */

DiscoveredReader_t *disc_remote_reader_add (Participant_t *pp,
					    DiscoveredReaderData *info)
{
	DiscoveredReader_t	*drp;
	UniQos_t		qos;
	int			new_node;

	if (lock_take (pp->p_domain->lock))
		return (NULL);

	drp = (DiscoveredReader_t *) endpoint_create (pp, pp, 
					&info->proxy.guid.entity_id, &new_node);
	if (drp && new_node) {
		qos_disc_reader_set (&qos, &info->qos);
		disc_subscription_add (pp, drp, &qos, NULL, NULL, info);
	}
	lock_release (pp->p_domain->lock);
	return (drp);
}

#ifdef SIMPLE_DISCOVERY

/* discovered_reader_cleanup -- Cleanup a previously discovered reader. */

static void discovered_reader_cleanup (DiscoveredReader_t *rp,
				       int                ignore,
				       int                *p_last_topic,
				       int                *topic_gone)
{
	Topic_t		*tp;
	Endpoint_t	*ep, *prev;
	Participant_t	*pp;

	/* Break all existing endpoint matches. */
	tp = rp->dr_topic;
	if (lock_take (tp->lock)) {
		warn_printf ("discovered_reader_cleanup: topic lock error");
		return;
	}
	if (rp->dr_rtps)
		for (ep = tp->writers; ep; ep = ep->next) {
			if ((ep->entity.flags & EF_LOCAL) == 0)
				continue;

#ifndef RW_TOPIC_LOCK
			if (lock_take (((Writer_t *) ep)->w_lock)) {
				warn_printf ("discovered_reader_cleanup: writer lock error");
				continue;
			}
#endif
			if (rtps_writer_matches ((Writer_t *) ep, rp))
				disc_end_match ((LocalEndpoint_t *) ep, &rp->dr_ep);
#ifndef RW_TOPIC_LOCK
			lock_release (((Writer_t *) ep)->w_lock);
#endif
		}

	/* Remove from topic list. */
	for (prev = NULL, ep = tp->readers;
	     ep && ep != &rp->dr_ep;
	     prev = ep, ep = ep->next)
		;

	if (ep) {
		/* Still in topic list: remove it. */
		if (prev)
			prev->next = ep->next;
		else
			tp->readers = ep->next;
	}

	/* Cleanup all endpoint data. */
	if (rp->dr_content_filter) {
		filter_data_cleanup (rp->dr_content_filter);
		xfree (rp->dr_content_filter);
		rp->dr_content_filter = NULL;
	}
	locator_list_delete_list (&rp->dr_ucast);
	locator_list_delete_list (&rp->dr_mcast);
	qos_disc_reader_free (rp->dr_qos);
	rp->dr_qos = NULL;
#ifdef DDS_TYPECODE
	if (rp->dr_tc && rp->dr_tc != TC_IS_TS) {
		vtc_free (rp->dr_tc);
		rp->dr_tc = NULL;
	}
#endif

	pp = rp->dr_participant;
	if (ignore) {
		rp->dr_flags &= ~EF_NOT_IGNORED;
		lock_release (tp->lock);
	}
	else {
		/* Free the endpoint. */
		endpoint_delete (pp, &rp->dr_ep);
		topic_delete (pp, tp, p_last_topic, topic_gone);
	}
}

/* disc_subscription_remove -- Remove a Discovered Reader.
			       On entry/exit: DP locked. */

static void disc_subscription_remove (Participant_t      *pp,
				      DiscoveredReader_t *rp)
{
	Topic_t		*tp = rp->dr_topic;
	Domain_t	*dp;
	InstanceHandle	topic_handle;
	int		last_p_topic, topic_gone;

	if (entity_shutting_down (rp->dr_flags) || !tp)
		return;

	dp = tp->domain;
	topic_handle = tp->entity.handle;
	rp->dr_flags |= EF_SHUTDOWN;
	if (pp->p_domain->builtin_readers [BT_Subscription])
		user_notify_delete (dp, BT_Subscription, rp->dr_handle);

	discovered_reader_cleanup (rp, 0, &last_p_topic, &topic_gone);

	if (topic_gone && pp->p_domain->builtin_readers [BT_Topic])
		user_notify_delete (dp, BT_Topic, topic_handle);
}

/* disc_subscription_update -- Update a Discovered Reader.
			       On entry/exit: DP locked. */

static int disc_subscription_update (Participant_t        *pp,
				     DiscoveredReader_t   *drp,
				     DiscoveredReaderData *info)
{
	Endpoint_t	*ep;
	Writer_t	*wp;
	Topic_t		*tp;
	LocatorList_t	tlp;
	FilterData_t	*fp;
	Strings_t	*parsp;
	DDS_QOS_POLICY_ID qid;
	int		old_match, new_match, ret;
	UniQos_t	qp;
	int		incompatible = 0;

	/* If the topic name/type name changes or the Reader/Topic QoS becomes
	   incompatible, simply delete and recreate the endpoint. */
	if (strcmp (str_ptr (drp->dr_topic->name),
		    str_ptr (info->topic_name)) ||
	    strcmp (str_ptr (drp->dr_topic->type->type_name),
		    str_ptr (info->type_name)) ||
	    update_dr_topic_qos (drp->dr_topic, info) ||
	    qos_disc_reader_update (&drp->dr_qos, &info->qos)
#ifdef DDS_TYPECODE
	 || (info->typecode && 
	     !tc_update (&drp->dr_ep, &drp->dr_tc,
	     		 &info->typecode, &incompatible))
#endif
	    ) {
		tp = drp->dr_topic;
		disc_subscription_remove (pp, drp);
		qos_disc_reader_set (&qp, &info->qos);        
		return (disc_subscription_add (pp, drp, &qp, tp, NULL, info));
	}
	tp = drp->dr_topic;
	if (lock_take (tp->lock)) {
		warn_printf ("disc_subscription_update: topic lock error");
		return (DDS_RETCODE_ERROR);
	}
	if (info->proxy.exp_il_qos)
		drp->dr_flags |= EF_INLINE_QOS;
	else
		drp->dr_flags &= ~EF_INLINE_QOS;

	/* Update locator lists - notify if changed. */
	if (!locator_list_equal (drp->dr_ucast, info->proxy.ucast)) {
		locator_list_swap (drp->dr_ucast, info->proxy.ucast, tlp);
		if (drp->dr_rtps)
			rtps_endpoint_locators_update (&drp->dr_ep, 0);
	}
	if (!locator_list_equal (drp->dr_mcast, info->proxy.mcast)) {
		locator_list_swap (drp->dr_mcast, info->proxy.mcast, tlp);
		if (drp->dr_rtps)
			rtps_endpoint_locators_update (&drp->dr_ep, 1);
	}

	/* Update time-based filter - notify if changed. */
	if (memcmp (&drp->dr_time_based_filter.minimum_separation,
		    &info->qos.time_based_filter.minimum_separation,
		    sizeof (DDS_TimeBasedFilterQosPolicy))) {
		drp->dr_time_based_filter.minimum_separation = 
		       info->qos.time_based_filter.minimum_separation;
		if (drp->dr_rtps)
			rtps_endpoint_time_filter_update (&drp->dr_ep);
	}

	/* Update content filter. */
	if (drp->dr_content_filter &&
	    info->filter &&
	    !strcmp (str_ptr (drp->dr_content_filter->filter.class_name),
		     str_ptr (info->filter->filter.class_name)) &&
	    !strcmp (str_ptr (drp->dr_content_filter->filter.expression),
		     str_ptr (info->filter->filter.expression))) {

		/* Filter wasn't changed - simply update filter parameters. */
		parsp = drp->dr_content_filter->filter.expression_pars;
		drp->dr_content_filter->filter.expression_pars = info->filter->filter.expression_pars;
		info->filter->filter.expression_pars = parsp;
		bc_cache_flush (&drp->dr_content_filter->cache);
	}
	else {
		/* Simply swap active filter with new parameters. */
		fp = drp->dr_content_filter;
		drp->dr_content_filter = info->filter;
		info->filter = fp;
		if (drp->dr_content_filter &&
		    (drp->dr_flags & EF_NOT_IGNORED) != 0) {
			ret = sql_parse_filter (drp->dr_topic->type->type_support,
						str_ptr (drp->dr_content_filter->filter.expression),
						&drp->dr_content_filter->program);
			bc_cache_init (&drp->dr_content_filter->cache);
			if (!ret)
				drp->dr_flags |= EF_FILTERED;
			else
				drp->dr_flags &= ~EF_FILTERED;
		}
		else
			drp->dr_flags &= ~EF_FILTERED;
	}
	if (pp->p_domain->builtin_readers [BT_Subscription])
		user_reader_notify (drp, 0);

	/* Check if not ignored. */
	if ((drp->dr_flags & EF_NOT_IGNORED) == 0) {
		lock_release (tp->lock);
		return (DDS_RETCODE_OK);
	}

	/* Check for matching local endpoints. */
	if (incompatible)
		dcps_inconsistent_topic (tp);
	for (ep = tp->writers; ep; ep = ep->next) {
		if (!local_active (ep->entity.flags))
			continue;

		wp = (Writer_t *) ep;
#ifndef RW_TOPIC_LOCK
		if (lock_take (wp->w_lock)) {
			warn_printf ("disc_subscription_update: writer lock error");
			continue;
		}
#endif
		old_match = (drp->dr_rtps &&
			     rtps_writer_matches (wp, drp));
		new_match = 0;
		if (incompatible)
			/* Different types: cannot match! */;
		else if (!qos_same_partition (wp->w_publisher->qos.partition,
					 drp->dr_qos->qos.partition))
			dcps_offered_incompatible_qos (wp, DDS_PARTITION_QOS_POLICY_ID);
		else if (!qos_match (qos_ptr (wp->w_qos), &wp->w_publisher->qos,
			     	     &drp->dr_qos->qos, NULL, &qid))
			dcps_offered_incompatible_qos (wp, qid);
		else
			new_match = 1;
		if (old_match && !new_match)
			disc_end_match (&wp->w_lep, &drp->dr_ep);
		else if (!old_match && new_match)
			disc_new_match (&wp->w_lep, &drp->dr_ep);
#ifndef RW_TOPIC_LOCK
		lock_release (wp->w_lock);
#endif
	}
	lock_release (tp->lock);
	return (DDS_RETCODE_OK);
}

#ifdef DISC_EV_TRACE
#define	dtrc_print0(s)		log_printf (DISC_ID, 0, s)
#define	dtrc_print1(s,a)	log_printf (DISC_ID, 0, s, a)
#define	dtrc_print2(s,a1,a2)	log_printf (DISC_ID, 0, s, a1, a2)
#else
#define	dtrc_print0(s)
#define	dtrc_print1(s,a)
#define	dtrc_print2(s,a1,a2)
#endif


#define	CDD_WR_MATCH(wpp,rpp,wqp,rqp) (qos_same_partition (wpp,rpp) && \
				       qos_match (wqp,NULL,rqp,NULL,NULL))

/* sedp_subscription_event -- Receive a subscription event.
			      On entry/exit: DP, R(rp) locked. */

static void sedp_subscription_event (Reader_t *rp, NotificationType_t t, int cdd)
{
	Domain_t		*dp = rp->r_subscriber->domain;
	Participant_t		*pp;
	ChangeData_t		change;
	DiscoveredReaderData	*info = NULL, tinfo;
	Topic_t			*tp;
	DiscoveredReader_t	*drp;
	Writer_t		*mwp;
	UniQos_t		qos;
	InfoType_t		type;
	GUID_t			*guidp;
	int			error;

	if (t != NT_DATA_AVAILABLE)
		return;


	rp->r_status &= ~DDS_DATA_AVAILABLE_STATUS;
	for (;;) {
		if (info) {
			pid_reader_data_cleanup (info);
			xfree (info);
			info = NULL;
		}
		/*dtrc_print0 ("SEDP-Sub: get samples ");*/
		error = disc_get_data (rp, &change);
		if (error) {
			/*dtrc_print0 ("- none\r\n");*/
			break;
		}
		/*dtrc_print1 ("- valid(%u)\r\n", kind);*/
		if (change.kind != ALIVE) {
			error = hc_get_key (rp->r_cache, change.h, &tinfo, 0);
			if (error)
				continue;

			guidp = &tinfo.proxy.guid;       
			type = EI_DELETE;
			hc_inst_free (rp->r_cache, change.h);
		}
		else {
			info = change.data;
			if (!info->topic_name || !info->type_name) {
				hc_inst_free (rp->r_cache, change.h);
				continue;
			}
			type = EI_NEW;
			guidp = &info->proxy.guid;
		}
			pp = entity_participant (change.writer);
		if (!pp ||				/* Not found. */
		    pp == &dp->participant ||		/* Own sent info. */
		    entity_ignored (pp->p_flags) ||
		    entity_shutting_down (pp->p_flags)) {		/* Ignored. */
			hc_inst_free (rp->r_cache, change.h);
			dtrc_print0 ("SEDP-Sub: unneeded!\r\n");
			continue;	/* Filter out unneeded info. */
		}

		/* Subscription from remote participant. */
		if (type == EI_DELETE) {
			drp = (DiscoveredReader_t *) endpoint_lookup (pp,
						    &guidp->entity_id);
			if (!drp) {
				dtrc_print0 ("SEDP-Sub: DELETE && doesn't exist!\r\n");
				continue; /* If doesn't exist - no action. */
			}
			if (!drp->dr_topic) {
				endpoint_delete (pp, &drp->dr_ep);
				continue; /* Ignored topic -- only endpoint. */
			}
			if (sedp_log)
				log_printf (SEDP_ID, 0, "SEDP: Deleted subscription (%s/%s) from peer!\r\n",
					str_ptr (drp->dr_topic->name),
					str_ptr (drp->dr_topic->type->type_name));
			disc_subscription_remove (pp, drp);
			hc_inst_free (rp->r_cache, change.h);
			continue;
		}

		/* Do we know this topic? */
		tp = topic_lookup (&dp->participant, str_ptr (info->topic_name));
		if (tp && entity_ignored (tp->entity.flags)) {
			hc_inst_free (rp->r_cache, change.h);
			dtrc_print1 ("SEDP: ignored topic (%s)!\r\n", str_ptr (info->topic_name));
			continue;	/* Ignored topic. */
		}

		/* Do we know this endpoint already? */
		drp = (DiscoveredReader_t *) endpoint_lookup (pp, &guidp->entity_id);
		if (drp) {
			if (entity_ignored (drp->dr_flags) || cdd) {
				hc_inst_free (rp->r_cache, change.h);
				continue; /* Ignored endpoint. */
			}
			dtrc_print1 ("Already exists (%s)!\r\n", str_ptr (info->topic_name));
			type = EI_UPDATE;
			disc_subscription_update (pp, drp, info);
			if (sedp_log)
				log_printf (SEDP_ID, 0, "SEDP: Updated subscription (%s/%s) from peer!\r\n",
						str_ptr (info->topic_name),
						str_ptr (info->type_name));
			hc_inst_free (rp->r_cache, change.h);
		}
		else {
			/* Get QoS parameters. */
			qos_disc_reader_set (&qos, &info->qos);
			mwp = NULL;
			/* Create new endpoint. */
			drp = (DiscoveredReader_t *) endpoint_create (pp,
					pp, &guidp->entity_id, NULL);
			if (!drp) {
				dtrc_print1 ("SEDP: Create endpoint (%s) not possible - exit!\r\n", str_ptr (info->topic_name));
				hc_inst_free (rp->r_cache, change.h);
				qos_disc_reader_restore (&info->qos, &qos);
				continue;  /* Can't create -- ignore. */
			}
			disc_subscription_add (pp, drp, &qos, tp, mwp, info);
			hc_inst_free (rp->r_cache, change.h);

			if (sedp_log)
				log_printf (SEDP_ID, 0, "SEDP: New subscription (%s/%s) from %s!\r\n",
						str_ptr (info->topic_name),
						str_ptr (info->type_name),
						(cdd) ? "CDD" : "peer");

		}
	}
	if (info) {
		pid_reader_data_cleanup (info);
		xfree (info);
	}
}

#endif /* !SIMPLE_DISCOVERY */

/* user_writer_notify -- Notify a discovered Writer to discovery listeners. */

static void user_writer_notify (DiscoveredWriter_t *wp, int new1)
{
	Domain_t	*dp;
	KeyHash_t	hash;
	Reader_t	*rp;
	Cache_t		cache;
	Change_t	*cp;
	InstanceHandle	h;
	int		error = 0;

	dp = wp->dw_participant->p_domain;
	rp = dp->builtin_readers [BT_Publication];
	if (!rp)
		return;

	cp = hc_change_new ();
	if (!cp)
		goto notif_err;

	memcpy (hash.hash, &wp->dw_participant->p_guid_prefix, 
						sizeof (GuidPrefix_t) - 4);
	memcpy (&hash.hash [sizeof (GuidPrefix_t) - 4], &wp->dw_entity_id,
							sizeof (EntityId_t));
	memset (&hash.hash [12], 0, 4); 
	lock_take (rp->r_lock);
	cache = rp->r_cache;
	cp->c_writer = cp->c_handle = wp->dw_handle;
	if (new1 &&
	    !hc_lookup_hash (cache, &hash, hash.hash,
	    		     sizeof (DDS_BuiltinTopicKey_t),
			     &h, 0, 0, NULL) &&
	    h != cp->c_handle)
		hc_inst_free (cache, h);
	hc_lookup_hash (cache, &hash, hash.hash, sizeof (DDS_BuiltinTopicKey_t),
					&cp->c_handle,
					(new1) ? LH_ADD_SET_H : 0, 0, NULL);
	cp->c_kind = ALIVE;
	cp->c_data = cp->c_xdata;
	cp->c_length = sizeof (cp->c_writer);
	memcpy (cp->c_xdata, &cp->c_writer, sizeof (cp->c_writer));
	error = hc_add_inst (cache, cp, NULL, 0);
	if (!error)
		wp->dw_flags |= EF_CACHED;
	lock_release (rp->r_lock);
	if (!error)
		return;

    notif_err:
	warn_printf ("Discovered writer notification failed! %d", error);
}

/* disc_match_readers_new -- Match local readers to a new remote writer.
			     On entry/exit: DP, TP locked. */

static void disc_match_readers_new (Topic_t            *tp,
				    Reader_t           *mrp, 
				    const UniQos_t     *qp,
				    DiscoveredWriter_t *dwp)
{
	Endpoint_t	*ep;
	Reader_t	*rp;
	DDS_QOS_POLICY_ID qid;

	for (ep = tp->readers; ep; ep = ep->next) {
		if (!local_active (ep->entity.flags))
			continue;

		rp = (Reader_t *) ep;
#ifndef RW_TOPIC_LOCK
		if (lock_take (rp->r_lock)) {
			warn_printf ("disc_match_readers_new: reader lock error");
			continue;
		}
#endif
		if (rp == mrp)
			disc_new_match (&rp->r_lep, &dwp->dw_ep);
		else if (!qos_same_partition (ep->u.subscriber->qos.partition,
						 dwp->dw_qos->qos.partition))
			dcps_requested_incompatible_qos (rp, DDS_PARTITION_QOS_POLICY_ID);
		else if (!qos_match (qp, NULL,
		    	             qos_ptr (rp->r_qos), &ep->u.subscriber->qos,
				     &qid))
			dcps_requested_incompatible_qos (rp, qid);
		else
			disc_new_match (&rp->r_lep, &dwp->dw_ep);
#ifndef RW_TOPIC_LOCK
		lock_release (rp->r_lock);
#endif
	}
}

/* disc_publication_add -- Add a discovered Writer.
			   On entry/exit: DP locked. */

static int disc_publication_add (Participant_t        *pp,
				 DiscoveredWriter_t   *dwp,
				 const UniQos_t       *qp,
				 Topic_t              *tp,
				 Reader_t             *rp,
				 DiscoveredWriterData *info)
{
	FilteredTopic_t		*ftp;
	Topic_t			*ptp;
	int			new_topic = 0, ret = DDS_RETCODE_OK, ignored;
	DiscoveredTopicQos	qos_data;
	int			incompatible = 0;

	ignored = 0;
	ptp = topic_lookup (pp, str_ptr (info->topic_name));
	if (!ptp) {
		/* log_printf (DISC_ID, 0, "Discovery: add_dw_topic (%s)\r\n", str_ptr (info->topic_name)); */
		dw2dt (&qos_data, info);
#ifdef DDS_SECURITY
		if (pp->p_domain->security &&
		    check_peer_topic (pp->p_permissions,
		    		      str_ptr (info->topic_name),
				      &qos_data) != DDS_RETCODE_OK) {
			ignored = 1;
			endpoint_delete (pp, &dwp->dw_ep);
			return (DDS_RETCODE_ACCESS_DENIED);
		}
#endif
		tp = add_dw_topic (pp, tp, info, &qos_data, ignored);
		if (!tp || entity_ignored (tp->entity.flags)) {
			dwp->dw_flags &= ~EF_NOT_IGNORED;
			return (DDS_RETCODE_OUT_OF_RESOURCES);
		}
		new_topic = 1;
	}
#ifdef DDS_TYPECODE
	if (info->typecode) {
		if (!tp->type->type_support) {
			tp->type->type_support = tc_typesupport (info->typecode,
						 str_ptr (tp->type->type_name));
			dwp->dw_tc = info->typecode;
		}
		else
			dwp->dw_tc = tc_unique (tp, &dwp->dw_ep,
						info->typecode, &incompatible);
		info->typecode = NULL;
	}
	else
		dwp->dw_tc = NULL;
#endif
	if (lock_take (tp->lock)) {
		warn_printf ("disc_publication_add: topic lock error");
		return (DDS_RETCODE_ERROR);
	}
	if (!new_topic)
		tp->nrrefs++;
	dwp->dw_topic = tp;

#ifdef DDS_SECURITY
	if (pp->p_domain->security) {
		info->qos.partition = qp->partition;
		info->qos.topic_data = qp->topic_data;
		info->qos.user_data = qp->user_data;
		info->qos.group_data = qp->group_data;
		
		if (check_peer_writer (pp->p_permissions,
				       str_ptr (info->topic_name),
				       &info->qos) != DDS_RETCODE_OK)
			ignored = 1;
		info->qos.partition = NULL;
		info->qos.topic_data = NULL;
		info->qos.user_data = NULL;
		info->qos.group_data = NULL;
	}
		
#endif
	if (ignored) {
		dwp->dw_qos = NULL;
		dwp->dw_ucast = NULL;
		dwp->dw_mcast = NULL;
		dwp->dw_flags &= ~EF_NOT_IGNORED;
		ret = DDS_RETCODE_ACCESS_DENIED;
		goto done;
	}
	dwp->dw_qos = qos_add (qp);
	if (!dwp->dw_qos) {
		dwp->dw_flags &= ~EF_NOT_IGNORED;
		ret = DDS_RETCODE_OUT_OF_RESOURCES;
		goto done;
	}
	dwp->dw_ucast = info->proxy.ucast;
	info->proxy.ucast = NULL;
	dwp->dw_mcast = info->proxy.mcast;
	info->proxy.mcast = NULL;
	dwp->dw_flags |= EF_NOT_IGNORED;
	if (pp->p_domain->builtin_readers [BT_Publication])
		user_writer_notify (dwp, 1);

	/* Check if not ignored. */
	if ((dwp->dw_flags & EF_NOT_IGNORED) == 0)
		goto done;

	/* Hook into the Topic Writers list. */
	dwp->dw_next = tp->writers;
	tp->writers = &dwp->dw_ep;

	/* Can we match local readers with new publication? */
	if (incompatible)
		dcps_inconsistent_topic (tp);
	else {
		disc_match_readers_new (tp, rp, qp, dwp);
		for (ftp = tp->filters; ftp; ftp = ftp->next) {
			if (lock_take (ftp->topic.lock)) {
				warn_printf ("disc_publication_add: topic lock error");
				continue;
			}
			disc_match_readers_new (&ftp->topic, rp, qp, dwp);
			lock_release (ftp->topic.lock);
		}
	}

    done:
    	lock_release (tp->lock);
	return (ret);
}

/* disc_remote_writer_add -- Add a new discovered Writer as if it was discovered
			     via a Discovery protocol. On entry/exit: no locks. */

DiscoveredWriter_t *disc_remote_writer_add (Participant_t        *pp,
					    DiscoveredWriterData *info)
{
	DiscoveredWriter_t	*dwp;
	UniQos_t		qos;
	int			new_node;

	if (lock_take (pp->p_domain->lock))
		return (NULL);

	dwp = (DiscoveredWriter_t *) endpoint_create (pp, pp, 
					&info->proxy.guid.entity_id, &new_node);
	if (dwp && new_node) {
		qos_disc_writer_set (&qos, &info->qos);
		disc_publication_add (pp, dwp, &qos, NULL, NULL, info);
	}
	lock_release (pp->p_domain->lock);
	return (dwp);
}

#ifdef SIMPLE_DISCOVERY

/* disc_match_readers_end -- Remove all matching readers.
			     On entry/exit: DP, TP locked. */

static void disc_match_readers_end (Topic_t *tp, DiscoveredWriter_t *wp)
{
	Endpoint_t	*ep;

	for (ep = tp->readers; ep; ep = ep->next) {
		if ((ep->entity.flags & EF_LOCAL) == 0)
			continue;

#ifndef RW_TOPIC_LOCK
		if (lock_take (((Reader_t *) ep)->r_lock)); {
			warn_printf ("disc_match_readers_end: reader lock error");
			continue;
		}
#endif
		if (rtps_reader_matches ((Reader_t *) ep, wp))
			disc_end_match ((LocalEndpoint_t *) ep, &wp->dw_ep);
#ifndef RW_TOPIC_LOCK
		lock_release (((Reader_t *) ep)->r_lock);
#endif
	}
}

/* discovered_writer_cleanup -- Cleanup a previously discovered writer.
				On entry/exit: DP locked. */

static void discovered_writer_cleanup (DiscoveredWriter_t *wp,
				       int                ignore,
				       int                *p_last_topic,
				       int                *topic_gone)
{
	Topic_t		*tp;
	FilteredTopic_t	*ftp;
	Endpoint_t	*ep, *prev;
	Participant_t	*pp;

	/* Break all existing matches. */
	tp = wp->dw_topic;
	if (lock_take (tp->lock)) {
		warn_printf ("discovered_writer_cleanup: topic lock error");
		return;
	}
	if (wp->dw_rtps) {
		disc_match_readers_end (tp, wp);
		for (ftp = tp->filters; ftp; ftp = ftp->next) {
			if (lock_take (ftp->topic.lock)) {
				warn_printf ("discovered_writer_cleanup: filter topic lock error");
				continue;
			}
			disc_match_readers_end (&ftp->topic, wp);
			lock_release (ftp->topic.lock);
		}
	}

	/* Remove from topic list. */
	for (prev = NULL, ep = wp->dw_topic->writers;
	     ep && ep != &wp->dw_ep;
	     prev = ep, ep = ep->next)
		;

	if (ep) {
		/* Still in topic list: remove it. */
		if (prev)
			prev->next = ep->next;
		else
			wp->dw_topic->writers = ep->next;
	}

	/* Cleanup all Endpoint data. */
	locator_list_delete_list (&wp->dw_ucast);
	locator_list_delete_list (&wp->dw_mcast);
	qos_disc_writer_free (wp->dw_qos);
	wp->dw_qos = NULL;
#ifdef DDS_TYPECODE
	if (wp->dw_tc && wp->dw_tc != TC_IS_TS) {
		vtc_free (wp->dw_tc);
		wp->dw_tc = NULL;
	}
#endif

	if (ignore) {
		wp->dw_flags &= ~EF_NOT_IGNORED;
		lock_release (tp->lock);
	}
	else {
		/* Free the endpoint. */
		pp = wp->dw_participant;
		endpoint_delete (pp, &wp->dw_ep);
		topic_delete (pp, tp, p_last_topic, topic_gone);
	}
}

/* disc_publication_remove -- Remove a Discovered Writer.
			      On entry/exit: DP locked. */

static void disc_publication_remove (Participant_t      *pp,
				     DiscoveredWriter_t *wp)
{
	Topic_t		*tp = wp->dw_topic;
	Domain_t	*dp = tp->domain;
	InstanceHandle	topic_handle = tp->entity.handle;
	int		last_p_topic, topic_gone;

	if (entity_shutting_down (wp->dw_flags))
		return;

	wp->dw_flags |= EF_SHUTDOWN;
	if (pp->p_domain->builtin_readers [BT_Publication])
		user_notify_delete (dp, BT_Publication, wp->dw_handle);

	discovered_writer_cleanup (wp, 0, &last_p_topic, &topic_gone);

	if (topic_gone && pp->p_domain->builtin_readers [BT_Topic])
		user_notify_delete (dp, BT_Topic, topic_handle);
}

/* disc_match_readers_update -- Update matches with local readers.
				On entry/exit: DP, T locked. */

static void disc_match_readers_update (Topic_t            *tp,
				       DiscoveredWriter_t *dwp,
				       const UniQos_t     *qp,
				       int                incompatible)
{
	Endpoint_t	*ep;
	Reader_t	*rp;
	int 		old_match, new_match;
	DDS_QOS_POLICY_ID qid;

	for (ep = tp->readers; ep; ep = ep->next) {
		if (!local_active (ep->entity.flags))
			continue;

		rp = (Reader_t *) ep;
#ifndef RW_TOPIC_LOCK
		if (lock_take (rp->r_lock)) {
			warn_printf ("disc_match_readers_update: reader lock error");
			continue;
		}
#endif
		old_match = (dwp->dw_rtps &&
			     rtps_reader_matches (rp, dwp));
		new_match = 0;
		if (incompatible)
			/* Different types: cannot match! */;
		else if (!qos_same_partition (rp->r_subscriber->qos.partition,
					      qp->partition))
			dcps_requested_incompatible_qos (rp, DDS_PARTITION_QOS_POLICY_ID);
		else if (!qos_match (qp, NULL,
			     	     qos_ptr (rp->r_qos), &rp->r_subscriber->qos,
				     &qid))
			dcps_requested_incompatible_qos (rp, qid);
		else
			new_match = 1;
		if (old_match && !new_match)
			disc_end_match (&rp->r_lep, &dwp->dw_ep);
		else if (!old_match && new_match)
			disc_new_match (&rp->r_lep, &dwp->dw_ep);
#ifndef RW_TOPIC_LOCK
		lock_release (rp->r_lock);
#endif
	}
}

/* disc_publication_update -- Update a Discovered Writer.
			      On entry/exit: DP locked. */

static int disc_publication_update (Participant_t        *pp,
				    DiscoveredWriter_t   *dwp,
				    DiscoveredWriterData *info)
{
	Topic_t		*tp;
	FilteredTopic_t	*ftp;
	LocatorList_t	tlp;
	UniQos_t	qp;
	int		incompatible = 0;

	/* If the topic name/type name changes or the Writer/
	   Topic QoS becomes incompatible, simply delete and
	   recreate the endpoint. */
	if (strcmp (str_ptr (dwp->dw_topic->name),
		    str_ptr (info->topic_name)) ||
	    strcmp (str_ptr (dwp->dw_topic->type->type_name),
		    str_ptr (info->type_name)) ||
	    update_dw_topic_qos (dwp->dw_topic, info) ||
	    qos_disc_writer_update (&dwp->dw_qos, &info->qos)
#ifdef DDS_TYPECODE
	 || (info->typecode && 
	     !tc_update (&dwp->dw_ep, &dwp->dw_tc,
	     		 &info->typecode, &incompatible))
#endif
	    ) {
		tp = dwp->dw_topic;
		disc_publication_remove (pp, dwp);
		qos_disc_writer_set (&qp, &info->qos);
		return (disc_publication_add (pp, dwp, &qp, tp, NULL, info) ?
					 DDS_RETCODE_OK : DDS_RETCODE_OUT_OF_RESOURCES);
	}
	tp = dwp->dw_topic;
	if (lock_take (tp->lock)) {
		warn_printf ("disc_subscription_update: topic lock error");
		return (DDS_RETCODE_ERROR);
	}

	/* Update locator lists - notify if changed. */
	if (!locator_list_equal (dwp->dw_ucast, info->proxy.ucast)) {
		locator_list_swap (dwp->dw_ucast, info->proxy.ucast, tlp);
		if (dwp->dw_rtps)
			rtps_endpoint_locators_update (&dwp->dw_ep, 0);
	}
	if (!locator_list_equal (dwp->dw_mcast, info->proxy.mcast)) {
		locator_list_swap (dwp->dw_mcast, info->proxy.mcast, tlp);
		if (dwp->dw_rtps)
			rtps_endpoint_locators_update (&dwp->dw_ep, 1);
	}
	if (pp->p_domain->builtin_readers [BT_Publication])
		user_writer_notify (dwp, 0);

	/* Check if not ignored. */
	if ((dwp->dw_flags & EF_NOT_IGNORED) == 0) {
		lock_release (tp->lock);
		return (DDS_RETCODE_OK);
	}

	/* No match yet -- can we match local readers with the publication? */
	if (incompatible)
		dcps_inconsistent_topic (tp);

	disc_match_readers_update (dwp->dw_topic, dwp,
					&dwp->dw_qos->qos, incompatible);
	for (ftp = tp->filters; ftp; ftp = ftp->next)
		disc_match_readers_update (&ftp->topic, dwp, 
					&dwp->dw_qos->qos, incompatible);

	lock_release (tp->lock);
	return (DDS_RETCODE_OK);
}


/* sedp_publication_event -- Receive a publication event.
			     On entry/exit: DP, R(rp) locked. */

static void sedp_publication_event (Reader_t *rp, NotificationType_t t, int cdd)
{
	Domain_t		*dp = rp->r_subscriber->domain;
	Participant_t		*pp;
	ChangeData_t		change;
	DiscoveredWriterData	*info = NULL, tinfo;
	Topic_t			*tp;
	DiscoveredWriter_t	*dwp;
	Reader_t		*mrp;
	UniQos_t		qos;
	InfoType_t		type;
	GUID_t			*guidp;
	int			error;

	if (t != NT_DATA_AVAILABLE)
		return;

	rp->r_status &= ~DDS_DATA_AVAILABLE_STATUS;
	for (;;) {
		if (info) {
			pid_writer_data_cleanup (info);
			xfree (info);
			info = NULL;
		}
		/*dtrc_print0 ("SEDP-Pub: get samples ");*/
		error = disc_get_data (rp, &change);
		if (error) {
			/*dtrc_print0 ("- none\r\n");*/
			break;
		}
		/*dtrc_print1 ("- valid(%u)\r\n", change.kind);*/
		if (change.kind != ALIVE) {
			error = hc_get_key (rp->r_cache, change.h, &tinfo, 0);
			if (error)
				continue;

			guidp = &tinfo.proxy.guid;
			type = EI_DELETE;
			hc_inst_free (rp->r_cache, change.h);
		}
		else {
			info = change.data;
			if (!info->topic_name || !info->type_name) {
				hc_inst_free (rp->r_cache, change.h);
				continue;
			}
			type = EI_NEW;
			guidp = &info->proxy.guid;
		}
			pp = entity_participant (change.writer);
		if (!pp ||				/* Not found. */
		    pp == &dp->participant ||		/* Own sent info. */
		    entity_ignored (pp->p_flags)) {	/* Ignored. */
			if (pp != &dp->participant && !cdd)
				warn_printf ("sedp_publication_rx: invalid change.writer field!\r\n");

			hc_inst_free (rp->r_cache, change.h);
			dtrc_print0 ("SEDP-Pub: unneeded!\r\n");
			continue;	/* Filter out unneeded info. */
		}

		/* Publication from remote participant. */
		if (type == EI_DELETE) {
			dwp = (DiscoveredWriter_t *) endpoint_lookup (pp,
							&guidp->entity_id);
			if (!dwp) {
				dtrc_print0 ("SEDP-Pub: DELETE && doesn't exist!\r\n");
				continue; /* If doesn't exist - no action. */
			}
			if (!dwp->dw_topic) {
				endpoint_delete (pp, &dwp->dw_ep);
				continue; /* Ignored topic -- only endpoint. */
			}
			if (sedp_log)
				log_printf (SEDP_ID, 0, "SEDP: Deleted publication (%s/%s) from peer!\r\n",
						str_ptr (dwp->dw_topic->name),
						str_ptr (dwp->dw_topic->type->type_name));
			disc_publication_remove (pp, dwp);

			hc_inst_free (rp->r_cache, change.h);
			continue;
		}

		/* Do we know this topic? */
		tp = topic_lookup (&dp->participant, str_ptr (info->topic_name));
		if (tp && entity_ignored (tp->entity.flags)) {
			hc_inst_free (rp->r_cache, change.h);
			dtrc_print1 ("SEDP: ignored topic (%s)!\r\n", str_ptr (info->topic_name));
			continue;	/* Ignored topic. */
		}

		/* Do we know this endpoint already? */
		dwp = (DiscoveredWriter_t *) endpoint_lookup (pp, &guidp->entity_id);
		if (dwp) {
			if (entity_ignored (dwp->dw_flags) || cdd) {
				hc_inst_free (rp->r_cache, change.h);
				continue; /* Ignored endpoint. */
			}
			dtrc_print1 ("Already exists (%s)!\r\n", str_ptr (info->topic_name));
			type = EI_UPDATE;
			disc_publication_update (pp, dwp, info);
			if (sedp_log)
				log_printf (SEDP_ID, 0, "SEDP: Updated publication (%s/%s) from peer!\r\n",
						str_ptr (info->topic_name),
						str_ptr (info->type_name));
			hc_inst_free (rp->r_cache, change.h);
		}
		else {
			/* Get QoS parameters. */
			qos_disc_writer_set (&qos, &info->qos);
			mrp = NULL;
			/* Create new endpoint. */
			dwp = (DiscoveredWriter_t *) endpoint_create (pp,
					pp, &guidp->entity_id, NULL);
			if (!dwp) {
				dtrc_print1 ("SEDP: Create endpoint (%s) not possible - exit!\r\n", str_ptr (info->topic_name));
				hc_inst_free (rp->r_cache, change.h);
				qos_disc_writer_restore (&info->qos, &qos);
				continue;  /* Can't create -- just ignore. */
			}
			disc_publication_add (pp, dwp, &qos, tp, mrp, info);
			hc_inst_free (rp->r_cache, change.h);
			if (sedp_log)
				log_printf (SEDP_ID, 0, "SEDP: New publication (%s/%s) from %s!\r\n",
						str_ptr (info->topic_name),
						str_ptr (info->type_name),
						(cdd) ? "CDD" : "peer");

		}
	}
	if (info) {
		pid_writer_data_cleanup (info);
		xfree (info);
	}
}

#endif /* SIMPLE_DISCOVERY */

/* disc_rem_topic_add -- Add a new Topic as discovered by a protocol. */

static int disc_rem_topic_add (Participant_t       *pp,
			       Topic_t             *tp,
			       DiscoveredTopicData *info)
{
	tp->qos = qos_disc_topic_new (&info->qos);
	if (!tp->qos) {
		topic_delete (pp, tp, NULL, NULL);
		return (DDS_RETCODE_OUT_OF_RESOURCES);
	}
	if (pp->p_domain->builtin_readers [BT_Topic])
		user_topic_notify (tp, 1);

	return (DDS_RETCODE_OK);
}

/* disc_remote_topic_add -- Add a new discovered Topic as if it was discovered
			    via a Discovery protocol. */

Topic_t *disc_remote_topic_add (Participant_t *pp,
				DiscoveredTopicData *data)
{
	Topic_t	*tp;
	int	new_node;

	tp = topic_create (pp, NULL, str_ptr (data->name), str_ptr (data->type_name), &new_node);
	if (!tp || !new_node)
		return (NULL); /* Can't create info -- just ignore. */

	if (disc_rem_topic_add (pp, tp, data))
		return (NULL);

	return (tp);
}

#ifdef SIMPLE_DISCOVERY
#ifdef TOPIC_DISCOVERY

/* disc_rem_topic_remove -- Remove a discovered Topic reference. */

static int disc_rem_topic_remove (Participant_t *pp, Topic_t *tp)
{
	if (pp->p_domain->builtin_readers [BT_Topic])
		user_notify_delete (pp->p_domain, BT_Topic, tp->entity.handle);
	
	topic_delete (pp, tp);
	return (DDS_RETCODE_OK);
}

/* disc_rem_topic_update -- Update a Topic. */

static int disc_rem_topic_update (Participant_t       *pp,
				  Topic_t             *tp,
				  DiscoveredTopicData *info)
{
	/* If the Topic QoS becomes incompatible, simply delete and recreate
	   the topic. */
	if (qos_disc_topic_update (&tp->qos, &info->qos)) {
		disc_rem_topic_remove (pp, tp);
		disc_remote_topic_add (pp, info);
	}
	if (pp->p_domain->builtin_readers [BT_Topic])
		user_topic_notify (tp, 0);

	return (DDS_RETCODE_OK);
}

/* sedp_topic_info -- Add/update received topic info to the topic data that a
		      participant has sent. */

static void sedp_topic_info (Participant_t       *pp,
			     Topic_t             *tp,
			     DiscoveredTopicData *info,
			     InfoType_t          type)
{
	switch (type) {
		case EI_NEW:
			disc_rem_topic_add (pp, tp, info);
			break;

		case EI_UPDATE:
			disc_rem_topic_update (pp, tp, info);
			break;

		case EI_DELETE:
			disc_rem_topic_remove (pp, tp);
			return;
	}
}

/* sedp_topic_event -- Receive a topic change from a remote participant. */

static void sedp_topic_event (Reader_t *rp, NotificationType_t t)
{
	Domain_t		*dp = rp->r_subscriber->domain;
	Participant_t		*pp;
	ChangeData_t		change;
	DiscoveredTopicData	*info = NULL, tinfo;
	Topic_t			*tp;
	InfoType_t		type;
	int			error, new_node, valid_data = 0;
	const char		*names [2];

	if (t != NT_DATA_AVAILABLE)
		return;

	rp->r_status &= ~DDS_DATA_AVAILABLE_STATUS;
	for (;;) {
		if (info) {
			pid_topic_data_cleanup (&info);
			xfree (info);
			info = NULL;
		}
		/*dtrc_print0 ("SEDP-Topic: get samples ");*/
		error = disc_get_data (rp, &change);
		if (error) {
			/*dtrc_print0 ("- none\r\n");*/
			break;
		}
		/*dtrc_print1 ("- valid(%u)\r\n", change.kind);*/
		if (change.kind != ALIVE) {
			/* error = hc_get_key (rp->r_cache, change.h, &tinfo, 0);
			if (error)
				continue; */

			type = EI_DELETE;
			hc_inst_free (rp->r_cache, change.h);
		}
		else {
			type = EI_NEW;
			info = change.data;
		}
		pp = entity_participant (change.writer);
		if (!pp ||				/* Not found. */
		    pp == &dp->participant ||		/* Own sent info. */
		    entity_ignored (pp->p_flags)) {	/* Ignored. */
			hc_inst_free (rp->r_cache, change.h);
			continue;	/* Filter out unneeded info. */
		}

		/* Topic from remote participant. */
		if (type == EI_DELETE) {
			/* TBD: Certified not to work correctly. */
			tp = topic_lookup (pp, str_ptr (info.name));
			if (!tp) {
				hc_inst_free (rp->r_cache, change.h);
				continue; /* If doesn't exist - no action. */
			}
			names [0] = str_ptr (tp->name);
			names [1] = str_ptr (tp->type->type_name);
		}
		else {
			tp = topic_create (pp, NULL, names [0], names [1], &new_node);
			if (!tp) {
				hc_inst_free (rp->r_cache, change.h);
				continue; /* Can't create info -- just ignore. */
			}
			if (!new_node) {
				if (entity_ignored (tp->entity.flags)) {
					hc_inst_free (rp->r_cache, change.h);
					continue;
				}
				type = EI_UPDATE;
			}
			names [0] = str_ptr (info->name);
			names [1] = str_ptr (info->type_name);
		}
		if (sedp_log)
			log_printf (SEDP_ID, 0, "SEDP: %s topic (%s/%s) from peer!\r\n",
					    info_type_str [type], names [0], names [1]);
		sedp_topic_info (pp, tp, info, type);
	}
	if (info) {
		pid_topic_data_cleanup (info);
		xfree (info);
	}
}

#endif

/* sedp_publication_add -- Add a publication to the Publication Writer.
			   On entry/exit: DP,P(wp),W(wp) locked. */

static int sedp_publication_add (Domain_t *dp, Writer_t *wp)
{
	GUID_t		guid;
	Writer_t	*pw;
	InstanceHandle	handle;
	HCI		hci;
	DDS_HANDLE	endpoint;
	int		error;

	/* Derive key and publication endpoint. */
	guid.prefix = dp->participant.p_guid_prefix;
	guid.entity_id = wp->w_entity_id;
	pw = (Writer_t *) dp->participant.p_builtin_ep [EPB_PUBLICATION_W];
	if (!pw)
		return (DDS_RETCODE_ALREADY_DELETED);

	/* Register instance. */
	lock_take (pw->w_lock);
	hci = hc_register (pw->w_cache,
			   (unsigned char *) &guid,
			   sizeof (guid),
			   NULL,
			   &handle);
	if (!hci) {
		warn_printf ("sedp_publication_add: failed to register instance handle!");
		lock_release (pw->w_lock);
		return (DDS_RETCODE_OUT_OF_RESOURCES);
	}

	/* Write publication data. */
	if (sedp_log)
		log_printf (SEDP_ID, 0, "SEDP: Send publication (%s/%s)\r\n",
				str_ptr (wp->w_topic->name), 
				str_ptr (wp->w_topic->type->type_name));
	endpoint = wp->w_handle;
	error = rtps_writer_write (pw, &endpoint, sizeof (endpoint), handle,
							hci, NULL, NULL, 0);
	lock_release (pw->w_lock);
	if (error)
		warn_printf ("sedp_publication_add: write failure!");
	return (error);
}

/* sedp_publication_update -- Update a publication to the Publication Writer.
			      On entry/exit: DP,P(wp),W(wp) locked. */

static int sedp_publication_update (Domain_t *dp, Writer_t *wp)
{
	GUID_t		guid;
	Writer_t	*pw;
	HCI		hci;
	InstanceHandle	handle;
	DDS_HANDLE	endpoint;
	FTime_t		time;
	int		error;

	/* Derive key and publication endpoint. */
	guid.prefix = dp->participant.p_guid_prefix;
	guid.entity_id = wp->w_entity_id;
	pw = (Writer_t *) dp->participant.p_builtin_ep [EPB_PUBLICATION_W];
	if (!pw)
		return (DDS_RETCODE_ALREADY_DELETED);

	/* Lookup instance. */
	lock_take (pw->w_lock);
	hci = hc_lookup_key (pw->w_cache, (unsigned char *) &guid,
							sizeof (guid), &handle);
	if (!hci) {
		warn_printf ("sedp_publication_update: failed to lookup instance handle!");
		lock_release (pw->w_lock);
		return (DDS_RETCODE_ALREADY_DELETED);
	}

	/* Write publication data. */
	if (sedp_log)
		log_printf (SEDP_ID, 0, "SEDP: Resend publication (%s/%s)\r\n",
				str_ptr (wp->w_topic->name), 
				str_ptr (wp->w_topic->type->type_name));
	endpoint = wp->w_handle;
	sys_getftime (&time);
	error = rtps_writer_write (pw, &endpoint, sizeof (endpoint), handle,
							hci, &time, NULL, 0);
	lock_release (pw->w_lock);
	if (error)
		warn_printf ("sedp_publication_update: write failure!");

	return (error);
}

/* sedp_publication_remove -- Remove a publication from the Publication Writer.
			      On entry/exit: DP,P(wp),W(wp) locked. */

static int sedp_publication_remove (Domain_t *dp, Writer_t *wp)
{
	GUID_t		guid;
	Writer_t	*pw;
	HCI		hci;
	InstanceHandle	handle;
	FTime_t		time;
	int		error;

	/* Derive key and publication endpoint. */
	guid.prefix = dp->participant.p_guid_prefix;
	guid.entity_id = wp->w_entity_id;
	pw = (Writer_t *) dp->participant.p_builtin_ep [EPB_PUBLICATION_W];
	if (!pw)
		return (DDS_RETCODE_ALREADY_DELETED);

	/* Lookup instance. */
	lock_take (pw->w_lock);
	hci = hc_lookup_key (pw->w_cache, (unsigned char *) &guid,
							sizeof (guid), &handle);
	if (!hci) {
		/* Don't warn: perfectly ok if suspended(). */
		/*warn_printf ("sedp_publication_remove: failed to lookup instance handle!");*/
		lock_release (pw->w_lock);
		return (DDS_RETCODE_ALREADY_DELETED);
	}

	/* Unregister instance. */
	sys_getftime (&time);
	error = rtps_writer_unregister (pw, handle, hci, &time, NULL, 0);
	lock_release (pw->w_lock);
	if (error) {
		warn_printf ("sedp_publication_remove: failed to unregister instance handle!");
		return (error);
	}
	if (sedp_log)
		log_printf (SEDP_ID, 0, "SEDP: Publication (%s/%s) removed.\r\n",
				str_ptr (wp->w_topic->name), 
				str_ptr (wp->w_topic->type->type_name));
	return (error);
}

/* sedp_writer_add -- Add a local writer.
		      On entry/exit: all locks taken (DP,P,T,W). */

static int sedp_writer_add (Domain_t *domain, Writer_t *wp)
{
	Endpoint_t	*ep;
	int		ret;
	DDS_QOS_POLICY_ID qid;

	if (sedp_log)
		log_printf (SEDP_ID, 0, "SEDP: Writer (%s/%s) added.\r\n",
				str_ptr (wp->w_topic->name),
				str_ptr (wp->w_topic->type->type_name));

	/* Add publication. */
	if ((ret = sedp_publication_add (domain, wp)) != DDS_RETCODE_OK)
		return (ret);

	/* Can we match discovered readers with the writer? */
	for (ep = wp->w_topic->readers; ep; ep = ep->next) {
		if (!remote_active (ep->entity.flags))
			continue;

		if (!qos_same_partition (wp->w_publisher->qos.partition,
					 ep->qos->qos.partition))
			dcps_offered_incompatible_qos (wp, DDS_PARTITION_QOS_POLICY_ID);
		else if (!qos_match (qos_ptr (wp->w_qos), &wp->w_publisher->qos,
	    				      qos_ptr (ep->qos), NULL, &qid))
			dcps_offered_incompatible_qos (wp, qid);
		else
			disc_new_match (&wp->w_lep, ep);
	}
	return (DDS_RETCODE_OK);
}

/* sedp_writer_update -- Update a local writer.
		         On entry/exit: all locks taken (DP,P,T,W). */

static int sedp_writer_update (Domain_t *domain, Writer_t *wp)
{
	Endpoint_t	*ep;
	int		ret;
	DDS_QOS_POLICY_ID qid;

	if (sedp_log)
		log_printf (SEDP_ID, 0, "SEDP: Writer (%s/%s) updated.\r\n",
				str_ptr (wp->w_topic->name),
				str_ptr (wp->w_topic->type->type_name));

	/* Update publication. */
	if ((ret = sedp_publication_update (domain, wp)) != DDS_RETCODE_OK)
		return (ret);
       
	/* Can we match discovered readers with the writer? */
	for (ep = wp->w_topic->readers; ep; ep = ep->next) {
		int old_match;
		int new_match;

		if (!remote_active (ep->entity.flags))
			continue;

		old_match = rtps_writer_matches (wp, (DiscoveredReader_t *) ep);
		new_match = 0;
		if (!qos_same_partition (wp->w_publisher->qos.partition,
					 ep->qos->qos.partition))
			dcps_offered_incompatible_qos (wp, DDS_PARTITION_QOS_POLICY_ID);
		else if (!qos_match (qos_ptr (wp->w_qos), &wp->w_publisher->qos,
				     qos_ptr (ep->qos), NULL, &qid))
			dcps_offered_incompatible_qos (wp, qid);
		else
			new_match = 1;
		if (old_match && !new_match) 
			disc_end_match (&wp->w_lep, ep);
		else if (!old_match && new_match)
			disc_new_match (&wp->w_lep, ep);
	}
	return (DDS_RETCODE_OK);
}

/* sedp_writer_remove -- Remove a local writer.
		         On entry/exit: all locks taken (DP,P,T,W). */

static int sedp_writer_remove (Domain_t *dp, Writer_t *wp)
{
	Endpoint_t	*ep;

	if (sedp_log)
		log_printf (SEDP_ID, 0, "SEDP: Writer (%s/%s) removed.\r\n",
				str_ptr (wp->w_topic->name),
				str_ptr (wp->w_topic->type->type_name));

	/* Remove publication. */
	sedp_publication_remove (dp, wp);

	/* Do we have to unmatch discovered readers from the writer? */
	for (ep = wp->w_topic->readers; ep; ep = ep->next)
		if (remote_active (ep->entity.flags) &&
		    rtps_writer_matches (wp, (DiscoveredReader_t *) ep))
			disc_end_match (&wp->w_lep, ep);

	return (DDS_RETCODE_OK);
}

/* sedp_subscription_add -- Add a subscription to the Subscription Writer.
			    On entry/exit: DP,S(rp),R(rp) locked. */

static int sedp_subscription_add (Domain_t *dp, Reader_t *rp)
{
	GUID_t		guid;
	Writer_t	*sw;
	Topic_t		*tp;
	FilteredTopic_t	*ftp;
	HCI		hci;
	InstanceHandle	handle;
	DDS_HANDLE	endpoint;
	int		error;

	/* Derive key and Subscription endpoint. */
	guid.prefix = dp->participant.p_guid_prefix;
	guid.entity_id = rp->r_entity_id;
	sw = (Writer_t *) dp->participant.p_builtin_ep [EPB_SUBSCRIPTION_W];
	if (!sw)
		return (DDS_RETCODE_ALREADY_DELETED);

	/* Register instance. */
	lock_take (sw->w_lock);
	hci = hc_register (sw->w_cache, (unsigned char *) &guid,
						sizeof (guid), NULL, &handle);
	if (!hci) {
		warn_printf ("sedp_subscription_add: failed to register instance handle!");
		return (DDS_RETCODE_OUT_OF_RESOURCES);
	}

	/* Write subscription data. */
	tp = rp->r_topic;
	if ((tp->entity.flags & EF_FILTERED) != 0) {
		ftp = (FilteredTopic_t *) tp;
		tp = ftp->related;
	}
	if (sedp_log)
		log_printf (SEDP_ID, 0, "SEDP: Send subscription (%s/%s)\r\n",
				str_ptr (tp->name), 
				str_ptr (tp->type->type_name));
	endpoint = rp->r_handle;
	error = rtps_writer_write (sw, &endpoint, sizeof (endpoint), handle,
							hci, NULL, NULL, 0);
	lock_release (sw->w_lock);
	if (error)
		warn_printf ("sedp_subscription_add: write failure!");
	return (error);
}

/* sedp_subscription_update -- Update a subscription to the Subscription Writer.
			       On entry/exit: DP,S(rp),R(rp) locked. */

static int sedp_subscription_update (Domain_t *dp, Reader_t *rp)
{
	GUID_t		guid;
	Writer_t	*sw;
	Topic_t		*tp;
	FilteredTopic_t	*ftp;
	HCI		hci;
	InstanceHandle	handle;
	DDS_HANDLE	endpoint;
	FTime_t		time;
	int		error;

	/* Derive key and publication endpoint. */
	guid.prefix = dp->participant.p_guid_prefix;
	guid.entity_id = rp->r_entity_id;
	sw = (Writer_t *) dp->participant.p_builtin_ep [EPB_SUBSCRIPTION_W];
	if (!sw)
		return (DDS_RETCODE_ALREADY_DELETED);

	/* Lookup instance. */
	lock_take (sw->w_lock);
	hci = hc_lookup_key (sw->w_cache, (unsigned char *) &guid,
							sizeof (guid), &handle);
	if (!hci) {
		warn_printf ("sedp_subscription_update: failed to lookup instance handle!");
		lock_release (sw->w_lock);
		return (DDS_RETCODE_ALREADY_DELETED);
	}

	/* Write subscription data. */
	tp = rp->r_topic;
	if ((tp->entity.flags & EF_FILTERED) != 0) {
		ftp = (FilteredTopic_t *) tp;
		tp = ftp->related;
	}
	if (sedp_log)
		log_printf (SEDP_ID, 0, "SEDP: Resend subscription (%s/%s)\r\n",
				str_ptr (tp->name), 
				str_ptr (tp->type->type_name));
	endpoint = rp->r_handle;
	sys_getftime (&time);
	error = rtps_writer_write (sw, &endpoint, sizeof (endpoint), handle,
							hci, &time, NULL, 0);
	lock_release (sw->w_lock);
	if (error)
		warn_printf ("sedp_subscription_update: write failure!");

	return (DDS_RETCODE_OK);
}

/* sedp_subscription_remove -- Remove a subscription from the Subscription Writer.
			       On entry/exit: DP,S(rp),R(rp) locked. */

static int sedp_subscription_remove (Domain_t *dp, Reader_t *rp)
{
	GUID_t		guid;
	Writer_t	*sw;
	Topic_t		*tp;
	FilteredTopic_t	*ftp;
	HCI		hci;
	InstanceHandle	handle;
	FTime_t		time;
	int		error;

	/* Derive key and subscription endpoint. */
	guid.prefix = dp->participant.p_guid_prefix;
	guid.entity_id = rp->r_entity_id;
	sw = (Writer_t *) dp->participant.p_builtin_ep [EPB_SUBSCRIPTION_W];
	if (!sw)
		return (DDS_RETCODE_ALREADY_DELETED);

	/* Lookup instance. */
	lock_take (sw->w_lock);
	hci = hc_lookup_key (sw->w_cache, (unsigned char *) &guid,
							sizeof (guid), &handle);
	if (!hci) {
		warn_printf ("sedp_subscription_remove: failed to lookup instance handle!");
		lock_release (sw->w_lock);
		return (DDS_RETCODE_ALREADY_DELETED);
	}

	/* Unregister instance. */
	sys_getftime (&time);
	error = rtps_writer_unregister (sw, handle, hci, &time, NULL, 0);
	lock_release (sw->w_lock);
	if (error) {
		warn_printf ("sedp_subscription_remove: failed to unregister instance handle!");
		return (error);
	}
	tp = rp->r_topic;
	if ((tp->entity.flags & EF_FILTERED) != 0) {
		ftp = (FilteredTopic_t *) tp;
		tp = ftp->related;
	}
	if (sedp_log)
		log_printf (SEDP_ID, 0, "SEDP: Subscription (%s/%s) removed.\r\n",
				str_ptr (tp->name), 
				str_ptr (tp->type->type_name));
	return (error);
}

/* sedp_reader_add -- Add a local reader.
		      On entry/exit: all locks taken (DP,S,T,R). */

static int sedp_reader_add (Domain_t *dp, Reader_t *rp)
{
	Endpoint_t	*ep;
	Topic_t		*tp;
	FilteredTopic_t	*ftp;
	int		ret;
	DDS_QOS_POLICY_ID qid;

	tp = rp->r_topic;
	if ((tp->entity.flags & EF_FILTERED) != 0) {
		ftp = (FilteredTopic_t *) tp;
		tp = ftp->related;
	}
	if (sedp_log)
		log_printf (SEDP_ID, 0, "SEDP: Reader (%s/%s) added.\r\n",
				str_ptr (tp->name),
				str_ptr (tp->type->type_name));

	/* Add subscription. */
	if ((ret = sedp_subscription_add (dp, rp)) != DDS_RETCODE_OK)
		return (ret);

	/* Can we match/unmatch discovered writers with the reader? */
	for (ep = tp->writers; ep; ep = ep->next) {
		if (!remote_active (ep->entity.flags))
			continue;

		if (!qos_same_partition (rp->r_subscriber->qos.partition,
					 ep->qos->qos.partition))
			dcps_requested_incompatible_qos (rp, DDS_PARTITION_QOS_POLICY_ID);
		else if (!qos_match (qos_ptr (ep->qos), NULL,
				     qos_ptr (rp->r_qos), &rp->r_subscriber->qos,
				     &qid))
			dcps_requested_incompatible_qos (rp, qid);
		else
			disc_new_match (&rp->r_lep, ep);
	}
	return (DDS_RETCODE_OK);
}

/* sedp_reader_update -- Update a local reader.
		         On entry/exit: all locks taken (DP,S,T,R). */

static int sedp_reader_update (Domain_t *dp, Reader_t *rp)
{
	Endpoint_t	*ep;
	Topic_t		*tp;
	FilteredTopic_t	*ftp;
	int		ret;
	DDS_QOS_POLICY_ID qid;

	tp = rp->r_topic;
	if ((tp->entity.flags & EF_FILTERED) != 0) {
		ftp = (FilteredTopic_t *) tp;
		tp = ftp->related;
	}
	if (sedp_log)
		log_printf (SEDP_ID, 0, "SEDP: Reader (%s/%s) updated.\r\n",
				str_ptr (tp->name),
				str_ptr (tp->type->type_name));

	/* Update subscription. */
	if ((ret = sedp_subscription_update (dp, rp)) != DDS_RETCODE_OK)
		return (ret);

	/* Can we match/unmatch discovered writers with the reader? */
	for (ep = tp->writers; ep; ep = ep->next) {
		int old_match;
		int new_match;

		if (!remote_active (ep->entity.flags))
			continue;

		old_match = rtps_reader_matches (rp, (DiscoveredWriter_t *)ep);
		new_match = 0;
		if (!qos_same_partition (rp->r_subscriber->qos.partition,
					 ep->qos->qos.partition))
			dcps_requested_incompatible_qos (rp, DDS_PARTITION_QOS_POLICY_ID);
		else if (!qos_match (qos_ptr (ep->qos), NULL,
				     qos_ptr (rp->r_qos), &rp->r_subscriber->qos,
				     &qid))
			dcps_requested_incompatible_qos (rp, qid);
		else
			new_match = 1;

		if (old_match && !new_match) 
			disc_end_match (&rp->r_lep, ep);
		else if (!old_match && new_match)
			disc_new_match (&rp->r_lep, ep);
	}
	return (DDS_RETCODE_OK);
}

/* sedp_reader_remove -- Remove a local reader.
		         On entry/exit: all locks taken (DP,S,T,R). */

static int sedp_reader_remove (Domain_t *dp, Reader_t *rp)
{
	Endpoint_t	*ep;
	Topic_t		*tp;
	FilteredTopic_t	*ftp;

	tp = rp->r_topic;
	if ((tp->entity.flags & EF_FILTERED) != 0) {
		ftp = (FilteredTopic_t *) tp;
		tp = ftp->related;
	}
	if (sedp_log)
		log_printf (SEDP_ID, 0, "SEDP: Reader (%s/%s) removed.\r\n",
				str_ptr (tp->name),
				str_ptr (tp->type->type_name));

	/* Remove the subscription. */
	sedp_subscription_remove (dp, rp);

	/* Can we match/unmatch discovered writers with the reader? */
	for (ep = tp->writers; ep; ep = ep->next)
		if (remote_active (ep->entity.flags) &&
		    rtps_reader_matches (rp, (DiscoveredWriter_t *) ep))
			disc_end_match (&rp->r_lep, ep);

	return (DDS_RETCODE_OK);
}

#ifdef TOPIC_DISCOVERY

/* sedp_topic_add -- Add a local topic. */

static int sedp_topic_add (Domain_t *dp, Topic_t *tp)
{
	ARG_NOT_USED (dp)
	ARG_NOT_USED (tp)

	if (sedp_log)
		log_printf (SEDP_ID, 0, "SEDP: Topic (%s/%s) added.\r\n",
				str_ptr (tp->name),
				str_ptr (tp->type->type_name));

	/* ... TBC ... */

	return (DDS_ERR_UNIMPL);
}

/* sedp_topic_remove -- Remove a local topic. */

static int sedp_topic_remove (Domain_t *dp, Topic_t *tp)
{
	ARG_NOT_USED (dp)
	ARG_NOT_USED (tp)

	if (sedp_log)
		log_printf (SEDP_ID, 0, "SEDP: Topic (%s/%s) removed.\r\n",
				str_ptr (tp->name),
				str_ptr (tp->type->type_name));

	/* ... TBC ... */

	return (DDS_ERR_UNIMPL);
}

#endif /* TOPIC_DISCOVERY */
#endif /* SIMPLE_DISCOVERY */

/* sedp_endpoint_locator -- Add/remove a locator to/from an endpoint. */

int sedp_endpoint_locator (Domain_t        *domain,
			   LocalEndpoint_t *ep,
			   int             add,
			   int             mcast,
			   const Locator_t *loc)
{
	LocatorList_t	*lp;
	int		ret;

	lp = (mcast) ? &ep->ep.mcast : &ep->ep.ucast;
	if (add)
		locator_list_add (lp, loc->kind, loc->address, loc->port,
				  loc->scope_id, loc->scope, 0, 0);
	else
		locator_list_delete (lp, loc->kind, loc->address, loc->port);

	if ((ep->ep.entity.flags & EF_BUILTIN) != 0)
		return (DDS_RETCODE_OK);

#ifdef SIMPLE_DISCOVERY

	/* Notify peer participants of changed data. */
	if (rtps_used) {
		if (entity_type (&ep->ep.entity) == ET_WRITER)
			ret = sedp_publication_update (domain, (Writer_t *) ep);
		else
			ret = sedp_subscription_update (domain, (Reader_t *) ep);
	}
	else
		ret = DDS_RETCODE_OK;
#else
	ARG_NOT_USED (domain)
	ret = DDS_RETCODE_OK;
#endif
	return (ret);
}

#ifdef SIMPLE_DISCOVERY

/* disc_data_available -- Data available indication from cache. */

static void disc_data_available (uintptr_t user, Cache_t cdp)
{
	Reader_t	*rp = (Reader_t *) user;
	int		notify = (rp->r_status & DDS_DATA_AVAILABLE_STATUS) == 0;

	ARG_NOT_USED (cdp)

	rp->r_status |= DDS_DATA_AVAILABLE_STATUS;
	if (notify)
		dds_notify (NSC_DISC, (Entity_t *) rp, NT_DATA_AVAILABLE);
}

/* sedp_start -- Setup the SEDP protocol builtin endpoints and start the
		 protocol.  On entry/exit: no locks taken, */

static int sedp_start (Domain_t *dp)
{
	Reader_t	*rp;
	int		error;

	/* Create builtin Publications Reader. */
	error = create_builtin_endpoint (dp, EPB_PUBLICATION_R,
					 0, 1,
					 &dp->resend_per,
					 dp->participant.p_meta_ucast,
					 dp->participant.p_meta_mcast,
					 NULL);
	if (error)
		return (error);

	/* Attach to builtin Publications Reader. */
	rp = (Reader_t *) dp->participant.p_builtin_ep [EPB_PUBLICATION_R];
	error = hc_request_notification (rp->r_cache, disc_data_available, (uintptr_t) rp);
	if (error) {
		fatal_printf ("sedp_start: can't register SEDP Publications Reader!");
		return (error);
	}

	/* Create builtin Subscriptions Reader. */
	error = create_builtin_endpoint (dp, EPB_SUBSCRIPTION_R,
					 0, 1,
					 &dp->resend_per,
					 dp->participant.p_meta_ucast,
					 dp->participant.p_meta_mcast,
					 NULL);
	if (error)
		return (error);

	/* Attach to builtin Subscriptions Reader. */
	rp = (Reader_t *) dp->participant.p_builtin_ep [EPB_SUBSCRIPTION_R];
	error = hc_request_notification (rp->r_cache, disc_data_available, (uintptr_t) rp);
	if (error) {
		fatal_printf ("sedp_start: can't register SEDP Subscriptions Reader!");
		return (error);
	}

#ifdef TOPIC_DISCOVERY

	/* Create builtin Topic Reader. */
	error = create_builtin_endpoint (dp, EPB_TOPIC_R,
					 0, 1,
					 &dp->resend_per,
					 dp->participant.p_meta_ucast,
					 dp->participant.p_meta_mcast,
					 NULL);
	if (error)
		return (error);

	/* Attach to builtin Topics Reader. */
	rp = (Reader_t *) dp->participant.p_builtin_ep [EPB_TOPIC_R];
	error = hc_request_notification (rp->r_cache, disc_data_available, (uintptr_t) rp);
	if (error) {
		fatal_printf ("sedp_start: can't register SEDP Topics Reader!");
		return (error);
	}

#endif

	/* Create builtin Publications Writer. */
	error = create_builtin_endpoint (dp, EPB_PUBLICATION_W,
					 1, 1,
					 &dp->resend_per,
					 dp->participant.p_meta_ucast,
					 dp->participant.p_meta_mcast,
					 NULL);
	if (error)
		return (error);

	/* Create builtin Subscriptions Writer. */
	error = create_builtin_endpoint (dp, EPB_SUBSCRIPTION_W,
					 1, 1,
					 &dp->resend_per,
					 dp->participant.p_meta_ucast,
					 dp->participant.p_meta_mcast,
					 NULL);
	if (error)
		return (error);

#ifdef TOPIC_DISCOVERY

	/* Create builtin Topics Writer. */
	error = create_builtin_endpoint (dp, EPB_TOPIC_W,
					 1, 1,
					 &dp->resend_per,
					 dp->participant.p_meta_ucast,
					 dp->participant.p_meta_mcast,
					 NULL);
	if (error)
		return (error);
#endif

	return (DDS_RETCODE_OK);
}

/* sedp_disable -- Stop the SEDP protocol on the participant.
		   On entry/exit: domain and global lock taken */

static void sedp_disable (Domain_t *dp)
{
	disable_builtin_endpoint (dp, EPB_PUBLICATION_R);
	disable_builtin_endpoint (dp, EPB_PUBLICATION_W);
	disable_builtin_endpoint (dp, EPB_SUBSCRIPTION_R);
	disable_builtin_endpoint (dp, EPB_SUBSCRIPTION_W);
#ifdef TOPIC_DISCOVERY
	disable_builtin_endpoint (dp, EPB_TOPIC_R);
	disable_builtin_endpoint (dp, EPB_TOPIC_W);
#endif
}

/* sedp_stop -- Stop the SEDP protocol on the participant.
		On entry/exit: domain and global lock taken */

static void sedp_stop (Domain_t *dp)
{
	delete_builtin_endpoint (dp, EPB_PUBLICATION_R);
	delete_builtin_endpoint (dp, EPB_PUBLICATION_W);
	delete_builtin_endpoint (dp, EPB_SUBSCRIPTION_R);
	delete_builtin_endpoint (dp, EPB_SUBSCRIPTION_W);
#ifdef TOPIC_DISCOVERY
	delete_builtin_endpoint (dp, EPB_TOPIC_R);
	delete_builtin_endpoint (dp, EPB_TOPIC_W);
#endif
}

/* add_peer_builtin -- Add a builtin to a discovered participant. */

static Endpoint_t *add_peer_builtin (Participant_t   *rpp,
				     BUILTIN_INDEX   index,
				     LocalEndpoint_t *lep)
{
	Endpoint_t	*ep;
	Topic_t		*tp;

	ep = endpoint_create (rpp, rpp, &rtps_builtin_eids [index], NULL);
	if (!ep)
		return (NULL);

	ep->topic = tp = lep->ep.topic;
	ep->qos = lep->ep.qos;
	ep->qos->users++;
	if (entity_writer (entity_type (&ep->entity))) {
		ep->next = tp->writers;
		tp->writers = ep;
	}
	else {
		ep->next = tp->readers;
		tp->readers = ep;
	}
	ep->rtps = NULL;
	ep->entity.flags |= EF_BUILTIN | EF_ENABLED;
	rpp->p_builtin_ep [index] = ep;
	return (ep);
}

static void remove_peer_builtin (Participant_t *rpp, BUILTIN_INDEX index)
{
	Topic_t		*tp;
	Endpoint_t	*ep, **ep_list, *prev, *xep;

	ep = rpp->p_builtin_ep [index];
	rpp->p_builtin_ep [index] = NULL;
	tp = ep->topic;

	/* Remove from topic list. */
	if (entity_writer (entity_type (&ep->entity)))
		ep_list = &tp->writers;
	else
		ep_list = &tp->readers;
	for (prev = NULL, xep = *ep_list;
	     xep && xep != ep;
	     prev = xep, xep = xep->next)
		;

	if (xep) {
		if (prev)
			prev->next = ep->next;
		else
			*ep_list = ep->next;
	}

	/* Free the QoS parameters. */
	qos_free (ep->qos);

	/* Free the endpoint. */
	endpoint_delete (rpp, ep);
}

/* connect_builtin -- Connect a local builtin endpoint to a remote.
		      On entry/exit: DP locked. */

static void connect_builtin (Domain_t      *dp,
			     BUILTIN_INDEX l_index,
			     Participant_t *rpp,
			     BUILTIN_INDEX r_index)
{
	LocalEndpoint_t	*ep;
	Endpoint_t	*rep;

	ep = (LocalEndpoint_t *) dp->participant.p_builtin_ep [l_index];
	lock_take (ep->ep.topic->lock);
	if ((rep = add_peer_builtin (rpp, r_index, ep)) != NULL) {
		if (entity_writer (entity_type (&ep->ep.entity))) {
#ifndef RW_TOPIC_LOCK
			lock_take (((Writer_t *) ep)->w_lock);
#endif
			rtps_matched_reader_add ((Writer_t *) ep,
						 (DiscoveredReader_t *) rep);
#ifndef RW_TOPIC_LOCK
			lock_release (((Writer_t *) ep)->w_lock);
#endif
		}
		else {
#ifndef RW_TOPIC_LOCK
			lock_take (((Reader_t *) ep)->r_lock);
#endif
			rtps_matched_writer_add ((Reader_t *) ep,
						 (DiscoveredWriter_t *) rep);
#ifndef RW_TOPIC_LOCK
			lock_release (((Reader_t *) ep)->r_lock);
#endif
		}
	}
	lock_release (ep->ep.topic->lock);
}

/* disconnect_builtin -- Disconnect a local builtin from a remote.
			 On entry/exit: DP locked. */

static void disconnect_builtin (Domain_t      *dp,
				BUILTIN_INDEX l_index,
				Participant_t *rpp,
				BUILTIN_INDEX r_index)
{
	LocalEndpoint_t	*ep;
	Endpoint_t	*rep;

	ep = (LocalEndpoint_t *) dp->participant.p_builtin_ep [l_index];
	if (ep && (rep = rpp->p_builtin_ep [r_index]) != NULL) {
		lock_take (ep->ep.topic->lock);
		if (entity_writer (entity_type (&ep->ep.entity))) {
#ifndef RW_TOPIC_LOCK
			if (lock_take (((Writer_t *) ep)->w_lock)) {
				lock_release (ep->ep.topic->lock);
				return;
			}
#endif

			rtps_matched_reader_remove ((Writer_t *) ep,
						    (DiscoveredReader_t *) rep);
#ifndef RW_TOPIC_LOCK
			lock_release (((Writer_t *) ep)->w_lock);
#endif
		}
		else {
#ifndef RW_TOPIC_LOCK
			if (lock_take (((Reader_t *) ep)->r_lock)) {
				lock_release (ep->ep.topic->lock);
				return;
			}
#endif
			rtps_matched_writer_remove ((Reader_t *) ep,
						    (DiscoveredWriter_t *) rep);
#ifndef RW_TOPIC_LOCK
			lock_release (((Reader_t *) ep)->r_lock);
#endif
		}
		remove_peer_builtin (rpp, r_index);
		lock_release (ep->ep.topic->lock);
	}
}

/* sedp_connect -- Connect this participant to a peer participant.
		   On entry/exit: DP locked. */

static void sedp_connect (Domain_t *dp, Participant_t *rpp)
{
	if ((rpp->p_builtins & (1 << EPB_PUBLICATION_R)) != 0)
		connect_builtin (dp, EPB_PUBLICATION_W, rpp, EPB_PUBLICATION_R);
	if ((rpp->p_builtins & (1 << EPB_PUBLICATION_W)) != 0)
		connect_builtin (dp, EPB_PUBLICATION_R, rpp, EPB_PUBLICATION_W);
	if ((rpp->p_builtins & (1 << EPB_SUBSCRIPTION_R)) != 0)
		connect_builtin (dp, EPB_SUBSCRIPTION_W, rpp, EPB_SUBSCRIPTION_R);
	if ((rpp->p_builtins & (1 << EPB_SUBSCRIPTION_W)) != 0)
		connect_builtin (dp, EPB_SUBSCRIPTION_R, rpp, EPB_SUBSCRIPTION_W);
#ifdef TOPIC_DISCOVERY
	if ((rpp->p_builtins & (1 << EPB_TOPIC_R)) != 0)
		connect_builtin (dp, EPB_TOPIC_W, rpp, EPB_TOPIC_R);
	if ((rpp->p_builtins & (1 << EPB_TOPIC_W)) != 0)
		connect_builtin (dp, EPB_TOPIC_R, rpp, EPB_TOPIC_W);
#endif
}

/* sedp_disconnect -- Disconnect this participant from a peer participant.
		      On entry/exit: DP locked. */

static void sedp_disconnect (Domain_t *dp, Participant_t *rpp)
{
	if ((rpp->p_builtins & (1 << EPB_PUBLICATION_R)) != 0)
		disconnect_builtin (dp, EPB_PUBLICATION_W, rpp, EPB_PUBLICATION_R);
	if ((rpp->p_builtins & (1 << EPB_PUBLICATION_W)) != 0)
		disconnect_builtin (dp, EPB_PUBLICATION_R, rpp, EPB_PUBLICATION_W);
	if ((rpp->p_builtins & (1 << EPB_SUBSCRIPTION_R)) != 0)
		disconnect_builtin (dp, EPB_SUBSCRIPTION_W, rpp, EPB_SUBSCRIPTION_R);
	if ((rpp->p_builtins & (1 << EPB_SUBSCRIPTION_W)) != 0)
		disconnect_builtin (dp, EPB_SUBSCRIPTION_R, rpp, EPB_SUBSCRIPTION_W);
#ifdef TOPIC_DISCOVERY
	if ((rpp->p_builtins & (1 << EPB_TOPIC_R)) != 0)
		disconnect_builtin (dp, EPB_TOPIC_W, rpp, EPB_TOPIC_R);
	if ((rpp->p_builtins & (1 << EPB_TOPIC_W)) != 0)
		disconnect_builtin (dp, EPB_TOPIC_R, rpp, EPB_TOPIC_W);
#endif
}

/* sedp_unmatch_peer_endpoint -- If the endpoint matches one of ours, end the
				 association since the peer participant has
				 gone. */

static int sedp_unmatch_peer_endpoint (Skiplist_t *list, void *node, void *arg)
{
	Endpoint_t	*ep, **epp = (Endpoint_t **) node;

	ARG_NOT_USED (list)
	ARG_NOT_USED (arg)

	ep = *epp;
	/*log_printf (DISC_ID, 0, "sedp_unmatch_peer_endpoint (%s)!\r\n", str_ptr (ep->topic->name));*/
	if (entity_type (&ep->entity) == ET_WRITER) {
		disc_publication_remove (ep->u.participant, (DiscoveredWriter_t *) ep);
	}
	else {
		disc_subscription_remove (ep->u.participant, (DiscoveredReader_t *) ep);
	}
	return (1);
}

# if 0
/* sedp_topic_free -- Free a previously created topic. */

static int sedp_topic_free (Skiplist_t *list, void *node, void *arg)
{
	Topic_t		*tp, **tpp = (Topic_t **) node;
	Participant_t	*pp = (Participant_t *) arg;

	ARG_NOT_USED (list)

	tp = *tpp;
	lock_take (tp->lock);
	/*log_printf (DISC_ID, 0, "sedp_topic_free (%s)!\r\n", str_ptr (tp->name));*/
	if (pp->p_domain->builtin_readers [BT_Topic])
		user_topic_notify_delete (tp, tp->entity.handle);
	topic_delete (pp, tp, NULL, NULL);
	return (1);
}
# endif

#ifdef DISC_MSG_DUMP

static void msg_dump_data (DDS_OctetSeq *sp)
{
	unsigned		i;
	const unsigned char	*cp;

	if (!DDS_SEQ_LENGTH (*sp)) {
		log_printf (SPDP_ID, 0, "<empty>\r\n");
		return;
	}
	for (i = 0, cp = sp->_buffer; i < DDS_SEQ_LENGTH (*sp); i++) {
		if ((i & 0xf) == 0)
			log_printf (SPDP_ID, 0, "\t%04u: ", i);
		log_printf (SPDP_ID, 0, "%02x ", *cp++);
	}
	log_printf (SPDP_ID, 0, "\r\n");
}

/* msg_data_info -- A Participant Message was received from a remote participant. */

static void msg_data_info (Participant_t          *pp,
			   ParticipantMessageData *dp,
			   char                   dir,
			   InfoType_t             type)
{
	uint32_t	*lp = (uint32_t *) &dp->participantGuidPrefix;

	ARG_NOT_USED (pp)

	log_printf (SPDP_ID, 0, "MSG-%c: %08x:%08x:%08x - ", dir, ntohl (lp [0]), ntohl (lp [1]), ntohl (lp [2]));
	if ((dp->kind [0] & 0x80) != 0)
		log_printf (SPDP_ID, 0, "Vendor: %02x.%02x.%02x.%02x, ",
			dp->kind [0], dp->kind [1], dp->kind [2], dp->kind [3]);
	else if (dp->kind [0] == 0 &&
		 dp->kind [1] == 0 &&
		 dp->kind [2] == 0 &&
		 (dp->kind [3] == 1 || dp->kind [3] == 2))
		if (dp->kind [3] == 1)
			log_printf (SPDP_ID, 0, "Auto Liveliness Update, ");
		else
			log_printf (SPDP_ID, 0, "Manual Liveliness Update, ");
	else
		log_printf (SPDP_ID, 0, "Reserved: %02x.%02x.%02x.%02x, ",
			dp->kind [0], dp->kind [1], dp->kind [2], dp->kind [3]);
	switch (type) {
		case EI_NEW:
			log_printf (SPDP_ID, 0, "{New} ");
			msg_dump_data (&dp->data);
			break;
		case EI_UPDATE:
			msg_dump_data (&dp->data);
			break;
		case EI_DELETE:
			log_printf (SPDP_ID, 0, "{deleted}!\r\n");
			break;
	}
}

#endif

/* msg_data_event -- Receive a Participant Message from a remote participant. */

static void msg_data_event (Reader_t *rp, NotificationType_t t)
{
	Domain_t		*dp = rp->r_subscriber->domain;
	Participant_t		*pp;
	unsigned		nchanges;
	ChangeData_t		change;
	ParticipantMessageData	*info = NULL;
#ifdef DISC_MSG_DUMP
	InfoType_t		type;
#endif
	int			error;

	if (t != NT_DATA_AVAILABLE)
		return;

	rp->r_status &= ~DDS_DATA_AVAILABLE_STATUS;
	do {
		nchanges = 1;
		/*dtrc_print0 ("PMSG: get samples");*/
		error = disc_get_data (rp, &change);
		if (error) {
			/*dtrc_print0 ("- none\r\n");*/
			break;
		}
		/*dtrc_print1 ("- valid(%u)\r\n", change.kind);*/
		if (change.kind != ALIVE) {
			/*error = hc_get_key (cdp, change.h, &info, 0);
			if (error)
				continue;*/

#ifdef DISC_MSG_DUMP
			type = EI_DELETE;
#endif
			hc_inst_free (rp->r_cache, change.h);
		}
		else {
#ifdef DISC_MSG_DUMP
			if (change.is_new)
				type = EI_NEW;
			else
				type = EI_UPDATE;
#endif
			info = change.data;
		}
		pp = entity_participant (change.writer);
		if (!pp ||				/* Not found. */
		    pp == &dp->participant ||		/* Own sent info. */
		    entity_ignored (pp->p_flags)) {	/* Ignored. */
			hc_inst_free (rp->r_cache, change.h);
			continue;	/* Filter out unneeded info. */
		}
		/* If it's a liveliness indication, then propagate it. */ 
		if (info) {
#ifdef DISC_MSG_DUMP

			/* Message from remote participant. */
			if (spdp_log)
				msg_data_info (pp, info, 'R', type);
#endif
			if (info->kind [0] == 0 &&
			    info->kind [1] == 0 &&
			    info->kind [2] == 0 &&
			    (info->kind [3] == 1 || info->kind [3] == 2)) {
				pp = participant_lookup (dp, &info->participantGuidPrefix);
				if (pp)
					liveliness_participant_event (pp, info->kind [3] != 1);
			}
			xfree (info);
		}
		hc_inst_free (rp->r_cache, change.h);
	}
	while (nchanges);
}

/* msg_start -- Start the Participant message reader/writer.
		On entry/exit: no locks used. */

static int msg_start (Domain_t *dp)
{
	Reader_t	*rp;
	int		error;

	/* Create builtin Participant Message Reader. */
	error = create_builtin_endpoint (dp, EPB_PARTICIPANT_MSG_R,
					 0, 1,
					 &dp->resend_per,
					 dp->participant.p_meta_ucast,
					 dp->participant.p_meta_mcast,
					 NULL);
	if (error)
		return (error);

	/* Attach to builtin Participant Message Reader. */
	rp = (Reader_t *) dp->participant.p_builtin_ep [EPB_PARTICIPANT_MSG_R];
	error = hc_request_notification (rp->r_cache, disc_data_available, (uintptr_t) rp);
	if (error) {
		fatal_printf ("msg_start: can't register Message Reader!");
		return (error);
	}

	/* Create builtin Participant Message Writer. */
	error = create_builtin_endpoint (dp, EPB_PARTICIPANT_MSG_W,
					 1, 1,
					 &dp->resend_per,
					 dp->participant.p_meta_ucast,
					 dp->participant.p_meta_mcast,
					 NULL);
	if (error)
		return (error);

	return (DDS_RETCODE_OK);
}

static int msg_send_liveliness (Domain_t *dp, unsigned kind)
{
	ParticipantMessageData	msgd;
	Writer_t		*wp;
	DDS_Time_t		time;
	int			error;

	if (!domain_ptr (dp, 1, (DDS_ReturnCode_t *) &error))
		return (error);

	msgd.participantGuidPrefix = dp->participant.p_guid_prefix;
	wp = (Writer_t *) dp->participant.p_builtin_ep [EPB_PARTICIPANT_MSG_W];
	lock_release (dp->lock);

	msgd.kind [0] = msgd.kind [1] = msgd.kind [2] = 0;
	if (kind == 0)	/* Automatic mode. */
		msgd.kind [3] = 1;
	else if (kind == 1) /* Manual mode. */
		msgd.kind [3] = 2;
	else
		msgd.kind [3] = 0;
	msgd.data._length = msgd.data._maximum = 0;
	msgd.data._esize = 1;
	msgd.data._own = 1;
	msgd.data._buffer = NULL;
	sys_gettime ((Time_t *) &time);
	error = DDS_DataWriter_write_w_timestamp (wp, &msgd, 0, &time);

#ifdef DISC_MSG_DUMP

	/* Message to remote participant. */
	if (spdp_log)
		msg_data_info (&dp->participant, &msgd, 'T', 0);
#endif
	return (error);
}

/* msg_disable -- Disable the Participant message reader/writer.
		  On entry/exit: domain and global lock taken. */

static void msg_disable (Domain_t *dp)
{
	disable_builtin_endpoint (dp, EPB_PARTICIPANT_MSG_R);
	disable_builtin_endpoint (dp, EPB_PARTICIPANT_MSG_W);
}

/* msg_stop -- Stop the Participant message reader/writer.
	       On entry/exit: domain and global lock taken. */

static void msg_stop (Domain_t *dp)
{
	delete_builtin_endpoint (dp, EPB_PARTICIPANT_MSG_R);
	delete_builtin_endpoint (dp, EPB_PARTICIPANT_MSG_W);
}

/* msg_connect -- Connect the messaging endpoints to the peer participant. */

static void msg_connect (Domain_t *dp, Participant_t *rpp)
{
	if ((rpp->p_builtins & (1 << EPB_PARTICIPANT_MSG_R)) != 0)
		connect_builtin (dp, EPB_PARTICIPANT_MSG_W, rpp, EPB_PARTICIPANT_MSG_R);
	if ((rpp->p_builtins & (1 << EPB_PARTICIPANT_MSG_W)) != 0)
		connect_builtin (dp, EPB_PARTICIPANT_MSG_R, rpp, EPB_PARTICIPANT_MSG_W);
}

/* msg_disconnect -- Disconnect the messaging endpoints from the peer. */

static void msg_disconnect (Domain_t *dp, Participant_t *rpp)
{
	if ((rpp->p_builtins & (1 << EPB_PARTICIPANT_MSG_R)) != 0)
		disconnect_builtin (dp, EPB_PARTICIPANT_MSG_W, rpp, EPB_PARTICIPANT_MSG_R);
	if ((rpp->p_builtins & (1 << EPB_PARTICIPANT_MSG_W)) != 0)
		disconnect_builtin (dp, EPB_PARTICIPANT_MSG_R, rpp, EPB_PARTICIPANT_MSG_W);
}

#endif /* SIMPLE_DISCOVERY */

/* user_participant_notify -- Notify a discovered Participant to discovery
			      listeners.
			      Locked on entry/exit: DP. */

static void user_participant_notify (Participant_t *pp, int new1)
{
	Domain_t	*dp;
	KeyHash_t	hash;
	Cache_t		cache;
	Change_t	*cp;
	InstanceHandle	h;
	Reader_t	*rp;
	int		error;

	dp = pp->p_domain;
	rp = dp->builtin_readers [BT_Participant];
	if (!rp)
		return;

	cp = hc_change_new ();
	if (!cp)
		goto notif_err;

	memcpy (hash.hash, &pp->p_guid_prefix, sizeof (GuidPrefix_t));
	memset (&hash.hash [sizeof (GuidPrefix_t)], 0, sizeof (EntityId_t));
	lock_take (rp->r_lock);
	cache = rp->r_cache;
	cp->c_writer = cp->c_handle = pp->p_handle;
	if (new1 &&
	    hc_lookup_hash (cache, &hash, hash.hash,
	    		     sizeof (DDS_BuiltinTopicKey_t),
			     &h, 0, 0, NULL) &&
	    h != cp->c_handle)
		hc_inst_free (cache, h);
	hc_lookup_hash (cache, &hash, hash.hash, sizeof (DDS_BuiltinTopicKey_t),
					&cp->c_handle,
					(new1) ? LH_ADD_SET_H : 0, 0, NULL);
	cp->c_kind = ALIVE;
	cp->c_data = cp->c_xdata;
	cp->c_length = sizeof (cp->c_writer);
	memcpy (cp->c_xdata, &cp->c_writer, sizeof (cp->c_writer));
	error = hc_add_inst (cache, cp, NULL, 0);
	if (!error)
		pp->p_flags |= EF_CACHED;
	lock_release (rp->r_lock);
	if (!error)
		return;

    notif_err:
	warn_printf ("Discovered participant notification failed!");
}

#ifdef SIMPLE_DISCOVERY

/* spdp_end_participant -- End participant due to either a time-out or by an
			   explicit unregister by the peer via a NOT_ALIVE_*
			   change of the keyed instance, or from an ignore().
			   Locked on entry/exit: DP. */

static void spdp_end_participant (Participant_t *pp, int ignore)
{
	Domain_t	*dp;
	Reader_t	*rp;

	if ((ignore && entity_ignored (pp->p_flags)) ||
	    entity_shutting_down (pp->p_flags))
		return;

	pp->p_flags |= EF_SHUTDOWN;
	if (spdp_log) {
		log_printf (SPDP_ID, 0, "SPDP: Participant");
		if (pp->p_entity_name)
			log_printf (SPDP_ID, 0, " (%s)", str_ptr (pp->p_entity_name));
		log_printf (SPDP_ID, 0, " removed!\r\n");
	}
	dp = pp->p_domain;
	lock_required (dp->lock);

	/* Remove relay as default route. */
	if (pp->p_forward)
		rtps_relay_remove (pp);

	/* Disconnect the SPDP/SEDP endpoints from the peer participant. */
	sedp_disconnect (dp, pp);

	/* Connect the Participant Message endpoints. */
	msg_disconnect (dp, pp);


	/* Release the various ReaderLocator instances that were created for
	   the Stateless Writers and the Stateful Reader/Writer proxies. */
	sl_walk (&pp->p_endpoints, sedp_unmatch_peer_endpoint, pp);

	/* Release the discovered Topics. */
/*	sl_walk (&pp->p_topics, sedp_topic_free, pp);   <== should not be needed! */

#ifdef DDS_FORWARD
	rfwd_participant_dispose (pp);
#endif

	/* Notify the user of the participant's removal. */
	if (dp->builtin_readers [BT_Participant])
		user_notify_delete (pp->p_domain, BT_Participant, pp->p_handle);

	/* Release the various locator lists. */
	locator_list_delete_list (&pp->p_def_ucast);
	locator_list_delete_list (&pp->p_def_mcast);
	locator_list_delete_list (&pp->p_meta_ucast);
	locator_list_delete_list (&pp->p_meta_mcast);
	locator_list_delete_list (&pp->p_src_locators);
#ifdef DDS_SECURITY
	locator_list_delete_list (&pp->p_sec_locs);
#endif

	/* Release Participant user data. */
	if (pp->p_user_data)
		str_unref (pp->p_user_data);

	/* Release the entity name. */
	if (pp->p_entity_name) {
		str_unref (pp->p_entity_name);
		pp->p_entity_name = NULL;
	}

	/* Release the timer. */
	tmr_stop (&pp->p_timer);

	/* If ignore, we're done. */
	if (ignore) {

		/* Set ignored status. */
		pp->p_flags &= ~(EF_NOT_IGNORED | EF_SHUTDOWN);
		return;
	}

	/* Cleanup registered but unfilled cache instances created by RTPS. */
	rp = (Reader_t *) dp->participant.p_builtin_ep [EPB_PUBLICATION_R];
	if (rp)
		hc_reclaim_keyed (rp->r_cache, &pp->p_guid_prefix);
	rp = (Reader_t *) dp->participant.p_builtin_ep [EPB_SUBSCRIPTION_R];
	if (rp)
		hc_reclaim_keyed (rp->r_cache, &pp->p_guid_prefix);

	/* Remove the peer participant information. */
	participant_delete (pp->p_domain, pp);
}

/* spdp_participant_timeout -- A participant doesn't send information anymore. */

static void spdp_participant_timeout (uintptr_t user)
{
	Ticks_t		ticks;
	Participant_t	*pp = (Participant_t *) user;

	if (pp->p_alive) {
		pp->p_alive = 0;
		ticks = duration2ticks (&pp->p_lease_duration) + 2;

		tmr_start_lock (&pp->p_timer,
				ticks,
				(uintptr_t) pp,
				spdp_participant_timeout,
				&pp->p_domain->lock);
		return;
	}

	/* Cleanup endpoint connectivity. */
	spdp_end_participant (pp, 0);
}

#endif

/* disc_remote_participant_add -- Add a peer domain participamt on a domain.
				  Locked on entry/exit: DP. */

Participant_t *disc_remote_participant_add (Domain_t                      *domain,
					    SPDPdiscoveredParticipantData *info,
					    int                           ignored)
{
	Participant_t	*pp;

	pp = participant_create (domain, &info->proxy.guid_prefix, NULL);
	if (!pp)
		return (NULL);

	version_set (pp->p_proto_version, info->proxy.proto_version);
	vendor_id_set (pp->p_vendor_id, info->proxy.vendor_id);
	pp->p_exp_il_qos = info->proxy.exp_il_qos;
	pp->p_builtins = info->proxy.builtins;
	pp->p_no_mcast = info->proxy.no_mcast;
	pp->p_sw_version = info->proxy.sw_version;
	if (ignored) {
		pp->p_def_ucast = NULL;
		pp->p_def_mcast = NULL;
		pp->p_meta_ucast = NULL;
		pp->p_meta_mcast = NULL;
#ifdef DDS_SECURITY
		pp->p_permissions = 0;
		pp->p_sec_caps = 0;
		pp->p_sec_locs = NULL;
#endif
		pp->p_forward = 0;
	}
	else {
		if (pp->p_vendor_id [0] == VENDORID_H_TECHNICOLOR &&
		    pp->p_vendor_id [1] == VENDORID_L_TECHNICOLOR) {
#ifdef DDS_SECURITY
			pp->p_permissions = info->proxy.permissions;
			pp->p_sec_caps = info->proxy.sec_caps;
			pp->p_sec_locs = info->proxy.sec_locs;
			info->proxy.sec_locs = NULL;
#endif
			pp->p_forward = info->proxy.forward;
		}
		else {
			pp->p_no_mcast = 0;
			pp->p_sw_version = 0;
#ifdef DDS_SECURITY
			pp->p_permissions = 0;
			pp->p_sec_caps = 0;
			pp->p_sec_locs = NULL;
#endif
			pp->p_forward = 0;
		}
		pp->p_def_ucast = info->proxy.def_ucast;
		info->proxy.def_ucast = NULL;
		pp->p_def_mcast = info->proxy.def_mcast;
		info->proxy.def_mcast = NULL;
		pp->p_meta_ucast = info->proxy.meta_ucast;
		info->proxy.meta_ucast = NULL;
		pp->p_meta_mcast = info->proxy.meta_mcast;
		info->proxy.meta_mcast = NULL;
#ifdef DUMP_LOCATORS
		dbg_printf ("DUC:");
		locator_list_dump (pp->p_def_ucast);
		dbg_printf (";DMC:");
		locator_list_dump (pp->p_def_mcast);
		dbg_printf (";MUC:");
		locator_list_dump (pp->p_meta_ucast);
		dbg_printf (";MMC:");
		locator_list_dump (pp->p_meta_mcast);
		dbg_printf (";\r\n");
#endif
	}
	pp->p_man_liveliness = info->proxy.manual_liveliness;
	pp->p_user_data = info->user_data;
	info->user_data = NULL;
	pp->p_entity_name = info->entity_name;
	info->entity_name = NULL;
	pp->p_lease_duration = info->lease_duration;
	sl_init (&pp->p_endpoints, sizeof (Endpoint_t *));
	sl_init (&pp->p_topics, sizeof (Topic_t *));

	pp->p_alive = 0;

	if (ignored)
		return (pp);

	pp->p_flags |= EF_NOT_IGNORED;

#ifdef DDS_NO_MCAST
	pp->p_no_mcast = 1;
#else
	if (locator_list_no_mcast (domain->domain_id, pp->p_def_ucast))
		pp->p_no_mcast = 1;
#endif
	
#ifdef DDS_FORWARD
	rfwd_participant_new (pp, 0);
#endif

	/* Notify user of participant existence. */
	if (pp->p_domain->builtin_readers [BT_Participant]) {
		user_participant_notify (pp, 1);

		/* As a result of the notification, user may have done an
		   ignore_participant().  If so, we don't continue. */
		if ((pp->p_flags & EF_NOT_IGNORED) == 0)
			return (NULL);
	}
	return (pp);
}

#ifdef SIMPLE_DISCOVERY

/* spdp_new_participant -- Add a new peer participant as discovered by the
			   participant discovery algorithm.
			   On entry/exit: DP locked. */

static void spdp_new_participant (Domain_t                      *dp,
				  SPDPdiscoveredParticipantData *info,
				  Locator_t                     *src)
{
	Participant_t	*pp;
	Writer_t	*wp;
	unsigned	ticks;
	int		ignored;
#ifdef DDS_SECURITY
	unsigned	perm = 0;
#endif
#ifdef DDS_SECURITY
	DDS_AuthAction_t action;
	size_t		 clen;
#endif
	LocatorRef_t	*rp;
	LocatorNode_t	*np;

	log_printf (SPDP_ID, 0, "SPDP: New participant");
	if (spdp_log) {
		log_printf (SPDP_ID, 0, "SPDP: New participant");
		if (info->entity_name)
			log_printf (SPDP_ID, 0, " (%s)", str_ptr (info->entity_name));
		log_printf (SPDP_ID, 0, " detected!\r\n");
	}
#ifdef DDS_SECURITY
	if (dp->security && info->identity && info->permissions) {
		clen = 0;
		action = validate_peer_identity ((unsigned char *) str_ptr (info->identity),
						 str_len (info->identity),
						 NULL,
						 &clen);

		ignored = (action != DDS_AA_ACCEPTED);
		if (!ignored) {
			perm = validate_peer_permissions (dp->domain_id,
							  (unsigned char *) str_ptr (info->permissions),
							  str_len (info->permissions));
			if (check_peer_participant (perm, info->user_data) !=
								DDS_RETCODE_OK)	{
				log_printf (SPDP_ID, 0, "SPDP: ignore participant!\r\n");
				ignored = 1;
			}
		}
	}
	else if (dp->security)
		ignored = 1;
	else
#endif
		ignored = 0;

	pp = disc_remote_participant_add (dp, info, ignored);
	if (!pp)
		return;

	if (!ignored) {
		log_printf (SPDP_ID, 0, "SPDP: Connecting builtin endpoints.\r\n");
#ifdef DDS_SECURITY
		pp->p_permissions = perm;
#endif
		/* Remember if locally reachable. */
		pp->p_local = 0;
		if (src) {
			if ((src->kind & LOCATOR_KINDS_UDP) != 0)
				foreach_locator (pp->p_def_ucast, rp, np)
					if (locator_addr_equal (&np->locator, src)) {
						pp->p_local = sys_ticks_last;
						break;
					}

			/* Remember who sent this. */
			locator_list_add (&pp->p_src_locators,
					  src->kind,
					  src->address,
					  src->port,
					  0, 0, 0, 0);
		}

		/* If this is a relay node, use it for routing. */
		if (pp->p_forward) {
			/*if (!rtps_local_node (pp, src))
				pp->p_forward = 0;
			else*/
				rtps_relay_add (pp);
		}

		/* Resend participant info. */
		wp = (Writer_t *) dp->participant.p_builtin_ep [EPB_PARTICIPANT_W];
		rtps_stateless_resend (wp);

		/* Connect the Participant Message endpoints. */
		msg_connect (dp, pp);

		/* Connect SEDP endpoints to new participant. */
		sedp_connect (dp, pp);

	}

	/* Start participant timer. */
	ticks = duration2ticks (&pp->p_lease_duration) + 2;
	tmr_init (&pp->p_timer, "DiscParticipant");
	tmr_start_lock (&pp->p_timer,
		        ticks,
		        (uintptr_t) pp,
		        spdp_participant_timeout,
		        &dp->lock);
}

/* update_locators -- Update a locator list if the new one is different. */

static int update_locators (LocatorList_t *dlp, LocatorList_t *slp)
{
	if (locator_list_equal (*dlp, *slp))
		return (0);

	locator_list_delete_list (dlp);
	*dlp = *slp;
	*slp = NULL;
	return (1);
}

/* endpoint_locators_update -- Update the locators of an endpoint due to changed
			       locators. */

static int endpoint_locators_update (Skiplist_t *list, void *node, void *arg)
{
	Endpoint_t	*ep, **epp = (Endpoint_t **) node;
	unsigned	ofs, *n = (unsigned *) arg;

	ARG_NOT_USED (list)

	ep = *epp;
	if ((ep->entity.flags & EF_BUILTIN) != 0)
		ofs = 2;
	else
		ofs = 0;
	if (!n [ofs] && !n [ofs + 1])
		return (1);

	if (n [ofs])
		rtps_endpoint_locators_update (ep, 0);
	if (n [ofs + 1])
		rtps_endpoint_locators_update (ep, 1);

	return (1);
}

/* endpoint_locators_local -- Update the locators of an endpoint due to locality
			      changes. */

static int endpoint_locators_local (Skiplist_t *list, void *node, void *arg)
{
	Endpoint_t	*ep, **epp = (Endpoint_t **) node;
	Ticks_t		local, *p_local = (Ticks_t *) arg;

	ARG_NOT_USED (list)

	ep = *epp;
	local = *p_local;
	rtps_endpoint_locality_update (ep, local);

	return (1);
}

#define	MAX_LOCAL_TICKS		(TICKS_PER_SEC * 25)

/* spdp_update_participant -- Update an existing peer participant.
			      On entry/exit: DP locked. */

static void spdp_update_participant (Domain_t                      *dp,
				     Participant_t                 *pp,
				     SPDPdiscoveredParticipantData *info,
				     Locator_t                     *src)
{
	unsigned	n [4];
#ifdef DDS_SECURITY
	unsigned	ns;
#endif
	LocatorRef_t	*rp;
	LocatorNode_t	*np;
	int		proxy_local_update;
	int		relay_update;
	int		locators_update;
	unsigned	is_local;
	char		buf [30];

	ARG_NOT_USED (dp)

	pp->p_no_mcast = info->proxy.no_mcast;

	/* Update unicast participant locators if necessary. */
	n [0] = update_locators (&pp->p_def_ucast, &info->proxy.def_ucast);
	n [1] = update_locators (&pp->p_def_mcast, &info->proxy.def_mcast);
	n [2] = update_locators (&pp->p_meta_ucast, &info->proxy.meta_ucast);
	n [3] = update_locators (&pp->p_meta_mcast, &info->proxy.meta_mcast);
#ifdef DDS_SECURITY
	if (dp->security) {
		ns = update_locators (&pp->p_sec_locs, &info->proxy.sec_locs);
		n [0] += ns;
		n [2] += ns;
	}
#endif
	proxy_local_update = 0;
	locators_update = ((n [0] + n [1] + n [2] + n [3]) != 0);

	/* Remember who sent this. */
	is_local = 0;
	if (src) {
		if ((src->kind & LOCATOR_KINDS_UDP) != 0) {
			foreach_locator (pp->p_def_ucast, rp, np)
				if (locator_addr_equal (&np->locator, src)) {
					is_local = 1;
					break;
				}
		}
		locator_list_add (&pp->p_src_locators,
				  src->kind,
				  src->address,
				  src->port,
				  src->scope_id,
				  src->scope,
				  src->flags,
				  src->sproto);
		if (is_local) {
			if (!pp->p_local)
				proxy_local_update = 1;
			pp->p_local = sys_ticks_last;
		}
	}
	if (!is_local &&
	    pp->p_local && 
	    sys_ticksdiff (pp->p_local, sys_ticks_last) > MAX_LOCAL_TICKS) {
		proxy_local_update = 1;
		pp->p_local = 0;
		/*log_printf (SPDP_ID, 0, "SPDP: Participant locality time-out (%s).\r\n",
						guid_prefix_str (&pp->p_guid_prefix, buf));*/
	}
	relay_update = (pp->p_forward != info->proxy.forward ||
			(pp->p_forward && locators_update));

	/* If any locators have changed, update all proxies connected to
	   endpoints for this participant! */
	if (locators_update && !relay_update) {
		log_printf (SPDP_ID, 0, "SPDP: Participant locators update (%s).\r\n",
						guid_prefix_str (&pp->p_guid_prefix, buf));
		sl_walk (&pp->p_endpoints, endpoint_locators_update, n);
		locator_list_delete_list (&pp->p_src_locators);
	}

	/* Else if locality changes, update all related proxies as well. */
	else if (proxy_local_update && !relay_update) {
		log_printf (SPDP_ID, 0, "SPDP: Participant locality update (%s is %s now).\r\n",
			guid_prefix_str (&pp->p_guid_prefix, buf), (pp->p_local) ? "local" : "remote");
		sl_walk (&pp->p_endpoints, endpoint_locators_local, &pp->p_local);
	}

	/* Else if relay info is updated, update all proxies. */
	else if (relay_update) {
		log_printf (SPDP_ID, 0, "SPDP: Relay update (%s).\r\n",
						guid_prefix_str (&pp->p_guid_prefix, buf));
		/*if (info->proxy.forward && !rtps_local_node (pp, src))
			info->proxy.forward = 0;*/
		if (pp->p_forward && !info->proxy.forward) {
			pp->p_forward = info->proxy.forward;
			rtps_relay_remove (pp);
		}
		else if (!pp->p_forward && info->proxy.forward) {
			pp->p_forward = info->proxy.forward;
			rtps_relay_add (pp);
		}
		else 
			rtps_relay_update (pp);
	}

#ifdef DDS_FORWARD
	rfwd_participant_new (pp, 1);
#endif

	/* Update liveliness if changed. */
	if (pp->p_man_liveliness != info->proxy.manual_liveliness) {
		pp->p_man_liveliness = info->proxy.manual_liveliness;
#ifdef LOG_LIVELINESS
		log_printf (SPDP_ID, 0, "SPDP: Participant asserted (%s).\r\n",
						guid_prefix_str (&pp->p_prefix, buf));
#endif
		liveliness_participant_asserted (pp);
	}

	/* Check if user-data has changed. */
	if ((!pp->p_user_data && info->user_data) ||
	    (pp->p_user_data && !info->user_data) ||
	    (pp->p_user_data && info->user_data &&
	     (str_len (pp->p_user_data) != str_len (info->user_data) ||
	      !memcmp (str_ptr (pp->p_user_data),
		       str_ptr (info->user_data),
		       str_len (pp->p_user_data))))) {
		if (pp->p_user_data)
			str_unref (pp->p_user_data);
		pp->p_user_data = info->user_data;
		info->user_data = NULL;
		if (pp->p_domain->builtin_readers [BT_Participant])
			user_participant_notify (pp, 0);
	}
}

/* spdp_event -- New participant data available to be read callback function.
		 Locked on entry/exit: DP + R(rp). */

static void spdp_event (Reader_t *rp, NotificationType_t t)
{
	Domain_t			*dp = rp->r_subscriber->domain;
	Participant_t			*pp;
	ChangeData_t			change;
	SPDPdiscoveredParticipantData	*info = NULL, tinfo;
	GuidPrefix_t	  		*guidprefixp;
	RemPrefix_t			*prp;
	InfoType_t                      type;
	int				error;
	Ticks_t				ticks;

	lock_required (dp->lock);
	lock_required (rp->r_lock);

	if (t != NT_DATA_AVAILABLE)
		return;

	rp->r_status &= ~DDS_DATA_AVAILABLE_STATUS;
	for (;;) {
		change.data = &info;
		if (info) {
			pid_participant_data_cleanup (info);
			xfree (info);
			info = NULL;
		}
		/*dtrc_print0 ("SPDP: get samples");*/
		error = disc_get_data (rp, &change);
		if (error) {
			/*dtrc_print0 ("- none\r\n");*/
			break;
		}
		/*dtrc_print1 ("- valid(%u)\r\n", change.kind);*/
		if (change.kind == ALIVE) {
			info = change.data;
			type = EI_NEW;
			guidprefixp = &info->proxy.guid_prefix;
		}
		else {
			error = hc_get_key (rp->r_cache, change.h, &tinfo, 0);
			if (error)
				continue;

			guidprefixp = &tinfo.proxy.guid_prefix;
			type = EI_DELETE;
		}
		prp = prefix_lookup (dp, guidprefixp);
		if (memcmp (&dp->participant.p_guid_prefix,
			    guidprefixp,
			    sizeof (GuidPrefix_t)) != 0/* && prp*/) {
			pp = participant_lookup (dp, guidprefixp);
			if (type == EI_DELETE) {
				if (pp)
					spdp_end_participant (pp, 0);
			}
			else if (!pp)
				spdp_new_participant (dp, info,
						(prp) ? &prp->locator : NULL);
			else {
				if (!entity_ignored (pp->p_flags))
					spdp_update_participant (dp, pp, info, 
						 (prp) ? &prp->locator : NULL);
				pp->p_alive = 0;
				ticks = duration2ticks (&pp->p_lease_duration) + 2;
				tmr_start_lock (&pp->p_timer,
					        ticks,
					        (uintptr_t) pp,
					        spdp_participant_timeout,
						&dp->lock);
			}
		}
		if (prp)
			prefix_forget (prp);
		hc_inst_free (rp->r_cache, change.h);
	}
	if (info) {
		pid_participant_data_cleanup (info);
		xfree (info);
	}
}

/* spdp_send_participant_liveliness -- Resend Asserted Participant liveliness. */

int spdp_send_participant_liveliness (Domain_t *dp)
{
	Writer_t	*wp;
	HCI		hci;
	InstanceHandle	handle;
	FTime_t		time;
	DDS_HANDLE	endpoint;
	int		error;

	wp = (Writer_t *) dp->participant.p_builtin_ep [EPB_PARTICIPANT_W];
#ifdef LOG_LIVELINESS
	log_printf (SPDP_ID, 0, "SPDP: Assert Participant.\r\n");
#endif
	sys_getftime (&time);
	lock_take (wp->w_lock);
	hci = hc_register (wp->w_cache, dp->participant.p_guid_prefix.prefix,
					sizeof (GuidPrefix_t), &time, &handle);

	endpoint = dp->participant.p_handle;

	/* Update local domain participant assertion count. */
	dp->participant.p_man_liveliness++;
	pl_cache_reset ();

	/* Resend participant data. */
	error = rtps_writer_write (wp,
				   &endpoint, sizeof (endpoint),
				   handle, hci, &time, NULL, 0);
	lock_release (wp->w_lock);
	if (error) {
		fatal_printf ("spdp_start: can't send updated SPDP Participant Data!");
		return (error);
	}
	return (DDS_RETCODE_OK);
}

#ifdef DDS_AUTO_LIVELINESS

/* spdp_auto_liveliness_timeout -- Time-out for resending automatic liveliness. */

static void spdp_auto_liveliness_timeout (uintptr_t user)
{
	Domain_t	*dp = (Domain_t *) user;

#if defined (RTPS_USED) && defined (SIMPLE_DISCOVERY)
	if (!dp->participant.p_liveliness)
		msg_send_liveliness (dp, 0);
#endif
	tmr_start_lock (&dp->auto_liveliness,
			dp->resend_per.secs * 2 * TICKS_PER_SEC,
			(uintptr_t) dp,
			spdp_auto_liveliness_timeout,
			&dp->lock);
}

#endif

/* spdp_start -- Start the SPDP protocol.  On entry: Domain lock taken, */

static int spdp_start (Domain_t *dp)
{
	int			error;
	InstanceHandle		handle;
	DDS_HANDLE		endpoint;
	FTime_t			time;
	Reader_t		*rp;
	Writer_t		*wp;
	HCI			hci;
	LocatorList_t		muc_locs;
	LocatorList_t		mmc_locs;

	if (spdp_log)
		log_printf (SPDP_ID, 0, "SPDP: starting protocol for domain #%u.\r\n", dp->domain_id);

	/* Create SPDP builtin endpoints: Participant Announcer and Participant
	   Detector. */
	muc_locs = dp->participant.p_meta_ucast;
	mmc_locs = dp->participant.p_meta_mcast;
	error = create_builtin_endpoint (dp, EPB_PARTICIPANT_W,
					 0, 0,
					 &dp->resend_per,
					 muc_locs, mmc_locs,
					 dp->dst_locs);
	if (error)
		return (error);

	error = create_builtin_endpoint (dp, EPB_PARTICIPANT_R,
					 0, 0,
					 &dp->resend_per,
					 muc_locs, mmc_locs,
					 NULL);
	if (error)
		return (error);

	rp = (Reader_t *) dp->participant.p_builtin_ep [EPB_PARTICIPANT_R];
	error = hc_request_notification (rp->r_cache, disc_data_available, (uintptr_t) rp);
	if (error) {
		fatal_printf ("SPDP: can't register Participant detector!");
		return (error);
	}

	/* Create the builtin endpoints for SEDP. */
	sedp_start (dp);

	/* Create the builtin Participant Message endpoints. */
	msg_start (dp);

	sys_getftime (&time);
	log_printf (SPDP_ID, 0, "SPDP: registering Participant key.\r\n");
	wp = (Writer_t *) dp->participant.p_builtin_ep [EPB_PARTICIPANT_W];
	lock_take (wp->w_lock);
	hci = hc_register (wp->w_cache, dp->participant.p_guid_prefix.prefix,
					sizeof (GuidPrefix_t), &time, &handle);

	log_printf (SPDP_ID, 0, "SPDP: Send Participant data.\r\n");

#ifdef DDS_AUTO_LIVELINESS
	dp->auto_liveliness.name = "DP_ALIVE";
	tmr_start_lock (&dp->auto_liveliness,
			dp->resend_per.secs * 2 * TICKS_PER_SEC,
			(uintptr_t) dp,
			spdp_auto_liveliness_timeout,
			&dp->lock);
#endif
	endpoint = dp->participant.p_handle;
	error = rtps_writer_write (wp,
				   &endpoint, sizeof (endpoint),
				   handle, hci, &time, NULL, 0);
	lock_release (wp->w_lock);
	if (error) {
		fatal_printf ("spdp_start: can't send SPDP Participant Data!");
		return (error);
	}
	return (DDS_RETCODE_OK);
}

/* spdp_update -- Domain participant data was updated. */

static int spdp_update (Domain_t *dp)
{
	Writer_t	*wp;
	HCI		hci;
	InstanceHandle	handle;
	FTime_t		time;
	DDS_HANDLE	endpoint;
	int		error;

	wp = (Writer_t *) dp->participant.p_builtin_ep [EPB_PARTICIPANT_W];
	sys_getftime (&time);
	lock_take (wp->w_lock);

	hci = hc_lookup_key (wp->w_cache, (unsigned char *) &dp->participant.p_guid_prefix,
			sizeof (dp->participant.p_guid_prefix), &handle);
	if (!hci) {
		warn_printf ("spdp_update: failed to lookup instance handle!");
		lock_release (wp->w_lock);
		error = DDS_RETCODE_ALREADY_DELETED;
	}
	else {
		/* Update local domain participant. */
		pl_cache_reset ();

		/* Resend participant data. */
		endpoint = dp->participant.p_handle;
		error = rtps_writer_write (wp,
					   &endpoint, sizeof (endpoint),
					   handle, hci, &time, NULL, 0);
	}
	lock_release (wp->w_lock);
	if (error) {
		fatal_printf ("spdp_update: can't send updated SPDP Participant Data!");
		return (error);
	}
	return (DDS_RETCODE_OK);
}

/* spdp_stop -- Stop the SPDP discovery protocol. Called from disc_stop with
 		domain_lock and global_lock taken. */

static void spdp_stop (Domain_t *dp)
{
	HCI             hci;
	InstanceHandle  handle;
	Writer_t	*pw;
	FTime_t		time;
	int		error;
	Participant_t	**ppp;

	log_printf (SPDP_ID, 0, "SPDP: ending protocol for domain #%u.\r\n", dp->domain_id);

#ifdef DDS_AUTO_LIVELINESS

	/* Stop automatic liveliness. */
	tmr_stop (&dp->auto_liveliness);
#endif

	/* Disable the Participant Message endpoints. */
	msg_disable (dp);

	/* Disable SEDP builtin endpoints. */
	sedp_disable (dp);

	/* Remove all peer participants in domain. */
	while ((ppp = sl_head (&dp->peers)) != NULL)
		spdp_end_participant (*ppp, 0);

	/* Inform peer that we're quitting. */
	pw = (Writer_t *) dp->participant.p_builtin_ep [EPB_PARTICIPANT_W];
	lock_take (pw->w_lock);
	hci = hc_lookup_key (pw->w_cache, (unsigned char *) &dp->participant.p_guid_prefix,
			sizeof (dp->participant.p_guid_prefix), &handle);
	if (!hci) {
		warn_printf ("spdp_stop: failed to lookup participant instance handle!");
		lock_release (pw->w_lock);
	}
	else {
		/* Unregister participant instance. */
		sys_getftime (&time);
		error = rtps_writer_unregister (pw, handle, hci, &time, NULL, 0);
		lock_release (pw->w_lock);
		if (error) 
			warn_printf ("spdp_stop: failed to unregister instance handle!");
		else
			thread_yield ();
	}

	/* Disable participant discovery. */
	disable_builtin_endpoint (dp, EPB_PARTICIPANT_R);
        disable_builtin_endpoint (dp, EPB_PARTICIPANT_W);

	/* Delete the Participant Message endpoints. */
	msg_stop (dp);

	/* Delete SEDP builtin endpoints. */
	sedp_stop (dp);

	/* Delete SPDP builtin endpoints. */
	delete_builtin_endpoint (dp, EPB_PARTICIPANT_W);
	delete_builtin_endpoint (dp, EPB_PARTICIPANT_R);
}

static void disc_notify_listener (Entity_t *ep, NotificationType_t t)
{
	Reader_t	*rp = (Reader_t *) ep;
	Domain_t	*dp = rp->r_subscriber->domain;

	if (lock_take (dp->lock)) {
		warn_printf ("disc_notify_listener: domain lock error");
		return;
	}
	if (lock_take (rp->r_lock)) {
		warn_printf ("disc_notify_listener: lock error");
		lock_release (dp->lock);
		return;
	}
	if (rp->r_entity_id.id [1] == 0)	/* SEDP */
		switch (rp->r_entity_id.id [2]) {
#ifdef TOPIC_DISCOVERY
			case 2:			/* SEDP::TOPIC_READER */
				sedp_topic_event (rp, t);
				break;
#endif
			case 3:			/* SEDP::PUBLICATIONS_READER */
				sedp_publication_event (rp, t, 0);
				break;
			case 4:			/* SEDP::SUBSCRIPTIONS_READER */
				sedp_subscription_event (rp, t, 0);
				break;
			default:
				break;
		}
	else if (rp->r_entity_id.id [1] == 1)	/* SPDP */
		spdp_event (rp, t);
	else if (rp->r_entity_id.id [1] == 2)	/* PMSG */
		msg_data_event (rp, t);
	lock_release (rp->r_lock);
	lock_release (dp->lock);
}
#endif /* SIMPLE_DISCOVERY */
#endif /* RTPS_USED */

/* disc_start -- Start the discovery protocols. */

int disc_start (Domain_t *domain)		/* Domain. */
{
#ifdef SIMPLE_DISCOVERY
	Publisher_t	*up;
	Subscriber_t	*sp;
	TopicType_t	*tp;
#endif
	int		error;

	if (lock_take (domain->lock)) {
		warn_printf ("disc_start: domain lock error");
		return (DDS_RETCODE_ERROR);
	}
	disc_log = log_logged (DISC_ID, 0);

#ifdef SIMPLE_DISCOVERY

	if (!rtps_used) {
		lock_release (domain->lock);
		return (DDS_RETCODE_OK);
	}

	/* Create builtin Publisher if was not yet created. */
	if (!domain->builtin_publisher) {
		up = publisher_create (domain, 1);
		if (!up) {
			lock_release (domain->lock);
			return (DDS_RETCODE_OUT_OF_RESOURCES);
		}

		qos_publisher_new (&up->qos, &qos_def_publisher_qos);
		up->def_writer_qos = qos_def_writer_qos;
	}

	/* Create builtin Subscriber if it was not yet created. */
	if (!domain->builtin_subscriber) {
		sp = subscriber_create (domain, 1);
		if (!sp) {
			lock_release (domain->lock);
			return (DDS_RETCODE_OUT_OF_RESOURCES);
		}
		qos_subscriber_new (&sp->qos, &qos_def_subscriber_qos);
		sp->def_reader_qos = qos_def_reader_qos;
	}
	lock_release (domain->lock);

	error = DDS_DomainParticipant_register_type ((DDS_DomainParticipant) domain,
						     dds_participant_msg_ts,
						     "ParticipantMessageData");
	if (error) {
		warn_printf ("disc_start: can't register ParticipantMessageData type!");
		return (error);
	}
	if (lock_take (domain->lock)) {
		warn_printf ("disc_start: domain lock error (2)");
		return (DDS_RETCODE_ERROR);
	}
	tp = type_lookup (domain, "ParticipantMessageData");
	if (tp)
		tp->flags |= EF_BUILTIN;
	lock_release (domain->lock);

	/* Currently we only have the SPDP and SEDP protocols implemented. */
	spdp_log = log_logged (SPDP_ID, 0);
	sedp_log = log_logged (SEDP_ID, 0);
	error = spdp_start (domain);

	/* Other discovery protocols could be started here. */

#else
	ARG_NOT_USED (domain)

	error = DDS_RETCODE_OK;
#endif
	return (error);
}

/* disc_stop -- Stop the discovery protocols. Called from
		rtps_participant_delete with domain_lock and global_lock taken. */

void disc_stop (Domain_t *domain)
{
#ifdef SIMPLE_DISCOVERY
	if (!rtps_used)
		return;

	spdp_stop (domain);

	DDS_DomainParticipant_unregister_type ((DDS_DomainParticipant) domain,
						     dds_participant_msg_ts,
						     "ParticipantMessageData");

	/* Delete builtin publisher/subscriber. */
	if (domain->builtin_publisher) {
		publisher_delete (domain->builtin_publisher);
		domain->builtin_publisher = NULL;
	}
	if (domain->builtin_subscriber) {
		subscriber_delete (domain->builtin_subscriber);
		domain->builtin_subscriber = NULL;
	}

	/* Other discovery protocols should be notified here. */
#else
	ARG_NOT_USED (domain)
#endif
}

/* disc_send_participant_liveliness -- Resend Asserted Participant liveliness. */

int disc_send_participant_liveliness (Domain_t *dp)
{
	int	error = DDS_RETCODE_OK;

#ifdef SIMPLE_DISCOVERY
	if (rtps_used)
		error = spdp_send_participant_liveliness (dp);
#else
	ARG_NOT_USED (dp)
#endif
	return (error);
}

/* disc_participant_update -- Specifies that a domain participant was updated.*/

int disc_participant_update (Domain_t *domain)
{
#ifdef SIMPLE_DISCOVERY
	int	error;

	if (rtps_used) {
		error = spdp_update (domain);
		if (error)
			return (error);
	}

	/* Other discovery protocols should be notified here. */
#else
	ARG_NOT_USED (domain)
#endif

	return (DDS_RETCODE_OK);
}

/* disc_writer_add -- A new writer endpoint was added.
		      On entry/exit: all locks taken (DP,P,T,W). */

int disc_writer_add (Domain_t *domain, Writer_t *wp)
{
#ifdef SIMPLE_DISCOVERY
	int	error;

	if (rtps_used) {
		error = sedp_writer_add (domain, wp);
		if (error)
			return (error);
	}

	/* Other discovery protocols should be notified here. */
#else
	ARG_NOT_USED (domain)
	ARG_NOT_USED (wp)
#endif

	return (DDS_RETCODE_OK);
}

/* disc_writer_update -- A new writer endpoint was updated.
		         On entry/exit: all locks taken (DP,P,T,W). */

int disc_writer_update (Domain_t *domain, Writer_t *wp)
{
#ifdef SIMPLE_DISCOVERY
	int	error;

	if (rtps_used) {
		error = sedp_writer_update (domain, wp);
		if (error)
			return (error);
	}

	/* Other discovery protocols should be notified here. */
#else
	ARG_NOT_USED (domain)
	ARG_NOT_USED (wp)
#endif

	return (DDS_RETCODE_OK);
}

/* disc_writer_remove -- A writer endpoint was removed.
		         On entry/exit: all locks taken (DP,P,T,W). */

int disc_writer_remove (Domain_t *domain, Writer_t *wp)
{
#ifdef SIMPLE_DISCOVERY
	if (rtps_used)
		sedp_writer_remove (domain, wp);

	/* Other discovery protocols should be notified here. */
#else
	ARG_NOT_USED (domain)
	ARG_NOT_USED (wp)
#endif

	return (DDS_RETCODE_OK);
}

/* disc_reader_add -- A new reader endpoint was added. */

int disc_reader_add (Domain_t *domain, Reader_t *rp)
{
#ifdef SIMPLE_DISCOVERY
	int	error;

	if (rtps_used) {
		error = sedp_reader_add (domain, rp);
		if (error)
			return (error);
	}

	/* Other discovery protocols should be notified here. */
#else
	ARG_NOT_USED (domain)
	ARG_NOT_USED (rp)
#endif

	return (DDS_RETCODE_OK);
}

/* disc_reader_update -- A new reader endpoint was updateed.
		         On entry/exit: all locks taken (DP,S,T,R) */

int disc_reader_update (Domain_t *domain, Reader_t *rp)
{
#ifdef SIMPLE_DISCOVERY
	int	error;

	if (rtps_used) {
		error = sedp_reader_update (domain, rp);
		if (error)
			return (error);
	}

	/* Other discovery protocols should be notified here. */
#else
	ARG_NOT_USED (domain)
	ARG_NOT_USED (rp)
#endif

	return (DDS_RETCODE_OK);
}

/* disc_reader_remove -- A reader endpoint was removed.
		         On entry/exit: all locks taken (DP,S,T,R) */

int disc_reader_remove (Domain_t *domain, Reader_t *rp)
{
#ifdef SIMPLE_DISCOVERY
	if (rtps_used)
		sedp_reader_remove (domain, rp);

	/* Other discovery protocols should be notified here. */
#else
	ARG_NOT_USED (domain)
	ARG_NOT_USED (rp)
#endif

	return (DDS_RETCODE_OK);
}

/* disc_topic_add -- A topic was added. */

int disc_topic_add (Domain_t *domain, Topic_t *tp)
{
	ARG_NOT_USED (domain)

#if defined (SIMPLE_DISCOVERY) && defined (TOPIC_DISCOVERY)

	if (rtps_used)
		sedp_topic_add (domain, tp);

#else
	ARG_NOT_USED (tp)

#endif	/* Other discovery protocols should be notified here. */

	return (DDS_RETCODE_OK);
}

/* disc_topic_remove -- A topic was removed. */

int disc_topic_remove (Domain_t *domain, Topic_t *tp)
{
	ARG_NOT_USED (domain)
#if defined (SIMPLE_DISCOVERY) && defined (TOPIC_DISCOVERY)

	if (rtps_used)
		sedp_topic_remove (domain, tp);
#else
	ARG_NOT_USED (tp)

#endif	/* Other discovery protocols should be notified here. */

	return (DDS_RETCODE_OK);
}

/* disc_endpoint_locator -- Add/remove a locator to/from an endpoint. */

int disc_endpoint_locator (Domain_t        *domain,
			   LocalEndpoint_t *ep,
			   int             add,
			   int             mcast,
			   const Locator_t *loc)
{
#ifdef SIMPLE_DISCOVERY
	if (rtps_used)
		sedp_endpoint_locator (domain, ep, add, mcast, loc);

	/* Other discovery protocols should be notified here. */
#else
	ARG_NOT_USED (domain)
	ARG_NOT_USED (ep)
	ARG_NOT_USED (add)
	ARG_NOT_USED (mcast)
	ARG_NOT_USED (loc)
#endif

	return (DDS_RETCODE_OK);
}

/* disc_ignore_participant -- Ignore a discovered participant. */

int disc_ignore_participant (Participant_t *pp)
{
#ifdef SIMPLE_DISCOVERY
	if (rtps_used)
		spdp_end_participant (pp, 1);
#else
	ARG_NOT_USED (pp)
#endif

	return (DDS_RETCODE_OK);
}

/* disc_ignore_topic -- Ignore a discovered topic. */

int disc_ignore_topic (Topic_t *tp)
{
#ifdef SIMPLE_DISCOVERY
	Endpoint_t	*ep, *next_ep;

	if (!rtps_used || entity_ignored (tp->entity.flags))
		return (DDS_RETCODE_OK);

	if ((tp->entity.flags & EF_REMOTE) == 0)
		return (DDS_RETCODE_OK);

	for (ep = tp->readers; ep; ep = next_ep) {
		next_ep = ep->next;
		if ((ep->entity.flags & EF_REMOTE) != 0)
			discovered_reader_cleanup ((DiscoveredReader_t *) ep, 0, NULL, NULL);
	}
	for (ep = tp->writers; ep; ep = next_ep) {
		next_ep = ep->next;
		if ((ep->entity.flags & EF_REMOTE) != 0)
			discovered_writer_cleanup ((DiscoveredWriter_t *) ep, 0, NULL, NULL);
	}
	tp->entity.flags &= ~EF_NOT_IGNORED;
#else
	ARG_NOT_USED (tp)
#endif

	return (DDS_RETCODE_OK);
}

/* disc_ignore_writer -- Ignore a discovered writer. */

int disc_ignore_writer (DiscoveredWriter_t *wp)
{
#ifdef SIMPLE_DISCOVERY
	if (!rtps_used || entity_ignored (wp->dw_flags))
		return (DDS_RETCODE_OK);

	discovered_writer_cleanup (wp, 1, NULL, NULL);
#else
	ARG_NOT_USED (wp)
#endif
	return (DDS_RETCODE_OK);
}

/* disc_ignore_reader -- Ignore a discovered reader. */

int disc_ignore_reader (DiscoveredReader_t *rp)
{
#ifdef SIMPLE_DISCOVERY
	if (!rtps_used || entity_ignored (rp->dr_flags))
		return (DDS_RETCODE_OK);

	discovered_reader_cleanup (rp, 1, NULL, NULL);
#else
	ARG_NOT_USED (rp)
#endif
	return (DDS_RETCODE_OK);
}

#ifdef RTPS_USED

struct pop_bi_st {
	Domain_t	*domain;
	Builtin_Type_t	type;
};

static int populate_endpoint (Skiplist_t *list, void *node, void *arg)
{
	Endpoint_t	*ep, **epp = (Endpoint_t **) node;
	struct pop_bi_st *bip = (struct pop_bi_st *) arg;

	ARG_NOT_USED (list)

	ep = *epp;
	if ((ep->entity.flags & EF_BUILTIN) != 0)
		return (1);

	if (lock_take (ep->topic->lock)) {
		warn_printf ("populate_endpoint: topic lock error");
		return (0);
	}
	if (entity_writer (entity_type (&ep->entity)) && bip->type == BT_Publication)
		user_writer_notify ((DiscoveredWriter_t *) ep, 1);
	else if (entity_reader (entity_type (&ep->entity)) && bip->type == BT_Subscription)
		user_reader_notify ((DiscoveredReader_t *) ep, 1);
	lock_release (ep->topic->lock);

	return (1);
}

static int populate_participant (Skiplist_t *list, void *node, void *arg)
{
	Participant_t	*pp, **ppp = (Participant_t **) node;
	struct pop_bi_st *bip = (struct pop_bi_st *) arg;

	ARG_NOT_USED (list)

	pp = *ppp;
	if (bip->type == BT_Participant)
		user_participant_notify (pp, 1);
	else if (sl_length (&pp->p_endpoints))
		sl_walk (&pp->p_endpoints, populate_endpoint, arg);

	return (1);
}

static int populate_topic (Skiplist_t *list, void *node, void *arg)
{
	Topic_t	*tp, **tpp = (Topic_t **) node;

	ARG_NOT_USED (list)
	ARG_NOT_USED (arg)

	tp = *tpp;
	if (!tp->nlrefs && tp->nrrefs)
		user_topic_notify (tp, 1);

	return (1);
}

/* disc_populate_builtin -- Add already discovered data to a builtin reader. */

int disc_populate_builtin (Domain_t *dp, Builtin_Type_t type)
{
	struct pop_bi_st	data;

	if (lock_take (dp->lock)) {
		warn_printf ("disc_populate_builtin: domain lock error");
		return (DDS_RETCODE_ERROR);
	}
	if (type == BT_Topic) {
		if (sl_length (&dp->participant.p_topics))
			sl_walk (&dp->participant.p_topics, populate_topic, 0);
	}
	else if (sl_length (&dp->peers)) {
		data.domain = dp;
		data.type = type;
		sl_walk (&dp->peers, populate_participant, &data);
	}
	lock_release (dp->lock);
	return (DDS_RETCODE_OK);
}

#endif /* RTPS_USED */

/* disc_send_liveliness_msg == Send either a manual or automatic liveliness
			       message. */

int disc_send_liveliness_msg (Domain_t *dp, unsigned kind)
{
	int	error;

#if defined (RTPS_USED) && defined (SIMPLE_DISCOVERY)
	error = msg_send_liveliness (dp, kind);
#else
	ARG_NOT_USED (dp)
	ARG_NOT_USED (kind)

	error = DDS_RETCODE_OK;
#endif
	return (error);
}

/* disc_suspend_participant -- Suspend activated for a participant. */

static int disc_suspend_participant (Skiplist_t *list, void *node, void *arg)
{
	Participant_t	*pp, **ppp = (Participant_t **) node;

	ARG_NOT_USED (list)
	ARG_NOT_USED (arg)

	pp = *ppp;
	pp->p_alive = 0;
	return (1);
}

/* disc_suspend -- Suspend discovery. */

void disc_suspend (void)
{
	Domain_t	*dp;
	unsigned	i = 0;

	for (i = 0; ; ) {
		dp = domain_next (&i, NULL);
		if (!dp)
			return;

		lock_take (dp->lock);
		sl_walk (&dp->peers, disc_suspend_participant, NULL);
		lock_release (dp->lock);
	}
}


/* disc_resume -- Resume discovery. */

void disc_resume (void)
{
}

/* disc_init -- Initialize the Discovery module. */

int disc_init (void)
{
#ifdef SIMPLE_DISCOVERY
	if (!rtps_used)
		return (DDS_RETCODE_OK);

	dds_participant_msg_ts = DDS_DynamicType_register (dds_participant_msg_tsm);
	if (!dds_participant_msg_ts) {
		fatal_printf ("Can't register ParticipantMessageData type!");
		return (DDS_RETCODE_BAD_PARAMETER);
	}
	dds_attach_notifier (NSC_DISC, disc_notify_listener);
#endif
	return (DDS_RETCODE_OK);
}

/* disc_final -- Finalize the Discovery module. */

void disc_final (void)
{
#ifdef SIMPLE_DISCOVERY
	DDS_DynamicType_free (dds_participant_msg_ts);
#endif
}

#ifdef DDS_DEBUG

/* disc_dump -- Debug: dump the discovered participants and endpoints. */

void disc_dump (int all)
{
	unsigned flags;

	flags = DDF_LOCATORS_L | DDF_LOCATORS_R | DDF_PEERS;
	if (all)
		flags |= DDF_ENDPOINTS_L | DDF_ENDPOINTS_R |
			 DDF_TOPICS_L | DDF_TOPICS_R |
			 DDF_GUARD_L | DDF_GUARD_R;
	dump_domains (flags);
}

#endif

