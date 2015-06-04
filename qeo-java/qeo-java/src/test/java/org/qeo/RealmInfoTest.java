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

package org.qeo;

import org.qeo.testframework.QeoConnectionTestListener;
import org.qeo.testframework.QeoTestCase;

/**
 * 
 */
public class RealmInfoTest
    extends QeoTestCase
{
    private QeoFactory mQeoOpen;
    private QeoConnectionTestListener mQeoReadyListener;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        // create an Open Domain Factory
        mQeoReadyListener = createFactory(getFactoryOpenId());
        mQeoOpen = mQeoReadyListener.getFactory();
    }

    @Override
    public void tearDown()
        throws Exception
    {
        closeFactory(mQeoReadyListener);
        super.tearDown();
    }

    public void testRealmId()
    {
        long realmIdVerify = Long.decode("0x625b65da308464e");

        long realmId = mQeo.getRealmId();
        assertEquals(realmIdVerify, realmId);
    }

    public void testRealmIdOpen()
    {
        long realmId = mQeoOpen.getRealmId();
        assertEquals(0, realmId);
    }

    public void testUserId()
    {
        long userIdVerify = Long.decode("0x106");

        long userId = mQeo.getUserId();
        assertEquals(userIdVerify, userId);
    }

    public void testUserIdOpen()
    {
        long userId = mQeoOpen.getUserId();
        assertEquals(0, userId);
    }

    public void testRealmUrl()
    {
        String urlVerify = "http://join.qeodev.org:8080";

        String url = mQeo.getRealmUrl();
        assertEquals(urlVerify, url);
    }

    public void testRealmUrlOpen()
    {
        String realmUrl = mQeoOpen.getRealmUrl();
        assertNull(realmUrl);
    }
}
