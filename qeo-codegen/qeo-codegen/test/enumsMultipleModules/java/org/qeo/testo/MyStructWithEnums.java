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

package org.qeo.testo;

import org.qeo.QeoType;

/**
 * Struct containing enums.
 */
@QeoType
public class MyStructWithEnums
{
    public float MyFloat32;

    public String MyString;

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
        if (MyFloat32 != myObj.MyFloat32) {
            return false;
        }
        if (!MyString.equals(myObj.MyString)) {
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
        result = prime * result + Float.floatToIntBits(MyFloat32);
        result = prime * result + ((MyString == null) ? 0 : MyString.hashCode());
        result = prime * result + ((MyEnum == null) ? 0 : MyEnum.hashCode());
        return result;
    }
}
