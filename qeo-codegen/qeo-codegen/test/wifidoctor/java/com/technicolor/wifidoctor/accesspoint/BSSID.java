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

package com.technicolor.wifidoctor.accesspoint;

import org.qeo.QeoType;
import org.qeo.QeoType.Behavior;
import org.qeo.Key;

@QeoType(behavior = Behavior.STATE)
public class BSSID
{
    /**
     * ID of the TestRequest for which these stats are published. A value of 0 indicates these are passive monitoring stats, not associated with a specific test request
     */
    @Key
    public int testId;

    /**
     * MAC address associated with BSSID
     */
    @Key
    public String MACAddress;

    /**
     * Reference to the Radio object this BSSID belongs to.
     */
    public int radio;

    /**
     * Integer percentage
     */
    public byte mediumBusyIBSS;

    /**
     * Integer percentage
     */
    public byte mediumBusyOBSS;

    /**
     * statistics per associated station
     */
    public StationStats[] stationStats;

    /**
     * Default constructor.  This is used by Qeo to construct new objects.
     */
    public BSSID()
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
        final BSSID myObj = (BSSID) obj;
        if (testId != myObj.testId) {
            return false;
        }
        if (!MACAddress.equals(myObj.MACAddress)) {
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
        result = prime * result + ((MACAddress == null) ? 0 : MACAddress.hashCode());
        return result;
    }
}
