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

package org.qeo.deviceregistration.helper;

import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.QeoFactory;
import org.qeo.android.service.LocalServiceConnection;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.service.RemoteDeviceRegistration;
import org.qeo.exception.QeoException;
import org.qeo.java.QeoConnectionListener;
import org.qeo.system.DeviceId;
import org.qeo.system.RegistrationCredentials;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Runnable to encrypt OTC and write it over Qeo.
 */
public class RegistrationCredentialsWriterTask
    implements Runnable
{

    /** Action to be broadcasted when registration credentials are written over Qeo. */
    public static final String ACTION_REGISTRATION_QEO_DONE = "actionRegistrationQeoDone";
    /** Extra field (boolean) to be added to ACTION_REGISTRATION_QEO_DONE that defines the result. */
    public static final String INTENT_EXTRA_REGISTRATION_RESULT = "intentExtraRegistrationResult";
    private static final Logger LOG = Logger.getLogger("RegistrationCredentialsWriter");
    private final String mDeviceName;
    private final UnRegisteredDeviceModel mDeviceObject;
    private final Context mCtx;
    private final Intent mIntent;
    private final LocalBroadcastManager mBroadcastManager;
    private final Semaphore mSem;
    private final long mRealmId;
    private final long mUserId;

    /**
     * Create instance.
     *
     * @param ctx     activity context.
     * @param device  unregistered device detail to register.
     * @param realmId The realmId of the realm where to add the user/device to
     * @param userId  The userId of the user where to add the device to.
     */
    public RegistrationCredentialsWriterTask(Context ctx, UnRegisteredDeviceModel device, long realmId, long userId)
    {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid userId");
        }
        mSem = new Semaphore(0);
        mDeviceObject = device;
        this.mDeviceName = device.getUserFriendlyName();
        mCtx = ctx;
        mRealmId = realmId;
        mUserId = userId;
        mBroadcastManager = LocalBroadcastManager.getInstance(mCtx);
        mIntent = new Intent(ACTION_REGISTRATION_QEO_DONE);
    }

    @Override
    public void run()
    {
        LocalServiceConnection localServiceConnection = null;
        try {
            // connect to the qeo service
            localServiceConnection = new LocalServiceConnection(mCtx, mServiceConnection);

            boolean result = getOTCAndWriteToQeo();
            mIntent.putExtra(INTENT_EXTRA_REGISTRATION_RESULT, result);
            mBroadcastManager.sendBroadcast(mIntent);

            // after everything is finished, don't disconnect directly from the Qeo service.
            // this gives the writer some time to finish the write
            // it's not a problem anyway since this runs in a thread.
            Thread.sleep(10000); // sleep for 10 seconds
            localServiceConnection.close();
        }
        catch (Exception e) {
            mIntent.putExtra(INTENT_EXTRA_REGISTRATION_RESULT, false);
            mBroadcastManager.sendBroadcast(mIntent);

            // unbind immediately on a failure
            if (localServiceConnection != null) {
                localServiceConnection.close();
            }
        }
    }

    private boolean getOTCAndWriteToQeo()
        throws Exception
    {
        RegistrationCredentialsHelper helper = new RegistrationCredentialsHelper(mUserId, mDeviceName,
            mDeviceObject.getRSAPublicKey());
        helper.setRealmId(mRealmId);
        byte[] encryptedOtc = helper.registerRemoteDevice();
        if (encryptedOtc == null) {
            return false; //failure
        }

        RegistrationCredentials r = new RegistrationCredentials();
        DeviceId deviceId = new DeviceId();
        deviceId.upper = mDeviceObject.getUpper();
        deviceId.lower = mDeviceObject.getLower();

        r.deviceId = deviceId;
        r.requestRSAPublicKey = mDeviceObject.getRSAPublicKey();
        r.encryptedOtc = encryptedOtc;
        r.realmName = DeviceRegPref.getSelectedRealm();
        r.url = DeviceRegPref.getScepServerURL();
        LOG.fine("write object:" + r.deviceId + r.requestRSAPublicKey + r.realmName + r.url);

        // wait for qeo to be connected.
        mSem.acquire();
        // write over qeo.
        // DeviceRegisterService must be available since QeoService is started
        RemoteDeviceRegistration.getInstance().writeRegistrationCredentials(r);
        return true; // success!
    }

    private final QeoConnectionListener mServiceConnection = new QeoConnectionListener()
    {

        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            mSem.release();
        }

        @Override
        public void onQeoError(QeoException ex)
        {
            LOG.log(Level.WARNING, "Unexpected error", ex);
        }
    };

}
