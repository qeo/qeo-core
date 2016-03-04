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

package org.qeo.android.webview.testactivity;

import org.qeo.android.webview.QeoWebview;
import org.qeo.android.webview.test.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.webkit.WebView;

/**
 * Dummy activity to load a webview for testing.
 */
public class WebviewActivity
    extends Activity
{
    private WebView mWebview;
    private QeoWebview mJsInf;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        mWebview = (WebView) findViewById(R.id.webView1);
        // enable javascript to be able to load the Qeo javascript bindings
        mWebview.getSettings().setJavaScriptEnabled(true);
        mJsInf = QeoWebview.enableQeo(getApplicationContext(), mWebview);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.webview, menu);
        return true;
    }

    @Override
    protected void onDestroy()
    {
        if (mJsInf != null) {
            mJsInf.close();
        }
    }

    /**
     * Execute qeo cleanup.
     */
    public void cleanup()
    {
        if (mJsInf != null) {
            mJsInf.cleanup();
        }
    }

}
