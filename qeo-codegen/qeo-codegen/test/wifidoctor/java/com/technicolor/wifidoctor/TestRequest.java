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

/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

package com.technicolor.wifidoctor;

import org.qeo.QeoType;
import org.qeo.QeoType.Behavior;
import org.qeo.Key;

/**
 * A coordinator (typically the WifiDr Android app on the STA, but not necessarily) publishes a TestRequest to trigger a test between an AP and a STA. As long as the TestRequest instance lives, the test is 	 relevant and will be (eventually) carried out, or the results will 	 remain available. When the TestRequest is removed, all other traces 	 of the test (test states, results) will be removed as well.
 */
@QeoType(behavior = Behavior.STATE)
public class TestRequest
{
    @Key
    public int id;

    /**
     * MAC address of the transmitting node for this test
     */
    public String tx;

    /**
     * MAC address of the receiving node for this test
     */
    public String rx;

    /**
     * The test type. This is a poor man's substitute for an enumeration. Possible values are: 0: PING test 1: TX test
     */
    public int type;

    /**
     * Ping parameter (1 < = x < = 15)
     */
    public int count;

    /**
     * Ping parameter (0 < = x < = 20000)
     */
    public int size;

    /**
     * Ping parameter (100 < = x < = 1000000)
     */
    public int interval;

    /**
     * Ping parameter (1 < = x)
     */
    public int timeout;

    /**
     * TX test parameter (0 < = x < = 86400)
     */
    public int duration;

    /**
     * TX test parameter (64 < = x < = 2346)
     */
    public int packetSize;

    /**
     * TX test parameter. Enum with possible values: 0 = AUTO 1 = CCK 2 = OFDMLEGACY 3 = OFDMMCS
     */
    public int modulation;

    /**
     * TX test parameter (-1 < = x < = 32)
     */
    public int rateIndex;

    /**
     * TX test parameter (0 < = x < = 15)
     */
    public byte priority;

    /**
     * TX test parameter
     */
    public boolean AMPDU;

    /**
     * Default constructor.  This is used by Qeo to construct new objects.
     */
    public TestRequest()
    {
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }
        if ((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }
        final TestRequest myObj = (TestRequest) obj;
        if (id != myObj.id) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }
}
