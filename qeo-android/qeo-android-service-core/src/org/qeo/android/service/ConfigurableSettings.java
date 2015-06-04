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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

/**
 * Class to load settings that can be overridden for debugging.
 */
public class ConfigurableSettings
{
    private static final String CONFIG_APP = "org.qeo.android.debugconfig";
    /**
     * Name of generic config file (internal (in service)).
     */
    public static final String FILE_QEO_PREFS = "qeoPrefs";

    /**
     * Name of generic config file in debugconfig app.
     */
    public static final String FILE_QEO_PREFS_DEBUGCONFIG = "qeoPrefsDebugconfig";
    /**
     * Configuration file for forwarder specific options.
     */
    public static final String FILE_QEO_PREFS_FORWARDER = "qeoPrefsForwarder";

    private final SharedPreferences mSharedPrefs;

    /**
     * Create a new instance.
     * 
     * @param ctx The Android context.
     * @param configFile The configfile to open.
     * @param extern to indicate if the config file is located in the debugconfig app.
     */
    public ConfigurableSettings(Context ctx, String configFile, boolean extern)
    {
        if (extern) {
            Context sharedPrefsCtx = null;
            try {
                sharedPrefsCtx = ctx.createPackageContext(CONFIG_APP, 0);
            }
            catch (final NameNotFoundException e) {
                // ignore. This means the mQeo config app is not present on the system
            }
            if (sharedPrefsCtx != null) {
                mSharedPrefs = sharedPrefsCtx.getSharedPreferences(configFile, getSharedPrefsMultiProcess());
            }
            else {
                mSharedPrefs = null;
            }
        }
        else {
            mSharedPrefs = ctx.getSharedPreferences(configFile, Context.MODE_PRIVATE);
        }
    }

    @SuppressLint("InlinedApi")
    private static int getSharedPrefsMultiProcess()
    {
        if (Build.VERSION.SDK_INT >= 11) {
            // this flag is added in api 11
            return Context.MODE_MULTI_PROCESS;
        }
        else {
            // however, before api 11 this is default set to multi_process
            return Context.MODE_PRIVATE;
        }
    }

    /**
     * Get String value.
     * 
     * @param key preference name.
     * @param defValue Default value.
     * @return config value if config app is present and preference is set. Default value otherwise.
     */
    public String getString(String key, String defValue)
    {
        if (mSharedPrefs == null) {
            return defValue;
        }
        else {
            return mSharedPrefs.getString(key, defValue);
        }
    }

    /**
     * Get int value.
     * 
     * @param key preference name.
     * @param defValue Default value.
     * @return config value if config app is present and preference is set. Default value otherwise.
     */
    public int getInt(String key, int defValue)
    {
        if (mSharedPrefs == null) {
            return defValue;
        }
        else {
            return mSharedPrefs.getInt(key, defValue);
        }
    }

    /**
     * Get boolean value.
     * 
     * @param key preference name.
     * @param defValue Default value.
     * @return config value if config app is present and preference is set. Default value otherwise.
     */
    public boolean getBoolean(String key, boolean defValue)
    {
        if (mSharedPrefs == null) {
            return defValue;
        }
        else {
            return mSharedPrefs.getBoolean(key, defValue);
        }
    }
}
