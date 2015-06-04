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

/**
 * All security, authentication, encryption related functionality that needs to be executed in native code (qeo-c).
 */
public class NativeSecurity
{
    private boolean mNoSecurity = false;

    /**
     * Set no security.
     * 
     * @param noSecurity true if security needs to be disabled
     */
    public void noSecurity(boolean noSecurity)
    {
        mNoSecurity = noSecurity;
        if (noSecurity) {
            NativeError.checkError(nativeNoSecurity());
        }
    }

    /**
     * Encrypt an OTC using a public key.
     * 
     * @param otc the OTC to be encrypted
     * @param key the public key to encrypt the OTC
     * @return the encrypted OTC
     */
    public byte[] encryptOtc(String otc, String key)
    {
        return nativeEncryptOtc(otc, key);
    }

    /**
     * Get the public key from native. This is only possible during when onStartOtpRetrieval is called.
     * 
     * @return The public key (PEM format)
     */
    public static String getPublicKey()
    {
        return nativeGetPublicKey();
    }

    /**
     * Decrypt an OTC. This is only possible during when onStartOtpRetrieval is called.
     * 
     * @param encryptedOtc encrypted otc.
     * @return the decrypted otc.
     */
    public static String decryptOtc(byte[] encryptedOtc)
    {
        return nativeDecryptOtc(encryptedOtc);
    }

    private static native int nativeNoSecurity();

    private static native byte[] nativeEncryptOtc(String otc, String key);

    private static native String nativeDecryptOtc(byte[] encryptedOtc);

    private static native String nativeGetPublicKey();

}
