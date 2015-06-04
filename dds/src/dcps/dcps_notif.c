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

/* dcps_notif.c -- Implements built-in Discovery data readers that can optionally
                   dump received data automatically, and can also callback to the
		   user each time events occur. */

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "error.h"
#include "libx.h"
#include "pool.h"
#include "guid.h"
#include "dcps_notif.h"

#define	NBUILTINS	4

static const char	*names [NBUILTINS] = {
	"DCPSParticipant",
	"DCPSTopic",
	"DCPSPublication",
	"DCPSSubscription"
};

typedef struct bi_client_st BI_CLIENT;
struct bi_client_st {
	unsigned		handle;		/* Reference handle. */
	unsigned		m;		/* Notifications mask. */
	uintptr_t		user;		/* User parameter. */
	DDS_Participant_notify	pnotify;	/* Participant change fct. */
	DDS_Topic_notify	tnotify;	/* Topic change fct. */
	DDS_Publication_notify	wnotify;	/* Publication change fct. */
	DDS_Subscription_notify	rnotify;	/* Subscription change fct. */
	DDS_Domain_close_notify	cnotify;	/* Domain closed fct. */
	BI_CLIENT		*next;		/* Next in client list. */
};

typedef struct bi_participant_st BI_PARTICIPANT;
struct bi_participant_st {
	DDS_DomainParticipant	part;		/* Participant. */
	DDS_Subscriber		sub;		/* Builtin subscriber. */
	BI_CLIENT		*clients;	/* List of clients. */
	unsigned		users [NBUILTINS]; /* Per type usage counts. */
	BI_PARTICIPANT		*next;		/* Next in participant list. */
};

static BI_PARTICIPANT	*participants;	/* Discovery participants. */
static unsigned		last_handle;	/* Unique handle per user. */

static void participant_info (DDS_DataReaderListener *l,
			      DDS_DataReader         dr)
{
	static DDS_DataSeq	rx_sample = DDS_SEQ_INITIALIZER (void *);
	static DDS_SampleInfoSeq rx_info = DDS_SEQ_INITIALIZER (DDS_SampleInfo *);
	DDS_SampleStateMask	ss = DDS_NOT_READ_SAMPLE_STATE;
	DDS_ViewStateMask	vs = DDS_ANY_VIEW_STATE;
	DDS_InstanceStateMask	is = DDS_ANY_INSTANCE_STATE;
	DDS_SampleInfo		*info;
	DDS_ParticipantBuiltinTopicData tmp;
	DDS_ParticipantBuiltinTopicData *sample;
	DDS_ReturnCode_t	error;
	DDS_BuiltinTopicKey_t	*key;
	BI_PARTICIPANT		*p = (BI_PARTICIPANT *) l->cookie;
	BI_CLIENT		*cp;
	unsigned		i;

	for (;;) {
		error = DDS_DataReader_take (dr, &rx_sample, &rx_info, 16, ss, vs, is);
		if (error) {
			if (error != DDS_RETCODE_NO_DATA)
				fprintf (stderr, "Unable to read Discovered Participant samples: error = %u!\r\n", error);
			return;
		}
		for (i = 0; i < DDS_SEQ_LENGTH (rx_info); i++) {
			sample = DDS_SEQ_ITEM (rx_sample, i);
			info = DDS_SEQ_ITEM (rx_info, i);
			if (info->valid_data)
				key = &sample->key;
			else {
				memset (&tmp.key, 0, sizeof (tmp.key));
				DDS_DataReader_get_key_value (dr, &tmp, info->instance_handle);
				key = &tmp.key;
				sample = NULL;
			}
			for (cp = p->clients; cp; cp = cp->next)
				if (cp->pnotify)
					(*cp->pnotify) (key, sample, info, cp->user);
		}
		DDS_DataReader_return_loan (dr, &rx_sample, &rx_info);
	}
}

static void topic_info (DDS_DataReaderListener *l,
		        DDS_DataReader         dr)
{
	static DDS_DataSeq	rx_sample = DDS_SEQ_INITIALIZER (void *);
	static DDS_SampleInfoSeq rx_info = DDS_SEQ_INITIALIZER (DDS_SampleInfo *);
	DDS_SampleStateMask	ss = DDS_NOT_READ_SAMPLE_STATE;
	DDS_ViewStateMask	vs = DDS_ANY_VIEW_STATE;
	DDS_InstanceStateMask	is = DDS_ANY_INSTANCE_STATE;
	DDS_SampleInfo		*info;
	DDS_TopicBuiltinTopicData tmp;
	DDS_TopicBuiltinTopicData *sample;
	DDS_BuiltinTopicKey_t	*key;
	BI_PARTICIPANT		*p = (BI_PARTICIPANT *) l->cookie;
	BI_CLIENT		*cp;
	DDS_ReturnCode_t	error;
	unsigned		i;

	for (;;) {
		error = DDS_DataReader_take (dr, &rx_sample, &rx_info, 16, ss, vs, is);
		if (error) {
			if (error != DDS_RETCODE_NO_DATA)
				printf ("Unable to read Discovered Topic samples: error = %u!\r\n", error);
			return;
		}
		for (i = 0; i < DDS_SEQ_LENGTH (rx_info); i++) {
			sample = DDS_SEQ_ITEM (rx_sample, i);
			info = DDS_SEQ_ITEM (rx_info, i);
			if (info->valid_data)
				key = &sample->key;
			else {
				DDS_DataReader_get_key_value (dr, &tmp, info->instance_handle);
				key = &tmp.key;
				sample = NULL;
			}
			for (cp = p->clients; cp; cp = cp->next)
				if (cp->tnotify)
					(*cp->tnotify) (key, sample, info, cp->user);
		}
		DDS_DataReader_return_loan (dr, &rx_sample, &rx_info);
	}
}

static void publication_info (DDS_DataReaderListener *l,
			      DDS_DataReader         dr)
{
	static DDS_DataSeq	rx_sample = DDS_SEQ_INITIALIZER (void *);
	static DDS_SampleInfoSeq rx_info = DDS_SEQ_INITIALIZER (DDS_SampleInfo *);
	DDS_SampleStateMask	ss = DDS_NOT_READ_SAMPLE_STATE;
	DDS_ViewStateMask	vs = DDS_ANY_VIEW_STATE;
	DDS_InstanceStateMask	is = DDS_ANY_INSTANCE_STATE;
	DDS_SampleInfo		*info;
	DDS_PublicationBuiltinTopicData tmp;
	DDS_PublicationBuiltinTopicData *sample;
	DDS_ReturnCode_t	error;
	DDS_BuiltinTopicKey_t	*key;
	BI_PARTICIPANT		*p = (BI_PARTICIPANT *) l->cookie;
	BI_CLIENT		*cp;
	unsigned		i;

	for (;;) {
		error = DDS_DataReader_take (dr, &rx_sample, &rx_info, 16, ss, vs, is);
		if (error) {
			if (error != DDS_RETCODE_NO_DATA)
				fprintf (stderr, "Unable to read Discovered Publication samples: error = %u!\r\n", error);
			return;
		}
		for (i = 0; i < DDS_SEQ_LENGTH (rx_info); i++) {
			sample = DDS_SEQ_ITEM (rx_sample, i);
			info = DDS_SEQ_ITEM (rx_info, i);
			if (info->valid_data)
				key = &sample->key;
			else {
				DDS_DataReader_get_key_value (dr, &tmp, info->instance_handle);
				key = &tmp.key;
				sample = NULL;
			}
			for (cp = p->clients; cp; cp = cp->next)
				if (cp->wnotify)
					(*cp->wnotify) (key, sample, info, cp->user);
		}
		DDS_DataReader_return_loan (dr, &rx_sample, &rx_info);
	}
}

static void subscription_info (DDS_DataReaderListener *l,
		               DDS_DataReader         dr)
{
	static DDS_DataSeq	rx_sample = DDS_SEQ_INITIALIZER (void *);
	static DDS_SampleInfoSeq rx_info = DDS_SEQ_INITIALIZER (DDS_SampleInfo *);
	DDS_SampleStateMask	ss = DDS_NOT_READ_SAMPLE_STATE;
	DDS_ViewStateMask	vs = DDS_ANY_VIEW_STATE;
	DDS_InstanceStateMask	is = DDS_ANY_INSTANCE_STATE;
	DDS_SampleInfo		*info;
	DDS_SubscriptionBuiltinTopicData tmp;
	DDS_SubscriptionBuiltinTopicData *sample;
	DDS_ReturnCode_t	error;
	DDS_BuiltinTopicKey_t	*key;
	BI_PARTICIPANT		*p = (BI_PARTICIPANT *) l->cookie;
	BI_CLIENT		*cp;
	unsigned		i;

	for (;;) {
		error = DDS_DataReader_take (dr, &rx_sample, &rx_info, 1, ss, vs, is);
		if (error) {
			if (error != DDS_RETCODE_NO_DATA)
				fprintf (stderr, "Unable to read Discovered Subscription samples: error = %u!\r\n", error);
			return;
		}
		for (i = 0; i < DDS_SEQ_LENGTH (rx_info); i++) {
			sample = DDS_SEQ_ITEM (rx_sample, i);
			info = DDS_SEQ_ITEM (rx_info, i);
			if (info->valid_data)
				key = &sample->key;
			else {
				DDS_DataReader_get_key_value (dr, &tmp, info->instance_handle);
				key = &tmp.key;
				sample = NULL;
			}
			for (cp = p->clients; cp; cp = cp->next)
				if (cp->rnotify)
					(*cp->rnotify) (key, sample, info, cp->user);
		}
		DDS_DataReader_return_loan (dr, &rx_sample, &rx_info);
	}
}

static DDS_DataReaderListener builtin_listeners [] = {{
	NULL,			/* Sample rejected. */
	NULL,			/* Liveliness changed. */
	NULL,			/* Requested Deadline missed. */
	NULL,			/* Requested incompatible QoS. */
	participant_info,	/* Data available. */
	NULL,			/* Subscription matched. */
	NULL,			/* Sample lost. */
	NULL			/* Cookie */
}, {
	NULL,			/* Sample rejected. */
	NULL,			/* Liveliness changed. */
	NULL,			/* Requested Deadline missed. */
	NULL,			/* Requested incompatible QoS. */
	topic_info,		/* Data available. */
	NULL,			/* Subscription matched. */
	NULL,			/* Sample lost. */
	NULL			/* Cookie */
}, {
	NULL,			/* Sample rejected. */
	NULL,			/* Liveliness changed. */
	NULL,			/* Requested Deadline missed. */
	NULL,			/* Requested incompatible QoS. */
	publication_info,	/* Data available. */
	NULL,			/* Subscription matched. */
	NULL,			/* Sample lost. */
	NULL			/* Cookie */
}, {
	NULL,			/* Sample rejected. */
	NULL,			/* Liveliness changed. */
	NULL,			/* Requested Deadline missed. */
	NULL,			/* Requested incompatible QoS. */
	subscription_info,	/* Data available. */
	NULL,			/* Subscription matched. */
	NULL,			/* Sample lost. */
	NULL			/* Cookie */
}};

static DDS_ReturnCode_t dcps_notif_register (BI_PARTICIPANT         *p,
					     const char             *name,
					     DDS_DataReaderListener *l)
{
	DDS_DataReader		dr;
	DDS_ReturnCode_t	ret;

	dr = DDS_Subscriber_lookup_datareader (p->sub, name);
	if (!dr) {
		fprintf (stderr, "DDS_Subscriber_lookup_datareader returned an error!");
		return (DDS_RETCODE_OUT_OF_RESOURCES);
	}
	l->cookie = p;
	ret = DDS_DataReader_set_listener (dr, l, DDS_DATA_AVAILABLE_STATUS);
	if (ret)
		fprintf (stderr, "DDS_DataReader_set_listener returned an error (%s)!", 
							DDS_error (ret));

	return (ret);
}

unsigned DDS_Notification_attach (DDS_DomainParticipant   part,
				  unsigned                m,
				  DDS_Participant_notify  pnf,
				  DDS_Topic_notify        tnf,
				  DDS_Publication_notify  wnf,
				  DDS_Subscription_notify rnf,
				  DDS_Domain_close_notify cnf,
				  uintptr_t               u,
				  DDS_ReturnCode_t        *ret)
{
	BI_PARTICIPANT	*p;
	BI_CLIENT	*cp;
	unsigned	i;

	for (p = participants; p; p = p->next)
		if (p->part == part)
			break;

	if (!p) {
		p = xmalloc (sizeof (BI_PARTICIPANT));
		if (!p) {
			*ret = DDS_RETCODE_OUT_OF_RESOURCES;
			return (0);
		}
		p->part = part;
		p->sub = DDS_DomainParticipant_get_builtin_subscriber (part);
		if (!p->sub) {
			fprintf (stderr, "DDS_DomainParticipant_get_builtin_subscriber() returned an error!");
			*ret = DDS_RETCODE_OUT_OF_RESOURCES;
			xfree (p);
			return (0);
		}
		p->clients = NULL;
		memset (p->users, 0, sizeof (p->users));
		p->next = participants;
		participants = p;
	}
	cp = xmalloc (sizeof (BI_CLIENT));
	if (!cp) {
		if (!p->clients) {
			participants = p->next;
			xfree (p);
		}
		*ret = DDS_RETCODE_OUT_OF_RESOURCES;
		return (0);
	}
	cp->handle = ++last_handle;
	cp->m = 0;
	cp->user = u;
	cp->pnotify = pnf;
	cp->tnotify = tnf;
	cp->wnotify = wnf;
	cp->rnotify = rnf;
	cp->cnotify = cnf;
	cp->next = p->clients;
	p->clients = cp;

	for (i = 0; i < NBUILTINS; i++) {
		if (((1 << i) & m) != 0) {
			if (!p->users [i]++) {
				*ret = dcps_notif_register (p,
							    names [i],
							    &builtin_listeners [i]);
				if (*ret) {
					DDS_Notification_detach (cp->handle);
					return (0);
				}
			}
			cp->m |= 1 << i;
		}
	}
	*ret = DDS_RETCODE_OK;
	return (cp->handle);
}

void DDS_Notification_detach (unsigned h)
{
	BI_PARTICIPANT	*p, *prev_p;
	BI_CLIENT	*cp, *prev_cp;
	DDS_DataReader	dr;
	unsigned	i;

	if (!h || !participants)
		return;

	for (prev_p = NULL, p = participants; p; prev_p = p, p = p->next) {
		for (prev_cp = NULL, cp = p->clients;
		     cp;
		     prev_cp = cp, cp = cp->next)
			if (cp->handle == h)
				break;
		if (cp)
			break;
	}
	if (!cp)
		return;

	for (i = 0; i < NBUILTINS; i++)
		if (((i << i) & cp->m) != 0 && !--p->users [i]) {
			dr = DDS_Subscriber_lookup_datareader (p->sub, names [i]);
			DDS_DataReader_set_listener (dr, NULL, 0);
		}
	if (prev_cp)
		prev_cp->next = cp->next;
	else {
		p->clients = cp->next;
		if (!p->clients) {
			if (prev_p)
				prev_p->next = p->next;
			else
				participants = p->next;
			xfree (p);
		}
	}
	xfree (cp);
}

void dcps_notif_cleanup (DDS_DomainParticipant dp)
{
	BI_PARTICIPANT	*p;
	unsigned	h;

	do {
		for (h = 0, p = participants; p; p = p->next)
			if (p->part == dp) {
				if (p->clients->cnotify)
					(*p->clients->cnotify) (p->clients->user);
				h = p->clients->handle;
				DDS_Notification_detach (h);
				break;
			}
	}
	while (h);
}

