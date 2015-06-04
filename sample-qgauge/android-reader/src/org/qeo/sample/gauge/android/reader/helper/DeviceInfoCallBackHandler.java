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

package org.qeo.sample.gauge.android.reader.helper;

import java.util.List;

import org.qeo.sample.gauge.android.reader.model.DeviceModel;

/**
 * Interface to handle the callback to update Device list screen based on updates from DeviceInfoReader.
 */

public interface DeviceInfoCallBackHandler
{

    /**
     * Notifies this callback handler that the DeviceInfo list is updated.
     *
     * @param deviceList List of Qeo writer devices to be updated at UI level.
     */
    void onDeviceInfoUpdated(List<DeviceModel> deviceList);
}
