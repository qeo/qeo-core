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

package org.qeo.android.service.db;

import java.util.logging.Logger;

import android.database.sqlite.SQLiteDatabase;

/**
 * ManifestReaderWriter table.
 */
public final class TableManifestRW
    extends DBTable
{
    private static final Logger LOG = Logger.getLogger("TableManifestRW");

    /**
     * Create an instance of TableManifestRW.
     * 
     * @param helper DBHelper object.
     */
    public TableManifestRW(DBHelper helper)
    {
        super(helper);
    }

    @Override
    void onCreate(SQLiteDatabase db)
    {
        LOG.fine("Creating table " + NAME);
        db.execSQL("create table " + NAME + " (" + C_PKG_NAME + " text, " + C_UID + " int, " + C_NAME + " text, "
            + C_READ + " int, " + C_WRITE + " int, " + "UNIQUE(" + C_NAME + ", " + C_PKG_NAME + ", " + C_UID + "))");
    }

    @Override
    void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        LOG.fine("Upgrading table " + NAME + " from " + oldVersion + " to " + newVersion);
        // No upgrade needed
    }

    /**
     * Table name.
     */
    public static final String NAME = "ManifestReaderWriter";

    /**
     * Topic name.
     */
    public static final String C_NAME = "name";

    /**
     * Application uid.
     */
    public static final String C_UID = "uid";

    /**
     * Application package name.
     */
    public static final String C_PKG_NAME = "pkgName";

    /**
     * Read access.
     */
    public static final String C_READ = "read";

    /**
     * Write access.
     */
    public static final String C_WRITE = "write";
}
