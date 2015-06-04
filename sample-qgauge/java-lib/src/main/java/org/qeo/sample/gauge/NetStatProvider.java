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

package org.qeo.sample.gauge;

/**
 * NetStatProvider needs to be implemented to create a NetStatPublisher. It
 * abstracts the platform specific implementation of collecting network
 * statistics from the logic to publish these statistics over Qeo.
 */

public interface NetStatProvider
{
    /**
     * Retrieves the current counter values of all the interfaces that need to
     * be published by a NetStatPublisher.
     *
     * @return An array of NetStatmMessages that will be published by the
     *         NetStatPublisher.
     */
    NetStatMessage[] getCurrentStats();

    /**
     * Callback method when NetStatPublisher is stopped to clean up any resources used by this NetStatProvider.
     *
     * @param netStatPublisher object publishing the data
     * @param cause object contains the error details
     */
    void publisherStopped(NetStatPublisher netStatPublisher, Throwable cause);
}
