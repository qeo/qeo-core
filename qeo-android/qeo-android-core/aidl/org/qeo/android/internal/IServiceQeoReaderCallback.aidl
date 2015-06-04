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

package org.qeo.android.internal;

import org.qeo.android.internal.ParcelableData;
import org.qeo.android.internal.ParcelableException;

/**
 * This interface contains all possible reader callback methods. The qeo service uses these callbacks
 * to notify applications about the state of their reader.
 */
interface IServiceQeoReaderCallback
{
    /**
     * This callback will be called to notify the application about the fact there is new or changed
     * data available. There is no actual data given so it is up to the application now to iterate over all data.
     * This callback is typically implemented by iterating state readers.
     */
    void onUpdate();

    /**
     * This callback will be called when new data has arrived. The data is given to the application.
     *
     * @param type The type of callback. This will be an ID_ON_* listed in the QeoParceler class.
     * @param firstBlock Indicated this is the first block of potentially multiple blocks.
     * @param lastBlock Indicated this is the last block of potentially multiple blocks.
     * @param totalSize The total size of all blocks in bytes.
     * @param data The data of the current block.
     */
    void onData(int type, boolean firstBlock, boolean lastBlock, int totalSize, in byte[] data);

    /**
     * This callback is used to notify the application that there is no more data to come at the moment.
     * If later there is new data, onData or onUpdate will be called.
     */
    void onNoMoreData();

    /**
     * This gets called if an error occurs during reading.
     *
     * @param ex The exception.
     */
    void onException(in ParcelableException ex);
}
