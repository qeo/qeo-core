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

package org.qeo.sample.gauge.android.reader.interfaces;

/**
 * The NetworkInterface is used to represent network interfaces present on a device.
 */
public interface NetworkInterface
{
    /**
     * Returns the interface name.
     *
     * @return Display name of interface
     */
    String getName();

    /**
     * Returns the interface state.
     *
     * @return current state of interface
     */
    String getState();

    /**
     * Returns packet data transferred over the interface.
     *
     * @return number of packets transferred
     */
    int getTxPackets();

    /**
     * Returns packet data received over the interface.
     *
     * @return number of packets received
     */
    int getRxPackets();

    /**
     * Returns the IP address of interface.
     *
     * @return string representing the IP address
     */
    String getIpAddress();

}
