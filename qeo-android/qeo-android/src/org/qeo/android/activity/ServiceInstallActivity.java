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

package org.qeo.android.activity;

import java.util.logging.Logger;

import org.qeo.android.QeoAndroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

/**
 * Simple activity that shows a dialog to ask to open google play for installing Qeo.
 */
public class ServiceInstallActivity
    extends Activity
    implements DialogInterface.OnClickListener
{
    private static final Logger LOG = Logger.getLogger(QeoAndroid.TAG + ":ServiceInstallActivity");

    @Override
    public void onStart()
    {
        super.onStart();
        displayDialog();
    }

    private void displayDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Qeo service not installed");
        String msg = "Qeo is required to be able to use this application. Do you want to install Qeo from Google Play?";
        builder.setMessage(msg);
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(android.R.string.cancel, this);
        // Create the AlertDialog object and show it
        builder.create().show();
    }

    /**
     * Launch google play to install the Qeo Service.
     * 
     * @param ctx the Android context.
     * @return True if starting the activity succeeded.
     */
    public static boolean openQeoPlayStore(Context ctx)
    {
        final Intent marketIntent = new Intent(Intent.ACTION_VIEW);
        marketIntent.setData(Uri.parse("market://details?id=" + QeoAndroid.QEO_SERVICE_PACKAGE));

        try {
            LOG.fine("Going to download Qeo from google play");
            ctx.startActivity(marketIntent);
        }
        catch (final ActivityNotFoundException e) {
            return false;
        }
        return true;
    }

    @Override
    public void onClick(DialogInterface dialog, int which)
    {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                if (!openQeoPlayStore(this)) {
                    Toast.makeText(this, "No google play available, can't install Qeo service", Toast.LENGTH_LONG)
                        .show();
                    LOG.warning("No google play available, can't install Qeo service");
                }
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                LOG.fine("User declined to download Qeo");
                break;
            default:
                throw new IllegalStateException("Unkown button id: " + which);
        }
        // close activity
        finish();
    }
}
