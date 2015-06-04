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
 * Exception that will be thrown if the Qeo ecurity initialization procedure fails.
 */
public class QeoSecurityInitFailedException
        extends QeoException
{

    /**
     * Serializable classes need a static final serialVersionUID field of type long.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create a QeoServiceNotFoundException.
     * 
     * @param message extra information on the reason why it failed
     */
    public QeoSecurityInitFailedException(String message)
    {
        super(message);
    }
}
