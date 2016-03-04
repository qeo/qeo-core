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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.QeoFactory;
import org.qeo.android.internal.AidlConstants;
import org.qeo.android.internal.IServiceQeoCallback;
import org.qeo.android.service.db.DBHelper;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.QeoManagementApp;
import org.qeo.deviceregistration.service.OAuthTokenService;
import org.qeo.deviceregistration.service.RegisterService;
import org.qeo.deviceregistration.service.RemoteDeviceRegistration;
import org.qeo.deviceregistration.service.RemoteDeviceRegistrationService;
import org.qeo.exception.QeoException;
import org.qeo.internal.BaseFactory;
import org.qeo.internal.java.EntityAccessorJava;
import org.qeo.java.QeoConnectionListener;
import org.qeo.java.QeoJava;
import org.qeo.jni.NativeQeo;
import org.qeo.system.RegistrationRequest;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * This is the main Qeo service class. The Qeo service is responsible for communicating on DDS level.
 */
public class QeoService
    extends Service
{
    /** Tag used for QeoService logging. */
    public static final String TAG = "QeoService";

    /** Broadcast action to be sent out if security setup is complete. */
    public static final String SECURITY_SETUP_FINISHED = "org.qeo.android.security.SETUP_FINISHED";
    /** flag (boolean) added to SECURITY_SETUP_FINISHED in case of success. */
    public static final String INTENT_EXTRA_SUCCESS = "success";
    /** String added to SECURITY_SETUP_FINISHED containing the otc code. */
    public static final String INTENT_EXTRA_OTC = "otc";
    /** String added to SECURITY_SETUP_FINISHED containing the url. */
    public static final String INTENT_EXTRA_URL = "url";
    /** String added to SECURITY_SETUP_FINISHED containing error message. */
    public static final String INTENT_EXTRA_ERRORMSG = "errorMsg";
    /** Broadcast that the qeo service is destroyed. */
    public static final String ACTION_SERVICE_DESTROYED = "actionServiceDestroyed";
    private static final Logger LOG = Logger.getLogger(TAG);
    private MulticastLock mMulticastLock;
    private BaseFactory mQeoClosed;
    private BaseFactory mQeoOpen;
    private boolean mQeoReady = false;
    private boolean mQeoInitCalled = false;
    private boolean mOtpDialogCanceled = false;
    private String mLatestStatusUpdate = "";

    private final Object mQeoReadyLock = new Object();
    private Handler mHandler;
    private SQLiteDatabase mDatabase;
    private ConfigurableSettings mSettingsExternal;
    private ConfigurableSettings mSettingsInternal;
    private boolean mOpenDomainDisabled;
    private QeoConnectionListener mQeoConnectionListenerOpen;
    private QeoConnectionListener mQeoConnectionListenerClosed;
    private int mInitDoneCounter = 0;
    private int mPositiveResultCounter = 0;
    private String mQeoErrorReason = "";
    private boolean mDestroyed = false;
    private QeoServiceImpl mQeoServiceImpl;
    private QeoServiceV1 mBinderV1;
    private BroadcastReceiver mOnNetworkConnectivityChanged;
    private QeoSuspendHelper mQeoSuspendHelper;

    /**
     * Create a RemoteCallbackList to be able to detect when a remote client dies so that we can cleanup all zombie
     * datareaders and datawriters.
     *
     * http://developer.android.com/reference/android/os/RemoteCallbackList.html
     */
    private interface IRemoteCallbackDied<E extends IInterface>
    {
        void remoteCallbackDied(E callback, Object cookie);
    }

    /**
     * Represents the remote callback list.
     *
     * @param <E> The callback list type
     */
    private static final class RemoteCallbackListWithDiedCB<E extends IInterface>
        extends RemoteCallbackList<E>
    {
        private final IRemoteCallbackDied<E> mRemoteCallbackDied;

        public RemoteCallbackListWithDiedCB(IRemoteCallbackDied<E> remoteCallbackDied)
        {
            mRemoteCallbackDied = remoteCallbackDied;
        }

        @Override
        public void onCallbackDied(E callback, Object cookie)
        {
            super.onCallbackDied(callback, cookie);
            mRemoteCallbackDied.remoteCallbackDied(callback, cookie);
        }
    }

    private static class RemoteCallbackDied
        implements IRemoteCallbackDied<IServiceQeoCallback>
    {
        @Override
        public void remoteCallbackDied(IServiceQeoCallback iServiceQeo, Object cookie)
        {
            LOG.fine("Lost connection with client: " + iServiceQeo.asBinder());
            ConnectedWriter.removeWriters(iServiceQeo.asBinder());
            ConnectedReader.removeReaders(iServiceQeo.asBinder());
        }
    }

    /**
     * Check if all qeo factories needed are created.
     *
     * @param success If the native creation was success or not.
     */
    private synchronized void checkQeoReady(boolean success)
    {
        if (mDestroyed) {
            // Qeo is already destroyed, don't count this as positive
            success = false;
        }
        mInitDoneCounter++;
        if (success) {
            mPositiveResultCounter++;
        }
        int numNeeded = (mOpenDomainDisabled ? 1 : 2); // number of domains needed
        if (mInitDoneCounter == numNeeded) {
            if (mPositiveResultCounter == mInitDoneCounter) {
                // register receiver to monitor for network changes. Do this here since it's mandatory for DDS to be
                // started before calls from this listeners can work. Otherwise it would result in a deadlock.
                enableNetworkSettings();

                // start the Headless registration listener daemon if needed
                startHeadlessRegistrationListener();
            }
            else {
                mQeoInitCalled = false; // qeo init failed
            }
            if (mHandler != null) {
                mHandler.post(new Runnable()
                {

                    @Override
                    public void run()
                    {
                        /* For now: only continue if ALL factories could be created */
                        if (mPositiveResultCounter == mInitDoneCounter) {
                            mQeoServiceImpl.init(mQeoClosed, mQeoOpen);

                            // Done
                            LOG.info("Qeo init completed");

                            // notify peers that we're ready
                            LOG.fine("Informing Qeo Clients of Qeo Ready == true");
                            notifyClientsReady(true);
                        }
                        else {
                            // Failed
                            LOG.info("Qeo init failed");
                            notifyClientsReady(false);
                        }

                        LOG.fine("onQeoInitDone Runnable finished!");
                    }
                });
            }
        }
    }

    private class QeoServiceConnectionListener
        extends QeoConnectionListener
    {

        @Override
        public void onStatusUpdate(final String status, final String reason)
        {
            mLatestStatusUpdate = reason;
            LOG.fine("QeoService.onStatusUpdate(): [status=" + status + ", reason=" + reason + "]");
        }

        @Override
        public boolean onStartAuthentication()
        {
            LOG.fine("onStartAuthentication() start");

            synchronized (QeoService.this) {
                // start qeoService as a started service. This to ensure that qeoservice does not gets destroyed if
                // nobody
                // is bound anymore during registration.
                mHandler.removeCallbacks(mStopServiceAfterTimeoutRunnable);
                startService(new Intent(QeoService.this, QeoService.class));
            }

            // register broadcastreceiver to get notified of completion.
            IntentFilter iff = new IntentFilter();
            iff.addAction(QeoService.SECURITY_SETUP_FINISHED);
            LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(mOnSecurityNotice, iff);

            if (!checkClientsAuthentication()) {
                startRegisterService();
            }
            LOG.fine("onStartAuthentication() done");
            return true;
        }

        @Override
        public synchronized void onQeoReady(QeoFactory qeo)
        {
            BaseFactory factory = (BaseFactory) qeo;
            LOG.fine("onQeoReady start - domain: " + factory.getDomainId());

            if (factory.getDomainId() == QeoFactory.OPEN_ID) {
                mQeoOpen = factory;
            }
            else {
                mQeoClosed = factory;
            }

            checkQeoReady(true);
        }

        @Override
        public synchronized void onQeoError(QeoException ex)
        {
            LOG.log(Level.FINE, "got onQeoError", ex);
            if (ex != null) {
                mQeoErrorReason = ex.getMessage();
            }
            else {
                mQeoErrorReason = "";
            }
            checkQeoReady(false);
        }

        @Override
        public void onWakeUp(String typeName)
        {
            if (typeName == null) {
                LOG.warning("Suspending failed, sending empty wakeup");
                notifyClientsWakeUp(null);
                return;
            }

            LOG.log(Level.FINE, "got onWakeUp: " + typeName);
            if (QeoDefaults.isRemoteRegistrationListenerAvailable() && QeoManagementApp.isRealmAdmin()
                && typeName.equals(RegistrationRequest.class.getName())) {
                // Resume because we received something on the registration request topic.
                // TODO use plain resume + delayedSuspend
                mQeoSuspendHelper.autoResume();
            }
            else {
                notifyClientsWakeUp(typeName);
            }
        }

        @Override
        public void onBgnsConnectionChange(boolean connected)
        {
            LOG.log(Level.FINE, "got onBgnsConnected: " + connected);
            notifyClientsBgnsConnected(connected);
        }
    }

    /** Callback called whenever a connection is lost. */
    private final RemoteCallbackDied mRemoteCallbackDied = new RemoteCallbackDied();

    /** The actual callback list. */
    private final RemoteCallbackListWithDiedCB<IServiceQeoCallback> mClients =
        new RemoteCallbackListWithDiedCB<IServiceQeoCallback>(mRemoteCallbackDied);

    private String getTcpServer()
    {
        // For use as a regular client
        // This option will disable location service and connect to QeoForwarder at the given address.
        String tcpServer;
        // Check for shared preferences of the org.qeo.android.debugconfig package.
        tcpServer = mSettingsExternal.getString("tcpServer", null);
        if (tcpServer != null && !tcpServer.isEmpty()) {
            LOG.warning("Custom DDS tcpServer: " + tcpServer);
            return tcpServer;
        }
        else {
            return null;
        }
    }

    private synchronized void startRegisterService()
    {
        if (mHandler != null) {
            mHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    String mRealm = QeoManagementApp.getRealmName();
                    String mUserName = QeoManagementApp.getUserName();
                    String mDeviceName = QeoManagementApp.getDeviceName();

                    // Launch easy-install registration
                    Intent intent = new Intent(QeoService.this, RegisterService.class);
                    if (mRealm != null) {
                        LOG.info("Specific realm " + mRealm);
                        intent.putExtra(RegisterService.INTENT_EXTRA_REALMNAME, mRealm);
                    }
                    if (mUserName != null) {
                        LOG.info("Specific user name " + mUserName);
                        intent.putExtra(RegisterService.INTENT_EXTRA_USERNAME, mUserName);
                    }
                    if (mDeviceName != null) {
                        LOG.info("Specific device name " + mDeviceName);
                        intent.putExtra(RegisterService.INTENT_EXTRA_DEVICENAME, mDeviceName);
                    }
                    LOG.fine("Starting registerservice: " + intent);
                    startService(intent);
                }
            });
        }
    }

    private final BroadcastReceiver mOnSecurityNotice = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equals(SECURITY_SETUP_FINISHED)) {
                boolean success = intent.getBooleanExtra(INTENT_EXTRA_SUCCESS, false);

                if (success) {
                    final String otc = intent.getStringExtra(INTENT_EXTRA_OTC);
                    if (otc == null || otc.isEmpty()) {
                        throw new IllegalStateException("OTC missing");
                    }
                    String url = intent.getStringExtra(INTENT_EXTRA_URL);
                    if (url == null || url.isEmpty()) {
                        url = QeoDefaults.getPublicUrl();
                    }
                    LOG.fine("OTC received, passing it to native");
                    NativeQeo.setRegistrationCredentials(otc, url);
                    RemoteDeviceRegistrationService.checkStartStop(QeoService.this);
                }
                else {
                    String errorMsg = intent.getStringExtra(INTENT_EXTRA_ERRORMSG);
                    mOtpDialogCanceled = false;
                    LOG.warning("Device registration failed: " + errorMsg);
                    try {
                        // close factories.
                        if (mQeoConnectionListenerClosed != null) {
                            QeoJava.closeQeo(mQeoConnectionListenerClosed);
                        }
                        if (mQeoConnectionListenerOpen != null) {
                            QeoJava.closeQeo(mQeoConnectionListenerOpen);
                        }
                    }
                    catch (RuntimeException rte) {
                        LOG.log(Level.WARNING, "Failed to cancel registration", rte);
                    }
                    notifyClientsReady(false);
                }
            }
            else {
                throw new IllegalStateException("unhandled action: " + action);
            }

            synchronized (QeoService.this) {
                LOG.fine("Stopping started service after 10 sec");
                // stop started qeo service after 10 seconds. This will have no effect if somebody is bound
                mHandler.postDelayed(mStopServiceAfterTimeoutRunnable, 10000); // 10 sec
            }

        }
    };

    private final Runnable mStopServiceAfterTimeoutRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            LOG.fine("Stop started qeoservice");
            stopService(new Intent(QeoService.this, QeoService.class));
        }
    };

    // disable or enable UDP
    private void configureNetworkSettings(NetworkInfo activeNetworkInfo)
    {
        if (activeNetworkInfo != null) {
            switch (activeNetworkInfo.getType()) {
                case ConnectivityManager.TYPE_ETHERNET:
                case ConnectivityManager.TYPE_WIFI:
                    // only enable UDP on WIFI or ETHERNET
                    LOG.fine("enable UDP");
                    NativeQeo.setUdpMode(true);
                    break;
                default:
                    // disable UDP
                    LOG.fine("disable UDP");
                    NativeQeo.setUdpMode(false);
                    break;
            }
        }
    }

    private void enableNetworkSettings()
    {
        LOG.fine("Enable network change monitoring");
        mOnNetworkConnectivityChanged = new BroadcastReceiver()
        {
            @SuppressLint("Wakelock")
            @Override
            public void onReceive(Context context, Intent intent)
            {

                /*
                 * Check if there is still a connected active network interface. If not, take a wakelock to make sure
                 * DDS has the time to let all of it's timers time out properly. This is especially needed when the
                 * network goes down due to the device going in sleep mode.
                 */
                ConnectivityManager connMgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo activeNetworkInf = connMgr.getActiveNetworkInfo();
                if (((activeNetworkInf != null) && (!activeNetworkInf.isConnected())) || (activeNetworkInf == null)) {
                    PowerManager powerMgr = (PowerManager) getSystemService(POWER_SERVICE);
                    PowerManager.WakeLock wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

                    LOG.info("Take wakelock for 40 seconds");
                    wakeLock.acquire(40 * 1000);
                }

                configureNetworkSettings(activeNetworkInf);
            }

        };
        registerReceiver(mOnNetworkConnectivityChanged, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
        configureNetworkSettings(activeNetworkInfo);
    }

    /* Check if there is a client that wants to override authentication. */
    private boolean checkClientsAuthentication()
    {
        boolean result = false;
        synchronized (mQeoReadyLock) {
            int i = mClients.beginBroadcast();

            while (i > 0) {
                i--;
                try {
                    if (mClients.getBroadcastItem(i).onStartAuthentication()) {
                        LOG.info("Client implements its own authentication mechanism");
                        result = true;
                        break;
                    }
                }
                catch (final RemoteException e) {
                    // The RemoteCallbackList will take care of removing
                    // the dead object for us.
                }
            }
            mClients.finishBroadcast();
        }
        return result;
    }

    /* Notify all clients that Qeo is now ready or not. */
    private void notifyClientsReady(boolean success)
    {
        synchronized (this) {
            if (!success) {
                mQeoInitCalled = false; // qeo init failed.
                // make sure to stop headless registration service
                RegisterService.stopHeadlessRegistration(this);
            }
        }
        synchronized (mQeoReadyLock) {
            mQeoReady = success;

            int i = mClients.beginBroadcast();
            while (i > 0) {
                i--;
                try {
                    if (success) {
                        LOG.fine("Notify client: " + mClients.getBroadcastItem(i));
                        mClients.getBroadcastItem(i).onRegistered();
                    }
                    else {
                        if (mOtpDialogCanceled) {
                            LOG.warning("OTP dialog cancelled");
                            mClients.getBroadcastItem(i).onOtpDialogCanceled();
                        }
                        else {
                            LOG.warning("Security init failed: " + mLatestStatusUpdate);
                            if (mQeoErrorReason.equals("Invalid OTC")) {
                                mClients.getBroadcastItem(i).onSecurityInitFailed(mQeoErrorReason);
                            }
                            else {
                                mClients.getBroadcastItem(i).onSecurityInitFailed(mLatestStatusUpdate);
                            }
                        }
                    }
                }
                catch (final Exception e) {
                    // The RemoteCallbackList will take care of removing
                    // the dead object for us.
                }
            }
            mClients.finishBroadcast();
        }
    }

    /* Notify all clients of the waking up. */
    private void notifyClientsWakeUp(String typeName)
    {
        synchronized (mQeoReadyLock) {
            int i = mClients.beginBroadcast();

            while (i > 0) {
                i--;
                try {
                    mClients.getBroadcastItem(i).onWakeUp(typeName);
                }
                catch (final RemoteException e) {
                    // The RemoteCallbackList will take care of removing
                    // the dead object for us.
                }
            }
            mClients.finishBroadcast();
        }
    }

    private void notifyClientsBgnsConnected(boolean state)
    {
        synchronized (mQeoReadyLock) {
            int i = mClients.beginBroadcast();

            while (i > 0) {
                i--;
                try {
                    mClients.getBroadcastItem(i).onBgnsConnected(state);
                }
                catch (final RemoteException e) {
                    // The RemoteCallbackList will take care of removing
                    // the dead object for us.
                }
            }
            mClients.finishBroadcast();
        }
    }

    private Map<String, String> parseFlags(String flags)
    {
        Map<String, String> out = new HashMap<String, String>();
        for (String flag : flags.split(";")) {
            String[] flagSplit = flag.split("=");
            if (flagSplit.length == 2) {
                // ignore if not valid
                out.put(flagSplit[0], flagSplit[1]);
            }
        }
        return out;
    }

    // Set optional DDS flags from config app
    // this will only work if qeo-c is built with DEBUG=1
    private void setDdsFlags()
    {
        String flags = mSettingsExternal.getString("ddsFlags", null);
        if (flags != null) {
            for (Entry<String, String> e : parseFlags(flags).entrySet()) {
                NativeQeo.setDssFlag(e.getKey(), e.getValue());
            }
        }
    }

    private void setJunitConfigParameters()
    {
        String flags = mSettingsInternal.getString("qeoFlags", null);
        if (flags != null) {
            for (Entry<String, String> e : parseFlags(flags).entrySet()) {
                LOG.info("Set junit parameter " + e.getKey() + " = " + e.getValue());
                NativeQeo.setQeoParameter(e.getKey(), e.getValue());
            }
        }
    }

    private synchronized void checkOpenDomainDisabled()
    {
        // check if open domain is disabled in config app
        mOpenDomainDisabled = mSettingsExternal.getBoolean("openDomainDisabled", false);
    }

    private void checkDdsSecurityDisabled()
    {
        // check if dds security is disabled in config app
        boolean noSecurity = mSettingsExternal.getBoolean("ddsSecurityDisabled", false);
        if (noSecurity) {
            LOG.warning("Disabled security!!");
            NativeQeo.setSecurityDisabled(true);
        }
    }

    private void checkLocationServiceDisabled()
    {
        boolean noLocSrv = mSettingsExternal.getBoolean("locationServiceDisabled", false);
        if (noLocSrv) {
            LOG.warning("Disabled location service!!");
            NativeQeo.setQeoParameter("FWD_DISABLE_LOCATION_SERVICE", "1");
        }
    }

    private void checkDomainId()
    {
        int id;
        int defaultClosed = 128;
        int defaultOpen = 1;

        LOG.fine("getDomainId() start");
        id = mSettingsExternal.getInt("domainId", defaultClosed);
        if (id != defaultClosed) {
            NativeQeo.setQeoParameter("DDS_DOMAIN_ID_CLOSED", Integer.toString(id));
        }
        id = mSettingsExternal.getInt("domainIdOpen", defaultOpen);
        if (id != defaultOpen) {
            NativeQeo.setQeoParameter("DDS_DOMAIN_ID_OPEN", Integer.toString(id));
        }
        LOG.fine("getDomainId() done");
    }

    /**
     * return whether the manifest pop-up needs to be disabled according to the fact that an Oauth token is present or
     * if not, according to to the internal preferences.
     *
     * @return True if the manifest pop-up should be disabled, false otherwise.
     */
    public boolean isManifestPopupDisabled()
    {
        if (!QeoDefaults.isManifestEnabled()) {
            // manifest globally disabled
            return true;
        }
        String refreshToken = DeviceRegPref.getRefreshToken();

        if (refreshToken != null && !refreshToken.isEmpty()) {
            return true;
        }
        return mSettingsInternal.getBoolean("manifestPopupDisabled", false);
    }

    /**
     * Initialize nativeQeo before it can be used.
     *
     * @param ctx android context.
     */
    public static synchronized void initNative(Context ctx)
    {
        NativeQeo.setStoragePath(getStorageDir(ctx));

        NativeQeo.setDeviceInfo(DeviceInfoAndroid.getInstance().getDeviceInfo());
        // don't publish deviceInfo the the open domain
        NativeQeo.setQeoParameter("CMN_PUB_DEVICEINFO_OPEN_DOMAIN", "0");
    }

    /**
     * Get the qeo storage directory.
     *
     * @param ctx The android context.
     * @return The path.
     */
    public static String getStorageDir(Context ctx)
    {
        if (QeoDefaults.getQeoStorageDir() == null) {
            return ctx.getFilesDir().getAbsolutePath();
        }
        else {
            return QeoDefaults.getQeoStorageDir();
        }
    }

    private synchronized void initQeo()
    {
        if (mQeoInitCalled || mDestroyed) {
            return;
        }

        // initialize Qeo
        LOG.fine("Qeo.initQeo");

        // open sqlite database
        initDatabase();

        /**
         * Initialize Qeo (domain participant) >> this is blocking now!!!! Will be unblocked from native thread thanks
         * to calling setRegistrationCredentials or cancelRegistration. Those calls are done from within
         * BroadcastReceiver.onReceive()
         */
        Thread qeoInitThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    // Use Android's Log to make sure the version is always logged independent of the level.
                    Log.i(TAG, "Qeo-java version is " + QeoJava.getVersionString());

                    NativeQeo.clearState();

                    initNative(QeoService.this);

                    checkOpenDomainDisabled();
                    checkLocationServiceDisabled();
                    checkDdsSecurityDisabled();
                    checkDomainId();

                    setDdsFlags();
                    setJunitConfigParameters();

                    LOG.fine("Creating Native Factory CLOSED");

                    NativeQeo.setTcpServer(QeoFactory.DEFAULT_ID, getTcpServer());
                    mQeoConnectionListenerClosed = new QeoServiceConnectionListener();
                    if (!mDestroyed) {
                        QeoJava.initQeo(QeoFactory.DEFAULT_ID, mQeoConnectionListenerClosed);
                    }
                    else {
                        LOG.fine("Skipping initQeo for closed factory as service is destroyed");
                        mQeoConnectionListenerClosed.onQeoError(new QeoException("Service already stopped"));
                    }
                    if (!mOpenDomainDisabled) {
                        if (!mDestroyed) {
                            LOG.fine("Creating Native Factory OPEN");
                            NativeQeo.setTcpServer(QeoFactory.OPEN_ID, getTcpServer());
                            mQeoConnectionListenerOpen = new QeoServiceConnectionListener();
                            QeoJava.initQeo(QeoFactory.OPEN_ID, mQeoConnectionListenerOpen);
                        }
                        else {
                            LOG.fine("Skipping initQeo for open factory as service is destroyed");
                            mQeoConnectionListenerOpen.onQeoError(new QeoException("Service already stopped"));
                        }
                    }
                    LOG.fine("Creating Native Factory DONE");
                }
                catch (Exception e) {
                    LOG.log(Level.SEVERE, "Qeo native init failed", e);
                    synchronized (QeoService.this) {
                        // mark as not called again.
                        mQeoInitCalled = false;
                    }
                }
            }
        }, "QeoInitThread");

        qeoInitThread.start();
        mQeoInitCalled = true;
    }

    private synchronized void initDatabase()
    {
        if (mDatabase == null) {
            // open sqlite database
            DBHelper dbHelper = new DBHelper(this);
            mDatabase = dbHelper.getWritableDatabase();
        }
    }

    /**
     * Get SQlite database handle for the service.
     *
     * @return database handle
     */
    public SQLiteDatabase getDatabase()
    {
        initDatabase();
        return mDatabase;
    }

    @Override
    public void onCreate()
    {
        // initialize QeoSuspendHelper singleton
        mQeoSuspendHelper = QeoSuspendHelper.getInstance(this);
        // don't do any calls to native here as this will init the native libraries on the UI thread!
        LOG.info("Creating service");
        if (mDestroyed) {
            throw new IllegalStateException("This service is already destroyed");
        }

        // Init different version of the AIDL interfaces
        mQeoServiceImpl = new QeoServiceImpl(this);
        mBinderV1 = new QeoServiceV1(mQeoServiceImpl);

        // init ServiceApplication. Only really needed if the service is embedded, but won't do anything if running
        // standalone so it's not harm in calling it here.
        ServiceApplication.initServiceApp(getApplicationContext());

        mSettingsExternal = ServiceApplication.getProperties().getExternalSettings();
        mSettingsInternal = new ConfigurableSettings(this, ConfigurableSettings.FILE_QEO_PREFS, false);
        synchronized (this) {
            mHandler = new Handler(Looper.getMainLooper());
        }

        /* Uncomment the next line if you want to debug the Qeo service */
        // android.os.Debug.waitForDebugger();

        final WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            // take multicast lock. Not required on all devices as some have
            // this by default enabled in their drivers.
            mMulticastLock = wifi.createMulticastLock("mQeo");
            mMulticastLock.acquire();
        }
        mOnNetworkConnectivityChanged = null;
    }

    @Override
    public void onDestroy()
    {
        LOG.info("onDestroy(destroying service)");
        mDestroyed = true;

        synchronized (this) {
            // clear everything that would still be pending
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mOnSecurityNotice);
        if (mOnNetworkConnectivityChanged != null) {
            unregisterReceiver(mOnNetworkConnectivityChanged);
        }

        LocalBroadcastManager.getInstance(this).sendBroadcastSync(new Intent(ACTION_SERVICE_DESTROYED));

        // remove all dangling reader/writers
        ConnectedReader.closeAll();
        ConnectedWriter.closeAll();

        stopHeadlessRegistrationListener();

        // pre destroy
        NativeQeo.preDestroy();

        if (mQeoConnectionListenerClosed != null) {
            QeoJava.closeQeo(mQeoConnectionListenerClosed);
        }
        if (mQeoConnectionListenerOpen != null) {
            QeoJava.closeQeo(mQeoConnectionListenerOpen);
        }

        if (mDatabase != null) {
            mDatabase.close();
            mDatabase = null;
        }

        // post destroy
        NativeQeo.postDestroy();
        NativeQeo.clearState();

        // Tell the user we stopped.
        if (mMulticastLock != null) {
            mMulticastLock.release();
            mMulticastLock = null;
        }
        QeoSuspendHelper.stop();
        mQeoSuspendHelper = null;
        LOG.info("onDestroy(done)");
    }

    private void startHeadlessRegistrationListener()
    {
        if (QeoDefaults.isRemoteRegistrationListenerAvailable() && QeoManagementApp.isRealmAdmin()) {
            // only start if the user is admin
            RemoteDeviceRegistration deviceRegisterQeoService = RemoteDeviceRegistration.getInstance();
            deviceRegisterQeoService.start(this, mQeoOpen, mQeoClosed);
        }
    }

    private void stopHeadlessRegistrationListener()
    {
        RemoteDeviceRegistration deviceRegisterQeoService = RemoteDeviceRegistration.getInstance();
        deviceRegisterQeoService.stop();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // binder should return the correct version of the requested aidl interface. Make sure to update
        // QeoServiceVersion too if new version is created.
        mOtpDialogCanceled = false;
        String action = intent.getAction();
        if (action != null) {
            LOG.info("Binding to Qeo Service (action: " + action + ", type: " + intent.getType() + ")");
        }
        if (AidlConstants.AIDL_SERVICE_ACTION_V1.equals(action)) {
            return mBinderV1;
        }
        else {
            // default
            return mBinderV1;
        }

    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        LOG.fine("unbinding to Qeo Service");
        mBinderV1.onUnbind();
        return true;
    }

    private void checkDelayedStop()
    {
        QeoServiceTimeout.bindToService(getApplicationContext());
    }

    /**
     * Register service callback and init qeo if needed.
     *
     * @param cb The callback
     * @throws RemoteException if an error occurs.
     */
    void register(IServiceQeoCallback cb)
        throws RemoteException
    {
        LOG.fine("OnRegister");
        synchronized (mQeoReadyLock) {
            mClients.register(cb);
            if (mQeoReady) {
                /* Qeo is already ready, so we can notify the peer directly from here */
                cb.onRegistered();
            }
        }
        // initialize Qeo
        initQeo();
    }

    /**
     * Unregister service callback.
     *
     * @param cb The callback.
     * @throws RemoteException if an error occurs.
     */
    void unregister(IServiceQeoCallback cb)
        throws RemoteException
    {
        LOG.fine("OnUnregister");
        synchronized (mQeoReadyLock) {
            mClients.unregister(cb);
        }
        cb.onUnregistered();

        // start timeout service if client stops.
        checkDelayedStop();
    }

    /**
     * Trigger a policy refresh.
     */
    void refreshPolicy()
    {
        try {
            if (mQeoClosed == null) {
                LOG.severe("Qeo not yet initialized while trying to refresh policy");
                throw new IllegalStateException("Qeo not yet initialized");
            }
            LOG.fine("Refresh policy");
            ((EntityAccessorJava) mQeoClosed.getEntityAccessor()).getNativeFactory().refreshPolicy();
        }
        catch (Throwable e) {
            // catch everything, they can't be sent over AIDL
            LOG.log(Level.WARNING, "Unable to refresh policy", e);
        }
    }

    private void registerOauthReceiver()
    {
        // register broadcastreceiver to get notified when OAUthTokenService is ready.
        IntentFilter ifo = new IntentFilter();
        ifo.addAction(OAuthTokenService.ACTION_OAUTH_TOKEN_READY);

        BroadcastReceiver onOAuthTokenReady = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                LOG.fine("mOnOAuthTokenReady onReceive: " + intent.getAction());
                String action = intent.getAction();
                if (action.equals(OAuthTokenService.ACTION_OAUTH_TOKEN_READY)) {
                    LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
                    LOG.fine("Start registerService");
                    startRegisterService();
                }
                else {
                    throw new IllegalStateException("unhandled action: " + action);
                }
            }
        };
        LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(onOAuthTokenReady, ifo);
    }

    /**
     * Continue authentication.
     *
     * @param type code type
     * @param data the authentication data
     */
    void continueAuthentication(int type, String data)
    {
        LOG.fine("Got OAuth/JWT code from application, starting OAuthTokenService");
        // start the oauth service
        Intent i = new Intent(this, OAuthTokenService.class);
        switch (type) {
            case AidlConstants.AUTHENTICATION_DATA_CODE:
                i.putExtra(OAuthTokenService.INTENT_EXTRA_OAUTH_CODE, data);
                registerOauthReceiver();
                startService(i);
                break;
            case AidlConstants.AUTHENTICATION_DATA_JWT:
                i.putExtra(OAuthTokenService.INTENT_EXTRA_JWT, data);
                registerOauthReceiver();
                startService(i);
                break;
            case AidlConstants.AUTHENTICATION_DATA_CANCEL:
                LOG.info("Cancel registration");
                NativeQeo.cancelRegistration();
                stopService(new Intent(QeoService.this, QeoService.class));
                break;
            case AidlConstants.AUTHENTICATION_DATA_OTC: {
                String[] dataSplit = data.split("\\|");
                if (dataSplit.length != 2) {
                    LOG.warning("Invalid OTC data: " + data);
                    break;
                }
                i = new Intent(SECURITY_SETUP_FINISHED);
                i.putExtra(INTENT_EXTRA_SUCCESS, true);
                i.putExtra(INTENT_EXTRA_OTC, dataSplit[0]);
                i.putExtra(INTENT_EXTRA_URL, dataSplit[1]);
                LocalBroadcastManager.getInstance(this).sendBroadcast(i);
                break;
            }
            default:
                LOG.warning("Unknown type: " + type);
        }
    }

    /**
     * Suspend Qeo operations. All non-vital network connections are closed and timers are stopped.
     *
     * @see org.qeo.Notifiable
     * @see org.qeo.java.QeoConnectionListener#onWakeUp(java.lang.String)
     * @see #resume()
     */
    void suspend()
    {
        LOG.fine("Suspend operations");
        mQeoSuspendHelper.suspend();
    }

    /**
     * Resume Qeo operations. Re-establishes all connections as if nothing happened.
     *
     * @see #suspend()
     */
    void resume()
    {
        LOG.fine("Resume operations");
        mQeoSuspendHelper.resume();
    }
}
