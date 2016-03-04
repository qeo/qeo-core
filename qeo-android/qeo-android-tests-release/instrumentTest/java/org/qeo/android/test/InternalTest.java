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

package org.qeo.android.test;

import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import org.qeo.android.internal.IServiceQeoCallback;
import org.qeo.android.internal.IServiceQeoV1;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.test.AndroidTestCase;
import android.util.Log;

/**
 * Class to try to test security by using internal functions.
 */
public class InternalTest
    extends AndroidTestCase
{
    private static final Logger LOG = Logger.getLogger("ManifestTest");
    private BinderConnection binderConnection;
    private QeoConnection qeoConnection;
    private IServiceQeoV1 mServiceQeo;
    private Context ctx;
    private Semaphore sem;

    /**
     * Test if disabling the manifest popup does not work.
     * 
     * @throws Exception on error.
     */
    public void testManifestDisabling()
        throws Exception
    {
        LOG.fine("testManifestDisabling");
        ctx = getContext().getApplicationContext();
        binderConnection = new BinderConnection();
        qeoConnection = new QeoConnection();
        sem = new Semaphore(0);
        T t = new T();
        t.start();
        sem.acquireUninterruptibly(); // wait for qeo to start

        assertFalse(mServiceQeo.disableManifestPopup());
    }

    class T
        extends Thread
    {
        @Override
        public void run()
        {
            final Intent intent = new Intent("org.qeo.android.service.QeoService");
            if (!ctx.bindService(intent, binderConnection, Context.BIND_AUTO_CREATE)) {
                fail("oeps");
            }

        }
    }

    class BinderConnection
        implements ServiceConnection
    {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            Log.i("ManifestTest", "Bind OK");
            mServiceQeo = IServiceQeoV1.Stub.asInterface(service);

            try {
                mServiceQeo.register(qeoConnection);
            }
            catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {

        }

    }

    class QeoConnection
        extends IServiceQeoCallback.Stub
    {

        @Override
        public void onRegistered()
            throws RemoteException
        {
            // qeo ready
            LOG.fine("Qeo OK");
            sem.release();
        }

        @Override
        public void onUnregistered()
            throws RemoteException
        {
            LOG.warning("Error: onUnregistered");

        }

        @Override
        public void onManifestReady(boolean result)
            throws RemoteException
        {
            LOG.warning("Error: onManifestReady: " + result);

        }

        @Override
        public void onOtpDialogCanceled()
            throws RemoteException
        {
            LOG.warning("Error: onOtpDialogCanceled");

        }

        @Override
        public void onSecurityInitFailed(String reason)
            throws RemoteException
        {
            LOG.warning("Error: onSecurityInitFailed: " + reason);
        }

        @Override
        public boolean onStartAuthentication()
            throws RemoteException
        {
            LOG.warning("Error: onStartAuthentication");
            return false;
        }

        @Override
        public void onWakeUp(String typeName)
            throws RemoteException
        {
            LOG.warning("Error: onWakeUp");
        }

        @Override
        public void onBgnsConnected(boolean state) throws RemoteException
        {

        }
    }
}
