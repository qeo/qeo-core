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

package org.qeo.android.internal;

import java.util.concurrent.Semaphore;

import android.os.Looper;

/**
 * Utility class to construct easily and safely an handler that has it's own thread.
 */
public class LooperThread
    extends Thread
{
    private Semaphore mSem = new Semaphore(0);
    private Looper mLooper;
    private boolean mIsInit;

    @Override
    public void run()
    {
        Looper.prepare();
        mLooper = Looper.myLooper();
        mIsInit = true;
        mSem.release();
        Looper.loop();
    }

    /**
     * to be called after the thread has been started to ensure the handler is created.<br/>
     * NOTE: this can only be called once.
     */
    public void waitForInit()
    {
        mSem.acquireUninterruptibly();
        mSem = null;
    }

    /**
     * Check if the looper is created.
     * 
     * @return True if the looper is created. false otherwise.
     */
    public boolean isInit()
    {
        return mIsInit;
    }

    /**
     * Get the Looper of this thread. Make sure to have called waitForInit() first to ensure this is valid.
     * 
     * @return The Looper of this thread.
     */
    public Looper getLooper()
    {
        return mLooper;
    }
}
