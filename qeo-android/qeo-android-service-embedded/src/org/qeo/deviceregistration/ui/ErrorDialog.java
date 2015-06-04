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

import org.qeo.android.service.ui.R;
import org.qeo.deviceregistration.DeviceRegPref;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * ErrorDialog.
 */
public class ErrorDialog
    extends Dialog
{
    private String mMsg;
    private final Button mButton;
    private final TextView mErrorView;
    private Context mCtx;

    /**
     * Create an errorDialog.
     * 
     * @param context -activity context.
     * @param title -title to be shown in Dialog.
     * @param errorMessage - message to be shown in dialog.
     */
    public ErrorDialog(Context context, String title, String errorMessage)
    {

        this(context, getDialogTheme());
        setTitle(title);
        mCtx = context;
        mMsg = errorMessage;
        mErrorView.setText(mMsg);
    }

    private ErrorDialog(Context context, int theme)
    {
        super(context, theme);
        this.setContentView(R.layout.error_dialog);
        mButton = (Button) findViewById(R.id.ok_button);
        mErrorView = (TextView) findViewById(R.id.error_msg);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (mMsg.equals(mCtx.getString(R.string.version_mismatch_error_msg))) {
                    DeviceRegPref.edit().setVersionDialogStatus(false).apply();
                }
                else if (mMsg.equals(mCtx.getString(R.string.get_resource_error_msg))) {
                    DeviceRegPref.edit().setRootUrlDialogStatus(false).apply();
                }
                dismiss();
            }
        });
        this.setCancelable(false);

    }

    @SuppressLint("InlinedApi")
    private static int getDialogTheme()
    {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? android.R.style.Theme_Holo_Dialog_MinWidth
            : android.R.style.Theme_Dialog);
    }

    /**
     * Set the error messages.
     * 
     * @param displayMsg -Error message to be shown to user.
     */
    public void setErrorMessage(String displayMsg)
    {
        mMsg = displayMsg;
        mErrorView.setText(mMsg);
    }

}
