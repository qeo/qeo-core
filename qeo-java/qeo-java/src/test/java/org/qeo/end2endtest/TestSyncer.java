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

package org.qeo.end2endtest;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.qeo.DefaultStateChangeReaderListener;
import org.qeo.QeoFactory;
import org.qeo.StateChangeReader;
import org.qeo.StateWriter;

/**
 * The TestSyncer sync's the execution of test cases between different test runners.
 */
public class TestSyncer
{
    private static final int UNKNOWN_STAGE = -1;
    private static final int LOCK_TIMEOUT = 60;

    private final QeoFactory qeo;
    private final String testRunner;
    private int stage = UNKNOWN_STAGE;
    private int numberOfTestRunners = 0;

    private StateWriter<TestState> testStateWriter = null;

    private final MySCRL mySCRL = new MySCRL();

    private class MySCRL
            extends DefaultStateChangeReaderListener<TestState>
    {
        final Semaphore sem = new Semaphore(0);

        @Override
        public void onData(TestState t)
        {
            System.out.println("onData: " + t);
            if (t.stage == TestSyncer.this.stage) {
                System.out.println("Releasing semaphore");
                sem.release();
            }
        }
    }

    public TestSyncer(QeoFactory qeoVal, String testRunnerVal, int stageVal, int numberOfTestRunnersVal)
    {
        this.qeo = qeoVal;
        this.testRunner = testRunnerVal;
        this.stage = stageVal;
        this.numberOfTestRunners = numberOfTestRunnersVal;
    }

    public void sync()
        throws Exception
    {
        /* write test status */
        System.out.println("Creating test state writer");
        testStateWriter = qeo.createStateWriter(TestState.class);
        testStateWriter.write(new TestState(testRunner, stage));
        System.out.println("Wrote test status");

        /* expecting # numberOfTestRunners test states */
        System.out.println("Expecting " + numberOfTestRunners + " test states");
        final StateChangeReader<TestState> testStateReader = qeo.createStateChangeReader(TestState.class, mySCRL);
        if (!mySCRL.sem.tryAcquire(numberOfTestRunners, LOCK_TIMEOUT, TimeUnit.SECONDS)) {
            System.out.println("Semaphore lock timeout");
        }

        System.out.println("Sync'ed !");

        testStateReader.close();
    }

    @Override
    protected void finalize()
        throws Throwable
    {
        // only close writer here because otherwise the sample written by the writer is disposed before
        // other test runners' QeoTestCaseSyncers have read it
        if (null != testStateWriter) {
            testStateWriter.close();
        }
        super.finalize();
    }
}
