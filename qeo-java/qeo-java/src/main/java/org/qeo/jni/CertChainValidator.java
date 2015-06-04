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

package org.qeo.jni;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * A class to validate certificate chains.
 */
public final class CertChainValidator
{
    private static final Logger LOG = Logger.getLogger("CertChainValidator");
    private static final TrustManager[] TRUSTMANAGERS;

    static {
        TrustManager[] mngrs = null;
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ks = null;
            tmf.init(ks);
            mngrs = tmf.getTrustManagers();
        }
        catch (Throwable t) {
            LOG.log(Level.SEVERE, "Error setting trustmanager", t);
        }
        TRUSTMANAGERS = mngrs;
    }

    private CertChainValidator()
    {
    }

    /**
     * This method validates the certificate chain.
     * 
     * Do not rename, as this will be called from JNI !!!
     * 
     * @param rawCertificates a non-null array containing the raw data of the certificates.
     * @throws Exception in case of failure or if certificate chain can't be validated.
     */
    @NativeCallback
    public static void validateCertificateChain(byte[][] rawCertificates)
        throws Exception
    {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate[] chain = new X509Certificate[rawCertificates.length];
        for (int i = 0; i < rawCertificates.length; i++) {
            chain[i] = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(rawCertificates[i]));
        }
        // Key usage: Digital Signature 0, Key Encipherment 2, Data Encipherment 3, Key Agreement 4
        boolean[] usage = chain[0].getKeyUsage();
        if (!(usage[0] & usage[2])) {
            throw new CertificateException("Invalid keyUsage DS=" + usage[0] + ", KE=" + usage[2]);
        }
        // Extended: TLS Web Server Authentication (1.3.6.1.5.5.7.3.1)
        if (!chain[0].getExtendedKeyUsage().contains("1.3.6.1.5.5.7.3.1")) {
            throw new CertificateException("Certificate is not suited as TLS Web Server");
        }
        validateChain(chain);
    }

    private static synchronized void validateChain(X509Certificate[] chain)
        throws Exception
    {
        if (TRUSTMANAGERS == null || TRUSTMANAGERS.length == 0) {
            throw new SecurityException("No default Trustmanager found.");
        }
        for (TrustManager tManager : TRUSTMANAGERS) {
            X509TrustManager xtm = (X509TrustManager) tManager;
            xtm.checkServerTrusted(chain, "RSA");
        }
    }
}
