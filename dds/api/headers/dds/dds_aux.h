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

/* dds_aux.h -- Defines auxiliary DDS functionality that can be used. */

#ifndef __dds_aux_h_
#define __dds_aux_h_

#include <stdio.h>
#include <stdint.h>

#include "dds/dds_dcps.h"

#ifdef  __cplusplus
extern "C" {
#endif

/* === Version Info ========================================================= */

#define	TDDS_VERSION_MAJOR	4	/* Major version -> major API change. */
#define	TDDS_VERSION_MINOR	5	/* Minor version -> minor API change. */
#define	TDDS_VERSION_REVISION	0	/* Revision -> updated when released. */

#define	TDDS_VERSION	((TDDS_VERSION_MAJOR << 24) | \
			 (TDDS_VERSION_MINOR << 16) | TDDS_VERSION_REVISION)

/* === Standard DDS initialisation ========================================== */

typedef enum {
	DDS_EE_C,		/* C Environment. */
	DDS_EE_CPP,		/* C++ Environment. */
	DDS_EE_JAVA,		/* Java Environment. */
	DDS_EE_CSHARP,		/* C# Environment. */
	DDS_EE_PYTHON,		/* Python Environment. */
	DDS_EE_CDD = 0xcd	/* Central Discovery Environment. */
} DDS_ExecEnv_t;

/* Set the DDS Execution environment.  Default if not set is C. */
DDS_EXPORT DDS_ReturnCode_t DDS_execution_environment (
	DDS_ExecEnv_t eid
);

typedef struct {
	unsigned max_domains;
	unsigned min_subscribers;
	unsigned max_subscribers;
	unsigned min_publishers;
	unsigned max_publishers;
	unsigned min_local_readers;
	unsigned max_local_readers;
	unsigned min_local_writers;
	unsigned max_local_writers;
	unsigned min_topics;
	unsigned max_topics;
	unsigned min_filtered_topics;
	unsigned max_filtered_topics;
	unsigned min_topic_types;
	unsigned max_topic_types;
	unsigned min_reader_proxies;
	unsigned max_reader_proxies;
	unsigned min_writer_proxies;
	unsigned max_writer_proxies;
	unsigned min_remote_participants;
	unsigned max_remote_participants;
	unsigned min_remote_readers;
	unsigned max_remote_readers;
	unsigned min_remote_writers;
	unsigned max_remote_writers;
	unsigned min_pool_data;
	unsigned max_pool_data;
	unsigned max_rx_buffers;
	unsigned min_changes;
	unsigned max_changes;
	unsigned min_instances;
	unsigned max_instances;
	unsigned min_application_samples;
	unsigned max_application_samples;
	unsigned min_local_match;
	unsigned max_local_match;
	unsigned min_cache_waiters;
	unsigned max_cache_waiters;
	unsigned min_cache_transfers;
	unsigned max_cache_transfers;
	unsigned min_time_filters;
	unsigned max_time_filters;
	unsigned min_time_instances;
	unsigned max_time_instances;
	unsigned min_strings;
	unsigned max_strings;
	unsigned min_string_data;
	unsigned max_string_data;
	unsigned min_locators;
	unsigned max_locators;
	unsigned min_qos;
	unsigned max_qos;
	unsigned min_lists;
	unsigned max_lists;
	unsigned min_list_nodes;
	unsigned max_list_nodes;
	unsigned min_timers;
	unsigned max_timers;
	unsigned max_ip_sockets;
	unsigned max_ipv4_addresses;
	unsigned max_ipv6_addresses;
	unsigned min_waitsets;
	unsigned max_waitsets;
	unsigned min_statusconditions;
	unsigned max_statusconditions;
	unsigned min_readconditions;
	unsigned max_readconditions;
	unsigned min_queryconditions;
	unsigned max_queryconditions;
	unsigned min_guardconditions;
	unsigned max_guardconditions;
	unsigned min_notifications;
	unsigned max_notifications;
	unsigned min_topicwaits;
	unsigned max_topicwaits;
	unsigned min_guards;
	unsigned max_guards;
	unsigned min_dyn_types;
	unsigned max_dyn_types;
	unsigned min_dyn_samples;
	unsigned max_dyn_samples;
	unsigned min_data_holders;
	unsigned max_data_holders;
	unsigned min_properties;
	unsigned max_properties;
	unsigned min_binary_properties;
	unsigned max_binary_properties;
	unsigned min_holder_sequences;
	unsigned max_holder_sequences;
	unsigned grow_factor;
} DDS_PoolConstraints;


/* Get default pool constraint parameters.
   The max_factor parameter gives the relation between the minimum and maximum
   numbers as percentage of extra items compared to the mimimum quantity.
   (0:max=min, 100:max=2*min, 200:max=3*min, etc.).  The grow_factor parameter
   specifies how extra allocated items should be handled when no longer needed.
   It is a percentage of the number of extra items that must not be returned to
   the system. */
DDS_EXPORT DDS_ReturnCode_t DDS_get_default_pool_constraints (
	DDS_PoolConstraints *pars,
	unsigned max_factor,
	unsigned grow_factor
);

/* Update the pool parameters for the calling program in order to optimize
   memory usage.  Note that this function is not required, although memory
   *can* typically be optimized, based on usage patterns. */
DDS_EXPORT DDS_ReturnCode_t DDS_set_pool_constraints (
	const DDS_PoolConstraints *pars
);

/* Control the activity of the RTPS/Discovery layers. */
DDS_EXPORT void DDS_RTPS_control (
	int enable
);

/* Process the program name and arguments for DDS-specific purposes. */
DDS_EXPORT void DDS_program_name (
	int *argc,
	char *argv []
);

/* Set the announced entity name. */
DDS_EXPORT void DDS_entity_name (
	const char *name
);

/* Get the *_delete_contained_entities() delay in microseconds.
   Default setting is 50ms. */
DDS_EXPORT unsigned DDS_get_purge_delay (void);

/* Set the *_delete_contained_entities() delay in microseconds. */
DDS_EXPORT void DDS_set_purge_delay (
	unsigned us
);

/* Set the configuration filename for DDS parameters. */
DDS_EXPORT void DDS_configuration_set (
	const char *filename
);

/* Update/set one of the DDS parameters. */
DDS_EXPORT DDS_ReturnCode_t DDS_parameter_set (
	const char *name,
	const char *value
);

/* Unset one of the DDS parameters. */
DDS_EXPORT DDS_ReturnCode_t DDS_parameter_unset (
	const char *name
);

/* Retrieve one of the DDS parameters. */
DDS_EXPORT const char *DDS_parameter_get (
	const char *name,
	char buffer [],
	size_t size
);


/* === DDS shutdown ========================================================= */

typedef void (*DDS_exit_cb) (void);

/* Set a callback that will be called from the DDS core thread when it shuts
   down */
DDS_EXPORT void DDS_atexit (
	DDS_exit_cb cb
);


/* === DDS dynamic memory usage ============================================= */

/* Define the pool allocation primitives.
   If this function is not used, the standard C allocation functions will be
   called (e.g. malloc, realloc and free).
   Note: this function must be used before calling any other DDS function! */
DDS_EXPORT DDS_ReturnCode_t DDS_alloc_fcts_set (
	void *(*pmalloc) (size_t size),
	void *(*prealloc) (void *ptr, size_t size),
	void (*pfree) (void *ptr)
);


/* === DDS processing ======================================================= */

/* Process DDS events for at the most the specified duration.
   If all events were processed, the function returns, even though the
   duration might be longer. */
DDS_EXPORT void DDS_schedule (
	unsigned ms
);

/* Process DDS events for at least the specified duration. */
DDS_EXPORT void DDS_wait (
	unsigned ms
);

/* Ensures that DDS_wait() returns immediately i.o. waiting for the complete
   time interval. */
DDS_EXPORT void DDS_continue (void);


/* === DDS timers =========================================================== */

typedef struct DDS_Timer_st *DDS_Timer;

DDS_EXPORT DDS_Timer DDS_Timer_create (
	const char *name
);

DDS_EXPORT DDS_ReturnCode_t DDS_Timer_start (
	DDS_Timer self,
	unsigned ms,
	uintptr_t user,
	void (*fct) (uintptr_t)
);

DDS_EXPORT DDS_ReturnCode_t DDS_Timer_stop (
	DDS_Timer self
);

DDS_EXPORT DDS_ReturnCode_t DDS_Timer_delete (
	DDS_Timer self
);


/* === DDS Input eventing =================================================== */

#ifndef _WIN32
#ifdef HANDLE
#undef HANDLE
#endif
#define HANDLE int
#else
#include "win.h"
#endif

DDS_EXPORT DDS_ReturnCode_t DDS_Handle_attach (
	HANDLE handle,
	short poll_events,
	void (*fct) (HANDLE fd, short events, void *user_data),
	void *user_data
);

DDS_EXPORT void DDS_Handle_detach (
	HANDLE handle
);


/* === DDS activities management =============================================*/

typedef enum {
	DDS_TIMER_ACTIVITY	= (0x00000001UL << 0),
	DDS_UDP_ACTIVITY	= (0x00000001UL << 1),
	DDS_TCP_ACTIVITY	= (0x00000001UL << 2),
	DDS_DEBUG_ACTIVITY	= (0x00000001UL << 3)
} DDS_Activity;

#define	DDS_ALL_ACTIVITY	0x0000000fU

DDS_EXPORT void DDS_Activities_suspend (
	DDS_Activity activities
);

DDS_EXPORT void DDS_Activities_resume (
	DDS_Activity activities
);

DDS_EXPORT DDS_ReturnCode_t DDS_Activities_notify (
	DDS_DomainId_t domain_id,
	const char *topic_match,
	const char *type_match
);

DDS_EXPORT void DDS_Activities_unnotify (
	DDS_DomainId_t domain_id,
	const char *topic_match,
	const char *type_match
);

typedef void (*DDS_Activities_on_wakeup) (
	const char *topic_name,
	const char *type_name,
	unsigned char id [12]
);

typedef void (*DDS_Activities_on_connected) (
	int fd,
	int connected
);

DDS_EXPORT void DDS_Activities_register (
	DDS_Activities_on_wakeup wakeup_fct,
	DDS_Activities_on_connected connect_fct
);

typedef enum {
	DDS_ACTIVITIES_CLIENT_ACTIVE,
	DDS_ACTIVITIES_CLIENT_SLEEPING,
	DDS_ACTIVITIES_CLIENT_DIED
} DDS_ActivitiesClientState;

typedef void (*DDS_Activities_on_client_change) (
	DDS_DomainId_t domain_id,
	DDS_BuiltinTopicKey_t *client_key,
	DDS_ActivitiesClientState state
);

DDS_EXPORT void DDS_Activities_client_info (
	DDS_Activities_on_client_change client_fct
);


/* === DDS discovery notifications ===========================================*/

/* Notification types: */

/* Masks for creation/logging: */
#define	DDS_NOTIFY_PARTICIPANT	1
#define	DDS_NOTIFY_TOPIC	2
#define	DDS_NOTIFY_PUBLICATION	4
#define	DDS_NOTIFY_SUBSCRIPTION	8

#define	DDS_NOTIFY_LOCAL(m)	((m) << 4)
#define	DDS_NOTIFY_REMOTE(m)	(m)

#define	DDS_NOTIFY_ALL_LOCAL	(0xf0)
#define	DDS_NOTIFY_ALL_REMOTE	(0x0f)
#define	DDS_NOTIFY_ALL		(0xff)

/* Participant changes notification callback function: */
typedef void (* DDS_Participant_notify)  (
	DDS_BuiltinTopicKey_t *key,
	DDS_ParticipantBuiltinTopicData *data,
	DDS_SampleInfo *info,
	uintptr_t user
);

/* Topic changes notification callback function: */
typedef void (* DDS_Topic_notify) (
	DDS_BuiltinTopicKey_t *key,
	DDS_TopicBuiltinTopicData *data,
	DDS_SampleInfo *info,
	uintptr_t user
);

/* Publication changes notification callback function. */
typedef void (* DDS_Publication_notify) (
	DDS_BuiltinTopicKey_t *key,
	DDS_PublicationBuiltinTopicData *data,
	DDS_SampleInfo *info,
	uintptr_t user
);

/* Subscription changes notification callback function. */
typedef void (* DDS_Subscription_notify) (
	DDS_BuiltinTopicKey_t *key,
	DDS_SubscriptionBuiltinTopicData *data,
	DDS_SampleInfo *info,
	uintptr_t user
);

/* Domain close notification callback function. */
typedef void (* DDS_Domain_close_notify) (
	uintptr_t user
);

/* Attach the builtin topic readers (mask) of the given participant (p) such
   that all activated readers will immediately start calling the given functions
   (if non-NULL) with the given user parameter (user).
   If successful, a non-0 handle is returned that should be used in
   DDS_Notification_detach () later in order cleanup properly.
   Contrary to the normal (builtin) DDS Discovery readers, this function may be
   called repeatedly so that every registered entity can get its own requested
   info. Also, both local and remote entities can be specified. */
DDS_EXPORT unsigned DDS_Notification_attach (
	DDS_DomainParticipant p,
	unsigned mask,
	DDS_Participant_notify pnf,
	DDS_Topic_notify tnf,
	DDS_Publication_notify wnf,
	DDS_Subscription_notify rnf,
	DDS_Domain_close_notify cnf,
	uintptr_t user,
	DDS_ReturnCode_t *ret
);

/* Detach a client from the builtin topic readers: */
DDS_EXPORT void DDS_Notification_detach (
	unsigned handle
);

/* Specification of the logging parameters.  Received discovery information
   will be logged to the given file for the indicated readers in the mask.
   If p == NULL, all new participants (existing as well as new ones) will get
   the logging parameters, otherwise only the given participant, if it exists,
   will get them. */
DDS_EXPORT void DDS_Notification_log (
	DDS_DomainParticipant p,
	FILE *f,
	unsigned mask
);

#ifdef  __cplusplus
}
#endif

#endif /* !__dds_aux_h_ */

