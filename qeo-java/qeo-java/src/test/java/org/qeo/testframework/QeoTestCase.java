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

package org.qeo.testframework;

import org.qeo.ReaderWriter;
import org.qeo.testframework.impl.QeoTestCaseImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public abstract class QeoTestCase
    extends QeoTestCaseImpl
{
    private static boolean verbose = false;
    protected int timeout = 10; // default 10 seconds
    private List<ReaderWriter<?>> readerWriters;

    @Override
    public void setUp(boolean initQeo)
        throws Exception
    {
        startWatchDog();
        super.setUp(initQeo);
        initReaderWriters();
    }

    @Override
    public void setUp()
        throws Exception
    {
        startWatchDog();
        super.setUp();
        initReaderWriters();
    }

    @Override
    public void tearDown()
        throws Exception
    {
        closeReaderWriters();
        stopWatchDog();
        super.tearDown();
    }

    protected void tearDown(boolean stopWatchDog) throws Exception
    {
        closeReaderWriters();
        if (stopWatchDog) {
            stopWatchDog();
        }
        super.tearDown();
    }

    private synchronized void initReaderWriters()
    {
        readerWriters = new ArrayList<ReaderWriter<?>>();
    }

    protected synchronized void closeReaderWriters()
    {
        for (ReaderWriter<?> rw : readerWriters) {
            rw.close();
        }
        readerWriters.clear();
    }

    /**
     * Adds a reader/writer to this junit testcase. All readers/writers added here will be closed at the end of the
     * test.
     * 
     * @param rw The reader/writer to be added.
     */
    public synchronized void addJunitReaderWriter(ReaderWriter<?> rw)
    {
        readerWriters.add(rw);
    }

    protected void waitForData(Semaphore sem, int samples, boolean arrive)
        throws InterruptedException
    {
        boolean semUnlocked = sem.tryAcquire(samples, timeout, TimeUnit.SECONDS);
        if (arrive) {
            if (!semUnlocked) {
                fail("Semaphore lock timeout");
            }
        }
        else {
            if (semUnlocked) {
                fail("Semaphore unlocked");
            }
        }
    }

    protected void waitForData(Semaphore sem, int samples)
        throws InterruptedException
    {
        waitForData(sem, samples, true);
    }

    protected void waitForData(Semaphore sem)
        throws InterruptedException
    {
        waitForData(sem, 1, true);
    }

    public static void println(String msg)
    {
        if (verbose) {
            log(msg);
        }
    }

    public static synchronized void setVerbose(boolean v)
    {
        verbose = v;
    }

    private Thread watchDog = null;

    private void startWatchDog() throws InterruptedException
    {
        if (watchDog != null) {
            stopWatchDog();
        }
        //create exception already here, but don't throw it.
        //this will make the stracktrace of the exception the place where the timer was actually started
        //this provides more information.
        final IllegalStateException ex = new IllegalStateException("TIMEOUT");
        watchDog = new Thread() {
            @Override
            public void run()
            {
                try {
                    //just sleep for 60 seconds
                    Thread.sleep(60000); //60 seconds
                }
                catch (InterruptedException e) {
                    //This is expected, the watchdog should be interrupted before 60 seconds sleep.
                    return;
                }
                //if we get here, we slept for 60 seconds, this is bad. Abort!
                throw ex;
            }
        };
        watchDog.start();
    }

    private void stopWatchDog() throws InterruptedException
    {
        watchDog.interrupt();
        watchDog.join();
        watchDog = null;
    }

}
