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

import org.qeo.QeoFactory;
import org.qeo.exception.QeoException;
import org.qeo.internal.QeoListener;
import org.qeo.java.QeoConnectionListener;
import org.qeo.java.QeoJava;
import org.qeo.jni.NativeFactory;
import org.qeo.jni.NativeQeo;
import org.qeo.testframework.QeoConnectionTestListener;
import org.qeo.testframework.QeoTestCaseImplInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

/**
 * Android will override this class by a custom implementation that initializes the Qeo Service
 */
public abstract class QeoTestCaseImpl extends TestCase implements QeoTestCaseImplInterface
{
    private static final Logger LOG = Logger.getLogger(QeoTestCaseImpl.class.getName());
    private ExecutorService mExecutor;

    /**
     * The factory to be used
     */
    protected QeoFactory mQeo;
    private QeoReadyListener mQeoReady;

    /**
     * scale factor that can be used in some tests as native java is much faster than android
     */
    protected int mScaleFactor = 10;

    @Override
    public void setUp(boolean initQeo) throws Exception
    {
        super.setUp();
        mQeoReady = null;
        mQeo = null;
        mExecutor = Executors.newSingleThreadExecutor();
        String logLevel = System.getenv("LOGLEVEL");

        if (logLevel != null) {
            NativeQeo.setLogLevel(Level.parse(logLevel));
            QeoJava.setLogLevel(Level.parse(logLevel));
        }

        initNativeHost();

        if (initQeo) {
            LOG.fine("InitQeo");
            mQeoReady = new QeoReadyListener();
            QeoJava.initQeo(mQeoReady);
            mQeoReady.waitForInit();
            mQeo = mQeoReady.getFactory();
        }
    }

    public static void initNativeHost()
    {
        NativeQeo.setQeoParameter("FWD_DISABLE_LOCATION_SERVICE", "1");
        File dotQeo = new File("src/test/data/home.qeo/");
        // for this project
        if (dotQeo.exists()) {
            NativeQeo.setStoragePath(dotQeo.getAbsolutePath() + "/");
        }
        dotQeo = new File("build/download/home.qeo");
        // for other projects that download the files (samples)
        if (dotQeo.exists()) {
            NativeQeo.setStoragePath(dotQeo.getAbsolutePath() + "/");
        }
    }

    @Override
    public void setUp() throws Exception
    {
        setUp(true);
    }

    @Override
    protected void tearDown() throws Exception
    {
        NativeQeo.preDestroy();
        if (mQeoReady != null) {
            QeoJava.closeQeo(mQeoReady);
            mQeoReady = null;
        }

        mQeo = null;
        NativeQeo.postDestroy();
        mExecutor.shutdownNow();
        int nativeFactories = NativeFactory.getNumFactories();
        if (nativeFactories != 0) {
            fail("WARNING: not all native factories were destroyed, this may harm next tests!");
        }
        super.tearDown();
    }

    protected static synchronized void log(String msg)
    {
        System.out.println(msg);
    }

    @Override
    public QeoConnectionTestListener createFactory(int id) throws InterruptedException
    {
        QeoReadyListener qeoReady = new QeoReadyListener();
        QeoJava.initQeo(id, qeoReady);
        qeoReady.waitForInit();
        return qeoReady;
    }

    @Override
    public QeoConnectionTestListener createFactory(QeoListener listener)
    {
        QeoReadyListener qeoReady = new QeoReadyListener(listener);
        QeoJava.initQeo(qeoReady);
        // do not wait for init
        return qeoReady;
    }

    @Override
    public int getFactoryOpenId()
    {
        return QeoJava.OPEN_ID;
    }

    @Override
    public int getFactoryClosedId()
    {
        return QeoJava.DEFAULT_ID;
    }

    @Override
    public void closeFactory(QeoConnectionTestListener qeoConnectionListener)
    {
        qeoConnectionListener.closeFactory();
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

    @Override
    public void waitForFactoryInit(Semaphore sem) throws InterruptedException
    {
        if (!sem.tryAcquire(20, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Qeo factory could not be created");
        }
    }

    @Override
    public InputStream openAsset(String name) throws IOException
    {
        return new FileInputStream(new File("src/test/data/assets/" + name));
    }

    private class QeoReadyListener extends QeoConnectionListener implements QeoConnectionTestListener
    {
        private final Semaphore mSem;
        private QeoFactory mQeo;
        private String lastMsg = null;
        private final QeoListener mListener;

        public QeoReadyListener()
        {
            this(null);
        }

        public QeoReadyListener(QeoListener listener)
        {
            mListener = listener;
            mSem = new Semaphore(0);
        }

        @Override
        public void waitForInit() throws InterruptedException
        {
            waitForFactoryInit(mSem);
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
        }

        @Override
        public void onQeoError(QeoException ex)
        {
            if (mListener != null) {
                mListener.onQeoError(ex);
            }
            else {
                System.out.println("ERROR: Cannot init Qeo for Junit");
                throw new IllegalStateException("Qeo not ready: " + lastMsg);
            }
        }

        @Override
        public void closeFactory()
        {
            QeoJava.closeQeo(this);
        }
    }

}
