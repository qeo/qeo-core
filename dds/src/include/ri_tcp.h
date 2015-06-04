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

/* ri_tcp.h -- Interface to the RTPS over TCP/IPv4 and over TCP/IPv6 protocol.*/

#ifndef __ri_tcp_h_
#define __ri_tcp_h_

#include "ri_data.h"

#define	TCP_MAX_CLIENTS		48	/* Max. # of TCP clients. */

extern int		tcp_available;	/* Always exists: status of TCP. */
extern unsigned long	tcp_qdropped;	/* TCP queue entries discarded. */

/* - Endian and swapping - */

/* Send over the wire in little endian */
#if ENDIAN_CPU == ENDIAN_LITTLE /* LITTLE_ENDIAN --> no swapping */
/* TCP TO HOST */
#define TTOHS(x) x
#define TTOHL(x) x
/* HOST TO TCP */
#else /* BIG_ENDIAN --> swap */
/* TCP TO HOST */
#define TTOHS(x) ({uint16_t y = x, z = x; memcswap16(&z,&y); z;})
#define TTOHL(x) ({uint32_t y = x, z = x; memcswap32(&z,&y); z;})
/* HOST TO TCP */
#endif
#define HTOTS TTOHS
#define HTOTL TTOHL

/* Following variables/functions are only available if -DDDS_TCP */

extern RTPS_TCP_PARS	tcp_v4_pars;
#ifdef DDS_IPV6
extern RTPS_TCP_PARS	tcp_v6_pars;
#endif
extern IP_CX		*tcpv4_server;
#ifdef DDS_IPV6
extern IP_CX		*tcpv6_server;
#endif
extern IP_CX		*tcp_client [TCP_MAX_CLIENTS];

int rtps_tcpv4_attach (void);

/* Attach the TCPv4 protocol in order to send RTPS over TCPv4 messages. */

void rtps_tcpv4_detach (int suspend);

/* Detach the previously attached TCPv4 protocol. */


int rtps_tcpv6_attach (void);

/* Attach the TCPv6 protocol for sending RTPS over TCPv6 messages. */

void rtps_tcpv6_detach (int suspend);

/* Detach the previously attached TCPv6 protocol. */


void rtps_tcp_send (unsigned id, Locator_t *lp, LocatorList_t *next, RMBUF *msgs);

/* Send the given messages (msgs) on the TCP locator (lp). The locator list
   (next) gives more destinations to send to after the first if non-NULL.
   If there are no more destinations, and the messages are no longer needed,
   rtps_free_messages () should be called. */

void rtps_tcp_cleanup_cx (IP_CX *cxp);

/* Release all resources related to the given connection (and potential paired
   and client connections. */

int rtps_tcp_peer (unsigned     handle,
		   DomainId_t   domain_id,
		   unsigned     pid,
		   GuidPrefix_t *prefix);

/* Get the neighbour GUID prefix for the given domain id and participant id. */

LocatorList_t rtps_tcp_secure_servers (LocatorList_t uclocs);

/* Return all secure TCP server addresses private and/or public, based on the
   given unicast locators. */

void rtps_tcp_add_mcast_locator (Domain_t *dp);

/* Add the predefined TCP Meta Multicast locator. */

void rtps_tcp_addr_update_start (unsigned family);

/* Start updating addresses for the given address family. */

void rtps_tcp_addr_update_end (unsigned family);

/* Done updating addresses for the given address family. */

/* Protocol header sequence for control messages: */
#define	ctrl_protocol_valid(p)	!memcmp (p, rpsc_protocol_id, sizeof (ProtocolId_t))
#define	bgcp_protocol_valid(p)	!memcmp (p, bgcp_protocol_id, sizeof (ProtocolId_t))

extern ProtocolId_t	rpsc_protocol_id;
#ifdef TCP_SUSPEND
extern ProtocolId_t	bgcp_protocol_id;
#endif


/* Pending connect() serialization data structures and utility functions.
   ---------------------------------------------------------------------- */

typedef struct tcp_con_list_st TCP_CON_LIST_ST;
typedef struct tcp_con_req_st TCP_CON_REQ_ST;

typedef int (* TCP_CON_FCT) (TCP_CON_REQ_ST *req);

struct tcp_con_list_st {
	Locator_t	locator;
	TCP_CON_FCT	fct;
	TCP_CON_REQ_ST	*reqs;
	TCP_CON_LIST_ST	*next;
};

struct tcp_con_req_st {
	TCP_CON_LIST_ST	*head;
	IP_CX		*cxp;
	TCP_CON_REQ_ST	*next;
};

int tcp_connect_enqueue (IP_CX *cxp, unsigned port, TCP_CON_FCT fct);

/* Request a new [TLS/]TCP connection setup to a given destination. */

TCP_CON_REQ_ST *tcp_clear_pending_connect (TCP_CON_REQ_ST *p);

/* Remove the current pending connection and return the next. */

TCP_CON_REQ_ST *tcp_pending_connect_remove (IP_CX *cxp);

/* Remove a pending connection. */


/* Suspend/resume notification support.
   ------------------------------------ */

typedef enum {
	SRN_OPEN,	/* Control channel is established. */
	SRN_CLOSED,	/* Control channel is closed. */
	SRN_DATA	/* A message was received and should be processed. */
} TCP_SR_EVENT;

typedef void (*TCP_SR_IND) (uintptr_t           user,
			    TCP_SR_EVENT        event,
			    IP_CX               *cx,
			    const unsigned char *msg,
			    size_t              length);

/* Callback function for processing control channel events. */

int rtps_tcp_srn_attach (int server, TCP_SR_IND fct, uintptr_t user);

/* Attach to the existing TCP control channels for suspend/resume notifications.
   The callback function will be used to notify incoming data. */

void rtps_tcp_srn_detach (int server);

/* Detach from an existing TCP control channel. */

#define	TCP_SEND_OK	0
#define	TCP_SEND_ERR	1
#define	TCP_SEND_BUSY	2

int rtps_tcp_srn_send (IP_CX *cx, const unsigned char *msg, size_t length);

/* Send a suspend/resume notification message. */

void rtps_tcp_srn_suspend (IP_CX *cx, int enabled);

/* Signal the status of the control connection. */

#endif /* !__ri_tcp_h_ */

