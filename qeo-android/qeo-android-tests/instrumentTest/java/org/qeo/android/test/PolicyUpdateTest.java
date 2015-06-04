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

import org.qeo.EventReaderListener;
import org.qeo.ReaderWriter;
import org.qeo.exception.QeoException;
import org.qeo.policy.AccessRule;
import org.qeo.policy.Identity;
import org.qeo.policy.PolicyUpdateListener;
import org.qeo.testframework.QeoTestCase;
import org.qeo.unittesttypes.TestTypes;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Test for Android specific to test policyupdate callbacks with loopers.
 */
public class PolicyUpdateTest
    extends QeoTestCase
{
    private Handler handler;
    private ReaderWriter<TestTypes> rw = null;
    private Exception exception;
    private boolean policyUpdateDone;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        handler = new Handler(Looper.getMainLooper());
        exception = null;
    }

    public void testReader()
        throws Exception
    {
        generic(true);
    }

    public void testWriter()
        throws Exception
    {
        generic(true);
    }

    /**
     * Tests that the onPolicyUpdate callbacks happen on the correct threads.
     */
    private void generic(final boolean reader)
        throws Exception
    {
        final Semaphore sem = new Semaphore(0);
        final long threadId = Looper.getMainLooper().getThread().getId();
        // create reader on and schedule on main looper to ensure policy updates are triggered on that looper
        policyUpdateDone = false;
        Runnable r1 = new Runnable() {
            @Override
            public void run()
            {

                try {
                    PolicyUpdateListener pul = new PolicyUpdateListener() {

                        @Override
                        public AccessRule onPolicyUpdate(Identity identity)
                        {
                            if (Thread.currentThread().getId() != threadId) {
                                exception = new IllegalStateException("wrong thread");
                            }
                            if (identity == null) {
                                try {
                                    // sleep a little to try to make timing problems
                                    Thread.sleep(100);
                                }
                                catch (InterruptedException e) {
                                    exception = e;
                                }
                                policyUpdateDone = true;
                            }
                            return AccessRule.ALLOW;
                        }
                    };

                    if (reader) {
                        // reader
                        rw = mQeo.createEventReader(TestTypes.class, new EventReaderListener<TestTypes>() {

                            @Override
                            public void onData(TestTypes t)
                            {
                                // ignore
                            }

                            @Override
                            public void onNoMoreData()
                            {
                                // ignore
                            }
                        }, pul);
                    }
                    else {
                        // writer
                        rw = mQeo.createEventWriter(TestTypes.class, pul);
                    }
                }
                catch (QeoException e) {
                    Log.e("TEST", "fatal error", e);
                }
                addJunitReaderWriter(rw);
                sem.release();
            }
        };
        handler.post(r1);
        waitForData(sem);
        if (exception != null) {
            throw exception;
        }
        assertTrue(policyUpdateDone);

        // update policy from main looper.
        Runnable r2 = new Runnable() {
            @Override
            public void run()
            {
                rw.updatePolicy();
                sem.release();
            }
        };
        policyUpdateDone = false;
        handler.post(r2);
        waitForData(sem);
        if (exception != null) {
            throw exception;
        }
        assertTrue(policyUpdateDone);

        // update policy from another thread, callback should be on main looper
        policyUpdateDone = false;
        rw.updatePolicy();
        if (exception != null) {
            throw exception;
        }
        assertTrue(policyUpdateDone);

        Log.d("TEST", "test done!");
    }

}
