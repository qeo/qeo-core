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

/* table.c -- Implements an abstract table that can grow dynamically.
	      Note that contrary to a sequence or a list, table entries always
	      stay at their initial position.  Removed elements are simply
	      zeroed. */

#include <stdio.h>
#include "log.h"
#include "error.h"
#include "handle.h"
#include "pool.h"
#include "dds/dds_error.h"
#include "table.h"

#define	DEF_SIZE_INC	16

static int table_set (VoidPtrTable *dh, unsigned n)
{
	size_t	t;

	dh->_ht = handle_init (n);
	if (!dh->_ht) {
		log_printf (POOL_ID, 0, "DH: out of memory allocating handles!");
		return (DDS_RETCODE_OUT_OF_RESOURCES);
	}
	t = (n + 1) * dh->_esize;
	dh->_table = xmalloc (t);
	if (!dh->_table) {
		handle_final (dh->_ht);
		dh->_ht = NULL;
		log_printf (POOL_ID, 0, "DH: out of memory allocating table!");
		return (DDS_RETCODE_OUT_OF_RESOURCES);
	}
	memset (dh->_table, 0, t);
	dh->_cur = n;
	return (DDS_RETCODE_OK);
}

static int table_extend (VoidPtrTable *dh, unsigned n)
{
	void	*handles;
	void	*table;

	table = xrealloc (dh->_table, (dh->_cur + 1 + n) * dh->_esize);
	if (!table) {
		log_printf (POOL_ID, 0, "DH: out of memory while enlarging table!");
		return (DDS_RETCODE_OUT_OF_RESOURCES);
	}
	dh->_table = table;
	handles = handle_extend (dh->_ht, n);
	if (!handles) {
		log_printf (POOL_ID, 0, "DH: can't extend handles list!");
		dh->_table = xrealloc (table, (dh->_cur + 1) * dh->_esize);
		return (DDS_RETCODE_OUT_OF_RESOURCES);
	}
	dh->_ht = handles;
	memset ((unsigned char *) dh->_table + ((dh->_cur + 1) * dh->_esize),
		0,
		n * dh->_esize);
	dh->_cur += n;
	return (DDS_RETCODE_OK);
}

int table_reset (void *table)
{
	VoidPtrTable	*dh = (VoidPtrTable *) table;

	if (!dh->_ht && !dh->_min)
		return (DDS_RETCODE_OK);

	if (dh->_ht) {
		if (dh->_cur <= dh->_min) {
			if (dh->_n) {
				handle_reset (dh->_ht);
				memset (dh->_table, 0, (dh->_cur + 1) * dh->_esize);
				dh->_n = 0;
			}
			if (dh->_min > dh->_cur)
				return (table_extend (dh, dh->_min - dh->_cur));
			else
				return (DDS_RETCODE_OK);
		}
		handle_final (dh->_ht);
		dh->_ht = NULL;
		xfree (dh->_table);
		dh->_table = NULL;
		dh->_n = dh->_cur = 0;
	}
	if (!dh->_min)
		return (DDS_RETCODE_OK);

	return (table_set (dh, dh->_min));
}

void table_cleanup (void *table)
{
	VoidPtrTable	*dh = (VoidPtrTable *) table;

	if (dh->_ht) {
		handle_final (dh->_ht);
		dh->_ht = NULL;
		xfree (dh->_table);
		dh->_table = NULL;
		dh->_cur = dh->_n = 0;
	}
	dh->_min = 0;
	dh->_max = ~0;
}

int table_require (void *table, unsigned min, unsigned max)
{
	VoidPtrTable	*dh = (VoidPtrTable *) table;

	if (!dh || min > max || dh->_cur > max)
		return (DDS_RETCODE_PRECONDITION_NOT_MET);

	dh->_max = max;
	if (min == dh->_min)
		return (DDS_RETCODE_OK);

	dh->_min = min;
	if (!dh->_n)
		return (table_reset (dh));
	else if (min > dh->_cur)
		return (table_extend (dh, min - dh->_cur));
	else
		return (DDS_RETCODE_OK);
}

unsigned table_add (void *table)
{
	VoidPtrTable	*dh = (VoidPtrTable *) table;
	unsigned	n, h;
	int		error;

	if (!dh)
		return (0);

	if (dh->_n == dh->_cur) {
		if (!dh->_min)
			n = dh->_cur + DEF_SIZE_INC;
		else
			n = dh->_cur + dh->_min;
		if (n > dh->_max)
			n = dh->_max;
		n -= dh->_cur;
		if (!n) {
			log_printf (POOL_ID, 0, "DH: handles limit reached!");
			return (0);
		}
		if (dh->_cur)
			error = table_extend (dh, n);
		else
			error = table_set (dh, n);
		if (error)
			return (0);
	}
	h = handle_alloc (dh->_ht);
	if (!h) {
		log_printf (POOL_ID, 0, "DH: can't allocate handle!");
		return (0);
	}
	dh->_n++;
	return (h);
}

/* table_remove -- Remove the element on the given position. The element value
		   will be zeroed and the index might be reused in a subsequent
		   table_add(). */

void table_remove (void *table, unsigned n)
{
	VoidPtrTable	*dh = (VoidPtrTable *) table;

	if (!dh || !dh->_ht || n > dh->_cur || !dh->_n) {
		log_printf (POOL_ID, 0, "DH: invalid handle removal!");
		return;
	}
	handle_free (dh->_ht, n);
	memset ((unsigned char *) dh->_table + n * dh->_esize, 0, dh->_esize);
	if (!--dh->_n && dh->_cur > dh->_min)
		table_reset (dh);
}

/* table_first -- Return an index to the first non-NULL element in a table, or
		  0 if there are no more non-0 entries. */

unsigned table_first (void *table)
{
	VoidPtrTable	*dh = (VoidPtrTable *) table;
	unsigned	i;

	if (!dh->_n)
		return (0);

	for (i = 1; i <= dh->_cur; i++)
		if (dh->_table [i])
			return (i);

	return (0);
}

/* table_last -- Return an index to the last non-NULL element in a table, or 0
		 if there are no more non-0 entries. */

unsigned table_last (void *table)
{
	VoidPtrTable	*dh = (VoidPtrTable *) table;
	unsigned	i;

	if (!dh->_n)
		return (0);

	for (i = dh->_cur; i; i--)
		if (dh->_table [i])
			return (i);

	return (0);
}

