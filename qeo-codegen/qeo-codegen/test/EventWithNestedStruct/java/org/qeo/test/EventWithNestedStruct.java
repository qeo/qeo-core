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

/**
 * struct representing an event containing a nested struct
 */
@QeoType(behavior = Behavior.EVENT)
public class EventWithNestedStruct
{
    public MyStructWithPrimitives MyStructWithPrimitives;

    public org.qeo.DeviceId MyDeviceId;

    public boolean MyBoolean;

    public byte MyByte;

    public short MyInt16;

    public int MyInt32;

    public long MyInt64;

    public float MyFloat32;

    public String MyString;

    /**
     * Default constructor.  This is used by Qeo to construct new objects.
     */
    public EventWithNestedStruct()
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
        final EventWithNestedStruct myObj = (EventWithNestedStruct) obj;
        if (!MyStructWithPrimitives.equals(myObj.MyStructWithPrimitives)) {
            return false;
        }
        if (!MyDeviceId.equals(myObj.MyDeviceId)) {
            return false;
        }
        if (MyBoolean != myObj.MyBoolean) {
            return false;
        }
        if (MyByte != myObj.MyByte) {
            return false;
        }
        if (MyInt16 != myObj.MyInt16) {
            return false;
        }
        if (MyInt32 != myObj.MyInt32) {
            return false;
        }
        if (MyInt64 != myObj.MyInt64) {
            return false;
        }
        if (MyFloat32 != myObj.MyFloat32) {
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
        result = prime * result + ((MyStructWithPrimitives == null) ? 0 : MyStructWithPrimitives.hashCode());
        result = prime * result + ((MyDeviceId == null) ? 0 : MyDeviceId.hashCode());
        result = prime * result + (MyBoolean ? 1 : 0);
        result = prime * result + MyByte;
        result = prime * result + MyInt16;
        result = prime * result + MyInt32;
        result = prime * result + (int) (MyInt64 ^ (MyInt64 >>> 32));
        result = prime * result + Float.floatToIntBits(MyFloat32);
        result = prime * result + ((MyString == null) ? 0 : MyString.hashCode());
        return result;
    }
}
