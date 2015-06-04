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

import java.util.Random;
import java.util.concurrent.Semaphore;

import org.qeo.DefaultStateChangeReaderListener;
import org.qeo.ProcessService;
import org.qeo.ProcessService.FeedBack;
import org.qeo.StateChangeReader;
import org.qeo.testframework.QeoTestCase;

import android.content.Intent;

/**
 * Testcase to test qeo behavior while communicating with a started service that runs in a separate thread.
 */
public class ServiceTest
    extends QeoTestCase
{
    private Intent serviceStartIntent;
    private StateChangeReader<FeedBack> feedbackReader = null;
    private Semaphore sem;
    private FeedBack feedback;

    @Override
    public void setUp()
        throws Exception
    {
        if (Helper.isEmbedded()) {
            return; // not supported on embedded service
        }
        serviceStartIntent = new Intent(mContext, ProcessService.class);
        super.setUp();

        sem = new Semaphore(0);
    }

    @Override
    public void tearDown()
        throws Exception
    {
        if (Helper.isEmbedded()) {
            return; // not supported on embedded service
        }
        if (feedbackReader != null) {
            feedbackReader.close();
        }
        mContext.stopService(serviceStartIntent);
        super.tearDown();

    }

    public void testServiceLaunch()
        throws Exception
    {
        if (Helper.isEmbedded()) {
            return; // not supported on embedded service
        }
        int id = new Random().nextInt();
        serviceStartIntent.putExtra(ProcessService.INTENT_EXTRA_ID, id);
        mContext.startService(serviceStartIntent);

        feedbackReader = mQeo.createStateChangeReader(FeedBack.class, new DefaultStateChangeReaderListener<FeedBack>() {

            @Override
            public void onData(FeedBack t)
            {
                feedback = t;
                sem.release();
            }
        });

        // service will publish some data once it's ready
        waitForData(sem);
        assertTrue(feedback.ok);
        assertEquals(feedback.id, id);
        assertNotSame(feedback.pid, android.os.Process.myPid());
    }

}
