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

package org.qeo.jni;

import java.util.logging.Logger;

import org.qeo.internal.common.EntityType;
import org.qeo.internal.common.ObjectData;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.common.ReaderEntity;
import org.qeo.internal.common.ReaderFilter;
import org.qeo.internal.common.ReaderListener;
import org.qeo.internal.common.SampleInfo;
import org.qeo.policy.AccessRule;
import org.qeo.policy.Identity;
import org.qeo.policy.PolicyUpdateListener;

/**
 * Wrapper class around a native data reader.
 */
public class NativeReader
    extends ReaderEntity
{
    private long mNativeReader = 0;
    private NativeType mNativeType = null;
    private final String mName;
    private static final Logger LOG = Logger.getLogger(NativeQeo.TAG);

    /**
     * Create a new native data reader.
     * 
     * @param nativeQeo The NativeQeo object this reader belongs to
     * @param type The type for which to create a reader
     * @param etype The type of reader to create
     * @param listener An (optional) listener to attach to the reader
     * @param policyListener An (optional) policy update listener to attach to the reader
     */
    public NativeReader(NativeQeo nativeQeo, ObjectType type, EntityType etype, ReaderListener listener,
        PolicyUpdateListener policyListener)
    {
        super(type, listener, policyListener);
        mName = type.getName();
        try {
            final int[] rc = new int[1];

            mNativeType = NativeType.fromFactory(nativeQeo, type, type.getName());
            mNativeReader =
                nativeOpen(nativeQeo.getNativeFactory(), etype.ordinal(), mNativeType.getNativeType(), mName,
                    (listener == null ? false : true), (policyListener == null ? false : true), rc);
            NativeError.checkError(rc[0], "Cannot create Qeo reader");
            LOG.fine("NativeReader (" + mNativeReader + ") created for type (" + mNativeType.getNativeType() + ")");
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
        if (0 != mNativeReader) {
            nativeClose(mNativeReader);
            LOG.fine("NativeReader (" + mNativeReader + ") closed");
            mNativeReader = 0;
        }
        if (null != mNativeType) {
            mNativeType.close();
            mNativeType = null;
        }
    }

    @Override
    public synchronized ObjectData read(ReaderFilter f, SampleInfo info)
    {
        if (mNativeReader == 0) {
            throw new IllegalStateException("Native reader already closed");
        }
        final NativeData nativeData = new NativeData(this);
        nativeRead(mNativeReader, f.getInstanceHandle(), nativeData.getNativeData());
        info.setInstanceHandle(nativeData.getInstanceHandle());
        final ObjectData data = nativeData.getData(mType);
        nativeData.close();
        return data;
    }

    @Override
    public synchronized ObjectData take(ReaderFilter f, SampleInfo info)
    {
        if (mNativeReader == 0) {
            throw new IllegalStateException("Native reader already closed");
        }
        final NativeData nativeData = new NativeData(this);
        nativeTake(mNativeReader, f.getInstanceHandle(), nativeData.getNativeData());
        info.setInstanceHandle(nativeData.getInstanceHandle());
        final ObjectData data = nativeData.getData(mType);
        nativeData.close();
        return data;
    }

    @Override
    public void updatePolicy()
    {
        if (mPolicyListener == null) {
            throw new IllegalStateException("This reader is not enabled for fine-grained policy");
        }
        NativeError.checkError(nativeUpdatePolicy(mNativeReader));
    }

    @Override
    public void setBackgroundNotification(boolean enabled)
    {
        NativeError.checkError(nativeSetBGNS(mNativeReader, enabled));
    }

    /**
     * Get the native data reader pointer.
     * 
     * @return The native data reader pointer.
     */
    public long getNativeReader()
    {
        return mNativeReader;
    }

    /**
     * Called from native whenever new data is available on the native reader.
     * 
     * @param data The data
     */
    @NativeCallback
    public void onDataAvailable(long data)
    {
        final NativeData nativeData = new NativeData(data);
        NativeData.Status status = nativeData.getStatus();
        LOG.fine("onDataAvailable (" + status + ") for " + mType.getName());
        switch (status) {
            case NOTIFY:
                mListener.onUpdate();
                break;
            case DATA:
                try {
                    mListener.onData(nativeData.getData(mType));
                }
                catch (RuntimeException e) {
                    // catch exception here. This is a callback and makes no sence to throw
                    // don't print error here. On android this will result in printing it in the service. Send to the
                    // client.
                    mListener.onError(e);
                }
                break;
            case REMOVE:
                try {
                    mListener.onRemove(nativeData.getData(mType));
                }
                catch (RuntimeException e) {
                    // same as for DATA
                    mListener.onError(e);
                }
                break;
            case NO_MORE_DATA:
                mListener.onNoMoreData();
                break;
            default:
                break;
        }
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

    private native long nativeOpen(long factory, int readerType, long typeId, String nameVal,
        boolean setDataAvailableCb, boolean setPolicyUpdateCb, int[] rc);

    private native int nativeRead(long nativeReaderId, int instanceHandle, long nativeData);

    private native int nativeTake(long nativeReaderId, int instanceHandle, long nativeData);

    private native void nativeClose(long nativeReaderId);

    private native int nativeUpdatePolicy(long nativeReaderId);

    private native int nativeSetBGNS(long nativeReaderId, boolean enabled);
}
