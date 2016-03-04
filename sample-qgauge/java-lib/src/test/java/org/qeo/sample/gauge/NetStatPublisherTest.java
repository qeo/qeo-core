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

package org.qeo.sample.gauge;

import org.qeo.exception.QeoException;
import org.qeo.testframework.QeoTestCase;

/**
 * Test class for NetStatPublisherTest
 */
public class NetStatPublisherTest
    extends QeoTestCase
{
    public void testNullArg()
        throws QeoException
    {
        try {
            new NetStatPublisher(null, 1000, mQeo);
        }
        catch (IllegalArgumentException e) {
            return;
        }
        fail("IllegalArgumentException expected.");
    }
}
