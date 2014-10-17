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

import org.qeo.QeoType;
import org.qeo.QeoType.Behavior;
import org.qeo.Key;

/**
 * struct representing an event containing a nested struct
 */
@QeoType(behavior = Behavior.STATE)
public class StateWithNestedStructKeyIsInt16
{
    public boolean MyBoolean;

    public byte MyByte;

    @Key
    public short MyInt16;

    public int MyInt32;

    public long MyInt64;

    public float MyFloat32;

    public String MyString;

    public MyStructWithPrimitives MyStructWithPrimitives;

    public org.qeo.DeviceId MyDeviceId;

    /**
     * Default constructor.  This is used by Qeo to construct new objects.
     */
    public StateWithNestedStructKeyIsInt16()
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
        final StateWithNestedStructKeyIsInt16 myObj = (StateWithNestedStructKeyIsInt16) obj;
        if (MyInt16 != myObj.MyInt16) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + MyInt16;
        return result;
    }
}
