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

package org.qeo;

import java.util.concurrent.Semaphore;

import org.qeo.testframework.QeoTestCase;
import org.qeo.testframework.TestListener;
import org.qeo.testframework.TestListener2;
import org.qeo.unittesttypes.TestState;

/**
 * Checks what happens after closing reader/writers.
 */
public class BehaviorAfterCloseTest
    extends QeoTestCase
{
    private StateWriter<TestState> sw;
    private StateChangeReader<TestState> scr;
    private StateReader<TestState> sr;
    private TestListener<TestState> listener;
    private boolean failed;
    private Semaphore mSemStopped;
    private Semaphore mSemClosed;
    private TestListener2 tl;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        sw = mQeo.createStateWriter(TestState.class);
        addJunitReaderWriter(sw);
        listener = new TestListener<TestState>();
        scr = mQeo.createStateChangeReader(TestState.class, listener);
        addJunitReaderWriter(scr);
        tl = new TestListener2();
        sr = mQeo.createStateReader(TestState.class, tl);
        addJunitReaderWriter(sr);
    }

    public void testWriteAfterClose()
        throws InterruptedException
    {
        failed = false;
        mSemClosed = new Semaphore(0);
        mSemStopped = new Semaphore(0);
        Thread t1 = new Thread() {
            @Override
            public void run()
            {
                failed = true; // assume failed
                try {
                    TestState ts = new TestState();
                    ts.id = "stressTestId";
                    ts.i = 1;
                    ts.name = "name 2";
                    println("writing sample 1");
                    sw.write(ts);

                    // wait for close
                    println("waiting for close");
                    waitForData(mSemClosed);

                    try {
                        ts = new TestState();
                        ts.id = "stressTestId2";
                        ts.i = 2;
                        ts.name = "name 3";
                        println("writing sample 2");
                        sw.write(ts);
                        log("Writer did not throw exception as expected!");
                    }
                    catch (IllegalStateException ex) {
                        println("Got expected IllegalStateException");
                        failed = false; // this is expected to crash with illegalStateException if writer is closed
                    }
                }
                catch (Exception ex) {
                    failed = true;
                }
                mSemStopped.release();
            }
        };

        t1.start();
        // wait until at least 1 sample is received
        println("wait for first sample");
        waitForData(listener.onDataSem);

        // close writer and relese semaphore
        println("Closing writer");
        sw.close();
        mSemClosed.release();

        // wait till the writer stopped/crashed
        println("waiting for writer to stop");
        waitForData(mSemStopped);
        assertFalse(failed);
    }

    public void testReadAfterClose()
        throws InterruptedException
    {
        TestState ts = new TestState();
        ts.id = "stressTestId";
        ts.i = 1;
        ts.name = "name 2";
        println("writing sample 1");
        sw.write(ts);

        waitForData(tl.onUpdateSem);

        int num = 0;
        for (TestState ts2 : sr) {
            num++;
            assertEquals(ts.id, ts2.id);
        }
        assertEquals(1, num);

        // close reader
        sr.close();
        try {
            for (TestState ts2 : sr) {
                fail("Should not get here: " + ts2);
            }
            fail("Should not get here");
        }
        catch (IllegalStateException ex) {
            // expected
        }
    }

    public void testCloseDuringIterate()
        throws InterruptedException
    {
        TestState ts = new TestState();
        ts.id = "stressTestId";
        ts.i = 1;
        ts.name = "name 2";
        println("writing sample 1");
        sw.write(ts);

        waitForData(tl.onUpdateSem);

        int num = 0;
        for (TestState ts2 : sr) {
            num++;
            assertEquals(ts.id, ts2.id);
        }
        assertEquals(1, num);

        // close reader
        failed = true;
        try {
            for (TestState ts2 : sr) {
                println("Got state: " + ts2);
                sr.close();
                failed = false; // we should get here
            }
            fail("Should not get here");
        }
        catch (IllegalStateException ex) {
            // expected
        }
        assertFalse(failed);
    }

}
