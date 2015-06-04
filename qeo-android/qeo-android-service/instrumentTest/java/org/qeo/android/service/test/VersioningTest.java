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

package org.qeo.android.service.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qeo.java.QeoJava;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.test.AndroidTestCase;

/**
 * Test case to test Qeo versioning.
 */
public class VersioningTest
    extends AndroidTestCase
{
    private PackageInfo pInfo;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        Context ctx = getContext();
        PackageManager pm = ctx.getPackageManager();
        pInfo = pm.getPackageInfo(ctx.getPackageName(), 0);
        assertNotNull(pInfo);
    }

    /**
     * Tests whether the version string is valid.
     */
    public void testVersionString()
        throws NameNotFoundException
    {
        /* retrieve version name from Android package manager */
        String expVersion = pInfo.versionName;
        /* retrieve version from Java (-> native) */
        String version = QeoJava.getVersionString();
        /* test */
        assertFalse(version.endsWith("UNKNOWN"));
        // first part should be equal
        assertEquals(expVersion.split("-")[0], version.split("-")[0]);
    }

    public void testVersionCode()
    {
        Pattern patternVersion = Pattern.compile("^(\\d\\d*)\\.(\\d\\d*)\\.(\\d\\d*)-.*$");
        Matcher matcher = patternVersion.matcher(pInfo.versionName);
        assertTrue(matcher.matches());
        int versionCode =
            Integer.parseInt(matcher.group(1)) * 10000 + Integer.parseInt(matcher.group(2)) * 100
                + Integer.parseInt(matcher.group(3));

        assertEquals(versionCode, pInfo.versionCode);
    }
}
