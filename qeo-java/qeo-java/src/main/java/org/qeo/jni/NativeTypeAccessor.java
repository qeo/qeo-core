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

package org.qeo.jni;

import org.qeo.internal.common.Data;

/**
 * Interface used to convert to and from Native Qeo.
 */
public interface NativeTypeAccessor
{
    /**
     * Retrieve the value of a member inside Native Qeo.
     * 
     * @param b Native Qeo pointer to a buffer to retrieve the value from
     * @param id Unique identifier used inside Native Qeo to identify a field.
     * 
     * @return The actual value from Native Qeo
     */
    Data get(long b, int id);

    /**
     * Set a value inside Native Qeo.
     * 
     * @param d Native Qeo pointer to set the value to
     * @param id Unique identifier used inside Native Qeo to identify a field.
     * @param data The value to set.
     */
    void set(long d, int id, Data data);

    /**
     * Retrieve a pointer to the used Native Qeo type.
     * 
     * @return The pointer to the Native Qeo type
     */
    long getType();
}
