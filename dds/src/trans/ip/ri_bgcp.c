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

/* ri_bgcp.c -- Background notification service implementation. */

#include <stdio.h>
#include <errno.h>
#ifdef _WIN32
#include "win.h"
#include "Ws2IpDef.h"
#include "Ws2tcpip.h"
#include "Iphlpapi.h"
#define ERRNO	WSAGetLastError()
#else
#include <unistd.h>
#include <errno.h>
#include <poll.h>
#include <netinet/in.h>
#include <net/if.h>
#include <netdb.h>
#include <arpa/inet.h>
#include <sys/ioctl.h>
#define ERRNO errno
#endif
#include "sys.h"
#include "random.h"
#include "nmatch.h"
#include "log.h"
#include "error.h"
#include "list.h"
#include "table.h"
#include "debug.h"
#include "pool.h"
#include "pid.h"
#include "dds/dds_trans.h"
#include "dds.h"
#include "rtps_ip.h"
#include "ri_data.h"
#include "ri_tcp_sock.h"
#include "ri_tcp.h"
#ifdef DDS_SECURITY
#include "ri_tls.h"
#endif
#include "ri_bgcp.h"

#ifdef TCP_SUSPEND

#ifdef DDS_DEBUG
/*#define BGCP_TRC_CONTROL	** Trace/interpret control msgs if defined. */
/*#define BGCP_TRC_STATE	** Trace state changes if defined. */
/*#define BGCP_TRC_CX		** Trace connection setup/close if defined. */
#endif

#define	MAX_TX_BUF	1492

#define	CON_TO		(2 * TICKS_PER_SEC)
#define	CON_MAX_BACKOFF	4


/*************************************************/
/*   Background notification control protocol.	 */
/*************************************************/

#define	BREQ_TO			(2 * TICKS_PER_SEC)
#define	BREQ_RETRIES		3
#define	NREQ_TO			(2 * TICKS_PER_SEC)
#define	NREQ_RETRIES		3

#define	CMT_BIND_REQ		0x0c01	/* BindRequest (). */
#define	CMT_BIND_SUCC		0x0d01	/* BindSuccess (). */
#define	CMT_BIND_FAIL		0x0e01	/* BindFailure (). */

#define	CMT_NOTIFY_REQ		0x0c02	/* NotificationRequest (). */
#define	CMT_NOTIFY_SUCC		0x0d02	/* NotificationSuccess (). */
#define	CMT_NOTIFY_FAIL		0x0e02	/* NotificationFailure (). */

#define	CMT_SUSPENDING		0x0c03	/* Suspending (). */

#define	CMT_RESUMING		0x0c04	/* Resuming (). */

#define	CMT_WAKEUP		0x0c05	/* WakeUp (). */

#define	CMT_FINALIZE		0x0c0f	/* Finalize (). */

#define	MK_VENDOR_SPEC		0x8000

/* Following the control header are a number of variable length parameters
   encoded as a Parameter_t type (see pid.h). */

/* Following Parameter Ids are defined: */
#define	CAID_ERROR		0x0009	/* 4-byte error kind. */
#define	CAID_UNKN_ATTR		0x000a	/* Unknown attributes. */
#define	CAID_GUID_PREFIX	0x3d01	/* 12-byte GUID prefix. */
#define	CAID_DOMAIN_ID		0x3d02	/* 4-byte DomainId. */
#define	CAID_FLAGS		0x3d03	/* 1-byte Mode flags (1=enable). */
#define	CAID_TOPIC_NAME		0x3d04	/* TopicName. */
#define	CAID_TYPE_NAME		0x3d05	/* TypeName. */
#define	CAID_COOKIE		0x3d06	/* Cookie. */

/* Error codes: */
#define	CERR_BAD_REQUEST	400	/* Bad request - was malformed. */
#define	CERR_UNKN_ATTR		405	/* Unknown attribute. */
#define	CERR_OO_RESOURCES	407	/* Out of resources. */
#define	CERR_UNSUPP_DOMAIN	415	/* Unsupported domain. */
#define	CERR_NO_SERVER		444	/* No server. */
#define	CERR_EXISTS		446	/* Already notified. */

/* Which Parameters are allowed in which control message: */
#define	BINDREQ_PIDS	((1 << (CAID_GUID_PREFIX & 0xff)) |	\
			 (1 << (CAID_DOMAIN_ID & 0xff)))
#define	BINDSUCC_PIDS	 (1 << (CAID_GUID_PREFIX & 0xff))
#define	BINDFAIL_PIDS	 (1 << (CAID_ERROR & 0xff))
#define	NOTIFREQ_PIDS	((1 << (CAID_FLAGS & 0xff)) |		\
			 (1 << (CAID_TOPIC_NAME & 0xff)) |	\
			 (1 << (CAID_TYPE_NAME & 0xff)))
#define	NOTIFSUCC_PIDS	 (1 << (CAID_COOKIE & 0xff))
#define	NOTIFFAIL_PIDS	 (1 << (CAID_ERROR & 0xff))
#define	SUSPENDING_PIDS	 0
#define	RESUMING_PIDS	 0
#define	WAKEUP_PIDS	((1 << (CAID_COOKIE & 0xff)) |		\
			 (1 << (CAID_TOPIC_NAME & 0xff)) |	\
			 (1 << (CAID_TYPE_NAME & 0xff)) |	\
			 (1 << (CAID_GUID_PREFIX & 0xff)))
#define	FINALIZE_PIDS	 0

typedef enum {
	BGCP_OK,
	BGCP_FRAME_INCOMPLETE,	/* Message not completely received, continue later. */
	BGCP_ERR_INV_MSG,	/* Incorrect message type/version. */
	BGCP_ERR_VENDOR_KIND,	/* Vendor-specific message. */
	BGCP_ERR_UNKNOWN_KIND,	/* Message kind unknown. */
	BGCP_ERR_INV_LENGTH,	/* Incorrect length. */
	BGCP_ERR_INV_PAR,	/* Unknown parameter. */
	BGCP_ERR_NOMEM		/* Out of memory. */
} ParseError_t;

#define	MAX_COOKIE	8	/* Max. expected cookie data. */

/* Parse result or generation data for known message types: */
typedef struct ctrl_info_st {

	/* Parse results: */
	ParseError_t	 result;
	ControlMsgKind_t type;
	TransactionId_t	 transaction;
	unsigned	 pids;

	/* Parameter values: */
	unsigned	 error_kind;
	unsigned short	 parameter_id;
	int		 flags;
	unsigned	 domain_id;
	const char	 *topic_name;
	const char	 *type_name;
	unsigned char	 cookie [MAX_COOKIE];
	unsigned	 cookie_length;
	GuidPrefix_t	 prefix;
} CtrlInfo_t;

static uint16_t			bgcp_protocol_version = 0x10;
static uint32_t			bgcp_transaction;	/* Next transaction id*/
static unsigned			bgcp_backoff;
static lock_t			bgcp_lock;

static unsigned char		bgcp_tx_buf [MAX_TX_BUF];

IP_CX	*bgv4_server;
IP_CX	*bgv6_server;
IP_CX	*bg_client [BG_MAX_CLIENTS];

int	bgcp_v4_active;
int	bgcp_v6_active;

#ifdef BGCP_TRC_CONTROL

static struct err_kind_st {
	unsigned	error;
	const char	*string;
} ctrl_errs [] = {
	{ CERR_BAD_REQUEST,      "Bad request" },
	{ CERR_UNKN_ATTR,        "Unknown attribute" },
	{ CERR_OO_RESOURCES,     "Out of resources" },
	{ CERR_UNSUPP_DOMAIN,    "Unsupported domain" },
	{ CERR_EXISTS,           "Already exists" }
};

#define	N_CTRL_ERRS	(sizeof (ctrl_errs) / sizeof (struct err_kind_st))

static void bgcp_trace_ctrl (int tx, int fd, const unsigned char *buf, unsigned len)
{
	CtrlHeader	*hp;
	Parameter_t	*pp;
	GuidPrefix_t	*prp;
	unsigned char	*dp;
	unsigned	i, j, h, l, c, left, n;
	uint32_t	u;
	uint16_t	hpversion, hplength, ppparameter_id, pplength;
	ControlMsgKind_t hpmsg_kind;
	char		sbuf [28];
	static const char *cmd_str [] = { "Bind", "Notify", "Suspending", "Resuming", "Wakeup" },
			  *mode_str [] = { "Request", "Success", "Fail" },
			  *par_str [] = { "GUIDPrefix", "DomainId", "Flags", 
				  	  "TopicName", "TypeName", "Cookie",
					  NULL, NULL, "Error", "UnknAttr" };

	log_printf (BGNS_ID, 0, "BGCP: %c [%d] - %3u: ", (tx) ? 'T' : 'R', fd, len);
	hp = (CtrlHeader *) buf;
	hpversion = TTOHS (hp->version);
	hpmsg_kind = TTOHS (hp->msg_kind);
	hplength = TTOHS (hp->length);
	i = 0;
	if (!bgcp_protocol_valid (hp->protocol) ||
	    hpversion < bgcp_protocol_version) {
		log_printf (BGNS_ID, 0, "???");
		goto dump;
	}
	for (i = 8; i < 20; i++) {
		if (i == 12 || i == 16)
			log_printf (BGNS_ID, 0, ":");
		log_printf (BGNS_ID, 0, "%02x", buf [i]);
	}
	c = hpmsg_kind;
	i += 2;
	if ((c & MK_VENDOR_SPEC) != 0) {
		log_printf (BGNS_ID, 0, " ?(%u)", c);
		goto dump;
	}
	h = hpmsg_kind >> 8;
	l = hpmsg_kind & 0xff;
	log_printf (BGNS_ID, 0, " - ");
	if (h < 12 || h > 14 || l < 1 || (l > 5 && (l != 15 || h != 12))) {
		log_printf (BGNS_ID, 0, "?(%u)", hpmsg_kind);
		goto dump;
	}
	if (l == 15)
		log_printf (BGNS_ID, 0, "Finalize\r\n");
	else if (l >= 3 && l <= 5)
		log_printf (BGNS_ID, 0, "%s\r\n", cmd_str [l - 1]);
	else
		log_printf (BGNS_ID, 0, "%s%s\r\n", cmd_str [l - 1], mode_str [h - 12]);
	if (!hplength || (hp->length == 4 && buf [24] == 1 && buf [25] == 0))
		return;

	i += 2;
	left = hplength;
	dp = (unsigned char *) (hp + 1);
	while (left >= 2) {
		pp = (Parameter_t *) dp;
		ppparameter_id = TTOHS (pp->parameter_id);
		pplength = TTOHS (pp->length);
		if (ppparameter_id == PID_SENTINEL)
			return;

		n = pplength + 4U;
		if (n > left ||
		    (pplength & 0x3) != 0) {
		    	log_printf (BGNS_ID, 0, "\t? ");
			goto dump;
		}
		dp += n;
		left -= n;
		h = ppparameter_id >> 8;
		if (h >= 0x80) { /* Vendor-specific parameter. */
		    	log_printf (BGNS_ID, 0, "\t? ");
			goto dump;
		}
		l = ppparameter_id & 0xff;
		if ((!h && !(l == 9 || l == 10)) ||
		    (h == 0x3d && (l < 1 || l > 6)) ||
		    (h != 0 && h != 0x3d)) {
			log_printf (BGNS_ID, 0, "\t? ");
			goto dump;
		}
		i += 4;
		if (par_str [l - 1])
			log_printf (BGNS_ID, 0, "\t%10s: ", par_str [l - 1]);
		else
			log_printf (BGNS_ID, 0, "\t%10d?: ", l);
		if (ppparameter_id == CAID_ERROR ||
		    ppparameter_id == CAID_DOMAIN_ID) {
			memcpy (&u, &buf [i], 4);
			log_printf (BGNS_ID, 0, "%u", u);
			i += pplength;
			if (ppparameter_id == CAID_ERROR) {
				for (j = 0; j < N_CTRL_ERRS; j++)
					if (ctrl_errs [j].error == u)
						break;

				if (j < N_CTRL_ERRS)
					log_printf (BGNS_ID, 0, ": %s", ctrl_errs [j].string);
				else
					log_printf (BGNS_ID, 0, ".");
			}
			else
				log_printf (BGNS_ID, 0, ".");
		}
		else if (ppparameter_id == CAID_FLAGS) {
			if ((buf [i] & 1) != 0)
				log_printf (BGNS_ID, 0, "Register  ");
			else
				log_printf (BGNS_ID, 0, "Unregister");
			i += pplength;
		}
		else if (ppparameter_id == CAID_TOPIC_NAME ||
		         ppparameter_id == CAID_TYPE_NAME) {
			log_printf (BGNS_ID, 0, "%s", &buf [i]);
			i += pplength;
		}
		else if (ppparameter_id == CAID_GUID_PREFIX) {
			prp = (GuidPrefix_t *) &buf [i];
			i += 12;
			log_printf (BGNS_ID, 0, "%s", guid_prefix_str (prp, sbuf));
		}
		else {
			for (j = 0; j < pplength; j++)
				log_printf (BGNS_ID, 0, "%02x", buf [i++]);
		}
		log_printf (BGNS_ID, 0, "\r\n");
	}

    dump:
	for (; i < len; i++) {
		log_printf (BGNS_ID, 0, " %02x", buf [i]);
	}
	log_printf (BGNS_ID, 0, "\r\n");
}

#endif /* BGNS_TRC_CONTROL */

/* Requested client notification data: */
typedef struct sr_notify_st SR_NOTIFY;
struct sr_notify_st {
	SR_NOTIFY	*next;
	SR_NOTIFY	*prev;
	unsigned	domain_id;
	String_t	*topic;
	String_t	*type;
	uintptr_t	user;
	unsigned	nusers;
};

/* List of requested client notifications. */
typedef struct sr_notify_list_st {
	SR_NOTIFY	*head;
	SR_NOTIFY	*tail;
} SR_NOTIFY_LIST;

/* For each pending/active notification item to a peer, the following data is
   kept, associating it with peer cookies. */
typedef struct sr_notify_data_st SR_NOTIFY_DATA;
struct sr_notify_data_st {
	SR_NOTIFY_DATA	*next;
	SR_NOTIFY_DATA	*prev;
	int		enable;
	SR_NOTIFY	*notification;
	unsigned char	cookie [8];
	size_t		length;
};

/* List of registered notification topics. */
typedef struct sr_act_notify_st SR_ACT_NOTIFY;
struct sr_act_notify_st {
	SR_NOTIFY_DATA	*head;
	SR_NOTIFY_DATA	*tail;
};

/* Suspend/resume connection types: */
typedef enum {
	SRC_SERVER,	/* Dedicated notifications server. */
	SRC_SERVER_CX,	/* Dedicated notifications connection to a client. */
	SRC_CLIENT,	/* List of notification connections to servers. */
	SRC_CLIENT_CX,	/* Dedicated notifications connection to a server. */
	SRC_C_TCP,	/* Embedded client cx on existing TCP-Ctrl channel. */
	SRC_S_TCP	/* Embedded server cx on existing TCP-Ctrl channel. */
} SR_CX_TYPE;

/* Suspend/resume connection context. */
typedef struct sr_cx_st SR_CX;
struct sr_cx_st {
	SR_CX		*next;		/* Next connection in list. */
	SR_CX		*prev;		/* Previous connection in list. */
	SR_CX		*root;		/* Root connection. */
	SR_CX		*derived;	/* List of derived connections. */
	unsigned	handle;		/* Connection handle (>0). */
	SR_CX_TYPE	type;		/* Type of connection. */
	unsigned	domain_id;	/* Domain identifier. */
	unsigned	index;		/* Domain index. */
	RTPS_TCP_RSERV	rserver;	/* C: remote server info. */
	unsigned	port;		/* S: port number. */
	int		ipv6;		/* IPv6 mode. */
	int		secure;		/* S: secure connection. */
	int		active;		/* !S: can send messages. */
	int		suspended;	/* Client is suspended. */
	SRN_MATCH	match_fct;	/* Match callback function. */
	SRN_WAKEUP	wakeup_fct;	/* C: Wakeup callback function. */
	SRN_NOTIFY	notify_fct;	/* S: Notification callback function. */
	SRN_SUSPEND	suspend_fct;	/* S: Suspend callback function. */
	uintptr_t	user;		/* User parameter for notifications. */
	GuidPrefix_t	rprefix;	/* Peer prefix. */
	TCP_NOTIFY_STATE state;		/* Notification state. */
	SR_NOTIFY_DATA	*act_np;	/* C: Notification being registered. */
	SR_ACT_NOTIFY	notifications;	/* Registered notifications. */
	unsigned	retries;	/* # of retries. */
	Timer_t		*timer;		/* Retry timer. */
	uint32_t	transaction_id;	/* Transaction id. */
	IP_CX		*cxp;		/* IP connection pointer. */
};

typedef struct sr_cx_list_st {
	SR_CX		*head;
	SR_CX		*tail;
} SR_CX_LIST;

TABLE(SR_CX *, SR_CX_TABLE);

static SR_CX_LIST	cx_list;	/* Linked list of connection contexts.*/
static SR_CX_TABLE	cx_table;	/* Connection contexts table. */
static SR_NOTIFY_LIST	notify_list;	/* User notifications. */
static int		client_suspend;
static int		ctmr_suspend;
static DDS_Activities_on_connected connect_fct;

void bgcp_reset (void)
{
	client_suspend = ctmr_suspend = 0;
}

#ifdef BGCP_TRC_STATE

#define	BGCP_NCX_STATE(t,cxp,s)	bgcp_trace_cx_state(t, cxp, s)
#define	BGCP_NP_STATE(t,cp,s)	bgcp_trace_p_state(t, cp, s)

static void bgcp_trace_cx_state (const char *type, IP_CX *cxp, IP_CX_STATE ns)
{
	static const char *cx_state_str [] = {
		"CLOSED", "LISTEN", "CAUTH", "CONREQ", "CONNECT", "WRETRY", "SAUTH", "OPEN"
	};

	if (ns != cxp->cx_state) {
		log_printf (BGNS_ID, 0, "BGCP(%s", type);
		if (cxp->handle)
			log_printf (BGNS_ID, 0, ":%u", cxp->handle);
		log_printf (BGNS_ID, 0, ") CX: %s -> %s\r\n",
				cx_state_str [cxp->cx_state], 
				cx_state_str [ns]);
		cxp->cx_state = ns;
	}
}

static void bgcp_trace_p_state (const char *type, SR_CX *cp, int ns)
{
	static const char *c_state_str [] = {
		"IDLE", "WCXOK", "WBINDOK", "NOTIFY"
	};

	if (ns != (int) cp->state) {
		log_printf (BGNS_ID, 0, "BGCP(%s", type);
		if (cp->cxp && cp->cxp->handle)
			log_printf (BGNS_ID, 0, ":%u", cp->cxp->handle);
		log_printf (BGNS_ID, 0, ") ");
		log_printf (BGNS_ID, 0, "C: %s -> %s\r\n",
			c_state_str [cp->state], 
			c_state_str [ns]);
		cp->state = ns;
		if (cp->cxp && cp->cxp->cx_mode == ICM_NOTIFY)
			cp->cxp->p_state = ns;
	}
}

#else /* !BGCP_TRC_STATE */

#define	BGCP_NCX_STATE(t,cxp,s)	cxp->cx_state = s
#define	BGCP_NP_STATE(t,cp,s)	cp->state = s; if (cp->cxp && cp->cxp->cx_mode == ICM_NOTIFY) cp->cxp->p_state = s

#endif /* !BGCP_TRC_STATE */

static int terminated (const unsigned char *sp, size_t max_length)
{
	unsigned	i;

	for (i = 0; i < max_length; i++)
		if (!sp [i] && i)
			return (1);

	return (0);
}

static void bgcp_parse_pids (CtrlHeader *hp, unsigned pid_set, CtrlInfo_t *info)
{
	Parameter_t	*pp;
	unsigned char	*dp;
	unsigned	left, id, n;
	uint32_t	u;
	uint16_t	s, pplength, ppparameter_id, hplength;

	hplength = TTOHS (hp->length);
	left = hplength;
	dp = (unsigned char *) (hp + 1);
	info->result = BGCP_OK;
	while (left >= 2) {
		pp = (Parameter_t *) dp;
		ppparameter_id = TTOHS (pp->parameter_id);
		pplength = TTOHS (pp->length);
		if (ppparameter_id == PID_SENTINEL)
			break;

		n = pp->length + 4U;
		if (n > left ||
		    (pplength & 0x3) != 0) {
			info->result = BGCP_ERR_INV_LENGTH;
			return;
		}
		dp += n;
		left -= n;
		if (ppparameter_id >= 0x8000)
			continue;	/* Ignore vendor-specific parameters. */

		id = ppparameter_id & 0xff;
		if (id > 31 || ((1 << id) & pid_set) == 0) {
			info->result = BGCP_ERR_INV_PAR;
			break;
		}
		if (((1 << id) & info->pids) != 0)
			continue;	/* Ignore if multiple occurence. */

		info->pids |= (1 << id);
		switch (id) {
			case CAID_ERROR:
				if (pplength != 4) {
					info->result = BGCP_ERR_INV_LENGTH;
					break;
				}
				memcpy (&u, pp->value, sizeof (uint32_t));
				info->error_kind = TTOHL (u);
				break;
			case CAID_UNKN_ATTR:
				if (pplength != 4) {
					info->result = BGCP_ERR_INV_LENGTH;
					break;
				}
				memcpy (&s, pp->value, sizeof (uint16_t));
				info->parameter_id = TTOHS (s);
				break;
			case CAID_GUID_PREFIX & 0xff:
				if (pplength != 12) {
					info->result = BGCP_ERR_INV_LENGTH;
					break;
				}
				memcpy (info->prefix.prefix, pp->value, 12);
				break;
			case CAID_DOMAIN_ID & 0xff:
				if (pplength != 4) {
					info->result = BGCP_ERR_INV_LENGTH;
					break;
				}
				memcpy (&u, pp->value, sizeof (uint32_t));
				info->domain_id = TTOHL (u);
				break;
			case CAID_FLAGS & 0xff:
				if (pplength != 4) {
					info->result = BGCP_ERR_INV_LENGTH;
					break;
				}
				info->flags = pp->value [0];
				break;
			case CAID_TOPIC_NAME & 0xff:
				if (pplength < 2 || !terminated (pp->value, pplength)) {
					info->result = BGCP_ERR_INV_LENGTH;
					break;
				}
				info->topic_name = (const char *) pp->value;
				break;
			case CAID_TYPE_NAME & 0xff:
				if (pplength < 2 || !terminated (pp->value, pplength)) {
					info->result = BGCP_ERR_INV_LENGTH;
					break;
				}
				info->type_name = (const char *) pp->value;
				break;
			case CAID_COOKIE & 0xff:
				if (!pplength || pplength > MAX_COOKIE) {
					info->result = BGCP_ERR_INV_LENGTH;
					break;
				}
				info->cookie_length = pplength;
				memcpy (info->cookie, pp->value, pplength);
				break;
		}
		if (info->result)
			break;
	}
}

static void bgcp_parse (int                 fd,
			const unsigned char *rx,
			size_t              length,
			CtrlInfo_t          *info)
{
	unsigned		h, l;
	CtrlHeader		*chp;
	uint16_t		chpversion;
	ControlMsgKind_t	chpmsg_kind;

	ARG_NOT_USED (length)
#ifndef BGCP_TRC_CONTROL
	ARG_NOT_USED (fd)
#endif

	memset (info, 0, sizeof (CtrlInfo_t));
	chp = (CtrlHeader *) rx;
	chpversion = TTOHS (chp->version);
	chpmsg_kind = TTOHS (chp->msg_kind);

	/* Validate protocol type and version. */
	if (!bgcp_protocol_valid (chp->protocol) ||
	    chpversion < bgcp_protocol_version) {
		info->result = BGCP_ERR_INV_MSG;
		return;
	}
#ifdef BGCP_TRC_CONTROL
	bgcp_trace_ctrl (0, fd, rx, length);
#endif
	if ((chpmsg_kind & MK_VENDOR_SPEC) != 0) {
		info->result = BGCP_ERR_VENDOR_KIND;
		return;
	}
	info->type = chpmsg_kind;
	memcpy (info->transaction, chp->transaction, 12);
	h = chpmsg_kind >> 8;
	l = chpmsg_kind & 0xff;
	if (h < 12 || h > 14) {
		info->result = BGCP_ERR_UNKNOWN_KIND;
		return;
	}
	if (l < 1 || (l > 5 && l != 15)) {
		info->result = BGCP_ERR_UNKNOWN_KIND;
		return;
	}
	switch (l) {
		case 1:	if (h == 12) 		/* CMT_BIND_REQ */
				bgcp_parse_pids (chp, BINDREQ_PIDS, info);
			else if (h == 13)	/* CMT_BIND_SUCC */
				bgcp_parse_pids (chp, BINDSUCC_PIDS, info);
			else			/* CMT_BIND_FAIL */
				bgcp_parse_pids (chp, BINDFAIL_PIDS, info);
			break;
		case 2:	if (h == 12)		/* CMT_NOTIFY_REQ */
				bgcp_parse_pids (chp, NOTIFREQ_PIDS, info);
			else if (h == 13)	/* CMT_NOTIFY_SUCC */
				bgcp_parse_pids (chp, NOTIFSUCC_PIDS, info);
			else			/* CMT_NOTIFY_FAIL */
				bgcp_parse_pids (chp, NOTIFFAIL_PIDS, info);
			break;
		case 3:	if (h == 12)		/* CMT_SUSPENDING */
				bgcp_parse_pids (chp, SUSPENDING_PIDS, info);
			else {
				info->result = BGCP_ERR_UNKNOWN_KIND;
				return;
			}
			break;
		case 4:	if (h == 12)		/* CMT_RESUMING */
				bgcp_parse_pids (chp, RESUMING_PIDS, info);
			else {
				info->result = BGCP_ERR_UNKNOWN_KIND;
				return;
			}
			break;
		case 5:	if (h == 12)		/* CMT_WAKEUP */
				bgcp_parse_pids (chp, WAKEUP_PIDS, info);
			else {
				info->result = BGCP_ERR_UNKNOWN_KIND;
				return;
			}
			break;
		case 15:
			if (h == 12)		/* CMT_FINALIZE */
				bgcp_parse_pids (chp, FINALIZE_PIDS, info);
			else {
				info->result = BGCP_ERR_UNKNOWN_KIND;
				return;
			}
			break;
		default:
			info->result = BGCP_ERR_UNKNOWN_KIND;
			return;
	}
}

static size_t bgcp_create (unsigned char *tx, CtrlInfo_t *info, int new_tr)
{
	CtrlHeader		*chp;
	Parameter_t		*pp;
	size_t			length;
	unsigned		pids, i, m, n;
	unsigned char		*dp;
	uint32_t		u;
	uint16_t		s;

	if (!tx || !info)
		return (0);

	length = 0;
	chp = (CtrlHeader *) tx;
	memcpy (chp->protocol, bgcp_protocol_id, 4);
	chp->version = HTOTS (0x10);
	vendor_id_init (chp->vendor_id);
	if (new_tr) {
		memcpy (info->transaction, guid_local (), 8);
		bgcp_transaction++;
		memcpy (info->transaction + 8, &bgcp_transaction, 4);
	}
	memcpy (chp->transaction, info->transaction, 12);
	chp->msg_kind = HTOTS (info->type);
	chp->length = 0;
	length = sizeof (CtrlHeader);
	if (!info->pids)
		return (length);

	dp = (unsigned char *) (chp + 1);
	pids = info->pids;
	for (i = 0, m = 1U; pids; i++, m <<= 1U) {
		if ((pids & m) == 0)
			continue;

		pids &= ~m;
		pp = (Parameter_t *) dp;
		pp->parameter_id = HTOTS (i);
		if (i < 9)
			pp->parameter_id |= HTOTS (0x3d00);
		switch (i) {
			case CAID_ERROR:
				n = 4;
				u = HTOTL (info->error_kind);
				memcpy (pp->value, &u, sizeof (uint32_t));
				break;
			case CAID_UNKN_ATTR:
				n = 4;
				s = HTOTS (info->parameter_id);
				memcpy (pp->value, &s, sizeof (uint16_t));
				pp->value [2] = pp->value [3] = 0;
				break;
			case CAID_GUID_PREFIX & 0xff:
				n = 12;
				memcpy (pp->value, info->prefix.prefix, 12);
				break;
			case CAID_DOMAIN_ID & 0xff:
				n = 4;
				u = HTOTL (info->domain_id);
				memcpy (pp->value, &u, sizeof (uint32_t));
				break;
			case CAID_FLAGS & 0xff:
				n = 4;
				pp->value [0] = info->flags;
				pp->value [1] = pp->value [2] = pp->value [3] = 0;
				break;
			case CAID_TOPIC_NAME & 0xff:
				n = strlen (info->topic_name) + 1;
				memcpy (pp->value, info->topic_name, n);
				while ((n & 3) != 0)
					pp->value [n++] = 0;
				break;
			case CAID_TYPE_NAME & 0xff:
				n = strlen (info->type_name) + 1;
				memcpy (pp->value, info->type_name, n);
				while ((n & 3) != 0)
					pp->value [n++] = 0;
				break;
			case CAID_COOKIE & 0xff:
				n = info->cookie_length;
				memcpy (pp->value, info->cookie, info->cookie_length);
				while ((n & 3) != 0)
					pp->value [n++] = 0;
				break;
			default:
				n = 0;
				break;
		}
		pp->length = HTOTS (n);
		n += 4;
		chp->length += n;
		length += n;
		dp += n;
	}
	pp = (Parameter_t *) dp;
	pp->parameter_id = HTOTS (PID_SENTINEL);
	pp->length = 0;
	chp->length += 4;
	chp->length = HTOTS (chp->length);
	return (length + 4);
}

static WR_RC bgcp_send_ctrl (IP_CX *cxp, CtrlInfo_t *info, int new_tr)
{
	size_t		n;
	WR_RC		rc;
	unsigned char	*txp;

	if (!cxp->fd) {
		warn_printf ("bgcp_send_ctrl: no connection!");
		return (WRITE_ERROR);
	}
	n = bgcp_create (bgcp_tx_buf, info, new_tr);
	if (!n) {
		warn_printf ("BGCP: can't create control message!");
		cxp->stats.nomem++;
		return (WRITE_ERROR);
	}

	/* Set txp either to a newly allocated buffer (TLS expects to reuse the
	   same buffer data in some situations) or to rtps_tx_buf (for UDP).
	   If set to a new buffer, TLS is responsible to xfree() it when it
	   doesn't need it anymore. */
#ifdef DDS_SECURITY
	if (cxp->stream_fcts == &tls_functions) {
		txp = xmalloc (n);
		if (!txp) {
			warn_printf ("bgcp_send_ctrl: out-of-memory for send buffer!");
			cxp->stats.nomem++;
			return (WRITE_ERROR);
		}
		memcpy (txp, bgcp_tx_buf, n);
	}
	else
#endif
		txp = bgcp_tx_buf;

	/*trc_print1 ("C:T[%d:", cxp->fd);*/
	rc = cxp->stream_fcts->write_msg (cxp, txp, n);
	if (rc == WRITE_FATAL) {
		log_printf (BGNS_ID, 0, "bgcp_send_ctrl: fatal error sending control message [%d] {rc=%d} (%s).\r\n", cxp->fd, rc, strerror (ERRNO));
		return (rc);
	}
	else if (rc == WRITE_ERROR) {
		log_printf (BGNS_ID, 0, "bgcp_send_ctrl: error sending control message [%d] {rc=%d} (%s).\r\n", cxp->fd, rc, strerror (ERRNO));
		cxp->stats.write_err++;
		return (rc);
	}
	else if (rc == WRITE_BUSY) {
		log_printf (BGNS_ID, 0, "bgcp_send_ctrl: Write still busy from previous control message [%d] {rc=%d} (%s).\r\n", cxp->fd, rc, strerror (ERRNO));
		return (rc);
	}
	else if (rc == WRITE_PENDING) {
		log_printf (BGNS_ID, 0, "bgcp_send_ctrl: Write pending of following control message [%d] {rc=%d} (%s).\r\n", cxp->fd, rc, strerror (ERRNO));
	}
	/*trc_print_region (bgcp_tx_buf, n, 0, 0);
	trc_print ("]\r\n");*/
	ADD_ULLONG (cxp->stats.octets_sent, n);
	cxp->stats.packets_sent++;
#ifdef BGCP_TRC_CONTROL
	bgcp_trace_ctrl (1, cxp->fd, bgcp_tx_buf, n);
#endif
	return (rc);
}

static DDS_ReturnCode_t bgcp_send_request (SR_CX      *cp,
					   CtrlInfo_t *info,
					   TMR_FCT    fct,
					   Ticks_t    t)
{
	int	err;

	err = bgcp_send_ctrl (cp->cxp, info, 1);
	if (err == WRITE_FATAL)
		return (DDS_RETCODE_ALREADY_DELETED);

	if (!cp->timer) {
		cp->timer = tmr_alloc ();
		if (!cp->timer)
			return (DDS_RETCODE_OUT_OF_RESOURCES);

		tmr_init (cp->timer, "ReqTimer");
	}
	log_printf (BGNS_ID, 0, "BGCP(%u): timer started!\r\n", cp->cxp->handle);
	tmr_start (cp->timer, t, (uintptr_t) cp, fct);
	return ((err == WRITE_OK || err == WRITE_PENDING) ? 
				DDS_RETCODE_OK : DDS_RETCODE_NOT_ALLOWED_BY_SEC);
}

static void c_restart (SR_CX *cxp);

static DDS_ReturnCode_t bgcp_send_bind_request (SR_CX        *cp,
						TMR_FCT      fct,
						Ticks_t      t,
						unsigned     id)
{
	CtrlInfo_t	 info;
	DDS_ReturnCode_t r;
	Domain_t	 *dp;

	/*log_printf (BGNS_ID, 0, "BGCP(C): Sending BindRequest\r\n");*/
	memset (&info, 0, sizeof (info));
	info.type = CMT_BIND_REQ;
	info.pids = BINDREQ_PIDS;
	dp = domain_get (id, 1, &r);
	if (!dp)
		return (DDS_RETCODE_PRECONDITION_NOT_MET);

	memcpy (info.prefix.prefix, dp->participant.p_guid_prefix.prefix, 12);
	info.domain_id = dp->domain_id;
	lock_release (dp->lock);
	r = bgcp_send_request (cp, &info, fct, t);
	if (r == DDS_RETCODE_ALREADY_DELETED) {
		c_restart (cp);
		return (r);
	}
	memcpy (&cp->transaction_id, info.transaction + 8, 4);
	return (r);
}

static void bgcp_cx_closed (IP_CX *cxp)
{
	if (connect_fct)
		(*connect_fct) (cxp->fd, 0);
	if (cxp->fd_owner)
		log_printf (BGNS_ID, 0, "BGCP(*:%u): [%d] connection closed - %p.\r\n", cxp->handle, cxp->fd, (void *) cxp);
	if ((cxp->fd_owner || cxp->cx_state == CXS_CONREQ) && 
	    cxp->stream_fcts &&
	    cxp->stream_fcts->disconnect)
		cxp->stream_fcts->disconnect (cxp);

	BGCP_NCX_STATE ("*", cxp, CXS_CLOSED);
}

static int bgcp_stop_x (int handle);

#ifdef BGCP_TRC_CX
#define	tcx_print(s)		log_printf (BGNS_ID, 0, s)
#define	tcx_print1(s,a1)	log_printf (BGNS_ID, 0, s, a1)
#define	tcx_print2(s,a1,a2)	log_printf (BGNS_ID, 0, s, a1, a2)
#define	tcx_print3(s,a1,a2,a3)	log_printf (BGNS_ID, 0, s, a1, a2, a3)
#else
#define	tcx_print(s)
#define	tcx_print1(s,a1)
#define	tcx_print2(s,a1,a2)
#define	tcx_print3(s,a1,a2,a3)
#endif

static void bgcp_close_fd (IP_CX *cxp)
{
	SR_CX		*cp;
	IP_CX		*p, *prev;
	unsigned	h;

	tcx_print2 ("BGCP: close_fd(%p:%d);\r\n", (void *) cxp, cxp->handle);

	/* Get the corresponding context. */
	/* Close the file descriptor and adjust the state. */
	if (cxp->cx_mode == ICM_NOTIFY || cxp->cx_mode == ICM_ROOT) {
		bgcp_cx_closed (cxp);

		/* Remove timer if still running. */
		if (cxp->timer) {
			log_printf (BGNS_ID, 0, "BGCP(%u): Stop timer (%u).\r\n", cxp->handle, cxp->dst_port);
			tmr_stop (cxp->timer);
			tmr_free (cxp->timer);
			cxp->timer = NULL;
		}

		/* Remove context from parent list. */
		if (cxp->parent) {
			for (prev = NULL, p = cxp->parent->clients;
			     p;
			     prev = p, p = p->next)
				if (p == cxp) {
					if (prev)
						prev->next = p->next;
					else
						cxp->parent->clients = p->next;
					break;
				}
		}
		else
			while (cxp->clients)
				bgcp_close_fd (cxp->clients);

		/* Remove context handle if it was set. */
		if (cxp->handle) {
			rtps_ip_free_handle (cxp->handle);
			if (cxp->locator)
				cxp->locator->locator.handle = 0;

			/* Don't use the handle anymore. */
			cxp->handle = 0;
		}

		/* Free the locator. */
		if (cxp->locator) {
			if (!cxp->locator->users)
				xfree (cxp->locator);
			else
				locator_unref (cxp->locator);
			cxp->locator = NULL;
		}
	}
	h = cxp->user;
	if (h && TABLE_ITEM_EXISTS (cx_table, h)) {
		cp = TABLE_ITEM (cx_table, h);
		cp->cxp = NULL;
		cxp->user = 0;
		if (cp->type == SRC_SERVER_CX && cp->match_fct && cp->active)
			(*cp->match_fct) (0, cp->user, &cp->rprefix, NULL);
	}
	else
		cp = NULL;

	/* Free the context. */
	if (cxp->cx_mode == ICM_NOTIFY || cxp->cx_mode == ICM_ROOT) 
		rtps_ip_free (cxp);

	/* Free the associated context. */
	if (cp) {
		BGCP_NP_STATE ("*", cp, TNS_IDLE);
		bgcp_stop_x (h);
	}
}

static void bgcp_fatal (SR_CX *cp)
{
	if (cp->type < SRC_C_TCP) /* Not embedded. */
		bgcp_close_fd (cp->cxp);
	else
		bgcp_stop_x (cp->handle);
}

static DDS_ReturnCode_t bgcp_send_bind_success (SR_CX        *cp,
						CtrlInfo_t   *c_info,
						GuidPrefix_t *prefix)
{
	CtrlInfo_t	s_info;

	/*log_printf (BGNS_ID, 0, "BGCP(S): Sending BindSuccess\r\n");*/
	memset (&s_info, 0, sizeof (s_info));
	s_info.type = CMT_BIND_SUCC;
	s_info.pids = BINDSUCC_PIDS;
	memcpy (&s_info.transaction, &c_info->transaction, sizeof (TransactionId_t));
	memcpy (s_info.prefix.prefix, prefix->prefix, 12);
	if (bgcp_send_ctrl (cp->cxp, &s_info, 0) == WRITE_FATAL) {
		bgcp_fatal (cp);
		return (DDS_RETCODE_ALREADY_DELETED);
	}
	else
		return (DDS_RETCODE_OK);
}

# if 0
static DDS_ReturnCode_t bgcp_send_bind_fail (SR_CX *cp, CtrlInfo_t *c_info, unsigned error)
{
	CtrlInfo_t	s_info;

	/*log_printf (BGNS_ID, 0, "BGCP(S): Sending BindFail\r\n");*/
	memset (&s_info, 0, sizeof (s_info));
	s_info.type = CMT_BIND_FAIL;
	s_info.pids = BINDFAIL_PIDS;
	memcpy (&s_info.transaction, &c_info->transaction, sizeof (TransactionId_t));
	s_info.error_kind = error;
	if (bgcp_send_ctrl (cp->cxp, &s_info, 0) == WRITE_FATAL) {
		bgcp_fatal (cp);
		return (DDS_RETCODE_ALREADY_DELETED);
	}
	else
		return (DDS_RETCODE_OK);
}
# endif

static DDS_ReturnCode_t bgcp_send_finalize (SR_CX *cp)
{
	CtrlInfo_t	s_info;

	/*log_printf (BGNS_ID, 0, "BGCP(xx): Sending Finalize\r\n");*/
	memset (&s_info, 0, sizeof (s_info));
	s_info.type = CMT_FINALIZE;
	s_info.pids = FINALIZE_PIDS;
	if (bgcp_send_ctrl (cp->cxp, &s_info, 1) == WRITE_FATAL) {
		bgcp_fatal (cp);
		return (DDS_RETCODE_ALREADY_DELETED);
	}
	else
		return (DDS_RETCODE_OK);
}

static DDS_ReturnCode_t bgcp_send_notify_request (SR_CX      *cp,
						  TMR_FCT    fct,
						  Ticks_t    t,
						  int        reg,
						  const char *topic_name,
						  const char *type_name)
{
	CtrlInfo_t		info;
	DDS_ReturnCode_t	ret;

	/*log_print (BGNS_ID, 0, "BGCP(C): sending NotifyRequest\r\n");*/
	memset (&info, 0, sizeof (info));
	info.type = CMT_NOTIFY_REQ;
	info.pids = NOTIFREQ_PIDS;
	info.flags = reg;
	info.topic_name = topic_name;
	info.type_name = type_name;
	ret = bgcp_send_request (cp, &info, fct, t);
	if (ret == DDS_RETCODE_ALREADY_DELETED)
		c_restart (cp);

	memcpy (&cp->transaction_id, info.transaction + 8, 4);
	return (ret);
}

static DDS_ReturnCode_t bgcp_send_notify_success (SR_CX         *cp,
						  CtrlInfo_t    *c_info,
						  unsigned char *cookie,
						  size_t        length)
{
	CtrlInfo_t	s_info;

	/*log_printf (BGNS_ID, 0BG "BGCP(S): Sending NotifySuccess\r\n");*/
	memset (&s_info, 0, sizeof (s_info));
	s_info.type = CMT_NOTIFY_SUCC;
	s_info.pids = NOTIFSUCC_PIDS;
	if (cookie && length)
		memcpy (s_info.cookie, cookie, length);
	s_info.cookie_length = length;
	memcpy (&s_info.transaction, &c_info->transaction, sizeof (TransactionId_t));
	if (bgcp_send_ctrl (cp->cxp, &s_info, 0) == WRITE_FATAL) {
		bgcp_fatal (cp);
		return (DDS_RETCODE_ALREADY_DELETED);
	}
	else
		return (DDS_RETCODE_OK);
}

static DDS_ReturnCode_t bgcp_send_notify_fail (SR_CX      *cp,
					       CtrlInfo_t *c_info,
					       unsigned   error)
{
	CtrlInfo_t	s_info;

	/*log_printf (BGNS_ID, 0, "BGCP(S): Sending NotifyFail\r\n");*/
	memset (&s_info, 0, sizeof (s_info));
	s_info.type = CMT_NOTIFY_FAIL;
	s_info.pids = NOTIFFAIL_PIDS;
	memcpy (&s_info.transaction, &c_info->transaction, sizeof (TransactionId_t));
	s_info.error_kind = error;
	if (bgcp_send_ctrl (cp->cxp, &s_info, 0) == WRITE_FATAL) {
		bgcp_fatal (cp);
		return (DDS_RETCODE_ALREADY_DELETED);
	}
	else
		return (DDS_RETCODE_OK);
}

static DDS_ReturnCode_t bgcp_send_suspend (SR_CX *cp, int suspend)
{
	CtrlInfo_t	s_info;

	/*log_printf (BGNS_ID, 0, "BGCP(C): Sending Suspend\r\n");*/
	memset (&s_info, 0, sizeof (s_info));
	if (suspend) {
		s_info.type = CMT_SUSPENDING;
		s_info.pids = SUSPENDING_PIDS;
	}
	else {
		s_info.type = CMT_RESUMING;
		s_info.pids = RESUMING_PIDS;
	}
	if (bgcp_send_ctrl (cp->cxp, &s_info, 1) == WRITE_FATAL) {
		bgcp_fatal (cp);
		return (DDS_RETCODE_ALREADY_DELETED);
	}
	else
		return (DDS_RETCODE_OK);
}

static DDS_ReturnCode_t bgcp_send_wakeup (SR_CX         *cp,
					  unsigned char *cookie,
					  size_t        length,
					  const char    *topic_name,
					  const char    *type_name,
					  GuidPrefix_t  *prefix)
{
	CtrlInfo_t	s_info;

	/*log_printf (BGNS_ID, 0, "BGCP(C): Sending Suspend\r\n");*/
	memset (&s_info, 0, sizeof (s_info));
	s_info.type = CMT_WAKEUP;
	s_info.pids = WAKEUP_PIDS;
	if (length) {
		memcpy (s_info.cookie, cookie, length);
		s_info.cookie_length = length;
	}
	else
		s_info.pids &= ~(1 << (CAID_COOKIE & 0xff));
	if (topic_name)
		s_info.topic_name = topic_name;
	else
		s_info.pids &= ~(1 << (CAID_TOPIC_NAME & 0xff));
	if (type_name)
		s_info.type_name = type_name;
	else
		s_info.pids &= ~(1 << (CAID_TYPE_NAME & 0xff));
	s_info.prefix = *prefix;
	if (bgcp_send_ctrl (cp->cxp, &s_info, 1) == WRITE_FATAL) {
		bgcp_fatal (cp);
		return (DDS_RETCODE_ALREADY_DELETED);
	}
	else
		return (DDS_RETCODE_OK);
}


/************************/
/*   Client FSM logic   */
/************************/

void bgcp_register_connect (DDS_Activities_on_connected fct)
{
	connect_fct = fct;
}

static void c_closed (SR_CX *cp)
{
	log_printf (BGNS_ID, 0, "BGCP(C:%u): connection closed!\r\n", cp->cxp->handle);
	BGCP_NP_STATE ("*", cp, TNS_IDLE);
	bgcp_cx_closed (cp->cxp);
}

static void c_connect (SR_CX *cxp);

static void c_timeout (uintptr_t user);

static void c_retry (SR_CX *cp, int delay)
{
	if (!cp->timer) {
		cp->timer = tmr_alloc ();
		if (!cp->timer) {
			warn_printf ("BGCP: not enough memory for TCP Client timer!");
			return;
		}
		tmr_init (cp->timer, "BGCP-Client");
		if (delay) {
			log_printf (BGNS_ID, 0, "BGCP(%u): timer started!\r\n", cp->cxp->handle);
			tmr_start (cp->timer, delay, 
					(uintptr_t) cp, c_timeout);
		}
	}
	if (delay) {
		BGCP_NCX_STATE ("C", cp->cxp, CXS_WRETRY);
		BGCP_NP_STATE ("C", cp, TNS_WCXOK);
	}
	else
		c_connect (cp);
}

static void c_bind (SR_CX *cp)
{
	DDS_ReturnCode_t	ret;

	cp->retries = (cp->type == SRC_C_TCP) ? ~0 : BREQ_RETRIES;
	ret = bgcp_send_bind_request (cp, c_timeout, BREQ_TO, cp->index);
	if (!ret)
		BGCP_NP_STATE ("C", cp, TNS_WBINDOK);
	else if (ret == DDS_RETCODE_ALREADY_DELETED)
		c_restart (cp);
}

static void c_connected (IP_CX *cxp)
{
	SR_CX			*cp;

	log_printf (BGNS_ID, 0, "BGCP(C:%u): control connection established.\r\n", cxp->handle);

	if (client_suspend) /* Restore previous timing mode. */
		dds_tmr_suspend = ctmr_suspend;

	BGCP_NCX_STATE ("C", cxp, CXS_OPEN);
	bgcp_backoff = 0;

	if (!TABLE_ITEM_EXISTS (cx_table, cxp->user))
		return;

	cp = TABLE_ITEM (cx_table, cxp->user);
	if (cp->index)
		c_bind (cp);
}

static void c_connect (SR_CX *cp)
{
	int		r;
	unsigned	n;

	BGCP_NCX_STATE ("C", cp->cxp, CXS_CONNECT);
	BGCP_NP_STATE ("C", cp, TNS_WCXOK);
	r = cp->cxp->stream_fcts->connect (cp->cxp, cp->cxp->dst_port);
	if (r < 0) {
		log_printf (BGNS_ID, 0, "BGCP(%u): timer started!\r\n", cp->cxp->handle);
		if (bgcp_backoff < CON_MAX_BACKOFF) {
			bgcp_backoff++;
			/*log_printf (BGNS_ID, 0, "BGCP: backoff = %u\r\n", bgcp_backoff);*/
		}
		n = 1 << (fastrandn (bgcp_backoff + 1));
		tmr_start (cp->timer, CON_TO * n, (uintptr_t) cp, c_timeout);
		if (r == -1) {
			log_printf (BGNS_ID, 0, "BGCP(C:%u): connecting to server ... \r\n", cp->cxp->handle);
			return;
		}
		log_printf (BGNS_ID, 0, "BGCP(CC:%u): connect() failed!\r\n", cp->cxp->handle);
		BGCP_NCX_STATE ("C", cp->cxp, CXS_WRETRY);
		return;
	}
	c_connected (cp->cxp);
}

static void c_reconnect (SR_CX *cp, int delay)
{
	c_closed (cp);
	c_retry (cp, delay);
}

typedef enum {
	CE_START,
	CE_STOP,
	CE_CXERR,
	CE_TO,
	CE_FINALIZE,
	CE_BINDFAIL,
	CE_BINDSUCC,
	CE_NOTFAIL,
	CE_NOTSUCC,
	CE_WAKEUP
} C_EVENT;

#define	C_NSTATES	((unsigned) TNS_NOTIFY + 1)
#define	C_NEVENTS	((unsigned) CE_WAKEUP + 1)

typedef void (*CFCT) (SR_CX *cp);

static void c_restart (SR_CX *cp)
{
	unsigned	delay;

	/* Try to connect again to the remote server with a randomized delay. */
	delay = TICKS_PER_SEC + fastrandn (TICKS_PER_SEC * 4);
	c_reconnect (cp, delay);
}

static void c_i_start (SR_CX *cp)
{
	bgcp_backoff = 0;
	c_connect (cp);
}

static void c_x_stop (SR_CX *cp)
{
	bgcp_fatal (cp);
}

static void c_wc_stop (SR_CX *cp)
{
	log_printf (BGNS_ID, 0, "BGCP(C): Stop timer!\r\n");
	tmr_stop (cp->timer);
	c_x_stop (cp);
}

static void c_x_cxerr (SR_CX *cp)
{
	int	delay;

	delay = cp->state != TNS_NOTIFY;
	if (client_suspend) { /* Overrule previous timers mode until cx up. */
		ctmr_suspend = dds_tmr_suspend;
		dds_tmr_suspend = 0;
	}
	c_reconnect (cp, delay);
}

#define c_wc_cxerr c_x_cxerr

static void c_wc_to (SR_CX *cp)
{
	if (cp->cxp->cx_state == CXS_WRETRY)
		c_connect (cp);
	else
		c_reconnect (cp, 0);
}

#define c_wb_stop	c_x_stop
#define c_wb_cxerr	c_x_cxerr

static void c_wb_to (SR_CX *cp)
{
	if (cp->retries > 0) {
		cp->retries--;
		bgcp_send_bind_request (cp, c_timeout, BREQ_TO, cp->index);
	}
	else {
		bgcp_send_finalize (cp);
		BGCP_NP_STATE ("C", cp, TNS_WCXOK);
		c_wc_to (cp);
	}
}

static void c_x_final (SR_CX *cp)
{
	log_printf (BGNS_ID, 0, "BGCP(C): control connection ready!\r\n");
	log_printf (BGNS_ID, 0, "BGCP(C): Stop timer.\r\n");
	tmr_stop (cp->timer);
	c_reconnect (cp, 1);
}

#define c_wb_final c_x_final

static void c_wb_bfail (SR_CX *cp)
{
	log_printf (BGNS_ID, 0, "BGCP(C): Bind rejected by server.\r\n");
	c_reconnect (cp, 1);
}

static void bgcp_control_ready (SR_CX *cp);

static void c_wb_bsucc (SR_CX *cp)
{
	log_printf (BGNS_ID, 0, "BGCP(C): control connection ready!\r\n");
	log_printf (BGNS_ID, 0, "BGCP(C): Stop timer.\r\n");
	tmr_stop (cp->timer);
	tmr_free (cp->timer);
	cp->timer = NULL;
	BGCP_NP_STATE ("C", cp, TNS_NOTIFY);
	bgcp_control_ready (cp);

}

#define c_a_stop c_x_stop
#define	c_a_cxerr c_x_cxerr
#define	c_a_final c_x_final

static void c_a_resend (SR_CX *cp)
{
	SR_NOTIFY	*np;

	/* Time-out on NotifySuccess/Failure response: retry! */
	if (!--cp->retries)
		return;

	np = cp->act_np->notification;
	bgcp_send_notify_request (cp,
				  c_timeout,
				  NREQ_TO,
				  1,
				  str_ptr (np->topic),
				  str_ptr (np->type));
}

static void c_a_notfail (SR_CX *cp)
{
	if (!cp->act_np)
		return;

	log_printf (BGNS_ID, 0, "BGCP(C): Stop timer!\r\n");
	tmr_stop (cp->timer);
	tmr_free (cp->timer);
	cp->timer = NULL;
	cp->act_np = NULL;

	log_printf (BGNS_ID, 0, "BGCP(C): Notification rejected by server!\r\n");
}

static void c_a_notsucc (SR_CX *cp)
{
	SR_NOTIFY	*np;
	CtrlInfo_t	*info = cp->cxp->data;

	if (!cp->act_np)
		return;

	cp->act_np->length = info->cookie_length;
	if (info->cookie_length)
		memcpy (cp->act_np->cookie, info->cookie, info->cookie_length);
	if (cp->timer) {
		log_printf (BGNS_ID, 0, "BGCP(C): Stop timer!\r\n");
		tmr_stop (cp->timer);
	}
	cp->act_np = LIST_NEXT (cp->notifications, *cp->act_np);
	if (!cp->act_np) {
		if (cp->timer) {
			tmr_free (cp->timer);
			cp->timer = NULL;
		}
		log_printf (BGNS_ID, 0, "BGCP(C:%u): Final notification request success!\r\n", cp->cxp->fd);
		if (connect_fct)
			(*connect_fct) (cp->cxp->fd, 1);
		if (client_suspend)
			bgcp_send_suspend (cp, 1);
		return;
	}
	np = cp->act_np->notification;
	bgcp_send_notify_request (cp,
				  c_timeout,
				  NREQ_TO,
				  1,
				  str_ptr (np->topic),
				  str_ptr (np->type));
}

static void c_a_wakeup (SR_CX *cp)
{
	CtrlInfo_t	*info;

	info = cp->cxp->data;
	if (cp->wakeup_fct)
		(*cp->wakeup_fct) (cp->user,
				   info->topic_name,
				   info->type_name,
				   info->prefix.prefix);
}

static CFCT c_fsm [C_NEVENTS][C_NSTATES] = {
		/* IDLE		WCXOK		WIBINDOK	CONTROL */
/*START   */  {	c_i_start,	NULL,		NULL,		NULL	    },
/*STOP    */  {	NULL,		c_wc_stop,	c_wb_stop,	c_a_stop    },
/*CXERR   */  {	NULL,		c_wc_cxerr,	c_wb_cxerr,	c_a_cxerr   },
/*TO      */  {	NULL,		c_wc_to,	c_wb_to,	c_a_resend  },
/*FINALIZE*/  {	NULL,		NULL,		c_wb_final,	c_a_final   },
/*BINDFAIL*/  {	NULL,		NULL,		c_wb_bfail,	NULL	    },
/*BINDSUCC*/  {	NULL,		NULL,		c_wb_bsucc,	NULL	    },
/*NOTFAIL */  {	NULL,		NULL,		NULL,		c_a_notfail },
/*NOTSUCC */  {	NULL,		NULL,		NULL,		c_a_notsucc },
/*WAKEUP  */  {	NULL,		NULL,		NULL,		c_a_wakeup  }
};

static void c_timeout (uintptr_t user)
{
	SR_CX	*cp = (SR_CX *) user;
	CFCT	fct;

	log_printf (BGNS_ID, 0, "BGCP(C): timeout!\r\n");
	if (cp->state == TNS_IDLE)
		fct = c_fsm [CE_START][cp->state];
	else
		fct = c_fsm [CE_TO][cp->state];
	if (fct)
		(*fct) (cp);
}

static void c_on_close (IP_CX *cxp)
{
	CFCT	fct;
	SR_CX	*cp;

	if (!TABLE_ITEM_EXISTS (cx_table, cxp->user))
		return;

	cp = TABLE_ITEM (cx_table, cxp->user);
	fct = c_fsm [CE_CXERR][cp->state];
	if (fct)
		fct (cp);
}

static void c_start (SR_CX *cp)
{
	c_fsm [CE_START][TNS_IDLE] (cp);
}

static int c_control (IP_CX *cxp, const unsigned char *msg, size_t size)
{
	SR_CX		*cp;
	CtrlInfo_t	info;
	C_EVENT		e;
	CFCT		fct;
	uint32_t	tid;

	if (!cxp->user || !TABLE_ITEM_EXISTS (cx_table, cxp->user))
		return (0);

	cp = TABLE_ITEM (cx_table, cxp->user);
	if (cp->cxp != cxp)
		return (0);

	bgcp_parse (cxp->fd, msg, size, &info);
	if (info.result) {
		log_printf (BGNS_ID, 0, "BGCP(CC): message error (%u)!\r\n", info.result);
		/*info_cleanup (&info);*/
		if (cp->type < SRC_C_TCP)
			c_reconnect (cp, 1);
		return (0);
	}

	/* Check message type. */
	ADD_ULLONG (cxp->stats.octets_rcvd, size);
	cxp->stats.packets_rcvd++;
	memcpy (&tid, info.transaction + 8, 4);
	cxp->data = &info;
	cxp->data_length = 0;
	switch (info.type) {
		case CMT_BIND_SUCC:
			/*log_printf (BGNS_ID, 0, "BGCP(CC): BindSuccess received.\r\n");*/
			if (tid == cp->transaction_id) {
				cp->rprefix = info.prefix;
				cxp->dst_prefix = info.prefix;
				cxp->has_prefix = 1;
				fct = c_fsm [CE_BINDSUCC][cp->state];
				if (fct)
					(*fct) (cp);
			}
			else
				log_printf (BGNS_ID, 0, "BGCP(CC): ignoring IdentityBindSuccess (wrong id).\r\n");
			break;
		case CMT_BIND_FAIL:
			/*log_printf (BGNS_ID, 0, "BGCP(CC): BindFail received.\r\n");*/
			if (tid == cp->transaction_id) {
				fct = c_fsm [CE_BINDFAIL][cp->state];
				if (fct) {
					(*fct) (cp);
					/*info_cleanup (&info);*/
                                        cxp->data = NULL;
                                        cxp->data_length = 0;
					return (0);
				}
			}
			else
				log_printf (BGNS_ID, 0, "BGCP(CC): ignoring BindFail (wrong id).\r\n");
			break;
		case CMT_NOTIFY_SUCC:
		case CMT_NOTIFY_FAIL:
			/*if (info.type == CMT_NOTIFY_SUCC)
				log_printf (BGNS_ID, 0, "BGCP(CC): NotifySuccess received.\r\n");
			else
				log_printf (BGNS_ID, 0, "BGCP(CC): NotifyFail received.\r\n");*/

			if (cp->state == TNS_NOTIFY) {
				cp->transaction_id = tid;
				if (info.type == CMT_NOTIFY_SUCC)
					e = CE_NOTSUCC;
				else
					e = CE_NOTFAIL;
				fct = c_fsm [e][TNS_NOTIFY];
				if (fct)
					(*fct) (cp);
			}
			break;
		case CMT_WAKEUP:
			/*log_printf (BGNS_ID, 0, "BGCP(CC): Wakeup received.\r\n");*/
			fct = c_fsm [CE_WAKEUP][cp->state];
			if (fct)
				(*fct) (cp);
			break;
		case CMT_FINALIZE:
			/*log_printf (BGNS_ID, 0, "BGCP(CC): Finalize received.\r\n");*/
			fct = c_fsm [CE_FINALIZE][cp->state];
			if (fct)
				(*fct) (cp);
			/*info_cleanup (&info);*/
                        cxp->data = NULL;
                        cxp->data_length = 0;
			return (0);
		default:
			log_printf (BGNS_ID, 0, "BGCP(CC): Unexpected message (%u) received!\r\n", info.type);
	  		break;
	}
	/*info_cleanup (&info);*/
        cxp->data = NULL;
        cxp->data_length = 0;
	return (1);
}

/* bgcp_notification_add -- Send a notification request to the peer server. */

static void bgcp_notification_add (SR_CX *cp, int enable, SR_NOTIFY *np)
{
	SR_NOTIFY_DATA	*ndp;

	ndp = xmalloc (sizeof (SR_NOTIFY_DATA));
	if (!ndp)
		return;

	ndp->notification = np;
	ndp->enable = enable;
	np->nusers++;
	ndp->length = 0;
	LIST_ADD_TAIL (cp->notifications, *ndp);
	if (!cp->act_np) {
		cp->act_np = ndp;
		if (cp->state == TNS_NOTIFY) {
			cp->retries = NREQ_RETRIES;
			bgcp_send_notify_request (cp,
						  c_timeout,
						  NREQ_TO,
						  enable,
						  str_ptr (np->topic),
						  str_ptr (np->type));
		}
	}
}

static void bgcp_server_notify (SR_CX *cp, CtrlInfo_t *ip);
static void bgcp_server_suspend (SR_CX *cp, int enable);
static void bgcp_server_ctrl_stop (SR_CX *cp);

static void bgcp_add_notifications (SR_CX *cp)
{
	SR_NOTIFY	*np;

	if (LIST_NONEMPTY (notify_list))
		LIST_FOREACH (notify_list, np)
			if (np->domain_id == cp->domain_id)
				bgcp_notification_add (cp, 1, np);
}

static SR_CX *bgcp_lookup (GuidPrefix_t *gp)
{
	SR_CX	*cp;

	LIST_FOREACH (cx_list, cp)
		if (guid_prefix_eq (cp->rprefix, *gp))
			return (cp);

	return (NULL);
}

static void bgcp_duplicate_cx (SR_CX *old_cp, SR_CX *new_cp)
{
	IP_CX	*cxp;

	log_printf (BGNS_ID, 0, "BGCP: Bind() indicates duplicate client connection!\r\n");

	/* Swap connections of both contexts. */
	cxp = old_cp->cxp;
	old_cp->cxp = new_cp->cxp;
	new_cp->cxp = cxp;

	/* Signal user that old connection got destroyed. */
	if (connect_fct && cxp->fd_owner)
		(*connect_fct) (cxp->fd, 0);

	/* Delete the new context. */
	bgcp_stop_x (new_cp->handle);
}

/* bgcp_ll_event -- Process a lower-layer connection event or received message. */

static void bgcp_ll_event (uintptr_t           user,
			   TCP_SR_EVENT        event,
			   IP_CX               *cxp,
			   const unsigned char *msg,
			   size_t              length)
{
	SR_CX		*pcp, *cp, *xcp;
	CtrlInfo_t	info;
	Domain_t	*dp;
	void		*u;

	tcx_print3 ("BGCP: ll_event (%p, %s, %p);\r\n", (void *) user, (!event) ? "OPEN" : (event == SRN_CLOSED) ? "CLOSED" : "DATA", (void *) cxp);

	if (!TABLE_ITEM_EXISTS (cx_table, (unsigned) user))
		return;

	pcp = TABLE_ITEM (cx_table, (unsigned) user);
	for (cp = pcp->derived; cp; cp = cp->derived)
		if (cp->cxp == cxp &&
		    (cp->type == SRC_S_TCP || cp->type == SRC_C_TCP))
			break;

	if (!cp && event == SRN_OPEN) {
		cp = xmalloc (sizeof (SR_CX));
		if (!cp) {
			log_printf (BGNS_ID, 0, "BGCP: not enough memory for passive context!\r\n");
			return;
		}
		cp->handle = table_add (&cx_table);
		if (!cp->handle) {
			log_printf (BGNS_ID, 0, "BGCP: not enough room for passive handle!\r\n");
			xfree (cp);
			return;
		}
		cp->root = pcp;
		cp->derived = pcp->derived;
		pcp->derived = cp;
		cp->type = (pcp->type == SRC_SERVER) ? SRC_S_TCP : SRC_C_TCP;
		cp->domain_id = pcp->domain_id;
		cp->index = pcp->index;
		cp->rserver = pcp->rserver;
		cp->port = pcp->port;
		cp->ipv6 = pcp->ipv6;
		cp->secure = pcp->secure;
		cp->active = 0; /*pcp->type != SRC_SERVER;*/
		cp->suspended = 0;
		cp->match_fct = pcp->match_fct;
		cp->wakeup_fct = pcp->wakeup_fct;
		cp->notify_fct = pcp->notify_fct;
		cp->suspend_fct = pcp->suspend_fct;
		cp->user = pcp->user;
		memset (&cp->rprefix, 0, sizeof (cp->rprefix));
		cp->state = TNS_IDLE;
		cp->act_np = NULL;
		LIST_INIT (cp->notifications);
		cp->retries = 0;
		cp->timer = NULL;
		cp->cxp = cxp;
		TABLE_ITEM (cx_table, cp->handle) = cp;
		LIST_ADD_TAIL (cx_list, *cp);
		cxp->user = cp->handle;
		tcx_print1 ("BGCP: embedded %s context opened.\r\n", (cp->type == SRC_C_TCP) ? "client" : "server");
		if (cp->type == SRC_C_TCP) {
			bgcp_add_notifications (cp);
			c_connected (cxp);
		}
	}
	else if (cp && event == SRN_CLOSED) {
		tcx_print1 ("BGCP: embedded %s context closed.\r\n", (cp->type == SRC_C_TCP) ? "client" : "server");
		if (cp->active && cp->type == SRC_S_TCP)
			(*cp->match_fct) (0, cp->user, &cp->rprefix, &u);
		if (connect_fct && cxp->fd_owner)
			(*connect_fct) (cxp->fd, 0);
		bgcp_stop_x (cp->handle);
	}
	else if (cp && event == SRN_DATA) {
		if (cp->type == SRC_S_TCP) {
			bgcp_parse (cxp->fd, msg, length, &info);
			if (info.result) {
				log_printf (BGNS_ID, 0, "BGCP(S): message error!\r\n");
				return;
			}

			/* Check message type. */
			switch (info.type) {
				case CMT_BIND_REQ:
					dp = domain_lookup (cp->domain_id);
					if (!dp) {
						log_printf (BGNS_ID, 0, "BGCP: Domain (%u) deleted!\r\n", cp->domain_id);
						return;
					}
					if (bgcp_send_bind_success (cp, &info,
						    &dp->participant.p_guid_prefix) ==
								DDS_RETCODE_ALREADY_DELETED)
						return;

					if ((xcp = bgcp_lookup (&info.prefix)) != NULL) {
						if (xcp->suspended) { /* Implicit resume due to renewed Cx! */
							bgcp_server_suspend (xcp, 0);
							if (xcp->cxp)
								rtps_tcp_srn_suspend (xcp->cxp, 0);
						}
						if (xcp->cxp != cxp)
							bgcp_duplicate_cx (xcp, cp);
					}
					else {
						cp->active = 1;
						cp->rprefix = info.prefix;
						(*cp->match_fct) (1, cp->user, &cp->rprefix, &u);
						cp->user = (uintptr_t) u;
					}
					break;

				case CMT_NOTIFY_REQ:
					if (cp->active)
						bgcp_server_notify (cp, &info);
					break;

				case CMT_SUSPENDING:
					if (cp->active) {
						bgcp_server_suspend (cp, 1);
						rtps_tcp_srn_suspend (cxp, 1);
					}
					break;

				case CMT_RESUMING:
					if (cp->active) {
						bgcp_server_suspend (cp, 0);
						rtps_tcp_srn_suspend (cxp, 0);
					}
					break;

				case CMT_FINALIZE:
					bgcp_server_ctrl_stop (cp);
					return;

				default:
					log_printf (BGNS_ID, 0, "BGCP(S): Unexpected message (%u) received!\r\n", info.type);
			  		break;
			}
		}
		else if (cp->type == SRC_C_TCP)
			c_control (cxp, msg, length);
	}
}

/* bgcp_control_ready -- Control connection had a successful bind. */

static void bgcp_control_ready (SR_CX *cp)
{
	SR_NOTIFY	*np;

	cp->active = 1;
	if (LIST_EMPTY (cp->notifications))
		cp->act_np = NULL;
	else
		cp->act_np = LIST_HEAD (cp->notifications);
	if (cp->act_np) {
		np = cp->act_np->notification;
		cp->retries = NREQ_RETRIES;
		bgcp_send_notify_request (cp,
					  c_timeout,
					  NREQ_TO,
					  1,
					  str_ptr (np->topic),
					  str_ptr (np->type));
	}
	else {
		log_printf (BGNS_ID, 0, "BGCP(C:%u): No notifications pending!\r\n", cp->cxp->fd);
		if (connect_fct)
			(*connect_fct) (cp->cxp->fd, 1);
		if (client_suspend)
			bgcp_send_suspend (cp, 1);
	}
}

/* bgcp_connect -- Open a new connection to a specified peer.  If successfully
		   created, a new IP context will be returned. */

IP_CX *bgcp_connect (RTPS_TCP_RSERV *sp, SR_CX *cp, Ticks_t delay)
{
	static STREAM_CB	channel_cb = {
		NULL,			/* on_new_connection (NA) */
		c_connected,		/* on_connected */
		NULL,			/* on_write_completed (NA) */
		c_control,		/* on_new_message */
		c_on_close		/* on_close */
	};
	IP_CX			*cxp;
#ifdef DDS_IPV6
	struct addrinfo		hints, *res, *rp, *ip4res;
	int			nat64, s, ipv6;
	struct sockaddr_in	*sa;
	struct sockaddr_in6	*sa6;
	struct in6_addr		addr6;
	static char		sbuf [40];
#else
	struct hostent		*he;
#endif
	struct in_addr		addr;
	unsigned		i;

	log_printf (BGNS_ID, 0, "BGCP: Client start.\r\n");
	cxp = Alloc (sizeof (IP_CX));
	if (!cxp) {
		log_printf (BGNS_ID, 0, "BGCP: connect: out of IP contexts!\r\n");
		return (NULL);
	}
	memset (cxp, 0, sizeof (IP_CX));
#ifdef DDS_SECURITY
	if (sp->secure) {
		cxp->stream_fcts = &tls_functions;
		cxp->cx_type = CXT_TCP_TLS;
	}
	else {
#endif
		cxp->stream_fcts = &tcp_functions;
		cxp->cx_type = CXT_TCP;
#ifdef DDS_SECURITY
	}
#endif
	cxp->stream_cb = &channel_cb;
	cxp->cx_side = ICS_CLIENT;
	cxp->cx_mode = ICM_NOTIFY;
	cxp->cx_state = CXS_CLOSED;
	cxp->p_state = cp->state = TNS_IDLE;
	cxp->locator = xmalloc (sizeof (LocatorNode_t));;
	if (!cxp->locator) {
		warn_printf ("BGCP: not enough memory for BGCP Client locator!");
		goto free_cx;
	}
	cxp->locator->users = 0;
	memset (&cxp->locator->locator, 0, sizeof (Locator_t));
#ifdef DDS_SECURITY
	if (sp->secure) {
		cxp->locator->locator.flags |= LOCF_SECURE;
		cxp->locator->locator.sproto = SECC_TLS_TCP;
	}
#endif
	cxp->user = cp->handle;
	cxp->id = cp->index;
	cp->timer = tmr_alloc ();
	if (!cp->timer) {
		warn_printf ("BGCP: not enough memory for BGCP Client timer!");
		goto free_cx;
	}
	tmr_init (cp->timer, "BGCP-Client");
#ifdef DDS_IPV6
	nat64 = rtps_ipv6_nat64_required ();
#endif
	if (sp->name) {
#ifdef DDS_IPV6
		memset (&hints, 0, sizeof (struct addrinfo));
		hints.ai_family = AF_UNSPEC;	/* Allow IPv4 or IPv6. */
		hints.ai_socktype = SOCK_STREAM;
		hints.ai_flags = 0;
		hints.ai_protocol = 0;
		sprintf (sbuf, "%u", sp->port);
		ipv6 = 0;
		ip4res = NULL;
		s = getaddrinfo (sp->addr.name, sbuf, &hints, &res);
		if (!s) {
			for (rp = res; rp; rp = rp->ai_next)
				if (rp->ai_family == AF_INET)
					if (nat64) {
						ip4res = rp;
						continue;
					}
					else {
						ipv6 = 0;
						sa = (struct sockaddr_in *) rp->ai_addr;
						addr = sa->sin_addr;
						if (ipv4_proto.enabled)
							break;
					}
				else if (rp->ai_family == AF_INET6) {
					ipv6 = 1;
					sa6 = (struct sockaddr_in6 *) rp->ai_addr;
					addr6 = sa6->sin6_addr;
					if (ipv6_proto.enabled)
						break;
				}
		}
		else
			rp = NULL;
		if (!rp && ip4res) {
			rp = ip4res;
			sa = (struct sockaddr_in *) rp->ai_addr;
			ipv6 = 0;
			addr = sa->sin_addr;
		}
		freeaddrinfo (res);
		if (s || !rp) {
			warn_printf ("BGCP: server name could not be resolved!");
			goto free_timer;
		}
		log_printf (BGNS_ID, 0, "BGCP: server name resolved to %s\r\n", 
				          inet_ntop ((ipv6) ? AF_INET6 : AF_INET,
					  	     (ipv6) ? (void *) &addr6 : (void *) &addr,
						     sbuf, sizeof (sbuf)));
#else
		he = gethostbyname (sp->addr.name);
		if (!he || he->h_addrtype != AF_INET) {
			warn_printf ("BGCP: server name could not be resolved!");
			goto free_timer;
		}
		addr = *((struct in_addr *) he->h_addr_list [0]);
		log_printf (BGNS_ID, 0, "BGCP: server name resolved to %s\r\n",
						          inet_ntoa (addr));
#endif
	}
#ifdef DDS_IPV6
	else if (sp->ipv6) {
		memcpy (addr6.s6_addr, sp->addr.ipa_v6, 16);
		ipv6 = 1;
	}
#endif
	else {
		addr.s_addr = htonl (sp->addr.ipa_v4);
#ifdef DDS_IPV6
		ipv6 = 0;
#endif
	}

#ifdef DDS_IPV6
	if (!ipv6 && nat64) {
		rtps_ipv6_nat64_addr (addr.s_addr, addr6.s6_addr);
		ipv6 = 1;
	}
	if (ipv6) {
		cxp->locator->locator.kind = LOCATOR_KIND_TCPv6;
		memcpy (cxp->dst_addr, addr6.s6_addr, 16);
		log_printf (BGNS_ID, 0, "BGCP: connecting to %s", 
			          inet_ntop (AF_INET6, (void *) &addr6,
					     sbuf, sizeof (sbuf)));
	}
	else
#endif
	     {
       		cxp->locator->locator.kind = LOCATOR_KIND_TCPv4;
		cxp->dst_addr [12] = ntohl (addr.s_addr) >> 24;
		cxp->dst_addr [13] = (ntohl (addr.s_addr) >> 16) & 0xff;
		cxp->dst_addr [14] = (ntohl (addr.s_addr) >> 8) & 0xff;
		cxp->dst_addr [15] = ntohl (addr.s_addr) & 0xff;
		log_printf (BGNS_ID, 0, "BGCP: connecting to %s", 
						          inet_ntoa (addr));
	}
	log_printf (BGNS_ID, 0, ":%u\r\n", sp->port);
	cxp->associated = 1;
	cxp->dst_port = sp->port;
	cxp->has_dst_addr = 1;
	for (i = 0; i < BG_MAX_CLIENTS; i++)
		if (!bg_client [i]) {
			bg_client [i] = cxp;
			break;
		}

	bgcp_backoff = 0;
	/*log_printf (BGNS_ID, 0, "BGCP: backoff = %u\r\n", bgcp_backoff);*/
	if (delay) {
		log_printf (BGNS_ID, 0, "BGCP(%u): timer started!\r\n", cxp->handle);
		tmr_start (cp->timer, delay, (uintptr_t) cp, c_timeout);
	}
	else
		c_start (cp);
	return (cxp);

    free_timer:
    	tmr_free (cp->timer);
	cp->timer = NULL;
    free_cx:
	rtps_ip_free (cxp);
	return (NULL);
}

static void bgcp_cleanup_ll (SR_CX *cp)
{
	IP_CX	*cxp;

	if ((cxp = cp->cxp) != NULL) {
		cxp->user = 0;
		tcx_print2 ("BGCP: close %p (type %u).\r\n",
						(void *) cxp, cp->type);
		bgcp_close_fd (cxp);
		cp->cxp = NULL;
	}
	if (cp->timer) {
		log_printf (BGNS_ID, 0, "BGCP: Stop timer!\r\n");
		tmr_stop (cp->timer);
		tmr_free (cp->timer);
		cp->timer = NULL;
	}
}

static void bgcp_client_domain_active (SR_CX *cp)
{
	if (cp->type == SRC_CLIENT_CX && !cp->cxp) {
		cp->cxp = bgcp_connect (&cp->rserver, cp, 1);
		if (cp->cxp)
			bgcp_add_notifications (cp);
	}
	else
		rtps_tcp_srn_attach (0, bgcp_ll_event, cp->handle);
}

static void bgcp_client_domain_inactive (SR_CX *cp)
{
	if (cp->cxp)
		bgcp_cleanup_ll (cp);
}

/* bgcp_start_client -- Start a notification client for the given URL or piggy-
			backed on the RTPS/TCP control channel (url == NULL). */

int bgcp_start_client (RTPS_TCP_RSERV   *dest,
		       unsigned         domain_id,
		       SRN_MATCH        match_fct,
		       SRN_WAKEUP       wakeup_fct,
		       uintptr_t        user,
		       DDS_ReturnCode_t *error)
{
	Domain_t	*dp;
	SR_CX		*cp;

	/* Create a new connection context. */
	lock_take (bgcp_lock);
	cp = xmalloc (sizeof (SR_CX));
	if (!cp) {
		log_printf (BGNS_ID, 0, "bgcp_start_client: no memory for connection!\r\n");
		*error = DDS_RETCODE_OUT_OF_RESOURCES;
		lock_release (bgcp_lock);
		return (0);
	}
	cp->handle = table_add (&cx_table);
	if (!cp->handle) {
		xfree (cp);
		log_printf (BGNS_ID, 0, "bgcp_start_client: cant't create handle!\r\n");
		*error = DDS_RETCODE_OUT_OF_RESOURCES;
		lock_release (bgcp_lock);
		return (0);
	}
	cp->root = cp->derived = NULL;
	cp->type = (dest) ? SRC_CLIENT_CX : SRC_CLIENT;
	cp->domain_id = domain_id;
	dp = domain_lookup (domain_id);
	cp->index = (dp) ? dp->index : 0;
	if (dest) {
		cp->rserver = *dest;
		cp->ipv6 = dest->ipv6;
	}
	else {
		memset (&cp->rserver, 0, sizeof (cp->rserver));
		cp->ipv6 = 0;
	}
	cp->port = 0;
	cp->active = 0;
	cp->suspended = 0;
	cp->match_fct = match_fct;
	cp->wakeup_fct = wakeup_fct;
	cp->notify_fct = NULL;
	cp->suspend_fct = NULL;
	memset (&cp->rprefix, 0, sizeof (cp->rprefix));
	cp->user = user;
	cp->state = TNS_IDLE;
	cp->act_np = NULL;
	LIST_INIT (cp->notifications);
	cp->retries = 0;
	cp->timer = 0;
	cp->cxp = NULL;
	TABLE_ITEM (cx_table, cp->handle) = cp;
	LIST_ADD_TAIL (cx_list, *cp);

	/* Lower-layer connection establishment. */
	if (dp)
		bgcp_client_domain_active (cp);

#ifdef DDS_IPV6
	if (cp->ipv6)
		bgcp_v6_active = 1;
	else
#endif
		bgcp_v4_active = 1;

	lock_release (bgcp_lock);
	return (cp->handle);
}

/**********************************************************************/
/*   BGCP Server Control logic.					      */
/**********************************************************************/

static void bgcp_server_ctrl_stop (SR_CX *cp)
{
	log_printf (BGNS_ID, 0, "TCP(S): Finalize received.\r\n");
	bgcp_close_fd (cp->cxp);
	log_printf (BGNS_ID, 0, "BGCP(S): stop!\r\n");
}

static void bgcp_server_close_fd (IP_CX *cxp)
{
	bgcp_close_fd (cxp);
}

static void bgcp_notify_unref (SR_NOTIFY *np)
{
	if (!--np->nusers) {
		LIST_REMOVE (notify_list, *np);
		str_unref (np->topic);
		str_unref (np->type);
		xfree (np);
	}
}

static SR_NOTIFY_DATA *bgcp_notify_add (SR_ACT_NOTIFY *list,
					unsigned      domain_id,
					const char    *topic,
					const char    *type,
					const char    *cookie,
					size_t        cookie_length)
{
	SR_NOTIFY_DATA	*ndp;
	SR_NOTIFY	*np;

	LIST_FOREACH (*list, ndp)
		if (!strcmp (topic, str_ptr (ndp->notification->topic)) &&
		    !strcmp (type, str_ptr (ndp->notification->type)))
			return (ndp);

	LIST_FOREACH (notify_list, np)
		if (np->domain_id == domain_id &&
		    !strcmp (topic, str_ptr (np->topic)) &&
		    !strcmp (type, str_ptr (np->type)))
			break;

	if (LIST_END (notify_list, np)) {
		np = xmalloc (sizeof (SR_NOTIFY));
		if (!np)
			return (NULL);

		np->domain_id = domain_id;
		np->topic = str_new_cstr (topic);
		if (!np->topic) {
			xfree (np);
			return (NULL);
		}
		np->type = str_new_cstr (type);
		if (!np->type) {
			str_unref (np->topic);
			xfree (np);
			return (NULL);
		}
		np->user = 0;
		np->nusers = 1;
		LIST_ADD_TAIL (notify_list, *np);
	}
	else
		np->nusers++;

	ndp = xmalloc (sizeof (SR_NOTIFY_DATA));
	if (!ndp) {
		bgcp_notify_unref (np);
		return (NULL);
	}
	ndp->enable = 1;
	ndp->notification = np;
	memcpy (ndp->cookie, cookie, cookie_length);
	ndp->length = cookie_length;
	LIST_ADD_TAIL (*list, *ndp);
	return (ndp);
}

static SR_NOTIFY_DATA *bgcp_notify_lookup (SR_ACT_NOTIFY *list,
					   const char    *topic,
					   const char    *type)
{
	SR_NOTIFY_DATA	*np;

	LIST_FOREACH (*list, np)
		if (!strcmp (topic, str_ptr (np->notification->topic)) &&
		    !strcmp (type, str_ptr (np->notification->type)))
			return (np);

	return (NULL);
}

static void bgcp_notify_remove (SR_ACT_NOTIFY *list, SR_NOTIFY_DATA *np)
{
	ARG_NOT_USED (list)

	LIST_REMOVE (*list, *np);
	bgcp_notify_unref (np->notification);
	xfree (np);
}

static void bgcp_server_notify (SR_CX *cp, CtrlInfo_t *ip)
{
	SR_NOTIFY_DATA	*np;
	uintptr_t	cookie;

	log_printf (BGNS_ID, 0, "BGCP: NotifyRequest received.\r\n");
	if (!cp)
		return;

	np = bgcp_notify_lookup (&cp->notifications, ip->topic_name, ip->type_name);
	if (!ip->flags && !np) {
		bgcp_send_notify_success (cp, ip, NULL, 0);
		return;
	}
	if (np && ip->flags) {
		bgcp_send_notify_success (cp, ip, (unsigned char *) &np->cookie, np->length);
		return;
	}
	if (cp->notify_fct)
		if ((*cp->notify_fct) (ip->flags,
				       cp->user,
				       (uintptr_t) cp,
				       ip->topic_name,
				       ip->type_name,
				       &cookie)) {
			if (np && !ip->flags) {
				bgcp_send_notify_success (cp,
							  ip,
							  (unsigned char *) &cookie,
							  sizeof (cookie));
				bgcp_notify_remove (&cp->notifications, np);
			}
			else if (bgcp_notify_add (&cp->notifications,
						  cp->domain_id,
						  ip->topic_name,
						  ip->type_name,
						  (char *) &cookie,
						  sizeof (cookie)))
				bgcp_send_notify_success (cp,
							  ip,
							  (unsigned char *) &cookie,
							  sizeof (cookie));
			else
				bgcp_send_notify_fail (cp, ip, BGCP_ERR_NOMEM);
		}
		else
			bgcp_send_notify_fail (cp, ip, (unsigned) cookie);
	else
		bgcp_send_notify_fail (cp, ip, CERR_NO_SERVER);
}

/* bgcp_match -- Return a non-0 result if a topic/type name matches the list of
		 requested notifications on a server for a specific client. */

int bgcp_match (void *cx, const char *topic_name, const char *type_name)
{
	SR_CX		*cp = (SR_CX *) cx;
	SR_NOTIFY_DATA	*anp;
	SR_NOTIFY	*np;

	LIST_FOREACH (cp->notifications, anp) {
		np = anp->notification;
		if (!nmatch (str_ptr (np->topic), topic_name, 0) &&
		    !nmatch (str_ptr (np->type), type_name, 0))
			return (1);
	}
	return (0);
}

static void bgcp_server_suspend (SR_CX *cp, int enable)
{
	if (enable)
		log_printf (BGNS_ID, 0, "BGCP(S): Suspending received.\r\n");
	else
		log_printf (BGNS_ID, 0, "BGCP(S): Resuming received.\r\n");
	if (cp) {
		cp->suspended = enable;
		if (cp->suspend_fct)
			(*cp->suspend_fct) (enable, cp->user, cp);
	}
}

static int bgcp_server_ctrl_in (IP_CX *cxp, const unsigned char *msg, size_t size)
{
	CtrlInfo_t	info;
	SR_CX		*cp;

	bgcp_parse (cxp->fd, msg, size, &info);
	if (info.result) {
		log_printf (BGNS_ID, 0, "BGCP(S): message error!\r\n");
		return (0);
	}

	/* Check message type. */
	ADD_ULLONG (cxp->stats.octets_rcvd, size);
	cxp->stats.packets_rcvd++;
	cp = TABLE_ITEM (cx_table, cxp->user);
	switch (info.type) {
		case CMT_NOTIFY_REQ:
			bgcp_server_notify (cp, &info);
			break;

		case CMT_SUSPENDING:
			bgcp_server_suspend (cp, 1);
			break;

		case CMT_RESUMING:
			bgcp_server_suspend (cp, 0);
			break;

		case CMT_FINALIZE:
			bgcp_server_ctrl_stop (cp);
			return (0);

		default:
			log_printf (BGNS_ID, 0, "BGCP(S): Unexpected message (%u) received!\r\n", info.type);
	  		break;
	}
	return (1);
}

static IP_CX *bgcp_server_ctrl_start (TCP_FD *pp, CtrlInfo_t *info)
{
	IP_CX			*cxp, *ccxp, *next_cxp;
	struct linger		l;
	int			ret;
	static STREAM_CB	server_ctrl_cb = {
		NULL,			/* on_new_connection */
		NULL,			/* on_connected */
		NULL,			/* on_write_completed */
		bgcp_server_ctrl_in,	/* on_new_message */
		bgcp_server_close_fd	/* on_close */
	};
	Domain_t		*dp;
	SR_CX			*cp, *pcp;
	void			*user;

	dp = domain_lookup (info->domain_id);
	if (!dp)
		return (NULL);

	cp = xmalloc (sizeof (SR_CX));
	if (!cp) {
		warn_printf ("BGCP(S): not enough memory for context!");
		return (NULL);
	}
	cp->handle = table_add (&cx_table);
	if (!cp->handle)
		goto free_cp;

	cxp = Alloc (sizeof (IP_CX));
	if (!cxp) {
		warn_printf ("BGCP(S): not enough memory for context(2)!");
		goto free_handle;
	}
	memset (cxp, 0, sizeof (IP_CX));
	cxp->locator = xmalloc (sizeof (LocatorNode_t));
	if (!cxp->locator) {
		warn_printf ("TMP(S): not enough memory for locator!");
		goto free_cx;
	}
	cxp->locator->users = 0;
	cxp->locator->locator = pp->parent->locator->locator;

	cxp->stream_fcts = pp->parent->stream_fcts; /* Inherit transport */
	cxp->stream_cb = &server_ctrl_cb;

	cxp->cx_type = pp->parent->cx_type;
	cxp->cx_side = ICS_SERVER;
	cxp->cx_mode = ICM_NOTIFY;
	BGCP_NCX_STATE ("S", cxp, CXS_OPEN);
	cxp->fd = pp->fd;
	cxp->fd_owner = 1;

	l.l_onoff = 1;
	l.l_linger = 0;
	ret = setsockopt (cxp->fd, SOL_SOCKET, SO_LINGER, &l, sizeof (l));
	if (ret)
		perror ("bgcp_server_ctrl_start(): setsockopt(SO_LINGER)");

	memcpy (cxp->dst_addr, pp->dst_addr, 16);
	cxp->dst_port = pp->dst_port;
	cxp->has_dst_addr = 1;
	cxp->dst_prefix = info->prefix;
	cxp->has_prefix = 1;
	cxp->associated = 1;
	cxp->parent = pp->parent;
	cxp->sproto = pp->sproto;

	/* Dispose other, already established control channels of client. */
	for (ccxp = cxp->parent->clients; ccxp; ccxp = next_cxp) {
		next_cxp = ccxp->next;
		if (ccxp->has_prefix && 
		    guid_prefix_eq (ccxp->dst_prefix, info->prefix))
			bgcp_close_fd (ccxp);
	}

	/* Add to list of TCP clients. */
	cxp->next = cxp->parent->clients;
	cxp->parent->clients = cxp;
	pcp = TABLE_ITEM (cx_table, cxp->parent->user);

	/* Add connection record. */
	cp->root = pcp;
	cp->derived = pcp->derived;
	pcp->derived = cp;
	cp->type = SRC_SERVER_CX;
	cp->domain_id = info->domain_id;
	cp->index = dp->index;
	cp->rserver = pcp->rserver;
	cp->port = pcp->port;
	cp->ipv6 = pcp->ipv6;
	cp->active = 1;
	cp->suspended = 0;
	cp->match_fct = pcp->match_fct;
	cp->wakeup_fct = pcp->wakeup_fct;
	cp->notify_fct = pcp->notify_fct;
	cp->suspend_fct = pcp->suspend_fct;
	cp->user = pcp->user;
	cp->act_np = NULL;
	LIST_INIT (cp->notifications);
	cp->retries = 0;
	cp->timer = NULL;
	cp->cxp = cxp;
	cp->state = TNS_IDLE;
	cp->rprefix = info->prefix;
	BGCP_NP_STATE ("S", cp, TNS_NOTIFY);
	TABLE_ITEM (cx_table, cp->handle) = cp;
	LIST_ADD_TAIL (cx_list, *cp);
	cxp->user = cp->handle;

	/* Indicate successful binding. */
	if (bgcp_send_bind_success (cp,
				    info,
				    &dp->participant.p_guid_prefix) == DDS_RETCODE_ALREADY_DELETED)
		return (NULL);

	(*cp->match_fct) (1, cp->user, &cp->rprefix, &user);
	cp->user = (uintptr_t) user;
	log_printf (BGNS_ID, 0, "BGCP(S): control connection ready!\r\n");

	return (cxp);

    free_cx:
	rtps_ip_free (cxp);
    free_handle:
	table_remove (&cx_table, cp->handle);
    free_cp:
	xfree (cp);
	return (NULL);
}

static IP_CX *bgcp_pending_in (TCP_FD *pp, const unsigned char *msg, size_t size)
{
	SR_CX		*cp;
	IP_CX		*cxp;
	CtrlInfo_t	info;

	bgcp_parse (pp->fd, msg, size, &info);
	if (info.result) {
		log_printf (BGNS_ID, 0, "BGCP(Sp): message error!\r\n");
		return (NULL);
	}

	/* Check message type. */
	cxp = NULL;
	switch (info.type) {
		case CMT_BIND_REQ:
			LIST_FOREACH (cx_list, cp)
				if (cp->type == SRC_SERVER &&
				    cp->domain_id == info.domain_id &&
				    cp->port) {
					cxp = bgcp_server_ctrl_start (pp, &info);
					if (cxp) {
						ADD_ULLONG (cxp->stats.octets_rcvd, (unsigned) size);
						cxp->stats.packets_rcvd++;
					}
					break;
				}
			break;

		case CMT_FINALIZE:
			break;

		default:
			log_printf (BGNS_ID, 0, "BGCP(Sp): Unexpected message (%u) received!\r\n", info.type);
	  		break;
	}
	return (cxp);
}

static void bgcp_tcp_server_timeout (uintptr_t user)
{
	SR_CX	*cp = (SR_CX *) user;

	if (cp->cxp &&
	    cp->cxp->stream_fcts &&
	    cp->cxp->stream_fcts->start_server (cp->cxp)) {
		tmr_start (cp->timer, TICKS_PER_SEC * 5, (uintptr_t) cp, bgcp_tcp_server_timeout);
		return;
	}
	if (cp->timer) {
		tmr_free (cp->timer);
		cp->timer = NULL;
	}
}

/* bgcp_serve -- Start a new server via a dedicated connection.  If an error
		 occurs, NULL will be returned. */

static IP_CX *bgcp_serve (unsigned         uport,
			  int              ipv6,
		          int              secure,
			  DDS_ReturnCode_t *error)
{
	IP_CX			*cxp;
	static STREAM_CB	server_cb = {
		bgcp_pending_in,	/* on_new_connection */
		NULL,			/* on_connected */
		NULL,			/* on_write_completed */
		NULL,			/* on_new_message */
		NULL			/* on_close */
	};

#ifndef DDS_IPV6
	ARG_NOT_USED (ipv6)
#endif
	log_printf (BGNS_ID, 0, "BGCP: start %sserver on port %u.\r\n",
					(secure) ? "secure " : "", uport);
	cxp = Alloc (sizeof (IP_CX));
	if (!cxp) {
		log_printf (BGNS_ID, 0, "bgcp_serve: out of contexts!\r\n");
		*error = DDS_RETCODE_OUT_OF_RESOURCES;
		return (NULL);
	}
	memset (cxp, 0, sizeof (IP_CX));
	cxp->locator = xmalloc (sizeof (LocatorNode_t));
	if (!cxp->locator) {
		warn_printf ("bgcp_serve: out of memory for locator.");
		rtps_ip_free (cxp);
		*error = DDS_RETCODE_OUT_OF_RESOURCES;
		return (NULL);
	}
	memset (cxp->locator, 0, sizeof (LocatorNode_t));
	cxp->locator->locator.flags = LOCF_SERVER;

#ifdef DDS_SECURITY
	if (secure) {
#ifdef TCP_SIMULATE_TLS
		cxp->stream_fcts = &tcp_functions;
#else
		cxp->stream_fcts = &tls_functions;
#endif
		cxp->locator->locator.flags |= LOCF_SECURE;
		cxp->locator->locator.sproto = SECC_TLS_TCP;
		cxp->cx_type = CXT_TCP_TLS;
	}
	else {
#endif
		cxp->stream_fcts = &tcp_functions;
		cxp->cx_type = CXT_TCP;
#ifdef DDS_SECURITY
	}
#endif
	cxp->stream_cb = &server_cb;
	cxp->cx_side = ICS_SERVER;
	cxp->cx_mode = ICM_ROOT;
	cxp->cx_state = CXS_CLOSED;

#ifdef DDS_IPV6
	if (ipv6) {
		cxp->locator->locator.kind = LOCATOR_KIND_TCPv6;
		bgv6_server = cxp;
	}
	else
#endif
	      {
		cxp->locator->locator.kind = LOCATOR_KIND_TCPv4;
		bgv4_server = cxp;
	}
	cxp->locator->locator.port = uport;

	if (!cxp->stream_fcts->start_server (cxp)) {
		*error = DDS_RETCODE_OK;
		return (cxp);
	}
	cxp->timer = tmr_alloc ();
	if (!cxp->timer) {
		xfree (cxp->locator);
		rtps_ip_free (cxp);
#ifdef DDS_IPV6
		if (ipv6)
			bgv6_server = NULL;
		else
#endif
			bgv4_server = NULL;
	}
	tmr_init (cxp->timer, "BGCP_S_RETRY");
	BGCP_NCX_STATE ("S", cxp, CXS_WRETRY);
	tmr_start (cxp->timer, TICKS_PER_SEC * 5, (uintptr_t) cxp, bgcp_tcp_server_timeout);
	*error = DDS_RETCODE_ALREADY_DELETED;
	return (cxp);
}

/* bgcp_init -- Initialize the background notification service. */

int bgcp_init (unsigned min_cx, unsigned max_cx)
{
	DDS_ReturnCode_t	error;

	LIST_INIT (cx_list);
	TABLE_INIT (cx_table);
	error = TABLE_REQUIRE (cx_table, min_cx, max_cx);
	if (error)
		return (error);

	LIST_INIT (notify_list);
	client_suspend = 0;

	lock_init_nr (bgcp_lock, "BGCP");

	return (DDS_RETCODE_OK);
}

static void bgcp_server_domain_active (SR_CX *cp)
{
	DDS_ReturnCode_t	ret;

	if (cp->port) {
		cp->cxp = bgcp_serve (cp->port, cp->ipv6, cp->secure, &ret);
		if (cp->cxp)
			cp->cxp->user = cp->handle;
	}
	else
		rtps_tcp_srn_attach (1, bgcp_ll_event, cp->handle);
}

static void bgcp_server_domain_inactive (SR_CX *cxp)
{
	bgcp_cleanup_ll (cxp);
}

int bgcp_start_server (unsigned         port,
		       int              ipv6,
		       int              secure,
		       unsigned         domain_id,
		       SRN_MATCH        match_fct,
		       SRN_NOTIFY       notify_fct,
		       SRN_SUSPEND      suspend_fct,
		       uintptr_t        user,
		       DDS_ReturnCode_t *error)
{
	Domain_t	*dp;
	SR_CX		*cp;

	lock_take (bgcp_lock);

	/* Create a new connection context. */
	cp = xmalloc (sizeof (SR_CX));
	if (!cp) {
		log_printf (BGNS_ID, 0, "bgcp_start_server: no memory for connection!\r\n");
		*error = DDS_RETCODE_OUT_OF_RESOURCES;
		lock_release (bgcp_lock);
		return (0);
	}
	memset (cp, 0, sizeof (SR_CX));
	cp->handle = table_add (&cx_table);
	if (!cp->handle) {
		xfree (cp);
		log_printf (BGNS_ID, 0, "bgcp_start_server: cant't create handle!\r\n");
		*error = DDS_RETCODE_OUT_OF_RESOURCES;
		lock_release (bgcp_lock);
		return (0);
	}
	cp->root = cp->derived = NULL;
	cp->type = SRC_SERVER;
	cp->domain_id = domain_id;
	dp = domain_lookup (domain_id);
	cp->index = (dp) ? dp->index : 0;
	cp->port = port;
	cp->ipv6 = ipv6;
	cp->secure = secure;
	cp->active = 0;
	cp->match_fct = match_fct;
	cp->notify_fct = notify_fct;
	cp->suspend_fct = suspend_fct;
	memset (&cp->rprefix, 0, sizeof (cp->rprefix));
	cp->user = user;
	cp->act_np = NULL;
	LIST_INIT (cp->notifications);
	cp->cxp = NULL;
	LIST_ADD_TAIL (cx_list, *cp);
	TABLE_ITEM (cx_table, cp->handle) = cp;

	/* Lower-layer connection establishment. */
	if (dp)
		bgcp_server_domain_active (cp);

#ifdef DDS_IPV6
	if (ipv6)
		bgcp_v6_active = 1;
	else
#endif
		bgcp_v4_active = 1;

	lock_release (bgcp_lock);
	return (cp->handle);
}

int bgcp_notify (unsigned   domain_id,
		 const char *topic_name,
		 const char *type_name,
		 uintptr_t  user)
{
	SR_NOTIFY	*np;
	SR_CX		*cp;

	lock_take (bgcp_lock);
	LIST_FOREACH (notify_list, np)
		if (np->domain_id == domain_id &&
		    !strcmp (str_ptr (np->topic), topic_name) &&
		    !strcmp (str_ptr (np->type), type_name)) {

			/* Already registered. */
			np->nusers++;
			lock_release (bgcp_lock);
			return (DDS_RETCODE_OK);
		}

	np = xmalloc (sizeof (SR_NOTIFY));
	if (!np) {
		lock_release (bgcp_lock);
		return (DDS_RETCODE_OUT_OF_RESOURCES);
	}
	np->topic = str_new_cstr (topic_name);
	if (!np->topic) {
		xfree (np);
		lock_release (bgcp_lock);
		return (DDS_RETCODE_OUT_OF_RESOURCES);
	}
	np->type = str_new_cstr (type_name);
	if (!np->type) {
		str_unref (np->topic);
		xfree (np);
		lock_release (bgcp_lock);
		return (DDS_RETCODE_OUT_OF_RESOURCES);
	}
	np->domain_id = domain_id;
	np->user = user;
	LIST_ADD_TAIL (notify_list, *np);
	np->nusers = 1;
	LIST_FOREACH (cx_list, cp)
		if ((cp->type == SRC_CLIENT_CX || cp->type == SRC_C_TCP) &&
		    cp->domain_id == domain_id)
			bgcp_notification_add (cp, 1, np);

	lock_release (bgcp_lock);
	return (DDS_RETCODE_OK);
}

int bgcp_unnotify (unsigned   domain_id,
		   const char *topic_name,
		   const char *type_name)
{
	SR_CX		*cp;
	SR_NOTIFY	*np;

	lock_take (bgcp_lock);
	LIST_FOREACH (notify_list, np)
		if (np->domain_id == domain_id &&
		    !strcmp (str_ptr (np->topic), topic_name) &&
		    !strcmp (str_ptr (np->type), type_name)) {
			LIST_FOREACH (cx_list, cp)
				if ((cp->type == SRC_CLIENT_CX ||
				     cp->type == SRC_C_TCP) &&
				    cp->domain_id == domain_id)
					bgcp_notification_add (cp, 0, np);
			if (!--np->nusers) {
				LIST_REMOVE (notify_list, *np);
				str_unref (np->topic);
				str_unref (np->type);
				xfree (np);
			}
			lock_release (bgcp_lock);
			return (DDS_RETCODE_OK);
		}

	lock_release (bgcp_lock);
	return (DDS_RETCODE_ALREADY_DELETED);
}

int bgcp_suspending (void)
{
	SR_CX	*cp;

	lock_take (bgcp_lock);
	client_suspend = 1;
	LIST_FOREACH (cx_list, cp) {
		if ((cp->type == SRC_CLIENT_CX || cp->type == SRC_C_TCP) &&
		    cp->state == TNS_NOTIFY)
			bgcp_send_suspend (cp, 1);
		cp->suspended = 1;
	}
	lock_release (bgcp_lock);
	return (DDS_RETCODE_OK);
}

int bgcp_resuming (void)
{
	SR_CX	*cp;

	lock_take (bgcp_lock);
	client_suspend = 0;
	LIST_FOREACH (cx_list, cp) {
		cp->suspended = 0;
		if ((cp->type == SRC_CLIENT_CX || cp->type == SRC_C_TCP) &&
		    cp->state == TNS_NOTIFY)
			bgcp_send_suspend (cp, 0);
	}
	lock_release (bgcp_lock);
	return (DDS_RETCODE_OK);
}

static SR_NOTIFY_DATA *bgcp_notify_match (SR_CX      *cp,
					  const char *topic_name,
					  const char *type_name)
{
	SR_NOTIFY_DATA	*ndp;

	LIST_FOREACH (cp->notifications, ndp)
		if (!nmatch (str_ptr (ndp->notification->topic), topic_name, 0) &&
		    !nmatch (str_ptr (ndp->notification->type), type_name, 0))
			return (ndp);

	return (NULL);
}

int bgcp_wakeup (void         *cx,
		 const char   *topic_name,
		 const char   *type_name,
		 GuidPrefix_t *prefix)
{
	SR_CX		*cp = (SR_CX *) cx;
	SR_NOTIFY_DATA	*ndp;

	lock_take (bgcp_lock);
	if (!topic_name && !type_name)
		bgcp_send_wakeup (cp, NULL, 0, NULL, NULL, prefix);

	else if ((ndp = bgcp_notify_match (cp, topic_name, type_name)) != NULL)
		bgcp_send_wakeup (cp,
				  ndp->cookie,
				  ndp->length,
				  topic_name,
				  type_name,
				  prefix);

	lock_release (bgcp_lock);
	return (DDS_RETCODE_OK);
}

static int bgcp_stop_x (int handle)
{
	SR_CX		*cp, *xcp, *prev_cp;
	SR_NOTIFY_DATA	*np;
	unsigned	i;

	tcx_print1 ("BGCP: stop_x(%d);\r\n", handle);

	if (!TABLE_ITEM_EXISTS (cx_table, (unsigned) handle))
		return (DDS_RETCODE_ALREADY_DELETED);

	cp = TABLE_ITEM (cx_table, handle);
	if (cp->root) {
		for (xcp = cp->root, prev_cp = NULL;
		     xcp;
		     prev_cp = xcp, xcp = xcp->derived)
			if (xcp == cp) {
				if (prev_cp)
					prev_cp->derived = cp->derived;
				else
					cp->root->derived = cp->derived;
				break;
			}
		cp->root = NULL;
	}
	bgcp_cleanup_ll (cp);
	if (cp->type == SRC_SERVER) {
		while (cp->derived)
			bgcp_stop_x (cp->derived->handle);
#ifdef DDS_IPV6
		if (cp->cxp == bgv6_server) {
			bgv6_server = NULL;
			bgcp_v6_active = 0;
		}
		else if (cp->cxp == bgv4_server)
#endif
		{
			bgv4_server = NULL;
			bgcp_v4_active = 0;
		}
	}
	else if (cp->type == SRC_CLIENT_CX)
		for (i = 0; i < BG_MAX_CLIENTS; i++)
			if (cp->cxp == bg_client [i]) {
				bg_client [i] = NULL;
#ifdef DDS_IPV6
				if (cp->ipv6)
					bgcp_v6_active = 0;
				else
#endif
					bgcp_v4_active = 0;
				break;
			}

	while ((np = LIST_HEAD (cp->notifications)) != NULL)
		bgcp_notify_remove (&cp->notifications, np);

	LIST_REMOVE (cx_list, *cp);
	table_remove (&cx_table, cp->handle);
	xfree (cp);
	return (DDS_RETCODE_OK);
}

int bgcp_stop (int handle)
{
	lock_take (bgcp_lock);
	bgcp_stop_x (handle);
	lock_release (bgcp_lock);
	return (DDS_RETCODE_OK);
}

void bgcp_final (void)
{
	unsigned	h;
	SR_NOTIFY	*np, *next_np;

	log_printf (BGNS_ID, 0, "BGCP: Cleanup.\r\n");

	lock_take (bgcp_lock);
	while ((h = table_last (&cx_table)) != 0)
		bgcp_stop_x (h);

	table_cleanup (&cx_table);
	for (np = LIST_HEAD (notify_list); np; np = next_np) {
		next_np = LIST_NEXT (notify_list, *np);
		LIST_REMOVE (notify_list, *np);
		str_unref (np->topic);
		str_unref (np->type);
		xfree (np);
	}
	lock_release (bgcp_lock);
	lock_destroy (bgcp_lock);
}

/* bgcp_domain_active -- A domain became active. */

void bgcp_domain_active (unsigned domain_id)
{
	SR_CX		*cxp;
	Domain_t	*dp;

	dp = domain_lookup (domain_id);
	if (!dp)
		return;

	LIST_FOREACH (cx_list, cxp)
		if (!cxp->index && cxp->domain_id == domain_id) {
			cxp->index = dp->index;
			if (cxp->type == SRC_SERVER)
				bgcp_server_domain_active (cxp);
			else if (cxp->type == SRC_CLIENT || cxp->type == SRC_CLIENT_CX)
				bgcp_client_domain_active (cxp);
		}
}

/* bgcp_domain_inactive -- A domain became inactive. */

void bgcp_domain_inactive (unsigned domain_id)
{
	SR_CX	*cxp;

	LIST_FOREACH (cx_list, cxp)
		if (cxp->index && cxp->domain_id == domain_id) {
			if (cxp->type == SRC_SERVER)
				bgcp_server_domain_inactive (cxp);
			else if (cxp->type == SRC_CLIENT || cxp->type == SRC_CLIENT_CX)
				bgcp_client_domain_inactive (cxp);
			cxp->index = 0;
		}
}

#ifdef DDS_DEBUG

static void bgcp_dump_notification (SR_NOTIFY *p)
{
	dbg_printf ("Domain %u: %s/%s %u users", p->domain_id,
			p->topic ? str_ptr (p->topic) : NULL,
			p->type ? str_ptr (p->type) : NULL, p->nusers);
}

static void bgcp_dump_cx (SR_CX *p)
{
	static const char *type_s [] = {
		"SERVER", "SERVER-CX",
		"CLIENT", "CLIENT-CX",
		"C-TCP", "S-TCP"
	};
	unsigned	i;
	SR_NOTIFY_DATA	*np;
	char		buf [32];

	dbg_printf ("      #%u: %s  domain=%u  IPv%c  %s", 
		p->handle, type_s [p->type], p->domain_id,
		(p->ipv6) ? '6' : '4',
		p->suspended ? "SUSPENDED" : "ACTIVE");
	if (p->active)
		dbg_printf ("  peer=%s", guid_prefix_str (&p->rprefix, buf));
	dbg_printf ("\r\n\t");
	if (p->type == SRC_SERVER || p->type == SRC_SERVER_CX)
		if (p->port)
			dbg_printf ("port: %u", p->port);
		else
			dbg_printf ("port: @");
	else if (p->type == SRC_CLIENT)
		dbg_printf ("server: @");
	else if (p->type == SRC_CLIENT_CX) {
		dbg_printf ("server: ");
		if (p->rserver.name)
			dbg_printf ("%s", p->rserver.addr.name);
#ifdef DDS_IPV6
		else if (p->rserver.ipv6)
			dbg_printf ("%s", inet_ntop (AF_INET6,
						     p->rserver.addr.ipa_v6,
						     buf,
						     sizeof (buf)));
#endif
		else
			dbg_printf ("%d:%d:%d:%d",
						p->rserver.addr.ipa_v4 >> 24,
						(p->rserver.addr.ipa_v4 >> 16) & 0xff,
						(p->rserver.addr.ipa_v4 >> 8) & 0xff,
						p->rserver.addr.ipa_v4 & 0xff);
		dbg_printf (", port %u", p->rserver.port);
	}
	else if (p->type == SRC_C_TCP)
		dbg_printf ("server: @");
	else
		dbg_printf ("port: @");
	dbg_printf ("\r\n");
	dbg_printf ("\tself: %p  root: %p  derived: %p  IP-cx: %p\r\n",
		(void *) p, (void *) p->root, (void *) p->derived, (void *) p->cxp);
	if (LIST_NONEMPTY (p->notifications)) {
		dbg_printf ("\tNotifications:\r\n");
		LIST_FOREACH (p->notifications, np) {
			dbg_printf ("\t    %c: ", np->enable ? '+' : '-');
			bgcp_dump_notification (np->notification);
			dbg_printf ("\tcookie: ");
			for (i = 0; i < np->length; i++)
				dbg_printf ("%02x", np->cookie [i]);
			dbg_printf ("\r\n");
		}
	}
}

void bgcp_dump (void)
{
	SR_CX		*cxp;
	SR_NOTIFY	*np;

	dbg_printf ("BGCP: %s\r\n", (client_suspend) ? "suspended" : "active");
	dbg_printf ("    Connections:\r\n");
	LIST_FOREACH (cx_list, cxp)
		bgcp_dump_cx (cxp);
	dbg_printf ("    Notifications:\r\n");
	LIST_FOREACH (notify_list, np) {
		dbg_printf ("\t");
		bgcp_dump_notification (np);
		dbg_printf ("\r\n");
	}
}

#endif
#endif

