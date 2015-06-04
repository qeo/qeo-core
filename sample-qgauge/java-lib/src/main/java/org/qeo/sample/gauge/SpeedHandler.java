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

package org.qeo.sample.gauge;

import java.util.List;

/**
 * SpeedHandler defines the interface that needs to be implemented to be used by
 * the SpeedCalculator class.
 *
 */
public interface SpeedHandler
{
    /**
     * Handles a new speed measurement as computed by the SpeedCalculator.
     *
     * @param ifaceSpeedDataList List of Interfaces speed data received from reader
     */
    void onSpeedAvailable(List<NetworkInterfaceSpeedData> ifaceSpeedDataList);

    /**
     * Handles the updates if publisher instance gets removed.
     *
     * @param ifNameList The list of network interface names to be removed from UI.
     * @param deviceId the device id of writer that has stopped publishing messages.
     */
    void onSpeedInterfaceRemoved(List<String> ifNameList, String deviceId);

    /**
     * Callback to handle if new writer is available.
     *
     * @param writerId device id of new writer
     */
    void onSpeedDeviceAvailable(String writerId);

    /**
     * Callback to handle if writer is removed.
     *
     * @param writerId device id of removed writer
     */
    void onSpeedDeviceRemoved(String writerId);
}
