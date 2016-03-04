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

package org.qeo.common;

import junit.framework.TestCase;

import org.qeo.internal.common.Util;

public class UtilTest
        extends TestCase
{
    public void test()
        throws Exception
    {
        long id;
        id = Util.calculateID("testfield1");
        assertTrue(id == 0x07D189A3);

        id = Util.calculateID("testfield2");
        assertTrue(id == 0x0ED8D819);
    }
}
