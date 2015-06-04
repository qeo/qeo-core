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

/**
 * Filter used to specify which sample to read/take.
 */
public class ReaderFilter
{
    /**
     * Handle of the instance to read, null to read the first instance.
     */
    private int mInstanceHandle;

    /**
     * Create a reader filter filtering on an instance handle.
     * 
     * @param instanceHandle The instance handle to filter on.
     */
    public ReaderFilter(int instanceHandle)
    {
        super();
        this.mInstanceHandle = instanceHandle;
    }

    /**
     * Create a default ReaderFilter that just will read/take the first sample.
     */
    public ReaderFilter()
    {
        this(0);
    }

    /**
     * Set a new instance handle to be used for filtering.
     * 
     * @param instanceHandle The instance handle to filter on.
     */
    public void setInstanceHandle(int instanceHandle)
    {
        this.mInstanceHandle = instanceHandle;
    }

    /**
     * Get the instance handle used for filtering.
     * 
     * @return The instance handle used for filtering.
     */
    public int getInstanceHandle()
    {
        return mInstanceHandle;
    }
}
