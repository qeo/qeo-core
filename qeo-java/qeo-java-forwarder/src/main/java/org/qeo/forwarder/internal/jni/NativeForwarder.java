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

package org.qeo.forwarder.internal.jni;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Logger;

import org.qeo.jni.NativeError;
import org.qeo.jni.NativeQeo;

/**
 * 
 */
public class NativeForwarder
{
    private static final String TAG = "NativeForwarder";

    private static final Logger LOG = Logger.getLogger(TAG);

    static {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run()
            {
                LOG.info("load library libminiupnpc.so");
                NativeQeo.loadLibrary("miniupnpc");
                LOG.info("load library libqeoforwarder.so");
                NativeQeo.loadLibrary("qeoforwarder");
                LOG.info("load library libqeojavaforwarder.so");
                NativeQeo.loadLibrary("qeojavaforwarder");
                LOG.info("forwarder libraries loaded");
                return null;
            }
        });
    }

    public void startForwarder()
    {
        NativeError.checkError(nativeStartForwarder());
    }

    public void stopForwarder()
    {
        NativeError.checkError(nativeStopForwarder());
    }

    /**
     * Configure local TCP port which the forwarder will use.
     * 
     * @param port The port.
     */
    public void configLocalPort(int port)
    {
        nativeConfigLocalPort(port);
    }

    /**
     * Configure public locator (IP address and port) on which the forwarder is reachable.
     * 
     * @param ip the address in format xxx.xxx.xxx.xxx
     * @param port the TCP port
     */
    public void configPublicLocator(String ip, int port)
    {
        nativeConfigPublicLocator(ip, port);
    }

    private static native int nativeStartForwarder();

    private static native int nativeStopForwarder();

    private static native void nativeConfigLocalPort(int port);

    private static native void nativeConfigPublicLocator(String ip, int port);

}
