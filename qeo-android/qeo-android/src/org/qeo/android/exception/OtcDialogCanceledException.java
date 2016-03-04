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

import org.qeo.exception.SecurityException;

/**
 * Exception that will be thrown if the user cancels the OTC dialog.
 */
public class OtcDialogCanceledException
        extends SecurityException
{
    /**
     * Serializable classes need a static final serialVersionUID field of type long.
     */
    private static final long serialVersionUID = -6955010724747587399L;

    /**
     * Constructor.
     */
    public OtcDialogCanceledException()
    {
        super("OTC Dialog was cancelled");
    }
}
