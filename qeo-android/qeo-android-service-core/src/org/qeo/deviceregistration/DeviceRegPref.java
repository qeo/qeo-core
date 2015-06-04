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

package org.qeo.deviceregistration;

import java.util.logging.Logger;

import org.qeo.android.service.QeoDefaults;
import org.qeo.android.service.ServiceApplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Preferences for deviceRegistration.
 */
public final class DeviceRegPref
{
    private static final String FILENAME = "DeviceRegistrationPreferences";

    private static final String PREF_URL_ROOT_SERVER = "urlRootServer";
    private static final String PREF_URL_REST_SERVER = "urlRestServer";
    private static final String PREF_URL_OAUTH_SERVER = "urlOAutherver";
    private static final String PREF_ROOT_URL_DIALOG_STATUS = "rootUrlDialogStatus";
    private static final String PREF_SELECTED_REALM_ID = "selectedRealmId";
    private static final String PREF_SELECTED_REALM = "selectedRealm";
    private static final String PREF_SHOW_REALM_PROGRESS = "showRealmProgress";
    private static final String PREF_VERSION_DIALOG_STATUS = "versionDialogStatus";
    private static final String PREF_URL_AUTHORIZATION = "urlOAuthorization";
    private static final String PREF_REFRESH_TOKEN = "refreshToken";
    private static final String PREF_ENABLE_DEVICE_REGISTER_NOTIFICATION = "enableDeviceRegisterNotification";
    private static final String PREF_ENABLE_DEVICE_REGISTER_SERVICE = "enableDeviceRegisterService";

    private static final Logger LOG = Logger.getLogger("DeviceRegPref");
    private static SharedPreferences sPrefs;

    private DeviceRegPref()
    {
        // private constructor
    }

    /**
     * Initialize the preferences for device registration.
     */
    static void init()
    {
        Context ctx = ServiceApplication.getApp();
        sPrefs = ctx.getSharedPreferences(FILENAME, Context.MODE_PRIVATE);
    }

    /**
     * Sets all resource Urls to persist.
     * 
     * @param qeoRootUrl -Root URl
     * @param restUrl -REST URl
     * @param oauthUrl - OAuth Server Url
     * @param authorizationUrl - OAuthorization Url
     */
    public static void setResourceURL(String qeoRootUrl, String restUrl, String oauthUrl, String authorizationUrl)
    {
        SharedPreferences.Editor editor = sPrefs.edit();
        editor.putString(PREF_URL_ROOT_SERVER, qeoRootUrl);
        editor.putString(PREF_URL_REST_SERVER, restUrl + "/");
        editor.putString(PREF_URL_OAUTH_SERVER, oauthUrl);
        editor.putString(PREF_URL_AUTHORIZATION, authorizationUrl);
        editor.apply();
    }

    /**
     * REST server URL.
     * 
     * @return Returns REST server URL.
     */
    public static String getServerURL()
    {
        return sPrefs.getString(PREF_URL_REST_SERVER, "");
    }

    /**
     * Authorization server URL.
     * 
     * @return Returns Authorization server URL.
     */
    public static String getAuthorizationUrl()
    {
        return sPrefs.getString(PREF_URL_AUTHORIZATION, "");
    }

    /**
     * OAuth server URL.
     * 
     * @return Returns OAuth server URL.
     */
    public static String getOauthServerURL()
    {
        return sPrefs.getString(PREF_URL_OAUTH_SERVER, "");
    }

    /**
     * ScepServer URl to connect.
     * 
     * @return Returns ScepServer URl to connect.
     */
    public static String getScepServerURL()
    {
        return sPrefs.getString(PREF_URL_ROOT_SERVER, QeoDefaults.getPublicUrl());
    }

    /**
     * Get the selected realm.
     * 
     * @return The name of the realm.
     */
    public static String getSelectedRealm()
    {
        return sPrefs.getString(PREF_SELECTED_REALM, "");
    }

    /**
     * Returns selected realmId.
     * 
     * @return selected realmId. 0 if no realm is selected.
     */
    public static Long getSelectedRealmId()
    {
        long reamlID = sPrefs.getLong(PREF_SELECTED_REALM_ID, 0);
        LOG.fine("RealmID: " + reamlID);
        return reamlID;
    }

    /**
     * Get the root url dialog status.
     * 
     * @return the status.
     */
    public static boolean getRootUrlDialogStatus()
    {
        return sPrefs.getBoolean(PREF_ROOT_URL_DIALOG_STATUS, false);
    }

    /**
     * Get the showRealmProgress preference.
     * 
     * @return the preference.
     */
    public static boolean getShowRealmProgress()
    {
        return sPrefs.getBoolean(PREF_SHOW_REALM_PROGRESS, false);
    }

    /**
     * Get the versionDialog status.
     * 
     * @return The status.
     */
    public static boolean getVersionDialogStatus()
    {
        return sPrefs.getBoolean(PREF_VERSION_DIALOG_STATUS, false);
    }

    /**
     * RefreshToken received from OAUTH Call.
     * 
     * @return RefreshToken received from OAUTH Call.
     */
    public static String getRefreshToken()
    {
        return sPrefs.getString(PREF_REFRESH_TOKEN, null);
    }

    /**
     * Get value if deviceregister service is enabled.
     * 
     * @return The status. Default true.
     */
    public static boolean getDeviceRegisterServiceEnabled()
    {
        return sPrefs.getBoolean(PREF_ENABLE_DEVICE_REGISTER_SERVICE, true);
    }

    /**
     * Check if device registration icon should be displayed.
     * 
     * @return The status. Default true.
     */
    public static boolean getDeviceRegisterNotifactionEnabled()
    {
        return sPrefs.getBoolean(PREF_ENABLE_DEVICE_REGISTER_NOTIFICATION, true);
    }

    /**
     * Start an editor to set preferences. Make sure to call apply() once done.
     * 
     * @return The edit object.
     */
    public static Edit edit()
    {
        return new Edit();
    }

    /**
     * Editor class to edit preferences for DeviceRegistration.
     */
    public static class Edit
    {
        private final Editor mEditor;

        /**
         * Create a new instance.
         */
        Edit()
        {
            mEditor = sPrefs.edit();
        }

        /**
         * Store the values on disk. Make sure to always call this as last.
         */
        public void apply()
        {
            mEditor.apply();
        }

        /**
         * Set the selected realm.
         * 
         * @param realmId The id of the realm
         * @param realmName The name of the realm
         * @return Instance of the current Edit object.
         */
        public Edit setSelectedRealmId(long realmId, String realmName)
        {
            mEditor.putLong(PREF_SELECTED_REALM_ID, realmId);
            mEditor.putString(PREF_SELECTED_REALM, realmName);
            return this;
        }

        /**
         * Set the root url dialog status.
         * 
         * @param status the status
         * @return Instance of the current Edit object.
         */
        public Edit setRootUrlDialogStatus(boolean status)
        {
            mEditor.putBoolean(PREF_ROOT_URL_DIALOG_STATUS, status);
            return this;
        }

        /**
         * Set the showRealmProgress preference.
         * 
         * @param val the new value.
         * @return Instance of the current Edit object.
         */
        public Edit setShowRealmProgress(boolean val)
        {
            mEditor.putBoolean(PREF_SHOW_REALM_PROGRESS, val);
            return this;
        }

        /**
         * Set the versionDialog status.
         * 
         * @param val The new status.
         * @return Instance of the current Edit object.
         */
        public Edit setVersionDialogStatus(boolean val)
        {
            mEditor.putBoolean(PREF_VERSION_DIALOG_STATUS, val);
            return this;
        }

        /**
         * Enable/disable device register service.
         * 
         * @param val The new status.
         * @return Instance of the current Edit object.
         */
        public Edit setDeviceRegisterServiceEnabled(boolean val)
        {
            mEditor.putBoolean(PREF_ENABLE_DEVICE_REGISTER_SERVICE, val);
            return this;
        }

        /**
         * Enable/disable device registration notification.
         * 
         * @param val The new status.
         * @return Instance of the current Edit object.
         */
        public Edit setDeviceRegisterNotificationEnabled(boolean val)
        {
            mEditor.putBoolean(PREF_ENABLE_DEVICE_REGISTER_NOTIFICATION, val);
            return this;
        }

        /**
         * Set the refresh token.
         * 
         * @param refreshToken The new status.
         * @return Instance of the current Edit object.
         */
        public Edit setRefreshToken(String refreshToken)
        {
            mEditor.putString(PREF_REFRESH_TOKEN, refreshToken);
            return this;
        }
    }
}
