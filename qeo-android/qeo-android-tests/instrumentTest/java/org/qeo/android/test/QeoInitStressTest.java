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
import android.util.Log;

/**
 * 
 */
public class QeoInitStressTest
    extends QeoTestCase
{
    private Semaphore sem;
    private boolean failed;

    @Override
    public void setUp()
        throws Exception
    {
        sem = new Semaphore(0);
        failed = false;
        super.setUp(false);
    }

    public void testParallel()
        throws InterruptedException
    {
        for (int i = 0; i < 100; i++) {
            QeoAndroid.initQeo(mCtx, new QeoConnectionListener() {

                @Override
                public void onQeoReady(QeoFactory qeo)
                {
                    QeoAndroid.closeQeo(this);
                    sem.release();
                }

                @Override
                public void onQeoError(QeoException ex)
                {
                    Log.e("Junit", "StressTest error", ex);
                    failed = true;
                }
            }, Looper.getMainLooper());
        }
        for (int i = 0; i < 10; i++) {
            // do waiting in a loop to give the multple connections some more time to settle
            waitForData(sem, 10);
        }
        assertFalse(failed);

    }

    public void testSequential()
        throws InterruptedException
    {
        for (int i = 0; i < 100; i++) {
            QeoAndroid.initQeo(mCtx, new QeoConnectionListener() {

                @Override
                public void onQeoReady(QeoFactory qeo)
                {
                    QeoAndroid.closeQeo(this);
                    sem.release();
                }

                @Override
                public void onQeoError(QeoException ex)
                {
                    Log.e("Junit", "StressTest error", ex);
                    failed = true;
                }
            }, Looper.getMainLooper());
            waitForData(sem);
        }
        assertFalse(failed);

    }

}
