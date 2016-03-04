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

package org.qeo.util;

import junit.framework.TestCase;

import org.qeo.unittesttypes.TestState;

public class TypeUtilTest
        extends TestCase
{

    public void test1()
    {
        final TestState state1 = new TestState("id", "name", 1);
        final TestState state2 = new TestState("id", "name", 1); // identical to state1
        final TestState state3 = new TestState("id", "name", 2); // non-keyfield differs
        final TestState state4 = new TestState("id", "name2", 1); // non-keyfield differs
        final TestState state5 = new TestState("id2", "name", 1); // keyfield differs

        // test some null values
        assertFalse(TypeUtil.equalKeys(null, null));
        assertFalse(TypeUtil.equalKeys(state1, null));
        assertFalse(TypeUtil.equalKeys(null, state1));

        // obvious one
        assertTrue(TypeUtil.equalKeys(state1, state1));

        assertTrue(TypeUtil.equalKeys(state1, state2));
        assertTrue(TypeUtil.equalKeys(state1, state3));
        assertTrue(TypeUtil.equalKeys(state1, state4));
        assertFalse(TypeUtil.equalKeys(state1, state5));
    }
}
