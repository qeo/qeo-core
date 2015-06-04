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

package org.qeo.android.service;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.android.internal.AidlConstants;
import org.qeo.android.internal.IServiceQeoCallback;
import org.qeo.exception.QeoException;
import org.qeo.java.QeoConnectionListener;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * Local bound connection to QeoService.
 */
public class LocalServiceConnection
{
    private static final Logger LOG = Logger.getLogger("LocalServiceConnection");
    private final QeoConnectionListener mListener;
    private final Context mCtx;
    private final MyServiceConnection mConnection;
    private QeoServiceV1 mService;

    /**
     * Create connection to QeoService.
     * 
     * @param ctx Context.
     * @param listener Listener.
     */
    public LocalServiceConnection(Context ctx, QeoConnectionListener listener)
    {
        mListener = listener;
        mCtx = ctx.getApplicationContext();
        mConnection = new MyServiceConnection();
        Intent intent = new Intent(mCtx, QeoService.class);
        intent.setAction(AidlConstants.AIDL_SERVICE_ACTION_V1);
        intent.setType(ctx.getPackageName());
        mCtx.bindService(intent, mConnection, Service.BIND_AUTO_CREATE);
    }

    /**
     * Close connection to the Qeo service.
     */
    public void close()
    {
        mConnection.close();
        mCtx.unbindService(mConnection);
    }

    private final IServiceQeoCallback.Stub mServiceCallbacks = new IServiceQeoCallback.Stub() {

        @Override
        public void onRegistered()
            throws RemoteException
        {
            mListener.onQeoReady(mService.getQeoFactory());
        }

        @Override
        public void onManifestReady(boolean result)
            throws RemoteException
        {

        }

        @Override
        public void onOtpDialogCanceled()
            throws RemoteException
        {
            mListener.onQeoError(null);
        }

        @Override
        public void onSecurityInitFailed(String reason)
            throws RemoteException
        {
            mListener.onQeoError(new QeoException(reason));
        }

        @Override
        public void onUnregistered()
            throws RemoteException
        {
            mListener.onQeoClosed(mService.getQeoFactory());
        }

        @Override
        public boolean onStartAuthentication()
            throws RemoteException
        {
            return false;
        }

        @Override
        public void onWakeUp(String typeName)
            throws RemoteException
        {
            mListener.onWakeUp(typeName);
        }

        @Override
        public void onBgnsConnected(boolean state) throws RemoteException
        {
            mListener.onBgnsConnectionChange(state);
        }
    };

    private final class MyServiceConnection
        implements ServiceConnection
    {

        public void close()
        {
            try {
                mService.unregister(mServiceCallbacks);
            }
            catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Remote exception in onServiceConnected", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            mListener.onQeoClosed(null);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder)
        {
            mService = (QeoServiceV1) serviceBinder;
            try {
                mService.register(mServiceCallbacks);
            }
            catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Remote exception in onServiceConnected", e);
            }
        }

    }
}
