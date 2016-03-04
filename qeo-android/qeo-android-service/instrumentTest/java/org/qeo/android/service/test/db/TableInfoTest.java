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

package org.qeo.android.service.test.db;

import java.io.File;

import org.qeo.android.service.db.DBHelper;
import org.qeo.android.service.db.TableInfo;

import android.test.AndroidTestCase;

/**
 * 
 */
public class TableInfoTest
    extends AndroidTestCase
{
    private DBHelper mDBHelper;
    private TableInfo mTableInfo;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        // delete old datbase if it's there
        File db = getContext().getDatabasePath(DBHelper.DATABASE);
        db.delete();

        // clean creation
        mDBHelper = new DBHelper(getContext());
        mTableInfo = new TableInfo(mDBHelper);
    }

    public void testInsertRead()
    {
        runScenario();
    }

    public void testInsertRead2()
    {
        // run 2nd time. Should have created a clean database
        runScenario();
    }

    private void runScenario()
    {
        String key = "abc";
        String testValue1 = "testvalue1";
        String testValue2 = "testvalue2";

        String val = mTableInfo.getValue(key);
        assertNull(val);

        mTableInfo.insert(key, testValue1);
        val = mTableInfo.getValue(key);
        assertEquals(testValue1, val);

        mTableInfo.insert(key, testValue2);
        val = mTableInfo.getValue(key);
        assertEquals(testValue2, val);
    }
}
