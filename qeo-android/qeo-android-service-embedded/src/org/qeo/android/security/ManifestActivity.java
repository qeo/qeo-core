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

import org.qeo.android.security.ManifestDialog.ManifestDialogCallbacks;
import org.qeo.android.service.ApplicationSecurityStandalone;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Activity to show manifest acceptance dialog.
 */
public class ManifestActivity
    extends FragmentActivity
    implements ManifestDialogCallbacks
{
    private static final Logger LOG = Logger.getLogger("ManifestActivity");

    private ManifestDialog mDialog = null;
    private int mUid;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected synchronized void onStart()
    {
        super.onStart();
        Bundle args = getIntent().getExtras();
        mUid = args.getInt(ApplicationSecurityStandalone.INTENT_EXTRA_UID);

        mDialog = new ManifestDialog();

        mDialog.setArguments(getIntent().getExtras());
        mDialog.setCancelable(false);
        mDialog.setRetainInstance(true);
        mDialog.show(getSupportFragmentManager(), "Manifest");
    }

    /**
     * NOTE: exiting the activity with the home button is the same behavior as rejecting. You cannot do this in onStop
     * because, you don't want it to happen also on orientation change.
     */
    @Override
    protected void onUserLeaveHint()
    {
        LOG.fine("User has pressed home button, rejecting manifest");
        notifyService(false);
    }

    @Override
    public synchronized void notifyService(boolean result)
    {
        if (mDialog != null) {
            // only send the notification once
            Intent intent = new Intent(ApplicationSecurityStandalone.ACTION_MANIFEST_DIALOG_FINISHED);

            intent.putExtra(ApplicationSecurityStandalone.INTENT_EXTRA_UID, mUid);
            intent.putExtra(ApplicationSecurityStandalone.INTENT_EXTRA_RESULT, result);
            LOG.fine("notifyService: " + mUid + ": " + result);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            finish();
            mDialog = null;
        }
    }
}
