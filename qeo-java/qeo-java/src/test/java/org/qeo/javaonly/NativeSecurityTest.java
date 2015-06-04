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

package org.qeo.javaonly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.qeo.jni.NativeQeo;
import org.qeo.jni.NativeSecurity;
import org.qeo.testframework.QeoTestCase;

/**
 * Tests for native security. Test is not running on android since the private key file is not available there.
 */
public class NativeSecurityTest
    extends QeoTestCase
{

    private static final File KEY_DIR = new File("src/test/data/rsakey");

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp(false);
    }

    private String readKey(File key)
        throws IOException
    {
        BufferedReader br = null;
        StringBuilder builder = new StringBuilder();
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(key), Charset.forName("utf-8")));
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                builder.append(line).append("\n");
            }
        }
        finally {
            if (br != null) {
                br.close();
            }
        }
        return builder.toString();
    }

    private String readPubKey()
        throws IOException
    {
        return readKey(new File(KEY_DIR, "key.pub"));
    }

    public void testNativeEncryptOtc()
        throws IOException
    {
        String otc = "12345678";
        String pubKey = readPubKey();
        NativeSecurity nativeSec = NativeQeo.getNativeSecurity();
        byte[] out = nativeSec.encryptOtc(otc, pubKey);
        assertNotNull(out);
        assertEquals(128, out.length); // will always be equal to keysize

        // can't validate it as the decrypt function don't provide jni interface

    }

    public void testNativeDecryptOtc()
    {
        String a = NativeSecurity.decryptOtc(new byte[] {1});
        // can only do the decrypt during init of security. Should return null otherwise.
        assertNull(a);
    }
}
