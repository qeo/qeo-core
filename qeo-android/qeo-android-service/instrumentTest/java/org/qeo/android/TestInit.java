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

package org.qeo.android;

import java.io.File;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.qeo.QeoFactory;
import org.qeo.android.service.DeviceInfoAndroid;
import org.qeo.android.service.ServiceApplication;
import org.qeo.exception.QeoException;
import org.qeo.java.QeoConnectionListener;
import org.qeo.java.QeoJava;
import org.qeo.jni.NativeQeo;

import android.content.Context;
import android.test.AndroidTestCase;

/**
 * 
 */
public class TestInit
    extends AndroidTestCase
{
    private static final Logger LOG = Logger.getLogger("TestInit");
    private static final int NUM_LOOPS = 5;
    private boolean failed;

    @Override
    public void setUp()
    {
        Context ctx = getContext();
        ServiceApplication.initServiceApp(ctx.getApplicationContext());
        File dir = ctx.getDir("junit", Context.MODE_PRIVATE);
        for (File f : dir.listFiles()) {
            // clean directory
            assertTrue(f.delete());
        }
        NativeQeo.setStoragePath(dir.getAbsolutePath());
        NativeQeo.setDeviceInfo(DeviceInfoAndroid.getInstance().getDeviceInfo());
        failed = false;
    }

    public void testSingleFactory()
        throws InterruptedException
    {
        for (int i = 0; i < NUM_LOOPS; ++i) {
            LOG.fine("Run " + i);
            final MyConnectionListener connClosed = new MyConnectionListener(true);

            Thread t = new Thread() {
                @Override
                public void run()
                {
                    QeoJava.initQeo(connClosed);
                }
            };
            t.start();
            LOG.fine("Waiting for Qeo to be ready");
            assertTrue(connClosed.semAuth.tryAcquire(10, TimeUnit.SECONDS));
            Thread.sleep(100); // wait a few moments
            assertFalse("Failure!", failed);

            LOG.fine("Closing down!");
            QeoJava.closeQeo(connClosed);
            assertTrue(connClosed.semError.tryAcquire(10, TimeUnit.SECONDS));

            assertFalse("Failure!", failed);
        }
    }

    public void testSingleFactoryOpen()
        throws InterruptedException
    {
        for (int i = 0; i < NUM_LOOPS; ++i) {
            LOG.fine("Run " + i);
            final MyConnectionListener connOpen = new MyConnectionListener(false);

            Thread t = new Thread() {
                @Override
                public void run()
                {
                    QeoJava.initQeo(QeoJava.OPEN_ID, connOpen);
                }
            };
            t.start();
            LOG.fine("Waiting for Qeo to be ready");
            assertTrue(connOpen.semReady.tryAcquire(10, TimeUnit.SECONDS));
            Thread.sleep(100); // wait a few moments
            assertFalse("Failure!", failed);

            LOG.fine("Closing down!");
            QeoJava.closeQeo(connOpen);

            assertFalse("Failure!", failed);
        }
    }

    public void test2Factories()
        throws InterruptedException
    {
        for (int i = 0; i < NUM_LOOPS; ++i) {
            LOG.fine("Run " + i);
            final MyConnectionListener connClosed = new MyConnectionListener(true);
            final MyConnectionListener connOpen = new MyConnectionListener(false);

            Thread tClosed = new Thread() {
                @Override
                public void run()
                {
                    QeoJava.initQeo(connClosed);
                    // this will block, so create open factory in other thread
                }
            };
            Thread tOpen = new Thread() {
                @Override
                public void run()
                {
                    QeoJava.initQeo(QeoJava.OPEN_ID, connOpen);
                }
            };
            tClosed.start();
            tOpen.start();

            LOG.fine("Waiting for Qeo to be ready");
            assertTrue(connClosed.semAuth.tryAcquire(10, TimeUnit.SECONDS));
            assertTrue(connOpen.semReady.tryAcquire(10, TimeUnit.SECONDS));
            Thread.sleep(100); // wait a few moments
            assertFalse("Failure!", failed);

            LOG.fine("Closing down!");
            QeoJava.closeQeo(connClosed);
            QeoJava.closeQeo(connOpen);
            assertTrue(connClosed.semError.tryAcquire(10, TimeUnit.SECONDS));

            assertFalse("Failure!", failed);
        }
    }

    private final class MyConnectionListener
        extends QeoConnectionListener
    {
        private final Semaphore semReady;
        private final Semaphore semError;
        private final Semaphore semAuth;
        private boolean readyCalled = false;
        private final boolean mExpectAuth;

        public MyConnectionListener(boolean expectAuth)
        {
            mExpectAuth = expectAuth;
            semReady = new Semaphore(0);
            semError = new Semaphore(0);
            semAuth = new Semaphore(0);
        }

        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            if (readyCalled) {
                LOG.severe("Ready called twice for: " + qeo);
                failed = true;
            }
            readyCalled = true;

            if (mExpectAuth) {
                // should not get here, there are no credentials
                LOG.severe("OnQeoReady: " + qeo);
                failed = true;
            }
            else {
                LOG.fine("OnQeoReady: " + qeo);
            }
            semReady.release();
        }

        @Override
        public void onQeoError(QeoException ex)
        {
            if (readyCalled) {
                LOG.severe("Ready called twice");
                failed = true;
            }
            readyCalled = true;

            // should get here after cancelling registration
            semError.release();
        }

        @Override
        public boolean onStartAuthentication()
        {
            if (mExpectAuth) {
                LOG.fine("OnStartAuthentication");
            }
            else {
                LOG.severe("Did not expect onStartAuthentication call");
                failed = true;
            }
            semAuth.release();
            return true;
        }

        @Override
        public void onStatusUpdate(String status, String reason)
        {
            LOG.fine("OnStatusUpdate: " + status + " -- " + reason);
        }

    }
}
