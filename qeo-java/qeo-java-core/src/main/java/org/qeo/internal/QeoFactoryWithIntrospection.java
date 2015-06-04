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

package org.qeo.internal;

import org.qeo.EventReader;
import org.qeo.EventReaderListener;
import org.qeo.EventWriter;
import org.qeo.StateChangeReader;
import org.qeo.StateChangeReaderListener;
import org.qeo.StateReader;
import org.qeo.StateReaderListener;
import org.qeo.StateWriter;
import org.qeo.exception.QeoException;
import org.qeo.internal.reflection.IntrospectionUtil;
import org.qeo.policy.PolicyUpdateListener;

/**
 * Implementation of the Custom Qeo Factory using the BaseFactory to handle reader/writer creation.
 *
 * @param <TYPE> The description of the data type
 * @param <DATA> The data type
 */
public class QeoFactoryWithIntrospection<TYPE, DATA> implements CustomQeoFactory<TYPE, DATA>
{
    private final EntityAccessor mEntityAccessor;
    private final IntrospectionUtil<TYPE, DATA> mIntrospectionUtil;
    private final FactoryHelper mFactoryHelper;

    /**
     * Create an instannce of the custom qeo factory.
     *
     * @param baseFactory The base factory.
     * @param introspectionUtil The implementation that will to the introspection.
     */
    public QeoFactoryWithIntrospection(BaseFactory baseFactory, IntrospectionUtil<TYPE, DATA> introspectionUtil)
    {
        mEntityAccessor = baseFactory.getEntityAccessor();
        mFactoryHelper = baseFactory.getFactoryHelper();
        mIntrospectionUtil = introspectionUtil;
    }

    @Override
    public EventReader<DATA> createEventReader(TYPE type, EventReaderListener<DATA> listener) throws QeoException
    {
        return new EventReaderImpl<TYPE, DATA>(mFactoryHelper, mEntityAccessor, mIntrospectionUtil, type, listener,
            null);
    }

    @Override
    public EventReader<DATA> createEventReader(TYPE type, EventReaderListener<DATA> listener,
                                               PolicyUpdateListener policyListener) throws QeoException
    {
        return new EventReaderImpl<TYPE, DATA>(mFactoryHelper, mEntityAccessor, mIntrospectionUtil, type, listener,
            policyListener);
    }

    @Override
    public EventWriter<DATA> createEventWriter(TYPE type) throws QeoException
    {
        return new EventWriterImpl<TYPE, DATA>(mFactoryHelper, mEntityAccessor, mIntrospectionUtil, type, null);
    }

    @Override
    public EventWriter<DATA> createEventWriter(TYPE type, PolicyUpdateListener policyListener) throws QeoException
    {
        return new EventWriterImpl<TYPE, DATA>(mFactoryHelper, mEntityAccessor, mIntrospectionUtil, type,
            policyListener);
    }

    @Override
    public StateReader<DATA> createStateReader(TYPE type, StateReaderListener listener) throws QeoException
    {
        return new StateReaderImpl<TYPE, DATA>(mFactoryHelper, mEntityAccessor, mIntrospectionUtil, type, listener,
            null);
    }

    @Override
    public StateReader<DATA> createStateReader(TYPE type, StateReaderListener listener,
                                               PolicyUpdateListener policyListener) throws QeoException
    {
        return new StateReaderImpl<TYPE, DATA>(mFactoryHelper, mEntityAccessor, mIntrospectionUtil, type, listener,
            policyListener);
    }

    @Override
    public StateChangeReader<DATA> createStateChangeReader(TYPE type, StateChangeReaderListener<DATA> listener)
        throws QeoException
    {
        return new StateChangeReaderImpl<TYPE, DATA>(mFactoryHelper, mEntityAccessor, mIntrospectionUtil, type,
            listener, null);
    }

    @Override
    public StateChangeReader<DATA> createStateChangeReader(TYPE type, StateChangeReaderListener<DATA> listener,
                                                           PolicyUpdateListener policyListener) throws QeoException
    {
        return new StateChangeReaderImpl<TYPE, DATA>(mFactoryHelper, mEntityAccessor, mIntrospectionUtil, type,
            listener, policyListener);
    }

    @Override
    public StateWriter<DATA> createStateWriter(TYPE type) throws QeoException
    {
        return new StateWriterImpl<TYPE, DATA>(mFactoryHelper, mEntityAccessor, mIntrospectionUtil, type, null);
    }

    @Override
    public StateWriter<DATA> createStateWriter(TYPE type, PolicyUpdateListener policyListener) throws QeoException
    {
        return new StateWriterImpl<TYPE, DATA>(mFactoryHelper, mEntityAccessor, mIntrospectionUtil, type,
            policyListener);
    }
}
