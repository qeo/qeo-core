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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.android.QeoAndroid;
import org.qeo.exception.OutOfResourcesException;
import org.qeo.exception.QeoException;
import org.qeo.internal.common.EntityType;
import org.qeo.internal.common.ObjectData;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.common.WriterEntity;
import org.qeo.policy.PolicyUpdateListener;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Looper;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;

/**
 * The service writer class is responsible for handling all writer related communication between the Qeo library for
 * Android and the Qeo service.
 */
public class ServiceWriter
    extends WriterEntity
    implements QeoParceler.SplitCallbacks
{
    private static final String TAG = "ServiceWriter";
    private static final Logger LOG = Logger.getLogger(QeoAndroid.TAG + "." + TAG);
    private Boolean mClosed = false;
    private long mDataWriter = 0;
    private final QeoParceler mQeoParceler;
    private IServiceQeoV1 mProxy;
    private QeoConnection mQeoConnection;
    private static final int ID_WRITE = 0;
    private static final int ID_REMOVE = 1;

    /**
     * Create a new service writer.
     * 
     * @param qeo The service connection
     * @param looper The looper to post callbacks.
     * @param id The identity for which to create a reader
     * @param type The type for which to create a writer
     * @param etype The type of reader to create
     * @param policyListener An (optional) policy update listener to attach to the reader
     * 
     * @throws QeoException if the remote service is unreachable
     */
    public ServiceWriter(QeoConnection qeo, Looper looper, int id, ObjectType type, EntityType etype,
        PolicyUpdateListener policyListener)
        throws QeoException
    {
        super(type, policyListener);
        try {
            mQeoParceler = new QeoParceler(this);
            mQeoConnection = qeo;
            mProxy = qeo.getProxy();
            final ParcelableException exception = new ParcelableException();

            PolicyUpdateHandler policyUpdateHandler = null;
            if (policyListener != null) {
                policyUpdateHandler = new PolicyUpdateHandler(looper, policyListener);
            }
            mDataWriter =
                mProxy.createWriter(qeo.getServiceQeoCallback(), id, new ParcelableType(type), policyUpdateHandler,
                    etype.name(), exception);
            ExceptionTranslator.handleServiceException(exception);
            LOG.fine("Service writer created " + mDataWriter);
        }
        catch (final RemoteException e) {
            // In this case the service has crashed before we could even
            // do anything with it; we can count on soon being
            // disconnected (and then reconnected if it can be restarted)
            // so there is no need to do anything here.
            throw new QeoException("Service unreachable", e);
        }
    }

    @SuppressLint("NewApi")
    private void checkTransactionTooLarge(RemoteException e)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) { // 4.0.3, api 15
            // TransactionTooLargeException is only available on api 15 and up.
            // Catching of this exception is written like this to avoid problems on android < 15
            if (e instanceof TransactionTooLargeException) {
                throw new OutOfResourcesException("parcel too big", e);
            }
        }
    }

    private void checkClosed()
    {
        if (mClosed) {
            throw new IllegalStateException("Writer already closed");
        }
    }

    @Override
    public synchronized void write(ObjectData data)
    {
        LOG.fine("Writing from writer " + mDataWriter);
        checkClosed();
        // note: multiple threads/writers writing in parallel can cause a TransactionTooLargeException here
        // This can be solved by taking a static lock here, but at the impact of performance
        // Since this is not considered a very realistic approach it's not done.
        try {
            // this will call onWriteFragment() possibly multiple times
            ParcelableException exception = mQeoParceler.split(ID_WRITE, data);
            ExceptionTranslator.handleServiceException(exception);
        }
        catch (final RemoteException e) {
            checkTransactionTooLarge(e);
            LOG.log(Level.SEVERE, "Error writing", e);
        }
    }

    @Override
    public void onWriteFragment(int id, boolean firstBlock, boolean lastBlock, int totalSize, byte[] data,
        ParcelableException exception)
        throws RemoteException
    {
        // called by mQeoParceler.split()
        switch (id) {
            case ID_WRITE:
                mProxy.write(mDataWriter, firstBlock, lastBlock, totalSize, data, exception);
                break;
            case ID_REMOVE:
                mProxy.remove(mDataWriter, firstBlock, lastBlock, totalSize, data, exception);
                break;
            default:
                throw new IllegalStateException("Can't handle this id");
        }
    }

    @Override
    public synchronized void remove(ObjectData data)
    {
        LOG.fine("Removing from writer " + mDataWriter);
        checkClosed();
        if (mClosed) {
            LOG.warning("Trying to remove to closed writer: " + mDataWriter);
            return;
        }
        try {
            // this will call writePart() possibly multiple times
            ParcelableException exception = mQeoParceler.split(ID_REMOVE, data);
            ExceptionTranslator.handleServiceException(exception);
        }
        catch (final RemoteException e) {
            checkTransactionTooLarge(e);
            LOG.log(Level.SEVERE, "Error removing", e);
        }
    }

    @Override
    public synchronized void updatePolicy()
    {
        checkClosed();
        try {
            final ParcelableException exception = new ParcelableException();

            LOG.fine("Update policy for writer " + mDataWriter);
            mProxy.updateWriterPolicy(mDataWriter, exception);
            ExceptionTranslator.handleServiceException(exception);
        }
        catch (final RemoteException e) {
            LOG.log(Level.SEVERE, "Failing updatePolicy", e);
        }
    }

    @Override
    public synchronized void close()
    {
        if (!mClosed) {
            mClosed = true;
            if (mDataWriter != 0) {
                try {
                    LOG.fine("Remove service writer " + mDataWriter);
                    if (mQeoConnection.isConnected()) {
                        // only close if the service is still there
                        mProxy.removeWriter(mDataWriter);
                    }
                }
                catch (final Exception e) {
                    // only log exceptions. We don't want the application to crash if the closed did not go well.
                    LOG.log(Level.SEVERE, "Error closing writer", e);
                }
            }
        }
    }

}
