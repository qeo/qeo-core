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
 * Possible registration status code.
 */
public enum RegistrationStatusCode {
    /**
     * State if device is unregistered.
     */
    UNREGISTERED,
    /**
     * State if device has started registering process.
     */
    REGISTERING,
    /**
     * State if device has been successfully register.
     */
    REGISTERED

}
