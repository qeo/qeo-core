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

import java.io.IOException;

/**
 * Generic exception if an error occurs in Qeo.
 */
public class QeoException
        extends IOException
{
    private static final long serialVersionUID = 4216632514412048967L;

    /**
     * Create a QeoException.
     */
    protected QeoException()
    {
        super();
    }

    /**
     * Create a QeoException.
     * 
     * @param message The error message
     */
    public QeoException(String message)
    {
        super(message);
    }

    /**
     * Create a QeoException.
     * 
     * @param method The dds function name
     * @param ddsrc The dds return code
     */
    public QeoException(String method, int ddsrc)
    {
        super(String.format("Method \"%s\" returned \"%d\"", method, ddsrc));
    }

    /**
     * Create a QeoException.
     * 
     * @param message The error message
     * @param cause The cause of the exception
     */
    public QeoException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
