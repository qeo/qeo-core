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

import org.qeo.testframework.QeoTestCase;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import static org.qeo.jni.CertChainValidator.validateCertificateChain;


/**
 *
 */
public class CertChainValidatorTest
    extends QeoTestCase
{
    //private byte[] USERTRUST_ROOT;
    //private byte[] PROJECTQ;
    private byte[] GANDI;
    private byte[] QEO;
    private byte[] ENTRUST_LC1;
    private byte[] ENTRUST_ROOT;

    @Override
    public void setUp() throws Exception
    {
        super.setUp(false);
        //PROJECTQ = readFile(new File("src/test/data/certs/projectq_be.der"));
        GANDI = readFile("certs/GandiStandardSSLCA2.der");
        //USERTRUST_ROOT = readFile(new File("src/test/data/certs/USERTrustRSACertificationAuthority.der"));
        QEO = readFile("certs/qeo_org.der");
        ENTRUST_LC1 = readFile("certs/EntrustCertificationAuthority-L1C.der");
        ENTRUST_ROOT = readFile("certs/Entrust_root.der");
    }

    private byte[] readFile(String f) throws IOException
    {
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(openAsset(f));
            byte[] b = new byte[0];
            do {
                int avail = dis.available();
                if (avail <= 0) {
                    break;
                }
                int oldSize = b.length;
                b = Arrays.copyOf(b, oldSize + avail);
                dis.readFully(b, oldSize, avail);
            } while (true);
            return b;
        }
        finally {
            if (dis != null) {
                dis.close();
            }
        }
    }

    public void testValidateQeoCertificateChain()
        throws Exception
    {
        validateCertificateChain(new byte[][]{QEO, ENTRUST_LC1});
        validateCertificateChain(new byte[][]{QEO, ENTRUST_LC1, ENTRUST_ROOT});
    }

    public void testLoopValidQeo()
        throws Exception
    {
        for (int i = 0; i < 25; i++) {
            validateCertificateChain(new byte[][]{QEO, ENTRUST_LC1, ENTRUST_ROOT});
        }
    }

    //Projectq tests are currently disabled. The certificate chain changed and this should be fixed.
//    public void testValidateProjectQCertificateChain()
//        throws Exception
//    {
//        validateCertificateChain(new byte[][] {PROJECTQ, GANDI});
//    }
//
//    public static void testLoopValidPRojectQ()
//        throws Exception
//    {
//        for (int i = 0; i < 25; i++) {
//            validateCertificateChain(new byte[][] {PROJECTQ, GANDI});
//        }
//    }
    public void testInvalidCertificateChain()
        throws Exception
    {
        try {
            validateCertificateChain(new byte[][]{QEO, GANDI});
            fail("should trow validation exception");
        }
        catch (CertificateException e) {
            // expected
        }
    }

    public void testAloneCertificate()
        throws Exception
    {
        try {
            validateCertificateChain(new byte[][]{QEO});
            fail("should trow validation exception");
        }
        catch (CertificateException e) {
            // expected
        }
    }

    public void testNonSSLCertificate()
        throws Exception
    {
        try {
            validateCertificateChain(new byte[][]{GANDI});
            fail("should trow validation exception");
        }
        catch (CertificateException e) {
            // expected
        }
    }

    // tests with the provided cert array doesn't match the trust chain has been removed.
    // e.g. QEO, GANDI, ETRUST or QEO, ETRUST, GANDI
    // as the behavior change between Java 6 and Java 7.
    // Probably to be able to handle non linear trust chain (some certificate can be signed by multiple ca).

    public static void main(String[] args)
        throws Exception
    {
        FileInputStream in = new FileInputStream("<path-to-der-file>");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int rd;
        byte[] bytes = new byte[1024];
        while (-1 != (rd = in.read(bytes))) {
            baos.write(bytes, 0, rd);
        }
        in.close();
        System.out.println(Arrays.toString(baos.toByteArray()));
    }
}
