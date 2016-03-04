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

package org.qeo.sample.gauge.android.reader.interfaces;

import java.util.Date;

/**
 * LogMessage interface needs to be implemented for custom log messages.
 */
public interface LogMessage
{
    /**
     * Gets the type of this log message.
     *
     * @return returns the type of log message.
     */
    String getType();

    /**
     * Gets the content of this log message.
     *
     * @return returns the content of log message.
     */
    String getContent();

    /**
     * Gets the timestamp of this log message.
     *
     * @return returns the timestamp of log message.
     */
    Date getTimestamp();

}
