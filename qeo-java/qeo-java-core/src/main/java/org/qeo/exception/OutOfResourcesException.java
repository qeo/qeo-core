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

package org.qeo.exception;

/**
 * Signals that not enough resources could be allocated.
 */
public class OutOfResourcesException
        extends RuntimeException
{
    private static final long serialVersionUID = 9054429315974299578L;

    /**
     * Construct a non-descriptive OutOfResourcesException.
     */
    public OutOfResourcesException()
    {
        super();
    }

    /**
     * Construct an OutOfResourcesException with a descriptive message.
     * 
     * @param message a message describing the cause
     */
    public OutOfResourcesException(String message)
    {
        super(message);
    }

    /**
     * Construct an OutOfResourcesException with a descriptive message and throwable.
     *
     * @param message a message describing the cause
     * @param cause the cause of the exception
     */
    public OutOfResourcesException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
