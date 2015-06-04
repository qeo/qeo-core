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

/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

package org.qeo.test;

import org.qeo.Key;

/**
 * struct representing an event containing unbound arrays (sequences) of unbound arrays
 */
public class StateWithUnbArraysOfUnbArraysKeyInt16
{
    public boolean[][] MyUnbArrayOfUnbArrayOfBoolean;

    public byte[][] MyUnbArrayOfUnbArrayOfByte;

    @Key
    public short[][] MyUnbArrayOfUnbArrayOfInt16;

    public int[][] MyUnbArrayOfUnbArrayOfInt32;

    public long[][] MyUnbArrayOfUnbArrayOfInt64;

    public float[][] MyUnbArrayOfUnbArrayOfFloat32;

    public String[][] MyUnbArrayOfUnbArrayOfString;

    public StateWithUnbArraysOfUnbArraysKeyInt16()
    {
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }
        if ((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }
        final StateWithUnbArraysOfUnbArraysKeyInt16 myObj = (StateWithUnbArraysOfUnbArraysKeyInt16)obj;
        if (!MyUnbArrayOfUnbArrayOfInt16.equals(myObj.MyUnbArrayOfUnbArrayOfInt16)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((MyUnbArrayOfUnbArrayOfInt16 == null) ? 0 : MyUnbArrayOfUnbArrayOfInt16.hashCode());
        return result;
    }
}
