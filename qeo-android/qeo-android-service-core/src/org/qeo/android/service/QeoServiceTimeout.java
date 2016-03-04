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

package org.qeo.android.service;

import java.util.logging.Logger;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;

/**
 * Small service that gets started if a clients unbinds. This keeps the QeoService alive for 5 more seconds.<br>
 * This should help with rotating clients that don't survive rotation properly.
 */
public class QeoServiceTimeout
    extends Service
{
    private static final Logger LOG = Logger.getLogger("QeoServiceTimeout");
    private Handler mHandler;
    private static Context sCtx;
    private static boolean sBound = false;

    @Override
    public void onCreate()
    {
        super.onCreate();
        LOG.fine("onCreate");
        mHandler = new Handler();
    }

    /**
     * Execute the QeoService bind timeout service.
     * 
     * @param ctx Application context.
     */
    static synchronized void bindToService(Context ctx)
    {

        /*
         * Note: a normal approach would be to bind to the service in the onCreate. However this is problematic since
         * the onCreate (and hence the bind) will only be executed after the QeoService already got the unbind since
         * that gets scheduled first. Hence execute the bind immediately from a static context.
         */
        LOG.fine("Binding to service");
        sCtx = ctx;

        // start self
        sCtx.startService(new Intent(sCtx, QeoServiceTimeout.class));

        if (!sBound) {
            // and do the bind.
            sCtx.bindService(new Intent(sCtx, QeoService.class), CONN, Service.BIND_AUTO_CREATE);
        }
        setBound(true);
    }

    private static void setBound(boolean bound)
    {
        sBound = bound;
    }

    @Override
    public void onDestroy()
    {
        LOG.fine("onDestroy");
        sCtx.unbindService(CONN);
        setBound(false);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        LOG.fine("Keep connection to QeoService open for 5 seconds");
        // every time somebody calls onStart, restart the timer.
        mHandler.removeCallbacks(mCloseDelayedRunnable);
        mHandler.postDelayed(mCloseDelayedRunnable, 5000); // 5seconds
        return START_NOT_STICKY;
    }

    private final Runnable mCloseDelayedRunnable = new Runnable() {

        @Override
        public void run()
        {
            // stop self. This should cause unbind and QeoService will stop if nobody else reconnected.
            LOG.fine("StopSelf");
            stopSelf();
        }
    };

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    private static final ServiceConnection CONN = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            sCtx.unbindService(this);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            // nothing to be done here
        }
    };

}
