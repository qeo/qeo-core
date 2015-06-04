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

package org.qeo.android.test;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.qeo.android.QeoAndroid;
import org.qeo.android.internal.IServiceQeoCallback;
import org.qeo.android.internal.IServiceQeoV1;
import org.qeo.android.internal.ParcelableData;
import org.qeo.android.internal.ParcelableException;
import org.qeo.internal.common.ObjectData;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.test.AndroidTestCase;
import android.util.Log;

/**
 * Class to try to test security by using internal functions.
 */
public class InternalTest
    extends AndroidTestCase
{
    private BinderConnection binderConnection;
    private QeoConnection qeoConnection;
    private IServiceQeoV1 mServiceQeo;
    private Context ctx;
    private Semaphore sem;

    /**
     * Try to write on a writer that is not owned by us.
     * 
     * @throws Exception on error.
     */
    public void testStealWriter()
        throws Exception
    {
        if (Helper.isEmbedded()) {
            return;
        }
        ctx = getContext().getApplicationContext();
        binderConnection = new BinderConnection();
        qeoConnection = new QeoConnection();
        sem = new Semaphore(0);
        T t = new T();
        t.start();
        sem.tryAcquire(30, TimeUnit.SECONDS); // wait for qeo to start

        ObjectData od = new ObjectData(123);
        ParcelableData pd = new ParcelableData(od);
        Parcel p = Parcel.obtain();
        pd.writeToParcel(p, 0);
        p.setDataPosition(0);
        byte[] buf = p.marshall();
        p.recycle();
        ParcelableException pex = new ParcelableException();
        // writing on writer with id 5, will not exist and hence not owned by us
        mServiceQeo.write(5, true, true, buf.length, buf, pex);
        if (pex.getException() == null) {
            fail("Expected exception");
        }
        if (!(pex.getException() instanceof SecurityException)) {
            fail("Expected securityException");
        }
        ctx.unbindService(binderConnection);
    }

    class T
        extends Thread
    {
        @Override
        public void run()
        {
            final Intent intent = new Intent();
            intent.setClassName(QeoAndroid.QEO_SERVICE_PACKAGE, QeoAndroid.QEO_SERVICE_PACKAGE + ".QeoService");
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
            String[] manifest =
                new String[] {"[meta]", "appname=qeo-android-tests", "version=1", "[application]",
                    "org::something::not::used=rw"};
            // qeo ready
            mServiceQeo.disableManifestPopup();
            ParcelableException exception = new ParcelableException();
            mServiceQeo.pushManifest(qeoConnection, manifest, exception);
            if (exception.getException() != null) {
                fail("Unexpected exception");
            }
            Log.i("ManifestTest", "Qeo OK");
        }

        @Override
        public void onUnregistered()
            throws RemoteException
        {

        }

        @Override
        public void onManifestReady(boolean result)
            throws RemoteException
        {
            // manifest ready
            Log.i("ManifestTest", "Manifest OK");
            sem.release();
        }

        @Override
        public void onOtpDialogCanceled()
            throws RemoteException
        {
        }

        @Override
        public void onSecurityInitFailed(String reason)
            throws RemoteException
        {
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
        }

        @Override
        public void onBgnsConnected(boolean state) throws RemoteException
        {
        }
    }
}
