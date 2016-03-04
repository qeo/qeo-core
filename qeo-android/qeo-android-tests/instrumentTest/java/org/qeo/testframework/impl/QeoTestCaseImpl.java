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

package org.qeo.testframework.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.qeo.QeoFactory;
import org.qeo.android.QeoAndroid;
import org.qeo.android.QeoConnectionListener;
import org.qeo.android.internal.QeoConnection;
import org.qeo.exception.QeoException;
import org.qeo.internal.QeoListener;
import org.qeo.testframework.QeoConnectionTestListener;
import org.qeo.testframework.QeoTestCaseImplInterface;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.test.AndroidTestCase;
import android.util.Log;

/**
 * Android specific implementation for QeoTestCase to initialize the QeoFactory.
 */
public abstract class QeoTestCaseImpl
    extends AndroidTestCase
    implements QeoTestCaseImplInterface
{
    /** timeout for Qeo init. */
    private static final int QEO_INIT_TIMEOUT = 30;

    /**
     * QeoFactory instance.
     */
    protected QeoFactory mQeo = null;
    private QeoReady mQeoConnection;

    /**
     * Android application context.
     */
    protected Context mCtx = null;

    /**
     * The logging tag for unit tests.
     */
    protected static final String TAG = "QeoJunit";
    private static boolean sQeoDirty = false;

    /**
     * scale factor that can be used in some tests as native java is much faster than Android.
     */
    protected int mScaleFactor = 1;
    private Handler mHandler;

    @Override
    public void setUp()
        throws Exception
    {
        setUp(true);
    }

    /**
     * SetUp unit tests.
     * 
     * @param initQeo true if the factory needs to be created, false otherwise.
     * @throws Exception If something goes wrong.
     */
    @Override
    public void setUp(boolean initQeo)
        throws Exception
    {
        super.setUp();
        mQeo = null;
        mQeoConnection = null;
        log("setup");
        for (int i = 0; i < 10; i++) {
            // On Android 4 this call may not immediately be available. So check if the result is valid.
            mCtx = getContext().getApplicationContext();
            if (mCtx != null) {
                break;
            }
            log("Application context is not yet available, sleeping...");
            Thread.sleep(100);
        }
        if (mCtx == null) {
            throw new IllegalStateException("Application context not available");
        }
        mHandler = new Handler(Looper.getMainLooper());

        QeoConnection.disableManifestPopup();
        if (initQeo) {
            try {
                QeoConnection.getInstance();
                fail("QeoConnection was not closed properly by previous test");
            }
            catch (IllegalStateException ex) {
                // ok
            }
            if (sQeoDirty) {
                fail("A previous test already caused a connection problem to the factory, not retrying");
            }
            log("Binding to service");
            mQeoConnection = new QeoReady();
            QeoAndroid.initQeo(mCtx, mQeoConnection, Looper.getMainLooper());
            // fail if the service does not get initialized within 10 seconds
            mQeoConnection.waitForInit();
            mQeo = mQeoConnection.getFactory();
            if (sQeoDirty) {
                fail("Got an onQeoClosed and an automatic re-bind. This propably means the service has crashed. "
                    + "Please check logcat.");
            }
        }
    }

    @Override
    public void tearDown()
        throws Exception
    {
        super.tearDown();
        if (mQeoConnection != null) {
            QeoAndroid.closeQeo(mQeoConnection);
        }
        try {
            QeoConnection.getInstance();
            fail("QeoConnection did not get closed properly");
        }
        catch (IllegalStateException ex) {
            // ok
        }
    }

    @Override
    public QeoConnectionTestListener createFactory(int id)
        throws InterruptedException
    {
        QeoReady qeoReady2 = new QeoReady();
        QeoAndroid.initQeo(mCtx, qeoReady2, Looper.getMainLooper(), id);
        qeoReady2.waitForInit();
        return qeoReady2;
    }

    @Override
    public QeoConnectionTestListener createFactory(QeoListener listener)
    {
        QeoReady qeoReady = new QeoReady(listener);
        QeoAndroid.initQeo(mCtx, qeoReady, Looper.getMainLooper());
        // do not wait for init
        return qeoReady;
    }

    @Override
    public int getFactoryOpenId()
    {
        return QeoAndroid.OPEN_ID;
    }

    /**
     * Get closed domain id.
     * 
     * @return id.
     */
    public int getFactoryClosedId()
    {
        return QeoAndroid.DEFAULT_ID;
    }

    @Override
    public void closeFactory(QeoConnectionTestListener qeoConnection)
    {
        qeoConnection.closeFactory();
    }

    /**
     * Log a message to logcat. This function is synchronized to get a decent output.
     * 
     * @param msg The message to be logged
     */
    protected static synchronized void log(String msg)
    {
        Log.d(TAG, msg);
    }

    /**
     * Execute runnable on the same thread as Qeo does the callbacks.
     * 
     * @param r The runnable.
     */
    @Override
    public void runOnQeoThread(Runnable r)
    {
        mHandler.post(r);
    }

    @Override
    public void waitForFactoryInit(Semaphore sem)
        throws InterruptedException
    {
        if (sQeoDirty) {
            fail("A previous test already caused a connection problem to the factory, not retrying");
        }
        if (!sem.tryAcquire(QEO_INIT_TIMEOUT, TimeUnit.SECONDS)) {
            sQeoDirty = true;
            // If you want to be able to attach debugger if something goes wrong, uncomment this
            // while (true) {
            // Log.e("JUNIT", "Can't init qeo, BLOCKING");
            // Thread.sleep(30000);
            // }
            fail("Could not create Android factory. Most likely the service crashed. Please check logcat.");
        }

    }

    @Override
    public InputStream openAsset(String name) throws IOException
    {
        try {
            return getContext().getPackageManager().getResourcesForApplication("org.qeo.android.service.test")
                .getAssets().open(name);
        }
        catch (Exception e) {
            log("Can't find asset in test package, trying in own package");
        }
        return getContext().getResources().getAssets().open(name);
    }

    private class QeoReady
        extends QeoConnectionListener
        implements QeoConnectionTestListener
    {
        private final Semaphore mSem;
        private QeoFactory mFactory;
        private final QeoListener mListener;

        QeoReady()
        {
            this(null);
        }

        QeoReady(QeoListener listener)
        {
            mListener = listener;
            mSem = new Semaphore(0);
        }

        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            log("Binding to service done");
            mFactory = qeo;
            if (mListener != null) {
                mListener.onQeoReady(qeo);
            }
            mSem.release();
        }

        @Override
        public void onQeoClosed(QeoFactory qeo)
        {
            if (mListener != null) {
                mListener.onQeoClosed(qeo);
            }
            else {
                log("Got an onQeoClosed!");
                sQeoDirty = true;
            }
        }

        @Override
        public void onQeoError(QeoException ex)
        {
            if (mListener != null) {
                mListener.onQeoError(ex);
            }
            else {
                Log.e(TAG, "Qeo init failed", ex);
                fail("Qeo init failed: " + ex);
                sQeoDirty = true;
            }
        }

        @Override
        public QeoFactory getFactory()
        {
            return mFactory;
        }

        @Override
        public void waitForInit()
            throws InterruptedException
        {
            waitForFactoryInit(mSem);
        }

        @Override
        public void closeFactory()
        {
            QeoAndroid.closeQeo(this);
        }

    }
}
