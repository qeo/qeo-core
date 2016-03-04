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

package org.qeo.sample.gauge.android.reader;

import org.qeo.QeoFactory;
import org.qeo.android.QeoAndroid;
import org.qeo.android.QeoConnectionListener;
import org.qeo.exception.QeoException;
import org.qeo.sample.gauge.SpeedCalculator;
import org.qeo.sample.gauge.android.reader.helper.DeviceInfoReader;
import org.qeo.sample.gauge.android.reader.helper.ReaderCallbackHandler;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * This service starts the Qeo service as Android service and prepares the Readers to read the data available over Qeo.
 */
public class QeoLocalService
    extends Service
{
    private static final String LOGGING_TAG = QeoLocalService.class.getSimpleName();
    private QeoFactory mQeo = null;
    private QeoConnectionListener mListener = null;
    private ReaderCallbackHandler mIfdetails;
    private Intent mIntent;

    /**
     * Responsible for calculating speed and polling for new snew QeoConnectionListener() { // Callback received once
     * Qeo service is initialized successfully.
     * 
     * @Override public void onQeoReady(QeoFactory qeo) { mQeo = qeo; try { // DeviceInfoReader class creates reader to
     *           read for All Qeo devices. mDeviceReader = new DeviceInfoReader(mIfdetails, mQeo); // SpeedCalculator
     *           class creates the reader for reading traffic statistics data. mSpeedCalc = new
     *           SpeedCalculator(mIfdetails, mQeo); mSpeedCalc.start(1000); } catch (QeoException e) {
     *           Log.e(LOGGING_TAG, "Failure during initialisation", e); stopSelf(); } }
     * @Override public void onQeoClosed(QeoFactory qeo) { super.onQeoClosed(qeo); if (mDeviceReader != null) {
     *           mDeviceReader.stop(); } mDeviceReader = null; if (mSpeedCalc != null) { mSpeedCalc.stop(); } mSpeedCalc
     *           = null; mQeo = null; mIntent.putExtra(getString(R.string.broadcast_qeo_force_stop), true);
     *           sendBroadcast(mIntent); }
     * @Override public void onQeoError(QeoException ex) { Log.e(LOGGING_TAG, "Failure during initialisation", ex);
     *           mIntent.putExtra(getString(R.string.broadcast_qeo_force_stop), false);
     *           mIntent.putExtra(getString(R.string.broadcast_general_failure), false);
     *           mIntent.putExtra(getString(R.string.broadcast_qeo_fail_msg), ex.getMessage()); stopSelf();
     *           sendBroadcast(mIntent); } }peed messages.
     */
    private SpeedCalculator mSpeedCalc;
    private DeviceInfoReader mDeviceReader;
    /**
     * A constant to identify the Service status intent.
     */
    public static final String QEO_SERVICE_STATUS = "org.qeo.sample.gauge.android.reader.SERVICE_STATUS";

    @Override
    public void onCreate()
    {
        mIfdetails = GaugeReaderApplication.sReaderCallBackObj;
        mIntent = new Intent(QEO_SERVICE_STATUS);
        startQeoService();
    }

    private void startQeoService()
    {

        mListener = new QeoConnectionListener() {
            // Callback received once Qeo service is initialized successfully.
            @Override
            public void onQeoReady(QeoFactory qeo)
            {
                mQeo = qeo;
                try {
                    // DeviceInfoReader class creates reader to read for All Qeo devices.
                    mDeviceReader = new DeviceInfoReader(mIfdetails, mQeo);
                    // SpeedCalculator class creates the reader for reading traffic statistics data.
                    mSpeedCalc = new SpeedCalculator(mIfdetails, mQeo);
                    mSpeedCalc.start(1000);
                }
                catch (QeoException e) {
                    Log.e(LOGGING_TAG, "Failure during initialisation", e);
                    stopSelf();
                }
            }

            @Override
            public void onQeoClosed(QeoFactory qeo)
            {
                super.onQeoClosed(qeo);
                if (mDeviceReader != null) {
                    mDeviceReader.stop();
                }
                mDeviceReader = null;
                if (mSpeedCalc != null) {
                    mSpeedCalc.stop();
                }
                mSpeedCalc = null;
                mQeo = null;
                mIntent.putExtra(getString(R.string.broadcast_qeo_force_stop), true);
                sendBroadcast(mIntent);
            }

            @Override
            public void onQeoError(QeoException ex)
            {
                Log.e(LOGGING_TAG, "Failure during initialisation", ex);
                mIntent.putExtra(getString(R.string.broadcast_qeo_force_stop), false);
                mIntent.putExtra(getString(R.string.broadcast_general_failure), false);
                mIntent.putExtra(getString(R.string.broadcast_qeo_fail_msg), ex.getMessage());
                stopSelf();
                sendBroadcast(mIntent);
            }
        };
        // Get a QeoFactory object to create readers
        QeoAndroid.initQeo(getApplicationContext(), mListener);
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        return false;
    }

    @Override
    public void onDestroy()
    {
        if (mDeviceReader != null) {
            mDeviceReader.stop();
            mDeviceReader = null;
        }
        if (mSpeedCalc != null) {
            mSpeedCalc.stop();
            mSpeedCalc = null;
        }
        if (mListener != null) {
            QeoAndroid.closeQeo(mListener);
        }
    }
}
