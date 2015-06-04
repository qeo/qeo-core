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

package org.qeo.jni;

import java.util.logging.Logger;

import org.qeo.internal.common.EntityType;
import org.qeo.internal.common.ObjectData;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.common.WriterEntity;
import org.qeo.policy.AccessRule;
import org.qeo.policy.Identity;
import org.qeo.policy.PolicyUpdateListener;

/**
 * Wrapper class around a native data writer.
 */
public class NativeWriter
    extends WriterEntity
{
    private long mNativeWriter = 0;
    private NativeType mNativeType = null;
    private final String mName;
    private static final Logger LOG = Logger.getLogger("NativeWriter");

    /**
     * Create a new native data writer.
     * 
     * @param nativeQeo The NativeQeo object this reader belongs to
     * @param type The type for which to create a writer
     * @param etype The type of reader to create
     * @param policyListener An (optional) policy update listener to attach to the writer
     */
    public NativeWriter(NativeQeo nativeQeo, ObjectType type, EntityType etype, PolicyUpdateListener policyListener)
    {
        super(type, policyListener);
        mName = type.getName();
        try {
            final int[] rc = new int[1];

            mNativeType = NativeType.fromFactory(nativeQeo, type, type.getName());
            mNativeWriter =
                nativeOpen(nativeQeo.getNativeFactory(), etype.ordinal(), mNativeType.getNativeType(), mName,
                    (policyListener == null ? false : true), rc);
            NativeError.checkError(rc[0], "Cannot create Qeo writer");
            LOG.fine("NativeWriter (" + mNativeWriter + ") created for type (" + mNativeType.getNativeType() + ")");
        }
        catch (final RuntimeException e) {
            if (null != mNativeType) {
                mNativeType.close();
                mNativeType = null;
            }
            throw e;
        }
    }

    @Override
    public synchronized void close()
    {
        LOG.fine("Closing writer " + mNativeWriter);
        if (0 != mNativeWriter) {
            nativeClose(mNativeWriter);
            LOG.fine("NativeWriter (" + mNativeWriter + ") closed");
            mNativeWriter = 0;
        }
        if (null != mNativeType) {
            mNativeType.close();
            mNativeType = null;
        }
    }

    @Override
    public synchronized void write(ObjectData data)
    {
        LOG.finest("Writing on native writer " + mNativeWriter);
        if (mNativeWriter == 0) {
            throw new IllegalStateException("Native writer already closed");
        }
        final NativeData d = new NativeData(this, data, mType);
        try {
            NativeError.checkError(nativeWrite(mNativeWriter, d.getNativeData()));
        }
        finally {
            d.close();
        }
    }

    @Override
    public synchronized void remove(ObjectData data)
    {
        LOG.finest("Removing on native writer " + mNativeWriter);
        if (mNativeWriter == 0) {
            throw new IllegalStateException("Native writer already closed");
        }
        final NativeData d = new NativeData(this, data, mType);
        try {
            NativeError.checkError(nativeRemove(mNativeWriter, d.getNativeData()));
        }
        finally {
            d.close();
        }
    }

    @Override
    public synchronized void updatePolicy()
    {
        LOG.finest("Updating policy on native writer " + mNativeWriter);
        if (mPolicyListener == null) {
            throw new IllegalStateException("This writer is not enabled for fine-grained policy");
        }
        NativeError.checkError(nativeUpdatePolicy(mNativeWriter));
    }

    /**
     * Get the native data writer pointer.
     * 
     * @return The native data writer pointer.
     */
    public long getNativeWriter()
    {
        return mNativeWriter;
    }

    /**
     * Policy update callback from native-to-Java.
     * 
     * @param uid The user ID for which to ask permission.
     * @return User ID is allowed (true) or not (false)?
     */
    @NativeCallback
    public boolean onPolicyUpdate(long uid)
    {
        Identity id = new Identity(uid);
        boolean allow = false;

        if (AccessRule.ALLOW == mPolicyListener.onPolicyUpdate(id)) {
            allow = true;
        }
        return allow;
    }

    /**
     * Policy update done callback from native-to-Java.
     */
    @NativeCallback
    public void onPolicyUpdateDone()
    {
        mPolicyListener.onPolicyUpdate(null);
    }

    /*
     * The next block contains the native functions needed by the NativeType class.
     */

    private native long nativeOpen(long factory, int writerType, long nativeType, String name,
        boolean setPolicyUpdateCb, int[] rc);

    private static native int nativeWrite(long nativeWriter, long nativeData);

    private static native int nativeRemove(long nativeWriter, long nativeData);

    private static native void nativeClose(long nativeWriter);

    private static native int nativeUpdatePolicy(long nativeWriter);
}
