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

import org.qeo.Key;

/**
 * 
 */
public class NestedTypes
{
    @Key
    public String mId;

    public TestTypes mTestType;
    public TestTypes mTestType2;

    public TestTypes[] mTestTypeArray;

    @Key
    public TestState mTestState;

    public NestedTypes()
    {

    }

    public NestedTypes(TestTypes testType, TestTypes testType2, TestState testState, String id)
    {
        mTestType = testType;
        mTestType2 = testType2;
        mTestState = testState;
        mId = id;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(mId);
        sb.append(mTestType);
        sb.append(mTestType2);
        sb.append(mTestState);
        return sb.toString();
    }
}
