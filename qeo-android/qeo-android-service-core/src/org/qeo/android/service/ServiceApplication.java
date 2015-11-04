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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.android.internal.NoProguard;
import org.qeo.deviceregistration.QeoManagementApp;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

/**
 * Class with contains some global initialisation.
 */
public final class ServiceApplication
{
    /** Notification to be shown when device is auto registered. */
    public static final int NOTIFICATION_DEVICE_REGISTERED = 1;
    /** Notification if remote registration of a device failed. */
    public static final int NOTIFICATION_REMOTE_REGISTRATION_FAILED = 2;
    /** Notification to indicate unregistered device. */
    public static final int NOTIFICATION_UNREGISTERED_DEVICE_FOUND = 3;

    /** Notification to be shown when device is auto registered. */
    public static final int REQUEST_CODE_DEVICE_REGISTERED = 1;
    /** Notification if remote registration of a device failed. */
    public static final int REQUEST_CODE_REMOTE_REGISTRATION_FAILED = 2;
    /** Notification to indicate unregistered device. */
    public static final int REQUEST_CODE_UNREGISTERED_DEVICE_FOUND = 3;
    /** Requestcode for restarting notification service. */
    public static final int REQUEST_CODE_RESTART_NOTIFICATION_SERVICE = 4;

    /** meta-data key to get the google+ clientid. */
    public static final String META_DATA_GOOGLE_CLIENT_ID = "org.qeo.rest.google.client.id";
    /** meta-data key to get the Qeo OAuth client id. */
    public static final String META_DATA_QEO_CLIENT_ID = "org.qeo.rest.qeo.client.id";
    /** meta-data key to get the Qeo OAuth client secret. */
    public static final String META_DATA_QEO_CLIENT_SECRET = "org.qeo.rest.qeo.client.secret";

    private static final Logger LOG = Logger.getLogger("ServiceApplication");
    private static Application sApp;
    private static AppProperties sProperties;
    private static boolean sIsInit = false;
    private static Boolean sIsEmbedded = null;

    private ServiceApplication()
    {
    }

    /**
     * Initialize ServiceApplication class. Only needs to be done if using Qeo as embedded service.
     *
     * @param ctx The context, this must be the application context.
     */
    @NoProguard
    public static synchronized void initServiceApp(Context ctx)
    {
        if (!sIsInit) {
            if (!(ctx instanceof Application)) {
                throw new IllegalArgumentException("application context needed");
            }
            initInternal((Application) ctx);
            sIsInit = true;
        }
    }

    private static void initInternal(Application app)
    {
        // store reference to self.
        sApp = app;
        PRNGFixes.apply();
        sProperties = new AppProperties(app);
        QeoSuspendHelper.enableBgnsKeepalive();
        QeoManagementApp.init();
    }

    /**
     * Get the application context.
     *
     * @return The application context.
     */
    public static Application getApp()
    {
        return sApp;
    }

    /**
     * Get the properties object for this application.
     *
     * @return extra properties for this application.
     */
    public static AppProperties getProperties()
    {
        return sProperties;
    }

    /**
     * Get a meta-data field from the AndroidManifest.xml file.
     *
     * @param key The key of the meta-data entry. This key must exist or an IllegalStateException will be thrown.
     * @return The meta-data value
     */
    public static String getMetaData(String key)
    {
        String result;
        try {
            LOG.fine("Get meta-data entry from AndroidManifest: " + key);
            ApplicationInfo ai =
                sApp.getPackageManager().getApplicationInfo(sApp.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            result = bundle.getString(key);
        }
        catch (Exception e) {
            result = null;
        }
        if (result == null) {
            throw new IllegalStateException("No <meta-data/> entry defined in the manifest with the value " + key);
        }
        return result;
    }

    /**
     * Check if the service is running as an embedded service or standalone.
     * @return true for embedded, false for standalone.
     */
    public static synchronized boolean isEmbeddedService()
    {
        if (sIsEmbedded == null) {
            try {
                ApplicationInfo ai = sApp.getPackageManager().getApplicationInfo(sApp.getPackageName(),
                    PackageManager.GET_META_DATA);
                Bundle bundle = ai.metaData;
                sIsEmbedded = !bundle.getBoolean("org.qeo.service.standalone", false);
            }
            catch (PackageManager.NameNotFoundException e) {
                LOG.log(Level.WARNING, "Name not found", e);
                sIsEmbedded = false;
            }
            LOG.fine("Running as embedded service? " + sIsEmbedded);
        }
        return sIsEmbedded;
    }

    /**
     * Class to set extra global properties.
     */
    public static final class AppProperties
    {
        private final ConfigurableSettings mSettingsExternal;

        /**
         * Constructor.
         *
         * @param ctx the application context.
         */
        AppProperties(Context ctx)
        {
            mSettingsExternal = new ConfigurableSettings(ctx, ConfigurableSettings.FILE_QEO_PREFS_DEBUGCONFIG, true);
        }


        /**
         * Get the extra settings object. This object can be used to query settings set by the debugconfig application.
         *
         * @return The extrasettings.
         */
        public ConfigurableSettings getExternalSettings()
        {
            return mSettingsExternal;
        }

    }

}
