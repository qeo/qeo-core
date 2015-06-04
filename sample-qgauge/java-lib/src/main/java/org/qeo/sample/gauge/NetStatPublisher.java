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

import java.io.Closeable;
import java.util.Timer;
import java.util.TimerTask;

import org.qeo.QeoFactory;
import org.qeo.StateWriter;
import org.qeo.exception.QeoException;

/**
 * A NetStatPublisher publishes NetStatMessages at a periodic interval.
 */
public class NetStatPublisher
        implements Closeable
{
    private final StateWriter<NetStatMessage> mWriter;
    private final Timer mTimer;
    private boolean mTimerRunning;

    /**
     * Constructor for NetStatPublisher.
     * 
     * @param statProvider A statProvider that retrieves the network statistics from the platform.
     * @param publishInterval The interval at which networks statistics are published.
     * @param qeo The qeo factory to use
     * @throws QeoException Throws QeoException if problem occurs in to publishing data
     */
    public NetStatPublisher(final NetStatProvider statProvider, long publishInterval, QeoFactory qeo)
        throws QeoException
    {
        if (statProvider == null || qeo == null) {
            throw new IllegalArgumentException();
        }
        // create a new StateWriter for NetStatMessage
        mWriter = qeo.createStateWriter(NetStatMessage.class);
        // create and schedule timer for retrieving and writing NetStatMessage
        mTimerRunning = true;
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run()
            {
                synchronized (NetStatPublisher.this) {
                    if (mTimerRunning) {
                        NetStatMessage[] msgs = statProvider.getCurrentStats();
                        for (int i = 0; i < msgs.length; i++) {
                            // publish NetStatMessage to Qeo
                            mWriter.write(msgs[i]);
                        }
                    }
                }
            }
        }, 0, publishInterval);
    }

    /**
     * Closes this NetStatPublisher.
     */
    @Override
    public synchronized void close()
    {
        mTimerRunning = false;
        mTimer.cancel();
        mWriter.close();
    }
}
