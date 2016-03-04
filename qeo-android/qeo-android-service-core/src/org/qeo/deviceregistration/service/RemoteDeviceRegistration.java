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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.QeoFactory;
import org.qeo.StateChangeReader;
import org.qeo.StateChangeReaderListener;
import org.qeo.StateWriter;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.exception.QeoException;
import org.qeo.system.RegistrationCredentials;
import org.qeo.system.RegistrationRequest;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * This service starts the Qeo service and prepares the Readers/Writers to read/write the data over Qeo.
 */
public final class RemoteDeviceRegistration
{
    private static final Logger LOG = Logger.getLogger("DeviceRegisterService");
    /** Constant used to set Intent filter for device add call. */
    public static final String ACTION_UNREGISTERED_DEVICE_FOUND = "actionUnregisteredDeviceFound";
    /** Broadcast to be sent when a remote device is lost. */
    public static final String ACTION_UNREGISTERED_DEVICE_LOST = "actionUnregisteredDeviceLost";
    /** Result flag to be added to intent. This is a boolean. */
    public static final String INTENT_EXTRA_RESULT = "intentExtraResult";
    /** Error message in case of failure. */
    public static final String INTENT_EXTRA_ERRORMSG = "intentExtraErroMsg";
    private static RemoteDeviceRegistration sSelf = null;
    private StateChangeReader<RegistrationRequest> mReader;
    private StateWriter<RegistrationCredentials> mWriter;
    private StateChangeReader<RegistrationRequest> mReaderClosed;
    private StateWriter<RegistrationCredentials> mWriterClosed;
    private LinkedHashSet<RegistrationRequest> mUnregisteredDevices;
    private Map<String, RegistrationCredentials> mRegistrationRequests;
    private LocalBroadcastManager mLbm;

    private RemoteDeviceRegistration()
    {
    }

    /**
     * Get an instance of this service.
     *
     * @return The instance.
     */
    public static synchronized RemoteDeviceRegistration getInstance()
    {
        if (sSelf == null) {
            sSelf = new RemoteDeviceRegistration();
        }
        return sSelf;
    }

    /**
     * Start listening for unregistered devices.
     *
     * @param ctx       Android context.
     * @param qeo       QeoFactory. Must the the open domain factory.
     * @param qeoClosed The closed QeoFactory
     */
    public synchronized void start(Context ctx, QeoFactory qeo, QeoFactory qeoClosed)
    {
        LOG.fine("start");
        if (DeviceRegPref.getDeviceRegisterNotifactionEnabled()) {
            mUnregisteredDevices = new LinkedHashSet<RegistrationRequest>();
            mRegistrationRequests = new HashMap<String, RegistrationCredentials>();
            mLbm = LocalBroadcastManager.getInstance(ctx.getApplicationContext());
            init(qeo, qeoClosed);
        }
    }

    /**
     * Stop listening for unregistered devices.
     */
    public synchronized void stop()
    {
        LOG.fine("stop");
        if (mWriter != null) {
            mWriter.close();
            mWriter = null;
        }
        if (mReader != null) {
            mReader.close();
            mReader = null;
        }
        if (mWriterClosed != null) {
            mWriterClosed.close();
            mWriterClosed = null;
        }
        if (mReaderClosed != null) {
            mReaderClosed.close();
            mReaderClosed = null;
        }
    }

    /**
     * Get the list of unregistered devices.
     *
     * @return The list of devices in the open realm.
     */
    public List<RegistrationRequest> getUnregisteredDevices()
    {
        if (mUnregisteredDevices != null) {
            return new LinkedList<RegistrationRequest>(mUnregisteredDevices);
        }
        else {
            return new LinkedList<RegistrationRequest>();
        }
    }

    /**
     * Write registrationcredentials over Qeo.
     *
     * @param creds The credentials to write.
     */
    public synchronized void writeRegistrationCredentials(RegistrationCredentials creds)
    {
        if (mWriter == null) {
            LOG.warning("Can't write registrationcredentials, writer not available");
            return;
        }
        removeOldRequest(creds.requestRSAPublicKey);
        mRegistrationRequests.put(creds.requestRSAPublicKey, creds);
        mWriter.write(creds);
        mWriterClosed.write(creds);
    }

    private synchronized void removeOldRequest(String rsaPublicKey)
    {
        RegistrationCredentials old = mRegistrationRequests.remove(rsaPublicKey);
        if (old != null) {
            // if there is an old request, remove it first
            mWriter.remove(old);
            mWriterClosed.remove(old);
        }
    }

    private synchronized void init(QeoFactory qeo, QeoFactory qeoClosed)
    {
        try {

            mWriter = qeo.createStateWriter(RegistrationCredentials.class);
            mWriterClosed = qeoClosed.createStateWriter(RegistrationCredentials.class);

            StateChangeReaderListener<RegistrationRequest> mReaderListener =
                new StateChangeReaderListener<RegistrationRequest>()
                {

                    @Override
                    public void onData(RegistrationRequest device)
                    {
                        LOG.fine("Got device " + device.userName + " -- " + device.userFriendlyName);
                        mUnregisteredDevices.add(device);
                    }

                    @Override
                    public void onNoMoreData()
                    {
                        // send broadcast that new devices are found or removed
                        Intent intent = new Intent(ACTION_UNREGISTERED_DEVICE_FOUND);
                        intent.putExtra(INTENT_EXTRA_RESULT, true);
                        mLbm.sendBroadcast(intent);
                    }

                    @Override
                    public void onRemove(RegistrationRequest device)
                    {
                        mUnregisteredDevices.remove(device);
                        removeOldRequest(device.rsaPublicKey);

                        // device was removed from unregistered devices list.
                        // refresh list of registered devices, it's likely it's now there.
                        mLbm.sendBroadcast(new Intent(ACTION_UNREGISTERED_DEVICE_LOST));

                    }
                };
            // StatechangeReader to refresh Registered device list.
            mReader =
                qeo.createStateChangeReader(RegistrationRequest.class, mReaderListener);
            mReaderClosed =
                qeoClosed.createStateChangeReader(RegistrationRequest.class, mReaderListener);
            mReaderClosed.setBackgroundNotification(true);
        }

        catch (QeoException e) {
            LOG.log(Level.SEVERE, "Error creating reader/writers", e);
            sendFailureBroadcast(e.getMessage());
        }
        LOG.fine("onQeoReady done");
    }

    private void sendFailureBroadcast(String msg)
    {
        Intent intent = new Intent(ACTION_UNREGISTERED_DEVICE_FOUND);
        intent.putExtra(INTENT_EXTRA_RESULT, false);
        intent.putExtra(INTENT_EXTRA_ERRORMSG, msg);
        mLbm.sendBroadcast(intent);
    }

}
