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

package org.qeo.sample.gauge.android.reader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.qeo.sample.gauge.NetworkInterfaceSpeedData;
import org.qeo.sample.gauge.android.reader.helper.IfaceGraphDetailsHelper;
import org.qeo.sample.gauge.android.reader.helper.ReaderCallbackHandler;
import org.qeo.sample.gauge.android.reader.model.DeviceModel;

import android.app.Application;
import android.content.res.Configuration;

/**
 * GaugeReaderApplication is the entry point of the QGaugeReader Application.
 *
 **/
public class GaugeReaderApplication
        extends Application
{
    /**
     * ReaderCallbackHandler object reference.
     */
    public static ReaderCallbackHandler sReaderCallBackObj;
    /**
     * List of NetworkInterfaceSpeedData for selected interface.
     */
    public static List<NetworkInterfaceSpeedData> sDataListForClickedIface = new ArrayList<NetworkInterfaceSpeedData>();
    /**
     * List of Writer deviceids.
     */
    public static List<String> sDeviceIdList = new ArrayList<String>();
    /**
     * List of interfaces to be removed from UI.
     */
    public static List<String> sInterfaceList = new ArrayList<String>();

    /**
     * List of all Qeo devices.
     */
    public static List<DeviceModel> sQeoDeviceList = new ArrayList<DeviceModel>();
    /**
     * Helper map to save the graph data for interface.
     */
    public static HashMap<String, IfaceGraphDetailsHelper> sIfaceGraphHelperMap =
            new HashMap<String, IfaceGraphDetailsHelper>();

    /**
     * Displays current orientation.
     */
    public static int sCurrentOrientation = Configuration.ORIENTATION_PORTRAIT;
    /**
     * Displays last saved orientation.
     */
    public static int sLastOrientation = Configuration.ORIENTATION_PORTRAIT;
    @Override
    public void onCreate()
    {
        super.onCreate();
        sReaderCallBackObj = ReaderCallbackHandler.getInstance();

    }

    @Override
    public void onTerminate()
    {
        super.onTerminate();
    }

}
