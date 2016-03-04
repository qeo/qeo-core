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

package org.qeo.jni;

import org.qeo.testframework.QeoTestCase;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.logging.Logger;

import static org.qeo.jni.CertChainValidator.validateCertificateChain;


/**
 *
 */
public class CertChainValidatorTest extends QeoTestCase
{
    private static final Logger LOG = Logger.getLogger("CertChainValidatorTest");
    //private byte[] USERTRUST_ROOT;
    //private byte[] PROJECTQ;
    private byte[] GANDI;
    private byte[] QEO;
    private byte[] ENTRUST_L1K;
    private byte[] ENTRUST_L1K_XCERT;
    private byte[] ENTRUST_ROOT;

    @Override
    public void setUp() throws Exception
    {
        super.setUp(false);
        //PROJECTQ = readFile(new File("src/test/data/certs/projectq_be.der"));
        GANDI = readFile("certs/GandiStandardSSLCA2.der");
        //USERTRUST_ROOT = readFile(new File("src/test/data/certs/USERTrustRSACertificationAuthority.der"));
        QEO = readFile("certs/qeo_org.der");
        //L1K cert is signed by G2 root. However this root is rather new and still unknown by certain jdk/jvms
        //That's why it's also cross-signed by the L1K_XCERT which is then signed by their old root.
        //info: http://www.entrust.net/knowledge-base/technote.cfm?tn=8863
        ENTRUST_L1K = readFile("certs/EntrustCertificationAuthority-L1K.der");
        ENTRUST_L1K_XCERT = readFile("certs/L1K-2048-Xcert_sha256.der");
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
            assertTrue(b.length > 100); //check that there is some data in here.
            return b;
        }
        finally {
            if (dis != null) {
                dis.close();
            }
        }
    }

    public void testValidateQeoCertificateChain() throws Exception
    {
        validateCertificateChain(new byte[][]{QEO, ENTRUST_L1K, ENTRUST_L1K_XCERT});
        validateCertificateChain(new byte[][]{QEO, ENTRUST_L1K, ENTRUST_L1K_XCERT, ENTRUST_ROOT});
    }

    public void testLoopValidQeo() throws Exception
    {
        for (int i = 0; i < 25; i++) {
            validateCertificateChain(new byte[][]{QEO, ENTRUST_L1K, ENTRUST_L1K_XCERT, ENTRUST_ROOT});
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
    public void testInvalidCertificateChain() throws Exception
    {
        try {
            validateCertificateChain(new byte[][]{QEO, GANDI});
            fail("should trow validation exception");
        }
        catch (CertificateException e) {
            // expected
        }
    }

    public void testAloneCertificate() throws Exception
    {
        try {
            validateCertificateChain(new byte[][]{QEO});
            fail("should trow validation exception");
        }
        catch (CertificateException e) {
            // expected
        }
    }

    public void testNonSSLCertificate() throws Exception
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

    public static void main(String[] args) throws Exception
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
