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

package org.qeo.deviceregistration.ui;

import java.util.logging.Logger;

import org.qeo.android.security.OtcActivity;
import org.qeo.android.service.QeoService;
import org.qeo.android.service.ServiceApplication;
import org.qeo.android.service.ui.BuildConfig;
import org.qeo.android.service.ui.R;
import org.qeo.deviceregistration.service.HeadlessRegistrationQeoService;
import org.qeo.deviceregistration.service.OAuthTokenService;
import org.qeo.deviceregistration.service.RegisterService;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;

/**
 * WebView to ask for google login credentials to user.
 */
public class WebViewActivity
    extends SherlockFragmentActivity

{
    /**
     * Flag (boolean) to indicate that registration is triggered from an external app. This will change the behavior as
     * following:<br>
     * Headless registration is activated<br>
     * OTC dialog is activated<br>
     * Activity will be aborted on home button press<br>
     */
    public static final String INTENT_EXTRA_STARTED_FROM_EXTERNAL_APP = "startedFromExternalApp";

    private static final Logger LOG = Logger.getLogger("WebViewActivity");
    /** OAuth code that will be set as result of this activity. */
    public static final String INTENT_EXTRA_OAUTH_CODE = "oauthCode";
    private LocalBroadcastManager mLbm;
    private boolean mStartedFromExternalAppp;
    private boolean mIsConfigChange;
    private boolean mOtcActivityStarted;
    private static boolean sAbortOnStop = false;
    private MyBroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        LOG.fine("onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_single_fragment);
        mOtcActivityStarted = false;
        mLbm = LocalBroadcastManager.getInstance(this);

        if (savedInstanceState == null) {
            String googlePlusId = ServiceApplication.getMetaData(ServiceApplication.META_DATA_GOOGLE_CLIENT_ID);
            Fragment fragment;
            if (googlePlusId.isEmpty()) {
                //google openId connect disabled. Skip to regular openid login screen
                LOG.warning("Google+ login disabled.");
                fragment = new WebviewFragment();
            }
            else {
                fragment = new GplusFragment();
            }
            getSupportFragmentManager().beginTransaction().add(R.id.containerSingleFragment, fragment).commit();
        }

        mStartedFromExternalAppp = getIntent().getBooleanExtra(INTENT_EXTRA_STARTED_FROM_EXTERNAL_APP, false);
        LOG.fine("need to start headless registration? " + mStartedFromExternalAppp);
        if (mStartedFromExternalAppp) {
            RegisterService.startHeadlessRegistration(this);
        }

        // register receiver to handle results
        IntentFilter iff = new IntentFilter();
        iff.addAction(OAuthTokenService.ACTION_OAUTH_TOKEN_READY);
        iff.addAction(RegisterService.ACTION_HEADLESS_REGISTRATION_DONE);
        iff.addAction(OtcActivity.ACTION_OTC_REGISTRATION_DONE);
        iff.addAction(QeoService.ACTION_SERVICE_DESTROYED);
        mBroadcastReceiver = new MyBroadcastReceiver();
        mLbm.registerReceiver(mBroadcastReceiver, iff);
    }

    private void checkAbort()
    {
        if (mStartedFromExternalAppp && !isChangingConfigurationsCompat() && !mOtcActivityStarted) {
            LOG.fine("Aborting due to going to background");
            broadcastAbort();
            finish();
        }
    }

    @Override
    public void onStop()
    {
        if (sAbortOnStop) {
            checkAbort();
        }
        super.onStop();
    }

    @Override
    public void onDestroy()
    {
        if (!sAbortOnStop) {
            checkAbort();
        }
        if (mStartedFromExternalAppp) {
            LOG.fine("stopping headless registration");
            RegisterService.stopHeadlessRegistration(this);
        }
        if (mBroadcastReceiver != null) {
            // unregister receiver if not yet done.
            mLbm.unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
        super.onDestroy();
    }

    /**
     * Adds support for isChangingConfigurations when minSdkVersion targets api-levels below 11.
     * 
     * Unfortunately onSaveInstanceState is invoked AFTER onPause, so on platforms below 11 isChangingConfigurations
     * will only report correctly in onStop() or onDestroy().
     */
    @SuppressLint("NewApi")
    private boolean isChangingConfigurationsCompat()
    {
        // override
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            return isChangingConfigurations();
        }
        else {
            return mIsConfigChange;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        mIsConfigChange = true;
    }

    @Override
    public void onBackPressed()
    {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.containerSingleFragment);
        LOG.fine("Back button pressed: " + fragment);
        if (fragment instanceof WebviewFragment) {
            WebviewFragment webviewFragment = (WebviewFragment) fragment;
            if (!webviewFragment.tryGoBack()) {
                // natigate back to gplusfragment
                super.onBackPressed();
            }
        }
        else {
            // we're on the gplusfragment. pressing back here means abort.
            broadcastAbort();
            finish();
        }
    }

    /**
     * Set if otc actvity has been started.
     * @param value True if started.
     */
    void setOtcStarted(boolean value)
    {
        mOtcActivityStarted = value;
    }

    /**
     * Indicate if OTC option should be active.
     * @return True if active. False otherwise.
     */
    boolean isOtcEnabled()
    {
        return mStartedFromExternalAppp;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment != null) {
                LOG.fine("Delivering result to fragment " + fragment);
                fragment.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    /**
     * Configure to abort on stop or on destroy.
     * 
     * @param value true for stop, false for destroy.
     */
    public static void setAbortOnStop(boolean value)
    {
        sAbortOnStop = value;
    }

    /**
     * Login ok.
     */
    private void broadcastSuccess(String otc, String url)
    {
        LOG.fine("Broadcast success");
        final Intent intent = new Intent(RegisterService.ACTION_LOGIN_FINISHED);
        intent.putExtra(RegisterService.INTENT_EXTRA_SUCCESS, true);
        if (otc != null) {
            intent.putExtra(RegisterService.INTENT_EXTRA_OTC, otc);
        }
        if (url != null) {
            intent.putExtra(RegisterService.INTENT_EXTRA_URL, url);
        }
        mLbm.sendBroadcastSync(intent);
    }

    /**
     * Error occurred.
     * 
     * @param msg Error message
     */
    public void broadcastError(String msg)
    {
        LOG.fine("Broadcast error: " + msg);
        final Intent intent = new Intent(RegisterService.ACTION_LOGIN_FINISHED);
        intent.putExtra(RegisterService.INTENT_EXTRA_ERRORMSG, msg);
        intent.putExtra(RegisterService.INTENT_EXTRA_ERROR, true);
        intent.putExtra(RegisterService.INTENT_EXTRA_SUCCESS, false);
        mLbm.sendBroadcastSync(intent);
    }

    /**
     * Aborted by the user, not a real error.
     */
    private void broadcastAbort()
    {
        LOG.fine("Broadcast abort");
        final Intent intent = new Intent(RegisterService.ACTION_LOGIN_FINISHED);
        intent.putExtra(RegisterService.INTENT_EXTRA_SUCCESS, false);
        mLbm.sendBroadcastSync(intent);
    }

    /** Broadcast handler that will be signalled after oauth login. */
    private class MyBroadcastReceiver
        extends BroadcastReceiver
    {
        private void broadcast(boolean result, String otc, String url)
        {
            if (result) {
                broadcastSuccess(otc, url);
            }
            else {
                broadcastError(null);
            }
            finish();
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            LOG.fine("Got security finished broadcast: " + intent.getAction());
            String action = intent.getAction();
            if (action.equals(OAuthTokenService.ACTION_OAUTH_TOKEN_READY)) {
                // unregister self
                mLbm.unregisterReceiver(mBroadcastReceiver);
                mBroadcastReceiver = null;
                boolean result = intent.getBooleanExtra(OAuthTokenService.INTENT_EXTRA_SUCCESS, false);
                if (!result) {
                    String errorMsg = intent.getStringExtra(OAuthTokenService.INTENT_EXTRA_ERRORMSG);
                    AlertDialog.Builder errorDialogBuilder = new AlertDialog.Builder(WebViewActivity.this);
                    errorDialogBuilder.setTitle("Error with OAuth login");
                    errorDialogBuilder.setMessage("Error login in to Qeo. Please try again later.\n" + errorMsg);
                    errorDialogBuilder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            broadcast(false, null, null);
                        }
                    });
                    errorDialogBuilder.show();
                }
                else {
                    broadcast(result, null, null);
                }
                if (BuildConfig.DEBUG) {
                    // UIautomator uses this, don't remove it. Only print on debug
                    LOG.info("loaded url [OAuth token ready]");
                }

            }
            else if (action.equals(RegisterService.ACTION_HEADLESS_REGISTRATION_DONE)) {
                LOG.fine("Showing headless registration accept dialog");
                final boolean result =
                    intent.getBooleanExtra(RegisterService.INTENT_EXTRA_SUCCESS, false);
                final String otc = intent.getStringExtra(RegisterService.INTENT_EXTRA_OTC);
                final String url = intent.getStringExtra(RegisterService.INTENT_EXTRA_URL);
                final String realm = intent.getStringExtra(HeadlessRegistrationQeoService.INTENT_EXTRA_REALMNAME);
                AlertDialog.Builder acceptDialogBuilder = new AlertDialog.Builder(WebViewActivity.this);
                acceptDialogBuilder.setTitle(getString(R.string.remote_registration_done_title));
                acceptDialogBuilder.setMessage(getString(R.string.remote_registration_done_msg, realm));
                acceptDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        // yes button
                        // unregister self
                        mLbm.unregisterReceiver(mBroadcastReceiver);
                        mBroadcastReceiver = null;
                        broadcast(result, otc, url);
                    }
                });
                acceptDialogBuilder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        // no button
                        dialog.dismiss();
                    }
                });
                acceptDialogBuilder.show();
            }
            else if (action.equals(OtcActivity.ACTION_OTC_REGISTRATION_DONE)) {
                mOtcActivityStarted = false;
                final boolean result = intent.getBooleanExtra(OtcActivity.INTENT_EXTRA_RESULT, false);
                final String otc = intent.getStringExtra(OtcActivity.INTENT_EXTRA_OTC);
                final String url = intent.getStringExtra(OtcActivity.INTENT_EXTRA_URL);
                if (result) {
                    // unregister self
                    mLbm.unregisterReceiver(mBroadcastReceiver);
                    mBroadcastReceiver = null;
                    broadcastSuccess(otc, url);
                    finish();
                }
            }
            else if (action.equals(QeoService.ACTION_SERVICE_DESTROYED)) {
                // unregister self
                mLbm.unregisterReceiver(mBroadcastReceiver);
                broadcastAbort();
                finish();
            }
            else {
                throw new IllegalStateException("unhandled action: " + action);
            }
        }
    }
}
