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

import android.database.sqlite.SQLiteDatabase;

/**
 * abstract class to indicate database table.
 */
abstract class DBTable
{
    /** Database helper reference. */
    protected final DBHelper mDBHelper;

    /**
     * Create an instance of TableInfo.
     * 
     * @param helper dbHelper object
     */
    public DBTable(DBHelper helper)
    {
        mDBHelper = helper;
    }

    /**
     * Create the database.
     * 
     * @param db sql database instance.
     */
    abstract void onCreate(SQLiteDatabase db);

    /**
     * Upgrade the database.
     * 
     * @param db sql database instance.
     * @param oldVersion The old version.
     * @param newVersion The new version.
     */
    abstract void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
}
