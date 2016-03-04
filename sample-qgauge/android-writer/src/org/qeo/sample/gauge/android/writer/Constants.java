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

package org.qeo.sample.gauge.android.writer;

/**
 * This class maintains the list of constants used in the application.
 */
public final class Constants
{

    private Constants()
    {

    }
    /**
     * File path name for to get interface details.
     */
    public static final String INTERFACE_FILE_PATH = "/sys/class/net/";
    /**
     * File path name for to get details for received data in bytes.
     */
    public static final String RX_BYTES_PATH = "/statistics/rx_bytes";
    /**
     * File path name for to get details for received data in packets.
     */
    public static final String RX_PACKETS_PATH = "/statistics/rx_packets";
    /**
     * File path name for to get details for transmitted data in bytes.
     */
    public static final String TX_BYTES_PATH = "/statistics/tx_bytes";
    /**
     * File path name for to get details for transmitted data in packets.
     */
    public static final String TX_PACKETS_PATH = "/statistics/tx_packets";

    /**
     * Bundle- Extra used to save current published state of Writer.
     */
    public static final String EXTRA_PUBLISH_STATE = "CurrentPublishState";

}
