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

package org.qeo.dynamic.qdm.test;

import org.qeo.QeoType;

@QeoType
public class Substruct1
{
    public String msubstring;

    public int msubint32;

    /**
     * Default constructor.  This is used by Qeo to construct new objects.
     */
    public Substruct1()
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
        final Substruct1 myObj = (Substruct1) obj;
        if (!msubstring.equals(myObj.msubstring)) {
            return false;
        }
        if (msubint32 != myObj.msubint32) {
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
        result = prime * result + msubint32;
        return result;
    }
}
