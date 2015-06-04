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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.qeo.testframework.QeoTestCase;
import org.qeo.testframework.TestListener;
import org.qeo.unittesttypes.TestArrayTypes;
import org.qeo.unittesttypes.TestEnum;
import org.qeo.unittesttypes.TestState;
import org.qeo.unittesttypes.TestTypes;

public class EventTest
    extends QeoTestCase
{
    private EventWriter<TestTypes> ew1;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp(); // call as first
        // verbose = true; //uncomment this for verbose printing
        println("setup");
        ew1 = mQeo.createEventWriter(TestTypes.class);
        assertNotNull(ew1);
    }

    @Override
    public void tearDown()
        throws Exception
    {
        println("tearDown");
        if (ew1 != null) {
            ew1.close();
        }
        super.tearDown(); // call as last
    }

    private static class EventListener
        extends TestListener<TestTypes>
    {

        // CHECKSTYLE.OFF: ParameterNumber - need more than 7 to test all types
        public void checkExpected(long l, int i, short s, String str, byte b, float f, boolean ib, TestEnum ie)
        // CHECKSTYLE.ON: ParameterNumber
        {
            assertEquals(l, lastReceivedItem.longtype);
            assertEquals(i, lastReceivedItem.inttype);
            assertEquals(str, lastReceivedItem.stringtype);
            assertEquals(b, lastReceivedItem.bytetype);
            assertEquals(f, lastReceivedItem.floattype, 0.0001);
            assertEquals(ib, lastReceivedItem.booleantype);
            assertEquals(ie, lastReceivedItem.enumtype);
        }

        // CHECKSTYLE.OFF: ParameterNumber - need more than 7 to test all types
        public void checkArraysExpected(long[] la, int[] ia, short[] sa, String[] stra, byte[] ba, float[] fa,
            boolean[] boa, TestEnum[] ea)
        // CHECKSTYLE.ON: ParameterNumber
        {
            assertTrue(Arrays.equals(la, lastReceivedItem.longarraytype));
            assertTrue(Arrays.equals(ia, lastReceivedItem.intarraytype));
            assertTrue(Arrays.equals(sa, lastReceivedItem.shortarraytype));
            assertTrue(Arrays.equals(stra, lastReceivedItem.stringarraytype));
            assertTrue(Arrays.equals(ba, lastReceivedItem.bytearraytype));
            assertTrue(Arrays.equals(fa, lastReceivedItem.floatarraytype));
            assertTrue(Arrays.equals(boa, lastReceivedItem.booleanarraytype));
            assertTrue(Arrays.equals(ea, lastReceivedItem.enumarraytype));
        }
    }

    private static class EventArrayListener
        extends TestListener<TestArrayTypes>
    {

        // CHECKSTYLE.OFF: ParameterNumber - need more than 7 to test all types
        public void checkExpected(long[][][] la, int[][][] ia, short[][][] sa, String[][][] stra, byte[][][] ba,
            float[][][] fa, boolean[][][] boa, TestEnum[][][] ea)
        // CHECKSTYLE.ON: ParameterNumber
        {
            for (int i = 0; i < la.length; i++) {
                for (int j = 0; j < la[i].length; j++) {
                    assertTrue(Arrays.equals(la[i][j], lastReceivedItem.longarray3dtype[i][j]));
                    assertTrue(Arrays.equals(ia[i][j], lastReceivedItem.intarray3dtype[i][j]));
                    assertTrue(Arrays.equals(sa[i][j], lastReceivedItem.shortarray3dtype[i][j]));
                    assertTrue(Arrays.equals(stra[i][j], lastReceivedItem.stringarray3dtype[i][j]));
                    assertTrue(Arrays.equals(ba[i][j], lastReceivedItem.bytearray3dtype[i][j]));
                    assertTrue(Arrays.equals(fa[i][j], lastReceivedItem.floatarray3dtype[i][j]));
                    assertTrue(Arrays.equals(boa[i][j], lastReceivedItem.booleanarray3dtype[i][j]));
                    assertTrue(Arrays.equals(ea[i][j], lastReceivedItem.enumarray3dtype[i][j]));
                }
            }
        }
    }

    public void testClose()
        throws IOException
    {
        println("testClose");
        EventWriter<TestTypes> ew2 = null;
        try {
            ew2 = mQeo.createEventWriter(TestTypes.class);
            ew2.close();
            ew2.close(); // 2nd close, should do no harm
            ew2 = null;
        }
        finally {
            if (ew2 != null) {
                ew2.close();
                ew2 = null;
            }
        }
    }

    public void testLoop()
        throws Exception
    {
        println("testLoop");
        EventReader<TestTypes> er1 = null;
        ew1.close(); // don't need the eventWriter
        ew1 = null;
        // try to create quickly 1000 stateReaders
        for (int i = 0; i < 100 * mScaleFactor; i++) {
            er1 = mQeo.createEventReader(TestTypes.class, null);
            er1.close();
            er1 = null;
        }
        // try to create quickly 1000 stateWriters
        for (int i = 0; i < 100 * mScaleFactor; i++) {
            ew1 = mQeo.createEventWriter(TestTypes.class);
            ew1.close();
            ew1 = null;
        }
    }

    public void testReregister()
        throws IOException
    {
        println("testReregister");
        EventWriter<TestState> ew2 = null;
        EventWriter<TestTypes> ew3 = null;
        try {
            ew3 = mQeo.createEventWriter(TestTypes.class);
            assertNotNull(ew3);
            ew3.close();
            ew3 = null;

            ew3 = mQeo.createEventWriter(TestTypes.class);
            assertNotNull(ew3);
            ew3.close();
            ew3 = null;

            ew3 = mQeo.createEventWriter(TestTypes.class);
            assertNotNull(ew3);
        }
        finally {
            if (ew2 != null) {
                ew2.close();
                ew2 = null;
            }
            if (ew3 != null) {
                ew3.close();
                ew3 = null;
            }
        }

    }

    public void testTypes()
        throws IOException, InterruptedException
    {
        println("testTypes");
        EventReader<TestTypes> er = null;
        EventWriter<TestTypes> ew = null;

        try {
            final EventListener el = new EventListener();
            assertNotNull(el);
            er = mQeo.createEventReader(TestTypes.class, el);
            assertNotNull(er);
            ew = mQeo.createEventWriter(TestTypes.class);
            assertNotNull(ew);
            TestTypes testType =
                new TestTypes(99, 99, (short) 99, "str" + 99, (byte) (99), 1 / (99), true, TestEnum.ENUM_FIRST);
            testType.setArrays(new long[] {99}, new int[] {99}, new short[] {99}, new String[] {"str" + 99},
                new byte[] {99}, new float[] {99}, new boolean[] {true}, new TestEnum[] {TestEnum.ENUM_FIRST});

            ew.write(testType);
            waitForData(el.onDataSem);
            waitForData(el.onNoMoreDataSem);
            el.checkExpected(99, 99, (short) 99, "str" + 99, (byte) (99), 1 / (99), true, TestEnum.ENUM_FIRST);
            el.checkArraysExpected(new long[] {99}, new int[] {99}, new short[] {99}, new String[] {"str" + 99},
                new byte[] {99}, new float[] {99}, new boolean[] {true}, new TestEnum[] {TestEnum.ENUM_FIRST});
            for (int i = 0; i < 10; i++) {
                final String str = "str" + i;
                println("=== pub testtypes " + i);
                final long[] la = new long[i + 1];
                final int[] ia = new int[i + 1];
                final short[] sa = new short[i + 1];
                final String[] stra = new String[i + 1];
                final byte[] ba = new byte[i + 1];
                final float[] fa = new float[i + 1];
                final boolean[] boa = new boolean[i + 1];
                final TestEnum[] ea = new TestEnum[i + 1];
                for (int j = 0; j < i + 1; j++) {
                    la[j] = j;
                    ia[j] = j;
                    sa[j] = (short) j;
                    stra[j] = "str" + j;
                    ba[j] = (byte) j;
                    fa[j] = j;
                    boa[j] = ((j % 2) == 0 ? true : false);
                    ea[j] = TestEnum.values()[j % TestEnum.values().length];
                }
                TestTypes tt =
                    new TestTypes(i, i + 1, (short) (i - 1), str, (byte) (i + 2), (float) 1.0 / (i + 1), true,
                        TestEnum.ENUM_SECOND);
                tt.setArrays(la, ia, sa, stra, ba, fa, boa, ea);
                ew.write(tt);
                waitForData(el.onDataSem);
                waitForData(el.onNoMoreDataSem);
                el.checkExpected(i, i + 1, (short) (i - 1), str, (byte) (i + 2), (float) 1.0 / (i + 1), true,
                    TestEnum.ENUM_SECOND);
            }
        }
        finally {
            if (er != null) {
                er.close();
            }
            if (ew != null) {
                ew.close();
            }
        }
    }

    public void testArrayTypes()
        throws IOException, InterruptedException
    {
        println("testArrayTypes");
        EventReader<TestArrayTypes> er = null;
        EventWriter<TestArrayTypes> ew = null;

        try {
            final EventArrayListener el = new EventArrayListener();
            assertNotNull(el);
            er = mQeo.createEventReader(TestArrayTypes.class, el);
            assertNotNull(er);
            ew = mQeo.createEventWriter(TestArrayTypes.class);
            assertNotNull(ew);
            final long[][][] la3d = new long[][][] {{{99}}};
            final int[][][] ia3d = new int[][][] {{{99}}};
            final short[][][] sa3d = new short[][][] {{{99}}};
            final String[][][] stra3d = new String[][][] {{{"str99"}}};
            final byte[][][] ba3d = new byte[][][] {{{99}}};
            final float[][][] fa3d = new float[][][] {{{99}}};
            final boolean[][][] boa3d = new boolean[][][] {{{true}}};
            final TestEnum[][][] ea3d = new TestEnum[][][] {{{TestEnum.ENUM_SECOND}}};
            TestArrayTypes testArrayType = new TestArrayTypes(la3d, ia3d, sa3d, stra3d, ba3d, fa3d, boa3d, ea3d);

            ew.write(testArrayType);
            waitForData(el.onDataSem);
            waitForData(el.onNoMoreDataSem);
            el.checkExpected(la3d, ia3d, sa3d, stra3d, ba3d, fa3d, boa3d, ea3d);

            for (int i = 0; i < 10; i++) {
                println("=== pub testtypes " + i);
                final long[][][] la3d2 = new long[i + 1][i + 1][i + 1];
                final int[][][] ia3d2 = new int[i + 1][i + 1][i + 1];
                final short[][][] sa3d2 = new short[i + 1][i + 1][i + 1];
                final String[][][] stra3d2 = new String[i + 1][i + 1][i + 1];
                final byte[][][] ba3d2 = new byte[i + 1][i + 1][i + 1];
                final float[][][] fa3d2 = new float[i + 1][i + 1][i + 1];
                final boolean[][][] boa3d2 = new boolean[i + 1][i + 1][i + 1];
                final TestEnum[][][] ea3d2 = new TestEnum[i + 1][i + 1][i + 1];
                for (int j = 0; j < i + 1; j++) {
                    for (int k = 0; k < i + 1; k++) {
                        for (int l = 0; l < i + 1; l++) {
                            la3d2[j][k][l] = j;
                            ia3d2[j][k][l] = j;
                            sa3d2[j][k][l] = (short) j;
                            stra3d2[j][k][l] = "str" + j;
                            ba3d2[j][k][l] = (byte) j;
                            fa3d2[j][k][l] = j;
                            boa3d2[j][k][l] = ((j % 2) == 0 ? true : false);
                            ea3d2[j][k][l] = TestEnum.values()[j % TestEnum.values().length];
                        }
                    }
                }
                TestArrayTypes tt = new TestArrayTypes(la3d2, ia3d2, sa3d2, stra3d2, ba3d2, fa3d2, boa3d2, ea3d2);
                ew.write(tt);
                waitForData(el.onDataSem);
                waitForData(el.onNoMoreDataSem);
                el.checkExpected(la3d2, ia3d2, sa3d2, stra3d2, ba3d2, fa3d2, boa3d2, ea3d2);
            }
        }
        finally {
            if (er != null) {
                er.close();
            }
            if (ew != null) {
                ew.close();
            }
        }
    }

    public void testNullTypes()
        throws Exception
    {
        println("testNullTypes");
        final TestArrayTypes t = new TestArrayTypes();
        final TestTypes tt = new TestTypes();
        EventReader<TestArrayTypes> er = null;
        EventWriter<TestArrayTypes> ew = null;
        EventReader<TestTypes> er1 = null;

        try {
            final EventArrayListener el = new EventArrayListener();
            assertNotNull(el);
            er = mQeo.createEventReader(TestArrayTypes.class, el);
            assertNotNull(er);
            ew = mQeo.createEventWriter(TestArrayTypes.class);
            assertNotNull(ew);
            final EventListener el1 = new EventListener();
            assertNotNull(el1);
            er1 = mQeo.createEventReader(TestTypes.class, el1);
            assertNotNull(er1);

            // empty array
            tt.longarraytype = new long[] {};
            tt.intarraytype = new int[] {};
            tt.shortarraytype = new short[] {};
            tt.stringarraytype = new String[] {};
            tt.bytearraytype = new byte[] {};
            tt.floatarraytype = new float[] {};
            tt.booleanarraytype = new boolean[] {};
            ew1.write(tt);
            waitForData(el1.onDataSem);
            el1.checkArraysExpected(new long[] {}, new int[] {}, new short[] {}, new String[] {}, new byte[] {},
                new float[] {}, new boolean[] {}, new TestEnum[] {});
            t.longarray3dtype = new long[][][] {};
            t.intarray3dtype = new int[][][] {};
            t.shortarray3dtype = new short[][][] {};
            t.stringarray3dtype = new String[][][] {};
            t.bytearray3dtype = new byte[][][] {};
            t.floatarray3dtype = new float[][][] {};
            t.booleanarray3dtype = new boolean[][][] {};
            ew.write(t);
            waitForData(el.onDataSem);
            el.checkExpected(t.longarray3dtype, t.intarray3dtype, t.shortarray3dtype, null, t.bytearray3dtype,
                t.floatarray3dtype, t.booleanarray3dtype, t.enumarray3dtype);

            // null arrays
            // Remark: the null array will be translated into an empty array. This means upon reception, we
            // will get an empty array instead of null
            tt.longarraytype = null;
            tt.intarraytype = null;
            tt.shortarraytype = null;
            tt.stringarraytype = null;
            tt.bytearraytype = null;
            tt.floatarraytype = null;
            tt.booleanarraytype = null;
            ew1.write(tt);
            waitForData(el1.onDataSem);
            el1.checkArraysExpected(new long[] {}, new int[] {}, new short[] {}, new String[] {}, new byte[] {},
                new float[] {}, new boolean[] {}, new TestEnum[] {});
            t.longarray3dtype = null;
            t.intarray3dtype = null;
            t.shortarray3dtype = null;
            t.stringarray3dtype = null;
            t.bytearray3dtype = null;
            t.floatarray3dtype = null;
            t.booleanarray3dtype = null;
            ew.write(t);
            waitForData(el.onDataSem);
            el.checkExpected(new long[][][] {}, new int[][][] {}, new short[][][] {}, new String[][][] {},
                new byte[][][] {}, new float[][][] {}, new boolean[][][] {}, new TestEnum[][][] {});

            // null string
            // Remark: the null string will be translated into an empty string. This means upon reception, we
            // will get an empty string instead of null
            TestTypes types = new TestTypes(0, 0, (short) 0, null, (byte) 0, 0, false, TestEnum.ENUM_ZERO);
            ew1.write(types);
            waitForData(el1.onDataSem);
            assertEquals("", el1.getLastReceived().stringtype);
        }
        finally {
            if (er1 != null) {
                er1.close();
            }
            if (er != null) {
                er.close();
            }
            if (ew != null) {
                ew.close();
            }
        }
    }

    static class ConcurrentListener
        extends TestListener<TestTypes>
    {
        private final Map<Integer, Integer> lastIds = new HashMap<Integer, Integer>();
        private final Set<Integer> connectedWriters = new HashSet<Integer>();

        // private final int listenerId;

        public ConcurrentListener(int id)
        {
            super(Integer.toString(id));
        }

        @Override
        public void onData(TestTypes t)
        {
            final int writerId = (int) t.longtype;
            final int msgId = t.inttype;
            if (msgId == -1) {
                // msgId -1 is the message used to check if reader/writer is connected
                if (!connectedWriters.contains(writerId)) {
                    connectedWriters.add(writerId);
                    println("Listener " + name + " connected to writer " + writerId);
                }
                return;
            }

            println("W: " + writerId + " L: " + name + " MSG: " + msgId);

            if (!lastIds.containsKey(writerId)) {
                // first message from this writer, id must be 0
                assertEquals(0, msgId);
            }
            else {
                // check that the order is consistent
                final int expected = lastIds.get(writerId) + 1;
                if (expected != msgId) {
                    fail("Writer: " + writerId + " Listerner: " + name + " Expected id: " + expected + " got id:"
                        + msgId);
                }
            }
            lastIds.put(writerId, msgId);

            // check some value
            assertEquals(3, t.bytetype);
            assertTrue(Arrays.equals(new byte[] {1, 2, 3, 4, 5}, t.bytearraytype));

            super.onData(t);
        }

        public boolean isConnected(int writerId)
        {
            return connectedWriters.contains(writerId);
        }
    }

    private static class WriterThread
        extends Thread
    {
        private final int writerId;
        private final int numEvents;
        private final EventWriter<TestTypes> writer;

        public WriterThread(int id, int num, EventWriter<TestTypes> w)
        {
            writerId = id;
            numEvents = num;
            writer = w;
        }

        @Override
        public void run()
        {
            println("Writer " + writerId + " start writing");
            final TestTypes t = new TestTypes(0, 0, (short) 0, "", (byte) 3, 0, false, TestEnum.ENUM_ZERO);
            t.setArrays(new long[] {1, 2, 3, 4, 5}, new int[] {1, 2, 3, 4, 5}, new short[] {1, 2, 3, 4, 5},
                new String[] {"1", "2", "3", "4", "5"}, new byte[] {1, 2, 3, 4, 5}, new float[] {1, 2, 3, 4, 5},
                new boolean[] {true, false, true, false, false}, new TestEnum[] {TestEnum.ENUM_ZERO,
                    TestEnum.ENUM_FIRST, TestEnum.ENUM_SECOND, TestEnum.ENUM_FIRST, TestEnum.ENUM_ZERO});
            for (int j = 0; j < numEvents; j++) {
                t.longtype = writerId; // put writer num in the long
                t.inttype = j; // put msg id in the int
                writer.write(t);
            }
        }
    };

    public void testConcurrent()
        throws Exception
    {
        println("testConcurrent");
        // make 5 writers
        final int numWriters = 5;
        final int numReaders = 5;
        final int numEvents = 10 * mScaleFactor * mScaleFactor; // create a lot of events
        final List<EventWriter<TestTypes>> writers = new LinkedList<EventWriter<TestTypes>>();
        final List<Thread> writerThreads = new LinkedList<Thread>();
        final List<EventReader<TestTypes>> readers = new LinkedList<EventReader<TestTypes>>();
        // List<EventReaderListener<TestTypes>> listeners = new LinkedList<EventReaderListener<TestTypes>>();
        final List<ConcurrentListener> listeners = new LinkedList<ConcurrentListener>();

        // create the readers
        for (int i = 0; i < numReaders; ++i) {
            // EventReaderListener<TestTypes> l = new EventReaderListener<TestTypes>() {
            final ConcurrentListener l = new ConcurrentListener(i);

            final EventReader<TestTypes> r = mQeo.createEventReader(TestTypes.class, l);

            readers.add(r);
            listeners.add(l);
        }

        // create the writers
        for (int i = 0; i < numWriters; ++i) {
            final int writerId = i;
            final EventWriter<TestTypes> w = mQeo.createEventWriter(TestTypes.class);

            // first we have to verify that all listeners are connected to this writer before we can start writing
            // we send a special message and verify that's arrived
            final TestTypes syncMsg = new TestTypes();
            syncMsg.inttype = -1;
            syncMsg.longtype = writerId;
            for (final ConcurrentListener b : listeners) {
                println("Waiting for listener " + b.getName() + " to get connected to writer " + i);
                while (!b.isConnected(writerId)) {
                    // not yet connected. write again and give it a little time to settle
                    w.write(syncMsg);
                    Thread.sleep(100);
                }
            }

            final WriterThread writerThread = new WriterThread(writerId, numEvents, w);

            writers.add(w);
            writerThreads.add(writerThread);
        }

        // start the writers
        for (final Thread t : writerThreads) {
            t.start();
        }

        // make writers finish
        println("Waiting for writers to finish");
        for (final Thread t : writerThreads) {
            t.join(10000);
        }
        println("Writers done");

        // verify that all samples were received
        for (final ConcurrentListener b : listeners) {
            for (int i = 0; i < numWriters; i++) {
                // wait for all data to arrive. Do this wait in a loop as it might take a while on android
                waitForData(b.onDataSem, numEvents);
            }
            assertEquals(numEvents * numWriters, b.getNumReceived());
            for (int i = 0; i < numWriters; ++i) {
                assertEquals((Integer) (numEvents - 1), b.lastIds.get(i));
            }
        }

        // close all
        for (final EventWriter<TestTypes> w : writers) {
            w.close();
        }
        for (final EventReader<TestTypes> r : readers) {
            r.close();
        }

    }

    public void testEventWriterKeyedData()
        throws Exception
    {
        println("testEventWriterKeyedData");

        try {
            mQeo.createEventWriter(TestState.class);
            fail("Exception should have been thrown");
        }
        catch (final IllegalArgumentException e) {
            /* We expect an IllegalArgumentException here */
        }
    }

    public void testEventReaderKeyedData()
        throws Exception
    {
        println("testEventReaderKeyedData");

        try {
            mQeo.createEventReader(TestState.class, new DefaultEventReaderListener<TestState>());
            fail("Exception should have been thrown");
        }
        catch (final IllegalArgumentException e) {
            /* We expect an IllegalArgumentException here */
        }
    }
}
