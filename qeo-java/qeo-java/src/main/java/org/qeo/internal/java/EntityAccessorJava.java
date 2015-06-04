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

package org.qeo.internal.java;

import org.qeo.internal.EntityAccessor;
import org.qeo.internal.common.EntityType;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.common.ReaderEntity;
import org.qeo.internal.common.ReaderListener;
import org.qeo.internal.common.WriterEntity;
import org.qeo.jni.NativeQeo;
import org.qeo.jni.NativeReader;
import org.qeo.jni.NativeWriter;
import org.qeo.policy.PolicyUpdateListener;

/**
 * Entity accessor implementation for plain java. (using native Qeo).
 */
public class EntityAccessorJava
    implements EntityAccessor
{
    private NativeQeo mNativeFactory;

    /**
     * Default constructor.
     */
    public EntityAccessorJava()
    {
    }

    /**
     * Create an EntityAccessor for plain java.
     * 
     * @param nativeFactory The native qeo handle
     */
    public EntityAccessorJava(NativeQeo nativeFactory)
    {
        mNativeFactory = nativeFactory;
    }

    /**
     * Accessor to get the native factory.
     * 
     * @return the native factory
     */
    public NativeQeo getNativeFactory()
    {
        return mNativeFactory;
    }

    /**
     * Accessor to set the native factory.
     * 
     * @param nativeQeo the native factory
     */
    public void setNativeFactory(NativeQeo nativeQeo)
    {
        mNativeFactory = nativeQeo;
    }

    @Override
    public ReaderEntity getReader(ObjectType type, EntityType etype, ReaderListener listener,
        PolicyUpdateListener policyListener)
    {
        return new NativeReader(mNativeFactory, type, etype, listener, policyListener);
    }

    @Override
    public WriterEntity getWriter(ObjectType type, EntityType etype, PolicyUpdateListener policyListener)
    {
        return new NativeWriter(mNativeFactory, type, etype, policyListener);
    }

}
