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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Class that manages recurrent reconnection attempts to the background notification service.
 */
public class BgnsConnectionAlarmReceiver extends BroadcastReceiver
{
    private static final Logger LOG = Logger.getLogger("BgnsAlrmRcvr");
    private static long sInterval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    private static boolean sEnabled = true;
    private boolean mStarted;
    private AlarmManager mAlarmMgr;
    private PendingIntent mAlarmIntent;

    /**
     * Default constructor.
     */
    public BgnsConnectionAlarmReceiver()
    {
        mStarted = false;
    }

    /**
     * Set the poller interval time. This timing is inexact, see android AlarmManager for more information.
     * @param time Time in milliseconds.
     */
    public static void setPollerInterval(int time)
    {
        sInterval = time;
    }

    /**
     * Enable/disable connection poller.
     * @param enabled true for enabled, false for disabled.
     */
    public static void setEnabled(boolean enabled)
    {
        sEnabled = enabled;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        LOG.info("Checking for background notification service connection");
        QeoSuspendHelper.getInstance(context).autoResume();
    }

    /**
     * Start polling if not yet started.
     * @param ctx Android context.
     */
    public synchronized void startConnectionPoller(Context ctx)
    {
        if (!sEnabled) {
            return;
        }
        if (!mStarted) {
            LOG.fine("Scheduling bgns polling every " + Math.round((double) sInterval / 60000) + " minutes");
            mAlarmMgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(ctx, BgnsConnectionAlarmReceiver.class);
            mAlarmIntent = PendingIntent.getBroadcast(ctx, 0, intent, 0);

            //schedule the alarm. Use wakeup trigger since elapsed time.
            mAlarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, sInterval, sInterval, mAlarmIntent);
            mStarted = true;
        }
    }

    /**
     * Cancel polling if eneabled.
     */
    public synchronized void cancelConnectionPoller()
    {
        if (mAlarmMgr != null && mAlarmIntent != null) {
            LOG.fine("Stopping bgns polling");
            mAlarmMgr.cancel(mAlarmIntent);
        }
        mStarted = false;
    }
}
