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

import java.util.concurrent.Semaphore;

import org.qeo.system.DeviceId;
import org.qeo.testframework.QeoTestCase;
import org.qeo.unittesttypes.DE2602Data;

/**
 * 
 */
public class DE2602Test
    extends QeoTestCase
    implements StateChangeReaderListener<DE2602Data>
{
    private final DE2602Data mData = new DE2602Data();
    private StateChangeReader<DE2602Data> mReader = null;
    private StateWriter<DE2602Data> mWriter = null;
    private final Semaphore mSync = new Semaphore(0);
    private volatile boolean mSuccess = false;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp(); // call as first
        mData.deviceId = new DeviceId();
        mData.deviceId.lower = 1234;
        mData.deviceId.upper = 5678;
        mData.name = "something";
        mReader = mQeo.createStateChangeReader(DE2602Data.class, this);
        mWriter = mQeo.createStateWriter(DE2602Data.class);
    }

    @Override
    public void tearDown()
        throws Exception
    {
        mWriter.close();
        mReader.close();
        super.tearDown(); // call as last
    }

    private boolean validate(DE2602Data data)
    {
        if (mData.deviceId.lower != data.deviceId.lower) {
            return false;
        }
        if (mData.deviceId.upper != data.deviceId.upper) {
            return false;
        }
        if (!mData.name.equals(data.name)) {
            return false;
        }
        return true;
    }

    @Override
    public void onData(DE2602Data t)
    {
        mSuccess = validate(t);
        mSync.release();
    }

    @Override
    public void onNoMoreData()
    {
    }

    @Override
    public void onRemove(DE2602Data t)
    {
        mSuccess = validate(t);
        mSync.release();
    }

    public void testWriteRemove()
    {
        /* write and validate */
        mSuccess = Boolean.FALSE;
        mWriter.write(mData);
        mSync.acquireUninterruptibly();
        assertEquals("invalid data at update", true, mSuccess);
        /* remove and validate */
        mSuccess = Boolean.FALSE;
        mWriter.remove(mData);
        mSync.acquireUninterruptibly();
        assertEquals("invalid data at remove", true, mSuccess);
    }
}
