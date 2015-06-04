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

package org.qeo.android.forwarder;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qeo.android.service.ConfigurableSettings;
import org.qeo.forwarder.internal.jni.NativeForwarder;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * This service takes care of the polling of the security policy file. Every 2 minutes, a special HTTP request is done
 * to see if the policy file is still valid. If it is not valid, the QeoService will be triggered to pull it again from
 * the server.
 */
public class QeoForwarderService
    extends Service
{
    private static final String TAG = "QeoForwarder";
    private static final Logger LOG = Logger.getLogger(TAG);
    private static final String PREF_FORWARDER_TCP_PORT = "tcpPort";
    private static final String PREF_FORWARDER_PUBLIC_ADDRESS = "publicAddress";
    private NativeForwarder mNativeForwarder;
    private boolean mStarted;
    private ForwarderThread mThread;
    private ConfigurableSettings settings;
    private int forwarderTcpPort;
    private String forwarderPublicAddress;

    @Override
    public void onCreate()
    {
        settings = new ConfigurableSettings(this, ConfigurableSettings.FILE_QEO_PREFS_FORWARDER, true);
        forwarderTcpPort = settings.getInt(PREF_FORWARDER_TCP_PORT, 0);
        forwarderPublicAddress = settings.getString(PREF_FORWARDER_PUBLIC_ADDRESS, null);

        mNativeForwarder = new NativeForwarder();
        mStarted = false;
        mThread = new ForwarderThread();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (!mStarted) {
            mStarted = true;
            mThread.start();
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onDestroy()
    {
        if (mStarted) {
            LOG.info("Stopping native forwarder");
            mNativeForwarder.stopForwarder();
        }
    }

    private class ForwarderThread
        extends Thread
    {
        @Override
        public void run()
        {
            LOG.info("Native forwarder start!");
            if (forwarderTcpPort != 0) {
                LOG.warning("Using TCP port " + forwarderTcpPort);
                mNativeForwarder.configLocalPort(forwarderTcpPort);
            }
            if (forwarderPublicAddress != null && !forwarderPublicAddress.isEmpty()) {
                Pattern ipPattern = Pattern.compile("([0-9\\.]+):([0-9\\.]+)");
                Matcher matcher = ipPattern.matcher(forwarderPublicAddress);
                if (matcher.matches()) {
                    String ip = matcher.group(1);
                    int port = Integer.parseInt(matcher.group(2));
                    LOG.warning("Using public address " + ip + ":" + port);
                    mNativeForwarder.configPublicLocator(ip, port);
                }
            }
            mNativeForwarder.startForwarder();
            LOG.info("Native forwarder ended!");
        }
    }

}
