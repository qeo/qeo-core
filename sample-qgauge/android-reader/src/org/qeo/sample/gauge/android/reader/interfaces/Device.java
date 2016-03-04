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

package org.qeo.sample.gauge.android.reader.interfaces;

/**
 *
 * Device Interface needs to be implemented to display the Device details viz. name, description, interfaces to
 * differentiate between different devices.
 *
 */
public interface Device
{
    /**
     * Gets the unique id of device.
     *
     * @return deviceId of device
     */
    String getId();

    /**
     * Gets the name of device.
     *
     * @return Display name of device
     */
    String getName();

    /**
     * Gets the description of device.
     *
     * @return description of device
     */
    String getDescription();

    /**
     * Returns the list of network interfaces for device.
     *
     * @return list if interfaces for device
     */
    NetworkInterface[] getInterfaces();
}
