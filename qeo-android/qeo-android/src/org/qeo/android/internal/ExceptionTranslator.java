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

import org.qeo.exception.OutOfResourcesException;

/**
 * Utility class to convert exceptions thrown by the service to human-readable exceptions for the end-user.
 */
public final class ExceptionTranslator
{
    private ExceptionTranslator()
    {
    }

    /**
     * Check if there is an exception defined. If defined, translate into correct exception/stracktrace and throw the
     * exception.
     * 
     * @param pex The exception struct.
     */
    public static void handleServiceException(ParcelableException pex)
    {
        if (pex == null) {
            return;
        }
        RuntimeException ex = pex.getException();
        if (ex == null) {
            return; // no exception
        }

        if (ex instanceof SecurityException) {
            // user does something illegal. Throw new exception to get decent stacktrace.
            // don't care about the cause, message should be obvious enough
            throw new SecurityException(ex.getMessage());
        }
        else if (ex instanceof IllegalStateException) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
        else if (ex instanceof IllegalArgumentException) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
        else if (ex instanceof OutOfResourcesException) {
            throw new OutOfResourcesException(ex.getMessage(), ex);
        }
        else {
            // catch-all
            // throw a new runtimeException in order to get a stacktrace from the calling code
            throw new RuntimeException(ex);
        }

    }
}
