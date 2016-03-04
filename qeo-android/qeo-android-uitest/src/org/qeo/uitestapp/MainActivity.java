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

package org.qeo.uitestapp;

import org.qeo.QeoFactory;
import org.qeo.android.QeoAndroid;
import org.qeo.android.QeoConnectionListener;
import org.qeo.android.uitest.R;
import org.qeo.exception.QeoException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * Main activity.
 */
public class MainActivity
    extends Activity
    implements OnClickListener
{
    private Button mBbuttonStart;
    private Button mButtonStop;
    private CheckBox mCheckboxInitQeoStarted;
    private CheckBox mCheckboxInitQeoDone;
    private TextView mTextViewError;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBbuttonStart = (Button) findViewById(R.id.button_startQeo);
        mBbuttonStart.setOnClickListener(this);
        mButtonStop = (Button) findViewById(R.id.button_stopQeo);
        mButtonStop.setOnClickListener(this);
        mCheckboxInitQeoStarted = (CheckBox) findViewById(R.id.checkBox_initQeo);
        mCheckboxInitQeoDone = (CheckBox) findViewById(R.id.checkBox_qeoReady);
        mTextViewError = (TextView) findViewById(R.id.textView_error);

        if (getString(R.string.app_name).equals("UITestEmbedded")) {
            enableEmbeddedParameters();
        }
    }

    private void enableEmbeddedParameters()
    {
        // try {
        // parameters below should be default
        // // disable abort on stop for webview
        // Class<?> clazz = Class.forName("org.qeo.deviceregistration.ui.WebViewActivity");
        // Method m = clazz.getMethod("setAbortOnStop", boolean.class);
        // m.invoke(null, false);
        //
        // // disable new device notifcation
        // Class<?> clazzPrefs = Class.forName("org.qeo.deviceregistration.DeviceRegPref");
        // Method methodEdit = clazzPrefs.getMethod("edit");
        // Object edit = methodEdit.invoke(null);
        // Class<?> clazzEdit = Class.forName("org.qeo.deviceregistration.DeviceRegPref$Edit");
        // Method methodSetDeviceRegisterNotificationEnabled =
        // clazzEdit.getMethod("setDeviceRegisterNotificationEnabled", boolean.class);
        // methodSetDeviceRegisterNotificationEnabled.invoke(edit, false);
        // }
        // catch (Exception e) {
        // throw new IllegalStateException("not good", e);
        // }
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.button_startQeo:
                mCheckboxInitQeoStarted.setChecked(true);
                QeoAndroid.initQeo(getApplicationContext(), mQeoConnection);
                break;
            case R.id.button_stopQeo:
                mCheckboxInitQeoStarted.setChecked(false);
                mCheckboxInitQeoDone.setChecked(false);
                QeoAndroid.closeQeo(mQeoConnection);
                break;
            default:
                throw new IllegalArgumentException("Unknown button id");
        }
    }

    private final QeoConnectionListener mQeoConnection = new QeoConnectionListener() {

        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            mCheckboxInitQeoDone.setChecked(true);
        }

        @Override
        public void onQeoError(QeoException ex)
        {
            mCheckboxInitQeoDone.setChecked(false);
            mTextViewError.setText("QeoError: " + (ex != null ? ex.getMessage() : "No message"));
            Log.e("QeoUI", "onQeoError", ex);
        }

        @Override
        public void onQeoClosed(QeoFactory qeo)
        {
            mCheckboxInitQeoStarted.setChecked(false);
            mCheckboxInitQeoDone.setChecked(false);
            Log.w("QeoUI", "onQeoClosed");
            mTextViewError.setText("QeoClosed");
        };
    };

}
