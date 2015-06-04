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

package org.qeo.sample.gauge.test;

import org.qeo.sample.gauge.NetStatMessage;
import org.qeo.system.DeviceId;

/**
 * 
 */
public class NetStatMessageUtil
{
    private NetStatMessageUtil()
    {
    }

    /**
     * Constructs a Netstat message based on the data received as arguments.
     * 
     * @param ifaceName -Interface name
     * @param id -device id
     * @param inbytes -Number of bytes received
     * @param inpackets -Number of packets received
     * @param outbytes -Number of bytes sent out
     * @param outpackets -Number of packets sent out
     * @param time - Represents current time stamp
     */
    public static NetStatMessage newNetStatMessage(String ifaceName, DeviceId id, long inbytes, long inpackets,
        long outbytes, long outpackets, long time)
    {
        NetStatMessage msg = new NetStatMessage();
        msg.ifName = ifaceName;
        msg.deviceId = id;
        msg.bytesIn = inbytes;
        msg.packetsIn = inpackets;
        msg.bytesOut = outbytes;
        msg.packetsOut = outpackets;
        msg.timestamp = time;
        return msg;
    }

    /**
     * Constructs a Netstat message based on the data received as arguments.
     * 
     * @param ifaceName -Interface name
     * @param id -device id
     * @param inbytes -Number of bytes received
     * @param inpackets -Number of packets received
     * @param outbytes -Number of bytes sent out
     * @param outpackets -Number of packets sent out
     */
    public static NetStatMessage newNetStatMessage(String ifaceName, DeviceId id, long inbytes, long inpackets,
        long outbytes, long outpackets)
    {
        return newNetStatMessage(ifaceName, id, inbytes, inpackets, outbytes, outpackets, System.nanoTime());
    }

}
