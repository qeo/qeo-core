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

import org.qeo.QeoWriter;
import org.qeo.exception.QeoException;
import org.qeo.internal.common.EntityType;
import org.qeo.internal.common.ObjectData;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.common.WriterEntity;
import org.qeo.internal.reflection.IntrospectionUtil;
import org.qeo.policy.PolicyUpdateListener;

import java.util.logging.Logger;

/**
 * Base implementation of a writer wrapping a reflection based writer.
 *
 * @param <TYPE> The class of the Qeo data type.
 * @param <DATA> The type of the Qeo data.
 */
abstract class WriterBase<TYPE, DATA> extends ReaderWriterBase implements QeoWriter<DATA>
{
    private static final Logger LOG = Logger.getLogger("WriterBase");
    /** Reference to the lowlevel writer. */
    protected final WriterEntity mWriter;
    /** Object that will be used to to the introspection/reflection. */
    protected final IntrospectionUtil<TYPE, DATA> mIntrospectionUtil;

    /**
     * Create a writer.
     *
     * @param factoryHelper Helper class for factory management.
     * @param entityAccessor Entity accessor to be used for creating the writer.
     * @param introspectionUtil Util class to to the type introspection.
     * @param typedesc Description of the type.
     * @param etype The entity type.
     * @param policyListener An (optional) policy update listener to attach to the writer
     *
     * @throws QeoException If the creation of the writer failed.
     */
    public WriterBase(FactoryHelper factoryHelper, EntityAccessor entityAccessor, IntrospectionUtil<TYPE,
        DATA> introspectionUtil, TYPE typedesc, EntityType etype, PolicyUpdateListener policyListener) throws
        QeoException
    {
        super(factoryHelper);
        this.mIntrospectionUtil = introspectionUtil;
        ObjectType type = introspectionUtil.typeFromTypedesc(typedesc);
        this.mWriter = entityAccessor.getWriter(type, etype, policyListener);
    }

    @Override
    public void close()
    {
        LOG.fine("Closing writer: " + this);
        if (mWriter != null) {
            mWriter.close();
        }
        super.close();
    }

    @Override
    public void write(DATA t)
    {
        ObjectData data = mIntrospectionUtil.dataFromObject(t, mWriter.getType());
        mWriter.write(data);
    }

    @Override
    public void updatePolicy()
    {
        mWriter.updatePolicy();
    }

}
