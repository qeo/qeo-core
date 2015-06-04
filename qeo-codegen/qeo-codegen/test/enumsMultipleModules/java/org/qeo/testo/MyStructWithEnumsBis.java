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

package org.qeo.testo;

import org.qeo.QeoType;

/**
 * Struct containing enums.
 */
@QeoType
public class MyStructWithEnumsBis
{
    public boolean MyBooleanBis;

    public byte MyByteBis;

    public short MyInt16Bis;

    public EnumNameBis MyEnumBis;

    /**
     * Default constructor.  This is used by Qeo to construct new objects.
     */
    public MyStructWithEnumsBis()
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
        final MyStructWithEnumsBis myObj = (MyStructWithEnumsBis) obj;
        if (MyBooleanBis != myObj.MyBooleanBis) {
            return false;
        }
        if (MyByteBis != myObj.MyByteBis) {
            return false;
        }
        if (MyInt16Bis != myObj.MyInt16Bis) {
            return false;
        }
        if (!MyEnumBis.equals(myObj.MyEnumBis)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (MyBooleanBis ? 1 : 0);
        result = prime * result + MyByteBis;
        result = prime * result + MyInt16Bis;
        result = prime * result + ((MyEnumBis == null) ? 0 : MyEnumBis.hashCode());
        return result;
    }
}
