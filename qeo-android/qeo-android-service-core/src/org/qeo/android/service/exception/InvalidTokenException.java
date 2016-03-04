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

package org.qeo.android.service.exception;

import java.io.IOException;

/**
 * Exception to indicate that a token is invalid.
 */
public class InvalidTokenException extends IOException
{
    /**
     * Constructor.
     * @param msg The error message.
     */
    public InvalidTokenException(String msg)
    {
        super(msg);
    }
}
