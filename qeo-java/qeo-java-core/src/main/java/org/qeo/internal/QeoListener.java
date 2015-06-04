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

package org.qeo.internal;

import org.qeo.QeoFactory;
import org.qeo.exception.QeoException;

/**
 * Listener interface with callbacks to indicate the state of the Qeo factory.
 */
public interface QeoListener
{
    /**
     * Called if the Qeo initialization is finished. I.e. if the service connection has been established and security is
     * initialized.
     * 
     * @param qeo The qeo factory created.
     */
    void onQeoReady(QeoFactory qeo);

    /**
     * Called if the service connection has been lost for some reason. This typically happens when the process hosting
     * the service has crashed or been killed. You will receive a call to onQeoReady as soon as the service is back up
     * and running.
     * 
     * This function is not called when you explicitly do a Qeo.close(), in that case you need to do the cleanup
     * yourself.
     * 
     * @param qeo The qeo factory associated.
     */
    void onQeoClosed(QeoFactory qeo);

    /**
     * Called if something goes wrong during Qeo initialization.<br>
     * Either onQeoReady or this function gets called.
     * 
     * @param ex The error cause.
     */
    void onQeoError(QeoException ex);

    /**
     * Called when authentication needs to be started. Do whatever is necessary to get authenticated and return an OTC
     * back when the authentication part succeeded (NativeQeo.setRegistrationCredentials) or notify that authentication
     * failed (NativeQeo.cancelRegistration)..
     * 
     * @return should return true if the corresponding actions are started
     */
    boolean onStartAuthentication();

    /**
     * Called when waking up from a suspended state. You need to override this callback to resume operations otherwise
     * Qeo will remain suspended.
     * 
     * @param typeName The name of the type for which data arrived. Can be null if the suspend call failed.
     */
    void onWakeUp(String typeName);

    /**
     * Called whenever state to background notification service is established/lost.
     * @param connected true for connected, false for disconnected.
     */
    void onBgnsConnectionChange(boolean connected);
}
