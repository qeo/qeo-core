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

import org.qeo.exception.QeoException;
import org.qeo.testframework.QeoTestCase;
import org.qeo.testframework.TestListener;
import org.qeo.unittesttypes.TestState;

/**
 * 
 */
public class StateChangeTest
    extends QeoTestCase
{
    private StateWriter<TestState> sw1;
    private MyStateChangeListener scl;
    private StateChangeReader<TestState> scr1;

    private static class MyStateChangeListener
        extends TestListener<TestState>
    {
        public MyStateChangeListener(String name)
        {
            super(name);
        }

        public MyStateChangeListener()
        {
            super();
        }

        public void checkExpected(String id, String nameval, int i)
        {
            assertNotNull(lastReceivedItem);
            assertEquals(id, lastReceivedItem.id);
            assertEquals(nameval, lastReceivedItem.name);
            assertEquals(i, lastReceivedItem.i);
        }

        public void checkExpected(String id)
        {
            assertNotNull(lastReceivedItem);
            assertEquals(id, lastReceivedItem.id);
        }
    }

    @Override
    public void setUp()
        throws Exception
    {
        setVerbose(false);
        super.setUp(); // call as first
        println("setup");
        /* create a state writer */
        sw1 = mQeo.createStateWriter(TestState.class);
        assertNotNull(sw1);
    }

    @Override
    public void tearDown()
        throws Exception
    {
        println("tearDown");
        if (sw1 != null) {
            sw1.close();
        }
        super.tearDown(); // call as last
    }

    public void testStates()
        throws QeoException, InterruptedException
    {
        println("testStates");

        final MyStateChangeListener sl = new MyStateChangeListener();
        scr1 = mQeo.createStateChangeReader(TestState.class, sl);

        for (int i = 0; i < 100; i++) {
            final String name = "name" + i;
            final String id = "id" + i;
            // println("=== pub remove it" + i + " with name " + name);

            sw1.write(new TestState(id, name, i));
            waitForData(sl.onDataSem);
            waitForData(sl.onNoMoreDataSem);
            sl.checkExpected(id, name, i);
            sw1.remove(new TestState(id, "rubish", 0));
            waitForData(sl.onRemoveSem);
            waitForData(sl.onNoMoreDataSem);
            sl.checkExpected(id);
        }
        scr1.close();
    }

    public void testWriteBeforeCreateReader()
        throws IOException, InterruptedException
    {
        println("testWriteBeforeCreateReader");
        final int loopCount = 10;

        try {
            // first write loopcount samples
            for (int i = 0; i < loopCount; i++) {
                sw1.write(new TestState("id" + i, "name" + i, i));
            }

            // now create the state change reader
            // this is created and attached after the writes are done, but
            // should receive all samples too
            scl = new MyStateChangeListener();
            scl.setKeepSamples(true);
            scr1 = mQeo.createStateChangeReader(TestState.class, scl);

            // this one should get all data via onData
            waitForData(scl.onDataSem, loopCount);
            waitForData(scl.onNoMoreDataSem);
            assertEquals(loopCount, scl.getNumReceived());
            for (int i = 0; i < loopCount; i++) {
                assertEquals("id" + i, scl.getReceivedStates().get(i).id);
            }
        }
        finally {
            // always cleanup
            for (int i = 0; i < loopCount; i++) {
                sw1.remove(new TestState("id" + i, "name" + i, i));
            }
            waitForData(scl.onRemoveSem, loopCount);
            scr1.close();
        }

    }

    public void testCreateReaderBeforeWriting()
        throws QeoException, InterruptedException
    {
        println("testCreateReaderBeforeWriting");
        final int loopCount = 10;

        final Thread publisher = new Thread(new Runnable() {
            @Override
            public void run()
            {
                for (int i = 0; i < loopCount; i++) {
                    sw1.write(new TestState("id" + i, "name" + i, i));
                }
                return;
            }
        });

        try {
            // First create the state change reader
            scl = new MyStateChangeListener();
            scl.setKeepSamples(true);
            scr1 = mQeo.createStateChangeReader(TestState.class, scl);
            // Now start writing
            publisher.start();
            // And wait until all is received
            waitForData(scl.onDataSem, loopCount);
            waitForData(scl.onNoMoreDataSem);
            assertEquals(loopCount, scl.getNumReceived());
            for (int i = 0; i < loopCount; i++) {
                assertEquals("id" + i, scl.getReceivedStates().get(i).id);
            }

            // Repeat for remove
            for (int i = 0; i < loopCount; i++) {
                sw1.remove(new TestState("id" + i, "name" + i, i));
            }
            waitForData(scl.onRemoveSem, loopCount);
        }
        finally {
            scr1.close();
        }
    }

    public void testCloseWithoutRemove()
        throws IOException, InterruptedException
    {
        final TestState state = new TestState("id", "name", 5);
        StateChangeReader<TestState> scr2 = null;

        try {
            final MyStateChangeListener listener1 = new MyStateChangeListener("l1");
            scr1 = mQeo.createStateChangeReader(TestState.class, listener1);

            sw1.write(state);

            // wait until it arrives
            waitForData(listener1.onDataSem);
            waitForData(listener1.onNoMoreDataSem);
            listener1.checkExpected(state.id);

            // verify that there is 1 sample
            assertEquals(1, listener1.getNumReceived());
            listener1.checkExpected("id", "name", 5);

            // now remove writer
            sw1.close();
            sw1 = null;

            waitForData(listener1.onRemoveSem);
            waitForData(listener1.onNoMoreDataSem);

            // create new reader
            final MyStateChangeListener listener2 = new MyStateChangeListener("l2");
            scr2 = mQeo.createStateChangeReader(TestState.class, listener2);

            // verify that there is no sample
            assertEquals(0, listener2.getNumReceived());

            // now create a new writer
            sw1 = mQeo.createStateWriter(TestState.class);
            final TestState state2 = new TestState("id2", "name2", 5);
            sw1.write(state2);

            // wait until it arrives
            waitForData(listener1.onDataSem);
            waitForData(listener1.onNoMoreDataSem);
            listener1.checkExpected(state2.id);
            waitForData(listener2.onDataSem);
            waitForData(listener2.onNoMoreDataSem);
            listener2.checkExpected(state2.id);

            // verify that there is 1 sample for reader 1
            listener1.checkExpected("id2", "name2", 5);
            // verify that there is 1 sample for reader 2
            listener2.checkExpected("id2", "name2", 5);

            // now remove
            sw1.remove(state2);

            // wait until it arrives
            waitForData(listener1.onRemoveSem);
            waitForData(listener1.onNoMoreDataSem);
            waitForData(listener2.onRemoveSem);
            waitForData(listener2.onNoMoreDataSem);

        }
        finally {
            if (scr1 != null) {
                scr1.close();
            }
            if (scr2 != null) {
                scr2.close();
            }
        }

    }

}
