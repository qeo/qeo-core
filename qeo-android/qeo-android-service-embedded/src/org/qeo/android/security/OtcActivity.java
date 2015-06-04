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

package org.qeo.android.security;

import java.util.logging.Logger;

import org.qeo.android.service.ui.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Activity to show OTC retrieval dialog.
 */
public class OtcActivity
    extends FragmentActivity
{
    private static final Logger LOG = Logger.getLogger("OtcActivity");
    private SharedPreferences mPrefs;
    private OtcDialog mDialog = null;
    /** Constant used to set Intent filter for device. */
    public static final String ACTION_OTC_REGISTRATION_DONE = "actionOtcRegistrationDone";
    /** Result flag to be added to intent. This is a boolean. */
    public static final String INTENT_EXTRA_RESULT = "result";
    /** Error message in case of failure. */
    public static final String INTENT_EXTRA_ERRORMSG = "errorMsg";
    /** The OTC in case of success. */
    public static final String INTENT_EXTRA_OTC = "otc";
    /** The URL in case of success. */
    public static final String INTENT_EXTRA_URL = "url";
    private LocalBroadcastManager mLbm;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mPrefs = getSharedPreferences(getString(R.string.app_prefs), Context.MODE_PRIVATE);
        mDialog = OtcDialog.getInstance();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("Otp");

        if (prev == null) {
            mDialog.show(getSupportFragmentManager(), "Otp");
        }
        mLbm = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case OtcDialog.QRCODE_ACTIVITY_REQUEST_CODE:
                LOG.fine("OtpActivity.onActivityResult: BARCODESCANNER_ACTIVITY_REQUEST_CODE - resultCode: "
                    + resultCode);
                if (resultCode == Activity.RESULT_OK) {
                    String contents = intent.getStringExtra("SCAN_RESULT");
                    mDialog.setQrResult(contents.trim());
                }
                break;
            default:
                LOG.fine("OtpActivity.onActivityResult(): Unknown requestCode: " + requestCode);
                break;
        }
    }

    /**
     * FinishSecuritySetup ends the security setup as being successful.
     * 
     * @param otc the One time Code
     * @param url the URL of the security backend server
     * @param done boolean true if successfully registered
     */
    void finishSecuritySetup(String otc, String url, boolean done)
    {
        Intent intent = new Intent(ACTION_OTC_REGISTRATION_DONE);
        intent.putExtra(INTENT_EXTRA_RESULT, done);
        intent.putExtra(INTENT_EXTRA_OTC, otc);
        intent.putExtra(INTENT_EXTRA_URL, url);
        mLbm.sendBroadcast(intent);
        finish();
    }

    /**
     * NOTE: exiting the activity with the home button is the same behavior as Cancelling OTC dialog.
     */
    @Override
    protected void onUserLeaveHint()
    {
        LOG.fine("User has pressed home button, Cancelling OTC");
        if (!mPrefs.getBoolean(getString(R.string.qr_scan_code), false)) {
            if (mDialog != null) {
                mDialog.dismiss();
            }
            finishSecuritySetup("", "", false);
        }

    }
}
