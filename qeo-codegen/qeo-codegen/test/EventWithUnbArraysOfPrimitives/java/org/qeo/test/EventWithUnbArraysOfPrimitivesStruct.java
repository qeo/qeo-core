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

/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

package org.qeo.test;

import java.util.Arrays;
import org.qeo.QeoType;
import org.qeo.QeoType.Behavior;

/**
 * struct representing an event containing unbound arrays (sequences) of primitives
 */
@QeoType(behavior = Behavior.EVENT)
public class EventWithUnbArraysOfPrimitivesStruct
{
    public boolean[] MyUnbArrayOfBoolean;

    public byte[] MyUnbArrayOfByte;

    public byte[] MyUnbArrayOfByte1;

    public short[] MyUnbArrayOfInt16;

    public int[] MyUnbArrayOfInt32;

    public long[] MyUnbArrayOfInt64;

    public float[] MyUnbArrayOfFloat32;

    public String[] MyUnbArrayOfString;

    /**
     * Default constructor.  This is used by Qeo to construct new objects.
     */
    public EventWithUnbArraysOfPrimitivesStruct()
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
        final EventWithUnbArraysOfPrimitivesStruct myObj = (EventWithUnbArraysOfPrimitivesStruct) obj;
        if (!Arrays.equals(MyUnbArrayOfBoolean, myObj.MyUnbArrayOfBoolean)) {
            return false;
        }
        if (!Arrays.equals(MyUnbArrayOfByte, myObj.MyUnbArrayOfByte)) {
            return false;
        }
        if (!Arrays.equals(MyUnbArrayOfByte1, myObj.MyUnbArrayOfByte1)) {
            return false;
        }
        if (!Arrays.equals(MyUnbArrayOfInt16, myObj.MyUnbArrayOfInt16)) {
            return false;
        }
        if (!Arrays.equals(MyUnbArrayOfInt32, myObj.MyUnbArrayOfInt32)) {
            return false;
        }
        if (!Arrays.equals(MyUnbArrayOfInt64, myObj.MyUnbArrayOfInt64)) {
            return false;
        }
        if (!Arrays.equals(MyUnbArrayOfFloat32, myObj.MyUnbArrayOfFloat32)) {
            return false;
        }
        if (!Arrays.equals(MyUnbArrayOfString, myObj.MyUnbArrayOfString)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(MyUnbArrayOfBoolean);
        result = prime * result + Arrays.hashCode(MyUnbArrayOfByte);
        result = prime * result + Arrays.hashCode(MyUnbArrayOfByte1);
        result = prime * result + Arrays.hashCode(MyUnbArrayOfInt16);
        result = prime * result + Arrays.hashCode(MyUnbArrayOfInt32);
        result = prime * result + Arrays.hashCode(MyUnbArrayOfInt64);
        result = prime * result + Arrays.hashCode(MyUnbArrayOfFloat32);
        result = prime * result + Arrays.hashCode(MyUnbArrayOfString);
        return result;
    }
}
