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

/* libx.c -- Extra functions, not a part of DDS as such, but often used by
	     applications. */

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include "libx.h"
#include "dds/dds_error.h"

/* astrcmp -- Compare two strings for equality but do it case independent. */

int astrcmp (const char *s1, const char *s2)
{
	char	ch1, ch2;

	do {
		ch1 = *s1++;
		if (ch1 >= 'a' && ch1 <= 'z')
			ch1 -= 'a' - 'A';
		ch2 = *s2++;
		if (ch2 >= 'a' && ch2 <= 'z')
			ch2 -= 'a' - 'A';
		if (ch1 != ch2)
			return (ch1 - ch2);
	}
	while (ch1);
	return (0);
}

/* astrncmp -- Compare two strings for equality assuming a fixed length n but do
	       it case independent. */

int astrncmp (const char *s1, const char *s2, size_t n)
{
	char	ch1, ch2;

	while (n--) {
		ch1 = *s1++;
		if (ch1 >= 'a' && ch1 <= 'z')
			ch1 -= 'a' - 'A';
		ch2 = *s2++;
		if (ch2 >= 'a' && ch2 <= 'z')
			ch2 -= 'a' - 'A';
		if (ch1 != ch2)
			return (ch1 - ch2);
	}
	return (0);
}


DDS_EXPORT void fatal (const char *fmt, ...)
{
	va_list	arg;

	va_start (arg, fmt);
	printf ("\r\nFatal error: ");
	vprintf (fmt, arg);
	printf ("\r\n");
	va_end (arg);
	exit (1);
}

void skip_blanks (const char **args)
{
	while (**args == ' ')
		(*args)++;
}

void skip_string (const char **cmd, char *buf)
{
	const char	*cp = *cmd;

	while (*cp && *cp != ' ' && *cp != '\t' && *cp != '\r' && *cp != '\n')
		*buf++ = *cp++;
	*buf++ = '\0';
	while (*cp == ' ' || *cp == '\t' || *cp == '\r' || *cp == '\n')
		cp++;
	*cmd = cp;
}

int slist_match (const char *list, const char *name, char sep)
{
	const char	*cp;
	unsigned	i;
	size_t		slen, nlen;

	cp = list;
	nlen = strlen (name);
	while (*cp) {
		for (i = 1;; i++)
			if (cp [i] == sep || !cp [i])
				break;
		slen = i;
		if (cp [i] == sep)
			i++;
		if (slen == nlen && !memcmp (name, cp, slen))
			return (1);

		cp += i;
	}
	return (0);
}

int is_dns_name (const char *s)
{
	unsigned	ndots, ncolons;

	ndots = ncolons = 0;
	while (*s) {
		if (((*s < '0' || *s > '9') && 
		     *s != '.' && *s != ':') ||
		    (*s == '.' && ++ndots > 3) ||
		    (*s == ':' && ++ncolons > 1))
			return (1);

		s++;
	}
	return (0);
}

int num_list_contains (const char *s, unsigned v)
{
	unsigned	value = 0, ndigits = 0;

	for (; s; s++) {
		if (*s >= '0' && *s <= '9') {
			value = value * 10 + *s - '0';
			ndigits++;
			if (s [1] == '\0')
				return (value == v);
		}
		else if (*s == ',' || *s == ';' || *s == ':' || *s == '|') {
			if (ndigits) {
				if (value == v)
					return (1);

				value = ndigits = 0;
			}
		}
		else
			break;
	}
	return (0);
}

