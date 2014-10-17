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

/* sock.c -- Provides common socket control functions to manage the
	     different DDS transport protocols that use fds. */

#include <stdio.h>
#include <stdlib.h>
#ifdef _WIN32
#include "win.h"
#else
#include <fcntl.h>
#include <unistd.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <poll.h>
#endif
#include <string.h>
#include <errno.h>
#include "pool.h"
#include "config.h"
#include "log.h"
#include "atomic.h"
#include "list.h"
#include "error.h"
#include "timer.h"
#include "dds.h"
#include "debug.h"
#include "sock.h"
#include <sys/types.h>
#include <sys/socket.h>

static int n_ready;
static lock_t sock_lock;
static lock_t poll_lock;

#ifdef _WIN32

#define INC_SIZE	16
#define MAX_SIZE	MAXIMUM_WAIT_OBJECTS

typedef struct handle_st {
	int		is_socket;
	unsigned	index;
	unsigned	events;
	const char	*name;
	HANDLE		handle;
	RHDATAFCT	hfct;
	void		*udata;
} SockHandle_t;

typedef struct socket_st {
	int		is_socket;
	unsigned	index;
	unsigned	events;
	const char	*name;
	WSAEVENT	handle;
	RSDATAFCT	sfct;
	void		*udata;
	SOCKET		socket;
} SockSocket_t;

typedef struct sock_st {
	int		is_socket;
	unsigned	index;
	unsigned	events;
	const char	*name;
	HANDLE		handle;

	/* -- remainder depends on is_socket -- */

} Sock_t;

static unsigned		nhandles;
static HANDLE		whandles [MAXIMUM_WAIT_OBJECTS];
static Sock_t		*wsock [MAXIMUM_WAIT_OBJECTS];
static SockSocket_t	(*sockets) [MAXIMUM_WAIT_OBJECTS];
static SockHandle_t	(*handles) [MAXIMUM_WAIT_OBJECTS];
static unsigned		num_socks, max_socks;
static unsigned		num_handles, max_handles;

/* sock_fd_init -- Initialize the file descriptor array. */

int sock_fd_init (unsigned max_cx, unsigned grow)
{
	static int	initialized = 0;

	if (sockets || handles)
		return (0);

	sockets = xmalloc (sizeof (SockSocket_t) * INC_SIZE);
	if (!sockets)
		return (DDS_RETCODE_OUT_OF_RESOURCES);

	handles = xmalloc (sizeof (SockHandle_t) * INC_SIZE);
	if (!handles) {
		xfree (sockets);
		return (DDS_RETCODE_OUT_OF_RESOURCES);
	}
	num_socks = 0;
	max_socks = INC_SIZE;
	num_handles = 0;
	max_handles = INC_SIZE;

	if (!initialized) {
		lock_init_nr (sock_lock, "sock");
		lock_init_nr (poll_lock, "poll");
		initialized = 1;
	}
	return (0);
}

/* sock_fd_final -- Finalize the poll file descriptor array. */

void sock_fd_final (void)
{
	if (!max_socks)
		return;

	xfree (sockets);
	xfree (handles);
	num_socks = max_socks = num_handles = max_handles = 0;
	sockets = NULL;
	handles = NULL;
}

/* sock_fd_add_handle -- Add a handle and associated callback function. */

int sock_fd_add_handle (HANDLE     h,
			short      events,
			RHDATAFCT  rx_fct,
			void       *udata,
			const char *name)
{
	SockHandle_t	*hp;
	void		*p;

	if (!max_handles) /* Needed for Debug init ... */
		dds_pre_init ();

	lock_take (sock_lock);
	if (num_handles == max_handles ||
	    nhandles >= MAXIMUM_WAIT_OBJECTS) {
		if (!max_handles ||
		    max_handles >= MAX_SIZE ||
		    nhandles >= MAXIMUM_WAIT_OBJECTS) {
			lock_release (sock_lock);
			return (DDS_RETCODE_OUT_OF_RESOURCES);
		}
		max_handles += INC_SIZE;
		p = xrealloc (handles, sizeof (SockHandle_t) * max_handles);
		if (!p)
			fatal_printf ("sock_fd_add: can't realloc()!");

		handles = p;
	}

	/*printf ("handle added: fd=%d, events=%d\n", h, events);*/
	hp = &(*handles) [num_handles];
	hp->is_socket = 0;
	hp->index = nhandles;
	hp->events = events;
	hp->name = name;
	hp->handle = h;
	hp->hfct = rx_fct;
	hp->udata = udata;
	num_handles++;
	whandles [nhandles] = h;
	wsock [nhandles++] = (Sock_t *) hp;
	lock_release (sock_lock);
	return (DDS_RETCODE_OK);
}

/* sock_fd_remove_handle -- Remove a file descriptor. */

void sock_fd_remove_handle (HANDLE h)
{
	unsigned	i;
	SockHandle_t	*hp;

	lock_take (sock_lock);
	for (i = 0, hp = &(*handles) [0]; i < num_handles; i++, hp++)
		if (hp->handle == h) {
			if (hp->index + 1 < nhandles) {
				memmove (&whandles [hp->index],
					 &whandles [hp->index + 1],
					 (nhandles - i - 1) *
					 sizeof (HANDLE));
				memmove (&wsock [hp->index],
					 &wsock [hp->index + 1],
					 (nhandles - i - 1) *
					 sizeof (Sock_t *));
			}
			nhandles--;
			if (i + 1 < num_handles)
				memmove (&(*handles) [i],
					 &(*handles) [i + 1],
					 (num_handles - i - 1) *
					 sizeof (SockHandle_t));
			num_handles--;
			break;
		}
	lock_release (sock_lock);
}

/* sock_fd_add_socket -- Add a socket and associated callback function. */

int sock_fd_add_socket (SOCKET s, short events, RSDATAFCT rx_fct, void *udata, const char *name)
{
	SockSocket_t	*sp;
	void		*p;
	unsigned	e;
	WSAEVENT	ev;

	lock_take (sock_lock);
	if ((ev = WSACreateEvent ()) == WSA_INVALID_EVENT) {
		lock_release (sock_lock);
		return (DDS_RETCODE_OUT_OF_RESOURCES);
	}
	if (num_socks == max_socks ||
	    nhandles >= MAXIMUM_WAIT_OBJECTS) {
		if (!max_socks ||
		    max_socks > MAX_SIZE ||
		    nhandles >= MAXIMUM_WAIT_OBJECTS) {
			WSACloseEvent (ev);
			lock_release (sock_lock);
			return (DDS_RETCODE_OUT_OF_RESOURCES);
		}
		max_socks += INC_SIZE;
		p = xrealloc (sockets, sizeof (SockSocket_t) * max_socks);
		if (!p)
			fatal_printf ("sock_fd_add_socket: can't realloc()!");

		sockets = p;
	}

	/*printf ("socket added: fd=%d, events=%d\n", s, events);*/
	sp = &(*sockets) [num_socks];
	sp->is_socket = 1;
	sp->index = nhandles;
	sp->socket = s;
	sp->events = events;
	sp->name = name;
	sp->handle = ev;
	e = 0;
	if ((events & POLLIN) != 0)
		e = FD_READ;
	if ((events & POLLPRI) != 0)
		e |= FD_OOB;
	if ((events & POLLOUT) != 0)
		e |= FD_WRITE;
	if ((events & POLLHUP) != 0)
		e |= FD_CLOSE;
	if (WSAEventSelect (s, ev, e))
		fatal_printf ("sock_fd_add_socket(): WSAEventSelect() failed - error = %d", WSAGetLastError ());

	sp->sfct = rx_fct;
	sp->udata = udata;
	num_socks++;
	whandles [nhandles] = ev;
	wsock [nhandles++] = (Sock_t *) sp;
	lock_release (sock_lock);
	return (DDS_RETCODE_OK);
}

/* sock_fd_remove -- Remove a file descriptor. */

void sock_fd_remove_socket (SOCKET s)
{
	unsigned	i;
	SockSocket_t	*sp;

	lock_take (sock_lock);
	for (i = 0, sp = &(*sockets) [0]; i < num_socks; i++, sp++)
		if (sp->socket == s) {
			WSACloseEvent (sp->handle);
			if (sp->index + 1 < nhandles) {
				memmove (&whandles [sp->index],
					 &whandles [sp->index + 1],
					 (nhandles - i - 1) *
					 sizeof (HANDLE));
				memmove (&wsock [sp->index],
					 &wsock [sp->index + 1],
					 (nhandles - i - 1) *
					 sizeof (Sock_t *));
			}
			nhandles--;
			if (i + 1 < num_socks)
				memmove (&(*sockets) [i],
					 &(*sockets) [i + 1],
					 (num_socks - i - 1) *
					 sizeof (SockSocket_t));

			num_socks--;
			break;
		}
	lock_release (sock_lock);
}

/* sock_fd_valid_socket -- Check if a socket is still valid. */

int sock_fd_valid_socket (SOCKET s)
{
	unsigned	i;
	SockSocket_t	*sp;
	
	lock_take (sock_lock);
	for (i = 0, sp = &(*sockets) [0]; i < num_socks; i++, sp++)
		if (sp->socket == s) {
			lock_release (sock_lock);
			return (1);
		}

	lock_release (sock_lock);
	return (0);
}

/* sock_fd_event_socket -- Update the notified events on a socket. */

int sock_fd_event_socket (SOCKET s, short events, int set)
{
	unsigned	i;
	SockSocket_t	*sp;

	lock_take (sock_lock);
	for (i = 0, sp = &(*sockets) [0]; i < num_socks; i++, sp++)
		if (sp->socket == s) {
			if (set)
				sp->events |= events;
			else
				sp->events &= ~events;
			break;
		}
	lock_release (sock_lock);
	return (0);
}

/* sock_fd_udata_socket -- Update the notified user data on a socket. */

int sock_fd_udata_socket (SOCKET s, void *udata)
{
	unsigned	i;
	SockSocket_t	*sp;

	lock_take (sock_lock);
	for (i = 0, sp = &(*sockets) [0]; i < num_socks; i++, sp++)
		if (sp->socket == s) {
			sp->udata = udata;
			break;
		}
	lock_release (sock_lock);
	return (0);
}

/* sock_fd_schedule -- Schedule all pending event handlers. */

void sock_fd_schedule (void)
{
	Sock_t		*p;
	SockHandle_t	*hp;
	SockSocket_t	*sp;
	WSANETWORKEVENTS ev;
	unsigned	events;

	if (n_ready < 0 || n_ready >= (int) nhandles)
		return;

	lock_take (sock_lock);
	p = wsock [n_ready];
	if (p->is_socket) {
		sp = (SockSocket_t *) p;
		if (WSAEnumNetworkEvents (sp->socket, sp->handle, &ev)) {
			log_printf (LOG_DEF_ID, 0, "sock_fd_schedule: WSAEnumNetworkEvents() returned error %d\r\n", WSAGetLastError ());
			return;
		}
		events = 0;
		if ((ev.lNetworkEvents & FD_READ) != 0)
			events |= POLLRDNORM;
		if ((ev.lNetworkEvents & FD_OOB) != 0)
			events |= POLLPRI;
		if ((ev.lNetworkEvents & FD_WRITE) != 0)
			events |= POLLWRNORM;
		if ((ev.lNetworkEvents & FD_CLOSE) != 0)
			events |= POLLHUP;
		(*sp->sfct) (sp->socket, events, sp->udata);
	}
	else {
		hp = (SockHandle_t *) p;
		(*hp->hfct) (hp->handle, hp->events, hp->udata);
	}
	lock_release (sock_lock);
}

#undef errno
#define errno	WSAGetLastError()

/* sock_fd_poll -- Use poll() or select() to query the state of all file
		   descriptors. */

void sock_fd_poll (unsigned poll_time)
{
	if (!nhandles) {
		Sleep (poll_time);
		n_ready = -1;
		return;
	}

	/* Wait until at least one handle is signalled or until time-out. */
	n_ready = WaitForMultipleObjects (nhandles, whandles, 0, poll_time);
	if (n_ready >= WAIT_OBJECT_0 &&
	    n_ready <= (int) (WAIT_OBJECT_0 + (int) nhandles - 1)) {
		dds_lock_ev ();
		dds_ev_pending |= DDS_EV_IO;
		n_ready -= WAIT_OBJECT_0;
		dds_unlock_ev ();
	}
	else if (n_ready == WAIT_TIMEOUT)
		n_ready = -1;
	else if (n_ready >= WAIT_ABANDONED_0 &&
		 n_ready <= (int) (WAIT_ABANDONED_0 + (int) nhandles - 1)) {
		log_printf (LOG_DEF_ID, 0, "sock_fd_poll: WaitForMultipleObjects(): abandoned handle %d was signalled", n_ready - WAIT_ABANDONED_0);
		n_ready = -1;
	}
	else if (n_ready == WAIT_FAILED) {
		log_printf (LOG_DEF_ID, 0, "sock_fd_poll: WaitForMultipleObjects() returned an error: %d", GetLastError ());
		n_ready = -1;
	}
	else {
		log_printf (LOG_DEF_ID, 0, "sock_fd_poll: WaitForMultipleObjects() returned unknown status: %d", n_ready);
		n_ready = -1;
	}
}

#else

#define	FD_INC_SIZE	16

static MEM_DESC_ST	mem_blocks [1];
static const char	*mem_names [1] = { "SOCK" };
static size_t		mem_size;

typedef enum {
	PendingRemove,		/* Was active but needs to be deleted. */
	PendingAdd,		/* New socket. */
	ActiveSocket		/* Existing active socket. */
} SockState_t;

typedef struct sock_st Sock_t;
struct sock_st {
	Sock_t		*next;		/* Next in socket list. */
	Sock_t		*prev;		/* Previous in socket list. */
	SockState_t	state;		/* Current state. */
	int		index;		/* Index in pollfd table. */
	int		fd;		/* File descriptor handle. */
	short		events;		/* Requested events. */
	const char	*name;		/* Name of fd. */
	RSDATAFCT	fct;		/* Callback function. */
	void		*udata;		/* User data for callback. */
};

typedef struct sock_list_st {
	Sock_t		*head;		/* First entry in socket list. */
	Sock_t		*tail;		/* Last entry in socket list. */
} SockList_t;

static SockList_t	sockets;
static unsigned 	num_fds, max_fds, fd_max_cx, nfds;
static struct pollfd	*fds;
static Sock_t		**sds;
static int		sock_update_needed, poll_active;


/* sock_fd_init -- Initialize the poll file descriptor array. */

int sock_fd_init (unsigned max_cx, unsigned grow)
{
	POOL_LIMITS	limits;
	static int	initialized = 0;

	if (fds)
		return (DDS_RETCODE_OK);

	fds = xmalloc (sizeof (struct pollfd) * FD_INC_SIZE);
	sds = xmalloc (sizeof (Sock_t *) * FD_INC_SIZE);

	fd_max_cx = config_get_number (DC_IP_Sockets, max_cx);
	pool_limits_set (limits, FD_INC_SIZE, fd_max_cx, grow);

	MDS_POOL_TYPE (mem_blocks, 0, limits, sizeof (Sock_t));
	mem_size = mds_alloc (mem_blocks, mem_names, 1);

	if (!fds || !sds
#ifndef FORCE_MALLOC
			 || !mem_size
#endif
				     ) {
		if (fds) {
			xfree (fds);
			fds = NULL;
		}
		if (sds) {
			xfree (sds);
			sds = NULL;
		}
		if (mem_size)
			mds_free (mem_blocks, 1);

		return (DDS_RETCODE_OUT_OF_RESOURCES);
	}
	num_fds = nfds = 0;
	max_fds = FD_INC_SIZE;
	LIST_INIT (sockets);

	if (!initialized) {
		lock_init_nr (sock_lock, "lock");
		initialized = 1;
	}
	return (0);
}

/* sock_fd_final -- Finalize the poll file descriptor array. */

void sock_fd_final (void)
{
	xfree (fds);
	xfree (sds);
	mds_free (mem_blocks, 1);
	LIST_INIT (sockets);
	num_fds = nfds = max_fds = 0;
}

static void sock_poll_update (void)
{
	Sock_t		*sdp, *next;

	if (nfds > max_fds) {
		do {
			max_fds += FD_INC_SIZE;
			if (max_fds > fd_max_cx)
				max_fds = fd_max_cx;
		}
		while (nfds > max_fds);
		fds = xrealloc (fds, sizeof (struct pollfd) * max_fds);
		sds = xrealloc (sds, sizeof (Sock_t *) * max_fds);
		lock_release (poll_lock);
		if (!fds || !sds)
			fatal_printf ("sock_poll_update: can't extend sockets table!");
	}
	num_fds = 0;
	for (sdp = LIST_HEAD (sockets); sdp; sdp = next) {
		next = LIST_NEXT (sockets, *sdp);
		if (sdp->state == PendingRemove) {
			LIST_REMOVE (sockets, *sdp);
			mds_pool_free (&mem_blocks [0], sdp);
		}
		else {
			sdp->state = ActiveSocket;
			sdp->index = num_fds;
			sds [num_fds] = sdp;
			fds [num_fds].fd = sdp->fd;
			fds [num_fds].events = sdp->events;
			fds [num_fds].revents = 0;
			num_fds++;
		}
	}
	sock_update_needed = 0;
}

/* sock_fd_add -- Add a file descriptor and associated callback function. */

int sock_fd_add (int fd, short events, RSDATAFCT rx_fct, void *udata, const char *name)
{
	Sock_t		*sdp;

	if (!max_fds)
		dds_pre_init ();

	lock_take (sock_lock);
	sdp = mds_pool_alloc (&mem_blocks [0]);
	if (!sdp) {
		lock_release (sock_lock);
		return (DDS_RETCODE_OUT_OF_RESOURCES);
	}
	memset (sdp, 0, sizeof (Sock_t));
	/*printf ("socket added: fd=%d, events=%d\n", fd, events);*/
	sdp->state = PendingAdd;
	sdp->index = -1;
	sdp->fd = fd;
	sdp->events = events;
	sdp->fct = rx_fct;
	sdp->udata = udata;
	sdp->name = name;
	LIST_ADD_TAIL (sockets, *sdp);
	nfds++;
	sock_update_needed = 1;
	if (!poll_active)
		sock_poll_update ();

	lock_release (sock_lock);
	return (DDS_RETCODE_OK);
}

/* sock_fd_valid -- Check if a socket is still valid. */

int sock_fd_valid (int fd)
{
	Sock_t		*sdp;
	
	lock_take (sock_lock);
	LIST_FOREACH (sockets, sdp)
		if (sdp->state >= PendingAdd && sdp->fd == fd) {
			lock_release (sock_lock);
			return (1);
		}

	lock_release (sock_lock);
	return (0);
}

/* sock_fd_remove -- Remove a file descriptor. */

void sock_fd_remove (int fd)
{
	Sock_t		*sdp;

	lock_take (sock_lock);
	LIST_FOREACH (sockets, sdp)
		if (sdp->state >= PendingAdd && sdp->fd == fd) {
			sdp->state = PendingRemove;
			nfds--;
			sock_update_needed = 1;
			if (!poll_active)
				sock_poll_update ();
			break;
		}
	lock_release (sock_lock);
}

/* sock_fd_event_socket -- Update the notified events on a socket. */

void sock_fd_event_socket (int fd, short events, int set)
{
	Sock_t		*sdp;

	lock_take (sock_lock);
	LIST_FOREACH (sockets, sdp)
		if (sdp->state >= PendingAdd && sdp->fd == fd) {
			if (set)
				sdp->events |= events;
			else
				sdp->events &= ~events;
			if (sdp->state == ActiveSocket)
				fds [sdp->index].events = sdp->events;
			break;
		}
	lock_release (sock_lock);
}

/* sock_fd_fct_socket -- Update the notified callback function on a socket. */

void sock_fd_fct_socket (int fd, RSDATAFCT fct)
{
	Sock_t		*sdp;

	lock_take (sock_lock);
	LIST_FOREACH (sockets, sdp)
		if (sdp->state >= PendingAdd && sdp->fd == fd) {
			sdp->fct = fct;
			break;
		}
	lock_release (sock_lock);
}

/* sock_fd_udata_socket -- Update the notified user data on a socket. */

void sock_fd_udata_socket (int fd, void *udata)
{
	Sock_t		*sdp;

	lock_take (sock_lock);
	LIST_FOREACH (sockets, sdp)
		if (sdp->state >= PendingAdd && sdp->fd == fd) {
			sdp->udata = udata;
			break;
		}
	lock_release (sock_lock);
}

/* sock_fd_schedule -- Schedule all pending event handlers. */

void sock_fd_schedule (void)
{
	Sock_t		*sdp;
	struct pollfd	*iop;
	unsigned	i;
	RHDATAFCT	fct;
	int		fd;
	void		*user;
	short		events;

	lock_take (sock_lock);
	for (i = 0, iop = fds; i < num_fds; i++, iop++)
		if (iop->revents) {
			sdp = sds [i];
			if (sdp->state == PendingRemove || !sdp->fct)
				continue;

			fct = sdp->fct;
			fd = iop->fd;
			events = iop->revents;
			user = sdp->udata;
			iop->revents = 0;
			lock_release (sock_lock);
			(*fct) (fd, events, user);
			lock_take (sock_lock);
		}
	lock_release (sock_lock);
}

/* sock_fd_poll -- Use poll() or select() to query the state of all file
		   descriptors. */

void sock_fd_poll (unsigned poll_time)
{
	Sock_t		*sdp;
	struct pollfd	*iop;
	unsigned	i, n;

	/* There were changes in the pollfd table, apply them now. */
	if (sock_update_needed)
		sock_poll_update ();

	poll_active = 1;
	/*printf ("*"); fflush (stdout);*/
	n_ready = poll (fds, num_fds, poll_time);
	lock_release (poll_lock);
	if (n_ready < 0) {
		log_printf (LOG_DEF_ID, 0, "sock_fd_poll: poll() returned error: %s\r\n", strerror (errno));
		poll_active = 0;
		return;
	}
	else if (!n_ready) {
		poll_active = 0;
		return;
	}
	lock_take (sock_lock);
	n = 0;
	for (i = 0, iop = fds; i < num_fds; i++, iop++) {
		if (iop->revents) {
			sdp = sds [i];
			if (sdp->state == PendingRemove || !sdp->fct)
				continue;

			/*dbg_printf ("sock: %u %d=0x%04x->0x%04x\r\n", i, iop->fd, iop->events, iop->revents);*/
			dds_lock_ev ();
			dds_ev_pending |= DDS_EV_IO;
			dds_unlock_ev ();
			n = 1;
			break;
		}
	}
	poll_active = n;
	lock_release (sock_lock);
}

#ifdef DDS_DEBUG

/* sock_fd_dump -- Dump all file descriptor contexts. */

void sock_fd_dump (void)
{
	Sock_t		*sdp;

	lock_take (sock_lock);
	LIST_FOREACH (sockets, sdp) {
		if (sdp->state != ActiveSocket)
			continue;

		dbg_printf ("%d: [%d] %s {%s} -> ", sdp->index, sdp->fd, sdp->name, dbg_poll_event_str (sdp->events));
		dbg_printf ("{%s} ", dbg_poll_event_str (fds [sdp->index].events));
		dbg_printf ("Rxfct=0x%lx, U=%p\r\n", (unsigned long) sdp->fct, sdp->udata);
	}
	lock_release (sock_lock);
}

#endif

/* sock_set_socket_nonblocking - Set the socket with fd in non blocking mode. */

int sock_set_socket_nonblocking (int fd)
{
	int ofcmode = fcntl (fd, F_GETFL, 0);

	ofcmode |= O_NONBLOCK;
	if (fcntl (fd, F_SETFL, ofcmode)) {
		perror ("set_socket_nonblocking: fcntl(NONBLOCK)");
		warn_printf ("set_socket_nonblocking: can't set non-blocking!");
		return (0);
	}
	return (1);
}

/* sock_set_tcp_nodelay - set socket tcp option TCP_NODELAY. */

int sock_set_tcp_nodelay (int fd)
{
	int one = 1;

	if (setsockopt (fd, IPPROTO_TCP, TCP_NODELAY, &one, sizeof(one))) {
		perror ("set_tcp_nodelay (): setsockopt () failure");
		warn_printf ("setsockopt (TCP_NODELAY) failed - errno = %d.\r\n", errno);
		return (0);
	}
	return (1);
}

#endif

