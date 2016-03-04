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

package org.qeo.sample.gauge.android.reader.model;

import org.qeo.sample.gauge.android.reader.interfaces.Device;
import org.qeo.sample.gauge.android.reader.interfaces.NetworkInterface;

/**
 *
 * Device class implements the IDevice interface and acts as a model to fill with device data.
 *
 */
public class DeviceModel
        implements Device
{
    private final String mId;
    private final String mName;
    private final String mDescription;

    /**
     * Constructs a DeviceModel object with deviceId and other details.
     *
     * @param id deviceId
     * @param name device name
     * @param description detailed description of device
     */
    public DeviceModel(String id, String name, String description)
    {
        mId = id;
        mName = name;
        mDescription = description;
    }

    @Override
    public String getId()
    {
        return mId;
    }

    @Override
    public String getName()
    {
        return mName;
    }

    @Override
    public String getDescription()
    {
        return mDescription;
    }

    @Override
    public NetworkInterface[] getInterfaces()
    {
        return null;
    }
}
