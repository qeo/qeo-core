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

package org.qeo.android.internal;

import org.qeo.exception.QeoException;

/**
 * Exception when the Qeo service is not connected.
 */
public class ServiceDisconnectedException extends QeoException
{
    /**
     * Create new instance.
     */
    public ServiceDisconnectedException()
    {
        super("Qeo service is not connected");
    }

    /**
     * Convert this exception into an IllegalStateException.
     */
    public void throwNotInitException()
    {
        throw new IllegalStateException("Qeo service not yet initialized. Wait until the QeoCallback is called "
            + "please", this);
    }

}
