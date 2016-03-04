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

package org.qeo;

/**
 * Listener callbacks for a {@link StateChangeReader}.<br>
 * If desired a default implementation of all methods is available in the @{link DefaultStateChangeReaderListener}
 * class.
 * 
 * @param <T> The class of the Qeo data type
 */
public interface StateChangeReaderListener<T>
    extends EventReaderListener<T>
{
    /**
     * Called on arrival of an object instance removal notification. Note that this call will only be made for keyed
     * objects.
     * 
     * @param t The removed object instance.
     */
    void onRemove(T t);
}
