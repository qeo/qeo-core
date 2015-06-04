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

package org.qeo;

/**
 * A Qeo writer for writing Qeo states.
 * 
 * @param <T> The class of the Qeo data type
 */
public interface StateWriter<T>
    extends QeoWriter<T>
{
    @Override
    void write(T t);

    /**
     * Signal the removal of an object to all subscribers. Note that this call is only useful for keyed objects, an
     * exception will be thrown if not.
     * 
     * @param t The object.
     * 
     * @throws IllegalStateException if the writer has already been closed or the data is invalid
     * @throws IllegalArgumentException if the object does not exist
     * @throws org.qeo.exception.OutOfResourcesException if not enough resources are available
     */
    void remove(T t);
}
