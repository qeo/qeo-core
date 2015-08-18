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

package org.qeo.sample.simplechat.android;

import android.app.Application;

import org.qeo.android.service.QeoDefaults;
import org.qeo.android.service.ui.QeoDefaultsUI;

/**
 * Application class of this project.
 */
public class ChatApplication extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        QeoDefaults.setRemoteRegistrationServiceAvailable(true);
        QeoDefaults.setRemoteRegistrationListenerAvailable(true);
        QeoDefaultsUI.setDisplayRegistrationNotification(this, true);

    }
}
