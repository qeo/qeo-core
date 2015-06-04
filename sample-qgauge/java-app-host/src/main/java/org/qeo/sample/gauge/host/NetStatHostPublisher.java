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

package org.qeo.sample.gauge.host;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.qeo.QeoFactory;
import org.qeo.exception.QeoException;
import org.qeo.java.QeoConnectionListener;
import org.qeo.java.QeoJava;
import org.qeo.sample.gauge.NetStatMessage;
import org.qeo.sample.gauge.NetStatProvider;
import org.qeo.sample.gauge.NetStatPublisher;

/**
 * NetStatHostPublisher is a NetStatPublisher based on /proc/net/dev.
 * 
 * 
 */
public class NetStatHostPublisher
        implements NetStatProvider
{
    private void parseAndSend(String line, ArrayList<NetStatMessage> list)
    {
        String[] tokens = line.trim().split("[:\\s]+");
        if (17 == tokens.length) {
            NetStatMessage msg = new NetStatMessage();
            msg.ifName = tokens[0];
            msg.deviceId = null;
            msg.bytesIn = Long.parseLong(tokens[1]);
            msg.packetsIn = Long.parseLong(tokens[2]);
            msg.bytesOut = Long.parseLong(tokens[9]);
            msg.packetsOut = Long.parseLong(tokens[10]);
            msg.timestamp = System.nanoTime();
            list.add(msg);
        }
    }

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
            e.printStackTrace();
        }
        finally {
            if (r != null) {
                try {
                    r.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return list.toArray(new NetStatMessage[list.size()]);
    }

    public static void main(String[] args)
        throws QeoException
    {
        QeoJava.initQeo(new QeoConnectionListener() {

            @Override
            public void onQeoReady(QeoFactory qeo)
            {
                final NetStatPublisher publisher;
                try {
                    publisher = new NetStatPublisher(new NetStatHostPublisher(), 250, qeo);
                    System.out.println("NetStatHostPublisher.main(): created publisher " + publisher);
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run()
                        {
                            publisher.close();
                            System.out.println("NetStatHostPublisher.main: publisher killed");
                        }
                    });
                }
                catch (QeoException e) {
                    System.out.print("Error instantiating NetStatPublisher");
                }
            }

            @Override
            public void onQeoError(QeoException ex)
            {
                System.out.print("Error initializing Qeo");
            }
        });
    }

    @Override
    public void publisherStopped(NetStatPublisher netStatPublisher, Throwable cause)
    {
        System.err.println("Application is stopped due unexpected exception.");
        System.out.println(cause.getMessage());
    }
}
