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

/* bgns.c -- Implements the Background Notification Server functionality. */

#include <stdint.h>
#include <arpa/inet.h>
#include "log.h"
#include "list.h"
#include "error.h"
#include "sock.h"
#include "libx.h"
#include "ri_data.h"
#include "ri_bgcp.h"
#include "dds/dds_dreader.h"
#include "dds/dds_aux.h"
#include "bgns.h"

#ifdef TCP_SUSPEND

#define	MAX_SERVER_STR	79

typedef struct bgns_reader_st BGNS_READER;
typedef struct bgns_notification_st BGNS_NOTIF;
typedef struct bgns_client_st BGNS_CLIENT;
typedef struct bgns_data_st BGNS_DATA;

/* Activated reader for a suspended peer client. */
struct bgns_reader_st {
	BGNS_READER	*next;
	BGNS_READER	*prev;
	BGNS_DATA	*data;
	String_t	*topic_name;
	String_t	*type_name;
	DDS_DynamicTypeSupport ts;
	DDS_DynamicType	type;
	DDS_Topic	topic;
	DDS_DataReader	reader;
	unsigned	nusers;
};

/* List of activated readers. */
typedef struct bgns_readers_st {
	BGNS_READER	*head;
	BGNS_READER	*tail;
} BGNS_READERS;

/* Effective reader list for a peer client. */
struct bgns_notification_st {
	BGNS_NOTIF	*next;
	BGNS_NOTIF	*prev;
	BGNS_READER	*reader;
	BGNS_CLIENT	*client;
	GUID_t		guid;
};

/* Reader list for a peer client. */
typedef struct bgns_notifs_st {
	BGNS_NOTIF	*head;
	BGNS_NOTIF	*tail;
} BGNS_NOTIFS;

/* Peer client structure. */
struct bgns_client_st {
	BGNS_CLIENT	*next;		/* List of clients. */
	BGNS_CLIENT	*prev;
	BGNS_DATA	*data;		/* Server instance. */
	GuidPrefix_t	prefix;		/* GUID prefix. */
	BGNS_NOTIFS	notifs;		/* Active notifications. */
	int		suspended;	/* Currently suspended? */
	void		*cx;		/* BGCP client reference. */
	DDS_Time_t	suspend_time;	/* Time of suspend. */
};

/* List of clients for a domain. */
typedef struct bgns_clients_st {
	BGNS_CLIENT	*head;
	BGNS_CLIENT	*tail;
} BGNS_CLIENTS;

/* Server instance, responsible for all clients in a given domain. */
struct bgns_data_st {
	BGNS_DATA	*next;
	BGNS_DATA	*prev;
	unsigned	port;
	int		secure;
	int		ipv6;
	unsigned	domain;
	unsigned	handle;
	unsigned	nh;
	Domain_t	*part;
	DDS_Subscriber	subscriber;
	BGNS_CLIENTS	clients;
	BGNS_READERS	readers;
};

/* List of server instances. */
typedef struct bgns_list_st {
	BGNS_DATA	*head;
	BGNS_DATA	*tail;
} BGNS_LIST;

static BGNS_LIST 	servers;
static int	 	initialized;
static DDS_Activities_on_wakeup		bgns_wakeup_fct;
static DDS_Activities_on_client_change	bgns_client_fct;

static unsigned		bgns_domain;
static int		bgns_secure;
static unsigned		bgns_port;
static int		bgns_port_valid;
static char		bgns_server [MAX_SERVER_STR + 1];
static int		bgns_server_valid;
static unsigned		bgns_handle;
#ifdef DDS_IPV6
static int		bgns_secure6;
static unsigned		bgns_port6;
static int		bgns_port6_valid;
static char		bgns_server6 [MAX_SERVER_STR + 1];
static int		bgns_server6_valid;
static unsigned		bgns_handle6;
#endif

/* bgns_lookup -- Lookup a server context. */

static BGNS_DATA *bgns_lookup (unsigned port, int secure, unsigned domain_id)
{
	BGNS_DATA	*p;

	ARG_NOT_USED (secure)

	LIST_FOREACH (servers, p)
		if (p->port == port || p->domain == domain_id)
			return (p);

	return (NULL);
}

/* bgns_reader_dispose -- Dispose a proxy reader. */

static void bgns_reader_dispose (BGNS_DATA *dp, BGNS_READER *rp)
{
	if (--rp->nusers)
		return;

	/*if (dp->subscriber) {*/
		DDS_Subscriber_delete_datareader (dp->subscriber, rp->reader);
		DDS_DomainParticipant_delete_topic (dp->part, rp->topic);
		DDS_DynamicTypeSupport_unregister_type (rp->ts, dp->part, (const DDS_ObjectName) str_ptr (rp->type_name));
	/*}*/
	DDS_DynamicTypeSupport_delete_type_support (rp->ts);
	DDS_DynamicTypeBuilderFactory_delete_type (rp->type);
	str_unref (rp->topic_name);
	str_unref (rp->type_name);
	LIST_REMOVE (dp->readers, *rp);
	xfree (rp);
}

/* bgns_notification_dispose -- Dispose a client notification. */

static void bgns_notification_dispose (BGNS_DATA *dp, BGNS_NOTIFS *lp, BGNS_NOTIF *np)
{
	ARG_NOT_USED (lp)

	LIST_REMOVE (*lp, *np);
	bgns_reader_dispose (dp, np->reader);
	xfree (np);
}

/* bgns_notifications_dispose -- Dispose all client readers. */

static void bgns_notifications_dispose (BGNS_DATA *dp, BGNS_CLIENT *cp)
{
	BGNS_NOTIF	*np, *next_np;
	char		buf [32];

	for (np = LIST_HEAD (cp->notifs); np; np = next_np) {
		next_np = LIST_NEXT (cp->notifs, *np);
		log_printf (BGNS_ID, 0, "BGNS: Remove %s/%s proxy reader for %s.\r\n",
				str_ptr (np->reader->topic_name),
				str_ptr (np->reader->type_name),
				guid_prefix_str (&cp->prefix, buf));
		bgns_notification_dispose (dp, &cp->notifs, np);
	}
}

/* bgns_client_dispose -- Dispose all client data. */

static void bgns_client_dispose (BGNS_DATA *dp, BGNS_CLIENT *cp)
{
	/*char	buffer [32];

	log_printf (BGNS_ID, 0, "BGNS: Client %s is gone.\r\n", 
				guid_prefix_str (&cp->prefix, buffer));*/
	if (bgns_client_fct)
		(*bgns_client_fct) (dp->domain,
				    (DDS_BuiltinTopicKey_t *) &cp->prefix,
				    DDS_ACTIVITIES_CLIENT_DIED);
	LIST_REMOVE (dp->clients, *cp);
	bgns_notifications_dispose (dp, cp);
	xfree (cp);
}

/* bgns_match_ev -- Match/unmatch connection callback function. */

static void bgns_match_ev (int          enable,
			   uintptr_t    user,
			   GuidPrefix_t *peer,
			   void         **pctxt)
{
	BGNS_DATA	*server, *sp;
	BGNS_CLIENT	*cp, *found_cp = NULL;
	char		buf [32];

	log_printf (BGNS_ID, 0, "BGNS: %satch detected to %s.\r\n",
					(enable) ? "M" : "Unm",
					guid_prefix_str (peer, buf));
	if (enable) {
		server = (BGNS_DATA *) user;
		LIST_FOREACH (servers, sp)
			if (server == sp)
				break;

		if (!sp) {
			log_printf (BGNS_ID, 0, "BGNS: no such server :-( ... \r\n");
			return;
		}
		LIST_FOREACH (server->clients, cp)	
			if (guid_prefix_eq (cp->prefix, *peer)) {
				found_cp = cp;
				break;
			}
		if (found_cp) {
			*pctxt = found_cp;
			return;
		}
		cp = xmalloc (sizeof (BGNS_CLIENT));
		if (!cp)
			return;

		cp->data = server;
		cp->prefix = *peer;
		LIST_INIT (cp->notifs);
		cp->suspended = 0;
		cp->cx = NULL;
		LIST_ADD_TAIL (server->clients, *cp);
		*pctxt = cp;
		if (bgns_client_fct)
			(*bgns_client_fct) (server->domain,
					    (DDS_BuiltinTopicKey_t *) peer,
					    DDS_ACTIVITIES_CLIENT_ACTIVE);
	}
	else {
		found_cp = (BGNS_CLIENT *) user;
		server = found_cp->data;
		bgns_client_dispose (server, found_cp);
	}
}

/* bgns_notify_ev -- Ask server to enable/disable a topic/type notification. If
		     successful, the server will return a non-0 result and set
		     the cookie.  If not, the request will be declined and 0 is
		     returned. */

static int bgns_notify_ev (int        enable,
			   uintptr_t  user,
			   uintptr_t  pctxt,
			   const char *topic,
			   const char *type,
			   uintptr_t  *cookie)
{
	BGNS_CLIENT	*cp = (BGNS_CLIENT *) user;
	char		buf [32];
	static uint32_t	cookie_value = 0;

	ARG_NOT_USED (pctxt)

	log_printf (BGNS_ID, 0, "BGNS: Notification %s from %s for %s/%s.\r\n",
					(enable) ? "enabled" : "disabled",
					guid_prefix_str (&cp->prefix, buf),
					topic,
					type);

	*cookie = ++cookie_value;
	return (1);
}

static void wakeup_client (BGNS_CLIENT *cp, BGNS_READER *brp, Participant_t *src)
{
	log_printf (BGNS_ID, 0, "BGNS: Wakeup client <- domain %u data on %s/%s!\r\n",
		    brp->data->domain,
		    str_ptr (brp->topic_name),
		    str_ptr (brp->type_name));
	bgcp_wakeup (cp->cx,
		     str_ptr (brp->topic_name),
		     str_ptr (brp->type_name),
		     &src->p_guid_prefix);
}

#define	GT(t1,t2)	(((t1).sec > (t2).sec) || \
			 ((t1).sec == (t2).sec && (t1).nanosec > (t2).nanosec))

void read_data (DDS_DataReaderListener *l, DDS_DataReader dr)
{
	static DDS_DynamicDataSeq	drx_sample = DDS_SEQ_INITIALIZER (void *);
	static DDS_SampleInfoSeq	rx_info = DDS_SEQ_INITIALIZER (DDS_SampleInfo *);
	DDS_SampleStateMask		ss = DDS_NOT_READ_SAMPLE_STATE;
	DDS_ViewStateMask		vs = DDS_ANY_VIEW_STATE;
	DDS_InstanceStateMask		is = DDS_ANY_INSTANCE_STATE;
	DDS_SampleInfo			*info;
	DDS_Time_t			t;
	DDS_ReturnCode_t		error;
	BGNS_CLIENT			*cp;
	BGNS_NOTIF			*np;
	BGNS_READER			*brp;
	Participant_t			*src;
	unsigned			i;
	int				ds;
	unsigned			dns;

	brp = (BGNS_READER *) l->cookie;
	for (;;) {
		error = DDS_DynamicDataReader_take (dr, &drx_sample, &rx_info, 16, ss, vs, is);
		if (error) {
			if (error != DDS_RETCODE_NO_DATA)
				printf ("Unable to read samples: error = %s!\r\n", DDS_error (error));
			break;
		}
		for (i = 0; i < DDS_SEQ_LENGTH (rx_info); i++) {
			info = DDS_SEQ_ITEM (rx_info, i);
			if (info->valid_data) {
				src = entity_participant (info->publication_handle);
				if (!src)
					continue;

				ds = src->p_dt >> 32;
				dns = (unsigned) (((src->p_dt & 0xffffffff) * 1000000000) >> 32);
				t.sec = info->source_timestamp.sec + ds;
				t.nanosec = info->source_timestamp.nanosec;
				if (ds > 0) {
					t.nanosec += dns;
					while (t.nanosec >= 1000000000UL) {
						t.nanosec -= 1000000000UL;
						t.sec++;
					}
				}
				else if (ds < 0) {
					if (t.nanosec < dns) {
						t.sec--;
						t.nanosec += 1000000000UL;
					}
					t.nanosec -= dns;
				}
				LIST_FOREACH (brp->data->clients, cp) {
					if (!cp->suspended)
						continue;

					LIST_FOREACH (cp->notifs, np) {
						/*log_printf (BGNS_ID, 0, "BGNS: data sent: %d.%09u, suspended: %d.%09u\r\n",
								info->source_timestamp.sec, info->source_timestamp.nanosec,
								cp->suspend_time.sec, cp->suspend_time.nanosec);*/
						if (np->reader == brp &&
						    GT (t, cp->suspend_time)) {
							wakeup_client (cp, brp, src);
							break;
						}
					}
				}
			}
		}
		DDS_DynamicDataReader_return_loan (dr, &drx_sample, &rx_info);
	}
}

static DDS_DataReaderListener listener = {
	NULL,		/* Sample rejected. */
	NULL,		/* Liveliness changed. */
	NULL,		/* Requested Deadline missed. */
	NULL,		/* Requested incompatible QoS. */
	read_data,	/* Data available. */
	NULL,		/* Subscription matched. */
	NULL,		/* Sample lost. */
	NULL		/* Cookie */
};

/* bgns_new_reader -- Add a new DDS reader. */

static BGNS_READER *bgns_new_reader (BGNS_CLIENT *cp,
				     String_t    *topic,
				     String_t    *type)
{
	Participant_t		*pp;
	Endpoint_t		*ep;
	DDS_DomainParticipant	part;
	DDS_DynamicTypeBuilder	tb;
	DDS_TypeObject		tobj;
	DDS_TopicDescription	td;
	DDS_DataReaderQos	rqos;
	DDS_BuiltinTopicKey_t	key;
	BGNS_READER		*brp;

	brp = xmalloc (sizeof (BGNS_READER));
	if (!brp) {
		log_printf (BGNS_ID, 0, "BGNS: out-of-memory for reader!\r\n");
		return (NULL);
	}
	brp->data = cp->data;
	brp->topic_name = str_ref (topic);
	brp->type_name = str_ref (type);

	/* Taking a shortcut by not using DCPS builtin readers here.
	   This does mean that we need to fiddle a bit with the arguments :-) */
	part = cp->data->part;
	pp = participant_lookup (part, &cp->prefix);
	if (!pp) {
		log_printf (BGNS_ID, 0, "BGNS: client participant no longer usable!\r\n");
		goto no_dp;
	}
	ep = endpoint_from_name (pp, str_ptr (topic), str_ptr (type), 1);
	if (!ep) {
		log_printf (BGNS_ID, 0, "BGNS: client endpoint no longer present!\r\n");
		goto no_dp;
	}
	memset (&key, 0, sizeof (key));
	key.value [DDS_BUILTIN_TOPIC_SIZE_NATIVE - 1] = ep->entity_id.w;
	tobj = DDS_TypeObject_create_from_key (part,
					       (DDS_BuiltinTopicKey_t *) &pp->p_guid_prefix,
					       &key);
	if (!tobj) {
		log_printf (BGNS_ID, 0, "BGNS: type object couldn't be created for type '%s'!\r\n", str_ptr (type));
		goto no_dp;
	}
	tb = DDS_DynamicTypeBuilderFactory_create_type_w_type_object (tobj);
	DDS_TypeObject_delete (tobj);
	if (!tb) {
		log_printf (BGNS_ID, 0, "BGNS: can't get Type from Type object for type '%s'!", str_ptr (type));
		goto no_dp;
	}
	brp->type = DDS_DynamicTypeBuilder_build (tb);
	DDS_DynamicTypeBuilderFactory_delete_type (tb);
	if (!brp->type) {
		log_printf (BGNS_ID, 0, "BGNS: can't get build Type from Type builder for type '%s'!", str_ptr (type));
		goto no_dp;
	}
	brp->ts = DDS_DynamicTypeSupport_create_type_support (brp->type);
	if (!brp->ts) {
		log_printf (BGNS_ID, 0, "BGNS: can't get TypeSupport from Type for type '%s'!", str_ptr (type));
		goto no_ts;
	}
	if (DDS_DynamicTypeSupport_register_type (brp->ts, part, (const DDS_ObjectName) str_ptr (type))) {
		log_printf (BGNS_ID, 0, "BGNS: Can't register TypeSupport in domain for type '%s'!", str_ptr (type));
		goto no_reg;
	}
	brp->nusers = 1;
	LIST_ADD_TAIL (cp->data->readers, *brp);

	if (!cp->data->subscriber) {
		cp->data->subscriber = DDS_DomainParticipant_create_subscriber (part, NULL, NULL, 0); 
		if (!cp->data->subscriber) {
			log_printf (BGNS_ID, 0, "BGNS: Can't create subscriber in domain %u!", cp->data->domain);
			goto no_sub;
		}
	}
	brp->topic = DDS_DomainParticipant_create_topic (part,
							 str_ptr (topic),
							 str_ptr (type),
							 NULL,
							 NULL,
							 0);
	if (!brp->topic) {
		log_printf (BGNS_ID, 0, "BGNS: Can't create Topic '%s'!", str_ptr (topic));
		goto no_sub;
	}
	td = DDS_DomainParticipant_lookup_topicdescription (part, str_ptr (topic));
	if (!td) {
		log_printf (BGNS_ID, 0, "BGNS: Can't get TopicDescription for '%s'!", str_ptr (topic));
		goto no_topic_desc;
	}
	DDS_Subscriber_get_default_datareader_qos (cp->data->subscriber, &rqos);
	if (ep->qos->qos.durability_kind >= DDS_TRANSIENT_LOCAL_DURABILITY_QOS)
		rqos.durability.kind = DDS_TRANSIENT_LOCAL_DURABILITY_QOS
				     /*DDS_VOLATILE_DURABILITY_QOS*/;
	rqos.reliability.kind = ep->qos->qos.reliability_kind;
	rqos.ownership.kind = ep->qos->qos.ownership_kind;
	if (ep->qos->qos.user_data) {
		DDS_SEQ_DATA (rqos.user_data.value) = (unsigned char *) str_ptr (ep->qos->qos.user_data);
		DDS_SEQ_LENGTH (rqos.user_data.value) = 
		DDS_SEQ_MAXIMUM (rqos.user_data.value) = str_len (ep->qos->qos.user_data);
	}
	listener.cookie = brp;
	brp->reader = DDS_Subscriber_create_datareader (cp->data->subscriber, td, &rqos,
						       &listener,
						       DDS_DATA_AVAILABLE_STATUS);
	if (!brp->reader) {
		log_printf (BGNS_ID, 0, "BGNS: Can't create reader for '%s'!", str_ptr (topic));
		goto no_reader;
	}
	return (brp);

    no_reader:
    no_topic_desc:
	DDS_DomainParticipant_delete_topic (part, brp->topic);

    no_sub:
	DDS_DynamicTypeSupport_unregister_type (brp->ts, part, (const DDS_ObjectName) str_ptr (type));

    no_reg:
	DDS_DynamicTypeSupport_delete_type_support (brp->ts);

    no_ts:
	DDS_DynamicTypeBuilderFactory_delete_type (brp->type);

    no_dp:
	xfree (brp);
	return (NULL);
}

/* bgns_add_reader -- Add a proxy reader for a specific client endpoint. */

static int bgns_add_reader (BGNS_CLIENT *cp, String_t *topic, String_t *type)
{
	BGNS_READER	*rp;
	BGNS_NOTIF	*np;
	char		buf [32];

	log_printf (BGNS_ID, 0, "BGNS: Add %s/%s proxy reader for %s.\r\n",
				(topic) ? str_ptr (topic) : NULL,
				(type) ? str_ptr (type) : NULL,
				guid_prefix_str (&cp->prefix, buf));

	if (!topic || !type)
		return (0);

	LIST_FOREACH (cp->data->readers, rp)
		if (rp->topic_name == topic && rp->type_name == type) {
			rp->nusers++;
			break;
		}

	if (LIST_END (cp->data->readers, rp)) {
		rp = bgns_new_reader (cp, topic, type);
		if (!rp)
			return (0);
	}
	np = xmalloc (sizeof (BGNS_NOTIF));
	if (!np) {
		bgns_reader_dispose (cp->data, rp);
		log_printf (BGNS_ID, 0, "BGNS: out-of-memory for notified reader!\r\n");
		return (0);
	}
	np->reader = rp;
	np->client = cp;
	LIST_ADD_TAIL (cp->notifs, *np);
	return (1);
}

/* bgns_add_readers -- A client is entering suspend mode.  Create all proxy
		       readers for topics he's interested in.
		       Note that at this stage, client discovery data is used
		       to figure out which readers the client is using. */

static unsigned bgns_add_readers (BGNS_CLIENT *cp, void *cx)
{
	Participant_t	*pp;
	Endpoint_t	*ep;
	String_t	*topic, *type;
	unsigned	n = 0;

	pp = participant_lookup (cp->data->part, &cp->prefix);
	if (!pp)
		return (0);	/* Participant no longer there - must have timed out. */

	/* Calculate client participant timestamp = our time of suspend delta. */
	DDS_DomainParticipant_get_current_time (cp->data->part, &cp->suspend_time);

	/* Add proxy readers. */
	for (ep = endpoint_first (pp); ep; ep = endpoint_next (pp, &ep->entity_id)) {
		topic = ep->topic->name;
		type = ep->topic->type->type_name;
		if (entity_id_reader (ep->entity_id) &&
		    bgcp_match (cx, str_ptr (topic), str_ptr (type)) &&
		    bgns_add_reader (cp, topic, type))
			n++;
	}
	return (n);
}

static void wakeup_no_info (BGNS_CLIENT *cp)
{
	log_printf (BGNS_ID, 0, "BGNS: Wakeup client <- insufficient info for notifications!\r\n");
	bgcp_wakeup (cp->cx, NULL, NULL, &cp->data->part->participant.p_guid_prefix);
}

/* bgns_suspend_ev -- Callback to specify that a peer went to sleep. */

static void bgns_suspend_ev (int       enable,
			     uintptr_t user,
			     void      *cx)
{
	BGNS_CLIENT	*cp;
	unsigned	n;
	char		buffer [32];

	cp = (BGNS_CLIENT *) user;
	if (!cp)
		return;

	/*log_printf (BGNS_ID, 0, "BGNS: %s from (0x%lx).\r\n",
					(enable) ? "Suspend" : "Resume",
					(unsigned long) user);*/
	if (enable && !cp->suspended) {

		/* Figure out which client readers need to be proxied, based
		   on received notification registrations. */
		n = bgns_add_readers (cp, cx);
		cp->cx = cx;
		cp->suspended = 1;
		log_printf (BGNS_ID, 0, "BGNS: Client %s went to sleep.\r\n", 
					guid_prefix_str (&cp->prefix, buffer));
		if (bgns_client_fct)
			(*bgns_client_fct) (cp->data->domain,
					    (DDS_BuiltinTopicKey_t *) &cp->prefix,
					    DDS_ACTIVITIES_CLIENT_SLEEPING);
		if (!n)
			wakeup_no_info (cp);
	}
	else if (!enable && cp->suspended) {

		/* Figure out which client readers need to be proxied, based
		   on received notification registrations. */
		bgns_notifications_dispose (cp->data, cp);
		cp->cx = NULL;
		cp->suspended = 0;
		log_printf (BGNS_ID, 0, "BGNS: Client %s became active.\r\n", 
					guid_prefix_str (&cp->prefix, buffer));
		if (bgns_client_fct)
			(*bgns_client_fct) (cp->data->domain,
					    (DDS_BuiltinTopicKey_t *) &cp->prefix,
					    DDS_ACTIVITIES_CLIENT_ACTIVE);
	}
	/*dump_data ();*/
}

/* bgns_disc_reader_new -- A new reader was discovered while suspended.
			   Can happen due to timing where discovery happens
			   immediately after client suspended. */

static void bgns_disc_reader_new (BGNS_CLIENT                      *cp,
				  DDS_SubscriptionBuiltinTopicData *data)
{
	BGNS_NOTIF	*np;

	if (bgcp_match (cp->cx, data->topic_name, data->type_name)) {

		/* Check if it already exists: */
		LIST_FOREACH (cp->notifs, np)
			if (!memcmp (&np->guid, &data->key, GUID_SIZE))
				return;	/* Nothing to do. */

		/* Didn't see this one yet: add it! */
		log_printf (BGNS_ID, 0, "BGNS: discovery: add proxy reader!\r\n");
		bgns_add_reader (cp,
				 str_new_cstr (data->topic_name),
				 str_new_cstr (data->type_name));
	}
}

/* bgns_disc_subscription -- Discovery notification: peer reader change. */

static void bgns_disc_subscription (DDS_BuiltinTopicKey_t            *key,
				    DDS_SubscriptionBuiltinTopicData *data,
				    DDS_SampleInfo                   *info,
				    uintptr_t                        user)
{
	BGNS_DATA	*np = (BGNS_DATA *) user;
	BGNS_CLIENT	*cp;

	LIST_FOREACH (np->clients, cp)
		if (!memcmp (cp->prefix.prefix, (unsigned char *) key, GUIDPREFIX_SIZE)) {
			log_printf (BGNS_ID, 0, "BGNS: discovery: client subscription!\r\n");
			if (!cp->suspended)
				break;

			if ((info->view_state & DDS_NEW_VIEW_STATE) != 0)
				bgns_disc_reader_new (cp, data);
			break;
		}
}

/* bgns_discovery_attach -- Attach to the discovery services. */

static void bgns_discovery_attach (BGNS_DATA *np)
{
	DDS_ReturnCode_t	ret;

	log_printf (BGNS_ID, 0, "BGNS: attach discovery service.\r\n");
	np->nh = DDS_Notification_attach (
			np->part,
			DDS_NOTIFY_PARTICIPANT | DDS_NOTIFY_SUBSCRIPTION,
			NULL,
			NULL,
			NULL,
			bgns_disc_subscription,
			NULL,
			(uintptr_t) np,
			&ret);
	if (!np->nh)
		warn_printf ("BGNS: Couldn't attach to discovery service!");
}

/* bgns_start_server -- Start the background notification server.  Returns a
			non-0 handle if successful. */

int bgns_start_server (unsigned         port,
		       int              ipv6,
		       int              secure,
		       unsigned         domain_id,
		       DDS_ReturnCode_t *error)
{
	BGNS_DATA	*np;
	unsigned	h;

	if (!initialized) {
		LIST_INIT (servers);
		initialized = 1;
	}
	np = bgns_lookup (port, secure, domain_id);
	if (np) {
		*error = DDS_RETCODE_PRECONDITION_NOT_MET;
		return (0);
	}
	np = xmalloc (sizeof (BGNS_DATA));
	if (!np) {
		*error = DDS_RETCODE_OUT_OF_RESOURCES;
		return (0);
	}
	log_printf (BGNS_ID, 0, "BGNS: bgns_start_server(%u, %sIPv%c, %u)\r\n", 
					port, (secure) ? "secure " : "",
					(ipv6) ? '6' : '4', domain_id);
	h = bgcp_start_server (port,
			       ipv6,
			       secure,
			       domain_id,
			       bgns_match_ev,
			       bgns_notify_ev,
			       bgns_suspend_ev,
			       (uintptr_t) np,
			       error);
	if (!h) {
		xfree (np);
		return (0);
	}
	np->port = port;
	np->secure = secure;
	np->ipv6 = ipv6;
	np->domain = domain_id;
	np->handle = h;
	np->part = domain_lookup (domain_id);
	np->subscriber = NULL;
	LIST_INIT (np->clients);
	LIST_INIT (np->readers);
	LIST_ADD_TAIL (servers, *np);
	log_printf (BGNS_ID, 0, "BGNS: server started [%u]!\r\n", h);

	if (np->part)
		bgns_discovery_attach (np);

	return (h);
}

/* bgns_client_match_ind -- Client match callback. */

static void bgns_client_match_ind (int          enable,
			           uintptr_t    data,
			           GuidPrefix_t *peer,
			           void         **pctxt)
{
	char	buf [32];

	ARG_NOT_USED (data)

	if (enable)
		log_printf (BGNS_ID, 0, "BGNS: connected to server %s.\r\n", guid_prefix_str (peer, buf));
	else
		log_printf (BGNS_ID, 0, "BGNS: connection to server %s lost.\r\n", guid_prefix_str (peer, buf));
	*pctxt = NULL;
}

/* bgns_client_wakeup -- Client needs to wakeup callback. */

static void bgns_client_wakeup_ind (uintptr_t     data,
				    const char    *topic_name,
				    const char    *type_name,
				    unsigned char prefix [12])
{
	char	buf [32];

	ARG_NOT_USED (data)

	if (bgns_wakeup_fct) {
		(*bgns_wakeup_fct) (topic_name, type_name, prefix);
		return;
	}
	log_printf (BGNS_ID, 0, "BGNS: wakeup (%s/%s by %s)!\r\n",
					topic_name,
					type_name,
					guid_prefix_str ((GuidPrefix_t *) prefix, buf));
	DDS_Activities_resume (DDS_ALL_ACTIVITY);
}

/* bgns_register_wakeup -- Register a callback function to be called on wakeup. */

void bgns_register_wakeup (DDS_Activities_on_wakeup fct)
{
	bgns_wakeup_fct = fct;
}

void bgns_register_info (DDS_Activities_on_client_change fct)
{
	BGNS_DATA	*sp;
	BGNS_CLIENT	*cp;

	bgns_client_fct = fct;
	if (!fct || !initialized)
		return;

	LIST_FOREACH (servers, sp)
		LIST_FOREACH (sp->clients, cp)
			(*fct) (sp->domain,
			        (DDS_BuiltinTopicKey_t *) &cp->prefix,
			         cp->suspended ?
					DDS_ACTIVITIES_CLIENT_SLEEPING :
					DDS_ACTIVITIES_CLIENT_ACTIVE);
}

/* bgns_start_client -- Start a background notification service client. */

int bgns_start_client (const char       *server,
		       int              ipv6,
		       int              secure,
		       unsigned         domain,
		       DDS_ReturnCode_t *error)
{
	char			*cp;
	int			d [4], h, n;
	static RTPS_TCP_RSERV	dest;
	static char		name [80];

#ifndef DDS_IPV6
	ARG_NOT_USED(ipv6)
#endif

	log_printf (BGNS_ID, 0, "BGNS: bgns_start_client(%s, %sIPv%c, %u)\r\n", server, (secure) ? "secure " : "", (ipv6) ? '6' : '4', domain);
	if (!initialized) {
		LIST_INIT (servers);
		initialized = 1;
	}
	if (!server || server [0] == '\0')
		h = bgcp_start_client (NULL,
				       domain,
				       bgns_client_match_ind,
				       bgns_client_wakeup_ind,
				       0,
				       error);
	else {
		strcpy (name, server);
		if ((cp = strrchr (name, '$')) == NULL &&
		    (cp = strrchr (name, ':')) == NULL) {
			*error = DDS_RETCODE_BAD_PARAMETER;
			return (-1);
		}
		*cp = '\0';
		dest.port = atoi (++cp);
		dest.secure = secure;
		if ((dest.name = is_dns_name (name)) != 0)
			dest.addr.name = name;
#ifdef DDS_IPV6
		else if (ipv6) {
			if (!inet_pton (AF_INET6, name, dest.addr.ipa_v6)) {
				*error = DDS_RETCODE_BAD_PARAMETER;
				return (-1);
			}
		}
#endif
		else {
			n = sscanf (name, "%d.%d.%d.%d", &d [0], &d [1],
					                 &d [2], &d [3]);
			if (n != 4)
				*error = DDS_RETCODE_BAD_PARAMETER;
			dest.addr.ipa_v4 = (d [0] << 24) |
					   (d [1] << 16) |
					   (d [2] << 8) |
					    d [3];
		}
		h = bgcp_start_client (&dest,
				       domain, 
				       bgns_client_match_ind,
				       bgns_client_wakeup_ind,
				       0,
				       error);
	}
	log_printf (BGNS_ID, 0, "BGNS: client started [%u]!\r\n", h);
	return (h);
}

/* bgns_stop -- Stop the background notification server. */

int bgns_stop (unsigned handle)
{
	BGNS_DATA	*sp;
	BGNS_CLIENT	*cp, *next_cp;

	/*log_printf (BGNS_ID, 0, "BGNS: bgns_stop(%u)\r\n", handle);*/
	if (!initialized)
		return (DDS_RETCODE_OK);

	LIST_FOREACH (servers, sp)
		if (sp->handle == handle)
			break;

	if (LIST_END (servers, sp))
		sp = NULL;

	bgcp_stop (handle);

	if (sp) {
		LIST_REMOVE (servers, *sp);
		for (cp = LIST_HEAD (sp->clients); cp; cp = next_cp) {
			next_cp = LIST_NEXT (sp->clients, *cp);
			bgns_client_dispose (sp, cp);
		}
		xfree (sp);
	}
	return (DDS_RETCODE_OK);
}

void bgns_dispose (Domain_t *dp)
{
	BGNS_DATA	*p, *next_p;

	if (initialized)
		for (p = LIST_HEAD (servers); p; p = next_p) {
			next_p = LIST_NEXT (servers, *p);
			if (p->part == dp)
				bgns_stop (p->handle);
		}
}

void bgns_activate (Domain_t *dp)
{
	BGNS_DATA	*p, *next_p;

	if (!initialized)
		return;

	for (p = LIST_HEAD (servers); p; p = next_p) {
		next_p = LIST_NEXT (servers, *p);
		if (!p->part && p->domain == dp->domain_id) {
			p->part = dp;
			bgns_discovery_attach (p);
		}
	}
	bgcp_domain_active (dp->domain_id);
}

void bgns_deactivate (Domain_t *dp)
{
	BGNS_DATA	*p, *next_p;

	if (!initialized)
		return;

	bgcp_domain_inactive (dp->domain_id);
	for (p = LIST_HEAD (servers); p; p = next_p) {
		next_p = LIST_NEXT (servers, *p);
		if (p->part == dp) {
			p->subscriber = NULL;
			p->part = NULL;
		}
	}
}

void bgns_final (void)
{
	BGNS_DATA	*p, *next_p;

	if (initialized)
		for (p = LIST_HEAD (servers); p; p = next_p) {
			next_p = LIST_NEXT (servers, *p);
			bgns_stop (p->handle);
		}

	bgcp_final ();
}

/* bgns_mode_update -- Update BGNS parameters due to config changes. */

static void bgns_mode_update (int ipv6)
{
	int			h;
	DDS_ReturnCode_t	error;

#ifdef DDS_IPV6
	if (!ipv6) {
#endif
		if (bgns_handle) {
			bgns_stop (bgns_handle);
			bgns_handle = 0;
		}
		if (!ipv6 && bgns_port_valid) {
			h = bgns_start_server (bgns_port, 0, bgns_secure, bgns_domain, &error);
			if (h > 0)
				bgns_handle = h;
			else
				log_printf (BGNS_ID, 0, "BGNS: error %u (%s) starting TCP %sserver!\r\n",
						error, DDS_error (error), 
						(bgns_secure) ? "secure " : "");
		}
		else if (!ipv6 && bgns_server_valid) {
			h = bgns_start_client (bgns_server, 0, bgns_secure, bgns_domain, &error);
			if (h > 0)
				bgns_handle = h;
			else
				log_printf (BGNS_ID, 0, "BGNS: error %u (%s) starting TCP %sclient!\r\n",
						error, DDS_error (error), 
						(bgns_secure) ? "secure " : "");
		}
#ifdef DDS_IPV6
	}
	else {
		if (bgns_handle6) {
			bgns_stop (bgns_handle6);
			bgns_handle6 = 0;
		}
		if (bgns_port6_valid) {
			h = bgns_start_server (bgns_port6, 1, bgns_secure6, bgns_domain, &error);
			if (h > 0)
				bgns_handle6 = h;
			else
				log_printf (BGNS_ID, 0, "BGNS: error %u (%s) starting TCPv6 %sserver!\r\n",
						error, DDS_error (error),
						(bgns_secure6) ? "secure " : "");
		}
		else if (bgns_server6_valid) {
			h = bgns_start_client (bgns_server, 1, bgns_secure, bgns_domain, &error);
			if (h > 0)
				bgns_handle6 = h;
			else
				log_printf (BGNS_ID, 0, "BGNS: error %u (%s) starting TCPv6 %sclient!\r\n",
						error, DDS_error (error),
						(bgns_secure6) ? "secure " : "");
		}
	}
#endif
}

/* bgns_check_close -- Check if already established and close down if so. */

static void bgns_check_close (int ipv6)
{
	if (!ipv6 && bgns_handle) {
		bgns_stop (bgns_handle);
		bgns_handle = 0;
	}
#ifdef DDS_IPV6
	if (ipv6 && bgns_handle6) {
		bgns_stop (bgns_handle6);
		bgns_handle6 = 0;
	}
#endif
}

/* bgns_domain_change -- BGNS domain update callback. */

static void bgns_domain_change (Config_t c)
{
	unsigned	new_domain;

	if (config_defined (c)) {
		new_domain = config_get_number (c, 0);
		log_printf (BGNS_ID, 0, "BGNS: Domain set (%u).\r\n", new_domain);
		if (new_domain != bgns_domain) {
			bgns_domain = new_domain;
			bgns_mode_update (0);
#ifdef DDS_IPV6
			bgns_mode_update (1);
#endif
		}
	}
	else if (bgns_domain) {
		log_printf (BGNS_ID, 0, "BGNS: Domain cleared.\r\n");
		bgns_check_close (0);
#ifdef DDS_IPV6
		bgns_check_close (1);
#endif
		bgns_domain = 0;
	}
}

/* bgns_port_change -- BGNS port update callback. */

static void bgns_port_change (Config_t c)
{
	int		secure, prev_secure, ipv6;
	const char	*s;
	unsigned	port, prev_port, i;

	switch (c) {
		case DC_BGNS_Port:
			ipv6 = 0;
			secure = 0;
			prev_secure = bgns_secure;
			prev_port = bgns_port;
			break;
#ifdef DDS_IPV6
		case DC_BGNS_Port6:
			ipv6 = 1;
			secure = 0;
			prev_secure = bgns_secure6;
			prev_port = bgns_port6;
			break;
#endif
#ifdef DDS_SECURITY
		case DC_BGNS_SecPort:
			ipv6 = 0;
			secure = 1;
			prev_secure = bgns_secure;
			prev_port = bgns_port;
			break;
#ifdef DDS_IPV6
		case DC_BGNS_SecPort6:
			ipv6 = 1;
			secure = 1;
			prev_secure = bgns_secure6;
			prev_port = bgns_port6;
			break;
#endif
#endif
		default:
			return;
	}
	if (config_defined (c)) {
		if (secure != prev_secure) {
			bgns_check_close (ipv6);
#ifdef DDS_IPV6
			if (ipv6)
				bgns_secure6 = secure;
			else
#endif
				bgns_secure = secure;
		}
		s = config_get_string (c, 0);
		if ((s [0] == '~' || s [0] == '@') && s [1] == '\0')
			port = 0;
		else if (s [0] >= '1' && s [0] <= '9') {
			port = i = 0;
			do {
				port = port * 10 + s [i++] - '0';
			}
			while (s [i] && s [i] >= '0' && s [i] <= '9');
			if (s [i] || port < 1024 || port > 65530)
				goto no_port;
		}
		else
			goto no_port;

		log_printf (BGNS_ID, 0, "BGNS: IPv%c %sort set (",
					(ipv6) ? '6' : '4',
					(secure) ? "Secure p" : "P");
		if (!port)
			log_printf (BGNS_ID, 0, "'@').\r\n");
		else
			log_printf (BGNS_ID, 0, "%u).\r\n", port);
		if (port != prev_port)
			bgns_check_close (ipv6);

#ifdef DDS_IPV6
		if (ipv6) {
			bgns_port6 = port;
			bgns_port6_valid = 1;
			bgns_mode_update (1);
		}
		else {
		
#endif
			bgns_port = port;
			bgns_port_valid = 1;
			bgns_mode_update (0);
#ifdef DDS_IPV6
		}
#endif
		return;
	}
	else if (!ipv6 && !bgns_port_valid)
		return;
#ifdef DDS_IPV6
	else if (ipv6 && !bgns_port6_valid)
		return;
#endif
	else
		log_printf (BGNS_ID, 0, "BGNS: Port cleared.\r\n");

    no_port:
	bgns_check_close (ipv6);

#ifdef DDS_IPV6
	if (ipv6)
		bgns_port6_valid = 0;
	else
#endif
		bgns_port_valid = 0;
}

/* bgns_server_change -- BGNS server update callback. */

static void bgns_server_change (Config_t c)
{
	int		secure, prev_secure, ipv6;
	const char	*server;
	char		sbuf [MAX_SERVER_STR + 1];

	switch (c) {
		case DC_BGNS_Server:
			ipv6 = 0;
			secure = 0;
			prev_secure = bgns_secure;
			break;
#ifdef DDS_IPV6
		case DC_BGNS_Server6:
			ipv6 = 1;
			secure = 0;
			prev_secure = bgns_secure6;
			break;
#endif
#ifdef DDS_SECURITY
		case DC_BGNS_SecServer:
			ipv6 = 0;
			secure = 1;
			prev_secure = bgns_secure;
			break;
#ifdef DDS_IPV6
		case DC_BGNS_SecServer6:
			ipv6 = 1;
			secure = 1;
			prev_secure = bgns_secure6;
			break;
#endif
#endif
		default:
			return;
	}
	if (config_defined (c)) {
		if (secure != prev_secure) {
			bgns_check_close (ipv6);
#ifdef DDS_IPV6
			if (ipv6) {
				bgns_secure6 = secure;
				bgns_server6_valid = 0;
			}
			else {
#endif
				bgns_secure = secure;
				bgns_server_valid = 0;
#ifdef DDS_IPV6
			}
#endif
		}
		server = config_get_string (c, NULL);
		if (strlen (server) > MAX_SERVER_STR) {
			bgns_check_close (ipv6);
#ifdef DDS_IPV6
			if (ipv6)
				bgns_server6_valid = 0;
			else
#endif
				bgns_server_valid = 0;
			return;
		}
		strcpy (sbuf, server);
		log_printf (BGNS_ID, 0, "BGNS: TCPv%c %server set ('%s')\r\n",
						(ipv6) ? '6' : '4',
						(secure) ? "ecure s" : "S", server);
		if ((server [0] == '~' || server [0] == '@') && server [1] == '\0')
			sbuf [0] = '\0';
		else if (!strrchr (server, '$') && !strrchr (server, ':')) {
			bgns_check_close (ipv6);
#ifdef DDS_IPV6
			if (ipv6)
				bgns_server6_valid = 0;
			else
#endif
				bgns_server_valid = 0;
			return;
		}
#ifdef DDS_IPV6
		if (ipv6) {
			if (bgns_server6_valid && strcmp (bgns_server6, sbuf))
				bgns_check_close (1);
			bgns_server6_valid = 1;
			strcpy (bgns_server6, sbuf);
		}
		else {
#endif
			if (bgns_server_valid && strcmp (bgns_server, sbuf))
				bgns_check_close (0);
			bgns_server_valid = 1;
			strcpy (bgns_server, sbuf);
#ifdef DDS_IPV6
		}
#endif
		bgns_mode_update (ipv6);
	}
	else if (!ipv6 && bgns_server_valid) {
		log_printf (BGNS_ID, 0, "BGNS: TCPv4 server cleared.\r\n");
		bgns_check_close (0);
		bgns_server [0] = '\0';
		bgns_server_valid = 0;
	}
#ifdef DDS_IPV6
	else if (ipv6 && bgns_server6_valid) {
		log_printf (BGNS_ID, 0, "BGNS: TCPv6 server cleared.\r\n");
		bgns_check_close (1);
		bgns_server6 [0] = '\0';
		bgns_server6_valid = 0;
	}
#endif
}

/* bgns_init -- BGNS initialization. */

int bgns_init (unsigned min_cx, unsigned max_cx)
{
	int	error;

	error = bgcp_init (min_cx, max_cx);

	config_notify (DC_BGNS_Domain, bgns_domain_change);
#ifdef DDS_SECURITY
	config_notify (DC_BGNS_SecPort, bgns_port_change);
	config_notify (DC_BGNS_SecServer, bgns_server_change);
#endif
	config_notify (DC_BGNS_Port, bgns_port_change);
	config_notify (DC_BGNS_Server, bgns_server_change);
#ifdef DDS_IPV6
#ifdef DDS_SECURITY
	config_notify (DC_BGNS_SecPort6, bgns_port_change);
	config_notify (DC_BGNS_SecServer6, bgns_server_change);
#endif
	config_notify (DC_BGNS_Port6, bgns_port_change);
	config_notify (DC_BGNS_Server6, bgns_server_change);
#endif

	return (error);
}

#ifdef DDS_DEBUG

static void dump_reader (BGNS_READER *rp, unsigned indent)
{
	unsigned	i;

	for (i = 0; i < indent; i++)
		dbg_printf ("\t");
	dbg_printf ("%s/%s*%u\r\n",
			str_ptr (rp->topic_name),
			str_ptr (rp->type_name),
			rp->nusers);
}

static void dump_client (BGNS_CLIENT *cp)
{
	BGNS_NOTIF	*np;
	char		buf [32];

	dbg_printf ("\t%s is %s",
			guid_prefix_str (&cp->prefix, buf),
			cp->suspended ? "suspended" : "awake");
	if (LIST_NONEMPTY (cp->notifs)) {
		dbg_printf (":\r\n");
		LIST_FOREACH (cp->notifs, np)
			dump_reader (np->reader, 2);
	}
	else
		dbg_printf ("\r\n");
}

static void dump_server (BGNS_DATA *dp)
{
	BGNS_CLIENT	*cp;
	BGNS_READER	*rp;

	dbg_printf ("Server (%u): Domain=%u, IPv%c Port=%u, %s\r\n",
			dp->handle,
			dp->domain,
			(dp->ipv6) ? '6' : '4',
			dp->port,
			(dp->secure) ? "secure" : "open");
	dbg_printf ("    clients:\r\n");
	LIST_FOREACH (dp->clients, cp)
		dump_client (cp);
	dbg_printf ("    readers:\r\n");
	LIST_FOREACH (dp->readers, rp)
		dump_reader (rp, 1);
}

void bgns_dump (void)
{
	BGNS_DATA	*sp;

	if (!initialized)
		return;

	LIST_FOREACH (servers, sp)
		dump_server (sp);

	bgcp_dump ();
}

#endif
#endif

