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

/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

package com.technicolor.wifidoctor;

import org.qeo.QeoType;
import org.qeo.QeoType.Behavior;
import org.qeo.Key;

@QeoType(behavior = Behavior.STATE)
public class TestState
{
    /**
     * id of the corresponding TestRequest
     */
    @Key
    public int id;

    /**
     * MAC address of the test participant publishing this test state
     */
    @Key
    public String participant;

    /**
     * This should be an enum really. Possible values: 0 = QUEUED: acknowledge we've seen the test request, but it is not yet ready for execution 1 = WILLING: RX node indicates it is ready to participate in the test, waits for a COMMIT from the TX node before starting 2 = COMMIT: TX node indicates it is committed to starting the test, waits for RX node to go to TESTING before actually starting 3 = TESTING: test ongoing (for both RX and TX node) 4 = DONE: test is finished, results will be published 5 = REJECTED: node is unwilling to perform this test for some reason For tests where both TX and RX node are WifiDr-capable, we assume the following sequence of states: Coordinator TX node RX node --------------------------------------------------------- publish TestRequest QUEUED QUEUED v v WILLING COMMIT v v TESTING TESTING v v DONE v read TX node results DONE read RX node results remove TestRequest v v remove TestState remove TestState For "blind" tests (where the RX node is not WifiDr-capable), we assume the following sequence of states: Coordinator TX node ----------------------------------------- publish TestRequest QUEUED v TESTING v v DONE read TX node results remove TestRequest v remove TestState
     */
    public int state;

    /**
     * Default constructor.  This is used by Qeo to construct new objects.
     */
    public TestState()
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
        final TestState myObj = (TestState) obj;
        if (id != myObj.id) {
            return false;
        }
        if (!participant.equals(myObj.participant)) {
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
        result = prime * result + ((participant == null) ? 0 : participant.hashCode());
        return result;
    }
}
