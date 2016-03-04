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

package org.qeo.android.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.android.internal.IServiceQeoPolicyUpdateCallback;
import org.qeo.android.internal.IServiceQeoReaderCallback;
import org.qeo.android.internal.LooperThread;
import org.qeo.android.internal.ParcelableData;
import org.qeo.android.internal.ParcelableException;
import org.qeo.android.internal.ParcelableFilter;
import org.qeo.android.internal.ParcelableSampleInfo;
import org.qeo.android.internal.ParcelableType;
import org.qeo.android.internal.QeoParceler;
import org.qeo.exception.QeoException;
import org.qeo.internal.BaseFactory;
import org.qeo.internal.common.EntityType;
import org.qeo.internal.common.ObjectData;
import org.qeo.internal.common.ReaderListener;
import org.qeo.internal.common.SampleInfo;
import org.qeo.jni.NativeReader;

import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;

/**
 * the connected reader is actually the counter part of the service reader on the Qeo library for Android. So all reader
 * related method calls done from the Qeo library for Android to the Qeo service are being handled by this class.
 */
public final class ConnectedReader
{
    private static final Logger LOG = Logger.getLogger(QeoService.TAG + ":ConnectedReader");
    private static final int MSG_ON_UPDATE = 1;
    private static final int MSG_ON_DATA = 2;
    private static final int MSG_ON_NO_MORE_DATA = 3;
    private static final int MSG_ON_REMOVE = 4;
    private static final int MSG_ON_EXCEPTION = 5;
    private static final int MSG_ON_READ_OR_TAKE = 6;
    private static final String KEY_DATA = "data";
    private static final String KEY_EXCEPTION = "exception";
    private final NativeReader mReader;
    private final ConnectedReaderListener mReaderListener;
    private final ConnectedPolicyUpdateListener mPolicyUpdateListener;
    private final IBinder mBinder;
    private final long mReaderId;
    private final ReaderHandler mCallbackHandler;
    private static final LooperThread CALLBACK_THREAD = new LooperThread();

    /**
     * This hash-table keeps all the created mReader along with the mReader id from DDS.
     */
    private static Map<Long, ConnectedReader> sReaders = new HashMap<Long, ConnectedReader>();

    /**
     * Constructor of the connected mReader.
     * 
     * @param factory the native factory where the writer will be created
     * @param type The type to be used by this mReader
     * @param listener The data callback interface towards the application
     * @param policyListener The policy callback interface towards the application
     * @param readerType The type of mReader (e.g. state or event)
     * @param ib the IBinder
     * @throws QeoException if getWriter fails
     * 
     */
    private ConnectedReader(BaseFactory factory, ParcelableType type, IServiceQeoReaderCallback listener,
        IServiceQeoPolicyUpdateCallback policyListener, EntityType readerType, IBinder ib)
        throws QeoException
    {
        synchronized (CALLBACK_THREAD) {
            // check if the handler for callbacks is created
            // will only be done for the very first ConnectedReader
            if (!CALLBACK_THREAD.isInit()) {
                // start thread
                CALLBACK_THREAD.start();
                // wait to be sure the callbackHandler is created
                CALLBACK_THREAD.waitForInit();
            }
        }
        // create handler
        mCallbackHandler = new ReaderHandler(CALLBACK_THREAD.getLooper(), listener);
        mBinder = ib;
        mReaderListener = (null == listener ? null : new ConnectedReaderListener());
        mPolicyUpdateListener = (null == policyListener ? null : new ConnectedPolicyUpdateListener(policyListener));
        mReader =
            (NativeReader) factory.getEntityAccessor().getReader(type.getType(), readerType, mReaderListener,
                mPolicyUpdateListener);
        mReaderId = mReader.getNativeReader();
    }

    private void sendDataMsg(int msgId, ObjectData data)
    {
        final Bundle bundle = new Bundle();
        final Message msg = mCallbackHandler.obtainMessage(msgId);
        ParcelableData pd = new ParcelableData(data);
        bundle.putParcelable(KEY_DATA, pd);
        msg.setData(bundle);
        mCallbackHandler.sendMessage(msg);
    }

    /**
     * This class implements the ReaderListener for a connected mReader. It will use the IServiceQeoReaderCallback AIDL
     * interface to communicate to the application listening
     */
    private final class ConnectedReaderListener
        implements ReaderListener
    {

        /**
         * Default constructor.
         */
        private ConnectedReaderListener()
        {
            super();
        }

        /**
         * there is new data available. Forward this indication to the application so iteration can be done again.
         */
        @Override
        public void onUpdate()
        {
            LOG.finer("onUpdate");
            final Message msg = mCallbackHandler.obtainMessage(MSG_ON_UPDATE);
            mCallbackHandler.sendMessage(msg);
        }

        /**
         * New data has been received. Forward the data to the application.
         * 
         * @param sample The data received
         */
        @Override
        public void onData(final ObjectData sample)
        {
            LOG.finer("onData: " + sample);
            sendDataMsg(MSG_ON_DATA, sample);
        }

        /**
         * there is no more data received. Forward this indication to the application.
         */
        @Override
        public void onNoMoreData()
        {
            LOG.finer("onNoMoreData");
            final Message msg = mCallbackHandler.obtainMessage(MSG_ON_NO_MORE_DATA);
            mCallbackHandler.sendMessage(msg);
        }

        /**
         * Data has been removed. Forward it to the application.
         * 
         * @param sample The data received
         */
        @Override
        public void onRemove(final ObjectData sample)
        {
            LOG.finer("onRemove: " + sample);
            sendDataMsg(MSG_ON_REMOVE, sample);
        }

        @Override
        public void close()
        {
            // does nothing
        }

        @Override
        public boolean isClosed()
        {
            return false;
        }

        @Override
        public void onError(RuntimeException ex)
        {
            final Bundle bundle = new Bundle();
            final Message msg = mCallbackHandler.obtainMessage(MSG_ON_EXCEPTION);
            ParcelableException pex = new ParcelableException(ex);
            bundle.putParcelable(KEY_EXCEPTION, pex);
            msg.setData(bundle);
            mCallbackHandler.sendMessage(msg);
        }

    }

    /**
     * Add a new connected mReader and put the mReader and its id in the sReaders hash-table.
     * 
     * @param factory the factory where the reader will be created
     * @param type The type to be used by this mReader
     * @param listener The data callback interface towards the application
     * @param policyListener The policy callback interface towards the application
     * @param readerType The type of mReader (e.g. state or event)
     * @param binder the IBinder
     * 
     * @return the mReader id or -1 in case of an exception
     */
    public static synchronized long addReader(BaseFactory factory, ParcelableType type,
        IServiceQeoReaderCallback listener, IServiceQeoPolicyUpdateCallback policyListener, EntityType readerType,
        IBinder binder)
    {
        // Reader creation/deletion is done while holding class lock. This ensures they don't get created/deleted from
        // multiple threads.
        try {
            ConnectedReader cr = new ConnectedReader(factory, type, listener, policyListener, readerType, binder);
            sReaders.put(cr.mReader.getNativeReader(), cr);
            LOG.fine("addReader " + readerType + " " + cr.mReader.getNativeReader() + " for type " + type.getType()
                + " from activity " + binder.toString());
            return cr.mReader.getNativeReader();
        }
        catch (QeoException e) {
            LOG.warning("addWriter " + readerType + " failed for type " + type.getType() + " from activity "
                + binder.toString());
        }
        return -1;
    }

    /**
     * Get a reader instance based on a readerId.
     * 
     * @param readerId The id of the reader.
     * @return The reader. Will throw an exception if not available.
     */
    public static synchronized ConnectedReader getReader(long readerId)
    {
        ConnectedReader cr = sReaders.get(readerId);
        if (cr == null) {
            throw new IllegalStateException("Could not find datareader " + readerId);
        }
        return cr;
    }

    /**
     * Delete (close) the connected mReader.
     * 
     */
    private void dispose()
    {
        LOG.fine("disposeReader " + mReaderId);
        mReader.close();
    }

    /**
     * Closes the mReader and removes it from the hash table.
     * 
     */
    public void close()
    {
        LOG.fine("removeReader " + mReaderId);
        synchronized (ConnectedReader.class) {
            sReaders.remove(mReaderId);
            dispose();
        }
        // close callbackhandler
        mCallbackHandler.close();
    }

    /**
     * Delete all sReaders and remove them all from the hash table.
     * 
     */
    public static synchronized void closeAll()
    {
        LOG.fine("closeAll readers");
        /* To loops are necessary because we cannot remove items while iterating */
        final List<ConnectedReader> toremove = new ArrayList<ConnectedReader>();
        toremove.addAll(sReaders.values());
        for (ConnectedReader cr : toremove) {
            cr.close();
        }
    }

    /**
     * Delete all sReaders originating from a client.
     * 
     * @param ibinder the ibinder
     */
    public static synchronized void removeReaders(IBinder ibinder)
    {
        /* To loops are necessary because we cannot remove items while iterating */
        final List<ConnectedReader> toremove = new ArrayList<ConnectedReader>();
        LOG.fine("Removing all sReaders from: " + ibinder);

        for (Entry<Long, ConnectedReader> entry : sReaders.entrySet()) {
            final ConnectedReader cr = entry.getValue();
            if (ibinder.equals(cr.mBinder)) {
                toremove.add(cr);
                LOG.fine("Found datareader to remove: " + entry.getKey());
            }
        }
        for (ConnectedReader cr : toremove) {
            cr.close();
        }
    }

    /**
     * Take data from the mReader represented by dataReader based on the info in filter. sampleInfo and data are
     * actually return parameters. They contain the data requested by the mReader.
     * 
     * @param filter The filter information used to take new data
     * @param sampleInfo The returned sampleInfo
     * 
     * @return 1 if there is data, 0 if there is no data.
     */
    public int take(ParcelableFilter filter, ParcelableSampleInfo sampleInfo)
    {
        final SampleInfo info = new SampleInfo();
        ObjectData data = mReader.take(filter.getFilter(), info);
        sampleInfo.setInfo(info);
        LOG.fine("take :" + data + " on " + mReaderId);
        if (data != null) {
            sendDataMsg(MSG_ON_READ_OR_TAKE, data);
            return 1;
        }
        else {
            return 0;
        }
    }

    /**
     * Read data from the mReader represented by dataReader based on the info in filter. sampleInfo and data are
     * actually return parameters. They contain the data requested by the mReader.
     * 
     * @param filter The filter information used to read new data
     * @param sampleInfo The returned sampleInfo
     * 
     * @return 1 if there is data, 0 if there is no data.
     */
    public int read(ParcelableFilter filter, ParcelableSampleInfo sampleInfo)
    {
        final SampleInfo info = new SampleInfo();
        ObjectData data = mReader.read(filter.getFilter(), info);
        sampleInfo.setInfo(info);
        LOG.fine("read :" + data + " on " + mReaderId);
        if (data != null) {
            sendDataMsg(MSG_ON_READ_OR_TAKE, data);
            return 1;
        }
        else {
            return 0;
        }
    }

    /**
     * Trigger a policy update.
     */
    public void updatePolicy()
    {
        mReader.updatePolicy();
    }

    /**
     * Enables or disables background notifications for a reader.
     * 
     * @param enabled True to enable or false to disable notifications.
     */
    public void setBackgroundNotification(boolean enabled)
    {
        mReader.setBackgroundNotification(enabled);
    }

    /**
     * Get a string that contains info of all connected readers.
     * 
     * @return the string containing the info of all connected readers
     */
    public static synchronized String printReaders()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("Registered sReaders:\n");
        for (ConnectedReader cr : sReaders.values()) {
            sb.append(cr.toString() + "\n");
        }
        return sb.toString();
    }

    @Override
    public String toString()
    {
        return "CR: <" + mReader.toString() + "> created by <" + mBinder + ">";
    }

    /**
     * dispatch sending to the client to the background in order to release dds core thread this also has the advantage
     * that sending data to multiple readers/apps will be done sequential and hence reducing the risk of getting a
     * TransactionTooLargeException.
     */
    private static class ReaderHandler
        extends Handler
        implements QeoParceler.SplitCallbacks
    {
        private final QeoParceler mQeoParceler;
        private final IServiceQeoReaderCallback mListener;
        private boolean mIsClosed;

        public ReaderHandler(Looper looper, IServiceQeoReaderCallback listener)
        {
            super(looper);
            mListener = listener;
            mQeoParceler = new QeoParceler(this);
            mIsClosed = false;
        }

        @Override
        public void handleMessage(Message msg)
        {
            LOG.fine("handle message callback");
            if (mIsClosed) {
                LOG.fine("Reader already closed, ignoring callback");
                return;
            }
            final Bundle bundle = msg.getData();
            final ParcelableData data = bundle.getParcelable(KEY_DATA);
            try {
                switch (msg.what) {
                    case MSG_ON_UPDATE:
                        mListener.onUpdate();
                        break;
                    case MSG_ON_DATA:
                        mQeoParceler.split(QeoParceler.ID_ON_DATA, data);
                        break;
                    case MSG_ON_NO_MORE_DATA:
                        mListener.onNoMoreData();
                        break;
                    case MSG_ON_REMOVE:
                        mQeoParceler.split(QeoParceler.ID_ON_REMOVE, data);
                        break;
                    case MSG_ON_READ_OR_TAKE:
                        mQeoParceler.split(QeoParceler.ID_ON_READ_OR_TAKE, data);
                        break;
                    case MSG_ON_EXCEPTION:
                        ParcelableException pex = bundle.getParcelable(KEY_EXCEPTION);
                        mListener.onException(pex);
                        break;
                    default:
                        throw new IllegalStateException("Can't handle msg " + msg.what);
                }
            }
            catch (DeadObjectException ex) {
                // can happen if client was force closed but service did not notice it yet
                // (happens in different threads)
                LOG.finest("dead object posting to reader - ignoring");
            }
            catch (RemoteException ex) {
                LOG.log(Level.SEVERE, "Remote error on msg type " + msg.what, ex);
            }
        }

        @Override
        public void onWriteFragment(int id, boolean firstBlock, boolean lastBlock, int totalSize, byte[] data,
            ParcelableException exception)
            throws RemoteException
        {
            // called by mQeoParceler.split()
            mListener.onData(id, firstBlock, lastBlock, totalSize, data);
        }

        public void close()
        {
            mIsClosed = true;
        }
    }
}
