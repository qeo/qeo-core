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

import java.util.logging.Logger;

import org.qeo.internal.common.ObjectData;

import android.os.Parcel;
import android.os.RemoteException;

/**
 * Utility class to fragment parcels between service and app.
 */
public class QeoParceler
{
    private static final Logger LOG = Logger.getLogger("QeoParceler");

    /** ID to indicate ON_DATA call. */
    public static final int ID_ON_DATA = 0;
    /** ID to indicate ON_REMOVE call. */
    public static final int ID_ON_REMOVE = 1;
    /** ID to indicate ON_READ_OR_TAKE call. */
    public static final int ID_ON_READ_OR_TAKE = 2;

    /** Data size (in bytes) that parcels will be fragmented in. */
    private static final int PARCEL_DATA_SIZE = 128 * 1024; // 128KB
    private int mCurrentSize;
    private byte[] mAllData;
    private final SplitCallbacks mSplitCallbacks;
    private final JoinCallbacks mJoinCallbacks;

    /**
     * Create a QeoParceler that can join fragments.
     * 
     * @param callbacks The callbacks that will be called if a packet is fully reconstructed.
     */
    public QeoParceler(JoinCallbacks callbacks)
    {
        mJoinCallbacks = callbacks;
        mSplitCallbacks = null;
    }

    /**
     * Create a QeoParceler that can split data into fragments.
     * 
     * @param callbacks The callbacks that will be called for each fragment.
     */
    public QeoParceler(SplitCallbacks callbacks)
    {
        mJoinCallbacks = null;
        mSplitCallbacks = callbacks;
    }

    /**
     * Join fragments into a ParcelableData object.<br/>
     * Fragments have to be passed in order.<br/>
     * Fragments from different readers/ids can't be mixed. One ParcelableData object should be fully reconstructed
     * before starting reconstruction on a 2nd one.<br/>
     * It will call JoinCallbacks.partCreated() if an object is created.
     * 
     * @param id An id that can be passed. This will be passed to the partCreated() callback.
     * @param firstBlock Indicate that this is the first block.
     * @param lastBlock Indicate that this is the last block.
     * @param totalSize The total size of the data to be joined.
     * @param data The data block.
     */
    public void join(int id, boolean firstBlock, boolean lastBlock, int totalSize, byte[] data)
    {
        // the unmarshall function of parcel needs to have the complete byte array at once, so reconstruct big array
        // first
        int dataSize = data.length;
        if (firstBlock) {
            // first block, assign memory for the complete array.
            LOG.finest("Creating new parcel for writer");
            mAllData = new byte[totalSize];
            mCurrentSize = 0;
        }
        // put the next block of data in the big array
        System.arraycopy(data, 0, mAllData, mCurrentSize, dataSize);
        data = null;
        LOG.finest("Unmarshall " + dataSize + " bytes at position " + mCurrentSize);

        mCurrentSize += dataSize;
        if (lastBlock) {
            // this was the last block, so reconstruct parcel
            Parcel parcel;
            // get a parcel
            parcel = Parcel.obtain();
            // unmarshall data
            parcel.unmarshall(mAllData, 0, mCurrentSize);
            // free memory, big array is not needed anymore now.
            mAllData = null;
            // reset parcel position, very important
            parcel.setDataPosition(0);

            // create ParcelabelData from parcel
            ParcelableData pd = ParcelableData.CREATOR.createFromParcel(parcel);
            // give parcel object back to os.
            parcel.recycle();

            // part created, call callback to let it handle it.
            mJoinCallbacks.onFragmentsJoined(id, pd);
        }
    }

    /**
     * Split ObjectData objects into fragments (byte arrays).<br/>
     * It will call SplitCallbacks.writePart() for every fragment created.
     * 
     * @param id An id that can be passed. This will be passed to the writePart() callback.
     * @param data The data to be splitted
     * @return Will return a ParcelableException. It will contain an exception if something went wrong.
     * @throws RemoteException If sending data over the wire fails.
     */
    public ParcelableException split(int id, ObjectData data)
        throws RemoteException
    {
        return split(id, new ParcelableData(data));
    }

    /**
     * Split ParcelableData objects into fragments (byte arrays).<br/>
     * It will call SplitCallbacks.writePart() for every fragment created.
     * 
     * @param id An id that can be passed. This will be passed to the writePart() callback.
     * @param pd The data to be splitted
     * @return Will return a ParcelableException. It will contain an exception if something went wrong.
     * @throws RemoteException If sending data over the wire fails.
     */
    public ParcelableException split(int id, ParcelableData pd)
        throws RemoteException
    {
        // get a parcel from the os.
        Parcel p = Parcel.obtain();
        // create the parcel.
        pd.writeToParcel(p, 0);
        pd = null;

        // query how big the parcel is
        int size = p.dataSize();
        LOG.finest("Writing " + size + " bytes");
        // create empty exception, might get filled if something wrong happens
        final ParcelableException exception = new ParcelableException();

        // create byte array from the parcel
        byte[] buf = p.marshall();
        // give parcel back to the os, no longer needed
        p.recycle();
        p = null;
        if (size < PARCEL_DATA_SIZE) {
            // can send in 1 go
            mSplitCallbacks.onWriteFragment(id, true, true, size, buf, exception);
        }
        else {
            // need to fragment

            int start = 0;
            boolean last = false;
            boolean first = true;
            int i = 0;
            // allocate buffer of parcel size to avoid having to create a new buffer in every iteration.
            byte[] buf2 = new byte[PARCEL_DATA_SIZE];
            do {
                i++;
                int blockSize = PARCEL_DATA_SIZE;
                if (i * PARCEL_DATA_SIZE >= size) {
                    // last block
                    blockSize = size - ((i - 1) * PARCEL_DATA_SIZE);
                    last = true;
                    buf2 = new byte[blockSize];
                }
                LOG.finest("Writing parcel " + i + "(" + start + " to " + (start + blockSize) + " of " + size + ")");
                // copy chunk of the big array into a small array
                System.arraycopy(buf, start, buf2, 0, blockSize);

                // call the callback with the created fragment.
                mSplitCallbacks.onWriteFragment(id, first, last, buf.length, buf2, exception);
                first = false;
                start += PARCEL_DATA_SIZE;
            }
            while (!last);
            buf2 = null;
        }
        buf = null;
        return exception;
    }

    /**
     * Interface to be used for splitting ObjectData into fragments.
     */
    public interface SplitCallbacks
    {
        /**
         * Will be called for every fragment created.
         * 
         * @param id The id passed to QeoParceler.split()
         * @param firstBlock Indicates that this is the first block.
         * @param lastBlock Indicated that this is the last block.
         * @param totalSize The total size of all the blocks.
         * @param data The block itself.
         * @param exception An exception that can be set by the remote end.
         * @throws RemoteException If writing over the wire fails.
         */
        void onWriteFragment(int id, boolean firstBlock, boolean lastBlock, int totalSize, byte[] data,
            ParcelableException exception)
            throws RemoteException;
    }

    /**
     * Interface to be used for joining fragments into ParcelableData.
     */
    public interface JoinCallbacks
    {
        /**
         * Will be called for every object created.
         * 
         * @param id the id passed to QeoParceler.join()
         * @param pd The data itself.
         */
        void onFragmentsJoined(int id, ParcelableData pd);
    }

}
