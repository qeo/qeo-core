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

package org.qeo.exception;

/**
 * A generic Qeo security exception.
 */
public abstract class SecurityException
    extends QeoException
{
    private static final long serialVersionUID = -7674915385306454376L;

    /**
     * Create a Qeo Security Exception.
     */
    public SecurityException()
    {
        super();
    }

    /**
     * Create a QeoException.
     * 
     * @param message The error message
     */
    public SecurityException(String message)
    {
        super(message);
    }

    /**
     * Create a QeoException.
     * 
     * @param message The error message
     * @param cause The cause of the exception
     */
    public SecurityException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
