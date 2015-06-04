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

package org.qeo.dynamic.qdm.test;

import java.util.Arrays;
import org.qeo.QeoType;

@QeoType
public class Substruct3
{
    public String msubstring;

    public Substruct2[] msubstruct2;

    public Substruct1[] msubstruct1;

    public float msubfloat;

    /**
     * Default constructor.  This is used by Qeo to construct new objects.
     */
    public Substruct3()
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
        final Substruct3 myObj = (Substruct3) obj;
        if (!msubstring.equals(myObj.msubstring)) {
            return false;
        }
        if (!Arrays.equals(msubstruct2, myObj.msubstruct2)) {
            return false;
        }
        if (!Arrays.equals(msubstruct1, myObj.msubstruct1)) {
            return false;
        }
        if (msubfloat != myObj.msubfloat) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((msubstring == null) ? 0 : msubstring.hashCode());
        result = prime * result + Arrays.hashCode(msubstruct2);
        result = prime * result + Arrays.hashCode(msubstruct1);
        result = prime * result + Float.floatToIntBits(msubfloat);
        return result;
    }
}
