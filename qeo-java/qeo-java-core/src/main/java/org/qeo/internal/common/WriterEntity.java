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

package org.qeo.internal.common;

import org.qeo.policy.PolicyUpdateListener;

/**
 * Abstract class representing a writer (agnostic of DDS and reflection).
 */
public abstract class WriterEntity
{
    /** The data type for which this writer is created. */
    protected final ObjectType mType;
    /** policy update listener connected to this reader. can be null. */
    protected final PolicyUpdateListener mPolicyListener;

    /**
     * Create a new WriterEntity.
     * 
     * @param type Type to write
     * @param policyListener An (optional) policy update listener to attach to the writer
     */
    public WriterEntity(ObjectType type, PolicyUpdateListener policyListener)
    {
        mType = type;
        mPolicyListener = policyListener;
    }

    /**
     * Get the type associated with this writer.
     * 
     * @return The associated type.
     */
    public ObjectType getType()
    {
        return mType;
    }

    /**
     * Cleanup all resources connect to this writer.
     */
    public abstract void close();

    /**
     * Publish or write new data.
     * 
     * @param data The data to write
     */
    public abstract void write(ObjectData data);

    /**
     * Dispose keyed data. Only the keyed fields are used.
     * 
     * @param data The data to remove
     */
    public abstract void remove(ObjectData data);

    /**
     * Call this method to trigger a re-evaluation of the reader's policy.
     */
    public abstract void updatePolicy();
}
