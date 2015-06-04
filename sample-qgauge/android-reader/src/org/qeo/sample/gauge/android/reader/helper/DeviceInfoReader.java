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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.qeo.QeoFactory;
import org.qeo.StateReader;
import org.qeo.StateReaderListener;
import org.qeo.exception.QeoException;
import org.qeo.sample.gauge.android.reader.model.DeviceModel;
import org.qeo.system.DeviceInfo;

import android.util.Log;

/**
 * DeviceInfoReader class creates the Qeo reader to read the data of type- DeviceInfo and retrieves the list of all Qeo
 * enabled devices.
 */
public class DeviceInfoReader
{
    private static final String LOGGING_TAG = DeviceInfoReader.class.getSimpleName();
    private StateReader<DeviceInfo> mDevices = null;

    /**
     * Constructs a DeviceInfoReader.
     *
     * @param handler - callback handler to update UI
     * @param qeo - Qeo object to create reader
     */
    public DeviceInfoReader(final DeviceInfoCallBackHandler handler, QeoFactory qeo)
    {
        /**
         * Create a state reader for DeviceInfo and listen for updates using the StateReaderListener
         * interface.Implementation of a StateReaderListener interface that will be used in triggering updates to the
         * device list.
         */
        try {
            mDevices = qeo.createStateReader(DeviceInfo.class, new StateReaderListener() {

                /**
                 * Whenever anything changes with respect to discovered devices this method will be called. This can be
                 * either due to the discovery of a new device, the disappearance of an existing device or the update of
                 * the data of an existing device. In response to this notification we will update our device list in
                 * the UI.
                 */
                @Override
                public void onUpdate()
                {

                    List<DeviceModel> devicelist = new ArrayList<DeviceModel>();
                    for (final DeviceInfo device : mDevices) {

                        String deviceId = new UUID(device.deviceId.upper, device.deviceId.lower).toString();
                        DeviceModel d = new DeviceModel(deviceId, device.modelName, device.manufacturer);
                        devicelist.add(d);
                    }
                    if (devicelist.size() != 0) {
                        handler.onDeviceInfoUpdated(devicelist);
                    }

                }
            });
        }
        catch (QeoException e) {
            Log.d(LOGGING_TAG, e.getMessage());
        }
    }

    /**
     * Stop the device info reader in a clean way.
     */
    public void stop()
    {
        if (mDevices != null) {
            mDevices.close();
            mDevices = null;
        }
    }
}
