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

package org.qeo;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.Semaphore;

import org.qeo.testframework.QeoTestCase;
import org.qeo.testframework.TestListener;

/**
 * 
 */
public class BigDataTest
    extends QeoTestCase
{
    private static final int SIZE_100 = 100 * 1024; // 100 KB
    private static final int SIZE_300 = 300 * 1024; // 300 KB
    private static final int SIZE_900 = 900 * 1024; // 900 KB (Android MAX transaction size = 1 MB)
    private static final int SIZE_1500 = 1500 * 1024; // 1500 KB
    private final Random random = new Random();

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

    }

    @Override
    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    /**
     * Write 100 kb state.
     */
    public void testState100()
        throws Exception
    {
        state(SIZE_100);
    }

    /**
     * Write 300 kb state.
     */
    public void testState300()
        throws Exception
    {
        state(SIZE_300);
    }

    /**
     * Write 900 kb state.
     */
    public void testState900()
        throws Exception
    {
        state(SIZE_900);
    }

    /**
     * Write 1.5 mb state.
     */
    public void testState1500()
        throws Exception
    {
        state(SIZE_1500);
    }

    /**
     * Write 100 kb event.
     */
    public void testEvent100()
        throws Exception
    {
        event(SIZE_100);
    }

    /**
     * Write 300 kb event.
     */
    public void testEvent300()
        throws Exception
    {
        event(SIZE_300);
    }

    /**
     * Write 900 kb event.
     */
    public void testEvent900()
        throws Exception
    {
        event(SIZE_900);
    }

    /**
     * Write 1.5 mb event.
     */
    public void testEvent1500()
        throws Exception
    {
        event(SIZE_1500);
    }

    private void checkState(BigDataState excepted, BigDataState actual)
    {
        assertNotNull(actual);
        assertEquals(excepted.id, actual.id);
        assertTrue(Arrays.equals(excepted.bytes, actual.bytes));
    }

    private void state(int size)
        throws Exception
    {
        // create readers/writers
        StateWriter<BigDataState> sw1 = mQeo.createStateWriter(BigDataState.class);
        addJunitReaderWriter(sw1);
        TestListener<BigDataState> listener = new TestListener<BigDataState>();
        StateChangeReader<BigDataState> sr1 = mQeo.createStateChangeReader(BigDataState.class, listener);
        addJunitReaderWriter(sr1);
        MyStateListener listener2 = new MyStateListener();
        StateReader<BigDataState> sr2 = mQeo.createStateReader(BigDataState.class, listener2);
        addJunitReaderWriter(sr2);

        // create oject
        BigDataState bds1 = getBigDataState(random.nextInt(), size);

        // write
        sw1.write(bds1);

        // state change reader
        waitForData(listener.onDataSem);
        BigDataState bds2 = listener.getLastReceived();
        checkState(bds1, bds2);
        listener.reset(); // free memory

        // state reader
        waitForData(listener2.onUpdateSem);
        int count = 0;
        Iterator<BigDataState> it = sr2.iterator();
        while (it.hasNext()) {
            count++;
            bds2 = it.next();
            assertEquals(1, count);
            checkState(bds1, bds2);
        }
        assertEquals(1, count);

        // remove call
        sw1.remove(bds1);

        // state change reader
        waitForData(listener.onRemoveSem);
        bds2 = listener.getLastRemoved();
        checkState(bds1, bds2);
        listener.reset(); // free memory

        // state reader, no way to figure out here which exactly is removed
        waitForData(listener2.onUpdateSem);
        it = sr2.iterator();
        assertFalse(it.hasNext());

    }

    private void event(int size)
        throws Exception
    {
        // create readers/writers
        EventWriter<BigDataEvent> ew1 = mQeo.createEventWriter(BigDataEvent.class);
        addJunitReaderWriter(ew1);
        TestListener<BigDataEvent> listener = new TestListener<BigDataEvent>();
        EventReader<BigDataEvent> er1 = mQeo.createEventReader(BigDataEvent.class, listener);
        addJunitReaderWriter(er1);

        // create oject
        BigDataEvent bde1 = getBigDataEvent(size);

        // write
        ew1.write(bde1);
        waitForData(listener.onDataSem);
        BigDataEvent bde2 = listener.getLastReceived();
        assertNotNull(bde2);
        assertEquals(bde1.id, bde2.id);
        assertTrue(Arrays.equals(bde1.bytes, bde2.bytes));
        listener.reset();

    }

    private BigDataState getBigDataState(int id, int size)
    {
        BigDataState bigDataState = new BigDataState();
        bigDataState.id = id;
        byte[] myBytes = new byte[size];
        random.nextBytes(myBytes);
        bigDataState.bytes = myBytes;
        return bigDataState;
    }

    private BigDataEvent getBigDataEvent(int size)
    {
        BigDataEvent bigDataEvent = new BigDataEvent();
        bigDataEvent.id = random.nextInt();
        byte[] myBytes = new byte[size];
        random.nextBytes(myBytes);
        bigDataEvent.bytes = myBytes;
        return bigDataEvent;
    }

    // The following test creates a lot of writers that write in parallel.
    // This is problematic on android since that causes TransactionTooLargeException.
    // That again can easy be solved with a static lock but will have performance impact.
    // It's considered a not so normal usecase so not not enabled
    // public void testParallelWrite()
    // throws Exception
    // {
    // int numWriters = 15;
    // List<MyWriter> writers = new ArrayList<MyWriter>();
    // for (int i = 0; i < numWriters; i++) {
    // // create all writers
    // writers.add(new MyWriter(i));
    // }
    //
    // // fire them!
    // for (MyWriter writer : writers) {
    // writer.start();
    // }
    //
    // // wait for completion
    // for (MyWriter writer : writers) {
    // writer.join();
    // }
    // }
    //
    //
    // private class MyWriter
    // extends Thread
    // {
    // private final BigDataState bigDataState;
    // private final StateWriter<BigDataState> w;
    //
    // public MyWriter(int id)
    // throws QeoException
    // {
    // bigDataState = getBigDataState(id, SIZE_300);
    // w = mQeo.createStateWriter(BigDataState.class);
    // addJunitReaderWriter(w);
    // }
    //
    // @Override
    // public void run()
    // {
    // w.write(bigDataState);
    // }
    // }

    public static class BigDataState
    {
        @Key
        public int id;
        @Key
        public byte[] bytes;
    }

    public static class BigDataEvent
    {
        public int id;
        public byte[] bytes;
    }

    private static class MyStateListener
        implements StateReaderListener
    {
        final Semaphore onUpdateSem = new Semaphore(0);

        @Override
        public void onUpdate()
        {
            onUpdateSem.release();
        }
    }
}
