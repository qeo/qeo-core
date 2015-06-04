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

import org.qeo.Key;
import org.qeo.QeoFactory;
import org.qeo.StateReader;
import org.qeo.StateReaderListener;
import org.qeo.StateWriter;
import org.qeo.android.QeoAndroid;
import org.qeo.android.QeoConnectionListener;
import org.qeo.exception.QeoException;
import org.qeo.testframework.QeoTestCase;

import android.os.Handler;
import android.os.Looper;

/**
 * testcase to see if listener callbacks are still called after reader/qeo is closed
 */
public class CallbackAfterCloseTest
    extends QeoTestCase
{
    private Semaphore sem;
    private StateWriter<MyType> writer;
    private StateReader<MyType> reader;
    private boolean dataReceived = false;
    private boolean isClosed = false;
    private QeoConnectionListener mQeoConnection;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp(false);
        sem = new Semaphore(0);
        mQeoConnection = null;
    }

    @Override
    public void tearDown()
        throws Exception
    {
        if (reader != null) {
            reader.close();
        }
        if (writer != null) {
            writer.close();
        }
        if (mQeoConnection != null) {
            QeoAndroid.closeQeo(mQeoConnection);
        }
        super.tearDown();
    }

    // Own custom looper
    private static class LooperThread
        extends Thread
    {
        private Looper looper;
        private Semaphore semThread;

        @Override
        public void run()
        {
            Looper.prepare();
            looper = Looper.myLooper();
            semThread.release();
            Looper.loop();
        }
    }

    public void testOnUpdate()
        throws Exception
    {
        println("Staring testOnUpdate");

        // create custom looper
        LooperThread lThread = new LooperThread();
        lThread.semThread = sem;
        lThread.start(); // start looper
        waitForData(sem); // make sure looper is initialized
        Handler handler = new Handler(lThread.looper); // create handler for this looper

        // initialize qeo with the custom looper
        mQeoConnection = new QeoConnectionListener() {

            @Override
            public void onQeoReady(QeoFactory qeo)
            {
                mQeo = qeo;
                sem.release();
            }

            @Override
            public void onQeoError(QeoException ex)
            {
                fail("Qeo error: " + ex);
            }
        };
        QeoAndroid.initQeo(mCtx, mQeoConnection, lThread.looper);
        waitForFactoryInit(sem);

        // create reader and writer
        writer = mQeo.createStateWriter(MyType.class);
        reader = mQeo.createStateReader(MyType.class, new StateReaderListener() {

            @Override
            public void onUpdate()
            {
                log("onUpdate");
                dataReceived = true;

                // If data is received after the reader is closed, it should fail
                // this is the main purpose of the test
                assertFalse(isClosed);
            }
        });

        MyType t = new MyType();
        t.id = 1;
        // make sure reader and writer are connected
        int i = 0;
        while (!dataReceived) {
            i++;
            writer.write(t);
            Thread.sleep(100);
            if (i == 20) {
                fail("Reader/Writer did not get connected");
            }
        }
        println("reader/writer are connected");

        // now write something on the looper thread and close the reader
        handler.post(new Runnable() {
            @Override
            public void run()
            {
                try {
                    MyType t2 = new MyType();
                    t2.id = 1;
                    println("Writing data!");
                    writer.write(t2);

                    // give the reader some time to receive the data before closing it
                    Thread.sleep(100);
                    println("Closing reader!");
                    reader.close();
                    isClosed = true;
                    println("Handler done!");
                    sem.release();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        // wait until the writing is done
        waitForData(sem);

        // now sleep again for 100ms.
        // this should give the reader time to call onupdate.
        // in that cause a failure should happen.
        // If we would not sleep here we give the reader no time to call onUpdate
        println("checking for onupdate");
        Thread.sleep(100);
        println("done");

    }

    public static class MyType
    {
        @Key
        public int id;
    }
}
