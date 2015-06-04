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

import java.util.Arrays;

import org.qeo.exception.QeoException;
import org.qeo.testframework.QeoTestCase;
import org.qeo.testframework.TestListener;
import org.qeo.unittesttypes.NestedTypes;
import org.qeo.unittesttypes.TestEnum;
import org.qeo.unittesttypes.TestState;
import org.qeo.unittesttypes.TestTypes;

public class NestedTypeTest
    extends QeoTestCase
{

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp(); // call as first
        setVerbose(false);
        println("setup");
    }

    @Override
    public void tearDown()
        throws Exception
    {
        println("tearDown");
        super.tearDown(); // call as last
    }

    private static class NestedTypeListener
        extends TestListener<NestedTypes>
    {
        public NestedTypeListener()
        {
        }

        void checkExpected(TestTypes testType, TestTypes testType2, TestState testState, String id, boolean checkArray)
        {
            assertTrue(Arrays.equals(testType.bytearraytype, lastReceivedItem.mTestType.bytearraytype));
            checkSub(testType, lastReceivedItem.mTestType);
            assertTrue(Arrays.equals(testType2.bytearraytype, lastReceivedItem.mTestType2.bytearraytype));
            checkSub(testType2, lastReceivedItem.mTestType2);
            if (checkArray) {
                assertEquals(2, lastReceivedItem.mTestTypeArray.length);
                checkSub(testType, lastReceivedItem.mTestTypeArray[0]);
                checkSub(testType2, lastReceivedItem.mTestTypeArray[1]);
            }
            else {
                assertEquals(0, lastReceivedItem.mTestTypeArray.length);
            }
            assertEquals(testState.name, lastReceivedItem.mTestState.name);
            assertEquals(testState.id, lastReceivedItem.mTestState.id);
            assertEquals(testState.i, lastReceivedItem.mTestState.i);
            assertEquals(id, lastReceivedItem.mId);
        }

        private void checkSub(TestTypes testType, TestTypes received)
        {
            assertEquals(testType.bytetype, received.bytetype);
            assertEquals(testType.floattype, received.floattype);
            assertEquals(testType.inttype, received.inttype);
            assertEquals(testType.longtype, received.longtype);
            assertEquals(testType.stringtype, received.stringtype);
            assertEquals(testType.booleantype, received.booleantype);
        }
    }

    public void testNestedType()
        throws QeoException, InterruptedException
    {
        NestedTypeListener listener;
        StateChangeReader<NestedTypes> reader = null;
        StateWriter<NestedTypes> writer = null;
        try {
            listener = new NestedTypeListener();
            reader = mQeo.createStateChangeReader(NestedTypes.class, listener);
            writer = mQeo.createStateWriter(NestedTypes.class);

            final TestTypes testType =
                new TestTypes(99, 99, (short) 99, "str" + 99, (byte) (99), (float) 1.0 / (99), true,
                    TestEnum.ENUM_FIRST);
            testType.setArrays(new long[] {99}, new int[] {99}, new short[] {99}, new String[] {}, new byte[] {99},
                new float[] {(float) (1.0 / 99)}, new boolean[] {false}, new TestEnum[] {TestEnum.ENUM_FIRST});
            final TestTypes testType2 =
                new TestTypes(-99, -99, (short) -99, "str" + -99, (byte) (-99), (float) 1.0 / (-99), false,
                    TestEnum.ENUM_SECOND);
            testType2.setArrays(new long[] {-99}, new int[] {-99}, new short[] {-99}, new String[] {},
                new byte[] {-99}, new float[] {(float) (1.0 / -99)}, new boolean[] {false},
                new TestEnum[] {TestEnum.ENUM_SECOND});
            final TestState testState = new TestState("id", "name", 999);
            final NestedTypes nestedType = new NestedTypes(testType, testType2, testState, "nested");
            nestedType.mTestTypeArray = new TestTypes[] {testType, testType2};

            writer.write(nestedType);
            waitForData(listener.onDataSem);
            waitForData(listener.onNoMoreDataSem);
            listener.checkExpected(testType, testType2, testState, "nested", true);

            writer.write(nestedType);
            waitForData(listener.onDataSem);
            waitForData(listener.onNoMoreDataSem);
            listener.checkExpected(testType, testType2, testState, "nested", true);

            writer.remove(nestedType);
            waitForData(listener.onRemoveSem);
        }
        finally {
            if (null != writer) {
                writer.close();
            }
            if (null != reader) {
                reader.close();
            }
        }
    }

    public void testNullNestedType()
        throws QeoException, InterruptedException
    {
        NestedTypeListener listener;
        StateChangeReader<NestedTypes> reader = null;
        StateWriter<NestedTypes> writer = null;
        try {
            listener = new NestedTypeListener();
            reader = mQeo.createStateChangeReader(NestedTypes.class, listener);
            writer = mQeo.createStateWriter(NestedTypes.class);

            final TestState testState = new TestState("id", "name", 999);
            final NestedTypes nestedType = new NestedTypes(new TestTypes(), new TestTypes(), testState, "nested");
            TestTypes nullTestType = new TestTypes(0, 0, (short) 0, "", (byte) 0, 0, false, TestEnum.ENUM_ZERO);
            nullTestType.setArrays(new long[] {}, new int[] {}, new short[] {}, new String[] {}, new byte[] {},
                new float[] {}, new boolean[] {}, new TestEnum[] {});

            writer.write(nestedType);
            waitForData(listener.onDataSem);
            waitForData(listener.onNoMoreDataSem);
            listener.checkExpected(nullTestType, nullTestType, testState, "nested", false);

            writer.write(nestedType);
            waitForData(listener.onDataSem);
            waitForData(listener.onNoMoreDataSem);
            listener.checkExpected(nullTestType, nullTestType, testState, "nested", false);

            writer.remove(nestedType);
            waitForData(listener.onRemoveSem);
        }
        finally {
            if (null != writer) {
                writer.close();
            }
            if (null != reader) {
                reader.close();
            }
        }
    }
}
