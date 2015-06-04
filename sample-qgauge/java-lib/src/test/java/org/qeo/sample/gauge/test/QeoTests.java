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

package org.qeo.sample.gauge.test;

import java.io.IOException;

import org.qeo.StateReader;
import org.qeo.sample.gauge.NetStatMessage;
import org.qeo.testframework.QeoTestCase;

public class QeoTests
    extends QeoTestCase
{

    /**
     * Test that repeatedly opens and closes the same reader. This test is expected to succeed when DE1560 is fixed.
     * 
     * @throws IOException
     */
    public synchronized void testRepeatedOpenCloseReader()
        throws IOException
    {
        StateReader<NetStatMessage> reader;
        for (int i = 0; i < 100; i++) {
            System.out.println(i);
            reader = mQeo.createStateReader(NetStatMessage.class, null);
            reader.close();
        }
    }
}
