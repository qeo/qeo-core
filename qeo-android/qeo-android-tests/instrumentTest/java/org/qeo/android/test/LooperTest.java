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
import java.util.concurrent.TimeUnit;

import org.qeo.EventReader;
import org.qeo.EventReaderListener;
import org.qeo.EventWriter;
import org.qeo.QeoFactory;
import org.qeo.StateChangeReader;
import org.qeo.StateChangeReaderListener;
import org.qeo.android.QeoAndroid;
import org.qeo.android.QeoConnectionListener;
import org.qeo.exception.QeoException;
import org.qeo.system.DeviceInfo;
import org.qeo.testframework.QeoTestCase;

import android.os.Looper;
import android.util.Log;

/**
 * Try to create the factory and readers on different threads
 * 
 */
public class LooperTest
    extends QeoTestCase
{
    StateChangeReader<DeviceInfo> stateReader = null;
    EventReader<SimpleType> eventReader = null;
    EventWriter<SimpleType> eventWriter = null;
    private boolean failed;
    private QeoConnectionListener mQeoConnection;
    private MyQeoListener mQeoListener;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp(false); // no qeo init
        mQeoConnection = null;
        mQeoListener = null;
        failed = false;
    }

    @Override
    public void tearDown() throws Exception
    {
        if (stateReader != null) {
            stateReader.close();
        }
        if (eventReader != null) {
            eventReader.close();
        }
        if (eventWriter != null) {
            eventWriter.close();
        }
        if (mQeoConnection != null) {
            QeoAndroid.closeQeo(mQeoConnection);
        }
        if (mQeoListener != null) {
            QeoAndroid.closeQeo(mQeoListener);
        }
        mQeo = null;
        super.tearDown();
    }

    // Thread without a Looper object
    // This should throw a runtimeException
    class Thread1
        extends Thread
    {
        private boolean ok = false;

        @Override
        public void run()
        {
            try {
                mQeoConnection = new QeoConnectionListener() {

                    @Override
                    public void onQeoReady(QeoFactory qeo)
                    {
                        mQeo = qeo;
                        fail("Should not get here");
                    }

                    @Override
                    public void onQeoError(QeoException ex)
                    {
                        fail("Should not get here");
                    }
                };
                QeoAndroid.initQeo(mCtx, mQeoConnection);
            }
            catch (RuntimeException e) {
                ok = true;
                return;
            }
            fail("RuntimeException is expected");

        }
    }

    class MyQeoListener
        extends QeoConnectionListener
    {

        private Thread verifyThread;
        private Semaphore semReady;
        private Semaphore semState;
        private Semaphore semEvent;
        private Semaphore factoryReady;

        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            mQeo = qeo;
            try {
                println("QeoReady: " + Thread.currentThread());
                waitForData(factoryReady);
                println("FactoryReady");
                assertEquals(verifyThread, Thread.currentThread());
                // qeo ready, release 1 semaphore
                semReady.release();

                // test stateChangeReader. Use available deviceInfo for this
                stateReader =
                    mQeo.createStateChangeReader(DeviceInfo.class, new StateChangeReaderListener<DeviceInfo>() {

                        @Override
                        public void onData(DeviceInfo t)
                        {
                            println("state onData: " + Thread.currentThread());
                            assertEquals(verifyThread, Thread.currentThread());
                            semState.release();
                        }

                        @Override
                        public void onNoMoreData()
                        {
                            println("state onNoMoreData: " + Thread.currentThread());
                            assertEquals(verifyThread, Thread.currentThread());
                            semState.release();
                        }

                        @Override
                        public void onRemove(DeviceInfo t)
                        {
                            // Nothing done here
                        }
                    });
            }
            catch (Exception e) {
                Log.e(TAG, "Qeo problem", e);
                failed = true; // fail() from another thread will not work good
                return;
            }

            // testing event reader
            final Semaphore semWrite = new Semaphore(0);

            try {
                eventReader = mQeo.createEventReader(SimpleType.class, new EventReaderListener<SimpleType>() {

                    @Override
                    public void onData(SimpleType t)
                    {
                        semWrite.release();
                        println("event onData: " + Thread.currentThread());
                        assertEquals(verifyThread, Thread.currentThread());
                        semEvent.release();
                    }

                    @Override
                    public void onNoMoreData()
                    {
                        println("event onNoMoreData: " + Thread.currentThread());
                        assertEquals(verifyThread, Thread.currentThread());
                        semEvent.release();

                    }
                });
            }
            catch (Exception e) {
                Log.e(TAG, "Qeo problem", e);
                failed = true; // fail() from another thread will not work good
                return;
            }

            // write a sample. Don't forget to do this on another thread!
            Thread t = new Thread() {

                @Override
                public void run()
                {

                    try {
                        println("creating writer");
                        eventWriter = mQeo.createEventWriter(SimpleType.class);
                        SimpleType test = new SimpleType();
                        test.name = "junit";
                        while (true) {
                            // write until it has been received
                            println("trying to write");
                            eventWriter.write(test);
                            if (semWrite.tryAcquire(1, 100, TimeUnit.MILLISECONDS)) {
                                break;
                            }
                        }
                        println("write done");

                    }
                    catch (Exception e) {
                        Log.e(TAG, "Qeo problem", e);
                        failed = true; // fail() from another thread will not work good
                        return;
                    }
                }
            };
            t.start();

        }

        @Override
        public void onQeoError(QeoException ex)
        {
            Log.e(TAG, "Qeo error", ex);
            failed = true;
        }
    }

    private class DeviceInfoReader
        extends Thread
    {
        private final Semaphore mSem;
        private StateChangeReader<DeviceInfo> myReader;
        private Thread mVerifyThread = null;

        private DeviceInfoReader(Semaphore sem, Thread verifyThread)
        {
            this.mSem = sem;
            this.mVerifyThread = verifyThread;
        }

        @Override
        public void run()
        {
            if (mVerifyThread == null) {
                mVerifyThread = Thread.currentThread();
            }
            Looper.prepare();

            try {
                myReader = mQeo.createStateChangeReader(DeviceInfo.class, new StateChangeReaderListener<DeviceInfo>() {

                    @Override
                    public void onData(DeviceInfo t)
                    {
                        println("state onData: " + Thread.currentThread());
                        assertEquals(mVerifyThread, Thread.currentThread());
                        mSem.release();
                    }

                    @Override
                    public void onNoMoreData()
                    {
                        println("state onNoMoreData: " + Thread.currentThread());
                        assertEquals(mVerifyThread, Thread.currentThread());
                        mSem.release();
                    }

                    @Override
                    public void onRemove(DeviceInfo t)
                    {
                        // nothing done here
                    }
                });
            }
            catch (Exception e) {
                Log.e(TAG, "Qeo problem", e);
                failed = true; // fail() from another thread will not work good
            }
            Looper.loop();
        }
    }

    class Thread2
        extends Thread
    {
        private Thread currentThread;
        private final Semaphore semReady = new Semaphore(0);
        private final Semaphore semState = new Semaphore(0);
        private final Semaphore semEvent = new Semaphore(0);

        @Override
        public void run()
        {
            currentThread = Thread.currentThread();
            try {
                Looper.prepare();
                mQeoListener = new MyQeoListener();
                mQeoListener.verifyThread = this;
                mQeoListener.semEvent = semEvent;
                mQeoListener.semState = semState;
                mQeoListener.semReady = semReady;
                mQeoListener.factoryReady = new Semaphore(0);
                println("Create factory: " + currentThread);
                QeoAndroid.initQeo(mCtx, mQeoListener);
                mQeoListener.factoryReady.release();
                Looper.loop();
            }
            catch (Exception e) {
                Log.e(TAG, "Qeo problem", e);
                failed = true; // fail() from another thread will not work good
            }

        }
    }

    class Thread3
        extends Thread
    {
        private Thread currentThread;
        private final Semaphore semReady = new Semaphore(0);
        private final Semaphore sem1 = new Semaphore(0);
        private final Semaphore sem2 = new Semaphore(0);
        DeviceInfoReader r1;
        DeviceInfoReader r2;

        @Override
        public void run()
        {
            currentThread = Thread.currentThread();
            try {
                Looper.prepare();
                println("Create factory: " + currentThread);
                mQeoConnection = new QeoConnectionListener() {

                    @Override
                    public void onQeoReady(QeoFactory qeo)
                    {
                        mQeo = qeo;
                        semReady.release();
                        r1 = new DeviceInfoReader(sem1, null);
                        r2 = new DeviceInfoReader(sem2, null);
                        r1.start();
                        r2.start();

                    }

                    @Override
                    public void onQeoError(QeoException ex)
                    {
                        Log.e(TAG, "Qeo problem", ex);
                        failed = true;
                    }
                };
                QeoAndroid.initQeo(mCtx, mQeoConnection);
                Looper.loop();
            }
            catch (Exception e) {
                Log.e(TAG, "Qeo problem", e);
                failed = true; // fail() from another thread will not work good
            }
        }
    }

    /**
     * Make a thread without a looper. This should fail
     * 
     * @throws Exception Failure
     */
    public void testThread1()
        throws Exception
    {
        Thread1 t1 = new Thread1();
        t1.start();
        t1.join();
        assertEquals(true, t1.ok);
        assertFalse(failed);
    }

    /**
     * Make thread and create the factory, eventReader and StateReader in this thread.</br> All callbacks should be done
     * on that thread
     * 
     * @throws Exception failure
     */
    public void testThread2()
        throws Exception
    {
        println("Staring testThread2");
        Thread2 t2 = new Thread2();
        t2.start();
        waitForData(t2.semReady);
        waitForData(t2.semState, 2); // should get 2 releases
        waitForData(t2.semEvent, 2); // should get 2 releases
        assertFalse(failed);
    }

    /**
     * Make a different thread for factory and stateReader. Callbacks should happen on their own thread.
     * 
     * @throws Exception failure
     */
    public void testThread3()
        throws Exception
    {
        Thread3 t3 = new Thread3();
        t3.start();
        waitForData(t3.semReady);
        waitForData(t3.sem1, 2);
        waitForData(t3.sem2, 2);
        t3.r1.myReader.close();
        t3.r2.myReader.close();
        assertFalse(failed);
    }

    /**
     * Pass a custom looper. All callbacks should be done on that looper
     * 
     * @throws Exception failure
     */
    public void testLooper()
        throws Exception
    {
        mQeoListener = new MyQeoListener();

        Semaphore semReady = new Semaphore(0);
        Semaphore semState = new Semaphore(0);
        Semaphore semEvent = new Semaphore(0);
        mQeoListener.verifyThread = Looper.getMainLooper().getThread();
        mQeoListener.semReady = semReady;
        mQeoListener.semState = semState;
        mQeoListener.semEvent = semEvent;
        mQeoListener.factoryReady = new Semaphore(0);
        QeoAndroid.initQeo(mCtx, mQeoListener, Looper.getMainLooper());
        mQeoListener.factoryReady.release(); // mQeo object created
        waitForData(semReady);
        waitForData(semState, 2); // should get 2 releases
        waitForData(semEvent, 2); // should get 2 releases
        assertFalse(failed);
    }

    /**
     * Pass a custom looper and create 2 readers each in a different thread. Callbacks should still happen in looper
     * thread.
     * 
     * @throws Exception failure
     */
    public void testLooper2()
        throws Exception
    {
        final Semaphore semReady = new Semaphore(0);
        final Semaphore sem1 = new Semaphore(0);
        final Semaphore sem2 = new Semaphore(0);

        mQeoConnection = new QeoConnectionListener() {

            @Override
            public void onQeoReady(QeoFactory qeo)
            {
                mQeo = qeo;
                semReady.release();
                Thread verifyThread = Looper.getMainLooper().getThread();
                DeviceInfoReader r1 = new DeviceInfoReader(sem1, verifyThread);
                DeviceInfoReader r2 = new DeviceInfoReader(sem2, verifyThread);
                r1.start();
                r2.start();

            }

            @Override
            public void onQeoError(QeoException ex)
            {
                failed = true;
                Log.e(TAG, "Qeo error", ex);
            }
        };
        QeoAndroid.initQeo(mCtx, mQeoConnection, Looper.getMainLooper());

        waitForData(semReady);
        waitForData(sem1, 2);
        waitForData(sem2, 2);
        assertFalse(failed);
    }

}
