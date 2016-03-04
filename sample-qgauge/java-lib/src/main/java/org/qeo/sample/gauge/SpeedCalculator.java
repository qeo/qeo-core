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

package org.qeo.sample.gauge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.qeo.QeoFactory;
import org.qeo.StateReader;
import org.qeo.StateReaderListener;
import org.qeo.exception.QeoException;

/**
 * SpeedCalculator is a helper class to compute network traffic speeds based on NetStatMessages.
 * 
 * At a given interval, SpeedCalculator will check whether NetStatMessages have been updated. The previous counter
 * values are cached within SpeedCalculator as the NetStatMessages contain only the latest counter values. The resulting
 * speed measurements are handled by a SpeedHandler.
 * 
 */
public class SpeedCalculator
{
    /**
     * SpeedCalculatorTask class calculates the speed based on the data available from NetStat message.
     */
    private static class SpeedCalculatorTask
            extends TimerTask
    {

        private final Map<String, NetStatMessage> mNetStatMsgCache = new HashMap<String, NetStatMessage>();
        private final Set<String> mDeviceCache = new HashSet<String>();
        private static final long S_SEC = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
        //
        private final SpeedHandler mHandler;
        private final StateReader<NetStatMessage> mReader;
        private boolean mNewDataAvailable; // defaults to false;

        //
        private NetworkInterfaceSpeedData computeSpeed(NetStatMessage msg)
        {
            /**
             * The combination of deviceid and interface name is used as key for cache to avoid overriding the
             * information because devices can have same interface names but unique device id.
             */

            String deviceId = new UUID(msg.deviceId.upper, msg.deviceId.lower).toString();
            String key = msg.ifName + deviceId;
            NetworkInterfaceSpeedData ifaceSpeedData = new NetworkInterfaceSpeedData(msg.ifName);
            // retrieve previous message from cache
            NetStatMessage prev = mNetStatMsgCache.get(key);
            if (null != prev) {
                // ensure we received new counter values
                if (msg.timestamp == prev.timestamp) {
                    return null;
                }
                double interval = (msg.timestamp - prev.timestamp) / (0.0 + S_SEC);
                // Fill InterfaceSpeedData object with new data.
                ifaceSpeedData.setDeviceId(deviceId);
                ifaceSpeedData.setKbpsIn((msg.bytesIn - prev.bytesIn) / (interval * 1024));
                ifaceSpeedData.setPcktsIn((msg.packetsIn - prev.packetsIn) / interval);
                ifaceSpeedData.setKbpsOut((msg.bytesOut - prev.bytesOut) / (interval * 1024));
                ifaceSpeedData.setPcktsOut((msg.packetsOut - prev.packetsOut) / interval);
            }
            // store message for next speed computation

            mNetStatMsgCache.put(key, msg);
            return ifaceSpeedData;

        }

        public SpeedCalculatorTask(SpeedHandler h, QeoFactory qeo)
            throws QeoException
        {
            mHandler = h;
            mReader = qeo.createStateReader(NetStatMessage.class, new StateReaderListener() {
                @Override
                public void onUpdate()
                {
                    synchronized (this) {
                        mNewDataAvailable = true;
                    }
                }
            });
        }

        @Override
        public void run()
        {
            /**
             * This code block is put in synchronized block to make sure that all data provided by reader is processed.
             */
            synchronized (this) {
                if (mNewDataAvailable) {
                    final List<String> ifaceListToRemove = new ArrayList<String>();
                    final List<NetworkInterfaceSpeedData> msgList = new ArrayList<NetworkInterfaceSpeedData>();
                    /**
                     * tempDataMap is used to do intermediate operations(deleting removed interfaces and writers, adding
                     * new interfaces and writers from mNetStatMsgCache) for processing data for callbacks.
                     */
                    final Map<String, NetStatMessage> tempDataMap = new HashMap<String, NetStatMessage>();
                    tempDataMap.putAll(mNetStatMsgCache);
                    /**
                     * iterate over all known network interfaces and save the computed data
                     */
                    for (NetStatMessage msg : mReader) {
                        String deviceId = new UUID(msg.deviceId.upper, msg.deviceId.lower).toString();
                        String key = msg.ifName + deviceId;
                        msgList.add(computeSpeed(msg));

                        /**
                         * send callback for new writer available update
                         */
                        if (!mDeviceCache.contains(deviceId)) {
                            mDeviceCache.add(deviceId);
                            mHandler.onSpeedDeviceAvailable(deviceId);
                        }
                        /**
                         * Removes the entry for interface name from tempDataMap if matches with new data received by
                         * reader.
                         */
                        if (tempDataMap.containsKey(key)) {
                            tempDataMap.remove(key);
                        }
                    }

                    /**
                     * send callback to update the UI with new interface data
                     */
                    if (msgList.size() != 0) {
                        mHandler.onSpeedAvailable(msgList);
                    }

                    /**
                     * Remove old interface data from cache and fill the list for callback
                     */
                    String tempdeviceid = "";
                    for (String key : tempDataMap.keySet()) {
                        ifaceListToRemove.add(mNetStatMsgCache.get(key).ifName);
                        /**
                         * get device id for removed writer to update UI
                         */
                        tempdeviceid = new UUID(mNetStatMsgCache.get(key).deviceId.upper,
                                mNetStatMsgCache.get(key).deviceId.lower).toString();
                        mNetStatMsgCache.remove(key);

                        /**
                         * send callback for writer remove update
                         */
                        if (mDeviceCache.contains(tempdeviceid)) {
                            mDeviceCache.remove(tempdeviceid);
                            mHandler.onSpeedDeviceRemoved(tempdeviceid);
                        }
                    }
                    /**
                     * send callback to update the UI for removed interface data
                     */
                    if (ifaceListToRemove.size() != 0) {
                        mHandler.onSpeedInterfaceRemoved(ifaceListToRemove, tempdeviceid);

                    }
                }
                mNewDataAvailable = false;
            }
        }

        @Override
        public boolean cancel()
        {
            // make sure to first trigger cancellation of our timerTask, we should not be executed anymore
            final boolean result = super.cancel();
            // next close/clear our resources
            synchronized (this) {
                mReader.close();
                mNetStatMsgCache.clear();
                mDeviceCache.clear();
            }
            return result;
        }
    }

    private final Timer mTimer;
    private SpeedCalculatorTask mTask;
    private final SpeedHandler mHandler;
    private final QeoFactory mQeo;

    /**
     * Constructs a SpeedCalculator object.
     * 
     * @param h A SpeedHandler that will be called when a new speed measurement is computed.
     * @param qeo QeoFactory instance to create readers/writers
     */
    public SpeedCalculator(SpeedHandler h, QeoFactory qeo)
    {
        mTimer = new Timer();
        mQeo = qeo;
        mHandler = h;
    }

    /**
     * Starts this SpeedCalculator at a given refresh rate.
     * 
     * @param refreshRate The rate at which each network traffic speeds are refreshed (in milliseconds).
     * @throws QeoException Throws QeoException if problem occurs in to Reading data
     */
    public synchronized void start(long refreshRate)
        throws QeoException
    {
        stop();
        mTask = new SpeedCalculatorTask(mHandler, mQeo);
        mTimer.scheduleAtFixedRate(mTask, 0, refreshRate);
    }

    /**
     * Stops this SpeedCalculator.
     */
    public synchronized void stop()
    {
        if (mTask != null) {
            mTask.cancel();
            mTask = null;
        }
    }
}
