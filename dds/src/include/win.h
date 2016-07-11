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

/* win.h -- Defines a number of Unix-like functions that have no alternative in
            Windows. */

#ifndef __win_h_
#define __win_h_

#ifdef _WIN32

/*#undef WINVER
#define WINVER 0x0500		** Minimum platform = Windows 2000. */ 
#define WIN32_LEAN_AND_MEAN

#include <windows.h>
#include <process.h>
#include <winsock2.h>
#if _MSC_VER //Only available on MS Visual Studio
#	if !defined (_M_X64) && !defined (_M_IA64)
#		include "vld.h"
#	endif
struct timespec {
	time_t	tv_sec;
	long	tv_nsec;
};
struct timezone {
	int	tz_minuteswest; /* Minutes W of Greenwich. */
	int	tz_dsttime;     /* Type of dst correction. */
};
void usleep (long usec);
#else
#	include <time.h>
#	include <unistd.h>
#endif //_MSC_VER

#define CLOCK_REALTIME	0
#define CLOCK_MONOTONIC	1

int clock_gettime (int X, struct timespec *tv);

int gettimeofday (struct timeval *tv, struct timezone *tz);

#ifndef POLLRDNORM	/* Pre-Vista? No WSAPoll(). */

#define POLLRDNORM  0x0100
#define POLLRDBAND  0x0200
#define POLLIN      (POLLRDNORM | POLLRDBAND)
#define POLLPRI     0x0400

#define POLLWRNORM  0x0010
#define POLLOUT     (POLLWRNORM)
#define POLLWRBAND  0x0020

#define POLLERR     0x0001
#define POLLHUP     0x0002
#define POLLNVAL    0x0004

#endif

#define INLINE

#define snprintf	sprintf_s
#define strncpy(d,s,n)	strcpy_s(d,n,s)
#define sscanf		sscanf_s
#define isblank(c)	((c)==' '||(c)=='\t')


#define PRId64	"lld"
#define PRIu64	"llu"

#if _MSC_VER /* Only available on MS Visual Studio */
#	define vsnprintf(d,dsize,fmt,arg)	vsnprintf_s(d,dsize,dsize-1,fmt,arg)
#	define strtold	strtod
#endif /* _MSC_VER */

#endif

#endif /* !__win_h_ */
