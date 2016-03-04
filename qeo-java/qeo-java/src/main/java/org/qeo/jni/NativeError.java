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

package org.qeo.jni;

import org.qeo.exception.OutOfResourcesException;

/**
 * Wrapper class for native error codes.
 */
public final class NativeError
{
    // prevent construction
    private NativeError()
    {
    }

    /**
     * Enum representing the error codes returned by Qeo-C.
     * 
     * Remark: At the moment this needs to be lined up with the Qeo-C enum qeo_retcode_t
     */
    public enum Error {
        /** Success. */
        OK,
        /** Failure. */
        FAIL,
        /** Out of memory. */
        NOMEM,
        /** Invalid arguments. */
        INVAL,
        /** No more data. */
        NODATA,
        /** Invalid state for operation. */
        BADSTATE
    };

    /**
     * Convert native error code into java Error enum.
     * 
     * @param nativeError The native error code
     * @return A java error enum.
     */
    public static Error translateError(int nativeError)
    {
        return Error.values()[nativeError];
    }

    /**
     * Check the native error code and throw an exception if appropriate.
     * 
     * @param nativeError The native error code
     * @param errorString The message to add to the exception
     */
    public static void checkError(int nativeError, String errorString)
    {
        final Error error = translateError(nativeError);
        switch (error) {
            case FAIL:
                throw new RuntimeException(errorString);
            case NOMEM:
                throw new OutOfResourcesException(errorString);
            case INVAL:
                throw new IllegalArgumentException(errorString);
            case BADSTATE:
                throw new IllegalStateException(errorString);
            default:
                break;
        }
    }

    /**
     * Check the native error code and throw an exception if appropriate.
     * 
     * @param nativeError The native error code
     */
    public static void checkError(int nativeError)
    {
        checkError(nativeError, "");
    }
}
