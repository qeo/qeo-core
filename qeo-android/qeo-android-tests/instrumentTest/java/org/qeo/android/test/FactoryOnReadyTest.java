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

import org.qeo.QeoFactory;
import org.qeo.android.QeoAndroid;
import org.qeo.android.QeoConnectionListener;
import org.qeo.android.internal.ServiceConnection;
import org.qeo.exception.QeoException;
import org.qeo.internal.QeoListener;
import org.qeo.testframework.QeoConnectionTestListener;
import org.qeo.testframework.QeoTestCase;

import android.os.Looper;
import android.util.Log;

/**
 * Testcase to see if onQeoReady is called at correct moments
 */
public class FactoryOnReadyTest
    extends QeoTestCase
{
    private QeoFactory qeo1;
    private QeoFactory qeo2;
    private QeoConnectionListener mQeoConnection1;
    private QeoConnectionListener mQeoConnection2;
    private boolean failure = false;
    private String failureMsg;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp(false); // no default qeo init
        failure = false;
        failureMsg = null;
    }

    @Override
    public void tearDown()
        throws Exception
    {
        if (mQeoConnection1 != null) {
            QeoAndroid.closeQeo(mQeoConnection1);
        }
        if (mQeoConnection2 != null) {
            QeoAndroid.closeQeo(mQeoConnection2);
        }
        super.tearDown();
    }

    /**
     * Testcase to see if an onQeoReady gets called again if a 2nd qeo factory is created
     * 
     * @throws Exception if something goes wrong
     */
    public void testMultipleReady()
        throws Exception
    {
        final Semaphore sem1 = new Semaphore(0);
        final Semaphore sem2 = new Semaphore(0);
        qeo1 = null;
        qeo2 = null;
        mQeoConnection1 = new QeoConnectionListener() {

            @Override
            public void onQeoReady(QeoFactory qeo)
            {
                if (qeo1 != null) {
                    failureMsg = "got twice onready on qeo1";
                }
                qeo1 = qeo;
                sem1.release();
            }

            @Override
            public void onQeoError(QeoException ex)
            {
                failure = true;
                Log.e(TAG, "Qeo error", ex);
            }
        };
        QeoAndroid.initQeo(mCtx, mQeoConnection1, Looper.getMainLooper());
        waitForData(sem1);
        assertEquals(sem1.availablePermits(), 0);
        mQeoConnection2 = new QeoConnectionListener() {

            @Override
            public void onQeoReady(QeoFactory qeo)
            {
                if (qeo2 != null) {
                    failureMsg = "got twice onready on qeo2";
                }
                qeo2 = qeo;
                sem2.release();
            }

            @Override
            public void onQeoError(QeoException ex)
            {
                failure = true;
                Log.e(TAG, "Qeo error", ex);
            }
        };
        QeoAndroid.initQeo(mCtx, mQeoConnection2, Looper.getMainLooper());
        waitForData(sem2);
        Thread.sleep(200);

        assertNotNull(qeo1);
        assertNotNull(qeo2);
        assertFalse(qeo1 == qeo2);
        assertEquals(sem1.availablePermits(), 0);
        assertEquals(sem2.availablePermits(), 0);
        assertFalse(failure);
        assertNull(failureMsg);
    }

    /**
     * Same as test 1, but not do QeoInit twice very fast instead of waiting.
     */
    public void testMultipleReady2()
        throws Exception
    {
        final Semaphore sem1 = new Semaphore(0);
        final Semaphore sem2 = new Semaphore(0);
        qeo1 = null;
        qeo2 = null;
        mQeoConnection1 = new QeoConnectionListener() {

            @Override
            public void onQeoReady(QeoFactory qeo)
            {
                if (qeo1 != null) {
                    failureMsg = "got twice onready on qeo1";
                }
                qeo1 = qeo;
                sem1.release();
            }

            @Override
            public void onQeoError(QeoException ex)
            {
                failure = true;
                Log.e(TAG, "Qeo error", ex);
            }
        };
        QeoAndroid.initQeo(mCtx, mQeoConnection1, Looper.getMainLooper());
        mQeoConnection2 = new QeoConnectionListener() {

            @Override
            public void onQeoReady(QeoFactory qeo)
            {
                if (qeo2 != null) {
                    failureMsg = "got twice onready on qeo2";
                }
                qeo2 = qeo;
                sem2.release();
            }

            @Override
            public void onQeoError(QeoException ex)
            {
                failure = true;
                Log.e(TAG, "Qeo error", ex);
            }
        };
        QeoAndroid.initQeo(mCtx, mQeoConnection2, Looper.getMainLooper());

        waitForData(sem1);
        waitForData(sem2);
        Thread.sleep(200);

        assertNotNull(qeo1);
        assertNotNull(qeo2);
        assertFalse(qeo1 == qeo2);
        assertEquals(sem1.availablePermits(), 0);
        assertEquals(sem2.availablePermits(), 0);
        assertFalse(failure);
        assertNull(failureMsg);
    }

    public void testCloseBeforeReady()
        throws InterruptedException
    {
        final Semaphore sem = new Semaphore(0);
        runOnQeoThread(new Runnable() {

            @Override
            public void run()
            {
                // open qeo, but do it on the qeo thread, this should block onQeoReady
                QeoConnectionTestListener qeoConnection = createFactory(mListener2);
                try {
                    // sleep for a bit, by now qeo should be ready. (but can't call onready because blocked)
                    Thread.sleep(500);
                }
                catch (InterruptedException e) {
                    fail("Got exception: " + e);
                    failure = true;
                }
                // check that onQeoReady or onQeoError is not yet called
                assertFalse(failure);

                // now close factory.
                qeoConnection.closeFactory();

                // release qeo thread.
                sem.release();
            }
        });
        waitForData(sem);
        // wait a little
        Thread.sleep(100);

        // check that neither onQeoReady or onQeoError still gets called
        assertFalse(failure);
    }

    /**
     * Close qeo before getting onQeoReady() and validate that a proper connection can still be made.
     */
    public void testCloseBeforeReady2()
        throws Exception
    {
        QeoConnectionTestListener qeoConnection = createFactory(mListener);
        qeoConnection.closeFactory();
        Thread.sleep(500); // give it some time to be really closed
        assertFalse(failure);

        // check that service is disconnected
        assertFalse(ServiceConnection.isConnected());

        // now try that's possible to create a new proper connection
        QeoConnectionTestListener listener = createFactory(getFactoryClosedId());
        assertTrue(ServiceConnection.isConnected());

        // close again
        listener.closeFactory();
        // and verify
        assertFalse(ServiceConnection.isConnected());
    }

    private final QeoListener mListener = new QeoListener() {

        @Override
        public boolean onStartAuthentication()
        {
            return false;
        }

        @Override
        public void onQeoReady(QeoFactory qeo)
        {

        }

        @Override
        public void onQeoError(QeoException ex)
        {
            failure = true;
            fail("Did not expect onQeoError");
        }

        @Override
        public void onQeoClosed(QeoFactory qeo)
        {
            fail("Did not expect onQeoClosed");
        }

        @Override
        public void onWakeUp(String typeName)
        {
            fail("Did not expect onWakeUp");
        }

        @Override
        public void onBgnsConnectionChange(boolean connected)
        {
        }
    };

    private final QeoListener mListener2 = new QeoListener() {

        @Override
        public boolean onStartAuthentication()
        {
            return false;
        }

        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            failure = true;
            fail("Did not expect onQeoReady");
        }

        @Override
        public void onQeoError(QeoException ex)
        {
            failure = true;
            fail("Did not expect onQeoError");
        }

        @Override
        public void onQeoClosed(QeoFactory qeo)
        {
            fail("Did not expect onQeoClosed");
        }

        @Override
        public void onWakeUp(String typeName)
        {
            fail("Did not expect onWakeUp");
        }

        @Override
        public void onBgnsConnectionChange(boolean connected)
        {
        }
    };

}
