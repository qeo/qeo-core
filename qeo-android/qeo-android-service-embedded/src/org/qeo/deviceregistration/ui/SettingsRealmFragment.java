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

import java.net.HttpURLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.android.service.QeoDefaults;
import org.qeo.android.service.QeoService;
import org.qeo.android.service.ui.R;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.QeoManagementApp;
import org.qeo.deviceregistration.helper.RealmCache;
import org.qeo.deviceregistration.rest.GetRealmListLoaderTask;
import org.qeo.deviceregistration.rest.RestResponse;
import org.qeo.deviceregistration.service.RemoteDeviceRegistrationService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * Fragment to show settings.
 */
public class SettingsRealmFragment
    extends SherlockFragment
    implements OnItemSelectedListener, LoaderManager.LoaderCallbacks<RestResponse>
{
    private static final Logger LOG = Logger.getLogger(QeoService.TAG + ":SettingsRealmFragment");
    /** Action to indicate refresh of realms. */
    public static final String ACTION_REFRESH_REALMS = "refreshRealms";
    /** Realm name. */
    public static final String INTENT_EXTRA_REALMNAME = "realmName";
    private static final String STATE_REALM = "stateRealm";
    private MyAdapter mSpinnerAdapter;
    private Spinner mSpinner;
    private EditText mURlView;
    private CheckBox mNotificationsCheckbox;
    private String mSelectedRealmName;
    private long mSelectedRealmId;
    private ErrorDialog mDialog;
    private Context mCtx;
    private boolean mIsDataRetained;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mCtx = getActivity();
        mIsDataRetained = true;

        LOG.fine("savedInstanceState? " + savedInstanceState);
        if (savedInstanceState != null) {
            mSelectedRealmName = savedInstanceState.getString(STATE_REALM);
        }
        if (mSelectedRealmName == null) {
            mSelectedRealmName = DeviceRegPref.getSelectedRealm();
        }
        LOG.fine("selected realm: " + mSelectedRealmName);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);

        mSpinner.setAdapter(mSpinnerAdapter);
        mSpinner.setOnItemSelectedListener(this);
        mDialog =
            new ErrorDialog(getActivity(), getString(R.string.title_error_msg), getString(R.string.general_error_msg));

        if (DeviceRegPref.getRootUrlDialogStatus()) {
            showUrlErrorDialog();
        }
        if (mIsDataRetained) {
            getActivity().getSupportLoaderManager().restartLoader(QeoManagementApp.LOADER_GET_REALMS_ID, null, this);
        }

    }

    @Override
    public void onResume()
    {
        super.onResume();
        LocalBroadcastManager.getInstance(mCtx).registerReceiver(mRefreshRealmListReceiver,
            new IntentFilter(ACTION_REFRESH_REALMS));
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mDialog.isShowing()) {
            mDialog.dismiss();
        }
        if (mRefreshRealmListReceiver != null) {
            LocalBroadcastManager.getInstance(mCtx).unregisterReceiver(mRefreshRealmListReceiver);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.settings_fragment, container, false);

        mSpinnerAdapter = new MyAdapter(getActivity());
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        // Apply the adapter to the spinner
        mSpinner = (Spinner) rootView.findViewById(R.id.realm_spinner);

        mURlView = (EditText) rootView.findViewById(R.id.et_url);
        String text = DeviceRegPref.getScepServerURL();
        mURlView.setText(text);

        mNotificationsCheckbox = (CheckBox) rootView.findViewById(R.id.notificationCheckbox);
        if (QeoDefaults.isRemoteRegistrationServiceAvailable()) {
            mNotificationsCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                {
                    // Check which checkbox was clicked
                    if (buttonView.getId() == R.id.notificationCheckbox) {
                        DeviceRegPref.edit().setDeviceRegisterServiceEnabled(isChecked).apply();
                        RemoteDeviceRegistrationService.checkStartStop(getActivity());
                    }
                }
            });
            mNotificationsCheckbox.setChecked(DeviceRegPref.getDeviceRegisterServiceEnabled());
        }
        else {
            mNotificationsCheckbox.setVisibility(View.GONE);
            rootView.findViewById(R.id.notification_description).setVisibility(View.GONE);
            rootView.findViewById(R.id.notificationText).setVisibility(View.GONE);
        }

        return rootView;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        mSelectedRealmName = parent.getItemAtPosition(position).toString();

        if (mSelectedRealmName.equals(getString(R.string.add_realm_item))) {
            // selected "add realm"
            ((SettingsActivity) getActivity()).addRealmDialog();

        }
        else {
            // selected existing realm
            mSelectedRealmId = RealmCache.getRealmId(mSelectedRealmName);
            if (mSelectedRealmId == 0) {
                LOG.warning("Can't find realm " + mSelectedRealmName + " in the cache");
                return;
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {

    }

    @Override
    public Loader<RestResponse> onCreateLoader(int i, Bundle bundle)
    {
        getActivity().setProgressBarIndeterminateVisibility(true);
        return new GetRealmListLoaderTask(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<RestResponse> restResponseLoader, RestResponse restResponse)
    {

        try {
            int code = restResponse.getCode();

            if (code == HttpURLConnection.HTTP_OK) {

                mSpinnerAdapter.clear();
                // add dummy default text. This will be set to GONE in the list
                mSpinnerAdapter.add(getString(R.string.select_realm_item));

                for (String i : restResponse.getListData()) {
                    // add all the items
                    mSpinnerAdapter.add(i);
                }

                // add the "add item" button
                mSpinnerAdapter.add(getString(R.string.add_realm_item));

                LOG.fine("loader finished, selected realm: " + mSelectedRealmName);
                final int pos = mSpinnerAdapter.getPosition(mSelectedRealmName);

                if (pos != -1 && restResponse.getListData().contains(mSelectedRealmName)) {
                    mSpinner.setSelection(pos, false);

                }
                mSpinner.post(new Runnable() {
                    @Override
                    public void run()
                    {
                        mSpinner.setSelection(pos);
                    }
                });

                mSpinnerAdapter.notifyDataSetChanged();

            }
            else {
                mDialog.setErrorMessage(restResponse.getData());
                mDialog.show();
            }

        }
        catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to get list of realms", e);
            mDialog.setErrorMessage(getString(R.string.realmlist_error_msg));
            mDialog.show();
        }
        getActivity().setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onLoaderReset(Loader<RestResponse> restResponseLoader)
    {
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_REALM, mSelectedRealmName);
    }

    private final BroadcastReceiver mRefreshRealmListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            mSelectedRealmName = intent.getStringExtra(INTENT_EXTRA_REALMNAME);
            getActivity().getSupportLoaderManager().restartLoader(QeoManagementApp.LOADER_GET_REALMS_ID, null,
                SettingsRealmFragment.this);
            RemoteDeviceRegistrationService.checkStartStop(getActivity());
        }
    };

    /**
     * Show url error dialog.
     */
    public void showUrlErrorDialog()
    {
        mDialog.setErrorMessage(getString(R.string.get_resource_error_msg));
        mDialog.show();
    }

    /**
     * Get selected realm.
     * 
     * @return String realm name.
     */
    public String getSelectedRealmName()
    {
        return mSelectedRealmName;
    }

    /**
     * Get selected realm id.
     * 
     * @return long realm id.
     */
    public long getSelectedRealmId()
    {
        return mSelectedRealmId;
    }

    private static class MyAdapter
        extends ArrayAdapter<String>
    {
        public MyAdapter(Context context)
        {
            super(context, android.R.layout.simple_spinner_item);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent)
        {
            View v = null;

            // If this is the initial dummy entry, make it hidden
            if (position == 0) {
                TextView tv = new TextView(getContext());
                tv.setHeight(0);
                tv.setVisibility(View.GONE);
                v = tv;
            }
            else {
                // Pass convertView as null to prevent reuse of special case views
                v = super.getDropDownView(position, null, parent);
            }

            return v;
        }
    }
}
