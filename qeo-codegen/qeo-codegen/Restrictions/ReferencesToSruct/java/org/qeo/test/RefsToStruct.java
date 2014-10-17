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

public class RefsToStruct
{
    /**
     * Regular reference to DeviceId
     */
    public org.qeo.DeviceId MyUnbArrayOfDeviceId1;

    /**
     * Reference to sequence DeviceId
     */
    public org.qeo.DeviceId[] MyUnbArrayOfDeviceId2;

    /**
     * Reference to sequence DeviceId, but trough typedef
     */
    public org.qeo.DeviceId[] MyUnbArrayOfDeviceId3;

    /**
     * Reference to sequence DeviceId, but trough typedef -- reference to a different alias
     */
    public org.qeo.DeviceId[] MyUnbArrayOfDeviceId4;

    public RefsToStruct()
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
        final RefsToStruct myObj = (RefsToStruct)obj;
        if (!MyUnbArrayOfDeviceId1.equals(myObj.MyUnbArrayOfDeviceId1)) {
            return false;
        }
        if (!MyUnbArrayOfDeviceId2.equals(myObj.MyUnbArrayOfDeviceId2)) {
            return false;
        }
        if (!MyUnbArrayOfDeviceId3.equals(myObj.MyUnbArrayOfDeviceId3)) {
            return false;
        }
        if (!MyUnbArrayOfDeviceId4.equals(myObj.MyUnbArrayOfDeviceId4)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((MyUnbArrayOfDeviceId1 == null) ? 0 : MyUnbArrayOfDeviceId1.hashCode());
        result = prime * result + ((MyUnbArrayOfDeviceId2 == null) ? 0 : MyUnbArrayOfDeviceId2.hashCode());
        result = prime * result + ((MyUnbArrayOfDeviceId3 == null) ? 0 : MyUnbArrayOfDeviceId3.hashCode());
        result = prime * result + ((MyUnbArrayOfDeviceId4 == null) ? 0 : MyUnbArrayOfDeviceId4.hashCode());
        return result;
    }
}
