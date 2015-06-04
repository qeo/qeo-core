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

package org.qeo.deviceregistration.service;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.qeo.android.service.DeviceInfoAndroid;
import org.qeo.android.service.QeoService;
import org.qeo.android.service.db.DBHelper;
import org.qeo.android.service.db.TableInfo;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.QeoManagementApp;
import org.qeo.deviceregistration.helper.RealmCache;
import org.qeo.deviceregistration.rest.RestHelper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

/**
 * This service will take care of add realm, user and device making use of the REST API. Then the device will be
 * registered. But first, a check will be done to see if log in has been done before.<br>
 * If login is required, there should be a BroadcastReceiver listening to {@link #ACTION_LOGIN_REQUIRED}<br>
 * If this service succeeds, it will broadcast {@link #ACTION_REGISTRATION_DONE}
 */
public class RegisterService
    extends IntentService
{
    /** Tag used for RegisterService logging. */
    public static final String TAG = "RegisterService";
    /** String. Intent parameter realmName. */
    public static final String INTENT_EXTRA_REALMNAME = "INTENT_EXTRA_REALMNAME";
    /** String. Intent parameter userName. */
    public static final String INTENT_EXTRA_USERNAME = "INTENT_EXTRA_USERNAME";
    /** String. Intent parameter deviceName. */
    public static final String INTENT_EXTRA_DEVICENAME = "INTENT_EXTRA_DEVICENAME";


    /**
     * This action will be broadcasted when this service has completed.
     * It will have extras {@link #INTENT_EXTRA_REALMNAME} and {@link #INTENT_EXTRA_USERNAME}.
     */
    public static final String ACTION_REGISTRATION_DONE = "org.qeo.deviceregistration.service.ACTION_REGISTRATION_DONE";

    /**
     * If this service requires a login action first, it will listen for this action to be broadcasted to continue.
     * It accepts the following intent extras:
     * <ul>
     *     <li>(required) {@link #INTENT_EXTRA_SUCCESS}</li>
     *     <li>(required) {@link #INTENT_EXTRA_OTC}</li>
     *     <li>(optional) {@link #INTENT_EXTRA_URL}</li>
     *     <li>(optional) {@link #INTENT_EXTRA_ERRORMSG}</li>
     * </ul>
     */
    public static final String ACTION_LOGIN_FINISHED = "org.qeo.deviceregistration.service.ACTION_LOGIN_FINISHED";

    /**
     * Broadcast that will be sent if authentication to the Qeo SMS is required.<br>
     * The receiver that consumes this action should reply with {@link #ACTION_LOGIN_FINISHED} when finished.<br>
     * Optionally headless registration mode can be started at this point by calling
     * {@link #startHeadlessRegistration(android.content.Context)}
     */
    public static final String ACTION_LOGIN_REQUIRED = "org.qeo.deviceregistration.service.ACTION_LOGIN_REQUIRED";

    /**
     * This action will be broadcasted if the device is in headless registration mode.
     * {@link #startHeadlessRegistration(android.content.Context)} should be used to start this mode.
     * <p>It will have {@link #INTENT_EXTRA_SUCCESS} set to true on success. In that case the following extra
     * properties will set:</p>
     * <ul>
     *     <li>{@link #INTENT_EXTRA_OTC}</li>
     *     <li>{@link #INTENT_EXTRA_URL}</li>
     *     <li>{@link org.qeo.deviceregistration.service.HeadlessRegistrationQeoService#INTENT_EXTRA_REALMNAME}</li>
     * </ul>
     * <p>Reply should be done by broadcasting {@link #ACTION_LOGIN_FINISHED}</p>
     *
     */
    public static final String ACTION_HEADLESS_REGISTRATION_DONE
        = "org.qeo.deviceregistration.service.ACTION_HEADLESS_REGISTRATION_DONE";

    /** Boolean. indicates if the action succeeded or not. */
    public static final String INTENT_EXTRA_SUCCESS = "INTENT_EXTRA_SUCCESS";
    /** String. The OTC to be passed. */
    public static final String INTENT_EXTRA_OTC = "INTENT_EXTRA_OTC";
    /** String. The URL to be passed. */
    public static final String INTENT_EXTRA_URL = "INTENT_EXTRA_URL";
    /** Boolean. Indicates an error. */
    public static final String INTENT_EXTRA_ERROR = "INTENT_EXTRA_ERROR";
    /** String. Error message. */
    public static final String INTENT_EXTRA_ERRORMSG = "INTENT_EXTRA_ERRORMSG";


    private static final Logger LOG = Logger.getLogger(TAG);
    private String mRealmName = null;
    private String mUserName = null;
    private String mDeviceName = null;
    private long mRealmId = -1;
    private long mUserId = -1;
    private LocalBroadcastManager mLbm;

    /**
     * Constructor of this intent service .
     */
    public RegisterService()
    {
        super("RegisterService");
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        QeoManagementApp.init(); // Initialize global variables
        mLbm = LocalBroadcastManager.getInstance(this);
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        if (DeviceRegPref.getRefreshToken() == null) {
            doLogin(intent.getExtras());
            return;
        }

        try {
            // getting intent parameters
            mRealmName = intent.getStringExtra(INTENT_EXTRA_REALMNAME);
            if (mRealmName == null) {
                mRealmName = DeviceRegPref.getSelectedRealm();
                if (mRealmName == null || mRealmName.isEmpty()) {
                    mRealmName = "QeoHome"; // default Realm name
                }
            }
            mUserName = intent.getStringExtra(INTENT_EXTRA_USERNAME);
            if (mUserName == null) {
                // Default username
                mUserName = getUserName(this.getBaseContext());
            }
            mDeviceName = intent.getStringExtra(INTENT_EXTRA_DEVICENAME);
            if (mDeviceName == null) {
                // default device name
                DeviceInfoAndroid device = DeviceInfoAndroid.getInstance();
                mDeviceName = device.getDeviceInfo().userFriendlyName;
            }
            LOG.fine("Add realm (" + mRealmName + ") for user (" + mUserName + ") and device (" + mDeviceName + ")");

            // start creating
            RestHelper restHelper = new RestHelper(this);
            mRealmId = RestHelper.addRealm(mRealmName);
            RealmCache.putRealmName(mRealmId, mRealmName);
            mUserId = restHelper.addUserWithBroadcast(mRealmId, mUserName);
            RealmCache.putUserName(mUserId, mUserName);
            String otc = RestHelper.addDevice(mRealmId, mUserId, mDeviceName);

            // store created realm/user in settings
            DeviceRegPref.edit().setSelectedRealmId(mRealmId, mRealmName).apply();

            // store information in databsae
            DBHelper dbHelper = new DBHelper(this);
            TableInfo tableInfo = new TableInfo(dbHelper);
            tableInfo.insert(TableInfo.KEY_REALM_NAME, mRealmName);
            tableInfo.insert(TableInfo.KEY_REALM_USERNAME, mUserName);
            dbHelper.close();

            // create notification
            publishNotification();

            broadcastSuccess(this, otc, DeviceRegPref.getScepServerURL());
        }
        catch (IOException e) {
            LOG.log(Level.WARNING, "Error registering device", e);
            broadcastFailure(e.getMessage());
        }
        catch (JSONException e) {
            LOG.log(Level.WARNING, "Error registering device", e);
            broadcastFailure(e.getMessage());
        }
        stopSelf();
    }

    /**
     * This will put your device in headless registration mode. Another device can now register you.<br>
     * If this happens, {@link #ACTION_HEADLESS_REGISTRATION_DONE} will be broadcasted. You must register a receiver
     * for this action or nothing will happen.
     * @param ctx The android context.
     */
    public static void startHeadlessRegistration(Context ctx)
    {
        Intent headlessService = new Intent(ctx, HeadlessRegistrationQeoService.class);
        ctx.startService(headlessService);
    }

    /**
     * Stop the headless registration service.
     * @param ctx The android context.
     */
    public static void stopHeadlessRegistration(Context ctx)
    {
        Intent headlessService = new Intent(ctx, HeadlessRegistrationQeoService.class);
        ctx.stopService(headlessService);
    }

    private void doLogin(Bundle extras)
    {
        mLbm.registerReceiver(new WebviewLogin(extras), new IntentFilter(ACTION_LOGIN_FINISHED));
        LOG.fine("Asking for login: " + ACTION_LOGIN_REQUIRED);
        if (!mLbm.sendBroadcast(new Intent(ACTION_LOGIN_REQUIRED))) {
            LOG.severe("Nobody handles action " + ACTION_LOGIN_REQUIRED);
        }
    }

    /**
     * Get the default username for registering.
     *
     * @param ctx The android context.
     * @return A default username to be used for registering.
     */
    public static String getUserName(Context ctx)
    {
        AccountManager manager = (AccountManager) ctx.getSystemService(ContextWrapper.ACCOUNT_SERVICE);
        Account[] list = manager.getAccounts();
        String account = "";

        for (Account acc : list) {
            if (acc.type.equalsIgnoreCase("com.google")) {
                account = acc.name;
                break;
            }
        }
        if (account.isEmpty()) {
            account = "User_" + DeviceInfoAndroid.getInstance().getDeviceInfo().userFriendlyName;
        }

        return account;
    }

    private void publishNotification()
    {
        Intent i = new Intent(ACTION_REGISTRATION_DONE);
        i.putExtra(INTENT_EXTRA_REALMNAME, mRealmName);
        i.putExtra(INTENT_EXTRA_USERNAME, mUserName);
        mLbm.sendBroadcast(i);
    }

    private static void broadcastSuccess(Context ctx, String otc, String url)
    {
        LOG.fine("broadcastSecurityFinished start");
        final Intent intent = new Intent(QeoService.SECURITY_SETUP_FINISHED);
        intent.putExtra(QeoService.INTENT_EXTRA_SUCCESS, true);
        intent.putExtra(QeoService.INTENT_EXTRA_OTC, otc);
        intent.putExtra(QeoService.INTENT_EXTRA_URL, url);

        LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
        LOG.fine("broadcastSecurityFinished end");
    }

    private void broadcastFailure(String errorMsg)
    {
        Intent i = new Intent(QeoService.SECURITY_SETUP_FINISHED);
        i.putExtra(QeoService.INTENT_EXTRA_SUCCESS, false);
        i.putExtra(QeoService.INTENT_EXTRA_ERRORMSG, errorMsg);
        mLbm.sendBroadcast(i);
    }

    private class WebviewLogin
        extends BroadcastReceiver
    {
        private final Bundle mExtras;

        public WebviewLogin(Bundle extras)
        {
            mExtras = extras;
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            mLbm.unregisterReceiver(this); // unregister self

            boolean success = intent.getBooleanExtra(INTENT_EXTRA_SUCCESS, false);
            if (success) {
                String otc = intent.getStringExtra(INTENT_EXTRA_OTC);
                String url = intent.getStringExtra(INTENT_EXTRA_URL);
                if (otc != null) {
                    // otc set, just broadcast
                    broadcastSuccess(context, otc, url);
                    return;
                }

                // otc not set
                // restart self to start easy-install registration after authentication is done.
                Intent i = new Intent(context, RegisterService.class);
                if (mExtras != null) {
                    // restore original extras
                    i.putExtras(mExtras);
                }

                startService(i);
            }
            else {
                LOG.warning("Problem in oauth login");
                broadcastFailure(intent.getStringExtra(INTENT_EXTRA_ERRORMSG));
            }
        }
    }

}
