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

package org.qeo.javaonly;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.qeo.EventReader;
import org.qeo.EventReaderListener;
import org.qeo.EventWriter;
import org.qeo.QeoFactory;
import org.qeo.exception.QeoException;
import org.qeo.java.QeoConnectionListener;
import org.qeo.java.QeoJava;
import org.qeo.jni.NativeQeo;

/**
 * Test that uses a certificate that is not yet valid. This validates behavior that qeo should not check startdate of
 * certificates to avoid clock issues.
 */
public class TestCertificateNotValideYet
    extends TestCase
{
    private QeoFactory mQeo;
    private Semaphore qeoReadySem;
    private QeoReadyListener mListener;
    private EventReader<Event1> mReader;
    private EventWriter<Event1> mWriter;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        qeoReadySem = new Semaphore(0);
        File dotQeo = new File("src/test/data/certStartdate2050/");
        if (dotQeo.exists()) {
            NativeQeo.setStoragePath(dotQeo.getAbsolutePath() + "/");
        }
        else {
            fail("Cannot find " + dotQeo);
        }
        NativeQeo.setQeoParameter("FWD_DISABLE_LOCATION_SERVICE", "1");
        mListener = new QeoReadyListener();
        QeoJava.initQeo(mListener);
        waitForData(qeoReadySem);

    }

    @Override
    protected void tearDown()
        throws Exception
    {
        if (mReader != null) {
            mReader.close();
        }
        if (mWriter != null) {
            mWriter.close();
        }
        QeoJava.closeQeo(mListener);
        mQeo = null;
        super.tearDown();
    }

    private void waitForData(Semaphore sem)
        throws InterruptedException
    {
        if (!sem.tryAcquire(10, TimeUnit.SECONDS)) {
            fail("Semaphore timeout");
        }
    }

    public void testComunication()
        throws IOException, InterruptedException
    {
        final Event1ReaderListener el1 = new Event1ReaderListener();
        assertNotNull(el1);

        // Create
        mReader = mQeo.createEventReader(Event1.class, el1);
        mWriter = mQeo.createEventWriter(Event1.class);

        Event1 t = new Event1();
        t.id = 123;
        t.name = "HelloWorld";
        // write
        mWriter.write(t);

        waitForData(el1.onEventSem);

        // verify message arrived
        assertEquals(t.id, el1.mScanRequest.id);
        assertEquals(t.name, el1.mScanRequest.name);
    }

    private static class Event1ReaderListener
        implements EventReaderListener<Event1>
    {
        private final Semaphore onEventSem = new Semaphore(0);
        private Event1 mScanRequest = null;

        @Override
        public void onData(Event1 t)
        {
            mScanRequest = t;
            onEventSem.release();
        }

        @Override
        public void onNoMoreData()
        {
        }
    };

    private class QeoReadyListener
        extends QeoConnectionListener
    {

        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            mQeo = qeo;
            qeoReadySem.release();
        }

        @Override
        public void onQeoClosed(QeoFactory qeo)
        {
            mReader = null;
            mWriter = null;
            mQeo = null;
        }

        @Override
        public void onQeoError(QeoException ex)
        {
        }

    }

    public static class Event1
    {
        public int id;
        public String name;

        public Event1()
        {
            id = 1;
            name = "default";
        }
    }

}
