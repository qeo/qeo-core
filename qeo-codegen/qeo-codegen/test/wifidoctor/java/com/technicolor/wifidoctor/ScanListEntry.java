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

package com.technicolor.wifidoctor;

import org.qeo.QeoType;

@QeoType
public class ScanListEntry
{
    public String BSSID;

    public String SSID;

    public String capabilities;

    /**
     * in MHz
     */
    public int frequency;

    /**
     * in dBm
     */
    public int level;

    /**
     * Default constructor.  This is used by Qeo to construct new objects.
     */
    public ScanListEntry()
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
        final ScanListEntry myObj = (ScanListEntry) obj;
        if (!BSSID.equals(myObj.BSSID)) {
            return false;
        }
        if (!SSID.equals(myObj.SSID)) {
            return false;
        }
        if (!capabilities.equals(myObj.capabilities)) {
            return false;
        }
        if (frequency != myObj.frequency) {
            return false;
        }
        if (level != myObj.level) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((BSSID == null) ? 0 : BSSID.hashCode());
        result = prime * result + ((SSID == null) ? 0 : SSID.hashCode());
        result = prime * result + ((capabilities == null) ? 0 : capabilities.hashCode());
        result = prime * result + frequency;
        result = prime * result + level;
        return result;
    }
}
