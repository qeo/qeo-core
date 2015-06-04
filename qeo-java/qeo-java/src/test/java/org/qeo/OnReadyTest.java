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

import org.qeo.exception.QeoException;
import org.qeo.internal.QeoListener;
import org.qeo.system.RegistrationRequest;
import org.qeo.testframework.QeoConnectionTestListener;
import org.qeo.testframework.QeoTestCase;
import org.qeo.unittesttypes.TestTypes;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test that creates multiple factories and creates a writer in the onQeoReady to make sure factories are indeed always
 * properly ready at that point.
 */
public class OnReadyTest
    extends QeoTestCase
{
    private final Logger LOG = Logger.getLogger("onReadyTest");
    private boolean mFailed;
    private Semaphore mSem;
    private List<QeoConnectionTestListener> mConnections;
    private List<QeoWriter<?>> ews = null;
    private final MyListener mListenerClosed = new MyListener(false);
    private final MyListener mListenerOpen = new MyListener(true);

    @Override
    public void setUp()
        throws Exception
    {
        mFailed = false;
        mSem = new Semaphore(0);
        mConnections = new LinkedList<QeoConnectionTestListener>();
        ews = new LinkedList<QeoWriter<?>>();
        super.setUp(false);
    }

    @Override
    public void tearDown()
        throws Exception
    {
        for (QeoWriter<?> ew : ews) {
            LOG.fine("Closing writer");
            ew.close();
        }
        for (QeoConnectionTestListener conn : mConnections) {
            LOG.fine("Closing factory");
            conn.closeFactory();
        }
        super.tearDown();
    }

    /**
     * Create 2 closed factories
     */
    public void test2ClosedFactories()
        throws InterruptedException
    {
        createClosedFactory();
        createClosedFactory();
        assertFalse(mFailed);
    }

    /**
     * Create 2 closed factories
     */
    public void test2OpenFactories()
        throws InterruptedException
    {
        createOpenFactory();
        createOpenFactory();
        assertFalse(mFailed);
    }

    public void testOpenClose()
        throws InterruptedException
    {
        // first open
        createOpenFactory();
        // then closed
        createClosedFactory();
        assertFalse(mFailed);
    }

    public void testCloseOpen()
        throws InterruptedException
    {
        // first closed
        createClosedFactory();
        // then open
        createOpenFactory();
        assertFalse(mFailed);
    }

    public void testMultiple()
        throws InterruptedException
    {
        // first closed
        createClosedFactory();
        // then open
        createOpenFactory();
        // and again
        createClosedFactory();
        createOpenFactory();
        assertFalse(mFailed);
    }

    private void createClosedFactory()
        throws InterruptedException
    {
        // create factory and create writer in onReady callback
        mConnections.add(createFactory(mListenerClosed));
        // wait for onQeoReady to be called
        waitForFactoryInit(mSem);
    }

    private void createOpenFactory()
        throws InterruptedException
    {
        // create factory and create writer in onReady callback
        mConnections.add(createFactory(mListenerOpen));
        // wait for onQeoReady to be called
        waitForFactoryInit(mSem);
    }

    private class MyListener
        implements QeoListener
    {
        private final boolean mOpen;

        MyListener(boolean open)
        {
            mOpen = open;
        }

        @Override
        public boolean onStartAuthentication()
        {
            mFailed = true;
            fail("did not expect this");
            return false;
        }

        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            // create writer immediately in this callback
            try {
                if (mOpen) {
                    StateWriter<RegistrationRequest> ew = qeo.createStateWriter(RegistrationRequest.class);
                    ews.add(ew);
                }
                else {
                    EventWriter<TestTypes> ew = qeo.createEventWriter(TestTypes.class);
                    ews.add(ew);
                }

                mSem.release();
            }
            catch (QeoException e) {
                LOG.log(Level.SEVERE, "Unexpected error creating writer", e);
                mFailed = true;
            }
        }

        @Override
        public void onQeoError(QeoException ex)
        {
            LOG.log(Level.SEVERE, "Unexpected onQeoError", ex);
            mFailed = true;
        }

        @Override
        public void onQeoClosed(QeoFactory qeo)
        {
            fail("did not expect this");
            mFailed = true;
        }

        @Override
        public void onWakeUp(String typeName)
        {
            fail("did not expect wake up");
            mFailed = true;
        }

        @Override
        public void onBgnsConnectionChange(boolean connected)
        {
        }
    };
}
