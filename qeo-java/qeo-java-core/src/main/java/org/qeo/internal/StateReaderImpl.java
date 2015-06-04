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

import org.qeo.StateReader;
import org.qeo.StateReaderListener;
import org.qeo.exception.QeoException;
import org.qeo.internal.common.EntityType;
import org.qeo.internal.reflection.IntrospectionUtil;
import org.qeo.policy.PolicyUpdateListener;

import java.util.Iterator;

/**
 * Implementation of a state reader that uses reflection to determine the type. This type of state reader can iterate
 * over instances. You can optionally provide a listener to be notified of changes, in which case reiteration can be
 * done.
 *
 * @param <TYPE> The class of the Qeo data type.
 * @param <DATA> The type of the Qeo data.
 */
public class StateReaderImpl<TYPE, DATA> extends ReaderBase<TYPE, DATA> implements StateReader<DATA>
{
    /**
     * Create a state reader.
     *
     * @param factoryHelper Helper class for factory management.
     * @param entityAccessor Entity accessor to be used for entity creation.
     * @param introspectionUtil Implementation that will be used for introspection.
     * @param typedesc The typedescription. See the implementation of the introspectionUtil parameter for which format
     *            is expected.
     * @param listener The optional listener to attach to this reader.
     * @param policyListener An (optional) policy update listener to attach to the reader
     *
     * @throws QeoException If the creation of the reader failed.
     */
    public StateReaderImpl(FactoryHelper factoryHelper, EntityAccessor entityAccessor, IntrospectionUtil<TYPE,
        DATA> introspectionUtil, TYPE typedesc, StateReaderListener listener, PolicyUpdateListener policyListener)
        throws QeoException
    {
        super(factoryHelper, entityAccessor, introspectionUtil, typedesc, EntityType.STATE_UPDATE, listener,
            policyListener);
    }

    /**
     * An iterator for iterating over all instances of the class {@code T} .
     *
     * An example of its usage:
     *
     * <pre>
     * StateReader&lt;MyData&gt; myDataReader = new StateReader&lt;MyData&gt;(MyData.class);
     *
     * for (MyData md : myDataReader) {
     *     // do something useful with md ...
     * }
     * </pre>
     *
     * @return The iterator.
     */
    @Override
    public Iterator<DATA> iterator()
    {
        return new InstanceIterator<TYPE, DATA>(this);
    }
}
