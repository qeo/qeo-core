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

import org.qeo.exception.QeoException;

/**
 * Interface definition for service connection callbacks.
 */
interface ServiceConnectionListener
{
    /**
     * Called when the service connection changed to the connected state.
     */
    void onConnected();

    /**
     * Called when the service connection changed to the disconnected state.
     */
    void onDisconnected();

    /**
     * Called when an error happens in the service initialization.
     * 
     * @param ex The exception
     */
    void onError(QeoException ex);
}
