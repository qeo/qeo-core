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

package org.qeo.sample.finder.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import org.qeo.QeoFactory;
import org.qeo.StateReader;
import org.qeo.StateReaderListener;
import org.qeo.android.QeoAndroid;
import org.qeo.android.QeoConnectionListener;
import org.qeo.android.exception.QeoSecurityInitFailedException;
import org.qeo.android.exception.QeoServiceNotFoundException;
import org.qeo.exception.QeoException;
import org.qeo.system.DeviceId;
import org.qeo.system.DeviceInfo;
import org.qeo.system.RealmInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main activity of the QFinder app that will display a list of discovered Qeo devices.
 */
public class QFinderActivity
    extends Activity implements DevicesAdapter.DeviceClickListener
{
    private static final String TAG = "QFinder";
    private QeoFactory mQeo = null;
    private QeoConnectionListener mListener = null;
    private StateReader<DeviceInfo> mDevicesReader = null;
    //private ArrayAdapter<DeviceInfo> mAdapter = null;
    //private ListView mListview = null;
    private DevicesAdapter mAdapter;
    private DeviceId mThisDeviceId = null;
    private boolean mQeoClosed = false;
    private final List<DeviceInfo> mDevices = new ArrayList<DeviceInfo>();

    /**
     * Implementation of a StateReaderListener interface that will be used in triggering updates to the device list.
     */
    private class DeviceIdListener
        implements StateReaderListener
    {
        /**
         * Whenever anything changes with respect to discovered devices this method will be called. This can be either
         * due to the discovery of a new device, the disappearance of an existing device or the update of the data of an
         * existing device. In response to this notification we will update our device list in the UI.
         */
        @Override
        public void onUpdate()
        {
            Log.d(TAG, "refreshing list");
            /* First remove all items from the list. */
            mDevices.clear();
            /* Then add all devices that are still available. */
            for (final DeviceInfo device : mDevicesReader) {
                mDevices.add(device);
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * A method for connecting to the Qeo service.
     */
    private void qeoInit()
    {
        Log.d(TAG, "Initializing qeo connection");
        /*
         * Initialize Qeo and provide a callback for handling the notification of being ready and disconnected.
         */
        mListener = new QeoConnectionListener()
        {
            /**
             * When the connection with the Qeo service is ready we can create our reader.
             */
            @Override
            public void onQeoReady(QeoFactory qeo)
            {
                mQeo = qeo;
                Log.d(TAG, "onQeoReady");
                mAdapter.setOwnDeviceId(QeoAndroid.getDeviceId());
                try {
                    /*
                     * Retrieve the identifier of the device on which the app is running. This identifier will be used
                     * later to change the determine which color to use for displaying the discovered devices.
                     */
                    mThisDeviceId = QeoAndroid.getDeviceId();
                    /*
                     * create a state reader for DeviceInfo and listen for updates using the StateReaderListener
                     * interface implemented by this activity (see the onUpdate method above).
                     */
                    mDevicesReader = mQeo.createStateReader(DeviceInfo.class, new DeviceIdListener());
                }
                catch (final QeoException e) {
                    Log.e(TAG, "Error creating state reader", e);
                }
                mQeoClosed = false;
            }

            @Override
            public void onQeoError(QeoException ex)
            {
                if (ex instanceof QeoServiceNotFoundException) {
                    Toast.makeText(QFinderActivity.this, "QeoService not installed, exiting!", Toast.LENGTH_LONG)
                        .show();
                }
                else if (ex instanceof QeoSecurityInitFailedException) {
                    Toast.makeText(QFinderActivity.this, "Qeo Service failed due to " + ex.getMessage(),
                        Toast.LENGTH_LONG).show();
                }
                Log.e(TAG, "Error initializing Qeo", ex);

                finish();
            }

            @Override
            public void onQeoClosed(QeoFactory qeo)
            {
                Log.w(TAG, "Connection to qeo service lost");
                mQeoClosed = true;
                mDevicesReader = null;
                mQeo = null;
                mDevices.clear();
                mAdapter.notifyDataSetChanged();
                Toast.makeText(QFinderActivity.this, "Qeo connection closed", Toast.LENGTH_LONG).show();
            }
        };
        QeoAndroid.initQeo(getApplicationContext(), mListener);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        mDevices.clear();

        /* Initialize the UI. */
        setContentView(R.layout.activity_main);

        RecyclerView devicesView = (RecyclerView) findViewById(R.id.devicesRecyclerView);
        devicesView.setHasFixedSize(false);
        devicesView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new DevicesAdapter(this, mDevices, this);
        devicesView.setAdapter(mAdapter);

        /* Initialize Qeo. */
        qeoInit();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (mQeoClosed) {
            // if the connection to the service was lost while we're not active, restore it again.
            qeoInit();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        /* Close our reader. */
        if (mDevicesReader != null) {
            mDevicesReader.close();
            mDevicesReader = null;
        }
        /* Disconnect from the service. */
        if (mListener != null) {
            QeoAndroid.closeQeo(mListener);
        }
    }

    @Override
    public void onItemClicked(DeviceInfo device)
    {
        //Show details about 1 device
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Qeo device");
        String deviceId = new UUID(device.deviceId.upper, device.deviceId.lower).toString();
        String msg = "DeviceId = " + deviceId + "\nmanufacturer = " + device.manufacturer + "\nmodelName = "
            + device.modelName + "\nproductClass = " + device.productClass + "\nserialNumber = "
            + device.serialNumber + "\nhardwareVersion = " + device.hardwareVersion
            + "\nsoftwareVersion = " + device.softwareVersion + "\nuserFriendlyName = "
            + device.userFriendlyName + "\nconfigURL = " + device.configURL;
        RealmInfo realmInfo = device.realmInfo;
        if (realmInfo != null && realmInfo.realmId != 0) {
            msg += "\nRealm:\n  Id: " + realmInfo.realmId + "\n  User: " + realmInfo.userId + "\n  Device: "
                + realmInfo.deviceId + "\n  URL: " + realmInfo.url;
        }
        builder.setMessage(msg);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }
}
