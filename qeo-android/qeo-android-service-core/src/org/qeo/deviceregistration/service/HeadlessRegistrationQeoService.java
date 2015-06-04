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

package org.qeo.deviceregistration.service;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.DefaultStateChangeReaderListener;
import org.qeo.QeoFactory;
import org.qeo.StateChangeReader;
import org.qeo.StateWriter;
import org.qeo.android.service.DeviceInfoAndroid;
import org.qeo.android.service.ServiceApplication;
import org.qeo.deviceregistration.QeoManagementApp;
import org.qeo.deviceregistration.helper.ErrorCode;
import org.qeo.deviceregistration.helper.RegistrationStatusCode;
import org.qeo.exception.QeoException;
import org.qeo.java.QeoConnectionListener;
import org.qeo.java.QeoJava;
import org.qeo.jni.NativeSecurity;
import org.qeo.system.DeviceInfo;
import org.qeo.system.RegistrationCredentials;
import org.qeo.system.RegistrationRequest;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

/**
 * This service starts the Qeo service and prepares the Readers/Writers to read/write the data over Qeo.<br>
 * It will broadcast yourself as being available for remote registration.
 * <p>This should be started by using
 * {@link org.qeo.deviceregistration.service.RegisterService#startHeadlessRegistration(android.content.Context)}</p>
 */
public class HeadlessRegistrationQeoService
    extends Service
{
    private static final Logger LOG = Logger.getLogger("HeadlessRegistrationQeoService");
    /** Error message in case of failure. */
    public static final String INTENT_EXTRA_ERRORMSG = "errorMsg";
    /** The realmname in case of success. */
    public static final String INTENT_EXTRA_REALMNAME = "realmname";
    private StateChangeReader<RegistrationCredentials> mReader;
    private StateWriter<RegistrationRequest> mWriter;

    private LocalBroadcastManager mLbm;
    private boolean mDestroyed;
    private RegistrationRequest mReq;

    @Override
    public void onCreate()
    {
        LOG.fine("onCreate");
        synchronized (this) {
            mDestroyed = false;
        }
        mLbm = LocalBroadcastManager.getInstance(this);

        // start qeo service in the background. Otherwise this will block the UI thread.
        QeoManagementApp.getGlobalExecutor().submitTaskToPool(new Runnable() {

            @Override
            public void run()
            {
                startQeoService();
            }
        });
    }

    private void startQeoService()
    {
        // Get a QeoFactory object to create readers
        QeoJava.initQeo(QeoFactory.OPEN_ID, mQeoConnection);
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null; // no bind support
    }

    @Override
    public void onDestroy()
    {
        LOG.fine("onDestroy");
        synchronized (this) {
            // mark as destroyed. Do this with a lock taken on the class to avoid destroying while the init is still
            // running.

            if (mWriter != null) {
                mWriter.close();
                mWriter = null;
            }
            if (mReader != null) {
                mReader.close();
                mReader = null;
            }

            QeoJava.closeQeo(mQeoConnection);
            mDestroyed = true;
        }
    }

    private void sendFailureBroadcast(String msg)
    {
        Intent intent = new Intent(RegisterService.ACTION_HEADLESS_REGISTRATION_DONE);
        intent.putExtra(RegisterService.INTENT_EXTRA_SUCCESS, false);
        intent.putExtra(INTENT_EXTRA_ERRORMSG, msg);
        mLbm.sendBroadcast(intent);
    }

    private void sendSuccessBroadcast(String otc, String url, String realmname)
    {
        Intent intent = new Intent(RegisterService.ACTION_HEADLESS_REGISTRATION_DONE);
        intent.putExtra(RegisterService.INTENT_EXTRA_SUCCESS, true);
        intent.putExtra(RegisterService.INTENT_EXTRA_OTC, otc);
        intent.putExtra(RegisterService.INTENT_EXTRA_URL, url);
        intent.putExtra(INTENT_EXTRA_REALMNAME, realmname);
        mLbm.sendBroadcast(intent);
    }

    private final QeoConnectionListener mQeoConnection = new QeoConnectionListener() {
        // Callback received once Qeo service is initialized successfully.
        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            synchronized (HeadlessRegistrationQeoService.this) {
                // take lock to prevent destroy and create to interfere.

                LOG.fine("onQeoReady");
                if (mDestroyed) {
                    // don't do init if service is already destroyed.
                    LOG.fine("Service already destroyed...");
                    return;
                }
                try {
                    mWriter = qeo.createStateWriter(RegistrationRequest.class);
                    mReader = qeo.createStateChangeReader(RegistrationCredentials.class, new RegistrationListener());

                    mReq = new RegistrationRequest();

                    DeviceInfo deviceInfo = DeviceInfoAndroid.getInstance().getDeviceInfo();
                    mReq.deviceId = deviceInfo.deviceId;
                    mReq.version = QeoManagementApp.CURRENT_OTC_ENCRYPT_VERSION;
                    mReq.manufacturer = deviceInfo.manufacturer;
                    mReq.modelName = deviceInfo.modelName;
                    mReq.userFriendlyName = QeoManagementApp.getDeviceName();
                    if (mReq.userFriendlyName == null) {
                        mReq.userFriendlyName = deviceInfo.userFriendlyName;
                    }
                    mReq.userName = QeoManagementApp.getUserName();
                    if (mReq.userName == null) {
                        mReq.userName =
                            RegisterService.getUserName(HeadlessRegistrationQeoService.this.getBaseContext());
                    }
                    mReq.registrationStatus = (short) RegistrationStatusCode.UNREGISTERED.ordinal();
                    mReq.errorCode = (short) ErrorCode.NONE.ordinal();
                    // fetch the public key from native. This can block if the key is still being generated
                    LOG.fine("Fetching public key from native");
                    mReq.rsaPublicKey = NativeSecurity.getPublicKey();
                    if (mReq.rsaPublicKey == null || mReq.rsaPublicKey.isEmpty()) {
                        LOG.warning("Can't get public key for headless registration.");
                        return;
                    }
                    LOG.info("Device put in headless registration mode for Qeo");
                    mWriter.write(mReq);
                }
                catch (QeoException e) {
                    LOG.log(Level.SEVERE, "Error creating reader/writers", e);
                    sendFailureBroadcast(e.getMessage());
                    stopSelf();
                }
                LOG.fine("onQeoReady done");
            }
        }

        @Override
        public void onQeoClosed(QeoFactory qeo)
        {
            // won't happen on qeo-java
            LOG.severe("got onQeoClosed, This should never happen");
        }

        @Override
        public void onQeoError(QeoException ex)
        {
            LOG.log(Level.SEVERE, "Error during Qeo init", ex);
            Toast.makeText(ServiceApplication.getApp(), "QeoService not Initialized!", Toast.LENGTH_LONG).show();
            sendFailureBroadcast(ex.getMessage());
            stopSelf();
        }
    };

    private class RegistrationListener
        extends DefaultStateChangeReaderListener<RegistrationCredentials>
    {

        @Override
        public void onData(RegistrationCredentials t)
        {
            LOG.fine("got incoming registration request from realm: " + t.realmName);
            if (!mReq.deviceId.equals(t.deviceId)) {
                LOG.fine("registration request for another device");
                return;
            }
            if (!mReq.rsaPublicKey.equals(t.requestRSAPublicKey)) {
                LOG.warning("public key mismatch in remote registration, ignoring request");
                return;
            }
            LOG.fine("Got remote registration approval");
            String otc = NativeSecurity.decryptOtc(t.encryptedOtc);
            LOG.fine("Decrypted otc: " + otc);
            if (otc == null || otc.isEmpty()) {
                LOG.warning("Error decrypting OTC");
                return;
            }
            sendSuccessBroadcast(otc, t.url, t.realmName);
        }

    }

}
