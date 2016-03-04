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

import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.qeo.policy.AccessRule;
import org.qeo.policy.Identity;
import org.qeo.policy.PolicyUpdateListener;
import org.qeo.testframework.QeoTestCase;
import org.qeo.testframework.TestListener;
import org.qeo.unittesttypes.TestState;
import org.qeo.unittesttypes.TestTypes;

public class PolicyTest
    extends QeoTestCase
    implements PolicyUpdateListener
{
    private static final long USER_ID_1 = 0x106;
    private final Semaphore onPolicySem = new Semaphore(0);
    private AccessRule mSelfPermission = AccessRule.ALLOW;
    private int mUIDCount = 0;
    private EventReader<TestTypes> er = null;
    private EventWriter<TestTypes> ew = null;
    private int state;
    private boolean failed;

    @Override
    public AccessRule onPolicyUpdate(Identity identity)
    {
        AccessRule perm = AccessRule.DENY;

        if (null != identity) {
            if (USER_ID_1 == identity.getUserID()) {
                perm = mSelfPermission;
            }
            mUIDCount++;
            return perm;
        }
        else {
            onPolicySem.release();
            return null;
        }
    }

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        er = null;
        ew = null;
        failed = false;
    }

    @Override
    public void tearDown()
        throws Exception
    {
        if (er != null) {
            er.close();
        }
        if (ew != null) {
            ew.close();
        }
        super.tearDown();
    }

    public void testEvent()
        throws Exception
    {
        TestListener<TestTypes> listener = new TestListener<TestTypes>();

        er = mQeo.createEventReader(TestTypes.class, listener, this); /* from self only */
        waitForData(onPolicySem);
        assertEquals(3, mUIDCount); /* expect 3 user IDs */
        ew = mQeo.createEventWriter(TestTypes.class, this); /* to all */
        /* write a sample */
        ew.write(new TestTypes());
        waitForData(listener.onDataSem);
        assertEquals(1, listener.getNumReceived());
        /* reconfigure reader to listen to no-one */
        mUIDCount = 0;
        mSelfPermission = AccessRule.DENY;
        er.updatePolicy();
        waitForData(onPolicySem);
        assertEquals(3, mUIDCount); /* again expect 3 user IDs */
        /* write another sample (should not arrive) */
        listener.reset();
        ew.write(new TestTypes());
        try {
            assertFalse(listener.onDataSem.tryAcquire(1, TimeUnit.SECONDS));
        }
        catch (InterruptedException e) {
            fail("unexpected interrupt while waiting");
        }
        assertEquals(0, listener.getNumReceived());
        /* now switch: reader=ALLOW, writer=DENY */
        mUIDCount = 0;
        ew.updatePolicy();
        waitForData(onPolicySem);
        assertEquals(3, mUIDCount);
        mUIDCount = 0;
        mSelfPermission = AccessRule.ALLOW;
        er.updatePolicy();
        waitForData(onPolicySem);
        assertEquals(3, mUIDCount);
        /* write another sample (again should not arrive) */
        listener.reset();
        ew.write(new TestTypes());
        try {
            assertFalse(listener.onDataSem.tryAcquire(1, TimeUnit.SECONDS));
        }
        catch (InterruptedException e) {
            fail("unexpected interrupt while waiting");
        }
        assertEquals(0, listener.getNumReceived());
    }

    private int countInstances(StateReader<TestState> r)
    {
        Iterator<TestState> it = r.iterator();
        int cnt = 0;

        while (it.hasNext()) {
            cnt++;
            it.next();
        }
        return cnt;
    }

    public void testState()
        throws Exception
    {
        StateReader<TestState> r = null;
        StateWriter<TestState> w = null;

        try {
            r = mQeo.createStateReader(TestState.class, null, this); /* from self only */
            waitForData(onPolicySem);
            assertEquals(3, mUIDCount); /* expect 3 user IDs */
            w = mQeo.createStateWriter(TestState.class, this); /* to all */
            /* write a sample */
            w.write(new TestState("id", "name", 0));
            assertEquals(1, countInstances(r));
            /* reconfigure reader to listen to no-one */
            mUIDCount = 0;
            mSelfPermission = AccessRule.DENY;
            r.updatePolicy();
            waitForData(onPolicySem);
            assertEquals(3, mUIDCount); /* again expect 3 user IDs */
            /* re-iterate */
            assertEquals(0, countInstances(r));
            /* now switch: reader=ALLOW, writer=DENY */
            mUIDCount = 0;
            w.updatePolicy();
            waitForData(onPolicySem);
            assertEquals(3, mUIDCount);
            mUIDCount = 0;
            mSelfPermission = AccessRule.ALLOW;
            r.updatePolicy();
            waitForData(onPolicySem);
            assertEquals(3, mUIDCount);
            /* re-iterate again */
            assertEquals(0, countInstances(r));
        }
        finally {
            if (null != r) {
                r.close();
            }
            if (null != w) {
                w.close();
            }
        }
    }

    public void testBad1()
        throws Exception
    {
        BadPolicyListener1 pl = new BadPolicyListener1();
        badListenerTester(pl);
    }

    /**
     * Check that writer creation and update call block until it's finished.
     */
    public void testTiming()
        throws Exception
    {
        state = 0;
        mUIDCount = 0;
        // callback should be executed before the writer is created
        EventWriter<TestTypes> ew1 = mQeo.createEventWriter(TestTypes.class, new PolicyUpdateListener() {

            @Override
            public AccessRule onPolicyUpdate(Identity identity)
            {
                if (identity == null) {
                    // last callback
                    // wait a little and check
                    try {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e) {
                        failed = true;
                    }
                    switch (mUIDCount) {
                        case 0:
                            if (state != 0) {
                                failed = true;
                            }
                            break;
                        case 1:
                            if (state != 1) {
                                failed = true;
                            }
                            break;
                        default:
                            failed = true;
                    }
                    mUIDCount++;
                }
                return AccessRule.ALLOW;
            }
        });
        addJunitReaderWriter(ew1); // register for cleanup
        assertFalse(failed);

        // again, this function should only return once callbacks are handled
        state = 1;
        ew1.updatePolicy();
        assertFalse(failed);

        // check that callbacks have been executed
        assertEquals(2, mUIDCount);
    }

    private void badListenerTester(PolicyUpdateListener pl)
        throws Exception
    {

        MyListener l = new MyListener();
        ew = mQeo.createEventWriter(TestTypes.class, pl);
        er = mQeo.createEventReader(TestTypes.class, l);
        Thread.sleep(1000); // give potential data time to arrive
        assertFalse(l.gotData);
    }

    private static class MyListener
        extends DefaultEventReaderListener<TestTypes>
    {
        boolean gotData = false;

        @Override
        public void onData(TestTypes data)
        {
            gotData = true;
        }
    }

    private static class BadPolicyListener1
        implements PolicyUpdateListener
    {

        @Override
        public AccessRule onPolicyUpdate(Identity identity)
        {
            return null; // always return null, not so nice!
        }
    }

}
