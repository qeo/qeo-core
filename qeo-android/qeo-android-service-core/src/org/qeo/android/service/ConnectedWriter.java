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

package org.qeo.android.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.qeo.android.internal.IServiceQeoPolicyUpdateCallback;
import org.qeo.android.internal.ParcelableData;
import org.qeo.android.internal.ParcelableType;
import org.qeo.android.internal.QeoParceler;
import org.qeo.exception.QeoException;
import org.qeo.internal.BaseFactory;
import org.qeo.internal.common.EntityType;
import org.qeo.jni.NativeWriter;

import android.os.IBinder;

/**
 * the connected writer is actually the counter part of the service writer on the Qeo library for Android. So all writer
 * related method calls done from the Qeo library for Android to the Qeo service are being handled by this class.
 */
public final class ConnectedWriter
    implements QeoParceler.JoinCallbacks
{
    private static final Logger LOG = Logger.getLogger(QeoService.TAG);
    private static final int ID_WRITE = 0;
    private static final int ID_REMOVE = 1;
    private final NativeWriter mNativeWriter;
    private final ConnectedPolicyUpdateListener mPolicyUpdateListener;
    private final IBinder mBinder;
    private final long mWriterId;
    private final QeoParceler mQeoParceler;

    /**
     * This hash-table keeps all the created mWriter along with the mWriter id from DDS.
     */
    private static Map<Long, ConnectedWriter> sWriters = new HashMap<Long, ConnectedWriter>();

    /**
     * Constructor of the connected mWriter.
     * 
     * @param factory the factory where the writer will be created
     * @param type The type to be used by this mWriter
     * @param etype The type of mWriter (e.g. state or event)
     * @param policyListener The policy callback interface towards the application
     * @param ib the IBinder
     * @throws QeoException if getWriter fails
     * 
     */
    private ConnectedWriter(BaseFactory factory, ParcelableType type, IServiceQeoPolicyUpdateCallback policyListener,
        EntityType etype, IBinder ib)
        throws QeoException
    {
        this.mBinder = ib;
        mPolicyUpdateListener = (null == policyListener ? null : new ConnectedPolicyUpdateListener(policyListener));
        mNativeWriter =
            (NativeWriter) factory.getEntityAccessor().getWriter(type.getType(), etype, mPolicyUpdateListener);
        mWriterId = mNativeWriter.getNativeWriter();
        mQeoParceler = new QeoParceler(this);
    }

    /**
     * Add a new connected mWriter and put the mWriter and its id in the sWriters hash-table.
     * 
     * @param factory the factory where the writer will be created
     * @param type The type to be used by this mWriter
     * @param policyListener The policy callback interface towards the application
     * @param etype The type of mWriter (e.g. state or event)
     * @param binder the IBinder
     * 
     * @return the mWriter id or -1 in case of an exception
     */
    public static synchronized long addWriter(BaseFactory factory, ParcelableType type,
        IServiceQeoPolicyUpdateCallback policyListener, EntityType etype, IBinder binder)
    {
        // Writer creation/deletion is done while holding class lock. This ensures they don't get created/deleted from
        // multiple threads.
        try {
            ConnectedWriter cw = new ConnectedWriter(factory, type, policyListener, etype, binder);
            sWriters.put(cw.mNativeWriter.getNativeWriter(), cw);
            LOG.fine("addWriter " + etype + " " + cw.mNativeWriter.getNativeWriter() + " for type " + type.getType()
                + " from activity " + binder.toString());
            return cw.mNativeWriter.getNativeWriter();
        }
        catch (QeoException e) {
            LOG.warning("addWriter " + etype + " failed for type " + type.getType() + " from activity "
                + binder.toString());
        }
        return -1;
    }

    /**
     * Get a writer instance based on a writerId.
     * 
     * @param writerId The id of the writer.
     * @return The writer. Will throw an exception if not available.
     */
    public static synchronized ConnectedWriter getWriter(long writerId)
    {
        ConnectedWriter cw = sWriters.get(writerId);
        if (cw == null) {
            throw new IllegalStateException("Could not find datawriter " + writerId);
        }
        return cw;
    }

    /**
     * Write new data to the mWriter represented by dataWriter.
     * 
     * @param firstBlock Indicates if this is the first block.
     * @param lastBlock Indicated if this is the last block.
     * @param totalSize the total size of all the blocks.
     * @param data the data itself.
     */
    public void write(boolean firstBlock, boolean lastBlock, int totalSize, byte[] data)
    {
        // send to the join function. This will call objectCreated() if all data has arrived
        mQeoParceler.join(ID_WRITE, firstBlock, lastBlock, totalSize, data);
    }

    /**
     * Remove data to the mWriter represented by dataWriter.
     * 
     * @param firstBlock Indicates if this is the first block.
     * @param lastBlock Indicated if this is the last block.
     * @param totalSize the total size of all the blocks.
     * @param data the data itself.
     */
    public void remove(boolean firstBlock, boolean lastBlock, int totalSize, byte[] data)
    {
        // send to the join function. This will call onFragmentsJoined() if all data has arrived
        mQeoParceler.join(ID_REMOVE, firstBlock, lastBlock, totalSize, data);
    }

    @Override
    public void onFragmentsJoined(int id, ParcelableData pd)
    {
        // called by mQeoParceler.join()
        switch (id) {
            case ID_WRITE:
                mNativeWriter.write(pd.getData());
                break;
            case ID_REMOVE:
                mNativeWriter.remove(pd.getData());
                break;
            default:
                throw new IllegalStateException("Can't handle this id");
        }

    }

    /**
     * Trigger a policy update.
     */
    public void updatePolicy()
    {
        mNativeWriter.updatePolicy();
    }

    /**
     * Closes the mWriter and removes it from the hash table.
     */
    public void close()
    {
        LOG.fine("removeWriter " + mWriterId);
        synchronized (ConnectedWriter.class) {
            sWriters.remove(mWriterId);
            mNativeWriter.close();
        }
    }

    /**
     * Delete all sWriters and remove them all from the hash table.
     * 
     */
    public static synchronized void closeAll()
    {
        LOG.fine("close all writers");
        /* To loops are necessary because we cannot remove items while iterating */
        final List<ConnectedWriter> toremove = new ArrayList<ConnectedWriter>();
        toremove.addAll(sWriters.values());
        for (ConnectedWriter cw : toremove) {
            cw.close();
        }
    }

    /**
     * Delete all sWriters originating from a client.
     * 
     * @param ibinder the IBinder
     */
    public static synchronized void removeWriters(IBinder ibinder)
    {
        /* To loops are necessary because we cannot remove items while iterating */
        final List<ConnectedWriter> toremove = new ArrayList<ConnectedWriter>();
        LOG.fine("Removing all sWriters from: " + ibinder);

        for (Entry<Long, ConnectedWriter> entry : sWriters.entrySet()) {
            final ConnectedWriter cw = entry.getValue();
            if (ibinder.equals(cw.mBinder)) {
                toremove.add(cw);
                LOG.fine("Found datawriter to remove: " + entry.getKey());
            }
        }
        for (ConnectedWriter cw : toremove) {
            cw.close();
        }
    }

    /**
     * Get a string that contains info of all connected writer.
     * 
     * @return the string containing the info of all connected writers
     */
    public static synchronized String printWriters()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("Registered sWriters:\n");
        for (ConnectedWriter cw : sWriters.values()) {
            sb.append(cw.toString() + "\n");
        }
        return sb.toString();
    }

    @Override
    public String toString()
    {
        return "CW: <" + this.mNativeWriter.toString() + "> created by <" + this.mBinder + ">";
    }

}
