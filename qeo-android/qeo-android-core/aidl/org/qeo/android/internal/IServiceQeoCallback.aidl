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

import org.qeo.android.internal.ParcelableData;

/**
 * This interface contains general application callback methods. The qeo service uses these callbacks
 * to notify applications about the state of the application within the Qeo service.
 */
interface IServiceQeoCallback
{
    /**
     * This callback will be called when the Qeo service is initialized and when
     * the security check has been done and the application has been successfully registered.
     */
    void onRegistered();

    /**
     * This callback will be called when the security manifest has been accepted or rejected.
     *
     * @param result indicates that the manifest was rejected (false) or accepted (true)
     */
    void onManifestReady(boolean result);

    /**
     * This callback will be called to allow the application developer to provide its own authentication.
     * The result is that the developer should also provide an OAuth code to the service in order to 
     * let the service continue.
     * 
     * @return return true if the application is going to handle authentication
     */
    boolean onStartAuthentication();

    /**
     * This callback will be called when the OTC pop-up dialog was canceled.
     */
    void onOtpDialogCanceled();

    /**
     * This callback will be called when the authentication procedure fails.
     *
     * @param reason the reason why the security initialization failed
     */
    void onSecurityInitFailed(String reason);

    /**
     * This callback will be called when the application was successfully unregistered.
     */
    void onUnregistered();

    /**
     * This callback will be called when Qeo wakes up from a suspended state.
     *
     * @param typeName The name of the type for which data arrived
     */
    void onWakeUp(String typeName);

    /**
     * This callback will be called when Qeo is connected/disconnected to the notification service.
     *
     * @param state true for connected, false for disconnected
     */
    void onBgnsConnected(boolean state);
}
