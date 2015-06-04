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

import java.io.Closeable;

/**
 * Generic interface to indicate a Qeo reader or Qeo writer entity.
 * 
 * @param <T> The class of the Qeo data type
 */
public interface ReaderWriter<T>
    extends Closeable
{
    /**
     * Call this method to trigger a re-evaluation of the reader's policy. The registered onPolicyUpdate from the
     * PolicyUpdateListener (if any) will be called again.
     */
    void updatePolicy();

    @Override
    void close();
}
