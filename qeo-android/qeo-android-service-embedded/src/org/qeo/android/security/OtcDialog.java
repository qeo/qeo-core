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

import org.qeo.android.service.QeoDefaults;
import org.qeo.android.service.ui.R;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * This is the class that creates the OTC security dialog during the Qeo security set-up phase.
 */
public class OtcDialog
    extends DialogFragment
{
    private static final Logger LOG = Logger.getLogger("OtcDialog");
    private static OtcDialog sOtpDialogFragment = null;

    /** The intent request code used for the OTC dialog. */
    static final int QRCODE_ACTIVITY_REQUEST_CODE = 1;

    private Button mButtonAuthenticate;
    private Button mButtonCancel;
    private Button mButtonScanQrCode;
    private EditText mOtc;
    private ColorStateList mOtcOriginalTextColors;
    private EditText mUrl;
    private OtcActivity mActivity;

    /**
     * Get the OTC retrieval dialog.
     * 
     * @return OtpDialog the OTC retrieval dialog fragment
     */
    static synchronized OtcDialog getInstance()
    {
        if (null == sOtpDialogFragment) {
            sOtpDialogFragment = new OtcDialog();
            sOtpDialogFragment.setRetainInstance(true);
        }

        return sOtpDialogFragment;
    }

    @Override
    public void onCancel(DialogInterface dialog)
    {
        mButtonCancelListener.onClick(null);
        super.onCancel(dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_otc, container);
        getDialog().setTitle(getResources().getString(R.string.otcDialogTitle));

        mActivity = (OtcActivity) getActivity();
        mButtonAuthenticate = (Button) view.findViewById(R.id.authenticate);
        mButtonCancel = (Button) view.findViewById(R.id.cancel);
        mButtonScanQrCode = (Button) view.findViewById(R.id.scan_qr);
        if (!isCameraAvailable(getActivity())) {
            mButtonScanQrCode.setVisibility(View.GONE);
        }

        mOtc = (EditText) view.findViewById(R.id.otc);
        mOtc.addTextChangedListener(mTextWatcher);
        mOtcOriginalTextColors = mOtc.getTextColors();
        mUrl = (EditText) view.findViewById(R.id.url);
        mUrl.setText(QeoDefaults.getPublicUrl());
        mUrl.addTextChangedListener(mTextWatcher);

        mButtonAuthenticate.setOnClickListener(mButtonAuthenticateListener);
        mButtonCancel.setOnClickListener(mButtonCancelListener);
        mButtonScanQrCode.setOnClickListener(mButtonScanQrCodeListener);

        updateUi();
        return view;
    }

    @Override
    public void onDestroyView()
    {
        // Avoid disappearing of dialog on rotation change. This is necessary since we use setRetainInstance(true).
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    /**
     * isCameraAvailable check if the device has a camera for QR Code scanning.
     * 
     * @return boolean true if device has at least 1 camera
     */
    private boolean isCameraAvailable(Context context)
    {
        return (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) || context
            .getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT));
    }

    private final OnClickListener mButtonScanQrCodeListener = new OnClickListener() {
        @Override
        public void onClick(View v)
        {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            SharedPreferences.Editor edit =
                getActivity().getSharedPreferences(getString(R.string.app_prefs), Context.MODE_PRIVATE).edit();
            edit.putBoolean(getString(R.string.qr_scan_code), true).apply();
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            try {
                getActivity().startActivityForResult(intent, QRCODE_ACTIVITY_REQUEST_CODE);
            }
            catch (ActivityNotFoundException e1) {
                LOG.fine("No QR Application found, proposing to download one");
                QrDownloadDialog d = new QrDownloadDialog();
                d.setCancelable(false);
                d.setRetainInstance(true);
                d.show(getActivity().getSupportFragmentManager(), "QrDownload");
            }
        }
    };

    private final OnClickListener mButtonAuthenticateListener = new OnClickListener() {
        @Override
        public void onClick(View v)
        {
            final String otc = mOtc.getText().toString();
            final String url = mUrl.getText().toString();
            updateUi();
            mActivity.finishSecuritySetup(otc, url, true);
        }
    };

    private final OnClickListener mButtonCancelListener = new OnClickListener() {
        @Override
        public void onClick(View v)
        {
            updateUi();
            LOG.fine("User has canceled OTC popup dialog");
            mActivity.finishSecuritySetup("", "", false);
        }
    };

    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after)
        {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count)
        {
        }

        @Override
        public void afterTextChanged(Editable s)
        {
            updateUi();
        }
    };

    private enum InputValidationResult {
        INPUT_NOK, INPUT_NOK_OTC_SYNTAX_INVALID, INPUT_OK;
    }

    /**
     * updateUI: helper method to update (activate/de-activate) your buttons according to some minimal validation
     * checks: <br/>
     * 1) OTC Code should not be empty <br/>
     * 2) URL should not be empty <br/>
     * 3) URL should be a valid URL <br/>
     * 4) OTC length should be 8 digits <br/>
     * 5) OTC modulo 97 value should be 1 (2 digit error verification).
     */
    private void updateUi()
    {
        final String otcText = mOtc.getText().toString();
        final String urlText = mUrl.getText().toString();

        InputValidationResult ivr = validateInputData(otcText, urlText);

        if (ivr == InputValidationResult.INPUT_OK) {
            mOtc.setTextColor(mOtcOriginalTextColors);
            mButtonAuthenticate.setEnabled(true);
        }
        else {
            if (ivr == InputValidationResult.INPUT_NOK_OTC_SYNTAX_INVALID) {
                Toast.makeText(getActivity(), getResources().getString(R.string.otcIncorrect), Toast.LENGTH_LONG)
                    .show();
                mOtc.setTextColor(Color.RED);
            }
            else {
                mOtc.setTextColor(mOtcOriginalTextColors);
            }
            mButtonAuthenticate.setEnabled(false);
        }
    }

    /**
     * validInputData helper methods to check when Authenticate Button can be enabled.
     * 
     * @param otcText
     * @param urlText
     * @return boolean true if otcText and urlText are valid as input data
     */
    private InputValidationResult validateInputData(String otcText, String urlText)
    {
        if (null == otcText || null == urlText || TextUtils.isEmpty(otcText) || TextUtils.isEmpty(urlText)) {
            return InputValidationResult.INPUT_NOK;
        }

        if (!URLUtil.isValidUrl(urlText)) {
            return InputValidationResult.INPUT_NOK;
        }

        return InputValidationResult.INPUT_OK;
    }

    /**
     * setQrResult a helper method to split the QR Code and fill in the appropriate text fields.
     * 
     * @param qrResult semicolon separated string containing <otc>;<url>
     */
    void setQrResult(String qrResult)
    {
        final String[] seperated = qrResult.split(";");
        final String otc = (null != seperated[0]) ? seperated[0] : "";
        final String url = (null != seperated[1]) ? seperated[1] : "";
        mOtc.setText(otc);
        mUrl.setText(url);
        updateUi();
    }
}
