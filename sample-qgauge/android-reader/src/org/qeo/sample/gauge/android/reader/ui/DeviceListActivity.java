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
import android.view.Menu;
import android.view.MenuItem;

/**
 * Activity class displaying the list of Qeo devices publishing traffic stata data.
 */
public class DeviceListActivity
        extends FragmentActivity
        implements RefreshUIListener
{
    private static final String DEVICE_LIST_FRAGMENT = "deviceList";
    /**
     * The progress dialog.
     */
    private ProgressDialog mProgress;
    private DeviceListFragment mDeviceListFragment;
    private static final String DEVICE_LIST_FRAGMENT_NAME = "deviceList";
    private DeviceListUpdater mUiUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devicelist_layout);
        mProgress =
                ProgressDialog.show(this, "Searching QGaugeWriters", "Please wait...", true, true,
                        new OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog)
                            {
                                finish();
                            }
                        });

        // Create fragment if activity created first time
        if (savedInstanceState == null) {
            mDeviceListFragment = new DeviceListFragment();
            getSupportFragmentManager().beginTransaction()

            .add(R.id.writer_container, mDeviceListFragment, DEVICE_LIST_FRAGMENT_NAME).commit();
        }
        // Use existing saved fragment on screen rotation
        else if (savedInstanceState != null) {
            mProgress.dismiss();

        }
        /**
         * Start the updater task to refresh UI after regular interval.
         */
        mUiUpdater = new DeviceListUpdater(mUpdateUITask, 1000);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPublish(List<NetworkInterfaceSpeedData> ifaceSpeedDataList)
    {
        // Do nothing

    }

    @Override
    public void onRemove(List<String> ifName)
    {
        // Do nothing
    }

    @Override
    public void onupdateDeviceList(List<DeviceModel> deviceList)
    {

        if (mProgress.isShowing()) {
            mProgress.dismiss();
        }

    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mProgress.dismiss();
        mUiUpdater.stopUpdates();
        GaugeReaderApplication.sReaderCallBackObj.unRegisterUiListener(this);

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        GaugeReaderApplication.sReaderCallBackObj.registerUiListener(this);
        mUiUpdater.startUpdates();

    }

    private final Runnable mUpdateUITask = new Runnable() {
        @Override
        public void run()
        {

            DeviceListFragment f =
                    (DeviceListFragment) getSupportFragmentManager().findFragmentByTag(DEVICE_LIST_FRAGMENT);
            if (f != null) {

                f.refreshDeviceList(GaugeReaderApplication.sQeoDeviceList);
            }
        }
    };

    /**
     * DeviceListUpdater class is used to perform periodical updates, specified inside a runnable object.
     * 
     */
    class DeviceListUpdater
    {

        private final Handler mHandler = new Handler();

        private final Runnable mStatusChecker;

        /**
         * Creates an DeviceListUpdater object, that can be used to perform UI Updates on a specified time interval.
         * 
         * @param uiUpdater A runnable containing the update routine.
         * @param interval The interval over which the routine should run (milliseconds).
         */
        public DeviceListUpdater(final Runnable uiUpdater, final int interval)
        {
            mStatusChecker = new Runnable() {
                @Override
                public void run()
                {
                    // Run the passed runnable
                    uiUpdater.run();
                    // Re-run it after the update interval
                    mHandler.postDelayed(this, interval);
                }
            };
        }

        /**
         * Starts the periodical update routine (mStatusChecker adds the callback to the handler).
         */
        public void startUpdates()
        {
            mStatusChecker.run();
        }

        /**
         * Stops the periodical update routine from running, by removing the callback.
         */
        public void stopUpdates()
        {
            mHandler.removeCallbacks(mStatusChecker);
        }
    }
}
