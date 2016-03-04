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

/**
 * 
 */
public class StaticFields
{
    private static int staticInt;
    public static final int STATIC_FINAL_INT = 123;
    public int normalInt1;
    public final int finalInt;
    public int normalInt2;

    public static synchronized void setStaticInt(int staticIntValue)
    {
        staticInt = staticIntValue;
    }

    public StaticFields()
    {
        // default constructor
        this(456);
    }

    public StaticFields(int finalIntValue)
    {
        finalInt = finalIntValue;
        normalInt1 = 0;
        normalInt2 = 0;
    }

    @Override
    public String toString()
    {
        return "SFI: " + STATIC_FINAL_INT + " -- SI: " + staticInt + " -- FI: " + finalInt + " -- NI1: " + normalInt1
                + " -- NI2: " + normalInt2;

    }
}
