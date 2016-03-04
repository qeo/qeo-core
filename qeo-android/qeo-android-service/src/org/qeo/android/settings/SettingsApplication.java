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

package org.qeo.android.settings;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.android.internal.QeoLogger;
import org.qeo.android.service.QeoDefaults;
import org.qeo.android.service.ServiceApplication;
import org.qeo.android.service.ui.QeoDefaultsUI;
import org.qeo.deviceregistration.ui.WebViewActivity;
import org.qeo.jni.NativeQeo;

import android.app.Application;

/**
 * Application class for Qeo Settings.
 */
public class SettingsApplication extends Application
{
    private static final Logger LOG = Logger.getLogger("SettingsApplication");

    @Override
    public void onCreate()
    {
        super.onCreate();
        ServiceApplication.initServiceApp(this);
        initLogLevel();
        QeoDefaultsUI.init(this);


        // using standalone service, set some defaults.
        QeoDefaults.setRemoteRegistrationServiceAvailable(true);
        QeoDefaults.setRemoteRegistrationListenerAvailable(true);
        QeoDefaults.setManifestEnabled(true);
        QeoDefaults.setProxySecurityEnabled(true);
        WebViewActivity.setAbortOnStop(true);
        QeoDefaultsUI.setDisplayRegistrationNotification(this, true);

    }

    private void initLogLevel()
    {
        QeoLogger.init();
        Logger.getLogger("").setLevel(Level.INFO); // default loglevel = INFO

        // Check for shared preferences of the org.qeo.android.debugconfig package.
        final String logLevel = ServiceApplication.getProperties().getExternalSettings().getString("logLevel", null);
        if (logLevel != null) {
            Level level = Level.parse(logLevel);
            LOG.info("Setting the level: " + logLevel);
            Logger.getLogger("").setLevel(level);
            NativeQeo.setLogLevel(level);
        }
    }


}
