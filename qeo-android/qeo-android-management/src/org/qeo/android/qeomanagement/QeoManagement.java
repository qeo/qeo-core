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

package org.qeo.android.qeomanagement;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

/**
 * Qeo management activity.
 */
@SuppressLint("ValidFragment")
public class QeoManagement
    extends FragmentActivity
{
    private ManagementDialog mDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mDialog = new ManagementDialog();

        mDialog.setRetainInstance(true);
        mDialog.show(getSupportFragmentManager(), "QeoManagement");
    }

    /**
     * The class that represents the manifest dialog. All requested topic permissions are provided by the users. User
     * can reject them all or allow them all.
     */
    public class ManagementDialog
        extends DialogFragment
    {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle("Qeo Security Management");
            builder.setMessage("Control the policy file polling");
            // Add the buttons
            builder.setPositiveButton(R.string.start, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id)
                {
                    final Intent mgmtService = new Intent(getBaseContext(), QeoManagementService.class);
                    // mgmtDialog.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startService(mgmtService);
                    finish();
                }
            });
            builder.setNegativeButton(R.string.stop, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id)
                {
                    final Intent mgmtService = new Intent(getBaseContext(), QeoManagementService.class);
                    // mgmtDialog.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    stopService(mgmtService);
                    finish();
                }
            });
            return builder.create();
        }

        @Override
        public void onCancel(DialogInterface dialog)
        {
            super.onCancel(dialog);
            finish();
        }
    }
}
