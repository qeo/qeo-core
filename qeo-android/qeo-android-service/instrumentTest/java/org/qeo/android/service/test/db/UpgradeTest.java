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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.qeo.android.service.db.DBHelper;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.InstrumentationTestCase;
import android.util.Log;

/**
 * Class to test upgradescenario of sqlite databases. It will use for each version an backup and try to upgrade to the
 * latest version. Next the structure of both will be compared and must be equal.
 */
public class UpgradeTest
    extends InstrumentationTestCase
{
    private final String TAG = "UpgradeTest";

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        // delete old datbase if it's there
        File db = getInstrumentation().getTargetContext().getDatabasePath(DBHelper.DATABASE);
        db.delete();
    }

    /**
     * Copy old database from assets folder to database folder.
     */
    private void dbTransfer(int version)
        throws Exception
    {
        Log.d(TAG, "dbtransfer");
        BufferedInputStream bis = null;
        BufferedOutputStream bus = null;
        try {
            String fromName = "QeoService_v" + version + ".db";
            File to = getInstrumentation().getTargetContext().getDatabasePath(DBHelper.DATABASE);
            // If you get an error on the next line that it can't file your database file, you should pull an old
            // database file from a device that's not yet upgraded an store it in instrumentTest/assets folder
            // adb pull /data/data/org.qeo.android.service/databases/QeoService.db
            bis = new BufferedInputStream(getInstrumentation().getContext().getAssets().open(fromName));
            bus = new BufferedOutputStream(new FileOutputStream(to));
            byte[] buf = new byte[1024];
            do {
                int size = bis.read(buf);
                if (size == -1) {
                    break;
                }
                bus.write(buf, 0, size);
            }
            while (true);
        }
        finally {
            if (bus != null) {
                bus.close();
            }
            if (bis != null) {
                bis.close();
            }
        }

        Log.d(TAG, "dbtransfer done");
    }

    private Set<String> dump(int expectedOldVersion)
    {
        DBHelper dbHelper = new DBHelper(getInstrumentation().getTargetContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // check that the database file was actually converted from the right version.
        assertEquals(expectedOldVersion, dbHelper.getOldVersion());
        Cursor c = db.query("sqlite_master", null, null, null, null, null, null);

        // table strucute of master table
        // CREATE TABLE sqlite_master (
        // type text,
        // name text,
        // tbl_name text,
        // rootpage integer,
        // sql text
        // );

        Set<String> list = new HashSet<String>();
        while (c.moveToNext()) {
            // ignore rootpage field as this will be the current id of auto increment fields and cannot be compared
            String entry = c.getString(0) + "|" + c.getString(1) + "|" + c.getString(2) + "|" + c.getString(4);
            list.add(entry);
        }
        dbHelper.close();
        return list;
    }

    public void testUpgrade()
        throws Exception
    {
        Set<String> current = dump(-1); // get structure of current database. oldVersion should be -1

        for (int i = 1; i < DBHelper.VERSION; ++i) {
            // there should be and old database for each version. From that version you should be able to update to the
            // latest verion
            dbTransfer(i);
            // dump the sqlite content after upgrading from this verion
            Set<String> upgrade = dump(i);

            // compare!
            for (String line : upgrade) {
                if (!current.contains(line)) {
                    fail("Upgraded database contains other SQL structure:\n" + line);
                }
            }
            for (String line : current) {
                if (!upgrade.contains(line)) {
                    fail("New created database contains other SQL structure:\n" + line);
                }
            }
        }

    }
}
