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

package org.qeo.android.test;

import org.qeo.DefaultStateChangeReaderListener;
import org.qeo.QeoFactory;
import org.qeo.StateChangeReader;
import org.qeo.android.QeoAndroid;
import org.qeo.exception.QeoException;
import org.qeo.system.DeviceId;
import org.qeo.system.DeviceInfo;
import org.qeo.testframework.QeoConnectionTestListener;
import org.qeo.testframework.QeoTestCase;
import org.qeo.testframework.TestListener;

public class DeviceInfoTest
    extends QeoTestCase

{
    protected QeoAndroid myQeoObject = null;
    DeviceId secondDeviceId = null;
    DeviceId firstDeviceId = null;
    private StateChangeReader<DeviceInfo> devices = null;
    public static final String TAG = "qeo-android.DeviceInfoTest";
    private boolean failed;

    private class MyStateListener
        extends TestListener<DeviceInfo>
    {

        public MyStateListener()
            throws QeoException
        {
            super();
        }

        public DeviceId checkData()
            throws QeoException
        {
            assertNotNull(lastReceivedItem.deviceId);
            return lastReceivedItem.deviceId;
        }
    }

    @Override
    public void setUp()
        throws Exception
    {
        println("DeviceInfoTest-setup");
        failed = false;
        super.setUp();

    }

    @Override
    public void tearDown()
        throws Exception
    {
        super.tearDown();
        println("DeviceInfoTest-teardown");
    }

    public void testDeviceInfoNull()
        throws Exception
    {
        println("DeviceInfoNull-test");
        tearDown();
        assertNull(QeoAndroid.getDeviceId());
        setUp();
    }

    public void testDeviceInfoTest()
        throws Exception
    {
        println("DeviceInfoTest-test");
        MyStateListener mylistener = new MyStateListener();
        devices = mQeo.createStateChangeReader(DeviceInfo.class, mylistener);
        println("DeviceInfoTest-test-createStateChangeReader-1st time");
        assertNotNull(devices);
        println("DeviceInfoTest-test-StateChangeReader-NOTNULL");
        // wait until DeviceInfo is received
        waitForData(mylistener.onDataSem);
        waitForData(mylistener.onNoMoreDataSem);
        firstDeviceId = mylistener.checkData();
        println("firstDeviceID : " + firstDeviceId);
        println("firstDeviceID.upper : " + firstDeviceId.upper);
        println("firstDeviceID.lower : " + firstDeviceId.lower);
        // close statereader and qeo
        devices.close();
        tearDown();
        println("Qeo is closed and will now be re-initialised");
        // init qeo again
        setUp();

        // second run - check the published device id is still the same

        mylistener = new MyStateListener();
        devices = mQeo.createStateChangeReader(DeviceInfo.class, mylistener);
        println("DeviceInfoTest-test-createStateChangeReader-2nd time");
        assertNotNull(devices);
        println("DeviceInfoTest-test-StateChangeReader-NOTNULL");

        // wait until DeviceInfo is received
        waitForData(mylistener.onDataSem);
        waitForData(mylistener.onNoMoreDataSem);
        secondDeviceId = mylistener.checkData();

        assertEquals(firstDeviceId.upper, secondDeviceId.upper);
        assertEquals(firstDeviceId.lower, secondDeviceId.lower);
    }

    /**
     * Test that deviceinfo is not published on open domain.
     */
    public void testOpenDomain()
        throws QeoException, InterruptedException
    {
        QeoConnectionTestListener testListener = super.createFactory(QeoFactory.OPEN_ID);
        QeoFactory qeoOpen = testListener.getFactory();
        final DeviceId deviceId = QeoAndroid.getDeviceId();
        assertNotNull(deviceId);
        qeoOpen.createStateChangeReader(DeviceInfo.class, new DefaultStateChangeReaderListener<DeviceInfo>() {
            @Override
            public void onData(DeviceInfo t)
            {
                if (t.deviceId.equals(deviceId)) {
                    // only fail if found own deviceId. Others might be detecting during test too.
                    failed = true;
                    fail("Did not expect deviceinfo from " + t.userFriendlyName + " -- " + t.modelName);
                }
            }
        });
        Thread.sleep(500); // give the reader 500ms to settle
        assertFalse(failed);

        closeFactory(testListener);
    }
}
