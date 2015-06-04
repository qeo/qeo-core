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


#include <stdio.h>
#include <assert.h>
#include "table.h"

void rcl_access (void *p) {}
void rcl_done (void *p) {}

TABLE(int, int_table);
TABLE(char *, cp_table);

static void int_table_dump (int_table *itp)
{
	unsigned	i;
	int		*p;

	TABLE_FOREACH (*itp, i, p)
		printf ("it[%u]=%d ", i, *p);
	printf ("\r\n");
}

void intt_test (void)
{
	int_table	it;
	unsigned	i, j;

	TABLE_INIT (it);
	for (j = 0; j < 10; j++) {
		i = table_add (&it);
		assert (i != 0);
		TABLE_ITEM (it, i) = j + 5;
		printf ("Add: it[%u]=%d\r\n", i, TABLE_ITEM (it, i));
	}
	int_table_dump (&it);
	table_remove (&it, 2);
	printf ("Remove: it[%u]\r\n", 2);
	table_remove (&it, 4);
	printf ("Remove: it[%u]\r\n", 4);
	int_table_dump (&it);

	for (j = 2; j < 12; j++) {
		i = table_add (&it);
		assert (i != 0);
		TABLE_ITEM (it, i) = j;
		printf ("Add: it[%u]=%d\r\n", i, TABLE_ITEM (it, i));
	}
	int_table_dump (&it);
	TABLE_REQUIRE (it, 2, ~0);
	table_reset (&it);
	int_table_dump (&it);
	for (j = 0; j < 5; j++) {
		i = table_add (&it);
		assert (i != 0);
		TABLE_ITEM (it, i) = j + 5;
		printf ("Add: it[%u]=%d\r\n", i, TABLE_ITEM (it, i));
	}
	int_table_dump (&it);
}

static void cp_table_dump (cp_table *itp)
{
	unsigned	i;
	char		**p;

	TABLE_FOREACH (*itp, i, p)
		printf ("cpt[%u]=%s ", i, *p);
	printf ("\r\n");
}

void cpt_test (void)
{
	cp_table	cpt = TABLE_INITIALIZER (char *);
	unsigned	i, j;

	TABLE_REQUIRE (cpt, 4, 20);
	for (j = 0; j < 21; j++) {
		i = table_add (&cpt);
		if (j != 20) {
			assert (i != 0);
			TABLE_ITEM (cpt, i) = "Hello";
			printf ("Add: cpt[%u]=%s\r\n", i, TABLE_ITEM (cpt, i));
		}
		else {
			printf ("Can't add %u - table full!\r\n", j + 1);
			assert (i == 0);
		}
	}
	cp_table_dump (&cpt);
	table_cleanup (&cpt);
	cp_table_dump (&cpt);
}

int main (int argc, char **argv)
{
	intt_test ();
	cpt_test ();

	return (0);
}
