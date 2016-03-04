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

package com.technicolor.wifidoctor;

import org.qeo.QeoType;
import org.qeo.QeoType.Behavior;
import org.qeo.Key;

@QeoType(behavior = Behavior.STATE)
public class ScanList
{
    /**
     * the radio that published this scan list (can be either AP or STA)
     */
    @Key
    public int radio;

    /**
     * the scan list entries
     */
    public ScanListEntry[] list;

    /**
     * seconds since Jan 1, 1970
     */
    public long timestamp;

    /**
     * Default constructor.  This is used by Qeo to construct new objects.
     */
    public ScanList()
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
        final ScanList myObj = (ScanList) obj;
        if (radio != myObj.radio) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + radio;
        return result;
    }
}
