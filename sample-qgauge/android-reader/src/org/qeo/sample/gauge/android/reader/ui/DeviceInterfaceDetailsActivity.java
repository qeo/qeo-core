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

import org.qeo.sample.gauge.NetworkInterfaceSpeedData;
import org.qeo.sample.gauge.android.reader.GaugeReaderApplication;
import org.qeo.sample.gauge.android.reader.R;
import org.qeo.sample.gauge.android.reader.helper.ReaderCallbackHandler.RefreshUIListener;
import org.qeo.sample.gauge.android.reader.model.DeviceModel;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;

/**
 * An activity class that takes care of displaying the SpeedGraphs for every available network interface.
 * 
 */
public class DeviceInterfaceDetailsActivity
        extends FragmentActivity
        implements RefreshUIListener

{
    private static final String FRAGMENT_NAME = "interfaceFragment";
    private InterfaceListFragment mIfdetailFragment;
    private Handler mHandler;
    private String mDeviceId;
    /**
     * The progress dialog.
     */
    private ProgressDialog mProgress;

    /**
     * Creates all the UI components.
     * 
     * @param savedInstanceState Bundle object to save the data
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_device_details_layout);

        mHandler = new Handler();
        mDeviceId = getIntent().getStringExtra("deviceid");
        mProgress = ProgressDialog.show(this, "Loading data", "Please wait...", true, true, new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                finish();
            }
        });
        if (savedInstanceState == null) {

            mIfdetailFragment = new InterfaceListFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.container, mIfdetailFragment, FRAGMENT_NAME)
                    .commit();
        }

        else if (savedInstanceState != null) {
            // Do nothing
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

    }

    /**
     * Is called when the Activity is resumed. This also includes starting the activity.
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        GaugeReaderApplication.sReaderCallBackObj.registerUiListener(this);

    };

    /**
     * Is called when the Activity goes in the background.
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        mProgress.dismiss();
        GaugeReaderApplication.sReaderCallBackObj.unRegisterUiListener(this);
    }

    /**
     * Is called when the Activity is destroyed.
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        return true;
    }

    /**
     * Callback from ReaderCallbackHandler to update the UI with information contained in 'dataList'.
     * 
     * @param dataList - list of message data to be displayed on UI
     */
    @Override
    public void onPublish(List<NetworkInterfaceSpeedData> dataList)
    {

        List<NetworkInterfaceSpeedData> tempList = new ArrayList<NetworkInterfaceSpeedData>();
        /**
         * Filter out the list of devices publishing NetStatMessage over Qeo.
         */
        for (NetworkInterfaceSpeedData d : dataList) {
            if (d != null && d.getDeviceId() != null && d.getDeviceId().equals(mDeviceId)) {
                tempList.add(d);
            }

        }
        GaugeReaderApplication.sDataListForClickedIface = tempList;
        if (mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }
        updateIfaceListData();
    }

    /**
     * Callback from ReaderCallbackHandler to update the UI for given list of interfaces- 'ifNameList'.
     * 
     * @param ifNameList - list of interfaces to be removed from UI
     */
    @Override
    public void onRemove(List<String> ifNameList)
    {
        GaugeReaderApplication.sInterfaceList = ifNameList;
        updateIfaceListData();

    }

    /**
     * Refreshes/updates the speed graph data with new information received.
     */
    private void updateIfaceListData()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

                if (mIfdetailFragment != null && mIfdetailFragment.isAdded()) {

                    ft.hide(mIfdetailFragment);

                    mHandler.post(new Runnable() {
                        @Override
                        public void run()
                        {
                            mIfdetailFragment.updateTrafficStatsData(DeviceInterfaceDetailsActivity.this);
                            ft.show(mIfdetailFragment);
                            ft.commit();
                        }
                    });
                }
                else {
                    mIfdetailFragment = new InterfaceListFragment();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.container, mIfdetailFragment, FRAGMENT_NAME).commitAllowingStateLoss();
                }

            }
        });

    }

    @Override
    public void onupdateDeviceList(List<DeviceModel> deviceList)
    {
        // Do Nothing
    }
}
