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

package end2end.src.test.java.org.qeo.sample.gauge.end2endTests;


public class GaugeEndToEnd
        extends QeoTestCase
{
    // private StateWriter<NetStatMessage> mWriter;
    // private StateReader<NetStatMessage> mReader;
    // private boolean mOnData = false;
    // private int mMsgCounter = 0;

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
     * Test for the speedwriter side<br>
     */
    @Device0
    public synchronized void testGaugeCreateSpeedWriter_writeSpeed()
            throws QeoException
    {

        // Sleep to make sure the reader was already created
        try {
            Thread.sleep(2000);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        mWriter = qeo.createStateWriter(NetStatMessage.class);
        System.out.println("SpeedGaugeWriter created!");

        try {
            synchronized (this) {
                this.wait(5000);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        NetStatMessage netMsg = new NetStatMessage("MOCK_eth0", 1000L, 5L,
                2000L, 10L);
        mWriter.write(netMsg);
        try {
            synchronized (this) {
                this.wait(5000);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test for the reader side<br>
     */
    @Device1
    public synchronized void testGaugeCreateSpeedReader_readSpeed()
            throws QeoException
    {
        mReader = qeo.createStateReader(NetStatMessage.class);
        try {
            mReader.setListener(new StateReaderAdapter<NetStatMessage>() {
                @Override
                public void onData(NetStatMessage arg0)
                {
                    System.out.println("onData: " + this + ";  " + arg0);
                    synchronized (GaugeEndToEnd.this) {
                        GaugeEndToEnd.this.notifyAll();
                    }
                }
            });
        } catch (QeoException e1) {
            e1.printStackTrace();
        }

        try {
            synchronized (this) {
                this.wait(5000);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }

        Iterator<NetStatMessage> it = mReader.iterator();
        NetStatMessage received;
        while (it.hasNext()) {
            received = it.next();
            // System.out.println(received.toString());
            assertTrue("ifname is not equal",
                    received.ifName.equals("MOCK_eth0"));
            assertTrue("bytesIn is not equal", received.bytesIn == 1000L);
            assertTrue("bytesOut is not equal", received.bytesOut == 2000L);
            assertTrue("packetsIn is not equal", received.packetsIn == 5L);
            assertTrue("packetsOut is not equal", received.packetsOut == 10L);
        }
    }
}
