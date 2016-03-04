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

package org.qeo.android.service.ui;

import org.qeo.android.internal.NoProguard;
import org.qeo.deviceregistration.service.RegisterService;
import org.qeo.deviceregistration.service.RemoteDeviceRegistration;
import org.qeo.deviceregistration.ui.NotificationHelper;
import org.qeo.deviceregistration.ui.WebViewActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Class which contains some initialisation if using the default Qeo service UI.
 */
public final class QeoDefaultsUI
{
    private static NotificationHelper sNotificationHelper = null;
    private static boolean sInit = false;

    private QeoDefaultsUI()
    {
    }

    /**
     * Initialize the default UI.
     *
     * @param ctx The android context.
     */
    @NoProguard
    public static void init(Context ctx)
    {
        if (!sInit) {
            sInit = true;
            LocalBroadcastManager.getInstance(ctx).registerReceiver(new MyBroadcastReceiver(),
                new IntentFilter(RegisterService.ACTION_LOGIN_REQUIRED));
        }
    }


    /**
     * Set flag to toggle display of registration notification.
     *
     * @param ctx                             the context
     * @param displayRegistrationNotification the flag
     */
    public static synchronized void setDisplayRegistrationNotification(Context ctx,
                                                                       boolean displayRegistrationNotification)
    {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(ctx);
        if (sNotificationHelper != null) {
            //unregister old
            lbm.unregisterReceiver(sNotificationHelper);
            sNotificationHelper = null;
        }
        if (displayRegistrationNotification) {
            sNotificationHelper = new NotificationHelper();
            IntentFilter filter = new IntentFilter();
            filter.addAction(RegisterService.ACTION_REGISTRATION_DONE);
            filter.addAction(RemoteDeviceRegistration.ACTION_UNREGISTERED_DEVICE_FOUND);
            lbm.registerReceiver(sNotificationHelper, filter);
        }
    }

    private static class MyBroadcastReceiver extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            Intent i = new Intent(context, WebViewActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            i.putExtra(WebViewActivity.INTENT_EXTRA_STARTED_FROM_EXTERNAL_APP, true);
            context.startActivity(i);
        }
    }
}
