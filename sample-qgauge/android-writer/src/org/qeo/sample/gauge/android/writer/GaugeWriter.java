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

package org.qeo.sample.gauge.android.writer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;

import org.qeo.sample.gauge.NetStatMessage;
import org.qeo.sample.gauge.NetStatProvider;
import org.qeo.sample.gauge.NetStatPublisher;
import org.qeo.system.DeviceId;

import android.util.Log;

/**
 * 
 * GaugeWriter is a NetStatPublisher for Android Platform based on /sys/class/net. Reads the data from network
 * interfaces for traffic statistics.
 * 
 */

public class GaugeWriter
        implements NetStatProvider
{
    private static final String LOGGING_TAG = GaugeWriter.class.getSimpleName();
    private final DeviceId mDeviceId;
    private final NetworkInterface[] mITFs;
    private String mRxbytes;
    private String mRxpackets;
    private String mTxbytes;
    private String mTxpackets;

    /**
     * Constructor.
     * 
     * @param device own device name
     */
    public GaugeWriter(DeviceId device)
    {
        mDeviceId = device;
        mITFs = retrieveItfs();
    }

    @Override
    public NetStatMessage[] getCurrentStats()
    {
        final ArrayList<NetStatMessage> list = new ArrayList<NetStatMessage>();
        for (NetworkInterface nif : mITFs) {
            mRxbytes = Constants.INTERFACE_FILE_PATH + nif.getDisplayName() + Constants.RX_BYTES_PATH;
            mRxpackets = Constants.INTERFACE_FILE_PATH + nif.getDisplayName() + Constants.RX_PACKETS_PATH;
            mTxbytes = Constants.INTERFACE_FILE_PATH + nif.getDisplayName() + Constants.TX_BYTES_PATH;
            mTxpackets = Constants.INTERFACE_FILE_PATH + nif.getDisplayName() + Constants.TX_PACKETS_PATH;
            try {
                NetStatMessage msg = new NetStatMessage();
                msg.ifName = nif.getDisplayName();
                msg.deviceId = mDeviceId;
                msg.bytesIn = readDataFromIfaceFile(mRxbytes);
                msg.packetsIn = readDataFromIfaceFile(mRxpackets);
                msg.bytesOut = readDataFromIfaceFile(mTxbytes);
                msg.packetsOut = readDataFromIfaceFile(mTxpackets);
                msg.timestamp = System.nanoTime();
                list.add(msg);
            }
            catch (FileNotFoundException e) {
                Log.e(LOGGING_TAG, e.getMessage());
            }
            catch (NumberFormatException e) {
                Log.e(LOGGING_TAG, e.getMessage());
            }
        }
        return list.toArray(new NetStatMessage[list.size()]);
    }

    @Override
    public void publisherStopped(NetStatPublisher devicePublisher, Throwable cause)
    {
        Log.e(LOGGING_TAG, cause.getMessage());
    }

    /**
     * Reads the data from file and converts to long format for packets/bytes sent or received over the interface.
     * 
     * @param filename Name of file to read the data
     * @return The long value representing the bytes/packets data.
     * @throws FileNotFoundException if File does not exist
     * @throws NumberFormatException if contents of file are non-numeric.
     */

    private long readDataFromIfaceFile(String filename)
        throws FileNotFoundException, NumberFormatException
    {
        RandomAccessFile f;
        f = new RandomAccessFile(filename, "r");
        try {
            String contents = f.readLine();
            if (contents != null && !contents.equals("")) {
                return Long.parseLong(contents);
            }
        }
        catch (IOException ioex) {
            Log.e(LOGGING_TAG, ioex.getMessage());
        }
        finally {
            if (f != null) {
                try {
                    f.close();
                }
                catch (IOException e) {
                    Log.e(LOGGING_TAG, e.getMessage());
                }
            }
        }
        return -1;
    }

    /**
     * Fetches an array of NetworkInterfaces available on this device.
     *
     * @return array of available NetworkInterfaces, empty array if none or ico errors
     */
    private NetworkInterface[] retrieveItfs()
    {
        try {
            ArrayList<NetworkInterface> lst = Collections.list(NetworkInterface.getNetworkInterfaces());
            return lst.toArray(new NetworkInterface[lst.size()]);
        }
        catch (SocketException se) {
            Log.e(LOGGING_TAG, se.getMessage());
        }
        return new NetworkInterface[0];
    }
}
