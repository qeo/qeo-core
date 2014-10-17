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

/**
 * Struct containing enums.
 */
@QeoType
public class MyStructWithEnums
{
    public boolean MyBoolean;

    public byte MyByte;

    public short MyInt16;

    public EnumName MyEnum;

    /**
     * Default constructor.  This is used by Qeo to construct new objects.
     */
    public MyStructWithEnums()
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
        final MyStructWithEnums myObj = (MyStructWithEnums) obj;
        if (MyBoolean != myObj.MyBoolean) {
            return false;
        }
        if (MyByte != myObj.MyByte) {
            return false;
        }
        if (MyInt16 != myObj.MyInt16) {
            return false;
        }
        if (!MyEnum.equals(myObj.MyEnum)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (MyBoolean ? 1 : 0);
        result = prime * result + MyByte;
        result = prime * result + MyInt16;
        result = prime * result + ((MyEnum == null) ? 0 : MyEnum.hashCode());
        return result;
    }
}
