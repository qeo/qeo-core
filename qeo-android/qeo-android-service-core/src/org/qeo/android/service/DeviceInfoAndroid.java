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

package org.qeo.android.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.android.service.db.DBHelper;
import org.qeo.android.service.db.TableInfo;
import org.qeo.system.DeviceId;
import org.qeo.system.DeviceInfo;

/**
 * This class is responsible for constructing, publishing the device info.
 */
public final class DeviceInfoAndroid
{
    private static final Logger LOG = Logger.getLogger(QeoService.TAG);
    private static final String USB_ID = "/sys/class/android_usb/android0/iSerial";
    private final DeviceInfo mDeviceInfo;
    private static DeviceInfoAndroid sDeviceInfoAndroid = null;

    /**
     * Get the DeviceInfoAndroid instance.
     * 
     * @return DeviceInfo instance.
     */
    public static synchronized DeviceInfoAndroid getInstance()
    {
        if (sDeviceInfoAndroid == null) {
            sDeviceInfoAndroid = new DeviceInfoAndroid();
        }
        return sDeviceInfoAndroid;
    }

    /**
     * Constructor of the device info class.
     * 
     */
    private DeviceInfoAndroid()
    {
        // open database for info
        DBHelper helper = new DBHelper(ServiceApplication.getApp());
        TableInfo tableInfo = new TableInfo(helper);

        mDeviceInfo = new DeviceInfo();
        mDeviceInfo.manufacturer = android.os.Build.MANUFACTURER;
        mDeviceInfo.modelName = android.os.Build.MODEL;
        mDeviceInfo.productClass = android.os.Build.PRODUCT;
        mDeviceInfo.hardwareVersion = android.os.Build.HARDWARE;
        mDeviceInfo.softwareVersion = Integer.toString(android.os.Build.VERSION.SDK_INT);
        mDeviceInfo.serialNumber = getSerialNumber(tableInfo);
        mDeviceInfo.userFriendlyName = android.os.Build.MODEL + " - " + mDeviceInfo.serialNumber;
        mDeviceInfo.configURL = "";
        // Initializing the Device ID (128 bits)
        mDeviceInfo.deviceId = getDeviceId(tableInfo);
        LOG.fine("DeviceInfo: " + mDeviceInfo.toString());

        // close database helper
        helper.close();
    }

    private String getSerialNumber(TableInfo tableInfo)
    {
        // see if value is cached in the database
        String serial = tableInfo.getValue(TableInfo.KEY_DEVICE_INFO_SERIAL);
        if (serial != null) {
            return serial;
        }

        // value not yet in the database, generate it.

        // Some device don't have android.os.Build.SERIAL (ro.serialno in /system/build.prop file) set
        // Use the ADB Serial Nr from /sys/class/android_usb/android0/iSerial as backup
        // Note: android.os.Build.SERIAL requires minimum API level 9
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD
            && null != android.os.Build.SERIAL && !android.os.Build.SERIAL.equals("")
            && !android.os.Build.SERIAL.equals("unknown")) {
            serial = android.os.Build.SERIAL;
        }
        else {
            // Fall-back to ADB USB Serial Number
            File usbIdFile = new File(USB_ID);
            StringBuffer usbSn = new StringBuffer("");

            if (usbIdFile.exists()) {
                FileInputStream fin = null;
                try {
                    int ch;
                    fin = new FileInputStream(usbIdFile);
                    while ((ch = fin.read()) != -1) {
                        usbSn.append((char) ch);
                    }
                }
                catch (IOException e) {
                    LOG.log(Level.SEVERE, "Exception", e);
                }
                finally {
                    try {
                        if (fin != null) {
                            fin.close();
                        }
                    }
                    catch (IOException e) {
                        LOG.log(Level.SEVERE, "Exception", e);
                    }
                }
                serial = usbSn.toString();
            }
            else {
                String hostName = "";
                BufferedReader bis = null;
                Process ifc = null;
                try {
                    // Fall-back to 'getprop net.hostname'
                    ifc = Runtime.getRuntime().exec("getprop net.hostname");
                    bis = new BufferedReader(new InputStreamReader(ifc.getInputStream(), "UTF8"));
                    hostName = bis.readLine();
                    ifc.waitFor();
                }
                catch (Exception e) {
                    LOG.log(Level.SEVERE, "Exception", e);
                }
                finally {
                    try {
                        if (bis != null) {
                            bis.close();
                        }
                    }
                    catch (IOException e) {
                        // don't care about close issues
                    }
                }
                serial = hostName;
            }
        }

        // store value in database
        tableInfo.insert(TableInfo.KEY_DEVICE_INFO_SERIAL, serial);
        return serial;
    }

    private DeviceId getDeviceId(TableInfo tableInfo)
    {
        // make sure deviceId is only generated once

        // fetch from database if available
        String deviceIdLower = tableInfo.getValue(TableInfo.KEY_DEVICE_ID_LOWER);
        String deviceIdUpper = tableInfo.getValue(TableInfo.KEY_DEVICE_ID_UPPER);
        if (deviceIdLower != null && deviceIdUpper != null) {
            // deviceId found
            DeviceId deviceId = new DeviceId();
            deviceId.upper = Long.parseLong(deviceIdUpper);
            deviceId.lower = Long.parseLong(deviceIdLower);
            return deviceId;
        }
        else {
            // no deviceId, generate random one.
            UUID uuid = UUID.randomUUID();
            DeviceId deviceId = new DeviceId();
            deviceId.lower = uuid.getLeastSignificantBits();
            deviceId.upper = uuid.getMostSignificantBits();

            // store in database
            tableInfo.insert(TableInfo.KEY_DEVICE_ID_LOWER, Long.toString(deviceId.lower));
            tableInfo.insert(TableInfo.KEY_DEVICE_ID_UPPER, Long.toString(deviceId.upper));
            return deviceId;
        }

    }

    /**
     * Get the device info.
     * 
     * @return the device info
     */
    public DeviceInfo getDeviceInfo()
    {
        return mDeviceInfo;
    }
}
