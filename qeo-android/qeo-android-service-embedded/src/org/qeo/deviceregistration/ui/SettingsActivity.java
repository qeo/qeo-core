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

package org.qeo.deviceregistration.ui;

import java.net.HttpURLConnection;
import java.util.logging.Logger;

import org.qeo.android.service.ui.R;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.QeoManagementApp;
import org.qeo.deviceregistration.helper.DataHandler;
import org.qeo.deviceregistration.helper.RealmCache;
import org.qeo.deviceregistration.rest.AddRealmLoaderTask;
import org.qeo.deviceregistration.rest.RestResponse;
import org.qeo.deviceregistration.service.RemoteDeviceRegistrationService;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.Window;

/**
 * This class manage the activity/fragment settings.
 */
public class SettingsActivity
    extends SherlockFragmentActivity
{
    private static final Logger LOG = Logger.getLogger("SettingsActivity");
    private SettingsRealmFragment mSettingsFragment;
    private Intent mRealmListRefreshIntent;
    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_settings);
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Creating Realm.");
        mProgress.setCancelable(false);
        mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mRealmListRefreshIntent = new Intent(SettingsRealmFragment.ACTION_REFRESH_REALMS);
        if (savedInstanceState == null) {
            mSettingsFragment = new SettingsRealmFragment();
            getSupportFragmentManager().beginTransaction()
                .add(R.id.settingrealm_frag, mSettingsFragment, "SettingsFragment").commit();
        }
        else {
            mSettingsFragment =
                (SettingsRealmFragment) getSupportFragmentManager().findFragmentById(R.id.settingrealm_frag);
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();

        if (DeviceRegPref.getSelectedRealmId() == 0 && DataHandler.getNativeRealmId() == 0) {
            // device not registered yet
            LOG.fine("Device not registered yet.");
            findViewById(R.id.activitySettings_textview_registrationNote).setVisibility(View.VISIBLE);
        }
        else {
            findViewById(R.id.activitySettings_textview_registrationNote).setVisibility(View.GONE);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    /**
     * Adds the AddRealm Dialog to calling fragment/activity.
     */
    public void addRealmDialog()
    {
        // Create and show the dialog.
        AddRealmFragment newFragment = new AddRealmFragment();
        newFragment.show(getSupportFragmentManager(), "AddRealm");
    }

    private void onAddRealmSuccess(String realmName)
    {

        if (mProgress.isShowing()) {
            mProgress.dismiss();
        }
        Toast.makeText(this, realmName + " added", Toast.LENGTH_SHORT).show();
        if (mSettingsFragment != null) {
            mRealmListRefreshIntent.putExtra(getString(R.string.add_realm_status_intent), true);
            mRealmListRefreshIntent.putExtra(getString(R.string.add_realm_cancel_status_intent), false);
            mRealmListRefreshIntent.putExtra(SettingsRealmFragment.INTENT_EXTRA_REALMNAME, realmName);
            LocalBroadcastManager.getInstance(this).sendBroadcast(mRealmListRefreshIntent);
        }
    }

    private void onAddRealmFailure(String errorMsg)
    {
        if (mProgress.isShowing()) {
            mProgress.dismiss();
        }

        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        if (mSettingsFragment != null) {
            mRealmListRefreshIntent.putExtra(getString(R.string.add_realm_status_intent), false);
            mRealmListRefreshIntent.putExtra(getString(R.string.add_realm_cancel_status_intent), false);
            LocalBroadcastManager.getInstance(this).sendBroadcast(mRealmListRefreshIntent);
        }
    }

    private void onAddRealmCancel()
    {
        if (mSettingsFragment != null) {
            mRealmListRefreshIntent.putExtra(getString(R.string.add_realm_status_intent), false);
            mRealmListRefreshIntent.putExtra(getString(R.string.add_realm_cancel_status_intent), true);
            LocalBroadcastManager.getInstance(this).sendBroadcast(mRealmListRefreshIntent);
        }
    }

    private void onAddRealmProgress()
    {
        mRealmListRefreshIntent.putExtra(getString(R.string.add_realm_cancel_status_intent), false);
        mRealmListRefreshIntent.putExtra(getString(R.string.add_realm_status_intent), false);
        DeviceRegPref.edit().setShowRealmProgress(true).apply();
        mProgress.show();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (DeviceRegPref.getShowRealmProgress()) {
            mProgress.show();
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mRealmProgressReceiver,
            new IntentFilter(getString(R.string.add_realm_progress_broadcast)));
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (mProgress.isShowing()) {
            mProgress.dismiss();
        }
        if (mRealmProgressReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mRealmProgressReceiver);
        }

        // save selected realm
        String realmName = mSettingsFragment.getSelectedRealmName();
        long realmId = RealmCache.getRealmId(realmName);
        if (realmId != 0) {
            LOG.fine("Saving realm info: " + realmName + " (" + realmId + ")");
            DeviceRegPref.edit().setSelectedRealmId(realmId, realmName).apply();
        }

    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        RemoteDeviceRegistrationService.checkStartStop(this);
    }

    private final BroadcastReceiver mRealmProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (mProgress != null && mProgress.isShowing()) {
                mProgress.dismiss();
            }
            mRealmListRefreshIntent.putExtra(getString(R.string.add_realm_status_intent), true);
            mRealmListRefreshIntent.putExtra(getString(R.string.add_realm_cancel_status_intent), false);
            LocalBroadcastManager.getInstance(SettingsActivity.this).sendBroadcast(mRealmListRefreshIntent);
        }
    };

    /**
     * Fragment to create a new realm.
     */
    public static class AddRealmFragment
        extends SherlockDialogFragment
        implements LoaderManager.LoaderCallbacks<RestResponse>
    {
        private static final Logger LOG = Logger.getLogger("AddRealmFragment");
        private EditText mRealmText;
        private Button mSubmitBtn;
        private Button mCancelBtn;
        private String mRealmName;
        private SettingsActivity mActivity;

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

        }

        @Override
        public void onAttach(Activity activity)
        {
            super.onAttach(activity);
            mActivity = (SettingsActivity) activity;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState)
        {
            super.onActivityCreated(savedInstanceState);
            // setRetainInstance(true);
            if (savedInstanceState != null) {
                mRealmText.setText(savedInstanceState.getString("realmName"));
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState)
        {
            View view = inflater.inflate(R.layout.addrealm_fragment, container);
            mRealmText = (EditText) view.findViewById(R.id.et_realm);
            mSubmitBtn = (Button) view.findViewById(R.id.ok_btn);
            mCancelBtn = (Button) view.findViewById(R.id.cancel_btn);
            getDialog().setTitle("AddRealm");
            mRealmText.requestFocus();

            // getDialog().getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            mSubmitBtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v)
                {
                    mRealmName = mRealmText.getText().toString().trim();
                    if (TextUtils.isEmpty(mRealmName) || mRealmName == null) {
                        mRealmText.setError(getString(R.string.invalid_realm_error));
                        return;
                    }
                    else {
                        getActivity().getSupportLoaderManager().restartLoader(QeoManagementApp.LOADER_ADD_REALM_ID,
                            null, AddRealmFragment.this);
                        mActivity.onAddRealmProgress();
                        dismiss();
                    }

                }
            });
            mCancelBtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v)
                {
                    mActivity.onAddRealmCancel();
                    dismiss();
                }
            });
            return view;
        }

        @Override
        public void onSaveInstanceState(Bundle outState)
        {
            super.onSaveInstanceState(outState);
            if (mRealmName != null && mRealmName.length() != 0) {
                outState.putString("realmName", mRealmName);
            }
        }

        @Override
        public Loader<RestResponse> onCreateLoader(int i, Bundle bundle)
        {
            return new AddRealmLoaderTask(getActivity(), mRealmName);
        }

        @Override
        public void onLoadFinished(Loader<RestResponse> restResponseLoader, RestResponse restResponse)
        {
            int code = restResponse.getCode();
            LOG.fine("onLoadFinished: code: " + code + ", json response: " + restResponse.getData());
            if (code == HttpURLConnection.HTTP_CREATED) {
                String realm = restResponse.getData();
                mActivity.onAddRealmSuccess(realm);
            }
            // handle for other error code but no exception
            else {
                mActivity.onAddRealmFailure(restResponse.getData());
            }
        }

        @Override
        public void onLoaderReset(Loader<RestResponse> restResponseLoader)
        {

        }
    }
}
