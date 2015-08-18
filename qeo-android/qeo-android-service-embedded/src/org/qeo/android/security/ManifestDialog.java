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

import org.qeo.android.service.ApplicationSecurityStandalone;
import org.qeo.android.service.ui.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * The class that represents the manifest dialog. All requested topic permissions are provided by the users. User can
 * reject them all or allow them all.
 */
public class ManifestDialog
    extends DialogFragment
{

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Bundle args = getArguments();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(args.getString(ApplicationSecurityStandalone.INTENT_EXTRA_TITLE));
        builder.setItems(args.getStringArray(ApplicationSecurityStandalone.INTENT_EXTRA_PERMISSIONS), null);
        // Add the buttons
        builder.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id)
            {
                notifyClicked(true);
            }
        });
        builder.setNegativeButton(R.string.reject, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id)
            {
                notifyClicked(false);
            }
        });
        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog)
    {
        super.onCancel(dialog);
        notifyClicked(false);
    }

    private void notifyClicked(boolean result)
    {
        ManifestDialogCallbacks activity = (ManifestDialogCallbacks) getActivity();
        activity.notifyService(result);
    }

    /**
     * Interface to define callbacks needed by this dialog.
     */
    interface ManifestDialogCallbacks
    {

        /**
         * Notify the Qeo service that the manifest has been accepted or rejected and that the dialog window is
         * finished.
         * 
         * @param result true if accepted, false if rejected
         */
        void notifyService(boolean result);
    }
}
