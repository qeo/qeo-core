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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.qeo.EventWriter;
import org.qeo.QeoFactory;
import org.qeo.exception.QeoException;
import org.qeo.java.QeoConnectionListener;
import org.qeo.java.QeoJava;
import org.qeo.jni.NativeQeo;
import org.qeo.policy.AccessRule;
import org.qeo.policy.Identity;
import org.qeo.policy.PolicyUpdateListener;
import org.qeo.unittesttypes.TestTypes;

/**
 * Test that mixes fine and coarse grained policy
 */
public class PolicyTestMix
    extends TestCase
{
    private QeoFactory mQeo;
    private Semaphore qeoReadySem;
    private QeoReadyListener mListener;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        qeoReadySem = new Semaphore(0);
        File dotQeo = new File("src/test/data/mixCoarseFine/");
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

    public void testPolicyUpdate()
        throws Exception
    {
        PolListener pl = new PolListener();
        EventWriter<TestTypes> ew = mQeo.createEventWriter(TestTypes.class, pl);
        waitForData(pl.getSem());
        Set<Long> uids = pl.getUids();
        assertEquals(2, uids.size());
        assertTrue(uids.contains(Long.valueOf(0x2d9)));
        assertTrue(uids.contains(Long.valueOf(0x2da)));
        ew.close();
    }

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
        }

        @Override
        public void onQeoError(QeoException ex)
        {
        }

    }

    private static class PolListener
        implements PolicyUpdateListener
    {
        private final Set<Long> uids;
        private boolean first;
        private final Semaphore sem;

        public PolListener()
        {
            uids = new HashSet<Long>();
            first = true;
            sem = new Semaphore(0);
        }

        public Semaphore getSem()
        {
            return sem;
        }

        public Set<Long> getUids()
        {
            return uids;
        }

        @Override
        public AccessRule onPolicyUpdate(Identity identity)
        {
            if (first) {
                first = false;
                uids.clear();
            }
            if (identity == null) {
                first = true;
                sem.release();
                return null;
            }
            uids.add(identity.getUserID());
            return AccessRule.ALLOW;
        }

    }

}
