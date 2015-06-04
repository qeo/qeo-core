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

package org.qeo.testframework.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.QeoFactory;
import org.qeo.android.service.QeoService;
import org.qeo.exception.QeoException;
import org.qeo.java.QeoConnectionListener;
import org.qeo.java.QeoJava;
import org.qeo.jni.NativeFactory;
import org.qeo.jni.NativeQeo;
import org.qeo.testframework.QeoConnectionTestListener;
import org.qeo.testframework.QeoTestCaseImplInterface;

import android.test.AndroidTestCase;

/**
 * Android will override this class by a custom implementation that initializes the Qeo Service
 */
public abstract class QeoTestCaseImpl
    extends AndroidTestCase
    implements QeoTestCaseImplInterface
{
    private static final Logger LOG = Logger.getLogger(QeoTestCaseImpl.class.getName());
    private ExecutorService mExecutor;

    /**
     * The factory to be used
     */
    protected QeoFactory mQeo;
    private QeoReadyListener qeoReady;

    /**
     * scale factor that can be used in some tests as native java is much faster than android
     */
    protected int mScaleFactor = 10;

    @Override
    public void setUp(boolean initQeo)
        throws Exception
    {
        super.setUp();
        mExecutor = Executors.newSingleThreadExecutor();

        NativeQeo.setQeoParameter("FWD_DISABLE_LOCATION_SERVICE", "1");
        QeoService.initNative(getContext().getApplicationContext());

        if (initQeo) {
            qeoReady = new QeoReadyListener();
            QeoJava.initQeo(qeoReady);
            qeoReady.waitForInit();
            mQeo = qeoReady.getFactory();
        }
    }

    @Override
    public void setUp()
        throws Exception
    {
        setUp(true);
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        if (qeoReady != null) {
            NativeQeo.preDestroy();
            QeoJava.closeQeo(qeoReady);


            mQeo = null;
            NativeQeo.postDestroy();

            int nativeFactories = NativeFactory.getNumFactories();
            if (nativeFactories != 0) {
                fail("WARNING: not all native factories were destroyed, this may harm next tests!");
            }
        }
        mExecutor.shutdownNow();
        super.tearDown();
    }

    protected static synchronized void log(String msg)
    {
        System.out.println(msg);
    }

    @Override
    public QeoConnectionTestListener createFactory(int id)
        throws InterruptedException
    {
        QeoReadyListener qeoReady2 = new QeoReadyListener();
        QeoJava.initQeo(id, qeoReady2);
        qeoReady2.waitForInit();
        return qeoReady2;
    }

    @Override
    public int getFactoryOpenId()
    {
        return QeoJava.OPEN_ID;
    }

    @Override
    public void closeFactory(QeoConnectionTestListener qeoConnection)
    {
        qeoConnection.closeFactory();
    }

    @Override
    public InputStream openAsset(String name) throws IOException
    {
        try {
            return getContext().getPackageManager().getResourcesForApplication("org.qeo.android.service.test")
                .getAssets().open(name);
        }
        catch (Exception e) {
            LOG.warning("Can't find asset in test package, trying in own package");
        }
        return getContext().getResources().getAssets().open(name);
    }

    /**
     * Execute runnable on the same thread as Qeo does the callbacks.<br>
     * NOTE: this is not possible in qeo-java, so will be executed on another thread.
     *
     * @param r The runnable.
     */
    @Override
    public void runOnQeoThread(Runnable r)
    {
        mExecutor.execute(r);
    }

    private static class QeoReadyListener
        extends QeoConnectionListener
        implements QeoConnectionTestListener
    {
        private final Semaphore mSem;
        private QeoFactory mQeo;
        private String lastMsg = null;

        public QeoReadyListener()
        {
            mSem = new Semaphore(0);
        }

        @Override
        public void waitForInit()
            throws InterruptedException
        {
            if (!mSem.tryAcquire(20, TimeUnit.SECONDS)) {
                // If you want to be able to attach debugger if something goes wrong, uncomment this
                // while (true) {
                // LOG.severe("Can't init qeo, BLOCKING");
                // Thread.sleep(30000);
                // }
                throw new IllegalStateException("Qeo factory could not be created");
            }
        }

        @Override
        public QeoFactory getFactory()
        {
            return mQeo;
        }

        @Override
        public void onStatusUpdate(String status, String reason)
        {
            LOG.fine("QeoReadyListener: " + status + ": " + reason);
            lastMsg = status + ": " + reason;
        }

        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            mQeo = qeo;
            mSem.release();
        }

        @Override
        public void onQeoClosed(QeoFactory qeo)
        {
        }

        @Override
        public void onQeoError(QeoException ex)
        {
            LOG.log(Level.SEVERE, "ERROR: Cannot init Qeo for Junit", ex);
            throw new IllegalStateException("Qeo not ready: " + lastMsg);
        }

        @Override
        public boolean onStartAuthentication()
        {
            LOG.severe("ERROR: got onStartAuthentication call, did not expect this");
            throw new IllegalStateException("not supported");
        }

        @Override
        public void closeFactory()
        {
            QeoJava.closeQeo(this);
        }
    }
}
