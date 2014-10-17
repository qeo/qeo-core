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
 * struct representing an event containing a nested struct. The struct is the key.
 */
@QeoType(behavior = Behavior.STATE)
public class StateWithNestedStructKeyIsStruct
{
    public boolean MyBoolean;

    public byte MyByte;

    public short MyInt16;

    public int MyInt32;

    public long MyInt64;

    public float MyFloat32;

    public String MyString;

    @Key
    public MyStructWithPrimitives MyStructWithPrimitives;

    /**
     * Default constructor.  This is used by Qeo to construct new objects.
     */
    public StateWithNestedStructKeyIsStruct()
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
        final StateWithNestedStructKeyIsStruct myObj = (StateWithNestedStructKeyIsStruct) obj;
        if (!MyStructWithPrimitives.equals(myObj.MyStructWithPrimitives)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((MyStructWithPrimitives == null) ? 0 : MyStructWithPrimitives.hashCode());
        return result;
    }
}
