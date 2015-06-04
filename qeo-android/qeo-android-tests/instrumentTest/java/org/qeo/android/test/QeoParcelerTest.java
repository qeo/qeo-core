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

package org.qeo.android.test;

import java.util.Arrays;
import java.util.Random;

import org.qeo.android.internal.ParcelableData;
import org.qeo.android.internal.ParcelableException;
import org.qeo.android.internal.QeoParceler;
import org.qeo.android.internal.QeoParceler.JoinCallbacks;
import org.qeo.android.internal.QeoParceler.SplitCallbacks;
import org.qeo.internal.common.Data;
import org.qeo.internal.common.ObjectData;
import org.qeo.internal.common.PrimitiveArrayData;

import android.os.RemoteException;
import android.test.AndroidTestCase;

/**
 * Test for testing parceling on a low level.
 */
public class QeoParcelerTest
    extends AndroidTestCase
{
    private ParcelableData pdCreated;
    private int numCreated;
    private int numFragments;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        pdCreated = null;
        numCreated = 0;
        numFragments = 0;
    }

    private void parcelTest(int sizeInBytes, int fragmentsExcepted)
        throws Exception
    {
        final int parcelId = 555;
        final int arrayKey = 111;

        final QeoParceler qeoParcelerJoin = new QeoParceler(new JoinCallbacks() {

            @Override
            public void onFragmentsJoined(int id, ParcelableData pd)
            {
                assertEquals(parcelId, id);
                pdCreated = pd;
                numCreated++;
            }
        });
        final QeoParceler qeoParcelerSplit = new QeoParceler(new SplitCallbacks() {

            @Override
            public void onWriteFragment(int id, boolean firstBlock, boolean lastBlock, int totalSize, byte[] data,
                ParcelableException exception)
                throws RemoteException
            {
                assertEquals(parcelId, id);
                numFragments++;
                // redirect to joiner
                qeoParcelerJoin.join(id, firstBlock, lastBlock, totalSize, data);
            }
        });

        byte[] byteArray = new byte[sizeInBytes]; // 1kb
        Random r = new Random();
        r.nextBytes(byteArray);
        PrimitiveArrayData pad = new PrimitiveArrayData(arrayKey, byteArray);
        ObjectData od = new ObjectData(123);
        od.addMember(pad);
        pad = null; // free mem

        qeoParcelerSplit.split(parcelId, od);
        od = null; // free mem

        assertEquals(1, numCreated);
        assertEquals(fragmentsExcepted, numFragments);
        assertNotNull(pdCreated);
        Data d = pdCreated.getData().getMembers().get(arrayKey);
        assertNotNull(d);
        assertTrue(d instanceof PrimitiveArrayData);
        byte[] byteArry2 = (byte[]) ((PrimitiveArrayData) d).getValue();
        assertTrue(Arrays.equals(byteArray, byteArry2));
    }

    public void testSmall()
        throws Exception
    {
        parcelTest(1024, 1); // 1kb, not parceling
    }

    public void testBigger()
        throws Exception
    {
        // QeoAndroid.setLogLevel(Level.ALL);
        parcelTest(200 * 1024, 2); // 200kb, should be 2 parcels
    }

    public void testBiggest()
        throws Exception
    {
        long maxMem = Runtime.getRuntime().maxMemory();
        if (maxMem > 16 * 1024 * 1024) {
            // only run on devices with a max heap bigger than 16MB, will not work otherwise
            parcelTest(2000 * 1024, 16); // 2000Kb, should be 16 parcels (in theory 15, but there's overhead)
        }
    }
}
