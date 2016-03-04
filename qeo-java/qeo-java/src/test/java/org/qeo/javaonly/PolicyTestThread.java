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

package org.qeo.javaonly;

import org.qeo.EventReader;
import org.qeo.EventReaderListener;
import org.qeo.EventWriter;
import org.qeo.policy.AccessRule;
import org.qeo.policy.Identity;
import org.qeo.policy.PolicyUpdateListener;
import org.qeo.testframework.QeoTestCase;
import org.qeo.unittesttypes.TestTypes;

/**
 * Test policy update callback if they're fired from the correct thread.<br/>
 * Test is java-only as on android the callbacks will be done on the looper thread.
 */
public class PolicyTestThread
    extends QeoTestCase
{
    private Exception exception;

    /**
     * Check that policy updates happen from the same thread from which they're fired
     */
    public void testThreadWriter()
        throws Exception
    {
        final long threadId = Thread.currentThread().getId();

        EventWriter<TestTypes> ew1 = mQeo.createEventWriter(TestTypes.class, new PolicyUpdateListener() {

            @Override
            public AccessRule onPolicyUpdate(Identity identity)
            {
                if (Thread.currentThread().getId() != threadId) {
                    exception = new IllegalStateException("Callback is from wrong thread");
                }
                return AccessRule.ALLOW;
            }
        });
        addJunitReaderWriter(ew1); // register for cleanup
        if (exception != null) {
            throw exception;
        }
        ew1.updatePolicy();
        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Check that policy updates happen from the same thread from which they're fired
     */
    public void testThreadReader()
        throws Exception
    {
        final long threadId = Thread.currentThread().getId();

        EventReader<TestTypes> er1 = mQeo.createEventReader(TestTypes.class, new EventReaderListener<TestTypes>() {

            @Override
            public void onData(TestTypes t)
            {
                // not used
            }

            @Override
            public void onNoMoreData()
            {
                // not used
            }
        }, new PolicyUpdateListener() {

            @Override
            public AccessRule onPolicyUpdate(Identity identity)
            {
                if (Thread.currentThread().getId() != threadId) {
                    exception = new IllegalStateException("Callback is from wrong thread");
                }
                return AccessRule.ALLOW;
            }
        });
        addJunitReaderWriter(er1); // register for cleanup
        if (exception != null) {
            throw exception;
        }
        er1.updatePolicy();
        if (exception != null) {
            throw exception;
        }
    }
}
