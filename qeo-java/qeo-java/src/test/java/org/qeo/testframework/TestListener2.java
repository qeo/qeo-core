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

package org.qeo.testframework;

import java.util.concurrent.Semaphore;

import org.qeo.StateReaderListener;

/**
 * testlistner for statereader.
 */
public class TestListener2
    implements StateReaderListener
{
    public final Semaphore onUpdateSem = new Semaphore(0);
    private int numReceived;

    @Override
    public void onUpdate()
    {
        numReceived++;
        onUpdateSem.release();
    }

    public int getNumReceived()
    {
        return numReceived;
    }

}
