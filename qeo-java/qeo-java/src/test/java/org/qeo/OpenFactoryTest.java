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

package org.qeo;

import java.util.Iterator;
import java.util.Random;

import org.qeo.exception.QeoException;
import org.qeo.system.DeviceId;
import org.qeo.system.RegistrationCredentials;
import org.qeo.system.RegistrationRequest;
import org.qeo.testframework.QeoConnectionTestListener;
import org.qeo.testframework.QeoTestCase;

public class OpenFactoryTest
    extends QeoTestCase
{

    private static final String PUBLIC_RSA_KEY = "-----BEGIN RSA PUBLIC KEY-----\n"
        + "MIGJAoGBAJNrHWRFgWLqgzSmLBq2G89exgi/Jk1NWhbFB9gHc9MLORmP3BOCJS9k\n"
        + "onzT/+Dk1hdZf00JGgZeuJGoXK9PX3CIKQKRQRHpi5e1vmOCrmHN5VMOxGO4d+zn\n"
        + "JDEbNHODZR4HzsSdpQ9SGMSx7raJJedEIbr0IP6DgnWgiA7R1mUdAgMBAAE=\n" + "-----END RSA PUBLIC KEY-----";
    private static final String URL = "http://join.qeodev.org:8080/";
    private final Random random = new Random();

    private QeoFactory mQeoOpen;
    private QeoConnectionTestListener mQeoReadyListener;
    private StateWriter<RegistrationCredentials> regCredWriter;
    private StateWriter<RegistrationRequest> regReqWriter;
    private StateReader<RegistrationCredentials> regCredReader;
    private StateReader<RegistrationRequest> regReqReader;
    private RegCredStateReaderListener regCredListener;
    private RegReqStateReaderListener regReqListener;

    private static class RegCredStateReaderListener
        implements StateReaderListener
    {
        @Override
        public void onUpdate()
        {
        }
    };

    private static class RegReqStateReaderListener
        implements StateReaderListener
    {
        @Override
        public void onUpdate()
        {
        }
    };

    @Override
    public void setUp(boolean initQeo)
        throws Exception
    {
        super.setUp(false);
        println("setup");
        /* create an Open Domain Factory */
        mQeoReadyListener = createFactory(getFactoryOpenId());
        mQeoOpen = mQeoReadyListener.getFactory();
        /* create two state readers on the allowed topics */
        regCredListener = new RegCredStateReaderListener();
        regCredReader = mQeoOpen.createStateReader(RegistrationCredentials.class, regCredListener);
        regReqListener = new RegReqStateReaderListener();
        regReqReader = mQeoOpen.createStateReader(RegistrationRequest.class, regReqListener);
        assertNotNull(regCredReader);
        assertNotNull(regReqReader);
        // sanity checks: no data should be present
        assertFalse(regCredReader.iterator().hasNext());
        assertFalse(regReqReader.iterator().hasNext());
        /* create two state writers on the allowed topics */
        regCredWriter = mQeoOpen.createStateWriter(RegistrationCredentials.class);
        regReqWriter = mQeoOpen.createStateWriter(RegistrationRequest.class);
        assertNotNull(regCredWriter);
        assertNotNull(regReqWriter);
    }

    @Override
    public void tearDown()
        throws Exception
    {
        println("tearDown");
        if (regCredReader != null) {
            // no more data should be present anymore, it may harm other tests
            if (regCredReader.iterator().hasNext()) {
                System.err.println("WARNING: not all RegistrationCredentials samples are properly removed: "
                    + regCredReader.iterator().next());
            }
            regCredReader.close();
        }
        if (regReqReader != null) {
            // no more data should be present anymore, it may harm other tests
            if (regReqReader.iterator().hasNext()) {
                System.err.println("WARNING: not all RegistrationRequest samples are properly removed: "
                    + regReqReader.iterator().next());
            }
            regReqReader.close();
        }
        if (regCredWriter != null) {
            regCredWriter.close();
        }
        if (regReqWriter != null) {
            regReqWriter.close();
        }
        closeFactory(mQeoReadyListener);
        super.tearDown();
    }

    /**
     * Create/write/read/remove topic RegistrationCredentials on Open Domain.
     * 
     * @throws QeoException
     */
    public void testRegistrationCredentialTopic()
        throws QeoException
    {
        println("testRegistrationCredentialTopic");
        RegistrationCredentials rc1 = getRegistrationCredentials();
        regCredWriter.write(rc1);
        Iterator<RegistrationCredentials> regCredIt = regCredReader.iterator();
        assertTrue(regCredIt.hasNext());
        RegistrationCredentials rc2 = regCredIt.next();
        assertEquals(rc1, rc2);
        assertFalse(regCredIt.hasNext());
        regCredWriter.remove(rc1);
    }

    /**
     * Create/write/read/remove topic RegistrationRequest on Open Domain.
     * 
     * @throws QeoException
     */
    public void testRegistrationRequestTopic()
        throws QeoException
    {
        println("testRegistrationRequestTopic");
        RegistrationRequest rr1 = getRegistrationRequest();
        regReqWriter.write(rr1);
        Iterator<RegistrationRequest> regReqIt = regReqReader.iterator();
        assertTrue(regReqIt.hasNext());
        RegistrationRequest rr2 = regReqIt.next();
        assertEquals(rr1, rr2);
        assertFalse(regReqIt.hasNext());
        regReqWriter.remove(rr1);
    }

    /**
     * Generate a valid RegistrationRequest topic instance.
     * 
     * @return RegistrationRequest instance with pseudo random data
     */
    private RegistrationRequest getRegistrationRequest()
    {
        RegistrationRequest regReq = new RegistrationRequest();
        DeviceId devId = new DeviceId();
        devId.lower = random.nextLong();
        devId.upper = random.nextLong();
        regReq.deviceId = devId;
        regReq.errorCode = (short) random.nextInt(Short.MAX_VALUE + 1);
        regReq.errorMessage = "Dummy error message";
        regReq.manufacturer = "Technicolor";
        regReq.modelName = "Gateway X.Y.Z.";
        regReq.registrationStatus = (short) random.nextInt(Short.MAX_VALUE + 1);
        regReq.rsaPublicKey = PUBLIC_RSA_KEY;
        regReq.userFriendlyName = "Friendly Dude_" + random.nextInt();
        regReq.userName = "UserName_" + random.nextInt();
        regReq.version = (short) random.nextInt(Short.MAX_VALUE + 1);
        return regReq;
    }

    /**
     * Generate a valid RegistrationCredentials topic instance.
     * 
     * @return RegistrationCredentials instance with pseudo random data
     */
    private RegistrationCredentials getRegistrationCredentials()
    {
        RegistrationCredentials regCred = new RegistrationCredentials();
        DeviceId devId = new DeviceId();
        devId.lower = random.nextLong();
        devId.upper = random.nextLong();
        regCred.deviceId = devId;
        byte[] b = new byte[256];
        random.nextBytes(b);
        regCred.encryptedOtc = b;
        regCred.realmName = "RealmName_" + random.nextInt();
        regCred.requestRSAPublicKey = PUBLIC_RSA_KEY;
        regCred.url = URL;
        return regCred;
    }
}
