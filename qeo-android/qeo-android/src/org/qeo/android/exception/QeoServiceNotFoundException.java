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

package org.qeo.android.exception;

import org.qeo.exception.QeoException;

/**
 * Exception thrown when the Qeo Service on Android is not found.
 */
public class QeoServiceNotFoundException
        extends QeoException
{
    private static final long serialVersionUID = -8603139150621433364L;

    /**
     * Create a QeoServiceNotFoundException.
     */
    public QeoServiceNotFoundException()
    {
        super("QeoService not found");
    }
}
