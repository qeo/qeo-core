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

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.qeo.testframework.QeoTestCase;
import org.qeo.unittesttypes.TestState;
import org.qeo.unittesttypes.TestTypes;

public class StateTest
    extends QeoTestCase
{
    private StateWriter<TestState> sw1;

    private MyStateReaderListener srl;
    private StateReader<TestState> sr1;
    private boolean readerClosed = false;
    private boolean writerClosed = false;
    private boolean failed;

    private static class MyStateReaderListener
        implements StateReaderListener
    {
        final Semaphore onUpdateSem = new Semaphore(0);

        @Override
        public void onUpdate()
        {
            onUpdateSem.release();
        }
    };

    @Override
    public void setUp()
        throws Exception
    {
        setVerbose(false);
        super.setUp(); // call as first
        println("setup");
        /* create a state reader (iterating/polling) */
        srl = new MyStateReaderListener();
        sr1 = mQeo.createStateReader(TestState.class, srl);
        assertNotNull(sr1);
        assertFalse(sr1.iterator().hasNext()); // sanity check: no data should be present
        /* create a state writer */
        sw1 = mQeo.createStateWriter(TestState.class);
        assertNotNull(sw1);
        failed = false;
    }

    @Override
    public void tearDown()
        throws Exception
    {
        println("tearDown");
        if (sr1 != null) {
            // no more data should be present anymore, it may harm other tests
            if (sr1.iterator().hasNext()) {
                System.err.println("WARNING: not all samples are properly removed: " + sr1.iterator().next());
            }
            sr1.close();
        }
        if (sw1 != null) {
            sw1.close();
        }
        super.tearDown(); // call as last
    }

    /**
     * Setup and teardown explicitely and check if everything still works
     * 
     * @throws Exception
     */
    public void testTeardownSetup()
        throws Exception
    {
        TestState ts = new TestState();
        ts.id = "12345";
        ts.name = "name";
        ts.i = 12345;
        sw1.write(ts);
        Iterator<TestState> it = sr1.iterator();
        assertTrue(it.hasNext());
        TestState ts2 = it.next();
        assertEquals(ts.id, ts2.id);
        assertFalse(it.hasNext());
        sw1.remove(ts);
        tearDown();

        setUp();

        ts = new TestState();
        ts.id = "54321";
        ts.name = "name";
        ts.i = 54321;
        sw1.write(ts);
        it = sr1.iterator();
        assertTrue(it.hasNext());
        ts2 = it.next();
        assertEquals(ts.id, ts2.id);
        assertFalse(it.hasNext());
        sw1.remove(ts);

    }

    public void testIterator()
        throws Exception
    {
        println("testIterator");
        final int runs = 10 * mScaleFactor;
        if (sr1.iterator().hasNext()) {
            fail("initial reader is not empty");
        }
        for (int i = 1; i < runs; i++) {
            final String name = "name" + i;
            final String id = "id" + i;

            sw1.write(new TestState(id, name, i));
            // first make sure that the data is available on the listener. This should ensure the data is in the
            // iterator too.
            waitForData(srl.onUpdateSem);

            Set<Integer> found = new HashSet<Integer>();
            for (final TestState state : sr1) {
                assertNotNull(state);
                assertNotNull(state.id);
                assertNotNull(state.name);
                // check if item was not yet found
                assertFalse(found.contains(state.i));
                found.add(state.i);
                println("First time read instance with id " + state.id);
            }

            // read 2nd time
            found = new HashSet<Integer>();
            for (final TestState state : sr1) {
                assertNotNull(state);
                assertNotNull(state.id);
                assertNotNull(state.name);
                // check if item was not yet found
                assertFalse(found.contains(state.i));
                found.add(state.i);
                println("Second time read instance with id " + state.id);
            }

            // check if all are found
            for (int j = 1; j <= i; j++) {
                println("Checking instance with id " + j);
                assertTrue(found.contains(j));
            }
        }

        // cleanup
        for (int i = 1; i < runs; i++) {
            final String name = "name" + i;
            final String id = "id" + i;
            sw1.remove(new TestState(id, name, i));
        }

        // no more data should be present anymore, it may harm other tests
        assertFalse(sr1.iterator().hasNext());
    }

    public void testListener()
        throws InterruptedException
    {
        // tested code that has been obsoleted
    }

    public void testLoop()
        throws IOException
    {
        println("loopTest");
        sw1.close(); // don't need the statewriter
        sw1 = null;
        // try to create quickly 100 stateReaders
        for (int i = 0; i < 100; i++) {
            sr1.close();
            sr1 = null;
            sr1 = mQeo.createStateReader(TestState.class, null);
        }
        sr1.close(); // don't need the stateReaders
        sr1 = null;
        // try to create quickly 1000 stateWriters
        for (int i = 0; i < 1000; i++) {
            sw1 = mQeo.createStateWriter(TestState.class);
            sw1.close();
            sw1 = null;
        }
    }

    public void testCloseWithoutRemove()
        throws IOException, InterruptedException
    {
        final TestState state = new TestState("id", "name", 5);
        MyStateReaderListener srl2 = null;
        StateReader<TestState> sr2 = null;

        try {
            // write something
            sw1.write(state);

            // first make sure that the data is available on the listener. This should ensure the data is in the
            // iterator too.
            waitForData(srl.onUpdateSem);

            // verify that there is 1 sample
            int count = 0;
            final Iterator<TestState> it = sr1.iterator();
            while (it.hasNext()) {
                it.next();
                count++;
            }
            assertEquals(1, count);

            // now remove writer
            sw1.close();
            sw1 = null;

            // first make sure that the data is removed on the listener. This should ensure the data is removed from the
            // iterator too.
            waitForData(srl.onUpdateSem);

            // verify that the reader is now empty
            assertFalse(sr1.iterator().hasNext());

            // create new reader
            srl2 = new MyStateReaderListener();
            sr2 = mQeo.createStateReader(TestState.class, srl2);

            // verify that the reader is now empty
            assertFalse(sr2.iterator().hasNext());

            // now create a new writer
            sw1 = mQeo.createStateWriter(TestState.class);
            final TestState state2 = new TestState("id2", "name2", 5);
            // and write a new sample
            sw1.write(state2);

            // wait until it arrives
            waitForData(srl.onUpdateSem);

            // verify that there is 1 sample for reader 1
            count = 0;
            for (final TestState t : sr1) {
                count++;
                /* create a state reader (iterating/polling) */
                // srl = new MyStateReaderListener();
                // sr1 = qeo.createStateReader(TestState.class, srl);
                // assertNotNull(sr1);
                // assertFalse(sr1.iterator().hasNext()); // sanity check: no data should be present
                assertEquals("id2", t.id);
            }
            assertEquals(1, count);

            // wait until it arrives on the second reader
            waitForData(srl2.onUpdateSem);

            // verify that there is 1 sample for reader 2
            count = 0;
            for (final TestState t : sr2) {
                count++;
                assertEquals("id2", t.id);
            }
            assertEquals(1, count);

            // now remove
            sw1.remove(state2);

            // wait until it arrives
            waitForData(srl.onUpdateSem);

            // wait until it arrives on the second reader
            waitForData(srl2.onUpdateSem);
        }
        finally {
            if (sr2 != null) {
                sr2.close();
            }
        }

    }

    /**
     * TestCase that tries to remove a sample on another writer as where it was written
     */
    public void testRemoveOnOtherWriter()
        throws IOException, InterruptedException
    {
        final TestState state1 = new TestState("id123", "name123", 123);
        StateWriter<TestState> sw2 = mQeo.createStateWriter(TestState.class);

        try {
            // write state on writer 2
            sw1.write(state1);

            // wait until it arrives
            waitForData(srl.onUpdateSem);
            assertTrue(sr1.iterator().hasNext());

            // remove same state on another writer
            sw2.remove(state1);

            // sample should not get removed. Not possible to check with
            // callback as there should be none
            Thread.sleep(100);
            assertTrue(sr1.iterator().hasNext());

            // now close writers
            sw2.close();
            sw1.close();

            // wait until it arrives
            waitForData(srl.onUpdateSem);
            // sample should be gone now
            assertFalse(sr1.iterator().hasNext());
        }
        finally {
            if (sw2 != null) {
                sw2.close();
                sw2 = null;
            }
        }
    }

    public static class TestType
    {
        @Key
        public int id;
        @Key
        public String string;

        @Override
        public String toString()
        {
            return "TestType: " + id + " -- " + string;
        }
    }

    /**
     * Test that writes states after the reader is closed. This should not trigger onData anymore
     * 
     */
    public void testStatChangeReaderOnData()
        throws Exception
    {
        WriterThread w = new WriterThread();
        final Semaphore sem = new Semaphore(0);

        println("Creating reader");
        final Semaphore dataArrived = new Semaphore(0);
        final StateChangeReader<TestType> reader =
            mQeo.createStateChangeReader(TestType.class, new DefaultStateChangeReaderListener<TestType>() {

                @Override
                public void onData(TestType t)
                {
                    dataArrived.release();
                    if (readerClosed) {
                        log("Unexpected data: " + t);
                        // check if the reader was not already closed
                        failed = true;
                    }

                }
            });
        addJunitReaderWriter(reader);

        w.start();
        // make sure the reader gets at least 1 message before closing it
        waitForData(dataArrived);
        assertFalse(failed);
        runOnQeoThread(new Runnable() {

            @Override
            public void run()
            {
                // close reader in same thread as qeo callbacks to ensure no more messages are queued.
                // note that this does not work reliably on qeo-java, on qeo-android it does.
                println("closing reader thread");
                reader.close();
                readerClosed = true;
                sem.release();
                println("Reader closed");
            }
        });
        waitForData(sem);

        // let the writer continue writing for 100ms
        Thread.sleep(100);
        writerClosed = true; // stop writer
        w.join();
        assertFalse(failed);
    }

    /**
     * Test that writes states after the reader is closed. This should not trigger onUpdate anymore
     */
    public void testStateReaderOnUpdate()
        throws Exception
    {
        final Semaphore sem = new Semaphore(0);

        readerClosed = false;
        WriterThread w = new WriterThread();

        println("Creating reader");
        final Semaphore dataArrived = new Semaphore(0);
        final StateReader<TestType> reader = mQeo.createStateReader(TestType.class, new StateReaderListener() {

            @Override
            public void onUpdate()
            {
                dataArrived.release();
                if (readerClosed) {
                    assertFalse(readerClosed); // check if the reader was not already closed
                    failed = true;
                }
            }
        });
        addJunitReaderWriter(reader);
        w.start();
        // make sure the reader gets at least 1 message before closing it
        waitForData(dataArrived);

        runOnQeoThread(new Runnable() {

            @Override
            public void run()
            {
                // close reader in same thread as qeo callbacks to ensure no more messages are queued.
                // note that this does not work reliably on qeo-java, on qeo-android it does.
                println("closing reader thread");
                reader.close();
                readerClosed = true;
                sem.release();
                println("Reader closed");
            }
        });
        waitForData(sem);

        // let the writer continue writing for 100ms
        Thread.sleep(100);
        writerClosed = true; // stop writer
        w.join();
        assertFalse(failed);
    }

    public void testStateWriterUnkeyedData()
        throws Exception
    {
        println("testStateWriterUnkeyedData");

        try {
            mQeo.createStateWriter(TestTypes.class);
            fail("Exception should have been thrown");
        }
        catch (final IllegalArgumentException e) {
            /* We expect an IllegalArgumentException here */
        }
    }

    public void testStateReaderUnkeyedData()
        throws Exception
    {
        println("testStateReaderUnkeyedData");

        try {
            mQeo.createStateReader(TestTypes.class, null);
            fail("Exception should have been thrown");
        }
        catch (final IllegalArgumentException e) {
            /* We expect an IllegalArgumentException here */
        }
    }

    public void testStateChangeReaderUnkeyedData()
        throws Exception
    {
        println("testStateReaderUnkeyedData");

        try {
            mQeo.createStateChangeReader(TestTypes.class, new DefaultStateChangeReaderListener<TestTypes>());
            fail("Exception should have been thrown");
        }
        catch (final IllegalArgumentException e) {
            /* We expect an IllegalArgumentException here */
        }
    }

    public void testWriteNullKey()
        throws Exception
    {
        TestState ts = new TestState();
        ts.id = null; // make key null
        ts.name = "abc";
        sw1.write(ts);

        waitForData(srl.onUpdateSem);
        for (TestState ts2 : sr1) {
            assertNotNull(ts2);
            assertEquals("", ts2.id);
            assertEquals(ts.name, ts2.name);
        }
        sw1.remove(ts);
    }

    /**
     * Writer that published constant change of state
     */
    class WriterThread
        extends Thread
    {
        @Override
        public void run()
        {
            StateWriter<TestType> writer = null;
            try {
                println("Creating writer");
                writer = mQeo.createStateWriter(TestType.class);

                int i = 0;
                while (!writerClosed) {
                    i++;
                    TestType t = new TestType();
                    t.id = i;
                    t.string = "string " + i;
                    writer.write(t);
                    Thread.sleep(10);
                }

            }
            catch (Exception e) {
                failed = true;
                throw new IllegalStateException("Unit test failure", e);
            }
            finally {
                if (writer != null) {
                    writer.close();
                    println("Closed writer");
                }
            }
        }
    }
}
