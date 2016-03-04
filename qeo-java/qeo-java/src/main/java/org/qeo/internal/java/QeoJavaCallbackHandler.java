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

package org.qeo.internal.java;

/**
 * Interface for Qeo security callbacks.
 */
public interface QeoJavaCallbackHandler
{
    /**
     * Call-back to trigger for user interaction with regards to entering OTP credentials and registration URL. Once
     * these parameters are retrieved from the end-user, he/she should call NativeQeo.setRegistrationCredentials(otp,
     * url) to inform the native part to proceed authenticating.
     */
    void onStartOtpRetrieval();

    /**
     * Call-back to update the user with security status information (from within JNI).
     * 
     * @param status the status update
     * @param reason the reason for the status update
     */
    void onStatusUpdate(final String status, final String reason);

    /**
     * Call-back to notify that Qeo is initialized (from within JNI) and ready to use.
     * 
     * @param result will be true if successful, false in case of failure
     */
    void onQeoInitDone(final boolean result);
}
