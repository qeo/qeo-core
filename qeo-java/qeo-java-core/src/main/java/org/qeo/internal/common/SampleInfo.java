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

/**
 * Information about a sample that is read/taken.
 */
public class SampleInfo
{
    /**
     * The instanceHandle identifying this sample.
     */
    private int mInstanceHandle;

    /**
     * Create a new sample information.
     */
    public SampleInfo()
    {
        super();
    }

    /**
     * Get the instance handle for the sample.
     * 
     * @return The instance handle.
     */
    public int getInstanceHandle()
    {
        return mInstanceHandle;
    }

    /**
     * Set the instance handle for the sample.
     * 
     * @param instanceHandle The instance handle.
     */
    public void setInstanceHandle(int instanceHandle)
    {
        this.mInstanceHandle = instanceHandle;
    }
}
