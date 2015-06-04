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

package org.qeo.sample.gauge;

import junit.framework.TestCase;

/**
 * Test class for computed network Interface data.
 */
public class NetworkInterfaceSpeedDataTest
        extends TestCase
{

    /**
     * This test checks for interface anme.If it is null, test fails.
     */
    public void testNullifaceName()
    {
        try {
            new NetworkInterfaceSpeedData(null);
        }
        catch (NullPointerException e) {
            return;
        }
        fail("NullPointerException expected.");
    }

    /**
     * This test checks for device id.If it is null, test fails.
     */
    public void testNullDeviceId()
    {
        try {
            NetworkInterfaceSpeedData n = new NetworkInterfaceSpeedData("test");
            n.setDeviceId(null);
            if (n.getDeviceId().equals(null)) {
                fail();
            }
        }
        catch (NullPointerException e) {
            return;
        }
        fail("NullPointerException expected.");
    }

}
