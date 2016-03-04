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

/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

package org.qeo.test;

import java.util.Arrays;
import org.qeo.QeoType;
import org.qeo.QeoType.Behavior;
import org.qeo.Key;

/**
 * struct representing a state containing unbound arrays (sequences) of primitives
 */
@QeoType(behavior = Behavior.STATE)
public class StateWithUnbArraysOfPrimitivesKeyInt16
{
    public boolean[] MyUnbArrayOfBoolean;

    public byte[] MyUnbArrayOfByte;

    @Key
    public short[] MyUnbArrayOfInt16;

    public int[] MyUnbArrayOfInt32;

    public long[] MyUnbArrayOfInt64;

    public float[] MyUnbArrayOfFloat32;

    public String[] MyUnbArrayOfString;

    /**
     * Default constructor.  This is used by Qeo to construct new objects.
     */
    public StateWithUnbArraysOfPrimitivesKeyInt16()
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
        final StateWithUnbArraysOfPrimitivesKeyInt16 myObj = (StateWithUnbArraysOfPrimitivesKeyInt16) obj;
        if (!Arrays.equals(MyUnbArrayOfInt16, myObj.MyUnbArrayOfInt16)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(MyUnbArrayOfInt16);
        return result;
    }
}
