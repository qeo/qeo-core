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
import org.qeo.QeoFactory;
import org.qeo.StateChangeReader;
import org.qeo.StateChangeReaderListener;
import org.qeo.StateReader;
import org.qeo.StateReaderListener;
import org.qeo.StateWriter;
import org.qeo.exception.QeoException;
import org.qeo.internal.reflection.IntrospectionUtil;
import org.qeo.internal.reflection.ReflectionUtil;
import org.qeo.policy.PolicyUpdateListener;

import java.util.logging.Logger;

/**
 * Abstract base class for a QeoFactory.
 */
public abstract class BaseFactory implements QeoFactory
{
    private static final Logger LOG = Logger.getLogger("BaseFactory");

    /** instance to get specific entities for different implementations (eg android and host). */
    protected final EntityAccessor mEntityAccessor;
    /** dds domainId. */
    protected final int mDomainId;
    private final FactoryHelper mFactoryHelper;

    /**
     * Create an instance of BaseFactory.
     *
     * @param domainId dds domainId.
     * @param entityAccessor The accessor to generate implementation specific readers/writers.
     */
    protected BaseFactory(int domainId, EntityAccessor entityAccessor)
    {
        mDomainId = domainId;
        mEntityAccessor = entityAccessor;
        mFactoryHelper = new FactoryHelper();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.qeo.QeoFactory#createEventReader(java.lang.Class, org.qeo.EventReaderListener)
     */
    @Override
    public <T> EventReader<T> createEventReader(Class<T> clazz, EventReaderListener<T> listener) throws QeoException
    {
        return new EventReaderImpl<Class<T>, T>(mFactoryHelper, mEntityAccessor, new ReflectionUtil<T>(clazz), clazz,
            listener, null);
    }

    @Override
    public <T> EventReader<T> createEventReader(Class<T> clazz, EventReaderListener<T> listener,
                                                PolicyUpdateListener policyListener) throws QeoException
    {
        return new EventReaderImpl<Class<T>, T>(mFactoryHelper, mEntityAccessor, new ReflectionUtil<T>(clazz), clazz,
            listener, policyListener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.qeo.QeoFactory#createEventWriter(java.lang.Class)
     */
    @Override
    public <T> EventWriter<T> createEventWriter(Class<T> clazz) throws QeoException
    {
        return new EventWriterImpl<Class<T>, T>(mFactoryHelper, mEntityAccessor, new ReflectionUtil<T>(clazz), clazz,
            null);
    }

    @Override
    public <T> EventWriter<T> createEventWriter(Class<T> clazz, PolicyUpdateListener policyListener) throws QeoException
    {
        return new EventWriterImpl<Class<T>, T>(mFactoryHelper, mEntityAccessor, new ReflectionUtil<T>(clazz), clazz,
            policyListener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.qeo.QeoFactory#createStateReader(java.lang.Class)
     */
    @Override
    public <T> StateReader<T> createStateReader(Class<T> clazz, StateReaderListener listener) throws QeoException
    {
        return new StateReaderImpl<Class<T>, T>(mFactoryHelper, mEntityAccessor, new ReflectionUtil<T>(clazz), clazz,
            listener, null);
    }

    @Override
    public <T> StateReader<T> createStateReader(Class<T> clazz, StateReaderListener listener,
                                                PolicyUpdateListener policyListener) throws QeoException
    {
        return new StateReaderImpl<Class<T>, T>(mFactoryHelper, mEntityAccessor, new ReflectionUtil<T>(clazz), clazz,
            listener, policyListener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.qeo.QeoFactory#createStateChangeReader(java.lang.Class, org.qeo.StateChangeReaderListener)
     */
    @Override
    public <T> StateChangeReader<T> createStateChangeReader(Class<T> clazz, StateChangeReaderListener<T> listener)
        throws QeoException
    {
        return new StateChangeReaderImpl<Class<T>, T>(mFactoryHelper, mEntityAccessor, new ReflectionUtil<T>(clazz),
            clazz, listener, null);
    }

    @Override
    public <T> StateChangeReader<T> createStateChangeReader(Class<T> clazz, StateChangeReaderListener<T> listener,
                                                            PolicyUpdateListener policyListener) throws QeoException
    {
        return new StateChangeReaderImpl<Class<T>, T>(mFactoryHelper, mEntityAccessor, new ReflectionUtil<T>(clazz),
            clazz, listener, policyListener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.qeo.QeoFactory#createStateWriter(java.lang.Class)
     */
    @Override
    public <T> StateWriter<T> createStateWriter(Class<T> clazz) throws QeoException
    {
        return new StateWriterImpl<Class<T>, T>(mFactoryHelper, mEntityAccessor, new ReflectionUtil<T>(clazz), clazz,
            null);
    }

    @Override
    public <T> StateWriter<T> createStateWriter(Class<T> clazz, PolicyUpdateListener policyListener) throws QeoException
    {
        return new StateWriterImpl<Class<T>, T>(mFactoryHelper, mEntityAccessor, new ReflectionUtil<T>(clazz), clazz,
            policyListener);
    }

    /**
     * Get the entity accessor.
     *
     * @return the entity accessor
     */
    public EntityAccessor getEntityAccessor()
    {
        return mEntityAccessor;
    }

    /**
     * Get the factory helper.
     * @return The factory helper.
     */
    FactoryHelper getFactoryHelper()
    {
        return mFactoryHelper;
    }

    /**
     * Get the domainId for this factory.
     *
     * @return The id.
     */
    public int getDomainId()
    {
        return mDomainId;
    }

    /**
     * Cleanup the factory.
     */
    public void cleanup()
    {
        LOG.fine("Cleanup factory");
        mFactoryHelper.closeAllReaderWriter();
    }

    /**
     * Create a custom QeoFactory based on own type introspection.
     *
     * @param introspectionUtil The custom type introspection to convert data types
     * @param <TYPE> The type description of the Qeo data type
     * @param <DATA> The type object of the Qeo data type
     *
     * @return A custom QeoFactory
     */
    public <TYPE, DATA> CustomQeoFactory<TYPE, DATA> getCustomFactory(final IntrospectionUtil<TYPE,
        DATA> introspectionUtil)
    {
        return new QeoFactoryWithIntrospection<TYPE, DATA>(this, introspectionUtil);
    }

}
