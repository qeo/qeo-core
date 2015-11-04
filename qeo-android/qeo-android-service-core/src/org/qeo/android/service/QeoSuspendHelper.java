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

package org.qeo.android.service;

import java.util.logging.Logger;

import org.qeo.internal.java.BgnsCallbacks;
import org.qeo.jni.NativeQeo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.PowerManager;

/**
 * This is a class for facilitating suspend and resume of Qeo. Besides providing an API for suspending and resuming it
 * will also keep track of network changes when suspended so a temporary auto-resume can be triggered. This is needed to
 * possibly re-establish a connection with the background notification service.
 *
 * Note that this class registers a broadcast receiver for the network and WiFi state. It also takes a wake lock when it
 * auto-resumes after network changes. Therefore it will need the correct permissions:
 * <ul>
 * <li>android.permission.ACCESS_NETWORK_STATE</li>
 * <li>android.permission.ACCESS_WIFI_STATE</li>
 * <li>android.permission.WAKE_LOCK</li>
 * </ul>
 */
public final class QeoSuspendHelper implements BgnsCallbacks
{
    private static final Logger LOG = Logger.getLogger("QeoSuspendHelper");

    /** Number of seconds to wait before actually suspending (in seconds). */
    public static final int SUSPEND_DELAY = 2;
    /** Number of seconds to auto-resume (e.g. on connectivity changes) (in seconds). */
    public static final int AUTO_RESUME_TIMEOUT = 30;

    private static final int BGNS_TCP_KEEPCNT = 4; //4 retries
    private static final int BGNS_TCP_KEEPIDLE = 15 * 60; //15minutes
    private static final int BGNS_TCP_KEEPINTVL = 30; //30 sec
    private static boolean sBgnsTcpKeepaliveNonDefault = false;

    private final Context mContext;
    private int mAutoResumeTimeOut;
    private final BroadcastReceiver mConnectionMonitor = new MyConnectionMonitor();
    private boolean mSuspended = false; // is Qeo suspended?
    private boolean mAutoResumed = false; // did we auto-resume on network changes?
    private boolean mIsStickyCM = false; // to skip first sticky Connectivity event
    private boolean mIsStickyWM = false; // to skip first sticky WiFi event
    private int mSuspendRefCnt = 0;
    private PowerManager.WakeLock mWakeLock = null;
    private final Handler mHandler = new Handler();
    private boolean mBgnsConnected = false;
    private final BgnsConnectionAlarmReceiver mBgnsAlarmReceiver;
    private static QeoSuspendHelper sInstance = null;

    /**
     * Get singleton instance of QeoSuspendHelper.
     *
     * @param ctx Android context.
     * @return The singleton instance.
     */
    public static synchronized QeoSuspendHelper getInstance(Context ctx)
    {
        if (null == sInstance) {
            sInstance = new QeoSuspendHelper(ctx.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Construct the helper.
     * @param ctx Android context.
     */
    private QeoSuspendHelper(Context ctx)
    {
        if (ctx == null) {
            throw new IllegalStateException("Can't create suspend helper with null context");
        }
        mContext = ctx;
        NativeQeo.addBgnsCallbacks(this);
        mBgnsAlarmReceiver = new BgnsConnectionAlarmReceiver();
    }

    /**
     * Shutdown the helper instance and clean up resources.
     */
    public static synchronized void stop()
    {
        if (sInstance != null) {
            sInstance.close();
        }
        sInstance = null;
    }

    private synchronized void close()
    {
        // clean up broadcast receiver
        if (mSuspended) {
            mContext.unregisterReceiver(mConnectionMonitor);
        }
        NativeQeo.removeBgnsCallbacks(sInstance);
        if (null != mWakeLock && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
        mHandler.removeCallbacks(mSuspendRunnable);
        mHandler.removeCallbacks(mAutoSuspendRunnable);
        mBgnsAlarmReceiver.cancelConnectionPoller();
    }

    /**
     * Suspend all Qeo operations and register a broadcast receiver for monitoring network changes.
     *
     * @param suspendDelay      Number of seconds to delay an actual suspend (allow for data to be sent out before
     *                          suspending, avoid suspend/resume in rapid succession, ...).
     * @param autoResumeTimeOut Number of seconds to wait before suspending again after an auto-resume triggered by
     *                          network changes.
     */
    public synchronized void suspend(int suspendDelay, int autoResumeTimeOut)
    {
        mSuspendRefCnt++;
        LOG.info("suspend after " + suspendDelay + " seconds (refcnt = " + mSuspendRefCnt + ")");
        mAutoResumeTimeOut = autoResumeTimeOut;
        mHandler.removeCallbacks(mSuspendRunnable);
        mHandler.postDelayed(mSuspendRunnable, suspendDelay * 1000);
        mAutoResumed = false;
    }

    /**
     * Suspend all Qeo operations and register a broadcast receiver for monitoring network changes.
     */
    public synchronized void suspend()
    {
        suspend(SUSPEND_DELAY, AUTO_RESUME_TIMEOUT);
    }

    /**
     * Resume all Qeo operations and unregister the broadcast receiver.
     */
    public synchronized void resume()
    {
        mSuspendRefCnt--;
        if (mSuspendRefCnt < 0) {
            mSuspendRefCnt = 0;
        }
        LOG.info("resume (refcnt = " + mSuspendRefCnt + ")");
        // Resume all Qeo operations
        doResume();

        if (mSuspended) {
            // Disable connectivity monitoring
            mContext.unregisterReceiver(mConnectionMonitor);
        }
        mAutoResumed = false;
    }

    /**
     * Resume all Qeo operations and suspend again after the specified time out, unless a manual resume was executed.
     *
     * @param autoResumeTimeOut Number of seconds to wait before suspending again.
     */
    public synchronized void autoResume(int autoResumeTimeOut)
    {
        if (mSuspended && !mAutoResumed) {
            LOG.fine("Auto-resuming");

            /* Install a delayed handler for auto-suspending after some time. */
            mHandler.postDelayed(mAutoSuspendRunnable, autoResumeTimeOut * 1000);
            /* Auto-resume */
            doResume();
            mAutoResumed = true;
            /* Also take a wake lock to keep the device temporarily awake */
            PowerManager powerMgr = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "QeoSuspendHelper");
            mWakeLock.acquire();
        }
        else {
            LOG.fine("Ignoring auto-resume");
        }
    }

    /**
     * Resume all Qeo operations and suspend again after the default time out, unless a manual resume was executed.
     */
    public void autoResume()
    {
        autoResume(AUTO_RESUME_TIMEOUT);
    }

    @Override
    public void dispatchWakeUp(String typeName)
    {

    }

    @Override
    public void onBgnsConnected(int fd, boolean state)
    {
        LOG.fine("onBgnsConnected: " + state);
        mBgnsConnected = state;
        if (mBgnsConnected) {
            //connected. Stop polling if active.
            mBgnsAlarmReceiver.cancelConnectionPoller();
        }
        else {
            //connection lost. Resume to try to find a new connection.
            autoResume();
        }
    }

    private synchronized void doResume()
    {
        mSuspended = false;
        NativeQeo.resume();
    }

    private synchronized void doSuspend()
    {
        mSuspended = true;
        NativeQeo.suspend();
        if (!mBgnsConnected) {
            LOG.warning("Suspended while no connection with BGNS");
            mBgnsAlarmReceiver.startConnectionPoller(mContext);
        }
    }

    /**
     * Enable TCP keepalive on BGNS channel with default parameters.<br>
     * This must be called before starting Qeo.
     */
    public static void enableBgnsKeepalive()
    {
        if (!sBgnsTcpKeepaliveNonDefault) {
            //only set defaults if not overridden.
            enableBgnsKeepaliveSet(BGNS_TCP_KEEPCNT, BGNS_TCP_KEEPIDLE, BGNS_TCP_KEEPINTVL);
        }
    }

    /**
     * Enable TCP keepalive on BGNS channel.<br>
     * See http://linux.die.net/man/7/tcp for more details about the paramters.
     * This must be called before starting Qeo.
     * @param keepCnt The maximum number of keepalive probes TCP should send before dropping the connection.
     * @param keepIdle The time (in seconds) the connection needs to remain idle before TCP starts sending keepalive
     * probes.
     * @param keepIntvl The time (in seconds) between individual keepalive probes.
     */
    public static void enableBgnsKeepalive(int keepCnt, int keepIdle, int keepIntvl)
    {
        sBgnsTcpKeepaliveNonDefault = true;
        enableBgnsKeepaliveSet(keepCnt, keepIdle, keepIntvl);
    }

    private static void enableBgnsKeepaliveSet(int keepCnt, int keepIdle, int keepIntvl)
    {
        LOG.fine("Set BGNS keepalive paramters: " + keepCnt + ", " + keepIdle + ", " + keepIntvl);
        NativeQeo.setQeoParameter("FWD_BGNS_TCP_KEEPCNT", Integer.toString(keepCnt));
        NativeQeo.setQeoParameter("FWD_BGNS_TCP_KEEPIDLE", Integer.toString(keepIdle));
        NativeQeo.setQeoParameter("FWD_BGNS_TCP_KEEPINTVL", Integer.toString(keepIntvl));
    }

    private final Runnable mAutoSuspendRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            synchronized (QeoSuspendHelper.this) {
                if (mAutoResumed) {
                    LOG.fine("Auto-suspending");
                    doSuspend();
                    mAutoResumed = false;
                }
                if (null != mWakeLock && mWakeLock.isHeld()) {
                    mWakeLock.release();
                    mWakeLock = null;
                }
            }
        }
    };

    private final Runnable mSuspendRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (mContext == null) {
                LOG.warning("No context, ignoring suspend");
                return;
            }
            synchronized (QeoSuspendHelper.this) {
                if ((mSuspendRefCnt > 0) && (!mSuspended || mAutoResumed)) {
                    // Enable connectivity monitoring when suspending (auto-resume has not removed receiver)
                    if (!mAutoResumed) {
                        IntentFilter filter = new IntentFilter();
                        // ConnectivityManager for switching mobile data <-> WiFi data
                        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                        // WifiManager for switching of WiFi networks
                        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                        Intent i = mContext.registerReceiver(mConnectionMonitor, filter);
                        if (null != i) {
                            mIsStickyCM = true;
                            mIsStickyWM = true;
                        }
                    }
                    LOG.info("suspending (refcnt = " + mSuspendRefCnt + ")");

                    // Suspend all Qeo operations
                    doSuspend();
                }
            }
        }
    };

    /**
     * Internal class for network connection monitoring.
     */
    private class MyConnectionMonitor
        extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            synchronized (QeoSuspendHelper.this) {
                boolean connectivity = false;
                boolean ignoreSticky = false;
                String action = intent.getAction();

                if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    if (mIsStickyWM) {
                        mIsStickyWM = false;
                        ignoreSticky = true;
                    }
                    else {
                        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                        if ((null != networkInfo) && (networkInfo.isConnected())) {
                            connectivity = true;
                        }
                    }
                }
                else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    if (mIsStickyCM) {
                        mIsStickyCM = false;
                        ignoreSticky = true;
                    }
                    else {
                        boolean noConnectivity =
                            intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                        connectivity = !noConnectivity;
                    }
                }
                LOG.fine("onReceive(" + action + ") -> "
                    + (ignoreSticky ? "ignore sticky" : "connectivityChanged = " + connectivity));
                if (ignoreSticky) {
                    return;
                }
                if (connectivity) {
                    /*
                     * We have connectivity and are suspended. Either we switched networks (e.g. mobile <-> WiFi) or we
                     * got connected after being disconnected. We need to resume Qeo for some time for it to be able to
                     * reconnect to the background notification service (if needed).
                     */
                    autoResume(mAutoResumeTimeOut);
                }
            }
        }
    }
}
