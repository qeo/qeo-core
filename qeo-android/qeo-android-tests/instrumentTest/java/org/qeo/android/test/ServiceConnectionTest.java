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

import org.qeo.QeoFactory;
import org.qeo.android.QeoAndroid;
import org.qeo.android.QeoConnectionListener;
import org.qeo.exception.QeoException;
import org.qeo.testframework.QeoTestCase;

import android.os.Looper;

/**
 * 
 */
public class ServiceConnectionTest
    extends QeoTestCase
{

    private Semaphore syncSem1;
    private Semaphore syncSem2;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp(false); // do not init qeo
        syncSem1 = new Semaphore(0);
        syncSem2 = new Semaphore(0);
    }

    private final QeoConnectionListener listener1 = new QeoConnectionListener() {
        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            assertNotNull(qeo);
            syncSem1.release();
        }

        @Override
        public void onQeoError(QeoException ex)
        {
            fail("Qeo Error" + ex);
        }

    };

    private final QeoConnectionListener listener2 = new QeoConnectionListener() {
        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            assertNotNull(qeo);
            syncSem2.release();
        }

        @Override
        public void onQeoError(QeoException ex)
        {
            fail("Qeo Error" + ex);
        }
    };

    public void testInitFini()
        throws Exception
    {
        QeoAndroid.initQeo(mCtx, listener1, Looper.getMainLooper());
        waitForData(syncSem1);
        QeoAndroid.closeQeo(listener1);
    }

    public void testInitFiniTwice()
        throws Exception
    {
        for (int i = 0; i < 2; i++) {
            QeoAndroid.initQeo(mCtx, listener1, Looper.getMainLooper());
            waitForData(syncSem1);
            QeoAndroid.closeQeo(listener1);
        }
    }

    public void testInitTwice()
        throws Exception
    {
        QeoAndroid.initQeo(mCtx, listener1, Looper.getMainLooper());
        waitForData(syncSem1);
        QeoAndroid.initQeo(mCtx, listener2, Looper.getMainLooper());
        waitForData(syncSem2);
        QeoAndroid.closeQeo(listener1);
        QeoAndroid.closeQeo(listener2);
    }

}
