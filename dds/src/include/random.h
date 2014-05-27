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

/* random.h -- Fast random number generator, based on the LCG algorithm. */

#ifndef __fastrand_h_
#define __fastrand_h_

#define MAX_FRAND_BITS	15	/* Max. # of significant bits. */

void fastsrand (unsigned seed);

/* Seed the random number generator. */

unsigned fastrand (void);

/* Return a random number with 15 significant bits. */

#endif /* !__fastrand_h_ */

