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

/* table.h -- Defines an abstract table that can grow dynamically.
	      Note that contrary to a sequence or a list, table entries always
	      stay at their initial position.  Removed elements are simply
	      zeroed. */

#ifndef __table_h_
#define __table_h_

/* Define a new table type of the given element type and specify its name. */
#define	TABLE(type,name)			\
typedef struct name ## _st {			\
	unsigned _min, _max, _cur, _n, _esize;	\
	void *_ht;				\
	type *_table;				\
} name

TABLE(void *,VoidPtrTable);

/* Initializer for a table of a given type. */
#define	TABLE_INITIALIZER(type)	{ 0, ~0, 0, 0, sizeof (type), NULL, NULL }

/* Set minimum/maximum table size attributes. */
#define	TABLE_REQUIRE(table,min,max) table_require(&table,min,max)

/* Static table initialization. */
#define	TABLE_INIT(table)				\
	(table)._min = (table)._cur = (table)._n = 0;	\
	(table)._max = ~0;				\
	(table)._esize = sizeof (*(table)._table);	\
	(table)._ht = NULL;				\
	(table)._table = NULL

/* Return/set an item from a table at the given index. Caller should make sure
   that the table is large enough and that there is an actual item at the
   given index (i.e. that the handle really exists). */
#define	TABLE_ITEM(table,i)	(table)._table[i]
#define	TABLE_ITEM_SET(table,i,v) (table)._table[i]=(v)

/* Check if a table item exists. */
#define	TABLE_ITEM_EXISTS(table,i) ((i) && (i <= (table)._cur) && (table)._table [i])

/* Set table requirements.  If min is != 0, that many elements will be
   preallocated in the _table and the handle table will be initialized for
   that many elements.  Note that minimum and maximum can be lowered as well
   as enlarged in subsequent calls to table_require. */
int table_require (void *table, unsigned min, unsigned max);

/* Add an element to the table. If successful, a non-0 index is returned which
   can be used to access the element via TABLE_ITEM() and TABLE_ITEM_SET(). */
unsigned table_add (void *table);

/* Remove the element on the given position. The element value will be zeroed
   and the index might be reused in a subsequent table_add(). */
void table_remove (void *table, unsigned n);

/* Remove all remaining elements from the table and reset it to its initial
   state, which depends on its _min attribute. */
int table_reset (void *table);

/* Remove all remaining elements from the table and clean the table completely,
   resetting all the attributes (including _min and _max). */
void table_cleanup (void *table);

/* Walk over a table and for each valid non-0 entry, point p to that table
   element, allowing it to be processed. */
#define TABLE_FOREACH(table,i,p) for(i=1,p=(table)._table+1;i<=(table)._cur;i++,p++) if((table)._table[i])

/* Return an index to the first non-0 element in a table, or 0 if there are
   no more non-NULL entries. */
unsigned table_first (void *table);

/* Return an index to the last non-0 element in a table, or 0 if there are no
   more non-NULL entries. */
unsigned table_last (void *table);

#endif /* !__table_h_ */

