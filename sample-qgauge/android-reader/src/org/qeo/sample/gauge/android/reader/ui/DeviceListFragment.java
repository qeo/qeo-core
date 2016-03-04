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

package org.qeo.sample.gauge.android.reader.ui;

import java.util.ArrayList;
import java.util.List;

import org.qeo.sample.gauge.android.reader.GaugeReaderAdapter;
import org.qeo.sample.gauge.android.reader.GaugeReaderApplication;
import org.qeo.sample.gauge.android.reader.QeoLocalService;
import org.qeo.sample.gauge.android.reader.R;
import org.qeo.sample.gauge.android.reader.interfaces.Device;
import org.qeo.sample.gauge.android.reader.model.DeviceModel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Fragment class that displays the list of writer devices on UI and starts the Qeo service.
 */
public class DeviceListFragment
    extends ListFragment

{
    private Context mCtx;
    private int mCurCheckPosition = 0;
    private GaugeReaderAdapter mDeviceBrowseAdapter;
    private ArrayList<Device> mDevices;
    private Intent mQeoserviceIntent;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        mCtx = getActivity();

        // Start the Qeo-android Service
        mQeoserviceIntent = new Intent(mCtx, QeoLocalService.class);
        mCtx.startService(mQeoserviceIntent);

        // fill the adapter with available device list
        mDevices = new ArrayList<Device>();

        getDeviceDisplayList();
        mDeviceBrowseAdapter = new GaugeReaderAdapter(this.getActivity(), R.layout.device_list_item, mDevices);
        setListAdapter(mDeviceBrowseAdapter);

        if (savedInstanceState != null) {
            // Restore last state for checked position.
            mCurCheckPosition = savedInstanceState.getInt("curChoice", 0);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putInt("curChoice", mCurCheckPosition);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        showInterfacesforSelectedDevice((DeviceModel) getListView().getItemAtPosition(position));
    }

    /**
     * Helper function to show the interface details of a selected device, starting a new activity in which it is
     * displayed.
     */
    private void showInterfacesforSelectedDevice(DeviceModel device)
    {
        Intent intent = new Intent();
        intent.setClass(getActivity(), DeviceInterfaceDetailsActivity.class);
        Log.d(DeviceListFragment.class.getSimpleName(), "device id:" + device.getId());
        intent.putExtra("deviceid", device.getId());
        startActivity(intent);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        // mCtx.registerReceiver(mReceiver, new IntentFilter(QeoLocalService.QEO_SERVICE_STATUS));
        mDeviceBrowseAdapter.notifyDataSetChanged();
    }

    /**
     * Refreshes the fragment with updated devicelist.
     * 
     * @param devicelist -list of devices to be shown on UI
     */
    public void refreshDeviceList(final List<DeviceModel> devicelist)
    {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                if (GaugeReaderApplication.sDeviceIdList.size() != 0) {
                    getDeviceDisplayList();
                }
                else {
                    mDevices.clear();
                }
                mDeviceBrowseAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        // stop the Qeo service before exiting application
        if (mQeoserviceIntent != null) {
            mCtx.stopService(mQeoserviceIntent);
        }
        GaugeReaderApplication.sIfaceGraphHelperMap.clear();
        GaugeReaderApplication.sDeviceIdList.clear();

    }

    private void getDeviceDisplayList()
    {
        mDevices.clear();
        for (DeviceModel d : GaugeReaderApplication.sQeoDeviceList) {
            for (int i = 0; i < GaugeReaderApplication.sDeviceIdList.size(); i++) {
                if (GaugeReaderApplication.sDeviceIdList.get(i).equals(d.getId().toString())) {
                    mDevices.add(d);
                }
            }
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {

            boolean gen = intent.getBooleanExtra(getString(R.string.broadcast_general_failure), false);

            boolean flag = intent.getBooleanExtra(getString(R.string.broadcast_qeo_force_stop), false);
            if (flag) {
                mCtx.stopService(mQeoserviceIntent);
            }

            else if (!gen) {
                String errMsg = intent.getStringExtra(getString(R.string.broadcast_qeo_fail_msg));
                if (errMsg != null && !errMsg.isEmpty()) {
                    Toast.makeText(getActivity(), "Qeo Service failed due to " + errMsg, Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(getActivity(), "QeoService initialization failed, exiting!", Toast.LENGTH_LONG)
                        .show();
                }
                mCtx.stopService(mQeoserviceIntent);

                if (mReceiver != null) {
                    mCtx.unregisterReceiver(mReceiver);
                    mReceiver = null;
                }
                DeviceListFragment.this.getActivity().finish();
            }
        }
    };

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onDestroyView()
     */
    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        if (mReceiver != null) {
            mCtx.unregisterReceiver(mReceiver);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup,
     * android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mCtx = this.getActivity();
        mCtx.registerReceiver(mReceiver, new IntentFilter(QeoLocalService.QEO_SERVICE_STATUS));
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
