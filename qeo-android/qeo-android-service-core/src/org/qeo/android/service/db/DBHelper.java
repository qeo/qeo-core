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

package org.qeo.android.service.db;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper class to provide database access.
 */
public class DBHelper
    extends SQLiteOpenHelper
{
    private static final Logger LOG = Logger.getLogger("DBHelper");

    /** Database version. Increase this if the structure changes. */
    public static final int VERSION = 2;
    /** Database name. */
    public static final String DATABASE = "QeoService.db";
    private static int sOldVersion = -1;

    /**
     * Initialize database.
     * 
     * @param context Android context.
     */
    public DBHelper(Context context)
    {
        super(context, DATABASE, null, VERSION);
    }

    private List<DBTable> getTables()
    {
        List<DBTable> dbTables = new LinkedList<DBTable>();

        // List all known tables here!
        dbTables.add(new TableManifestMeta(null));
        dbTables.add(new TableManifestRW(null));
        dbTables.add(new TableInfo(null));
        // end tables list

        return dbTables;
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        LOG.info("Creating database: " + DATABASE);
        for (DBTable table : getTables()) {
            table.onCreate(db);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        setOldVersion(oldVersion);
        LOG.info("Upgrading database " + DATABASE + " from version " + oldVersion + " to " + newVersion);
        for (DBTable table : getTables()) {
            table.onUpgrade(db, oldVersion, newVersion);
        }
    }

    private static void setOldVersion(int version)
    {
        sOldVersion = version;
    }

    /**
     * Get the old version number that the database was upgraded from. To be used for unit tests only.
     * 
     * @return The old version. -1 if not upgrade.
     */
    public int getOldVersion()
    {
        return sOldVersion;
    }

}
