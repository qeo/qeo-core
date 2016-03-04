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

package org.qeo.deviceregistration.service;

import java.util.logging.Logger;

import org.qeo.QeoFactory;
import org.qeo.android.service.LocalServiceConnection;
import org.qeo.android.service.QeoDefaults;
import org.qeo.android.service.ServiceApplication;
import org.qeo.android.service.ServiceReceiver;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.exception.QeoException;
import org.qeo.java.QeoConnectionListener;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

/**
 * Service to start remote device registration.<br>
 * This service runs at boot-time if remote device notification is turned on.
 */
public class RemoteDeviceRegistrationService
    extends Service
{
    private static final Logger LOG = Logger.getLogger("RemoteDeviceRegistrationService");
    private LocalServiceConnection mLocalServiceConnection;

    @Override
    public void onCreate()
    {
        if (!QeoDefaults.isRemoteRegistrationServiceAvailable()) {
            // service is disabled.
            stopSelf();
            return;
        }
        LOG.info("Starting Qeo Service to listen for unregistered devices");
        mLocalServiceConnection = new LocalServiceConnection(this, new QeoConnectionListener() {

            @Override
            public void onQeoReady(QeoFactory qeo)
            {
                LOG.fine("onQeoReady");
            }

            @Override
            public void onQeoError(QeoException ex)
            {
                LOG.fine("onQeoError");
            }

            @Override
            public void onQeoClosed(QeoFactory qeo)
            {
                LOG.fine("onQeoClosed");
                stopSelf();
            }

        });
        super.onCreate();
    }

    @Override
    public void onDestroy()
    {
        LOG.fine("onDestroy");
        mLocalServiceConnection.close();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        LOG.fine("onStartCommand");
        return START_STICKY;
    }

    /**
     * Check if the service needs to be started/stopped and do so.
     * 
     * @param ctx Android context.
     */
    public static synchronized void checkStartStop(Context ctx)
    {
        if (QeoDefaults.isRemoteRegistrationServiceAvailable() && DeviceRegPref.getSelectedRealmId() != 0) {
            // only start if a realm is selected
            Intent serviceIntent = new Intent(ctx, RemoteDeviceRegistrationService.class);
            ComponentName receiver = new ComponentName(ctx, ServiceReceiver.class);
            PackageManager pm = ctx.getPackageManager();
            boolean start =
                DeviceRegPref.getDeviceRegisterNotifactionEnabled() && DeviceRegPref.getDeviceRegisterServiceEnabled();
            if (start) {
                LOG.fine("Remote device registration enabled");
                ctx.startService(serviceIntent);
                // enable broadcastreceiver
                pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            }
            else {
                LOG.fine("Remote device registration disabled");
                ctx.stopService(serviceIntent);
                // disable broadcastreceiver
                pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTaskRemoved(Intent rootIntent)
    {
        // restart the service if somebody kills the application.
        // note that this will only work on 4.0 and above, but versions below restart automatically in that case
        LOG.fine("onTaskRemoved");
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        PendingIntent restartServicePendingIntent =
            PendingIntent.getService(getApplicationContext(),
                ServiceApplication.REQUEST_CODE_RESTART_NOTIFICATION_SERVICE, restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent);

        super.onTaskRemoved(rootIntent);
    }

}
