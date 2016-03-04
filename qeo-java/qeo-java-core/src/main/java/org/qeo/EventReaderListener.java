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
 * Listener callbacks for an {@link EventReader}.<br>
 * If desired a default implementation of all methods is available in the @{link DefaultEventReaderListener} class.
 * 
 * @param <T> The class of the Qeo data type
 */
public interface EventReaderListener<T>
    extends QeoReaderListener
{
    /**
     * Called on arrival of new object samples.
     * 
     * @param t Incoming object sample.
     */
    void onData(T t);

    /**
     * Called when there are no more object samples in the current burst.
     * 
     * There are situations where the {@link #onData} method will be called several times in a short period of time
     * because a burst of samples arrived. The <code>onNoMoreData</code> method will be called at the end of such a
     * burst. There is a default implementation of this method that is empty; i.e. does nothing. If you want to perform
     * some special actions at the end of a sample burst you should override it.
     */
    void onNoMoreData();
}
