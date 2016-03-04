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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.qeo.QeoFactory;
import org.qeo.android.service.LocalServiceConnection;
import org.qeo.deviceregistration.QeoManagementApp;
import org.qeo.deviceregistration.helper.RegistrationCredentialsWriterTask;
import org.qeo.deviceregistration.helper.UnRegisteredDeviceModel;
import org.qeo.deviceregistration.rest.RestHelper;
import org.qeo.exception.QeoException;
import org.qeo.java.QeoConnectionListener;
import org.qeo.system.RegistrationRequest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

/**
 * Special HOVIS hack to automatically register a device with a certain name via Qeo RegistrationRequest topic. This
 * class is responsible to automatically register specific devices (such as the DWI box).
 */
public class AutoRegistration
{
    /**
     * Tag used for logging.
     */
    public static final String TAG = "AutoRegistrationService";
    private static final Logger LOG = Logger.getLogger(TAG);
    private String mRealm = null;
    private String mUserName = null;
    private String mDeviceName = null;
    private Context mContext = null;
    private final RemoteDeviceRegistration mDeviceRegisterQeoService;
    private final LocalServiceConnection mLocalServiceConnection;

    /**
     * Construct the auto registration object.
     * 
     * @param context The application context
     * @param realm The realm name
     * @param userName The user name
     * @param deviceName The device name to automatically register
     */
    public AutoRegistration(Context context, String realm, String userName, String deviceName)
    {
        mContext = context;
        mRealm = realm;
        mUserName = userName;
        mDeviceName = deviceName;
        QeoManagementApp.init();
        /* Register broadcast receiver and startup deviceRegistration service. */
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mReceiver,
            new IntentFilter(RemoteDeviceRegistration.ACTION_UNREGISTERED_DEVICE_FOUND));

        mDeviceRegisterQeoService = RemoteDeviceRegistration.getInstance();
        mLocalServiceConnection = new LocalServiceConnection(context, mQeoConnectionListener);
        LOG.info("Started device registration service");
    }

    /**
     * Close all started services.
     */
    public void close()
    {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
        mLocalServiceConnection.close();
    }

    private RegistrationRequest checkForDeviceName(String deviceName)
    {
        if (deviceName != null) {
            for (RegistrationRequest device : mDeviceRegisterQeoService.getUnregisteredDevices()) {
                if (device.userFriendlyName.equals(deviceName)) {
                    return device;
                }
            }
        }
        return null;
    }

    /**
     * Broadcast receiver that will be triggered if a new unregistered device is found.
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent)
        {
            LOG.fine("onReceive");
            QeoManagementApp.getGlobalExecutor().submitTaskToPool(new Runnable() {

                @Override
                public void run()
                {
                    if (intent.getBooleanExtra(RemoteDeviceRegistration.INTENT_EXTRA_RESULT, false)) {
                        LocalBroadcastManager.getInstance(mContext).registerReceiver(mRegCredReceiver,
                            new IntentFilter(RegistrationCredentialsWriterTask.ACTION_REGISTRATION_QEO_DONE));
                        RegistrationRequest device = checkForDeviceName(mDeviceName);
                        if (device != null) {
                            try {
                                RestHelper restHelper = new RestHelper(context);
                                long realmId = RestHelper.addRealm(mRealm);
                                long userId = restHelper.addUserWithBroadcast(realmId, mUserName);
                                if (realmId != -1 && userId != -1) {
                                    RegistrationCredentialsWriterTask task =
                                        new RegistrationCredentialsWriterTask(mContext, new UnRegisteredDeviceModel(
                                            device), realmId, userId);
                                    QeoManagementApp.getGlobalExecutor().submitTaskToPool(task);
                                }
                            }
                            catch (IOException e) {
                                LOG.log(Level.WARNING, "Error registering device", e);
                            }
                            catch (JSONException e) {
                                LOG.log(Level.WARNING, "Error registering device", e);
                            }
                        }
                    }
                    else {
                        String errorMsg = intent.getStringExtra(RemoteDeviceRegistration.INTENT_EXTRA_ERRORMSG);
                        LOG.warning(errorMsg);
                    }
                }
            });

        }
    };

    private final BroadcastReceiver mRegCredReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this);
            boolean result =
                intent.getBooleanExtra(RegistrationCredentialsWriterTask.INTENT_EXTRA_REGISTRATION_RESULT, false);
            if (!result) {
                LOG.warning("Failure creating otc/registering device");
                Toast.makeText(mContext, "Failed to Send data over Qeo", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final QeoConnectionListener mQeoConnectionListener = new QeoConnectionListener() {

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
        }
    };

}
