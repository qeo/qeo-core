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

/**
 * struct representing an event containing unbound arrays (sequences) of unbound arrays
 */
public class EventWithUnbArraysOfUnbArrays
{
    public boolean[] MyUnbArrayOfUnbArrayOfBoolean;

    public byte[] MyUnbArrayOfUnbArrayOfByte;

    public short[][] MyUnbArrayOfUnbArrayOfInt16;

    public int[][] MyUnbArrayOfUnbArrayOfInt32;

    public long[][] MyUnbArrayOfUnbArrayOfInt64;

    public float[][] MyUnbArrayOfUnbArrayOfFloat32;

    public String[][] MyUnbArrayOfUnbArrayOfString;

    public org.qeo.DeviceId[] MyUnbArrayOfDeviceId;

    public int MyInt32;

    public String MyString;

    public EventWithUnbArraysOfUnbArrays()
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
        final EventWithUnbArraysOfUnbArrays myObj = (EventWithUnbArraysOfUnbArrays)obj;
        if (!MyUnbArrayOfUnbArrayOfBoolean.equals(myObj.MyUnbArrayOfUnbArrayOfBoolean)) {
            return false;
        }
        if (!MyUnbArrayOfUnbArrayOfByte.equals(myObj.MyUnbArrayOfUnbArrayOfByte)) {
            return false;
        }
        if (!MyUnbArrayOfUnbArrayOfInt16.equals(myObj.MyUnbArrayOfUnbArrayOfInt16)) {
            return false;
        }
        if (!MyUnbArrayOfUnbArrayOfInt32.equals(myObj.MyUnbArrayOfUnbArrayOfInt32)) {
            return false;
        }
        if (!MyUnbArrayOfUnbArrayOfInt64.equals(myObj.MyUnbArrayOfUnbArrayOfInt64)) {
            return false;
        }
        if (!MyUnbArrayOfUnbArrayOfFloat32.equals(myObj.MyUnbArrayOfUnbArrayOfFloat32)) {
            return false;
        }
        if (!MyUnbArrayOfUnbArrayOfString.equals(myObj.MyUnbArrayOfUnbArrayOfString)) {
            return false;
        }
        if (!MyUnbArrayOfDeviceId.equals(myObj.MyUnbArrayOfDeviceId)) {
            return false;
        }
        if (MyInt32 != myObj.MyInt32) {
            return false;
        }
        if (!MyString.equals(myObj.MyString)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((MyUnbArrayOfUnbArrayOfBoolean == null) ? 0 : MyUnbArrayOfUnbArrayOfBoolean.hashCode());
        result = prime * result + ((MyUnbArrayOfUnbArrayOfByte == null) ? 0 : MyUnbArrayOfUnbArrayOfByte.hashCode());
        result = prime * result + ((MyUnbArrayOfUnbArrayOfInt16 == null) ? 0 : MyUnbArrayOfUnbArrayOfInt16.hashCode());
        result = prime * result + ((MyUnbArrayOfUnbArrayOfInt32 == null) ? 0 : MyUnbArrayOfUnbArrayOfInt32.hashCode());
        result = prime * result + ((MyUnbArrayOfUnbArrayOfInt64 == null) ? 0 : MyUnbArrayOfUnbArrayOfInt64.hashCode());
        result = prime * result + ((MyUnbArrayOfUnbArrayOfFloat32 == null) ? 0 : MyUnbArrayOfUnbArrayOfFloat32.hashCode());
        result = prime * result + ((MyUnbArrayOfUnbArrayOfString == null) ? 0 : MyUnbArrayOfUnbArrayOfString.hashCode());
        result = prime * result + ((MyUnbArrayOfDeviceId == null) ? 0 : MyUnbArrayOfDeviceId.hashCode());
        result = prime * result + MyInt32;
        result = prime * result + ((MyString == null) ? 0 : MyString.hashCode());
        return result;
    }
}
