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

package org.qeo.deviceregistration;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.qeo.android.service.QeoDefaults;
import org.qeo.android.service.ServiceApplication;
import org.qeo.deviceregistration.helper.CustomTrustManager;
import org.qeo.deviceregistration.helper.GlobalExecutor;
import org.qeo.deviceregistration.helper.UnRegisteredDeviceModel;
import org.qeo.deviceregistration.service.RemoteDeviceRegistrationService;

/**
 * Class that contains global variables for the management app.
 */
public final class QeoManagementApp
{
    private static final Logger LOG = Logger.getLogger("QeoManagementApp");
    /** Constant to debug if invalid Auth token problem occurs. */
    public static final int TIME_DIV = 1;

    /** LoaderId to get list of users from realm. */
    public static final int LOADER_GET_USERS_ID = 0;
    /** LoaderId2 to get list of users from realm. */
    public static final int LOADER_GET_USERS_ID2 = 10;
    /** LoaderId to add user to realm. */
    public static final int LOADER_ADD_USER_ID = 1;
    /** LoaderId to add user to realm. */
    public static final int LOADER_ADD_USER_ID2 = 11;
    /** LoaderId to get list of realms. */
    public static final int LOADER_GET_REALMS_ID = 2;
    /** LoaderId to add a realm. */
    public static final int LOADER_ADD_REALM_ID = 3;
    /** LoaderId to get list of devices in the realm. */
    public static final int LOADER_GET_DEVICES_ID = 4;

    /** Key for identifying selected realm in a Bundle. */
    public static final String BUNDLE_SELECTED_REALM = "selectedRealm";

    /** Current version to encrypt OTC. */
    public static final int CURRENT_OTC_ENCRYPT_VERSION = 1;

    private static GlobalExecutor sGlobalExecutor;
    private static List<UnRegisteredDeviceModel> sDeviceList;
    private static UnRegisteredDeviceModel sSelectedDevice;
    private static boolean sIsInit = false;

    /** A global realm name to be used (written by the app developer). */
    private static String sRealmName = null;
    /** A global user name to be used (written by the app developer). */
    private static String sUserName = null;
    /** A global device name to be used (written by the app developer). */
    private static String sDeviceName = null;

    private QeoManagementApp()
    {
        // private constructor
    }

    /**
     * Initialize global things for the QeoManagement. This can be called multiple times.
     */
    public static synchronized void init()
    {
        if (sIsInit) {
            return;
        }
        LOG.fine("Init QeoManagementApp");

        // init preferences
        DeviceRegPref.init();

        if (QeoDefaults.isAllowSelfSignedCertificate()) {
            // qeodev uses self-signed certificate, use custom trust manager
            // notice that this will only work on a debug build
            CustomTrustManager.install();
        }

        /**
         * http://code.google.com/p/google-http-java-client/issues/detail?id=116
         */
        System.setProperty("http.keepAlive", "false");

        sDeviceList = new ArrayList<UnRegisteredDeviceModel>();
        sGlobalExecutor = new GlobalExecutor();
        sIsInit = true;
        RemoteDeviceRegistrationService.checkStartStop(ServiceApplication.getApp());
    }

    /**
     * Get the list of unregistered devices.
     * 
     * @return Returns list of UnRegisteredDeviceModel.
     */
    public static List<UnRegisteredDeviceModel> getUnRegisteredDeviceList()
    {
        return sDeviceList;
    }

    /**
     * Returns an executor that can be used in the whole process.
     * 
     * @return Returns the global executor.
     */
    public static GlobalExecutor getGlobalExecutor()
    {
        init();
        return sGlobalExecutor;
    }

    /**
     * Returns the last selected unregistered device for registering.
     * 
     * @return last selected unregistered device.
     */
    public static UnRegisteredDeviceModel getLastSelectedDevice()
    {
        return sSelectedDevice;
    }

    /**
     * Sets the current selected device.
     * 
     * @param d -UnRegisteredDeviceModel object
     */
    public static void setLastSelectedDevice(UnRegisteredDeviceModel d)
    {
        sSelectedDevice = d;
    }

    /**
     * Realm name set by the app developer.
     * 
     * @param realmName the sRealmName to set
     */
    public static void setRealmName(String realmName)
    {
        sRealmName = realmName;
    }

    /**
     * Retrieve the realm name as set by the app developer.
     * 
     * @return the sRealmName
     */
    public static String getRealmName()
    {
        return sRealmName;
    }

    /**
     * User name set by the app developer.
     * 
     * @param userName the sUserName to set
     */
    public static void setUserName(String userName)
    {
        sUserName = userName;
    }

    /**
     * Retrieve the user name as set by the app developer.
     * 
     * @return the sUserName
     */
    public static String getUserName()
    {
        return sUserName;
    }

    /**
     * Device name set by the app developer.
     * 
     * @param deviceName the sDeviceName to set
     */
    public static void setDeviceName(String deviceName)
    {
        sDeviceName = deviceName;
    }

    /**
     * Retrieve the device name as set by the app developer.
     * 
     * @return the sDeviceName
     */
    public static String getDeviceName()
    {
        return sDeviceName;
    }

    /**
     * Check if the current user is a realm admin.
     * 
     * @return true if the user is realm admin. False if not an admin or not registered yet.
     */
    public static boolean isRealmAdmin()
    {
        String token = DeviceRegPref.getRefreshToken();
        return (token != null && !token.isEmpty());
    }
}
