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

package org.qeo.sample.gauge.osgi.host;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.qeo.sample.gauge.NetStatMessage;
import org.qeo.sample.gauge.NetStatProvider;
import org.qeo.sample.gauge.NetStatPublisher;
import org.qeo.system.DeviceId;

/**
 * The NetstatProvider implementation for OSGi.
 */
public class OsgiGaugeWriter
    implements NetStatProvider
{


    private final DeviceId mDeviceId;

    /**
     * Constructs an OsgiGaugeWriter.
     *
     * @param deviceID the non-null deviceID to add in the QGauge samples.
     */
    public OsgiGaugeWriter(DeviceId deviceID)
    {
        mDeviceId = deviceID;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.qeo.sample.gauge.NetStatProvider#getCurrentStats()
     */
    @Override
    public NetStatMessage[] getCurrentStats()
    {
        BufferedReader r = null;
        ArrayList<NetStatMessage> list = new ArrayList<NetStatMessage>();
        try {
            File f = new File("/proc/net/dev");
            r = new BufferedReader(new FileReader(f));
            // skip the header
            r.readLine();
            r.readLine();
            // parse the real data
            String l = null;
            while (null != (l = r.readLine())) {
                parseAndSend(l, list);
            }
        }
        catch (Exception e) {
            System.err.println(e + e.getMessage());
        }
        finally {
            if (r != null) {
                try {
                    r.close();
                }
                catch (IOException e) {
                    System.err.println(e + e.getMessage());
                }
            }
        }
        return list.toArray(new NetStatMessage[list.size()]);
    }

    private void parseAndSend(String line, ArrayList<NetStatMessage> list)
    {
        String[] tokens = line.trim().split("[:\\s]+");
        if (17 == tokens.length) {
            NetStatMessage msg = new NetStatMessage();
            msg.ifName = tokens[0];
            msg.deviceId = mDeviceId;
            msg.bytesIn = Long.parseLong(tokens[1]);
            msg.packetsIn = Long.parseLong(tokens[2]);
            msg.bytesOut = Long.parseLong(tokens[9]);
            msg.packetsOut = Long.parseLong(tokens[10]);
            msg.timestamp = System.nanoTime();
            list.add(msg);
        }
    }
    /*
     * (non-Javadoc)
     * 
     * @see org.qeo.sample.gauge.NetStatProvider#publisherStopped(org.qeo.sample.gauge.NetStatPublisher,
     * java.lang.Throwable)
     */
    @Override
    public void publisherStopped(NetStatPublisher netStatPublisher, Throwable cause)
    {
        System.err.println("Osgi Writer stopped unexpectedly.");
    }

}
