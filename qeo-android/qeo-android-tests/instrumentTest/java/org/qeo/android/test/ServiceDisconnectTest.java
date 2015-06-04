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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.qeo.EventWriter;
import org.qeo.QeoFactory;
import org.qeo.android.QeoAndroid;
import org.qeo.android.QeoConnectionListener;
import org.qeo.android.internal.QeoConnection;
import org.qeo.android.internal.ServiceConnection;
import org.qeo.exception.QeoException;
import org.qeo.testframework.QeoTestCase;
import org.qeo.unittesttypes.TestTypes;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Looper;
import android.os.Process;

/**
 * Testcase to check if everything gets cleaned if service connection is closed or aborted.
 */
public class ServiceDisconnectTest
    extends QeoTestCase
{

    private QeoFactory qeo1 = null;

    private Semaphore syncSem1;
    private Semaphore syncSemClosed1;
    private QeoConnection qeoConnection;
    private ServiceConnection sc;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp(false); // do not init qeo
        syncSem1 = new Semaphore(0);
        syncSemClosed1 = new Semaphore(0);
    }

    private final QeoConnectionListener listener1 = new QeoConnectionListener() {
        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            qeo1 = qeo;
            assertNotNull(qeo1);
            syncSem1.release();
        }

        @Override
        public void onQeoError(QeoException ex)
        {
            fail("Qeo Error" + ex);
        }

        @Override
        public void onQeoClosed(QeoFactory factory)
        {
            assertNotNull(factory);
            assertEquals(qeo1, factory);
            syncSemClosed1.release();
        }
    };

    // verify that qeoconnection is up and running
    private void checkAllWorking(QeoFactory factory)
        throws Exception
    {
        boolean serviceConnected = (Boolean) fServiceConnected.get(sc);
        // checks before connection is closed
        assertNotNull(qeoConnection.getProxy());
        assertTrue(serviceConnected);
    }

    private void checkAllClosed(QeoFactory factory)
        throws Exception
    {
        boolean serviceConnected = (Boolean) fServiceConnected.get(sc);
        assertFalse(serviceConnected);

        Field fServiceListeners = ServiceConnection.class.getDeclaredField("mListeners");
        fServiceListeners.setAccessible(true);
        Set<?> listeners = (Set<?>) fServiceListeners.get(sc);
        assertEquals(0, listeners.size());

        Field fConnectionFactories = QeoConnection.class.getDeclaredField("mFactories");
        fConnectionFactories.setAccessible(true);
        Map<?, ?> factories = (Map<?, ?>) fConnectionFactories.get(qeoConnection);
        assertEquals(0, factories.size());
    }

    private Field fServiceConnected;

    private void getPrivateFields()
        throws Exception
    {
        // store for later user
        qeoConnection = QeoConnection.getInstance();
        assertNotNull(qeoConnection);

        // do some reflection magic to get some private fields
        Method m1 = ServiceConnection.class.getDeclaredMethod("getInstance", Context.class);
        m1.setAccessible(true);
        sc = (ServiceConnection) m1.invoke(null, new Object[] {mCtx});
        assertNotNull(sc);
        fServiceConnected = ServiceConnection.class.getDeclaredField("mConnected");
        fServiceConnected.setAccessible(true);
    }

    // Test connection status by closing qeo factory normally
    public void testConnectionClosed()
        throws Exception
    {
        QeoAndroid.initQeo(mCtx, listener1, Looper.getMainLooper());
        waitForData(syncSem1);

        getPrivateFields();

        checkAllWorking(qeo1);

        // close normally
        QeoAndroid.closeQeo(listener1);

        // not NOT have the onQeoClosed
        Thread.sleep(100);
        assertEquals(0, syncSemClosed1.availablePermits());

        // checks after connection is closed
        checkAllClosed(qeo1);

        QeoFactory oldFactory = qeo1;
        // create a new connection to see if that works
        QeoAndroid.initQeo(mCtx, listener1, Looper.getMainLooper());
        waitForData(syncSem1);

        // check that current instances are still closed
        checkAllClosed(oldFactory);

        // get new object data
        getPrivateFields();
        checkAllWorking(qeo1);

        // try to create writer and write something, should not give any exception
        EventWriter<TestTypes> ew = qeo1.createEventWriter(TestTypes.class);
        ew.write(new TestTypes());
        ew.close();

        // close factory
        QeoAndroid.closeQeo(listener1);

        // check if this also closes property qeo Android connection
        checkAllClosed(qeo1);
        assertEquals(0, syncSemClosed1.availablePermits());

    }

    // test connection status by simulating kill of Android service
    public void testConnectionAborted()
        throws Exception
    {
        if (Helper.isEmbedded()) {
            return; // not supported on embedded service
        }
        QeoAndroid.initQeo(mCtx, listener1, Looper.getMainLooper());
        waitForData(syncSem1);

        getPrivateFields();

        checkAllWorking(qeo1);

        // Kill the service. Should work as we have the same UID
        ActivityManager activityManager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        boolean processFound = false;
        for (ActivityManager.RunningAppProcessInfo pap : activityManager.getRunningAppProcesses()) {
            if (pap.processName.equals(QeoAndroid.QEO_SERVICE_PACKAGE)) {
                Process.killProcess(pap.pid);
                processFound = true;
                break;
            }
        }
        assertTrue(processFound);

        // should have gotten onQeoClosed
        waitForData(syncSemClosed1);

        // checks after connection is closed
        checkAllClosed(qeo1);

        QeoFactory oldFactory = qeo1;
        // create a new connection to see if that works
        QeoAndroid.initQeo(mCtx, listener1, Looper.getMainLooper());
        waitForData(syncSem1);

        // check that current instances are still closed
        checkAllClosed(oldFactory);

        // get new object data
        getPrivateFields();
        checkAllWorking(qeo1);

        // try to create writer and write something, should not give any exception
        EventWriter<TestTypes> ew = qeo1.createEventWriter(TestTypes.class);
        ew.write(new TestTypes());
        ew.close();

        // close factory
        QeoAndroid.closeQeo(listener1);

        // check if this also closes property qeo Android connection
        checkAllClosed(qeo1);

    }

}
