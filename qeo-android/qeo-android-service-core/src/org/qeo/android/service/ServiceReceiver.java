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

package org.qeo.android.service;

import java.util.logging.Logger;

import org.qeo.deviceregistration.service.RemoteDeviceRegistrationService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Broadcast receiver to listen to the BOOT_COMPLETED action at startup.
 */
public class ServiceReceiver
    extends BroadcastReceiver
{
    private static final Logger LOG = Logger.getLogger(QeoService.TAG);

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            LOG.fine("Start RemoteDeviceRegistrationService");
            Intent serviceIntent = new Intent(context, RemoteDeviceRegistrationService.class);
            context.startService(serviceIntent);
        }

    }

}
