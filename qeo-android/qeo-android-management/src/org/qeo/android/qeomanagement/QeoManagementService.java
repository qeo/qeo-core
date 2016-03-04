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

package org.qeo.android.qeomanagement;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.QeoFactory;
import org.qeo.android.QeoAndroid;
import org.qeo.android.QeoConnectionListener;
import org.qeo.android.internal.QeoConnection;
import org.qeo.android.internal.ServiceDisconnectedException;
import org.qeo.exception.QeoException;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * This service takes care of the polling of the security policy file. Every 2 minutes, a special HTTP request is done
 * to see if the policy file is still valid. If it is not valid, the QeoService will be triggered to pull it again from
 * the server.
 */
public class QeoManagementService
    extends Service
{
    private static final String TAG = "QeoMgmtApp";
    private static final Logger LOG = Logger.getLogger(TAG);
    private boolean mStarted = false;
    private ServiceThread mServiceThread;
    private QeoFactory mQeo = null;
    private QeoConnectionListener mListener = null;

    private class ServiceThread
        extends Thread
    {
        @Override
        public void run()
        {
            while (!isInterrupted()) {
                LOG.fine("Refresh policy");
                try {
                    /* Check if the policy needs to be refreshed */
                    if (mQeo != null) {
                        QeoConnection.getInstance().getProxy().refreshPolicy();
                    }
                    /* Sleep for 2 minutes */
                    Thread.sleep(2 * 60 * 1000);
                }
                catch (RemoteException e) {
                    LOG.severe("Refreshing policy failed");
                }
                catch (ServiceDisconnectedException e) {
                    LOG.log(Level.SEVERE, "Refreshing policy failed", e);
                }
                catch (InterruptedException e) {
                    LOG.fine("Polling thread interrupted");
                    break;
                }
            }
        }
    }

    @Override
    public void onCreate()
    {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (!mStarted) {
            mStarted = true;
            LOG.fine("Initialize Qeo");
            mListener = new QeoConnectionListener() {
                @Override
                public void onQeoReady(QeoFactory qeo)
                {
                    LOG.fine("Construct polling thread");
                    mQeo = qeo;
                    startPolling();
                }

                @Override
                public void onQeoClosed(QeoFactory qeo)
                {
                    stopPolling();
                }

                @Override
                public void onQeoError(QeoException ex)
                {
                    LOG.fine("Stop the polling service");
                    stopSelf();
                }
            };
            QeoAndroid.initQeo(getApplicationContext(), mListener, null);
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onDestroy()
    {
        if (mStarted) {
            QeoAndroid.closeQeo(mListener);
            stopPolling();
            mStarted = false;
        }
    }

    private void startPolling()
    {
        LOG.fine("Start polling thread");
        mServiceThread = new ServiceThread();
        mServiceThread.start();
    }

    private void stopPolling()
    {
        LOG.fine("Stop polling thread");
        if (mServiceThread != null) {
            mServiceThread.interrupt();
            mServiceThread = null;
        }
    }
}
