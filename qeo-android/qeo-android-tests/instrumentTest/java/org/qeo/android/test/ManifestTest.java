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

package org.qeo.android.test;

import org.qeo.DefaultEventReaderListener;
import org.qeo.exception.QeoException;
import org.qeo.system.DeviceInfo;
import org.qeo.testframework.QeoTestCase;

/**
 * 
 */
public class ManifestTest
    extends QeoTestCase
{
    public static class ClassNotListedInManifest
    {
        public int id;
    }

    public void testReader()
        throws QeoException
    {
        try {
            mQeo.createEventReader(ClassNotListedInManifest.class,
                new DefaultEventReaderListener<ClassNotListedInManifest>());
            if (!Helper.isEmbedded()) {
                fail("Should throw securityexception");
            }
        }
        catch (SecurityException e) {
            if (Helper.isEmbedded()) {
                fail("Embedded service should not throw securityexception");
            }
        }
    }

    public void testWriter()
        throws QeoException
    {
        try {
            mQeo.createEventWriter(ClassNotListedInManifest.class);
            if (!Helper.isEmbedded()) {
                fail("Should throw securityexception");
            }
        }
        catch (SecurityException e) {
            if (Helper.isEmbedded()) {
                fail("Embedded service should not throw securityexception");
            }
        }
    }

    public void testWriter2()
        throws QeoException
    {
        try {
            // DeviceInfo is readonly
            mQeo.createStateWriter(DeviceInfo.class);
            if (!Helper.isEmbedded()) {
                fail("Should throw securityexception");
            }
        }
        catch (SecurityException e) {
            if (Helper.isEmbedded()) {
                fail("Embedded service should not throw securityexception");
            }
        }
    }
}
