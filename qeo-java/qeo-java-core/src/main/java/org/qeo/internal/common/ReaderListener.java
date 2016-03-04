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
 * Listener interface called by the implementation whenever something happens.
 */
public interface ReaderListener
{
    /**
     * Called when a change in the data is detected.
     */
    void onUpdate();

    /**
     * New data was received.
     * 
     * @param data The data that is received
     */
    void onData(ObjectData data);

    /**
     * Something went wrong during reading.
     * 
     * @param ex The exception.
     */
    void onError(RuntimeException ex);

    /**
     * After receiving some data, this method is called to indicate that no new data is in the queue.
     */
    void onNoMoreData();

    /**
     * A keyed sample is removed.
     * 
     * @param data The sample which is removed, only the keyed fields are filled in.
     */
    void onRemove(ObjectData data);

    /**
     * Check if the listener is closed.
     * 
     * @return true if closed
     */
    boolean isClosed();

    /**
     * Mark this listener as closed.
     */
    void close();

}
