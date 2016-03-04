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

import org.qeo.exception.QeoException;
import org.qeo.internal.QeoListener;
import org.qeo.system.RegistrationRequest;
import org.qeo.testframework.QeoConnectionTestListener;
import org.qeo.testframework.QeoTestCase;
import org.qeo.unittesttypes.TestState;

public class FactoryTest
    extends QeoTestCase
{
    private boolean mFailed;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        mFailed = false;
    }

    /**
     * Create multiple factories with default id.
     * 
     * @throws QeoException
     * @throws InterruptedException
     */
    public void testFactoryDifferentId()
        throws QeoException, InterruptedException
    {
        QeoFactory mQeo1;
        QeoConnectionTestListener mQeoReadyListener1;
        QeoFactory mQeo2;
        QeoConnectionTestListener mQeoReadyListener2;

        mQeoReadyListener1 = createFactory(getFactoryClosedId());
        mQeo1 = mQeoReadyListener1.getFactory();
        assertNotNull(mQeo1);
        mQeoReadyListener2 = createFactory(getFactoryClosedId());
        mQeo2 = mQeoReadyListener2.getFactory();
        assertNotNull(mQeo2);

        StateWriter<TestState> sw1 = mQeo1.createStateWriter(TestState.class);
        addJunitReaderWriter(sw1);
        StateWriter<RegistrationRequest> sw2 = mQeo2.createStateWriter(RegistrationRequest.class);
        addJunitReaderWriter(sw2);

        closeFactory(mQeoReadyListener1);
        closeFactory(mQeoReadyListener2);
    }

    public void testQuickOpenAfterClose()
        throws Exception
    {
        tearDown(false); // first close regular factory

        // create factory
        QeoConnectionTestListener l = createFactory(new MyListener());
        // wait for it to be ready
        l.waitForInit();

        // now close and open quickly, this is like rotation on android.
        for (int i = 0; i < 10; ++i) {
            l.closeFactory();
            assertFalse(mFailed);
            l = createFactory(new MyListener());
        }

        // make sure the last one works
        l.waitForInit();
        // and close again.
        l.closeFactory();
        assertFalse(mFailed);
    }

    private class MyListener
        implements QeoListener
    {

        @Override
        public void onQeoReady(QeoFactory qeo)
        {
        }

        @Override
        public void onQeoClosed(QeoFactory qeo)
        {
        }

        @Override
        public void onQeoError(QeoException ex)
        {
            log("onQeoError: " + ex);
            mFailed = true;
        }

        @Override
        public boolean onStartAuthentication()
        {
            return false;
        }

        @Override
        public void onWakeUp(String typeName)
        {
        }

        @Override
        public void onBgnsConnectionChange(boolean connected)
        {
        }
    }

}
