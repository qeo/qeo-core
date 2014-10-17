/*
 * Copyright (c) 2014 - Qeo LLC
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

package com.technicolor.qeo.codegen;

/**
 * Exception to be thrown if the code should abort and exit with exit 1.
 */
public class ExitException
    extends RuntimeException
{
    private static final long serialVersionUID = -8102815016130216296L;

    /**
     * Abort code with exit 1.
     * 
     * @param msg The message to be displayed.
     */
    public ExitException(String msg)
    {
        super(msg);
    }

    /**
     * Abort code with exit 1.
     * 
     * @param msg The message to be displayed.
     * @param cause The cause. Will not be displayed if running from the commandline, but will be if running from junit.
     */
    public ExitException(String msg, Throwable cause)
    {
        super(msg, cause);
    }

}
