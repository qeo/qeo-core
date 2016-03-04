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

package org.qeo.unittesttypes;

import java.util.Random;

import org.qeo.Key;

public class TestState
{
    @Key
    public String id; // key

    public String name; // non-key

    public int i; // non-key

    public TestState()
    {
        name = null;
    }

    public TestState(String idVal, String nameVal, int iVal)
    {
        super();
        this.id = idVal;
        this.name = nameVal;
        this.i = iVal;
    }

    public TestState(String idVal, String nameVal)
    {
        this(idVal, nameVal, new Random().nextInt(100));
    }

    /**
     * Copy constructor
     */
    public TestState(TestState s)
    {
        this.id = s.id;
        this.name = s.name;
        this.i = s.i;
    }

    @Override
    public String toString()
    {
        return "TestState: " + id + " -- " + i + " -- " + name;
    }
}
