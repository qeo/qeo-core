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

/* di_linux.c -- Dynamic IP handler for Linux. */

#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <asm/types.h>
#include <netinet/in.h>
#include <net/if.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <poll.h>
#include <linux/netlink.h>
#include <linux/rtnetlink.h>
#include <linux/sockios.h>
#include "set.h"
#include "thread.h"
#include "pool.h"
#include "log.h"
#include "error.h"
#include "dds/dds_error.h"
#include "di_data.h"
#include "dynip.h"

typedef struct nl_request_st NL_Request;
struct nl_request_st {
	struct {
	  struct nlmsghdr	n;
	  struct ifaddrmsg	r;
	}			req;
	struct rtattr		*rta;
	NL_Request		*next;
};

typedef struct if_data_st {
	uint16_t	index;
	int16_t		up;
} IfData_t;

static int		netlink_fd = -1;
static NL_Request	*n_pending;
static unsigned		n_proto;
static size_t		ifstate_nintfs;
static int		ifstate_valid;
static unsigned		ifstate_max_idx;
static unsigned		ifstate_size;
static uint32_t		*ifstate_set;

int di_sys_init (void)
{
	unsigned	i;
	struct sockaddr_nl addr;

	if ((netlink_fd = socket (PF_NETLINK, SOCK_RAW, NETLINK_ROUTE)) == -1) {
		warn_printf ("di_sys_init: couldn't open NETLINK_ROUTE socket");
		return (DDS_RETCODE_UNSUPPORTED);
	}

	memset (&addr, 0, sizeof (addr));
	addr.nl_family = AF_NETLINK;
	addr.nl_pid = sys_pid ();
#ifdef THREADS_USED
	addr.nl_pid |= pthread_self ();
#endif
	addr.nl_groups = RTMGRP_LINK | 
			 RTMGRP_IPV4_IFADDR | RTMGRP_IPV4_ROUTE
#ifdef DDS_IPV6
		       | RTMGRP_IPV6_IFADDR | RTMGRP_IPV6_ROUTE
#endif
							       ;
	for (i = 0; bind (netlink_fd, (struct sockaddr *) &addr, sizeof (addr)) == -1; i++)
		if (i >= 5) {
			perror ("di_sys_init:bind()");
			warn_printf ("di_sys_init: couldn't bind NETLINK_ROUTE socket");
			close (netlink_fd);
			netlink_fd = -1;
			return (DDS_RETCODE_UNSUPPORTED);
		}
		else {
			warn_printf ("di_sys_init: NETLINK_ROUTE socket in use - retrying ...");
			usleep (500000);
		}

	n_proto = 0;
	return (DDS_RETCODE_OK);
}

static void di_ifstate_init (void)
{
	ifstate_valid = 0;
}

#define	MAX_IF_STATIC	16

static void di_ifstate_get (void)
{
	int			s, i;
	unsigned		max_index;
	struct ifconf		ifc;
	struct ifreq		*ifr, *r;
	int			numif;
	short			flags;
	size_t			size;
	uint32_t		*p;
	IfData_t		*ifs;
	IfData_t		if_data [MAX_IF_STATIC];

	/*log_printf (IP_ID, 0, "DynIP: querying interfaces.\r\n");*/
	if ((s = socket (AF_INET, SOCK_DGRAM, 0)) < 0) {
		perror ("socket");
		exit (1);
	}

	/* Find minimum number of interfaces. */
	memset (&ifc, 0, sizeof (ifc));
	ifc.ifc_ifcu.ifcu_req = NULL;
	ifc.ifc_len = 0;
	if (ioctl (s, SIOCGIFCONF, &ifc) < 0) {
		perror ("ioctl");
		exit (2);
	}

	/* Actual required buffer size in ifc.ifc_len now. */
	size = ifc.ifc_len;
	ifr = NULL;
	do {
		size += sizeof (struct ifreq) * 2;	/* Ask some more. */
		if (!ifr)
			r = xmalloc (size);
		else
			r = xrealloc (ifr, size);
		if (!r) {
			perror ("xmalloc");
			exit (3);
		}
		ifc.ifc_len = size;
		ifc.ifc_ifcu.ifcu_req = ifr = r;

		if (ioctl (s, SIOCGIFCONF, &ifc) < 0) {
			perror ("ioctl2");
			exit (4);
		}
	}
	while (ifc.ifc_len == (int) size);

	/* Buffer size is large enough to get all interfaces. */
	ifstate_nintfs = numif = ifc.ifc_len / sizeof (struct ifreq);
	/*log_printf (IP_ID, 0, "%u interfaces (%d bytes total, %lu bytes each)!\r\n",
			numif, ifc.ifc_len, (unsigned long) sizeof (struct ifreq));*/

	if (numif > MAX_IF_STATIC) {
		ifs = xmalloc (sizeof (IfData_t) * numif);
		if (!ifs)
			fatal_printf ("di_ifstate_get: out of memory for interfaces data array!");
	}
	else
		ifs = if_data;
	max_index = 0;
	for (i = 0, r = ifr; i < numif; i++, r++) {
		if (ioctl (s, SIOCGIFFLAGS, r) < 0) {
			perror ("ioctl3");
			exit (5);
		}
		flags = r->ifr_flags;
		if (ioctl (s, SIOCGIFINDEX, r) < 0) {
			perror ("ioctl4");
			exit (6);
		}
		ifs [i].index = r->ifr_ifindex;
		if (r->ifr_ifindex > (int) max_index)
			max_index = r->ifr_ifindex;
			if (max_index > 512)
				fatal_printf ("di_ifstate_get: invalid interface index (%s: %u)!",
								r->ifr_name, max_index);

		/*log_printf (IP_ID, 0, "DynIP: %s state = 0x%04x, index = %u", r->ifr_name, flags, i);*/
		if ((flags & (IFF_UP | IFF_RUNNING)) == (IFF_UP | IFF_RUNNING) &&
			(flags & IFF_LOOPBACK) == 0) {
			/*log_printf (IP_ID, 0, " :: up!");*/
			ifs [i].up = 1;
		}
		else
			ifs [i].up = 0;
		/*log_printf (IP_ID, 0, "\r\n");*/
	}
	size = ((max_index + 32) >> 5) << 2;
	if (size > ifstate_size) {
		if (ifstate_set)
			p = xrealloc (ifstate_set, size);
		else
			p = xmalloc (size);
		if (!p)
			fatal_printf ("di_ifstate_get: out of memory for ifstate_set");

		ifstate_set = p;
		ifstate_size = size;
	}
	memset (ifstate_set, 0, ifstate_size);
	for (i = 0; i < numif; i++)
		if (ifs [i].up)
			SET_ADD (ifstate_set, ifs [i].index);
	if (numif > MAX_IF_STATIC)
		xfree (ifs);
	xfree (ifr);

	ifstate_max_idx = max_index;
	close (s);
}

static int di_intf_active (unsigned intf)
{
	if (!ifstate_valid)
		di_ifstate_get ();

	if (intf > ifstate_max_idx)
		return (0);

	return (SET_CONTAINS (ifstate_set, intf));
}

static void di_event (int fd, short revents, void *arg)
{
	struct sockaddr_nl	sa;
	struct msghdr		msg;
	struct nlmsghdr		*nlmp;
	struct iovec		iov;
	unsigned 		len;
	struct ifaddrmsg	*ifa;
	int			rtl;
	int			multi;
	struct rtattr		*rta;
	struct ifinfomsg	*ifi;
	NL_Request		*rp;
	IP_Intf_t		*ifp;
	static char		buf [4096];

	ARG_NOT_USED (revents)
	ARG_NOT_USED (arg)

	di_evh_begin ();
	di_ifstate_init ();

	do {
		multi = 0;
		iov.iov_base = buf;
		iov.iov_len = sizeof (buf);

		msg.msg_name = (void *) &sa;
		msg.msg_namelen = sizeof (sa);
		msg.msg_iov = &iov;
		msg.msg_iovlen = 1;
		msg.msg_control = NULL;
		msg.msg_controllen = 0;
		msg.msg_flags = 0;

		len = recvmsg (fd, &msg, 0);

		for (nlmp = (struct nlmsghdr *) buf;
		     NLMSG_OK (nlmp, len);
		     nlmp = NLMSG_NEXT (nlmp, len)) {

			if ((nlmp->nlmsg_flags & NLM_F_MULTI) != 0)
				multi = 1;

			/* The end of a multipart message? */
			if (nlmp->nlmsg_type == NLMSG_DONE) {
				multi = 0;
				break;
			}
			if (nlmp->nlmsg_type == NLMSG_ERROR)
				log_printf (IP_ID, 0, "DynIP: Netlink event error!\r\n"); /* Should do some error handling. */

			if (nlmp->nlmsg_type == RTM_NEWADDR ||
			    nlmp->nlmsg_type == RTM_DELADDR) {
				ifa = (struct ifaddrmsg *) NLMSG_DATA (nlmp);
				rta = IFA_RTA (ifa);
				rtl = IFA_PAYLOAD (nlmp);
				for (; rtl && RTA_OK (rta, rtl); rta = RTA_NEXT (rta, rtl)) {
					ifp = di_intf_lookup (ifa->ifa_index);
					if (!ifp && nlmp->nlmsg_type == RTM_NEWADDR) {
						ifp = di_intf_new (ifa->ifa_index);
						if (!ifp)
							continue;
					}
					else if (!ifp)
						continue;

					if (rta->rta_type == IFA_LOCAL) {
						if (nlmp->nlmsg_type == RTM_NEWADDR) {
							if (!di_intf_up (ifp) &&
							    di_intf_active (ifa->ifa_index))
								di_intf_update (ifp, 1, -1, -1);
							di_add_addr (ifp, ifa->ifa_family, RTA_DATA (rta));
						}
						else if (ifp && nlmp->nlmsg_type == RTM_DELADDR) {
							di_remove_addr (ifp, ifa->ifa_family, RTA_DATA (rta));
							if (di_intf_up (ifp) &&
							    !di_intf_active (ifa->ifa_index))
								di_intf_update (ifp, 0, -1, -1);
						}
					}
					else if (rta->rta_type == IFA_ADDRESS) {
						if (nlmp->nlmsg_type == RTM_NEWADDR) {
							if (!di_intf_up (ifp) &&
							    di_intf_active (ifa->ifa_index))
								di_intf_update (ifp, 1, -1, -1);
							di_add_addr (ifp, ifa->ifa_family, RTA_DATA (rta));
						}
						else if (ifp && nlmp->nlmsg_type == RTM_DELADDR) {
							di_remove_addr (ifp, ifa->ifa_family, RTA_DATA (rta));
							if (di_intf_up (ifp) &&
							    !di_intf_active (ifa->ifa_index))
								di_intf_update (ifp, 0, -1, -1);
						}
					}
					/*else
						printf ("RTA Type = %d\r\n", rta->rta_type);*/
				}
			}
			else if (nlmp->nlmsg_type == RTM_NEWLINK ||
			         nlmp->nlmsg_type == RTM_DELLINK ||
			         nlmp->nlmsg_type == RTM_GETLINK) {
				ifi = (struct ifinfomsg *) NLMSG_DATA (nlmp);
				ifp = di_intf_lookup (ifi->ifi_index);
				if (nlmp->nlmsg_type == RTM_NEWLINK) {
					if (!ifp) {
						ifp = di_intf_new (ifi->ifi_index);
						if (!ifp)
							continue;
					}
					di_intf_update (ifp, (ifi->ifi_flags & IFF_RUNNING) != 0, -1, -1);
				}
				else if (ifp && nlmp->nlmsg_type == RTM_DELLINK)
					di_intf_removed (ifp);
			}
			/*else if (nlmp->nlmsg_type == RTM_NEWROUTE ||
			         nlmp->nlmsg_type == RTM_DELROUTE ||
				 nlmp->nlmsg_type == RTM_GETROUTE)
				printf (" --- Routing info (%u)!\r\n", nlp->nlmsg_type);
			else
				printf (" --- {%u} Unknown info!\n", nlmp->nlmsg_type);*/
		}
	}
	while (multi);

	di_evh_end ();

	rp = n_pending;
	if (!n_pending)
		return;

	n_pending = rp->next;
	if (n_pending)
		send (netlink_fd, &n_pending->req, n_pending->req.n.nlmsg_len, 0);

	xfree (rp);
}

static void di_request (NL_Request *rp)
{
	NL_Request	*xrp;

	rp->next = NULL;
	if (!n_pending) {
		n_pending = rp;
		send (netlink_fd, &n_pending->req, n_pending->req.n.nlmsg_len, 0);
	}
	else {
		for (xrp = n_pending; xrp->next; xrp = xrp->next)
			;
		xrp->next = rp;
	}
}

void di_sys_final (void)
{
	if (netlink_fd != -1) {
		close (netlink_fd);
		netlink_fd = -1;
	}
	ifstate_nintfs = 0;
	ifstate_valid = 0;
	ifstate_max_idx = 0;
	if (ifstate_set) {
		ifstate_size = 0;
		xfree (ifstate_set);
		ifstate_set = NULL;
	}
}

int di_sys_attach (unsigned      family,
		   unsigned char *ipa,
		   unsigned      *n,
		   unsigned      max,
		   Scope_t       min_scope,
		   Scope_t       max_scope,
		   DI_NOTIFY     fct)
{
	NL_Request	*rp;

	ARG_NOT_USED (ipa)
	ARG_NOT_USED (n)
	ARG_NOT_USED (max)
	ARG_NOT_USED (min_scope)
	ARG_NOT_USED (max_scope)
	ARG_NOT_USED (fct)

	rp = xmalloc (sizeof (NL_Request));
	if (!rp) {
		warn_printf ("di_sys_attach: out of memory for request!");
		return (DDS_RETCODE_OUT_OF_RESOURCES);
	}
	if (!n_proto) {
		memset (&rp->req, 0, sizeof (rp->req));
		rp->req.n.nlmsg_len = NLMSG_LENGTH (sizeof (struct ifaddrmsg));
		rp->req.n.nlmsg_flags = NLM_F_REQUEST | NLM_F_ROOT;
		rp->req.n.nlmsg_type = RTM_GETLINK;
		di_request (rp);
		rp = xmalloc (sizeof (NL_Request));
		if (!rp) {
			warn_printf ("di_attach: out of memory for request!");
			return (DDS_RETCODE_OUT_OF_RESOURCES);
		}
	}
	memset (&rp->req, 0, sizeof (rp->req));
	rp->req.n.nlmsg_len = NLMSG_LENGTH (sizeof (struct ifaddrmsg));
	rp->req.n.nlmsg_flags = NLM_F_REQUEST | NLM_F_ROOT;
	rp->req.n.nlmsg_type = RTM_GETADDR;
	rp->rta = (struct rtattr *) (((char *) &rp->req) +
			NLMSG_ALIGN (rp->req.n.nlmsg_len));
	if (family == AF_INET) {
		rp->req.r.ifa_family = AF_INET;
		rp->rta->rta_len = RTA_LENGTH (4);
	}
#ifdef DDS_IPV6
	else if (family == AF_INET6) {
		rp->req.r.ifa_family = AF_INET6;
		rp->rta->rta_len = RTA_LENGTH (16);
	}
#endif
	else
		return (DDS_RETCODE_BAD_PARAMETER);

	n_proto++;
	if (n_proto == 1)
		sock_fd_add (netlink_fd, POLLIN | POLLPRI | POLLHUP | POLLNVAL,
							di_event, 0, "DDS.netlink");
	di_request (rp);
	return (DDS_RETCODE_OK);
}

int di_sys_detach (unsigned family)
{
	if (family == AF_INET)
		n_proto--;
	else if (family == AF_INET6)
		n_proto--;
	else
		return (DDS_RETCODE_BAD_PARAMETER);

	if (!n_proto)
		sock_fd_remove (netlink_fd);

	return (DDS_RETCODE_OK);
}

void di_sys_check (void)
{
	/* Nada -- Netlink is supposed to tell us what we need to know! */
}

