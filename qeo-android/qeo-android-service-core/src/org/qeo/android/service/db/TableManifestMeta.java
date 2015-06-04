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
import android.provider.BaseColumns;

/**
 * ManifestMeta table.
 */
public final class TableManifestMeta
    extends DBTable
{
    private static final Logger LOG = Logger.getLogger("TableManifestMeta");

    /**
     * Create an instance of TableManifestMeta.
     * 
     * @param helper DBHelper object.
     */
    public TableManifestMeta(DBHelper helper)
    {
        super(helper);
    }

    @Override
    void onCreate(SQLiteDatabase db)
    {
        LOG.fine("Creating table " + NAME);
        db.execSQL("create table " + NAME + " (" + C_PKG_NAME + " text," + C_ID + " int primary key, " + C_APP_NAME
            + " text, " + C_VERSION + " int," + C_APP_VERSION + " int, " + "UNIQUE(" + C_ID + ", " + C_PKG_NAME + ")"
            + ")");
    }

    @Override
    void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        LOG.fine("Upgrading table " + NAME + " from " + oldVersion + " to " + newVersion);
        // no upgrade needed here
    }

    /**
     * Table name.
     */
    public static final String NAME = "ManifestMeta";

    /**
     * Application uid.
     */
    public static final String C_ID = BaseColumns._ID;

    /**
     * Application package name.
     */
    public static final String C_PKG_NAME = "pkgName";

    /**
     * Manifest appname.
     */
    public static final String C_APP_NAME = "appName";

    /**
     * Manifest version.
     */
    public static final String C_VERSION = "version";

    /**
     * Application version.
     */
    public static final String C_APP_VERSION = "appVersion";
}
