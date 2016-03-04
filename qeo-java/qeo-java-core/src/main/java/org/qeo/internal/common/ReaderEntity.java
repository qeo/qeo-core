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

package org.qeo.internal.common;

import org.qeo.Notifiable;
import org.qeo.policy.PolicyUpdateListener;

/**
 * Abstract class representing a reader (agnostic of DDS and reflection).
 */
public abstract class ReaderEntity
    implements Notifiable
{
    /** data listener connected to this reader. can be null. */
    protected final ReaderListener mListener;
    /** policy update listener connected to this reader. can be null. */
    protected final PolicyUpdateListener mPolicyListener;
    /** The type for which this reader is created. */
    protected final ObjectType mType;

    /**
     * Create a new ReaderEntity.
     * 
     * @param type Type to read
     * @param el Listener to attach
     * @param policyListener An (optional) policy update listener to attach to the reader
     */
    public ReaderEntity(ObjectType type, ReaderListener el, PolicyUpdateListener policyListener)
    {
        mListener = el;
        mPolicyListener = policyListener;
        mType = type;
    }

    /**
     * Clean up all resources connected to this reader.
     */
    public abstract void close();

    /**
     * Read a sample. The sample will still be available for new reads in the future.
     * 
     * @param f Filter used to specify which sample to read.
     * @param info [out] Meta-information about the returned sample.
     * @return The sample or null in case there is no match.
     */
    public abstract ObjectData read(ReaderFilter f, SampleInfo info);

    /**
     * Take a sample. The sample will no longer be available for new reads/takes in the future.
     * 
     * @param f Filter used to specify which sample to take.
     * @param info [out] Meta-information about the returned sample.
     * @return The sample or null in case there is no match.
     */
    public abstract ObjectData take(ReaderFilter f, SampleInfo info);

    /**
     * Call this method to trigger a re-evaluation of the reader's policy.
     */
    public abstract void updatePolicy();

    /**
     * Get the type associated with this reader.
     * 
     * @return The associated type.
     */
    public ObjectType getType()
    {
        return mType;
    }

    /**
     * Get the listener associated with this reader.
     * 
     * @return the associated listener
     */
    public ReaderListener getListener()
    {
        return mListener;
    }

    /**
     * Enables or disables background notifications for this reader.
     * 
     * @param enabled True to enable or false to disable notifications.
     */
    @Override
    public abstract void setBackgroundNotification(boolean enabled);
}
