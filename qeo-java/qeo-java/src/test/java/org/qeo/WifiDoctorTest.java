/*
 * Copyright (c) 2015 - Qeo LLC
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

package org.qeo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.Semaphore;

import org.qeo.exception.QeoException;
import org.qeo.system.DeviceId;
import org.qeo.testframework.QeoTestCase;
import org.qeo.wifi.Interface;
import org.qeo.wifi.Radio;
import org.qeo.wifi.ScanList;
import org.qeo.wifi.ScanListEntry;
import org.qeo.wifi.ScanRequest;

import com.technicolor.wifidoctor.APStats;
import com.technicolor.wifidoctor.AssociatedStationStats;
import com.technicolor.wifidoctor.RadioStats;
import com.technicolor.wifidoctor.STAStats;
import com.technicolor.wifidoctor.TestRequest;

/**
 * Test cases using the QDM's used in the WIFI Doctor.
 */
public class WifiDoctorTest
    extends QeoTestCase
{
    private final Random random = new Random();

    private static class ScanRequestListener
        extends DefaultEventReaderListener<ScanRequest>
    {
        private final Semaphore onScanRequestSem = new Semaphore(0);
        private ScanRequest mScanRequest = null;

        @Override
        public void onData(final ScanRequest request)
        {
            println("ScanRequestListener.onData");
            mScanRequest = request;
            onScanRequestSem.release();
        }

        private ScanRequest getLatestScanRequest()
        {
            return mScanRequest;
        }
    }

    private static class ScanListListener
        implements StateChangeReaderListener<ScanList>
    {
        private final Semaphore onScanListDataSem = new Semaphore(0);
        private final Semaphore onScanListRemoveSem = new Semaphore(0);
        private final ArrayList<ScanList> mScanList = new ArrayList<ScanList>();

        @Override
        public void onData(ScanList scanList)
        {
            println("ScanListListener.onData");
            mScanList.add(scanList);
            onScanListDataSem.release();
        }

        @Override
        public void onNoMoreData()
        {
        }

        @Override
        public void onRemove(ScanList scanList)
        {
            println("ScanListListener.onRemove");
            mScanList.remove(scanList);
            onScanListRemoveSem.release();
        }

        private boolean hasScanList(ScanList scanList)
        {
            return mScanList.contains(scanList);
        }
    }

    private static class RadioStatsListener
        implements StateChangeReaderListener<RadioStats>
    {
        private final Semaphore onRadioStatsDataSem = new Semaphore(0);
        private final Semaphore onRadioStatsRemoveSem = new Semaphore(0);
        private final ArrayList<RadioStats> mRadioStats = new ArrayList<RadioStats>();

        @Override
        public void onData(RadioStats radioStats)
        {
            println("RadioStatsListener.onData");
            mRadioStats.add(radioStats);
            onRadioStatsDataSem.release();
        }

        @Override
        public void onNoMoreData()
        {
        }

        @Override
        public void onRemove(RadioStats radioStats)
        {
            println("RadioStatsListener.onRemove");
            mRadioStats.remove(radioStats);
            onRadioStatsRemoveSem.release();
        }

        private boolean hasRadioStats(RadioStats radioStats)
        {
            return mRadioStats.contains(radioStats);
        }
    }

    private static class MyStateReaderListener
        implements StateReaderListener
    {
        @Override
        public void onUpdate()
        {
        }
    };

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        println("setup");
    }

    @Override
    public void tearDown()
        throws Exception
    {
        println("tearDown");
        super.tearDown();
    }

    /**
     * Test reader and writer for the ScanRequest event topic.
     * 
     * @throws Exception
     */
    public void testScanRequestEvents()
        throws Exception
    {
        ScanRequest sr1 = getScanRequest();
        ScanRequest sr2 = getScanRequest();
        EventReader<ScanRequest> srr = null;
        EventWriter<ScanRequest> srw = null;

        final ScanRequestListener srl = new ScanRequestListener();

        try {
            srr = mQeo.createEventReader(ScanRequest.class, srl);
            assertNotNull(srr);
            srw = mQeo.createEventWriter(ScanRequest.class);
            assertNotNull(srw);

            srw.write(sr1);
            waitForData(srl.onScanRequestSem);
            assertEquals(sr1, srl.getLatestScanRequest());
            srw.write(sr2);
            waitForData(srl.onScanRequestSem);
            assertEquals(sr2, srl.getLatestScanRequest());
        }
        finally {
            if (null != srr) {
                println("close ScanRequest reader");
                srr.close();
                srr = null;
            }

            if (null != srw) {
                println("close ScanRequest writer");
                srw.close();
                srw = null;
            }
        }
    }

    /**
     * Test changeReader and writer for the ScanList state topic.
     * 
     * @throws Exception
     */
    public void testScanListStates()
        throws Exception
    {
        ScanList sl1 = getScanList();
        ScanList sl2 = getScanList();
        StateChangeReader<ScanList> slr = null;
        StateWriter<ScanList> slw = null;

        final ScanListListener sll = new ScanListListener();

        try {
            slr = mQeo.createStateChangeReader(ScanList.class, sll);
            assertNotNull(slr);
            slw = mQeo.createStateWriter(ScanList.class);
            assertNotNull(slw);

            slw.write(sl1);
            waitForData(sll.onScanListDataSem);
            assertTrue(sll.hasScanList(sl1));

            slw.write(sl2);
            waitForData(sll.onScanListDataSem);
            assertTrue(sll.hasScanList(sl2));

            slw.remove(sl1);
            waitForData(sll.onScanListRemoveSem);
            assertFalse(sll.hasScanList(sl1));

            slw.remove(sl2);
            waitForData(sll.onScanListRemoveSem);
            assertFalse(sll.hasScanList(sl2));
        }
        finally {
            if (null != slr) {
                println("close ScanList change reader");
                slr.close();
                slr = null;
            }

            if (null != slw) {
                println("close ScanList writer");
                slw.close();
                slw = null;
            }
        }
    }

    /**
     * Test changeReader and writer for the RadioStats state topic.
     * 
     * @throws Exception
     */
    public void testRadioStatsStates()
        throws Exception
    {
        RadioStats rs1 = getRadioStats();
        RadioStats rs2 = getRadioStats();
        StateChangeReader<RadioStats> rsr = null;
        StateWriter<RadioStats> rsw = null;

        final RadioStatsListener rsl = new RadioStatsListener();

        try {
            rsr = mQeo.createStateChangeReader(RadioStats.class, rsl);
            assertNotNull(rsr);
            rsw = mQeo.createStateWriter(RadioStats.class);
            assertNotNull(rsw);

            rsw.write(rs1);
            waitForData(rsl.onRadioStatsDataSem);
            assertTrue(rsl.hasRadioStats(rs1));

            rsw.write(rs2);
            waitForData(rsl.onRadioStatsDataSem);
            assertTrue(rsl.hasRadioStats(rs2));

            rsw.remove(rs1);
            waitForData(rsl.onRadioStatsRemoveSem);
            assertFalse(rsl.hasRadioStats(rs1));

            rsw.remove(rs2);
            waitForData(rsl.onRadioStatsRemoveSem);
            assertFalse(rsl.hasRadioStats(rs2));
        }
        finally {
            if (null != rsr) {
                println("close RadioStats change reader");
                rsr.close();
                rsr = null;
            }

            if (null != rsw) {
                println("close RadioStats writer");
                rsw.close();
                rsw = null;
            }
        }
    }

    /**
     * Test reader and writer for the TestRequest state topic.
     * 
     * @throws Exception
     */
    public void testTestRequestStates()
        throws Exception
    {
        TestRequest tr1 = getTestRequest();
        TestRequest tr2 = getTestRequest();
        StateReader<TestRequest> trr = null;
        StateWriter<TestRequest> trw = null;

        final MyStateReaderListener trl = new MyStateReaderListener();

        try {
            trr = mQeo.createStateReader(TestRequest.class, trl);
            assertNotNull(trr);
            Iterator<TestRequest> tri = trr.iterator();
            assertFalse(tri.hasNext());
            trw = mQeo.createStateWriter(TestRequest.class);
            assertNotNull(trw);

            trw.write(tr1);
            assertTrue(tri.hasNext());
            TestRequest tr1out = tri.next();
            assertEquals(tr1, tr1out);
            assertFalse(tri.hasNext());

            trw.write(tr2);
            assertTrue(tri.hasNext());
            TestRequest tr2out = tri.next();
            assertEquals(tr2, tr2out);
            assertFalse(tri.hasNext());

            trw.remove(tr1);
            trw.remove(tr2);
        }
        finally {
            if (null != trr) {
                println("close TestRequest reader");
                trr.close();
                trr = null;
            }

            if (null != trw) {
                println("close TestRequest writer");
                trw.close();
                trw = null;
            }
        }
    }

    /**
     * Test reader and writer for the Interface state topic.
     * 
     * @throws Exception
     */
    public void testInterfaceStates()
        throws Exception
    {
        Interface i1 = getInterface();
        Interface i2 = getInterface();
        StateReader<Interface> ir = null;
        StateWriter<Interface> iw = null;

        final MyStateReaderListener irl = new MyStateReaderListener();

        try {
            ir = mQeo.createStateReader(Interface.class, irl);
            assertNotNull(ir);
            Iterator<Interface> ii = ir.iterator();
            assertFalse(ii.hasNext());
            iw = mQeo.createStateWriter(Interface.class);
            assertNotNull(iw);

            iw.write(i1);
            assertTrue(ii.hasNext());
            Interface i1out = ii.next();
            assertEquals(i1, i1out);
            assertFalse(ii.hasNext());

            iw.write(i2);
            assertTrue(ii.hasNext());
            Interface i2out = ii.next();
            assertEquals(i2, i2out);
            assertFalse(ii.hasNext());

            iw.remove(i1);
            iw.remove(i2);
        }
        finally {
            if (null != ir) {
                println("close Interface reader");
                ir.close();
                ir = null;
            }

            if (null != iw) {
                println("close Interface writer");
                iw.close();
                iw = null;
            }
        }
    }

    /**
     * Test reader and writer for the Radio state topic.
     * 
     * @throws QeoException
     */
    public void testRadioStates()
        throws QeoException
    {
        Radio r1 = getRadio();
        Radio r2 = getRadio();
        StateReader<Radio> rr = null;
        StateWriter<Radio> rw = null;

        final MyStateReaderListener rrl = new MyStateReaderListener();

        try {
            rr = mQeo.createStateReader(Radio.class, rrl);
            assertNotNull(rr);
            Iterator<Radio> ri = rr.iterator();
            assertFalse(ri.hasNext());
            rw = mQeo.createStateWriter(Radio.class);
            assertNotNull(rw);

            rw.write(r1);
            assertTrue(ri.hasNext());
            Radio r1out = ri.next();
            assertEquals(r1, r1out);
            assertFalse(ri.hasNext());

            rw.write(r2);
            assertTrue(ri.hasNext());
            Radio r2out = ri.next();
            assertEquals(r2, r2out);
            assertFalse(ri.hasNext());

            rw.remove(r1);
            rw.remove(r2);
        }
        finally {
            if (null != rr) {
                println("close Radio reader");
                rr.close();
                rr = null;
            }

            if (null != rw) {
                println("close Radio writer");
                rw.close();
                rw = null;
            }
        }
    }

    /**
     * Helper functions.
     * 
     */

    private ScanRequest getScanRequest()
    {
        ScanRequest scanReq = new ScanRequest();
        scanReq.radio = getRandomUuid();
        return scanReq;
    }

    private ScanList getScanList()
    {
        ScanList scanList = new ScanList();
        scanList.list = getRandomScanListEntryArray();
        scanList.radio = getRandomUuid();
        scanList.timestamp = random.nextLong();
        return scanList;
    }

    private RadioStats getRadioStats()
    {
        RadioStats radioStats = new RadioStats();
        radioStats.APStats = getRandomAPStatsArray();
        radioStats.mediumAvailable = (short) random.nextInt(Short.MAX_VALUE + 1);
        radioStats.radio = getRandomUuid();
        radioStats.STAStats = getRandomSTAStatsArray();
        radioStats.testId = random.nextInt();
        radioStats.timestamp = random.nextLong();
        return radioStats;
    }

    private ScanListEntry[] getRandomScanListEntryArray()
    {
        int length = random.nextInt(25);
        ScanListEntry[] scanListEntryArray = new ScanListEntry[length];
        for (int i = 0; i < length; i++) {
            scanListEntryArray[i] = getRandomScanListEntry();
        }
        return scanListEntryArray;
    }

    private ScanListEntry getRandomScanListEntry()
    {
        ScanListEntry entry = new ScanListEntry();
        entry.BSSID = "BSSID_" + random.nextInt();
        entry.channel = random.nextInt();
        entry.RSSI = random.nextInt();
        entry.SSID = "SSID_" + random.nextInt();
        return entry;
    }

    private APStats[] getRandomAPStatsArray()
    {
        int length = random.nextInt(5);
        APStats[] apStatsArray = new APStats[length];
        for (int i = 0; i < length; i++) {
            apStatsArray[i] = getRandomAPStats();
        }
        return apStatsArray;
    }

    private APStats getRandomAPStats()
    {
        APStats apStats = new APStats();
        apStats.associatedStationStats = getAssociatedStationStatsArray();
        apStats.MACAddress = generateRandomMacAddress();
        apStats.RXTimeFractionIBSS = (short) random.nextInt(Short.MAX_VALUE + 1);
        apStats.RXTimeFractionOBSS = (short) random.nextInt(Short.MAX_VALUE + 1);
        apStats.TXTimeFraction = (short) random.nextInt(Short.MAX_VALUE + 1);
        return apStats;
    }

    private AssociatedStationStats[] getAssociatedStationStatsArray()
    {
        int length = random.nextInt(5);
        AssociatedStationStats[] associatedStationStatsArray = new AssociatedStationStats[length];
        for (int i = 0; i < length; i++) {
            associatedStationStatsArray[i] = getRandomAssociatedStationStats();
        }
        return associatedStationStatsArray;
    }

    private AssociatedStationStats getRandomAssociatedStationStats()
    {
        AssociatedStationStats assStationStats = new AssociatedStationStats();
        assStationStats.avgSpatialStreamsRX = (short) random.nextInt(Short.MAX_VALUE + 1);
        assStationStats.avgSpatialStreamsTX = (short) random.nextInt(Short.MAX_VALUE + 1);
        assStationStats.avgUsedBandwidthRX = (short) random.nextInt(Short.MAX_VALUE + 1);
        assStationStats.avgUsedBandwidthTX = (short) random.nextInt(Short.MAX_VALUE + 1);
        assStationStats.dataRateRX = random.nextInt();
        assStationStats.dataRateTX = random.nextInt();
        assStationStats.MACAddress = generateRandomMacAddress();
        assStationStats.powerSaveTimeFraction = (short) random.nextInt(Short.MAX_VALUE + 1);
        assStationStats.RSSI = random.nextInt();
        assStationStats.trainedPhyRateRX = random.nextInt();
        assStationStats.trainedPhyRateTX = random.nextInt();
        return assStationStats;
    }

    private STAStats[] getRandomSTAStatsArray()
    {
        int length = random.nextInt(25);
        STAStats[] staStatsArray = new STAStats[length];
        for (int i = 0; i < length; i++) {
            staStatsArray[i] = getRandomSTAStats();
        }
        return staStatsArray;
    }

    private STAStats getRandomSTAStats()
    {
        STAStats staStats = new STAStats();
        staStats.MACAddress = generateRandomMacAddress();
        staStats.RSSI = random.nextInt();
        return staStats;
    }

    private TestRequest getTestRequest()
    {
        TestRequest testRequest = new TestRequest();
        testRequest.destinationMAC = generateRandomMacAddress();
        testRequest.duration = random.nextInt();
        testRequest.id = random.nextInt();
        testRequest.packetSize = (short) random.nextInt(Short.MAX_VALUE + 1);
        testRequest.sourceMAC = generateRandomMacAddress();
        testRequest.type = random.nextInt();
        testRequest.WMMClass = (short) random.nextInt(Short.MAX_VALUE + 1);
        return testRequest;
    }

    private Interface getInterface()
    {
        Interface inter = new Interface();
        inter.enabled = random.nextBoolean();
        inter.MACAddress = generateRandomMacAddress();
        inter.radio = getRandomUuid();
        inter.SSID = "SSID_" + random.nextInt();
        inter.type = random.nextInt();
        return inter;
    }

    private Radio getRadio()
    {
        Radio radio = new Radio();
        radio.capabilities = "Capability_" + random.nextInt();
        radio.channel = random.nextInt();
        radio.device = getRandomDeviceId();
        radio.id = getRandomUuid();
        return radio;
    }

    /**
     * More generic helper functions.
     * 
     */

    private UUID getRandomUuid()
    {
        UUID myUuid = new UUID();
        myUuid.lower = random.nextLong();
        myUuid.upper = random.nextLong();
        return myUuid;
    }

    private DeviceId getRandomDeviceId()
    {
        DeviceId myDeviceId = new DeviceId();
        myDeviceId.lower = random.nextLong();
        myDeviceId.upper = random.nextLong();
        return myDeviceId;
    }

    private String generateRandomMacAddress()
    {
        String macAddress =
            random.nextInt(9) + random.nextInt(9) + ":" + random.nextInt(9) + random.nextInt(9) + ":"
                + random.nextInt(9) + random.nextInt(9) + ":" + random.nextInt(9) + random.nextInt(9) + ":"
                + random.nextInt(9) + random.nextInt(9) + ":" + random.nextInt(9) + random.nextInt(9);

        return macAddress;
    }
}
