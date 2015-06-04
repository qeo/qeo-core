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
 * Default implementation of the {@link EventReaderListener} interface.<br>
 * All methods have a default empty implementation
 * 
 * @param <T> The class of the Qeo data type
 */
public class DefaultEventReaderListener<T>
    implements EventReaderListener<T>
{

    @Override
    public void onData(T t)
    {
    }

    @Override
    public void onNoMoreData()
    {
    }
}
