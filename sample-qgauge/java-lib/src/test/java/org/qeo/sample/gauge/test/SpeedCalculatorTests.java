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

package org.qeo.sample.gauge.test;

import static org.qeo.sample.gauge.test.NetStatMessageUtil.newNetStatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.qeo.StateWriter;
import org.qeo.exception.QeoException;
import org.qeo.sample.gauge.NetStatMessage;
import org.qeo.sample.gauge.NetworkInterfaceSpeedData;
import org.qeo.sample.gauge.SpeedCalculator;
import org.qeo.sample.gauge.SpeedHandler;
import org.qeo.system.DeviceId;
import org.qeo.testframework.QeoTestCase;

public class SpeedCalculatorTests
    extends QeoTestCase
{
    private StateWriter<NetStatMessage> mWriter;
    private final List<String> mDevicesAdded;
    private final List<String> mDevicesRemoved;
    private final DeviceId mDeviceId = new DeviceId();

    public SpeedCalculatorTests()
    {
        mDevicesAdded = new ArrayList<String>();
        mDevicesRemoved = new ArrayList<String>();
        mDeviceId.upper = 456L;
        mDeviceId.lower = 34L;
    }

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        mDevicesAdded.clear();
        mDevicesRemoved.clear();
    }

    /**
     * Test to repeatedly start and stop speedCalculator. This test is expected to succeed when DE1560 is fixed.
     * 
     * @throws QeoException
     */
    public void testRepeatedStartStopCalculator()
        throws QeoException
    {
        log("testRepeatedStartStopCalculator");
        final class SpeedIgnorer
            implements SpeedHandler
        {
            @Override
            public void onSpeedInterfaceRemoved(List<String> ifNameList, String id)
            {
                return;
            }

            @Override
            public void onSpeedAvailable(List<NetworkInterfaceSpeedData> ifaceSpeedDataList)
            {
                return;
            }

            @Override
            public void onSpeedDeviceAvailable(String writerId)
            {
            }

            @Override
            public void onSpeedDeviceRemoved(String writerId)
            {
            }
        }
        SpeedCalculator calc = new SpeedCalculator(new SpeedIgnorer(), mQeo);
        for (int i = 0; i < 100; i++) {
            calc.start(100);
            calc.stop();
            log(Integer.toString(i));
        }
    }

    /**
     * Test to test the speed calculation done.
     * 
     * @throws QeoException
     */
    public void testSpeedCalculator()
        throws QeoException
    {
        log("testSpeedCalculator");
        final List<NetworkInterfaceSpeedData[]> itfList = new CopyOnWriteArrayList<NetworkInterfaceSpeedData[]>();
        final class SpeedTester
            implements SpeedHandler
        {
            @Override
            public void onSpeedInterfaceRemoved(List<String> ifNameList, String id)
            {
                log("Entered Speedtester");
            }

            @Override
            public void onSpeedAvailable(List<NetworkInterfaceSpeedData> ifaceSpeedDataList)
            {
                final NetworkInterfaceSpeedData[] itfs =
                    ifaceSpeedDataList.toArray(new NetworkInterfaceSpeedData[ifaceSpeedDataList.size()]);
                // Add to iftList for having it verified in the main junit thread.
                itfList.add(itfs);
            }

            @Override
            public void onSpeedDeviceAvailable(String writerId)
            {
            }

            @Override
            public void onSpeedDeviceRemoved(String writerId)
            {
            }
        }
        SpeedCalculator calc = new SpeedCalculator(new SpeedTester(), mQeo);
        log("calculator created");
        NetStatMessage msg = newNetStatMessage("test", mDeviceId, 1L, 2L, 3L, 4L);
        NetStatMessage msg2 = newNetStatMessage("test", mDeviceId, 2L, 3L, 4L, 5L);
        log("netstatmessage created");
        mWriter = mQeo.createStateWriter(NetStatMessage.class);
        mWriter.write(msg);
        calc.start(100);
        for (int i = 0; i < 10; i++) {
            doSleep(500);
            if (i % 2 == 0) {
                mWriter.write(msg2);
            }
            else {
                mWriter.write(msg);
            }
        }
        calc.stop();
        mWriter.close();
        doSleep(1000);
        int cnt = 0;
        while (cnt < 10) {
            NetworkInterfaceSpeedData[][] result = itfList.toArray(new NetworkInterfaceSpeedData[itfList.size()][]);
            if (result.length != 0) {
                // we have received some at least
                log("SpeedCalculatorTests - found " + result.length + " samples");
                //
                for (NetworkInterfaceSpeedData[] data : result) {
                    assertEquals(1, data.length);
                    assertEquals("test", data[0].getIfaceName());
                }
                // test completed !
                break;
            }
            doSleep(1000);
            cnt++;
        }
        if (cnt >= 10) {
            fail("timeout");
        }
    }

    public void test_publishAndRemoveWriter()
        throws QeoException
    {
        log("test_publishAndRemoveWriter");
        final class SpeedTester
            implements SpeedHandler
        {
            @Override
            public void onSpeedInterfaceRemoved(List<String> ifNameList, String id)
            {
            }

            @Override
            public void onSpeedAvailable(List<NetworkInterfaceSpeedData> ifaceSpeedDataList)
            {
            }

            @Override
            public void onSpeedDeviceAvailable(String writerId)
            {
                mDevicesAdded.add(writerId);
            }

            @Override
            public void onSpeedDeviceRemoved(String writerId)
            {
                mDevicesRemoved.add(writerId);
            }
        }
        // check preconditions
        assertEquals(0, mDevicesAdded.size());
        assertEquals(0, mDevicesRemoved.size());
        // start test
        SpeedCalculator calc = new SpeedCalculator(new SpeedTester(), mQeo);
        NetStatMessage msg = newNetStatMessage("test", mDeviceId, 1L, 2L, 3L, 4L);
        log(msg.toString());
        mWriter = mQeo.createStateWriter(NetStatMessage.class);
        mWriter.write(msg);
        calc.start(100);
        int count = 10;
        while (--count > 0) {
            doSleep(1000);
            if (!mDevicesAdded.isEmpty()) {
                break;
            }
        }
        assertEquals(1, mDevicesAdded.size());
        assertEquals(0, mDevicesRemoved.size());
        mWriter.close();
        int i = 10;
        while (--i > 0) {
            doSleep(1000);
            if (!mDevicesRemoved.isEmpty()) {
                break;
            }
        }
        calc.stop();
        assertEquals(1, mDevicesAdded.size());
        assertEquals(1, mDevicesRemoved.size());
    }

    private void doSleep(int timeout)
    {
        try {
            Thread.sleep(timeout);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
