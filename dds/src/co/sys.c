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

/* sys.c -- System-specific functions that are needed by RTPS for constructing
            things like locators, GUIDs, etc. */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#ifdef _WIN32
#include "win.h"
#include <Lmcons.h>
#include <direct.h>
#include <process.h>
#include <ws2tcpip.h>
#else
#include <unistd.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <net/if.h>
#include <sys/utsname.h>
#if !defined (__ANDROID__) && !defined (__mips__)
#include <ifaddrs.h>
#define	USE_GETIFADDRS_V6
#elif !defined (USE_SIOCGIFCONF_V6)
#define USE_GETADDRINFO_V6
#endif
#if defined (DDS_IPV6) && defined (USE_GETADDRINFO_V6)
#include <netdb.h>
#endif /* IPv6 && GetAddrInfo */
#endif /* !_WIN32 */
#include <time.h>
#include "log.h"
#include "pool.h"
#include "error.h"
#include "libx.h"
#include "atomic.h"
#include "config.h"
#include "sys.h"

FTime_t	sys_startup_time;	/* Time at startup. */
Ticks_t	sys_ticks_last;		/* Last retrieved system ticks value. */

#ifdef IP_BLOCK_SRC
int	sys_block_ipv4;		/* No IPv4 addresses assigned. */
int	sys_block_ipv6;		/* No IPv6 addresses assigned. */
#endif

int sys_init (void)
{
#ifdef IP_BLOCK_SRC
	const char	*s;
#endif
#ifdef _WIN32
	WSADATA	ad;
	int	err;

	if ((err = WSAStartup (MAKEWORD(2, 2), &ad)) != 0)
		fatal_printf ("WSAStartup(v2.2) failed with error: %d\n", err);

	/* Confirm that the WinSock DLL supports 2.2.*/
	/* Note that if the DLL supports versions greater    */
	/* than 2.2 in addition to 2.2, it will still return */
	/* 2.2 in wVersion since that is the version we      */
	/* requested.                                        */

	if (LOBYTE (ad.wVersion) != 2 || HIBYTE (ad.wVersion) != 2) {
		/* Tell the user that we could not find a usable */
		/* WinSock DLL.                                  */
		WSACleanup ();
		fatal_printf ("Could not find a usable version of Winsock.dll (> v2.2)\n");
	}
#endif
	sys_getftime (&sys_startup_time);
#ifdef IP_BLOCK_SRC
	s = sys_getenv ("TDDS_BLOCK_IPV4");
	sys_block_ipv4 = (s != NULL && s [0] == '1' && s [1] == '\0');
	s = sys_getenv ("TDDS_BLOCK_IPV6");
	sys_block_ipv6 = (s != NULL && s [0] == '1' && s [1] == '\0');
#endif
	return (0 /*DDS_RETCODE_OK*/);
}

void sys_final (void)
{
#ifdef _WIN32
	WSACleanup ();
#endif
}

/* sys_username -- User name. */

char *sys_username (char *buf, size_t length)
{
#ifdef _WIN32
	char	ts [UNLEN + 1];
	DWORD	len = sizeof (ts);

	if (!GetUserNameA (ts, &len))
		fatal_printf ("sys_username(): GetUserNameA() returned error!");

	if (len >= length)
		fatal_printf ("sys_username(): buffer too short!");

	memcpy (buf, ts, len);
	buf [len] = '\0';
#else
	const char	*cp = getlogin ();

	if (!cp)
		cp = sys_getenv ("LOGNAME");
	if (!cp)
		return (NULL);

	if (strlen (cp) >= length) {
		memcpy (buf, cp, length - 1);
		buf [length - 1] = '\0';
		warn_printf ("sys_username(): login name too long ('%s')!", cp);
	}
	else
		strcpy (buf, cp);
#endif
	return (buf);
}

/* sys_hostname -- Fully qualified host name. */

char *sys_hostname (char *buf, size_t length)
{
	gethostname (buf, length);
	return (buf);
}

/* sys_osname -- Operating System name. */

char *sys_osname (char *buf, size_t length)
{
	ARG_NOT_USED (length)

#ifdef _WIN32
	strcpy (buf, "Windows");
#elif defined (__APPLE__)
	strcpy (buf, "OSX");
#elif defined (NetBSD)
	strcpy (buf, "NetBSD");
#elif defined (__FreeBSD__)
	strcpy (buf, "FreeBSD");
#elif defined (__OpenBSD__)
	strcpy (buf, "OpenBSD");
#else
	strcpy (buf, "Linux");
#endif
	return (buf);
}

/* sys_osrelease -- Operating System release. */

char *sys_osrelease (char *buf, size_t length)
{
#ifdef _WIN32
	ARG_NOT_USED (buf)
	ARG_NOT_USED (length)

	return (NULL);
#elif defined (ANDROID)
	ARG_NOT_USED (buf)
	ARG_NOT_USED (length)

	return ("Android");
#elif defined (__APPLE__)
	/* PRAETP: I added uname() here for mac,
	   I wonder why it was not used in the first place.
	   Feel free to use this for different platforms... */

	struct utsname un;

	if (uname (&un) < 0)
		return (NULL);

	snprintf (buf, length, "%s", un.release);
	return (buf);
#else
#include <unistd.h>
#if defined (NetBSD) || defined (__FreeBSD__) || defined (__OpenBSD__)
#include <sys/param.h>
#include <sys/sysctl.h>
#include <sys/utsname.h>
#else
#include <sys/syscall.h>
#include <linux/sysctl.h>
#endif
	struct __sysctl_args    args;
	int                     name [] = { CTL_KERN, KERN_OSRELEASE };

	memset (&args, 0, sizeof (struct __sysctl_args));
	args.name = name;
	args.nlen = sizeof (name) / sizeof (name [0]);
	args.oldval = buf;
	args.oldlenp = &length;

	if (syscall (SYS__sysctl, &args) == -1)
		return (NULL);

	buf [length] = '\0';
		return (buf);
#endif
}

/* sys_getcwd -- Current work directory. */

char *sys_getcwd (char *buf, size_t length)
{
#ifdef _WIN32
	return (_getcwd (buf, length));
#else
	return (getcwd (buf, length));
#endif
}

/* sys_uid -- User id. */

unsigned sys_uid (void)
{
#ifdef _WIN32
	char		buf [UNLEN + 1];
	unsigned	uid, i;

	sys_username (buf, sizeof (buf));
	uid = 0;
	for (i = 0; i < strlen (buf); i++)
		uid = (uid << 2) + buf [i];
	return (uid);
#else
	return ((unsigned) getuid ());
#endif
}

/* sys_hostid -- Host id. */

unsigned sys_hostid (void)
{
#ifdef _WIN32
	char		buf [128];
	unsigned	hid, i;

	sys_hostname (buf, sizeof (buf));
	hid = 0;
	for (i = 0; i < strlen (buf); i++)
		hid = (hid << 2) + buf [i];
	return (hid);
#else
#ifndef NOHOSTID
	return ((unsigned) gethostid ());
#else
	return 0;
#endif
#endif
}

/* sys_pid -- Process id. */

unsigned sys_pid (void)
{
#ifdef _WIN32
	return ((unsigned) _getpid ());
#else
	return ((unsigned) getpid ());
#endif
}

/* sys_ipv4_scope -- Retrieve the scope of an IPv4 address. */

Scope_t sys_ipv4_scope (const unsigned char *addr)
{
	Scope_t		scope;

	if (addr [0] == 255 && addr [1] == 255 && addr [2] == 255 && addr [3] == 255)
		scope = SITE_LOCAL;
	else if (*addr >= 224 && *addr <= 239) { /* Class D address. */
		if (*addr == 232)
			scope = NODE_LOCAL;
		else if (addr [0] == 224 && addr [1] == 0 && addr [2] == 0)
			scope = SITE_LOCAL;
		else if (addr [0] == 239)
			scope = ORG_LOCAL;
		else
			scope = GLOBAL_SCOPE;
	}
	else if (addr [0] == 127 && addr [1] == 0 && addr [2] == 0 && addr [3] == 1)
		scope = NODE_LOCAL;
	else if (addr [0] == 169 && addr [1] == 254)
		scope = LINK_LOCAL;
	else if (addr [0] == 10 ||
		 (addr [0] == 172 && (addr [1] & 0xf0) == 16) ||
	         (addr [0] == 192 && addr [1] == 168))
		scope = SITE_LOCAL;
	else
		scope = GLOBAL_SCOPE;
	return (scope);
}

const char *sys_scope_str (Scope_t scope)
{
	static const char *scope_str [] = { "?", "node", "link", "site", "org", "global" };

	return (scope_str [scope]);
}

/* sys_own_ipv4_addr -- Get a list of local IPv4 addresses in addr [].
			The function returns the number of addresses. */

unsigned sys_own_ipv4_addr (unsigned char *addr,
			    size_t        max,
			    Scope_t       min_scope,
			    Scope_t       max_scope,
			    int           log)
{
	int			s, i;
	unsigned		naddr = 0;
	Scope_t			scope;
#ifndef CDR_ONLY
	const char		*intfs;
#endif
#ifdef _WIN32
	unsigned char		buf [2048];
	DWORD			nbytes;
	SOCKET_ADDRESS_LIST	*slist;
	SOCKADDR_IN		*sap;

	if (sys_block_ipv4)
		return (0);

	if ((s = socket (AF_INET, SOCK_DGRAM, 0)) < 0) {
		perror ("socket");
		exit (1);
	}

	/* Get list of addresses. */
	if (WSAIoctl (s, SIO_ADDRESS_LIST_QUERY, NULL, 0, buf, sizeof (buf), &nbytes, NULL, NULL)) {
		printf ("sys_own_ipv4_addr: WSAIoctl.SIO_ADDRESS_LIST_QUERY() returned an error!");
		closesocket (s);
		return (0);
	}
	slist = (SOCKET_ADDRESS_LIST *) buf;
	for (i = 0; i < slist->iAddressCount && max >= OWN_IPV4_SIZE; i++) {
		sap = (SOCKADDR_IN *) slist->Address [i].lpSockaddr;
		memcpy (addr, &sap->sin_addr.s_addr, 4);
		scope = sys_ipv4_scope (addr);
		if (scope < min_scope || scope > max_scope)
			continue;

		memcpy (addr + OWN_IPV4_SCOPE_OFS, &scope, 4);
		addr += OWN_IPV4_SIZE;
		max -= OWN_IPV4_SIZE;
		naddr++;
	}
	closesocket (s);
	return (naddr);

#elif defined(__APPLE__)
	struct ifaddrs	*ifa = NULL, *ifp;
	struct in_addr	*ina;
	int		rc;

	if (sys_block_ipv4)
		return (0);

	rc = getifaddrs (&ifa);
	if (rc) {
		perror ("getifaddrs");
		return (0);
	}
	for (ifp = ifa; ifp; ifp = ifp->ifa_next) {
		if (ifp->ifa_addr->sa_family != AF_INET)
			continue;

		if (max < OWN_IPV4_SIZE)
			break;

		ina = &((struct sockaddr_in *)ifp->ifa_addr)->sin_addr;
		scope = sys_ipv4_scope ((const unsigned char *) ina);
		if (scope < min_scope || scope > max_scope)
			continue;

#ifdef FILT_VMWARE
		if (!memcmp (r->ifr_name, "vmnet", 5) ||
		    !memcmp (r->ifr_name, "vboxnet", 7))
			continue;
#endif
#ifndef CDR_ONLY
		if (config_defined (DC_IP_Intf)) {
			intfs = config_get_string (DC_IP_Intf, NULL);
			if (!slist_match (intfs, ifp->ifa_name, ':'))
				continue;
		}
#endif
		if (log) {
			if (!naddr)
				log_printf (LOG_DEF_ID, 0, "IP interfaces:\r\n");
			log_printf (LOG_DEF_ID, 0, "    %-8s : %s\r\n",
						ifp->ifa_name,
						inet_ntoa (*ina));
		}
		memcpy (addr, ina, 4);
		memcpy (addr + OWN_IPV4_SCOPE_OFS, &scope, 4);
		addr += OWN_IPV4_SIZE;
		max -= OWN_IPV4_SIZE;
		++naddr;
	}
	freeifaddrs (ifa);
	return (naddr);

#else
	struct ifconf		ifc;
	struct ifreq		*ifr, *r;
	struct sockaddr_in	*in4;
	int			numif;

	if (sys_block_ipv4)
		return (0);

	if ((s = socket (AF_INET, SOCK_DGRAM, 0)) < 0) {
		perror ("socket");
		exit (1);
	}

	/* Find number of interfaces. */
	memset (&ifc, 0, sizeof (ifc));
	ifc.ifc_ifcu.ifcu_req = NULL;
	ifc.ifc_len = 0;

	if (ioctl (s, SIOCGIFCONF, &ifc) < 0) {
		perror ("ioctl");
		exit (2);
	}

	if ((ifr = Alloc (ifc.ifc_len)) == NULL) {
		perror ("malloc");
		exit (3);
	}
	ifc.ifc_ifcu.ifcu_req = ifr;

	if (ioctl (s, SIOCGIFCONF, &ifc) < 0) {
		perror ("ioctl2");
		exit (4);
	}
	close (s);

	numif = ifc.ifc_len / sizeof (struct ifreq);
	for (i = 0, r = ifr; i < numif; i++, r++) {
		if (max < OWN_IPV4_SIZE)
			break;

		in4 = (struct sockaddr_in *) &r->ifr_addr;
		scope = sys_ipv4_scope ((const unsigned char *) &in4->sin_addr);
		if (scope < min_scope || scope > max_scope)
			continue;

#ifdef FILT_VMWARE
		if (!memcmp (r->ifr_name, "vmnet", 5) ||
		    !memcmp (r->ifr_name, "vboxnet", 7))
			continue;
#endif
#ifndef CDR_ONLY
		if (config_defined (DC_IP_Intf)) {
			intfs = config_get_string (DC_IP_Intf, NULL);
			if (!slist_match (intfs, r->ifr_name, ':'))
				continue;
		}
#endif
		if (log) {
			if (!naddr)
				log_printf (LOG_DEF_ID, 0, "IP interfaces:\r\n");
			log_printf (LOG_DEF_ID, 0, "    %-8s : %s\r\n",
						r->ifr_name,
						inet_ntoa (in4->sin_addr));
		}
		memcpy (addr, &in4->sin_addr, 4);
		memcpy (addr + OWN_IPV4_SCOPE_OFS, &scope, 4);
		addr += OWN_IPV4_SIZE;
		max -= OWN_IPV4_SIZE;
		++naddr;
	}
	Free (ifr);
	return (naddr);
#endif
}

#ifdef DDS_IPV6

Scope_t sys_ipv6_scope (const unsigned char *addr)
{
	unsigned	i;
	Scope_t		scope;
	static const Scope_t	mc_scopes [] = {
		UNKNOWN_SCOPE, NODE_LOCAL, LINK_LOCAL, UNKNOWN_SCOPE,
		UNKNOWN_SCOPE, SITE_LOCAL, UNKNOWN_SCOPE, UNKNOWN_SCOPE,
		ORG_LOCAL, UNKNOWN_SCOPE, UNKNOWN_SCOPE, UNKNOWN_SCOPE,
		UNKNOWN_SCOPE, UNKNOWN_SCOPE, GLOBAL_SCOPE, UNKNOWN_SCOPE
	};

	if (*addr == 0) {
		for (i = 1; i <= 9; i++)
			if (addr [i] != 0)
				return (UNKNOWN_SCOPE);

		if ((addr [10] == 0xff && addr [11] == 0xff) ||
		    (addr [10] == 0 && addr [11] == 0)) {

			for (i = 10; i < 15; i++)
				if (addr [i] != 0)
					break;

			if (i == 15 && addr [15] == 1)
				scope = NODE_LOCAL;
			else
				/* Embedded IPv4 */
				scope = sys_ipv4_scope (addr + 12);
		}
		else
			scope = UNKNOWN_SCOPE;
	}
	else if ((*addr >> 5) == 1)
		scope = GLOBAL_SCOPE;
	else if (*addr == 0xfe) {
		if (addr [1] >= 0xc0)
			scope = SITE_LOCAL;
		else if (addr [1] >= 0x80)
			scope = LINK_LOCAL;
		else
			scope = UNKNOWN_SCOPE;
	}
	else if (*addr == 0xff)
		scope = mc_scopes [addr [1] & 0xf];
	else
		scope = UNKNOWN_SCOPE;
	return (scope);
}

/* sys_own_ipv6_addr -- Get a list of local IPv6 addresses in addr [].
			The function returns the number of addresses. */

unsigned sys_own_ipv6_addr (unsigned char *addr,
			    size_t        max,
			    Scope_t       min_scope,
			    Scope_t       max_scope,
			    int           log)
{
#ifdef _WIN32
	int			s, i;
	unsigned		naddr = 0;
	unsigned char		buf [2048];
	DWORD			nbytes;
	SOCKET_ADDRESS_LIST	*slist;
	SOCKADDR_IN6		*sap;
	Scope_t			scope;

	if (sys_block_ipv6)
		return (0);

	if ((s = socket (AF_INET6, SOCK_DGRAM, 0)) < 0) {
		perror ("socket");
		exit (1);
	}

	/* Get list of addresses. */
	if (WSAIoctl (s, SIO_ADDRESS_LIST_QUERY, NULL, 0, buf, sizeof (buf), &nbytes, NULL, NULL)) {
		printf ("sys_own_ipv6_addr: WSAIoctl.SIO_ADDRESS_LIST_QUERY() returned an error!");
		closesocket (s);
		return (0);
	}
	slist = (SOCKET_ADDRESS_LIST *) buf;
	for (i = 0; i < slist->iAddressCount && max >= OWN_IPV6_SIZE; i++) {
		sap = (SOCKADDR_IN6 *) slist->Address [i].lpSockaddr;
		memcpy (addr, &sap->sin6_addr.s6_addr, 16);
		memcpy (addr + OWN_IPV6_SCOPE_ID_OFS, &sap->sin6_scope_id, 4);
		scope = sys_ipv6_scope (addr);
		if (scope < min_scope || scope > max_scope)
			continue;

		memcpy (addr + OWN_IPV6_SCOPE_OFS, &scope, 4);
		addr += OWN_IPV6_SIZE;
		max -= OWN_IPV6_SIZE;
		naddr++;
	}
	closesocket (s);
	return (naddr);
#else
#ifdef USE_GETIFADDRS_V6
	struct ifaddrs		*ifa = NULL, *ifp;
	struct sockaddr_in6	*ap;
	void			*p = NULL;
	unsigned		naddr = 0;
	int			rc;
	Scope_t			scope;
	const char		*intfs;
	char			buf [INET6_ADDRSTRLEN];

	if (sys_block_ipv6)
		return (0);

	rc = getifaddrs (&ifa);
	if (rc) {
		perror ("getifaddrs");
		return (0);
	}
	for (ifp = ifa; ifp; ifp = ifp->ifa_next) {
		if (!ifp->ifa_addr)
			continue;

		if (ifp->ifa_addr->sa_family != AF_INET6)
			continue;

		if (max < OWN_IPV6_SIZE)
			return (naddr);

#ifdef FILT_VMWARE
		if (!memcmp (ifp->ifa_name, "vmnet", 5) ||
		    !memcmp (ifp->ifa_name, "vboxnet", 7))
			continue;
#endif
		if ((ifp->ifa_flags & IFF_UP) == 0)
			continue;

		ap = (struct sockaddr_in6 *) ifp->ifa_addr;
		p = &ap->sin6_addr;
		scope = sys_ipv6_scope ((const unsigned char *) p);
		if (scope < min_scope || scope > max_scope)
			continue;

		if (config_defined (DC_IPv6_Intf)) {
			intfs = config_get_string (DC_IPv6_Intf, NULL);
			if (!slist_match (intfs, ifp->ifa_name, ':'))
				continue;
		}
		if (log) {
			if (!naddr)
				log_printf (LOG_DEF_ID, 0, "IPv6 interfaces:\r\n");

			log_printf (LOG_DEF_ID, 0, "    %-8s : %s\r\n",
						ifp->ifa_name,
						inet_ntop (AF_INET6, p, buf, sizeof (buf)));
		}
		memcpy (addr, p, 16);
		if (log)
			log_printf (LOG_DEF_ID, 0, " - scope_id = %u\r\n", ap->sin6_scope_id);
		memcpy (addr + OWN_IPV6_SCOPE_ID_OFS, &ap->sin6_scope_id, 4);
		memcpy (addr + OWN_IPV6_SCOPE_OFS, &scope, 4);
		addr += OWN_IPV6_SIZE;
		max -= OWN_IPV6_SIZE;
		++naddr;
	}
	freeifaddrs (ifa);
	return (naddr);

#elif defined (USE_GETADDRINFO_V6)
	/*int			s;*/
	unsigned		naddr = 0;
	struct sockaddr_in6	*in6;
	struct addrinfo		hints, *res;
	Scope_t			scope;
	char			addrstr [INET6_ADDRSTRLEN];

	if (sys_block_ipv6)
		return (0);

	memset (&hints, 0, sizeof (hints));
	hints.ai_family = AF_INET6;
	hints.ai_socktype = SOCK_DGRAM;
	hints.ai_flags = 0;
	hints.ai_protocol = 0;
	hints.ai_canonname = NULL;
	hints.ai_addr = NULL;
	hints.ai_next = NULL;

	/*if ((s = */getaddrinfo (NULL, "7400", &hints, &res);/*) < 0) {
		perror ("getaddrinfo");
		exit (1);
	}*/
	while (res) {
		if (res->ai_family == AF_INET6) {
			in6 = (struct sockaddr_in6 *) res->ai_addr;
			scope = sys_ipv6_scope (in6->sin6_addr.s6_addr);
			if (scope < min_scope || scope > max_scope) {
				res = res->ai_next;
				continue;
			}
			if (log)
				if (!naddr)
					log_printf (LOG_DEF_ID, 0, "IPv6 interface:\r\n");
				log_printf (LOG_DEF_ID, 0, "    %-8s : %s\r\n",
					    res->ai_canonname,
					    inet_ntop (AF_INET6, in6->sin6_addr.s6_addr, addrstr, INET6_ADDRSTRLEN));
			if (max >= 16) {
				memcpy (addr, in6->sin6_addr.s6_addr, 16);
				addr += 16;
				max -= 16;
				++naddr;
				if (max < 16)
					break;
			}
		}
		res = res->ai_next;
	}
	return (naddr);
#elif defined (USE_SIOCGIFCONF_V6)
	int			s, i;
	unsigned		naddr = 0;
#ifdef _WIN32
	unsigned char		buf [2048];
	DWORD			nbytes;
	SOCKET_ADDRESS_LIST	*slist;
	SOCKADDR_IN		*sap;
#else /* !_WIN32 */
	struct ifconf		ifconf;
	struct ifreq		ifr [50], *r;
	struct sockaddr_in6	*in6;
	int			numif;
#endif /* !_WIN32 */
	char			ip [INET_ADDRSTRLEN];

	if (sys_block_ipv6)
		return (0);

	if ((s = socket (AF_INET6, SOCK_DGRAM, 0)) < 0) {
		perror ("socket");
		exit (1);
	}

	ifconf.ifc_buf = (char *) ifr;
	ifconf.ifc_len = sizeof (ifr);

	if (ioctl (s, SIOCGIFCONF, &ifconf) == -1) {
		perror("ioctl");
		exit (1);
	}

	numif = ifconf.ifc_len / sizeof (ifr[0]);
	if (log)
		log_printf (LOG_DEF_ID, 0, "interfaces = %d:\r\n", numif);
	for (i = 0; i < numif; i++) {
		struct sockaddr_in *s_in = (struct sockaddr_in *) &ifr [i].ifr_addr;

		if (!inet_ntop (s_in->sin_family, &s_in->sin_addr, ip, sizeof (ip))) {
			perror ("inet_ntop");
			return 0;
		}
		if (log)
			log_printf (LOG_DEF_ID, 0, "%s - %s\n", ifr [i].ifr_name, ip);
	}
	close (s);
# if 0
#ifdef _WIN32
	/* Get list of addresses. */
	if (WSAIoctl (s, SIO_ADDRESS_LIST_QUERY, NULL, 0, buf, sizeof (buf), &nbytes, NULL, NULL)) {
		printf ("sys_own_ipv4_addr: WSAIoctl.SIO_ADDRESS_LIST_QUERY() returned an error!");
		closesocket (s);
		return (0);
	}
	slist = (SOCKET_ADDRESS_LIST *) buf;
	for (i = 0; i < slist->iAddressCount && max >= 4; i++) {
		sap = (SOCKADDR_IN *) slist->Address [i].lpSockaddr;
		memcpy (addr, &sap->sin_addr.s_addr, 4);
		addr += 4;
		max -= 4;
		naddr++;
	}
	closesocket (s);
	return (naddr);
#else /* !_WIN32 */
	/* Find number of interfaces. */
	memset (&ifc, 0, sizeof (ifc));
	ifc.ifc_ifcu.ifcu_req = NULL;
	ifc.ifc_len = 0;

	if (ioctl (s, SIOCGIFCONF, &ifc) < 0) {
		perror ("ioctl");
		exit (2);
	}

	if ((ifr = Alloc (ifc.ifc_len)) == NULL) {
		perror ("malloc");
		exit (3);
	}
	ifc.ifc_ifcu.ifcu_req = ifr;

	if (ioctl (s, SIOCGIFCONF, &ifc) < 0) {
		perror ("ioctl2");
		exit (4);
	}
	close (s);

	if (log && max > 16)
		log_printf (LOG_DEF_ID, 0, "IPv6 interfaces:\r\n");

	numif = ifc.ifc_len / sizeof (struct ifreq);
	for (i = 0, r = ifr; i < numif; i++, r++) {
		in6 = (struct sockaddr_in6 *) &r->ifr_addr;
		if (log && max > 16)
			log_printf (LOG_DEF_ID, 0, "    %-8s : %s\r\n",
					r->ifr_name,
					inet_ntop (in6->sin6_family,
						   &in6->sin6_addr,
						   buf, sizeof (buf)));
		memcpy (addr, &in6->sin6_addr, 16);
/*		if (addr [0] == 127 && addr [1] == 0 &&
		    addr [2] == 0   && addr [3] == 1)
		    	continue; 

#ifdef FILT_VMWARE
		if (!memcmp (r->ifr_name, "vmnet", 5) ||
		    !memcmp (r->ifr_name, "vboxnet", 7))
			continue;
#endif */
		addr += 16;
		max -= 16;
		++naddr;
		if (max < 16)
			break;
	}
	Free (ifr);
	return (naddr);
#endif /* !_WIN32 */
# endif /* 0 */
#else
#error "Unknown IPv6 address retrieval method!"
#endif /* USE_SIOCGIFCONF_V6 */
#endif
}

/* sys_nat64_ipv4_addr -- Dynamically convert a destination IPv4 address to an
			  IPv6 address using a NAT64 prefix if the system allows
			  this. */

int sys_nat64_ipv6_addr (uint32_t ipa, unsigned char *dst)
{
#if defined (DDS_NAT64) && defined (__APPLE__)
	int			ipv6 = 0;
	struct sockaddr_in6	*sa6;
	struct addrinfo		hints, *res, *rp;
	struct in_addr		ipv4_addr;
	/*char			buf [INET6_ADDRSTRLEN];*/

	ipv4_addr.s_addr = ipa;

	memset (&hints, 0, sizeof (struct addrinfo));
	hints.ai_family = AF_INET6;	/* Allow IPv6 only. */
	hints.ai_socktype = SOCK_STREAM;

	if ((getaddrinfo (inet_ntoa (ipv4_addr), NULL, &hints, &res)) == 0) {
		for (rp = res; rp; rp = rp->ai_next) {
			if (rp->ai_family != AF_INET6)
				continue;

			ipv6 = 1;
			sa6 = (struct sockaddr_in6 *) rp->ai_addr;
			memcpy (dst, sa6->sin6_addr.s6_addr, 16);
			/*log_printf (LOG_DEF_ID, 0, "Got IPv6 address %s\r\n",
				    inet_ntop (AF_INET6, dst, buf, sizeof (buf)));*/
			break;
		}
		freeaddrinfo (res);
	}
	return (ipv6);
#else
	ARG_NOT_USED (ipa)
	ARG_NOT_USED (dst)

	return (0);
#endif
}

#endif /* DDS_IPV6 */

/* sys_getenv -- Get the value of the environment variable with the given name*/

const char *sys_getenv (const char *var_name)
{
#ifdef _WIN32
	char	buffer [64];
	size_t	n;

	getenv_s (&n, buffer, sizeof (buffer), var_name);
	return (n ? buffer : NULL);
#else
	return (getenv (var_name));
#endif
}

#ifdef __MACH__

#include <sys/time.h>

#define CLOCK_MONOTONIC	0
#define CLOCK_REALTIME	1

/* clock_gettime is not implemented on OSX, simulate it using gettimeofday(). */

int clock_gettime (int clk_id, struct timespec *t)
{
	struct timeval now;
	int rv = gettimeofday (&now, NULL);

	if (rv)
		return (rv);

	t->tv_sec  = now.tv_sec;
	t->tv_nsec = now.tv_usec * 1000;
	return (0);
}

#endif

/* sys_getticks -- Get the system time in 10ms ticks since startup. */

Ticks_t sys_getticks (void)
{
	struct timespec	ts;
	Ticks_t		t;

	clock_gettime (CLOCK_MONOTONIC, &ts);
	t = (Ticks_t) (ts.tv_sec * TICKS_PER_SEC + 
				    ts.tv_nsec / (TMR_UNIT_MS * 1000 * 1000));
	atomic_set_l (sys_ticks_last, t);
	/*log_printf (LOG_DEF_ID, 0, "<STL:%lu>\r\n", sys_ticks_last);*/
	return (t);
}

/* sys_getftime -- Get the NTP timestamp in seconds/fractions. */

void sys_getftime (FTime_t *time)
{
	struct timespec	ts;
	Time_t		t;

	clock_gettime (CLOCK_REALTIME, &ts);
	t.seconds = ts.tv_sec;
	t.nanos = ts.tv_nsec;
	time2ftime (&t, time);
}

/* sys_gettime -- Get a system timestamp in seconds/nanoseconds. */

void sys_gettime (Time_t *time)
{
	struct timespec	ts;

	clock_gettime (CLOCK_REALTIME, &ts);
	time->seconds = (int32_t) ts.tv_sec;
	time->nanos = ts.tv_nsec;
}

/* NTP times in seconds: */
#define	MSB0_BASE_TIME	2085978496L	/* 7-Feb-2036 @ 06:28:16 UTC */
#define	MSB1_BASE_TIME	-2208988800L	/* 1-Jan-1900 @ 01:00:00 UTC */

/* NTP seconds adjust: */
#define	NTP_SECONDS(s)	((s < MSB0_BASE_TIME) ?	/* < February 2036? */ \
				((s) - MSB1_BASE_TIME) | (int) 0x80000000L : \
				(s) - MSB0_BASE_TIME)

/* time2ftime -- Convert a timestamp in system seconds/nanoseconds (1970..) to
		 NTP seconds/fractions (1900..). */

void time2ftime (const Time_t *t, FTime_t *ft)
{
	FTIME_SET (*ft, NTP_SECONDS (t->seconds), t->nanos);
}

/* ftime2time -- Convert a timestamp in NTP seconds/fractions (1900..) to
		 system seconds/nanoseconds (1970..). */

void ftime2time (const FTime_t *ft, Time_t *t)
{
	int	sec = FTIME_SEC (*ft);

	if (sec < 0)
		t->seconds = MSB0_BASE_TIME + sec;
	else
		t->seconds = MSB1_BASE_TIME + sec;
	t->nanos = FTIME_FRACT (*ft);
}

#define	ETERNITY	(50 * 3600 * 24 * 365)	/* 40 years :-) */

void ftime_set_time (FTime_t *ft, int32_t sec, uint32_t fract)
{
	int32_t	s_start = FTIME_SEC (sys_startup_time);

	/* Check if old (incorrect) timestamp, and adjust if so. */
	if (sec > s_start && sec - s_start > ETERNITY)
		FTIME_SETF (*ft, NTP_SECONDS (sec), fract);
	else
		FTIME_SETF (*ft, sec, fract);
}

/* ftime_add_ticks -- Add a ticks delta to an NTP seconds/fractions time. */

void ftime_add_ticks (FTime_t *ft, Ticks_t t)
{
	FTime_t	tt;

	FTIME_SETT (tt, t);	/* Delta FTime -> not absolute! */
	FTIME_ADD (*ft, tt);
}

/* ftime_delta -- Get the time delta between 2 timestamps in *res. */

int64_t ftime_delta (FTime_t *ft1, FTime_t *ft2)
{
#ifdef FTIME_STRUCT
	int64_t		f1, f2;

	f1 = ((int64_t) *ft1->seconds << 32) | ft1->fraction;
	f2 = ((int64_t) *ft2->seconds << 32) | ft2->fraction;
	return (f1 - f2);
#else
	return (*ft1 - *ft2);
#endif
}

#ifdef STANDALONE

int main (int argc, const char *argv[])
{
	char		username [UNLEN + 1];
	char		hostname [128];
	char		cwd [128];
	unsigned char	ipv4_addr [4 * 12];
	unsigned	n, i;

	if (sys_init ())
		return (1);

	printf ("username = %s\n", sys_username (username, sizeof (username)));
	printf ("hostname = %s\n", sys_hostname (hostname, sizeof (hostname)));
	printf ("cwd = %s\n", sys_getcwd (cwd, sizeof (cwd)));
	printf ("uid = %u\n", sys_uid ());
	printf ("hostid = %u\n", sys_hostid ());
	printf ("pid = %u\n", sys_pid ());
	printf ("IP addresses: ");
	n = sys_own_ipv4_addr (ipv4_addr, sizeof (ipv4_addr));
	for (i = 0; i < n << 2; i += 4)
		printf ("%u.%u.%u.%u ", ipv4_addr [i], ipv4_addr [i + 1], ipv4_addr [i + 2], ipv4_addr [i + 3]);
	printf ("\n");
	sys_final ();
	return 0;
}

#endif
