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

import org.qeo.sample.gauge.android.reader.interfaces.NetworkInterface;


/**
 * The NetworkInterfaceModel is an immutable implementation of the NetworkInterface.
 */
public class NetworkInterfaceModel
        implements NetworkInterface
{
    private final String mName;
    private final String mState;
    private final int mTxPackets;
    private final int mRxPackets;
    private final String mAddress;

    /**
     * Constructs an NetworkInterfaceModel object.
     * 
     * @param name Name of the interface
     * @param state Current state of interface
     * @param txPackets Transferred data in packets
     * @param rxPackets Received data in Packets
     * @param address IP address of interface
     */
    public NetworkInterfaceModel(String name, String state, int txPackets, int rxPackets, String address)
    {
        mName = name;
        mState = state;
        mTxPackets = txPackets;
        mRxPackets = rxPackets;
        mAddress = address;
    }

    @Override
    public String getName()
    {
        return mName;
    }

    @Override
    public String getState()
    {
        return mState;
    }

    @Override
    public int getTxPackets()
    {
        return mTxPackets;
    }

    @Override
    public int getRxPackets()
    {
        return mRxPackets;
    }

    @Override
    public String getIpAddress()
    {
        return mAddress;
    }

}
