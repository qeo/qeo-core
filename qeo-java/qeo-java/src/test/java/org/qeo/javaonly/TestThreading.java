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

package org.qeo.javaonly;

import org.qeo.QeoFactory;
import org.qeo.exception.QeoException;
import org.qeo.java.QeoConnectionListener;
import org.qeo.java.QeoJava;
import org.qeo.jni.NativeFactory;
import org.qeo.testframework.QeoTestCase;
import org.qeo.testframework.impl.QeoTestCaseImpl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tests to use Qeo from different threads.
 */
public class TestThreading extends QeoTestCase
{
    private static final Logger LOG = Logger.getLogger("TestThreading");
    private final Semaphore mSem = new Semaphore(0);
    private final Semaphore mSemCloseDone = new Semaphore(0);
    private ExecutorService mExecutor;

    @Override
    public void setUp() throws Exception
    {
        super.setUp(false);
        mSem.drainPermits();
        mSemCloseDone.drainPermits();
        mExecutor = Executors.newCachedThreadPool();
        QeoTestCaseImpl.initNativeHost();
    }

    @Override
    public void tearDown() throws Exception
    {
        mExecutor.shutdownNow();
        super.tearDown();
    }

    /**
     * Create factories and close them in other thread. Do this in a loop. Wait for factory to be closed before creating
     * new one
     */
    public void testOpenCloseFactory() throws InterruptedException
    {
        for (int i = 0; i < 10; i++) {
            MyListener listener = new MyListener(true);
            QeoJava.initQeo(listener);
            waitForData(mSemCloseDone);
            assertEquals(0, NativeFactory.getNumFactories());
        }
    }

    /**
     * Create factories and close them in other thread. Do this in a loop. Do NOT wait for factory to be closed before
     * creating new one
     */
    public void testOpenCloseFactory2() throws InterruptedException
    {
        int num = 10;
        for (int i = 0; i < num; i++) {
            MyListener listener = new MyListener(true);
            QeoJava.initQeo(listener);
            println("Waiting for Qeo " + (i + 1) + " to be ready");
            waitForData(mSem);
        }

        println("Waiting closing to be done");
        // wait for all closes to be done
        waitForData(mSemCloseDone, num);
    }

    public void testCloseBeforeInitDone() throws InterruptedException
    {
        for (int i = 0; i < 100; i++) {
            MyListener listener = new MyListener();
            QeoJava.initQeo(listener);
            QeoJava.closeQeo(listener);
            waitForData(mSemCloseDone);
            assertEquals(0, NativeFactory.getNumFactories());
        }
    }

    private static int sListenerCount = 0;

    synchronized int getListenerId()
    {
        sListenerCount++;
        return sListenerCount;
    }

    private final class MyListener extends QeoConnectionListener
    {
        private final boolean mDoClose;
        private final int mId;

        MyListener(boolean doClose)
        {
            mId = getListenerId();
            mDoClose = doClose;
        }

        MyListener()
        {
            this(false);
        }

        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            mExecutor.submit(new Runnable()
            {

                @Override
                public void run()
                {
                    println("Listener " + mId + ": starting");
                    // release semaphore and close
                    mSem.release();
                    if (mDoClose) {
                        println("Listener " + mId + ": close qeo");
                        QeoJava.closeQeo(MyListener.this);
                    }
                    mSemCloseDone.release();
                    println("Listener " + mId + ": done");
                }
            });
        }

        @Override
        public void onQeoError(QeoException ex)
        {
            LOG.log(Level.SEVERE, "onQeoError", ex);
        }

        @Override
        public boolean onStartAuthentication()
        {
            LOG.severe("ERROR: got onStartAuthentication call, did not expect this");
            throw new IllegalStateException("not supported");
        }

    }

}
