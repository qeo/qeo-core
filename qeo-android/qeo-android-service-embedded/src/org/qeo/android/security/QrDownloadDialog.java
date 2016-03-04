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

package org.qeo.android.security;

import java.util.logging.Logger;

import org.qeo.android.service.ui.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * This dialog allows the user to download a QR capable application in case he/she chooses to scan his/her OTC and URL
 * values form the OTC dialog.
 */
public class QrDownloadDialog
    extends DialogFragment
    implements DialogInterface.OnClickListener
{
    private static final Logger LOG = Logger.getLogger("QrDownloadDialog");

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.qrDownloadDialogTitle);
        builder.setMessage(R.string.qrDownloadDialogText);
        builder.setPositiveButton(R.string.ok, this);
        builder.setNegativeButton(R.string.cancel, this);
        return builder.create();
    }

    @Override
    public void onDestroyView()
    {
        // Avoid disappearing of dialog on rotation change
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onClick(DialogInterface dialog, int which)
    {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                // First option is to use the Google Market application to download bar-code scanner
                final Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                marketIntent.setData(Uri.parse("market://details?id=com.google.zxing.client.android"));
                try {
                    LOG.fine("Going to download QR Scanner from Android Market");
                    startActivity(marketIntent);
                }
                catch (final ActivityNotFoundException e2) {
                    // In case no Google Market application is available, use a direct URL
                    final Intent websiteIntent = new Intent(Intent.ACTION_VIEW);
                    websiteIntent.setData(Uri.parse("http://play.google.com/store/apps/details?"
                        + "id=com.google.zxing.client.android"));
                    LOG.fine("No market application available, retrying using direct URL");
                    startActivity(websiteIntent);
                }
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                LOG.fine("User refused to download QR Scanner");
                break;
            default:
                throw new IllegalStateException("Unkown button id: " + which);
        }
    }
}
