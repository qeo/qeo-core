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

package org.qeo.android.exception;

import org.qeo.exception.QeoException;

/**
 * Exception to be thrown if Qeo Service on the system is older as the library in the application. This means the user
 * should install an update of the Qeo Service.
 */
public class QeoServiceTooOldException
    extends QeoException
{
    /**
     * Create exception.
     */
    public QeoServiceTooOldException()
    {
        super("Qeo Service on your system is too old. Please install an update.");
    }
}
