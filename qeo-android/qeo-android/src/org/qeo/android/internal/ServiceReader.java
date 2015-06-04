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

import java.io.Closeable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.android.QeoAndroid;
import org.qeo.exception.QeoException;
import org.qeo.internal.common.EntityType;
import org.qeo.internal.common.ObjectData;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.common.ReaderEntity;
import org.qeo.internal.common.ReaderFilter;
import org.qeo.internal.common.ReaderListener;
import org.qeo.internal.common.SampleInfo;
import org.qeo.policy.PolicyUpdateListener;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;

/**
 * The service reader class is responsible for handling all reader related communication between the Qeo library for
 * Android and the Qeo service.
 */
public class ServiceReader
    extends ReaderEntity
    implements Closeable
{
    private static final String TAG = "ServiceReader";
    private static final Logger LOG = Logger.getLogger(QeoAndroid.TAG + "." + TAG);

    /** Indication that this reader is closed. */
    private Boolean mClosed = false;
    /** The native data reader id. */
    protected long mDataReader = 0;
    /**
     * To make sure the reader callbacks are called in the same thread as where this reader class has been created, we
     * need a new Handler that can handle the messages send from the IServiceQeoReaderCallback and
     * IServiceQeoPolicyUpdateCallback interfaces.
     */
    private ReaderHandler mHandler = null;

    private final ReaderCallbacks mServiceQeoCb;
    private final QeoConnection mQeoConnection;
    private final IServiceQeoV1 mProxy;

    /** Temporary storage for an object in read/take call. */
    private ObjectData mReadOrTakeData;
    /** Semaphore to unlock read/take call. */
    private final Semaphore mReadOrTakeSemaphore;
    private final Object mReadOrTakeLock = new Object();

    /**
     * Handler for notifications of the AIDL thread. By using this handler we can make sure that the listener callbacks
     * to the user will be called in the thread where the user instantiated its reader (if looper is null) or in a
     * specific thread (if loop is set).
     */
    private static class ReaderHandler
        extends Handler
    {
        private final ReaderListener mListener;

        ReaderHandler(ReaderListener listener)
        {
            super();
            this.mListener = listener;
        }

        ReaderHandler(Looper looper, ReaderListener listener)
        {
            super(looper);
            this.mListener = listener;
        }

        @Override
        public void handleMessage(Message msg)
        {
            LOG.fine("handle message callback");
            if (mListener == null) {
                LOG.fine("no listener, ignoring callback");
                return;
            }
            if (mListener.isClosed()) {
                LOG.fine("Listener already closed, ignoring callback");
                return;
            }
            final Bundle bundle = msg.getData();
            final ParcelableData data = bundle.getParcelable(MessageUtil.KEY_DATA);
            switch (msg.what) {
                case MessageUtil.MSG_UPDATE:
                    LOG.fine("handle update callback");
                    mListener.onUpdate();
                    break;
                case MessageUtil.MSG_DATA:
                    LOG.fine("handle data callback");
                    mListener.onData(data.getData());
                    break;
                case MessageUtil.MSG_REMOVE:
                    LOG.fine("handle remove callback");
                    mListener.onRemove(data.getData());
                    break;
                case MessageUtil.MSG_NO_MORE_DATA:
                    LOG.fine("handle no more data callback");
                    mListener.onNoMoreData();
                    break;
                case MessageUtil.MSG_EXCEPTION:
                    LOG.fine("handle error callback");
                    ParcelableException pex = bundle.getParcelable(MessageUtil.KEY_EXCEPTION);
                    mListener.onError(pex.getException());
                    break;
                default:
                    throw new IllegalStateException("Can't handle msg " + msg.what);
            }
        }
    }

    /**
     * Create a new service reader.
     * 
     * @param qeo The service connection
     * @param looper The looper to post callbacks.
     * @param id The identity for which to create a reader
     * @param type The type for which to create a reader
     * @param etype The type of reader to create
     * @param listener An (optional) listener to attach to the reader
     * @param policyListener An (optional) policy update listener to attach to the writer
     * 
     * @throws QeoException if the remote service is unreachable
     */
    public ServiceReader(QeoConnection qeo, Looper looper, int id, ObjectType type, EntityType etype,
        final ReaderListener listener, final PolicyUpdateListener policyListener)
        throws QeoException
    {
        super(type, listener, policyListener);
        mServiceQeoCb = new ReaderCallbacks();
        mReadOrTakeSemaphore = new Semaphore(0);
        try {
            mQeoConnection = qeo;
            mProxy = qeo.getProxy();
            final ParcelableException exception = new ParcelableException();

            if (null == looper) {
                mHandler = new ReaderHandler(listener);
            }
            else {
                mHandler = new ReaderHandler(looper, listener);
            }
            PolicyUpdateHandler policyUpdateHandler = null;
            if (policyListener != null) {
                policyUpdateHandler = new PolicyUpdateHandler(looper, policyListener);
            }
            mDataReader =
                mProxy.createReader(qeo.getServiceQeoCallback(), id, new ParcelableType(type), mServiceQeoCb,
                    policyUpdateHandler, etype.name(), exception);
            ExceptionTranslator.handleServiceException(exception);
            LOG.fine("Service reader created " + mDataReader);
        }
        catch (final RemoteException e) {
            // In this case the service has crashed before we could even
            // do anything with it; we can count on soon being
            // disconnected (and then reconnected if it can be restarted)
            // so there is no need to do anything here.
            throw new QeoException("Service unreachable", e);
        }
    }

    @Override
    public synchronized void close()
    {
        if (!mClosed) {
            // Mark reader as closed
            mClosed = true;

            // close the listener to avoid further callbacks
            if (mHandler != null && mHandler.mListener != null) {
                mHandler.mListener.close();
            }

            if (mDataReader != 0) {
                try {
                    LOG.fine("Remove service reader " + mDataReader);
                    if (mQeoConnection.isConnected()) {
                        // only close if the service is still there
                        mProxy.removeReader(mDataReader);
                    }
                    mDataReader = 0;
                }
                catch (final Exception e) {
                    // only log exceptions. We don't want the application to crash if the closed did not go well.
                    LOG.log(Level.SEVERE, "Error closing reader", e);
                }
            }
        }
    }

    /**
     * Performs the read/take towards the Qeo service. The returned data from the service is transformed into an new
     * object of class Data.
     * 
     * @param filter
     * @param sampleInfo
     * @param take
     * 
     * @return the returned data represented by Data
     * @throws RemoteException
     */
    private ObjectData readOrTake(ReaderFilter filter, SampleInfo sampleInfo, boolean take)
        throws RemoteException
    {
        if (mClosed) {
            LOG.warning("Trying to read from closed reader: " + mDataReader);
            return null;
        }
        final ParcelableException exception = new ParcelableException();
        final ParcelableSampleInfo info = new ParcelableSampleInfo(sampleInfo);

        ObjectData data;
        // take the readOrTakeLock
        // 2 parallel read/takes would both write to the same global mReadOrTakeData variable
        // this lock ensures there are no races possible.
        synchronized (mReadOrTakeLock) {
            mReadOrTakeSemaphore.drainPermits(); // reset the semaphore
            int result;
            // The read/take call will be blocking and read the data from dds.
            // The transfer of the data itself from the service to the app will be in callbacks in order to be able to
            // fragment it in small blocks
            if (take) {
                LOG.fine("Take from reader " + mDataReader);
                result = mProxy.take(mDataReader, new ParcelableFilter(filter), info, exception);
            }
            else {
                LOG.fine("Read from reader " + mDataReader);
                result = mProxy.read(mDataReader, new ParcelableFilter(filter), info, exception);
            }
            if (result == 1) {
                // there is data, it is going to be stored in the readOrTakeData variable.
                try {
                    // wait for all data callbacks to arrive and fragment to be reassembled
                    if (mReadOrTakeSemaphore.tryAcquire(1, TimeUnit.MINUTES)) {
                        data = mReadOrTakeData;
                        mReadOrTakeData = null; // release memory
                    }
                    else {
                        throw new RuntimeException("Timeout reading from service");
                    }
                }
                catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while reading from service");
                }
            }
            else {
                // no data, just make empty
                // no need to wait for the semaphore as there won't be any data callbacks.
                data = null;
            }
        }
        ExceptionTranslator.handleServiceException(exception);
        sampleInfo.setInstanceHandle(info.getInfo().getInstanceHandle());
        return data;
    }

    private void checkClosed()
    {
        if (mClosed) {
            throw new IllegalStateException("Reader already closed");
        }
    }

    @Override
    public final synchronized ObjectData read(ReaderFilter filter, SampleInfo sampleInfo)
    {
        checkClosed();
        try {
            return readOrTake(filter, sampleInfo, false);
        }
        catch (final RemoteException e) {
            LOG.log(Level.SEVERE, "Failing read", e);
        }
        return null;
    }

    @Override
    public final synchronized ObjectData take(ReaderFilter filter, SampleInfo sampleInfo)
    {
        checkClosed();
        try {
            return readOrTake(filter, sampleInfo, true);
        }
        catch (final RemoteException e) {
            LOG.log(Level.SEVERE, "Failing take", e);
        }
        return null;
    }

    @Override
    public synchronized void updatePolicy()
    {
        checkClosed();
        try {
            final ParcelableException exception = new ParcelableException();

            LOG.fine("Update policy for reader " + mDataReader);
            mProxy.updateReaderPolicy(mDataReader, exception);
            ExceptionTranslator.handleServiceException(exception);
        }
        catch (final RemoteException e) {
            LOG.log(Level.SEVERE, "Failing updatePolicy", e);
        }
    }

    @Override
    public synchronized void setBackgroundNotification(boolean enabled)
    {
        checkClosed();
        try {
            mProxy.setReaderBackgroundNotification(mDataReader, enabled);
        }
        catch (final RemoteException e) {
            LOG.log(Level.SEVERE, "Failing setBackgroundNotification", e);
        }
    }

    /**
     * This is the actual implementation of the IServiceQeoReaderCallback AIDL interface.
     */
    // IServiceQeoReaderCallback.Stub mServiceQeoCb = new IServiceQeoReaderCallback.Stub() {
    private class ReaderCallbacks
        extends IServiceQeoReaderCallback.Stub
        implements QeoParceler.JoinCallbacks
    {

        private final QeoParceler mQeoParceler;

        public ReaderCallbacks()
        {
            mQeoParceler = new QeoParceler(this);
        }

        @Override
        public void onNoMoreData()
            throws RemoteException
        {
            LOG.fine("no more data callback");
            final Message msg = mHandler.obtainMessage(MessageUtil.MSG_NO_MORE_DATA);
            mHandler.sendMessage(msg);
        }

        @Override
        public void onData(int type, boolean firstBlock, boolean lastBlock, int totalSize, byte[] data)
            throws RemoteException
        {
            // will call onFragmentsJoined() when an object is created.
            mQeoParceler.join(type, firstBlock, lastBlock, totalSize, data);
        }

        @Override
        public void onFragmentsJoined(int id, ParcelableData data)
        {
            // called by mQeoParceler.join()
            LOG.finest("objectCreated: " + id);
            int msgId;
            switch (id) {
                case QeoParceler.ID_ON_DATA: {
                    msgId = MessageUtil.MSG_DATA;
                    break;
                }
                case QeoParceler.ID_ON_REMOVE: {
                    msgId = MessageUtil.MSG_REMOVE;
                    break;
                }
                case QeoParceler.ID_ON_READ_OR_TAKE: {
                    // callback for read/take.
                    // store data in global variable and release the lock to release read/take thread.
                    mReadOrTakeData = data.getData();
                    mReadOrTakeSemaphore.release();
                    return;
                }
                default:
                    throw new IllegalStateException("Can't handle this id");
            }
            final Bundle bundle = new Bundle();
            final Message msg = mHandler.obtainMessage(msgId);
            bundle.putParcelable(MessageUtil.KEY_DATA, data);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }

        @Override
        public void onException(ParcelableException ex)
            throws RemoteException
        {
            LOG.fine("onException callback");
            final Bundle bundle = new Bundle();
            final Message msg = mHandler.obtainMessage(MessageUtil.MSG_EXCEPTION);
            bundle.putParcelable(MessageUtil.KEY_EXCEPTION, ex);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }

        @Override
        public void onUpdate()
            throws RemoteException
        {
            LOG.fine("onUpdate callback");
            final Message msg = mHandler.obtainMessage(MessageUtil.MSG_UPDATE);
            mHandler.sendMessage(msg);
        }

    };

}
