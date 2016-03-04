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

import java.util.Iterator;

import org.qeo.StateReader;
import org.qeo.StateReaderListener;
import org.qeo.StateWriter;
import org.qeo.exception.QeoException;
import org.qeo.sample.gauge.NetStatMessage;
import org.qeo.sample.gauge.NetStatProvider;
import org.qeo.sample.gauge.NetStatPublisher;
import org.qeo.system.DeviceId;
import org.qeo.testframework.QeoTestCase;

public class FunctionalTests
    extends QeoTestCase
{

    private StateWriter<NetStatMessage> mWriter;
    private StateReader<NetStatMessage> mReader;
    private boolean mOnData = false;
    private final DeviceId MOCK_DEVICE_ID = new DeviceId();

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        mOnData = false;
        MOCK_DEVICE_ID.upper = 536L;
        MOCK_DEVICE_ID.lower = 446L;
        // Initialize a reader and a writer before each test.
        mWriter = mQeo.createStateWriter(NetStatMessage.class);
        Thread.sleep(1000);
        mReader = mQeo.createStateReader(NetStatMessage.class, new StateReaderListener() {

            @Override
            public void onUpdate()
            {
                mOnData = true;
            }
        });
        try {
            Thread.sleep(2000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    public void tearDown()
        throws Exception
    {
        // deinit the reader and the writer after each test so you start with a
        // clean one for each test.
        if (null != mWriter) {
            mWriter.close();
        }
        if (null != mReader) {
            mReader.close();
        }
        super.tearDown();
    }

    /**
     * This tests a bug that existed where a publisher was created and then a reader that onData was never called.
     * 
     * @param args
     * @throws QeoException
     */
    public void testPublisherFirstThenReader()
        throws QeoException
    {

        try {
            mWriter.write(newNetStatMessage("test", MOCK_DEVICE_ID, 1L, 2L, 3L, 4L));
            System.out.println("Published message!");
        }
        catch (Exception qe) {
            qe.printStackTrace();
            return;
        }
        pause(1000);

        int count = 10;
        while (--count > 0) {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (mOnData) {
                break;
            }
        }

        assertTrue(mOnData);

    }

    public static void pause(int time)
    {
        try {
            Thread.sleep(time);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
     * @Desc We will create a new message, publish it over Qeo, read it at the subscriber side and evaluate if the
     * message was transmitted over Qeo succesfully.
     */
    public void test_expectedAndReceivedValues()
        throws QeoException
    {

        NetStatMessage netMsg = newNetStatMessage("MOCK_eth0", MOCK_DEVICE_ID, 1000L, 5L, 2000L, 10L);
        mWriter.write(netMsg);

        int count = 10;
        while (--count > 0) {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (mOnData) {

                break;
            }
        }
        assertTrue(mOnData);

        Iterator<NetStatMessage> it = mReader.iterator();
        NetStatMessage received;
        while (it.hasNext()) {
            received = it.next();
            // System.out.println(received.toString());
            assertTrue("ifname is not equal", netMsg.ifName.equals(received.ifName));
            assertTrue("deviceId is not equal", netMsg.deviceId.toString().equals(received.deviceId.toString()));
            assertTrue("bytesIn is not equal", netMsg.bytesIn == received.bytesIn);
            assertTrue("bytesOut is not equal", netMsg.bytesOut == received.bytesOut);
            assertTrue("packetsIn is not equal", netMsg.packetsIn == received.packetsIn);
            assertTrue("packetsOut is not equal", netMsg.packetsOut == received.packetsOut);
            assertTrue("timestamp is not equal", netMsg.timestamp == received.timestamp);

        }
    }

    public void test_NetStatPublisherInstantiation()
        throws QeoException
    {

        final NetStatMessage[] expectedMessages = new NetStatMessage[10];

        final class NetStatProviderTest
            implements NetStatProvider
        {
            @Override
            public NetStatMessage[] getCurrentStats()
            {
                expectedMessages[0] = newNetStatMessage("test1", MOCK_DEVICE_ID, 1L, 2L, 3L, 4L, 5L);
                expectedMessages[1] = newNetStatMessage("test2", MOCK_DEVICE_ID, 2L, 3L, 4L, 5L, 6L);
                expectedMessages[2] = newNetStatMessage("test3", MOCK_DEVICE_ID, 3L, 4L, 5L, 6L, 7L);
                expectedMessages[3] = newNetStatMessage("test4", MOCK_DEVICE_ID, 4L, 5L, 6L, 7L, 8L);
                expectedMessages[4] = newNetStatMessage("test5", MOCK_DEVICE_ID, 5L, 6L, 7L, 8L, 9L);
                expectedMessages[5] = newNetStatMessage("test6", MOCK_DEVICE_ID, 1L, 2L, 3L, 4L, 5L);
                expectedMessages[6] = newNetStatMessage("test7", MOCK_DEVICE_ID, 1L, 2L, 3L, 4L, 5L);
                expectedMessages[7] = newNetStatMessage("test8", MOCK_DEVICE_ID, 1L, 2L, 3L, 4L, 5L);
                expectedMessages[8] = newNetStatMessage("test9", MOCK_DEVICE_ID, 1L, 2L, 3L, 4L, 5L);
                expectedMessages[9] = newNetStatMessage("test10", MOCK_DEVICE_ID, 1L, 2L, 3L, 4L, 5L);
                return expectedMessages;
            }

            @Override
            public void publisherStopped(NetStatPublisher netStatPublisher, Throwable cause)
            {
                System.err.println("Application is stopped due unexpected exception.");
                cause.printStackTrace();
            }

        }

        final NetStatMessage[] receivedMsgs = new NetStatMessage[10];

        NetStatProviderTest netStatProvider = new NetStatProviderTest();

        NetStatPublisher publisher = new NetStatPublisher(netStatProvider, 7000L, mQeo);
        System.out.println("Publisher created");

        int count = 10;
        while (--count > 0) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (mOnData) {
                break;
            }
        }
        assertTrue(mOnData);
        int j = 0;

        while (j != 10) {
            for (NetStatMessage msg : mReader) {
                receivedMsgs[j] = msg;
                j++;
                if (j > 9) {
                    break;
                }
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        for (int i = 0; i < receivedMsgs.length; i++) {
            assertEquals(expectedMessages[i].ifName, receivedMsgs[i].ifName);
            assertEquals(expectedMessages[i].deviceId.toString(), receivedMsgs[i].deviceId.toString());
            assertEquals(expectedMessages[i].bytesIn, receivedMsgs[i].bytesIn);
            assertEquals(expectedMessages[i].packetsIn, receivedMsgs[i].packetsIn);
            assertEquals(expectedMessages[i].bytesOut, receivedMsgs[i].bytesOut);
            assertEquals(expectedMessages[i].packetsOut, receivedMsgs[i].packetsOut);
            assertEquals(expectedMessages[i].timestamp, receivedMsgs[i].timestamp);
            // System.out.println("ifname = " + receivedMsgs[i].ifName);
            // System.out.println("bytesIn = " + receivedMsgs[i].bytesIn);
            // System.out.println("bytesOut = " + receivedMsgs[i].packetsIn);
            // System.out.println("packetsIn = " + receivedMsgs[i].bytesOut);
            // System.out.println("packetsOut = " + receivedMsgs[i].packetsOut);
            // System.out.println("timestamp = " + receivedMsgs[i].timestamp);

        }
        if (null != publisher) {
            publisher.close();
        }
    }

}
