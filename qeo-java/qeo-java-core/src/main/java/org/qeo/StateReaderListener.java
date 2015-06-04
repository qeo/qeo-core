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

package org.qeo;

/**
 * Interface that can be used for a {@link StateReader} to be notified if the data changes.
 */
public interface StateReaderListener extends QeoReaderListener
{
    /**
     * Called when a change in the data is detected.
     */
    void onUpdate();
}
