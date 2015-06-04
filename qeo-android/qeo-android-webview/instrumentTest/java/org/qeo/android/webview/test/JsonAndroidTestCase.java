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

package org.qeo.android.webview.test;

import java.io.InputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.qeo.QeoFactory;
import org.qeo.android.QeoAndroid;
import org.qeo.android.QeoConnectionListener;
import org.qeo.android.internal.QeoConnection;
import org.qeo.exception.QeoException;
import org.qeo.json.JsonManifestParser;
import org.qeo.json.QeoFactoryJSON;
import org.qeo.json.QeoJSON;

import android.content.Context;
import android.os.Looper;
import android.test.AndroidTestCase;
import android.util.Log;

public abstract class JsonAndroidTestCase
    extends AndroidTestCase
{
    /** timeout for Qeo init. */
    private static final int QEO_INIT_TIMEOUT = 15;
    private static Semaphore syncSem = new Semaphore(0);
    protected QeoFactoryJSON qeoJson;
    protected QeoFactory mQeo = null;
    protected Context ctx = null;
    private static final String TAG = "QeoJunit";
    private static boolean qeoDirty = false;
    private QeoConnectionListener mQeoConnection = null;

    /**
     * scale factor that can be used in some tests as native java is much faster than Android
     */
    protected int scaleFactor = 1;

    @Override
    public void setUp()
        throws Exception
    {
        setUp(true);
    }

    public void setUp(boolean initQeo)
        throws Exception
    {
        super.setUp();
        log("setup");
        for (int i = 0; i < 10; i++) {
            // On Android 4 this call may not immediately be available. So check if the result is valid.
            ctx = getContext().getApplicationContext();
            if (ctx != null) {
                break;
            }
            log("Application context is not yet available, sleeping...");
            Thread.sleep(100);
        }
        if (ctx == null) {
            throw new IllegalStateException("Application context not available");
        }

        if (initQeo) {
            if (qeoDirty) {
                fail("A previous test already caused a connection problem to the factory, not retrying");
            }
            log("Binding to service");

            // disable manifest popup
            QeoConnection.disableManifestPopup();

            // use manifest in json format
            InputStream stream = ctx.getResources().getAssets().open("QeoManifest.json");
            QeoConnection.setManifest(JsonManifestParser.getManifest(stream));

            mQeoConnection = new QeoConnectionListener() {
                @Override
                public void onQeoReady(QeoFactory qeo)
                {
                    log("Binding to service done");
                    mQeo = qeo;
                    syncSem.release();
                }

                @Override
                public void onQeoClosed(QeoFactory qeo)
                {
                    log("Got an onQeoClosed!");
                    qeoDirty = true;
                }

                @Override
                public void onQeoError(QeoException ex)
                {
                    fail("Exception: " + ex);
                }
            };
            QeoAndroid.initQeo(ctx, mQeoConnection, Looper.getMainLooper());
            // fail if the service does not get initialized within 10 seconds
            if (!syncSem.tryAcquire(QEO_INIT_TIMEOUT, TimeUnit.SECONDS)) {
                qeoDirty = true;
                fail("Could not create Android factory. Most likely the service crashed. Please check logcat.");
            }
            if (qeoDirty) {
                fail("Got an onQeoClosed and an automatic re-bind. This propably means the service has crashed."
                    + " Please check logcat.");

            }
            qeoJson = QeoJSON.getFactory(mQeo);
        }
    }

    @Override
    public void tearDown()
        throws Exception
    {
        super.tearDown();
        QeoAndroid.closeQeo(mQeoConnection);
    }

    protected static synchronized void log(String msg)
    {
        Log.d(TAG, msg);
    }

    private static boolean verbose = false;
    protected int timeout = 10; // default 10 seconds

    protected void waitForData(Semaphore sem, int samples)
        throws InterruptedException
    {
        if (!sem.tryAcquire(samples, timeout, TimeUnit.SECONDS)) {
            fail("Semaphore lock timeout");
        }
    }

    protected void waitForData(Semaphore sem)
        throws InterruptedException
    {
        waitForData(sem, 1);
    }

    public static void println(String msg)
    {
        if (verbose) {
            log(msg);
        }
    }

    public static synchronized void setVerbose(boolean v)
    {
        verbose = v;
    }
}
