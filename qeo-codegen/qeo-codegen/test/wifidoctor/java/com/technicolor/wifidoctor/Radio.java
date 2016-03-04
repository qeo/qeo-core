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
public class Radio
{
    /**
     * ID of the TestRequest for which these stats are published. A value of 0 indicates these are passive monitoring stats, not associated with a specific test request
     */
    @Key
    public int testId;

    /**
     * 	 ID of the wifi radio. Basically a random number, assumed to be unique over the whole Qeo realm. 	 In the future, we'd probably use a UUID here but for the POC that's a bit overkill. 	
     */
    @Key
    public int id;

    /**
     * 	 Qeo Device ID of the device this radio belongs to. 	 Useful in the case of multiple devices that play the Access Point role within one realm. 	 Qeo provides a built-in function to retrieve this DeviceID. 	
     */
    public org.qeo.DeviceId device;

    /**
     * in MHz
     */
    public int frequency;

    /**
     * Integer percentage. For Station radios, this value is probably meaningless and would be 0.
     */
    public byte mediumBusy;

    /**
     * Default constructor.  This is used by Qeo to construct new objects.
     */
    public Radio()
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
        final Radio myObj = (Radio) obj;
        if (testId != myObj.testId) {
            return false;
        }
        if (id != myObj.id) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + testId;
        result = prime * result + id;
        return result;
    }
}
