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

package org.qeo.android.webview.test.qunit;

import org.json.JSONObject;
import org.qeo.android.internal.QeoConnection;
import org.qeo.android.webview.test.R;
import org.qeo.android.webview.test.qunit.Qunit.QunitListener;
import org.qeo.android.webview.test.qunit.Qunit.TestResult;
import org.qeo.android.webview.testactivity.WebviewActivity;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

/**
 * 
 */
public class QunitTest
    extends android.test.ActivityInstrumentationTestCase2<WebviewActivity>
{
    private WebviewActivity mActivity;
    private WebView mWebview;
    private Handler handler;
    private static final String TAG = "QunitTest";
    private Qunit qunit;

    public QunitTest()
    {
        super(WebviewActivity.class);
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        setActivityInitialTouchMode(false);
        QeoConnection.disableManifestPopup();
        mActivity = getActivity();
        handler = new Handler(Looper.getMainLooper());

        mWebview = (WebView) mActivity.findViewById(R.id.webView1);
        // set chromeclient to handle console logging
        handler.post(new Runnable() {
            @Override
            public void run()
            {
                mWebview.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public boolean onConsoleMessage(ConsoleMessage cm)
                    {
                        Log.d(TAG, cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
                        return true;
                    }
                });
            }
        });
    }

    public void testQunit()
        throws InterruptedException
    {
        // QeoAndroid.setLogLevel(Level.ALL);
        qunit = new Qunit(new Listener());
        handler.post(new Runnable() {

            @Override
            public void run()
            {
                Log.d(TAG, "Starting Qunit tests");
                mWebview.addJavascriptInterface(qunit, "TestReporter");
                mWebview.loadUrl("file:///android_asset/qunit.html");
            }
        });

        // give each test 30 seconds to start
        while (qunit.waitForStartOrCompleted(30)) {
            // if test got started, give it 60 seconds to complete
            qunit.waitForTestDone(60);
        }

        // give qunit 5 minutes to complete
        // normally qunit aborts every test after 20 seconds, but to be sure...
        // qunit.waitForDone(60 * 5);
        // Thread.sleep(10000); //add this to see qunit html output on an emulator/device

        // print results
        for (TestResult result : qunit.getResults()) {
            StringBuilder msg = new StringBuilder();
            msg.append("Test: ").append(result.getName());
            if (result.getFailed() != 0) {
                msg.append(" FAILED: " + result.getFailed());
            }
            else {
                msg.append(" OK");
            }
            msg.append(" Duration: ").append(result.getDuration());
            Log.i(TAG, msg.toString());
        }

        assertEquals("1 or more QUnit tests failed. Please take a look at logcat for more details", 0,
            qunit.getFailed());

    }

    private class Listener
        implements QunitListener
    {

        @Override
        public void testDone(JSONObject details)
        {
            mActivity.cleanup(); // qeo cleanup at end of test
        }

    }
}
