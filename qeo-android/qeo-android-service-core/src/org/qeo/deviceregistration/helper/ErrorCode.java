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

package org.qeo.deviceregistration.helper;

/**
 * Possible error codes.
 */
public enum ErrorCode {
    /**
     * No error, default.
     */
    NONE,
    /**
     * If request is cancelled due to some reasons.
     */
    CANCELLED,
    /**
     * Time out occurs while waiting for response from Unregistered device.
     */
    REMOTE_REGISTRATION_TIMEOUT,
    /**
     * Unregistered device rejects the registration request.
     */
    NEGATIVE_CONFIRMATION,
    /**
     * Platform failure.
     */
    PLATFORM_FAILURE,
    /**
     * Unregistered device received invalid OTP.
     */
    INVALID_OTP,
    /**
     * Internal error.
     */
    INTERNAL_ERROR,
    /**
     * Network failure.
     */
    NETWORK_FAILURE,
    /**
     * ssl handshake failure.
     */
    SSL_HANDSHAKE_FAILURE,
    /**
     * Invalide credentials.
     */
    RECEIVED_INVALID_CREDENTIALS,
    /**
     * Store failure.
     */
    STORE_FAILURE,
    /**
     * Unknown error.
     */
    UNKNOWN;

}
