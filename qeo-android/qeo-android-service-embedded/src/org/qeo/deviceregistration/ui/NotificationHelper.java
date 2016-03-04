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

package org.qeo.deviceregistration.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.qeo.android.service.ServiceApplication;
import org.qeo.android.service.ui.MainActivity;
import org.qeo.android.service.ui.R;
import org.qeo.deviceregistration.helper.RegistrationStatusCode;
import org.qeo.deviceregistration.helper.UnRegisteredDeviceModel;
import org.qeo.deviceregistration.service.RegisterService;
import org.qeo.deviceregistration.service.RemoteDeviceRegistration;
import org.qeo.system.RegistrationRequest;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

/**
 * Class which handles display of notifications.
 */
public class NotificationHelper extends BroadcastReceiver
{
    private static final Logger LOG = Logger.getLogger("NotificationHelper");

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        if (action.equals(RegisterService.ACTION_REGISTRATION_DONE)) {
            localDeviceRegistered(context, intent);
        }
        else if (action.equals(RemoteDeviceRegistration.ACTION_UNREGISTERED_DEVICE_FOUND)) {
            remoteDeviceFoundNotification(context, intent);
        }
        else {
            LOG.warning("Unknown action: " + action);
        }
    }

    private void localDeviceRegistered(Context context, Intent intent)
    {
        LOG.fine("publishing notification for local device");
        String username = intent.getStringExtra(RegisterService.INTENT_EXTRA_USERNAME);
        String realmName = intent.getStringExtra(RegisterService.INTENT_EXTRA_REALMNAME);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_stat_qeo);
        builder.setContentTitle(context.getString(R.string.notification_device_registered));
        builder.setContentText(username + " in realm " + realmName);
        builder.setAutoCancel(true);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, MainActivity.class);
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
            stackBuilder.getPendingIntent(ServiceApplication.REQUEST_CODE_DEVICE_REGISTERED,
                PendingIntent.FLAG_CANCEL_CURRENT);

        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        notificationManager.notify(ServiceApplication.NOTIFICATION_DEVICE_REGISTERED, builder.build());
    }

    private void remoteDeviceFoundNotification(Context context, Intent intent)
    {
        LOG.fine("(un)publishing notification for remote device");
        List<RegistrationRequest> unregistered = new ArrayList<RegistrationRequest>();
        for (RegistrationRequest req : RemoteDeviceRegistration.getInstance().getUnregisteredDevices()) {
            RegistrationStatusCode status = UnRegisteredDeviceModel.getRegistrationStatus(req.registrationStatus);
            if (status == RegistrationStatusCode.UNREGISTERED) {
                // only add unregistered devices
                unregistered.add(req);
            }
        }

        NotificationManager notificationManager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (unregistered.isEmpty()) {
            // no more devices in unregistered state, remove notification
            notificationManager.cancel(ServiceApplication.NOTIFICATION_UNREGISTERED_DEVICE_FOUND);
            return;
        }

        // need to create the notification

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_stat_qeo);
        builder.setContentTitle(context.getString(R.string.unregistered_device_notification_title));
        builder.setContentText(context.getString(R.string.unregistered_device_notification_subtitle));
        builder.setAutoCancel(true);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, ManagementActivity.class);
        resultIntent.putExtra(ManagementActivity.INTENT_EXTRA_START_TAB, ManagementActivity.TAB_DEVICES);
        if (unregistered.size() == 1) {
            // only 1 device
            UnRegisteredDeviceModel device = new UnRegisteredDeviceModel(unregistered.get(0));
            resultIntent.putExtra(UnRegisteredDeviceListFragment.INTENT_EXTRA_DEVICE_TO_REGISTER, device);
        }

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent =
            stackBuilder.getPendingIntent(ServiceApplication.REQUEST_CODE_UNREGISTERED_DEVICE_FOUND,
                PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        // mId allows you to update the notification later on.
        notificationManager.notify(ServiceApplication.NOTIFICATION_UNREGISTERED_DEVICE_FOUND, builder.build());

    }
}
