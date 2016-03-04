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
 * Interface for callbacks from BackgroundNotificationService.
 */
public interface BgnsCallbacks
{
    /**
     * When a wakeup event is received.
     * @param typeName The type name.
     */
    void dispatchWakeUp(String typeName);

    /**
     * When bgns connectivity changes.
     * @param fd The filedescriptor.
     * @param state The state (true for connected, false for disconnected).
     */
    void onBgnsConnected(int fd, boolean state);
}
