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

import org.qeo.EventReader;
import org.qeo.EventReaderListener;
import org.qeo.exception.QeoException;
import org.qeo.internal.common.EntityType;
import org.qeo.internal.reflection.IntrospectionUtil;
import org.qeo.policy.PolicyUpdateListener;

/**
 * Implementation of an event reader that uses reflection to determine the type.
 *
 * @param <TYPE> The class of the Qeo data type.
 * @param <DATA> The type of the Qeo data.
 */
public class EventReaderImpl<TYPE, DATA> extends ReaderBase<TYPE, DATA> implements EventReader<DATA>
{
    /**
     * Create an event reader.
     *
     * @param factoryHelper Helper class for factory management.
     * @param entityAccessor Entity accessor to be used for entity creation.
     * @param introspectionUtil Implementation that will be used for introspection.
     * @param typedesc The typedescription. See the implementation of the introspectionUtil parameter for which format
     *            is expected.
     * @param listener The listener to attach to this reader.
     * @param policyListener An (optional) policy update listener to attach to the reader
     *
     * @throws QeoException If the creation of the reader failed.
     */
    public EventReaderImpl(FactoryHelper factoryHelper, EntityAccessor entityAccessor, IntrospectionUtil<TYPE,
        DATA> introspectionUtil, TYPE typedesc, EventReaderListener<DATA> listener,
                           PolicyUpdateListener policyListener) throws QeoException
    {
        super(factoryHelper, entityAccessor, introspectionUtil, typedesc, EntityType.EVENT_DATA, listener,
            policyListener);
    }
}
