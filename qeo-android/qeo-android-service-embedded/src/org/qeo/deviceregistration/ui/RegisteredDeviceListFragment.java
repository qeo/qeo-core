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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.android.service.ui.R;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.QeoManagementApp;
import org.qeo.deviceregistration.rest.GetDeviceListLoaderTask;
import org.qeo.deviceregistration.rest.RestResponse;
import org.qeo.deviceregistration.service.RemoteDeviceRegistration;

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
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;

/**
 * Fragments that contains list of already registered devices in your realm.
 */
public class RegisteredDeviceListFragment
    extends SherlockListFragment
    implements LoaderManager.LoaderCallbacks<RestResponse>
{

    private static final Logger LOG = Logger.getLogger("RegisteredDeviceListFragment");

    private final List<String> mDevicelist = new ArrayList<String>();
    private RegisteredDeviceListAdapter mAdapter;
    private Bundle mParams;
    private ErrorDialog mDialog;
    private LocalBroadcastManager mLbm;

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new RegisteredDeviceListAdapter(this.getActivity(), mDevicelist);
        setListAdapter(null);
        setListAdapter(mAdapter);
        setListShown(false);

        mDialog =
            new ErrorDialog(getActivity(), getString(R.string.title_error_msg), getString(R.string.general_error_msg));

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mDialog.isShowing()) {
            mDialog.dismiss();
        }

        if (DeviceRegPref.getRefreshToken() != null) {
            getActivity().getSupportLoaderManager().destroyLoader(QeoManagementApp.LOADER_GET_DEVICES_ID);
        }

        mLbm.unregisterReceiver(mReceiver);

    }

    @Override
    public void onResume()
    {
        super.onResume();

        mLbm.registerReceiver(mReceiver, new IntentFilter(RemoteDeviceRegistration.ACTION_UNREGISTERED_DEVICE_LOST));
        if (DeviceRegPref.getRefreshToken() != null && DeviceRegPref.getSelectedRealmId() != 0) {
            getActivity().getSupportLoaderManager()
                .restartLoader(QeoManagementApp.LOADER_GET_DEVICES_ID, mParams, this);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {

    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mParams = new Bundle();

        mLbm = LocalBroadcastManager.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        setListAdapter(null);
    }

    @Override
    public Loader<RestResponse> onCreateLoader(int i, Bundle bundle)
    {
        return new GetDeviceListLoaderTask(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<RestResponse> restResponseLoader, RestResponse restResponse)
    {
        try {

            int code = restResponse.getCode();
            if (code == HttpURLConnection.HTTP_OK) {
                mDevicelist.clear();
                for (String i : restResponse.getListData()) {
                    mDevicelist.add(i);
                }
                mAdapter.notifyDataSetChanged();

            }
            else {
                mDialog.setErrorMessage(restResponse.getData());
                mDialog.show();
            }
            if (isResumed()) {
                setListShown(true);
            }
            else {
                setListShownNoAnimation(true);
            }
        }
        catch (Exception e) {
            LOG.log(Level.WARNING, "Error getting devicelist", e);
            mDialog.setErrorMessage(getString(R.string.devicelist_error_msg));
            mDialog.show();
        }
    }

    @Override
    public void onLoaderReset(Loader<RestResponse> restResponseLoader)
    {

    }

    /**
     * Refresh the list of devices.
     */
    public void refresh()
    {
        getActivity().getSupportLoaderManager().restartLoader(QeoManagementApp.LOADER_GET_DEVICES_ID, mParams, this);
    }

    /** This receiver will trigger a refresh of the list of registered devices from the sms. */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            refresh();
        }
    };

}
