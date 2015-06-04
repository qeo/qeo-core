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

package org.qeo.android.webview;

import org.qeo.android.webview.internal.QeoWebviewImpl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.webkit.WebView;

/**
 * Class to hook Qeo javascript functions into an Android WebView.
 */

public abstract class QeoWebview
{
    /**
     * Create an instance.
     */
    protected QeoWebview()
    {
    }

    /**
     * Add qeo javascript hooks to an Android WebView.<br>
     * This will enable javascript support on the given webview.
     * 
     * @param context The Android application context.
     * @param webview The webview where the javascript should be attached.
     * @return the instance.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static QeoWebview enableQeo(Context context, WebView webview)
    {
        webview.getSettings().setJavaScriptEnabled(true);
        return new QeoWebviewImpl(context, webview);
    }

    /**
     * Close all resources allocated. This QeoWebview will not be usable anymore after this call.
     */
    public abstract void close();

    /**
     * Cleanup all resources (closing readers/writer/factories), but keep this QeoWebview usable to create new
     * factories.
     */
    public abstract void cleanup();

}
