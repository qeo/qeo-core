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

package org.qeo.android.internal;

import org.qeo.exception.QeoException;
import org.qeo.internal.EntityAccessor;
import org.qeo.internal.common.EntityType;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.common.ReaderEntity;
import org.qeo.internal.common.ReaderListener;
import org.qeo.internal.common.WriterEntity;
import org.qeo.policy.PolicyUpdateListener;

import android.os.Looper;

/**
 * Class to determine which kind or reader/writer entities should be used for this implementation.
 */
public class EntityAccessorAndroid
    implements EntityAccessor
{
    private final int mId;
    private final Looper mLooper;
    private final QeoConnection mQeoConnection;

    /**
     * Create an instance.
     * 
     * @param id The identity to be used
     * @param qeoConnection The connection to the service.
     * @param looper the looper for callbacks. Can be null to use default looper.
     */
    public EntityAccessorAndroid(int id, QeoConnection qeoConnection, Looper looper)
    {
        mId = id;
        mLooper = looper;
        mQeoConnection = qeoConnection;
    }

    @Override
    public ReaderEntity getReader(ObjectType type, EntityType etype, ReaderListener listener,
        PolicyUpdateListener policyListener)
        throws QeoException
    {
        return new ServiceReader(mQeoConnection, mLooper, mId, type, etype, listener, policyListener);
    }

    @Override
    public WriterEntity getWriter(ObjectType type, EntityType etype, PolicyUpdateListener policyListener)
        throws QeoException
    {
        return new ServiceWriter(mQeoConnection, mLooper, mId, type, etype, policyListener);
    }

    /**
     * Get the qeo connection.
     * 
     * @return The qeo connection.
     */
    public QeoConnection getQeoConnection()
    {
        return mQeoConnection;
    }

}
