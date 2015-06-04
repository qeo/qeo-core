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

/* dd_log.c -- Utility library to log DDS discovery info to file. */

#include <string.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include "error.h"
#include "dd_log.h"

#define	RC_BUFSIZE	80

static FILE	*log_outf;		/* Output file. */
static unsigned	log_mask;		/* Logging mask. */

static const char *region_chunk_str (const void *p,
			             unsigned   length,
			             int        show_ofs,
			             const void *sp)
{
	char			ascii [17], *bp;
	static char		buf [RC_BUFSIZE];
	unsigned		i, left;
	const unsigned char	*dp = (const unsigned char *) p;
	unsigned char		c;

	bp = buf;
	left = RC_BUFSIZE;
	if (show_ofs)
		if (sp)
			snprintf (bp, left, "  %4ld: ", (long) (dp - (const unsigned char *) sp));
		else
			snprintf (bp, left, "  %p: ", p);
	else {
		buf [0] = '\t';
		buf [1] = '\0';
	}
	bp = &buf [strlen (buf)];
	left = RC_BUFSIZE - strlen (buf) - 1;
	for (i = 0; i < length; i++) {
		c = *dp++;
		ascii [i] = (c >= ' ' && c <= '~') ? c : '.';
		if (i == 8) {
			snprintf (bp, left, "- ");
			bp += 2;
			left -= 2;
		}
		snprintf (bp, left, "%02x ", c);
		bp += 3;
		left -= 3;
	}
	ascii [i] = '\0';
	while (i < 16) {
		if (i == 8) {
			snprintf (bp, left, "  ");
			bp += 2;
			left -= 2;
		}
		snprintf (bp, left, "   ");
		bp += 3;
		left -= 3;
		i++;
	}
	snprintf (bp, left, "  %s", ascii);
	return (buf);
}

static void print_region (FILE       *f,
			  const void *dstp,
			  unsigned   length,
			  int        show_addr,
			  int        ofs)
{
	unsigned		i;
	const unsigned char	*dp = (const unsigned char *) dstp;

	for (i = 0; i < length; i += 16) {
		fprintf (f, "%s\r\n", region_chunk_str (dp,
				(length < i + 16) ? length - i: 16, show_addr,
				(ofs) ? dstp : NULL));
		dp += 16;
	}
}

static void dump_key (FILE *f, DDS_BuiltinTopicKey_t *kp)
{
	fprintf (f, "%08x:%08x:%08x", ntohl (kp->value [0]), 
			ntohl (kp->value [1]), ntohl (kp->value [2]));
	if (sizeof (DDS_BuiltinTopicKey_t) > 12)
		fprintf (f, ":%08x", ntohl (kp->value [3]));
}

static void dump_user_data (FILE *f, DDS_OctetSeq *sp)
{
	unsigned	i;
	unsigned char	*p;

	if (!DDS_SEQ_LENGTH (*sp))
		fprintf (f, "<none>\r\n");
	else if (DDS_SEQ_LENGTH (*sp) < 10) {
		DDS_SEQ_FOREACH_ENTRY (*sp, i, p)
			fprintf (f, "%02x ", *p);
		fprintf (f, "\r\n");
	}
	else {
		fprintf (f, "\r\n");
		print_region (f, DDS_SEQ_ITEM_PTR (*sp, 0), DDS_SEQ_LENGTH (*sp), 0, 0);
	}
}

static void display_participant_info (FILE *f, DDS_ParticipantBuiltinTopicData *sample)
{
# if 0
	fprintf (f, "\tKey                = ");
	dump_key (f, &sample->key);
	fprintf (f, "\r\n");
# endif
	fprintf (f, "\tUser data          = ");
	dump_user_data (f, &sample->user_data.value);
}

static void participant_notify (DDS_BuiltinTopicKey_t           *key,
				DDS_ParticipantBuiltinTopicData *sample,
				DDS_SampleInfo                  *info,
				uintptr_t                       user)
{
	FILE	*f;

	ARG_NOT_USED (user)

	f = log_outf;
	fprintf (f, "* ");
	dump_key (f, key);
	fprintf (f, "  ");
	if ((info->view_state & DDS_NEW_VIEW_STATE) != 0)
		fprintf (f, "New");
	else if (info->instance_state == DDS_ALIVE_INSTANCE_STATE)
		fprintf (f, "Updated");
	else
		fprintf (f, "Deleted");
	fprintf (f, " Participant\r\n");
	if (info->valid_data)
		display_participant_info (f, sample);
}

static void dump_duration (FILE *f, DDS_Duration_t *dp)
{
	if (dp->sec == DDS_DURATION_ZERO_SEC &&
	    dp->nanosec == DDS_DURATION_ZERO_NSEC)
		fprintf (f, "0s");
	else if (dp->sec == DDS_DURATION_INFINITE_SEC &&
	         dp->nanosec == DDS_DURATION_INFINITE_NSEC)
		fprintf (f, "<infinite>");
	else
		fprintf (f, "%d.%09us", dp->sec, dp->nanosec);
}

static void dump_durability (FILE *f, DDS_DurabilityQosPolicy *dp)
{
	static const char *durability_str [] = {
		"Volatile", "Transient-local", "Transient", "Persistent"
	};

	if (dp->kind <= DDS_PERSISTENT_DURABILITY_QOS)
		fprintf (f, "%s", durability_str [dp->kind]);
	else
		fprintf (f, "?(%d)", dp->kind);
}

static void dump_history (FILE *f, DDS_HistoryQosPolicyKind k, int depth)
{
	if (k == DDS_KEEP_ALL_HISTORY_QOS)
		fprintf (f, "All");
	else
		fprintf (f, "Last %d", depth);
}

static void dump_resource_limits (FILE *f,
				  int max_samples,
				  int max_inst,
				  int max_samples_per_inst)
{
	fprintf (f, "max_samples/instances/samples_per_inst=%d/%d/%d",
			max_samples, max_inst, max_samples_per_inst);
}

static void dump_durability_service (FILE *f, DDS_DurabilityServiceQosPolicy *sp)
{
	fprintf (f, "\r\n\t     Cleanup Delay = ");
	dump_duration (f, &sp->service_cleanup_delay);
	fprintf (f, "\r\n\t     History       = ");
	dump_history (f, sp->history_kind, sp->history_depth);
	fprintf (f, "\r\n\t     Limits        = ");
	dump_resource_limits (f,
			      sp->max_samples,
			      sp->max_instances,
			      sp->max_samples_per_instance);
}

static void dump_liveliness (FILE *f, DDS_LivelinessQosPolicy *lp)
{
	static const char *liveness_str [] = {
		"Automatic", "Manual_by_Participant", "Manual_by_Topic"
	};

	if (lp->kind <= DDS_MANUAL_BY_TOPIC_LIVELINESS_QOS)
		fprintf (f, "%s", liveness_str [lp->kind]);
	else
		fprintf (f, "?(%d)", lp->kind);
	fprintf (f, ", Lease duration: ");
	dump_duration (f, &lp->lease_duration);
}

static void dump_reliability (FILE *f, DDS_ReliabilityQosPolicy *rp)
{
	if (rp->kind == DDS_BEST_EFFORT_RELIABILITY_QOS)
		fprintf (f, "Best-effort");
	else if (rp->kind == DDS_RELIABLE_RELIABILITY_QOS)
		fprintf (f, "Reliable");
	else
		fprintf (f, "?(%d)", rp->kind);
	fprintf (f, ", Max_blocking_time: ");
	dump_duration (f, &rp->max_blocking_time);
}

static void dump_destination_order (FILE *f, DDS_DestinationOrderQosPolicyKind k)
{
	if (k == DDS_BY_RECEPTION_TIMESTAMP_DESTINATIONORDER_QOS)
		fprintf (f, "Reception_Timestamp");
	else if (k == DDS_BY_SOURCE_TIMESTAMP_DESTINATIONORDER_QOS)
		fprintf (f, "Source_Timestamp");
	else
		fprintf (f, "?(%d)", k);
}

static void dump_ownership (FILE *f, DDS_OwnershipQosPolicyKind k)
{
	if (k == DDS_SHARED_OWNERSHIP_QOS)
		fprintf (f, "Shared");
	else if (k == DDS_EXCLUSIVE_OWNERSHIP_QOS)
		fprintf (f, "Exclusive");
	else
		fprintf (f, "?(%d)", k);
}

static void display_topic_info (FILE *f, DDS_TopicBuiltinTopicData *sample)
{
# if 0
	fprintf (f, "\tKey                = ");
	dump_key (f, &sample->key);
	fprintf (f, "\r\n\tName               = %s", sample->name);
	fprintf (f, "\r\n\tType Name          = %s", sample->type_name);
	fprintf (f, "\r\n");
# endif
	fprintf (f, "\tDurability         = ");
	dump_durability (f, &sample->durability);
	if (sample->durability.kind >= DDS_TRANSIENT_DURABILITY_QOS) {
		fprintf (f, "\r\n\tDurability Service:");
		dump_durability_service (f, &sample->durability_service);
	}
	fprintf (f, "\r\n\tDeadline           = ");
	dump_duration (f, &sample->deadline.period);
	fprintf (f, "\r\n\tLatency Budget     = ");
	dump_duration (f, &sample->latency_budget.duration);
	fprintf (f, "\r\n\tLiveliness         = ");
	dump_liveliness (f, &sample->liveliness);
	fprintf (f, "\r\n\tReliability        = ");
	dump_reliability (f, &sample->reliability);
	fprintf (f, "\r\n\tTransport Priority = %d", sample->transport_priority.value);
	fprintf (f, "\r\n\tLifespan           = ");
	dump_duration (f, &sample->lifespan.duration);
	fprintf (f, "\r\n\tDestination Order  = ");
	dump_destination_order (f, sample->destination_order.kind);
	fprintf (f, "\r\n\tHistory            = ");
	dump_history (f, sample->history.kind, sample->history.depth);
	fprintf (f, "\r\n\tResource Limits    = ");
	dump_resource_limits (f, sample->resource_limits.max_samples,
			      sample->resource_limits.max_instances,
			      sample->resource_limits.max_samples_per_instance);
	fprintf (f, "\r\n\tOwnership          = ");
	dump_ownership (f, sample->ownership.kind);
	fprintf (f, "\r\n\tTopic Data         = ");
	dump_user_data (f, &sample->topic_data.value);
}

static void topic_notify (DDS_BuiltinTopicKey_t     *key,
			  DDS_TopicBuiltinTopicData *sample,
			  DDS_SampleInfo            *info,
			  uintptr_t                 user)
{
	FILE	*f;

	ARG_NOT_USED (user)

	f = log_outf;
	fprintf (f, "* ");
	dump_key (f, key);
	fprintf (f, "  ");
	if ((info->view_state & DDS_NEW_VIEW_STATE) != 0)
		fprintf (f, "New");
	else if (info->instance_state == DDS_ALIVE_INSTANCE_STATE)
		fprintf (f, "Updated");
	else
		fprintf (f, "Deleted");
	fprintf (f, " Topic");
	if (info->valid_data)
		fprintf (f, " (%s/%s)", sample->name, sample->type_name);
	fprintf (f, "\r\n");
	if (info->valid_data)
		display_topic_info (f, sample);
}

static void dump_presentation (FILE *f, DDS_PresentationQosPolicy *pp)
{
	static const char *pres_str [] = {
		"Instance", "Topic", "Group"
	};

	fprintf (f, "Scope: ");
	if (pp->access_scope <= DDS_GROUP_PRESENTATION_QOS)
		fprintf (f, "%s", pres_str [pp->access_scope]);
	else
		fprintf (f, "?(%d)", pp->access_scope);
	fprintf (f, ", coherent: %d, ordered: %d", pp->coherent_access, pp->ordered_access);
}

static void dump_partition (FILE *f, DDS_PartitionQosPolicy *pp)
{
	unsigned	i;
	char		**cp;

	if (!DDS_SEQ_LENGTH (pp->name)) {
		fprintf (f, "<none>");
		return;
	}
	DDS_SEQ_FOREACH_ENTRY (pp->name, i, cp) {
		if (i)
			fprintf (f, ", ");
		fprintf (f, "%s", *cp);
	}
}

static void display_publication_info (FILE *f, DDS_PublicationBuiltinTopicData *sample)
{
# if 0
	fprintf (f, "\tKey                = ");
	dump_key (f, &sample->key);
	fprintf (f, "\r\n");
# endif
	fprintf (f, "\tParticipant Key    = ");
	dump_key (f, &sample->participant_key);
# if 0
	fprintf (f, "\r\n\tTopic Name         = %s", sample->topic_name);
	fprintf (f, "\r\n\tType Name          = %s", sample->type_name);
# endif
	fprintf (f, "\r\n\tDurability         = ");
	dump_durability (f, &sample->durability);
	if (sample->durability.kind >= DDS_TRANSIENT_DURABILITY_QOS) {
		fprintf (f, "\r\n\tDurability Service:");
		dump_durability_service (f, &sample->durability_service);
	}
	fprintf (f, "\r\n\tDeadline           = ");
	dump_duration (f, &sample->deadline.period);
	fprintf (f, "\r\n\tLatency Budget     = ");
	dump_duration (f, &sample->latency_budget.duration);
	fprintf (f, "\r\n\tLiveliness         = ");
	dump_liveliness (f, &sample->liveliness);
	fprintf (f, "\r\n\tReliability        = ");
	dump_reliability (f, &sample->reliability);
	fprintf (f, "\r\n\tLifespan           = ");
	dump_duration (f, &sample->lifespan.duration);
	fprintf (f, "\r\n\tUser Data          = ");
	dump_user_data (f, &sample->user_data.value);
	fprintf (f, "\tOwnership          = ");
	dump_ownership (f, sample->ownership.kind);
	fprintf (f, "\r\n\tOwnership strength = %d",
			sample->ownership_strength.value);
	fprintf (f, "\r\n\tDestination Order  = ");
	dump_destination_order (f, sample->destination_order.kind);
	fprintf (f, "\r\n\tPresentation       = ");
	dump_presentation (f, &sample->presentation);
	fprintf (f, "\r\n\tPartition          = ");
	dump_partition (f, &sample->partition);
	fprintf (f, "\r\n\tTopic Data         = ");
	dump_user_data (f, &sample->topic_data.value);
	fprintf (f, "\tGroup Data         = ");
	dump_user_data (f, &sample->group_data.value);
}

static void publication_notify (DDS_BuiltinTopicKey_t           *key,
				DDS_PublicationBuiltinTopicData *sample,
				DDS_SampleInfo                  *info,
				uintptr_t                       user)
{
	FILE	*f;

	ARG_NOT_USED (user)

	f = log_outf;
	fprintf (f, "* ");
	dump_key (f, key);
	fprintf (f, "  ");
	if ((info->view_state & DDS_NEW_VIEW_STATE) != 0)
		fprintf (f, "New");
	else if (info->instance_state == DDS_ALIVE_INSTANCE_STATE)
		fprintf (f, "Updated");
	else
		fprintf (f, "Deleted");
	fprintf (f, " Publication");
	if (info->valid_data)
		fprintf (f, " (%s/%s)", sample->topic_name, sample->type_name);
	fprintf (f, "\r\n");
	if (info->valid_data)
		display_publication_info (f, sample);
}

static void display_subscription_info (FILE *f, DDS_SubscriptionBuiltinTopicData *sample)
{
# if 0
	fprintf (f, "\tKey                = ");
	dump_key (f, &sample->key);
	fprintf (f, "\r\n");
# endif
	fprintf (f, "\tParticipant Key    = ");
	dump_key (f, &sample->participant_key);
# if 0
	fprintf (f, "\r\n\tTopic Name         = %s", sample->topic_name);
	fprintf (f, "\r\n\tType Name          = %s", sample->type_name);
# endif
	fprintf (f, "\r\n\tDurability         = ");
	dump_durability (f, &sample->durability);
	fprintf (f, "\r\n\tDeadline           = ");
	dump_duration (f, &sample->deadline.period);
	fprintf (f, "\r\n\tLatency Budget     = ");
	dump_duration (f, &sample->latency_budget.duration);
	fprintf (f, "\r\n\tLiveliness         = ");
	dump_liveliness (f, &sample->liveliness);
	fprintf (f, "\r\n\tReliability        = ");
	dump_reliability (f, &sample->reliability);
	fprintf (f, "\r\n\tOwnership          = ");
	dump_ownership (f, sample->ownership.kind);
	fprintf (f, "\r\n\tDestination Order  = ");
	dump_destination_order (f, sample->destination_order.kind);
	fprintf (f, "\r\n\tUser Data          = ");
	dump_user_data (f, &sample->user_data.value);
	fprintf (f, "\tTime based filter  = ");
	dump_duration (f, &sample->time_based_filter.minimum_separation);
	fprintf (f, "\r\n\tPresentation       = ");
	dump_presentation (f, &sample->presentation);
	fprintf (f, "\r\n\tPartition          = ");
	dump_partition (f, &sample->partition);
	fprintf (f, "\r\n\tTopic Data         = ");
	dump_user_data (f, &sample->topic_data.value);
	fprintf (f, "\tGroup Data         = ");
	dump_user_data (f, &sample->group_data.value);
}

static void subscription_notify (DDS_BuiltinTopicKey_t            *key,
				 DDS_SubscriptionBuiltinTopicData *sample,
				 DDS_SampleInfo                   *info,
				 uintptr_t                        user)
{
	FILE	*f;

	ARG_NOT_USED (user)

	f = log_outf;
	fprintf (f, "* ");
	dump_key (f, key);
	fprintf (f, "  ");
	if ((info->view_state & DDS_NEW_VIEW_STATE) != 0)
		fprintf (f, "New");
	else if (info->instance_state == DDS_ALIVE_INSTANCE_STATE)
		fprintf (f, "Updated");
	else
		fprintf (f, "Deleted");
	fprintf (f, " Subscription");
	if (info->valid_data)
		fprintf (f, " (%s/%s)", sample->topic_name, sample->type_name);
	fprintf (f, "\r\n");
	if (info->valid_data)
		display_subscription_info (f, sample);
}

unsigned discovery_log_enable (DDS_DomainParticipant part, FILE *f, unsigned mask)
{
	DDS_ReturnCode_t ret;
	unsigned	 h;

	log_outf = f;
	log_mask = mask;
	h = DDS_Notification_attach (part,
				     DDS_NOTIFY_ALL_REMOTE, 
				     participant_notify,
				     topic_notify,
				     publication_notify,
				     subscription_notify,
				     NULL,
				     0,
				     &ret);
	return (h);
}

void discovery_log_disable (unsigned handle)
{
	DDS_Notification_detach (handle);
}

