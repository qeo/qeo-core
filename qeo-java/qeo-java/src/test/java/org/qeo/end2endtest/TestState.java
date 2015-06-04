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

package org.qeo.end2endtest;

import org.qeo.Key;

public class TestState
{
    /**
     * Unique id.
     */
    @Key
    public String id; // key

    /**
     * The stage the test has reached.
     */
    public int stage; // non-key

    public TestState()
    {
    }

    public TestState(String idVal, int stageVal)
    {
        super();
        this.id = idVal;
        this.stage = stageVal;
    }

    @Override
    public String toString()
    {
        return "QeoTestCaseState [id=" + id + ", stage=" + stage + "]";
    }
}
