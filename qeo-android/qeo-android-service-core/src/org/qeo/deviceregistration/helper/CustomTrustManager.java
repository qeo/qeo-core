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

package org.qeo.deviceregistration.helper;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import org.qeo.android.service.core.BuildConfig;

import android.annotation.SuppressLint;

/**
 * Install a custom truststore manager that accepts self-signed certificates.<br>
 * This is only needed for using qeodev.org<br>
 * The TrulyRandom lint check can be ignored because of PRNGFixes class.
 */
@SuppressLint("TrulyRandom")
public final class CustomTrustManager
{
    private static final Logger LOG = Logger.getLogger("CustomTrustManager");

    private CustomTrustManager()
    {
    }

    /**
     * install the trust manager.<br>
     * <b>WARNING: this is not allowed in a release build and will hence only work in debug builds</b>
     */
    public static void install()
    {
        if (!BuildConfig.DEBUG) {
            LOG.warning("Trying to apply custom trust manager in release build, this is not allowed");
            return;
        }
        LOG.warning("Applying custom trust manager");

        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers()
            {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType)
            {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType)
            {
            }
        }};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        }
        catch (GeneralSecurityException e) {
            System.err.print(e);
        }
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String arg0, SSLSession session)
            {
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
    }
}
