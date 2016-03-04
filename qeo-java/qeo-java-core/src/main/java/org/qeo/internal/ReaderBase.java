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

package org.qeo.internal;

import org.qeo.EventReaderListener;
import org.qeo.Notifiable;
import org.qeo.QeoReader;
import org.qeo.QeoReaderListener;
import org.qeo.StateChangeReaderListener;
import org.qeo.StateReaderListener;
import org.qeo.exception.QeoException;
import org.qeo.internal.common.EntityType;
import org.qeo.internal.common.ObjectData;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.common.ReaderEntity;
import org.qeo.internal.common.ReaderFilter;
import org.qeo.internal.common.ReaderListener;
import org.qeo.internal.common.SampleInfo;
import org.qeo.internal.reflection.IntrospectionUtil;
import org.qeo.policy.PolicyUpdateListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base implementation of a reader wrapping a reflection based reader.
 *
 * @param <TYPE> The class of the Qeo data type.
 * @param <DATA> The type of the Qeo data.
 */
abstract class ReaderBase<TYPE, DATA> extends ReaderWriterBase implements QeoReader<DATA>, Notifiable
{
    private static final Logger LOG = Logger.getLogger(ReaderBase.class.getName());

    /** The type of the reader. */
    private final ObjectType mType;

    /** Reference to the lowlevel reader. */
    protected final ReaderEntity mReader;
    private final ReaderListener mListener;

    private final IntrospectionUtil<TYPE, DATA> mIntrospectionUtil;

    /**
     * Create an EventReader/StateChangeReader.
     *
     * @param factoryHelper Helper class for factory management.
     * @param entityAccessor Entity accessor to be used for creating the reader.
     * @param introspectionUtil Util class to to the type introspection.
     * @param typedesc Description of the type.
     * @param etype The entity type.
     * @param listener The listener to be associated with the reader.
     * @param policyListener An (optional) policy update listener to attach to the reader
     *
     * @throws QeoException If the creation of the reader failed.
     */
    public ReaderBase(FactoryHelper factoryHelper, EntityAccessor entityAccessor, IntrospectionUtil<TYPE,
        DATA> introspectionUtil, TYPE typedesc, EntityType etype, QeoReaderListener listener,
                      PolicyUpdateListener policyListener) throws QeoException
    {
        super(factoryHelper);
        this.mIntrospectionUtil = introspectionUtil;
        mType = introspectionUtil.typeFromTypedesc(typedesc);
        mListener = (null == listener ? null : new ReaderListenerImpl(listener));
        mReader = entityAccessor.getReader(mType, etype, mListener, policyListener);
    }

    /**
     * Read a sample from the reader using the provided filter and return the read data and sample information.
     *
     * @param filter The filter to be used while reading.
     * @param info The returned sample information.
     *
     * @return The data read.
     */
    public DATA read(ReaderFilter filter, SampleInfo info)
    {
        return mIntrospectionUtil.objectFromData(mReader.read(filter, info), mReader.getType());
    }

    /**
     * Take a sample from the reader using the provided filter and return the taken data and sample information.
     *
     * @param filter The filter to be used while taking.
     * @param info The returned sample information.
     *
     * @return The data taken.
     */
    public DATA take(ReaderFilter filter, SampleInfo info)
    {
        return mIntrospectionUtil.objectFromData(mReader.take(filter, info), mReader.getType());
    }

    @Override
    public void updatePolicy()
    {
        mReader.updatePolicy();
    }

    @Override
    public void setBackgroundNotification(boolean enabled)
    {
        mReader.setBackgroundNotification(enabled);
    }

    @Override
    public void close()
    {
        LOG.fine("Closing reader: " + this);
        if (mReader != null) {
            mReader.close();
        }
        super.close();
    }

    /**
     * Get the type name of the reader.
     *
     * @return The type name.
     */
    public String getName()
    {
        return this.mReader.getType().getName();
    }

    private class ReaderListenerImpl implements ReaderListener
    {
        private final EventReaderListener<DATA> mEventReaderListener;
        private final StateChangeReaderListener<DATA> mStateChangeReaderListener;
        private final StateReaderListener mStateReaderListener;
        private boolean mClosed = false;

        @SuppressWarnings("unchecked")
        public ReaderListenerImpl(QeoReaderListener listener)
        {
            if (listener instanceof EventReaderListener<?>) {
                mEventReaderListener = (EventReaderListener<DATA>) listener;
            }
            else {
                mEventReaderListener = null;
            }
            if (listener instanceof StateChangeReaderListener<?>) {
                mStateChangeReaderListener = (StateChangeReaderListener<DATA>) listener;
            }
            else {
                mStateChangeReaderListener = null;
            }
            if (listener instanceof StateReaderListener) {
                mStateReaderListener = (StateReaderListener) listener;
            }
            else {
                mStateReaderListener = null;
            }
        }

        @Override
        public void onUpdate()
        {
            if (mStateReaderListener != null) {
                mStateReaderListener.onUpdate();
            }
        }

        @Override
        public void onData(ObjectData data)
        {
            if (null != mEventReaderListener) {
                DATA processedData = null;
                try {
                    processedData = mIntrospectionUtil.objectFromData(data, mType);
                }
                catch (RuntimeException e) {
                    // don't throw exception here. This is a callback and won't get to the end-user
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.WARNING, "Error calling onData: ", e);
                    }
                    else {
                        LOG.warning(e.getMessage());
                    }
                }
                mEventReaderListener.onData(processedData);
            }
        }

        @Override
        public void onNoMoreData()
        {
            if (null != mEventReaderListener) {
                mEventReaderListener.onNoMoreData();
            }
        }

        @Override
        public void onRemove(ObjectData data)
        {
            if (null != mStateChangeReaderListener) {
                mStateChangeReaderListener.onRemove(mIntrospectionUtil.objectFromData(data, mType));
            }
        }

        @Override
        public boolean isClosed()
        {
            return mClosed;
        }

        @Override
        public void close()
        {
            mClosed = true;
        }

        @Override
        public void onError(RuntimeException ex)
        {
            // Exception is set. It's not possible to throw this to the client as this is a callback.
            // But print the logging here. This should in android also make sure the logging happens at the client
            // side and not the service.
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.WARNING, "Error in native reader: ", ex);
            }
            else {
                LOG.warning("Error in native reader: " + ex.getMessage());
            }
        }
    }
}
