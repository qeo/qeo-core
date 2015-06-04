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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Generic table to contain key/value pairs.
 */
public final class TableInfo
    extends DBTable
{
    // this table is created in DB version 2

    private static final Logger LOG = Logger.getLogger("TableInfo");
    /** Table name. */
    private static final String NAME = "Info";
    /** Key. */
    private static final String C_KEY = "key";
    /** Value. */
    private static final String C_VALUE = "value";

    /** Key for realm username. */
    public static final String KEY_REALM_USERNAME = "realmUsername";
    /** Key for realm name. */
    public static final String KEY_REALM_NAME = "realmName";
    /** Key for DeviceInfo serial. */
    public static final String KEY_DEVICE_INFO_SERIAL = "deviceInfoSerial";
    /** Key for DeviceIdUpper. */
    public static final String KEY_DEVICE_ID_UPPER = "deviceIdUpper";
    /** Key for DeviceIdLower. */
    public static final String KEY_DEVICE_ID_LOWER = "deviceIdLower";

    /**
     * Create an instance of TableInfo.
     * 
     * @param helper DBHelper object.
     */
    public TableInfo(DBHelper helper)
    {
        super(helper);
    }

    @Override
    void onCreate(SQLiteDatabase db)
    {
        LOG.info("Creating table " + NAME);
        db.execSQL("create table " + NAME + " (" + C_KEY + " TEXT NOT NULL UNIQUE, " + C_VALUE + " TEXT NOT NULL)");
    }

    @Override
    void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        LOG.info("Upgrading table " + NAME + " from " + oldVersion + " to " + newVersion);
        if (oldVersion < 2) {
            onCreate(db);
        }
    }

    /**
     * Insert a key value pair.
     * 
     * @param key The key. Will be overwritten if exists.
     * @param value The value. Cannot be null.
     */
    public void insert(String key, String value)
    {
        ContentValues cv = new ContentValues();
        cv.put(C_KEY, key);
        cv.put(C_VALUE, value);
        SQLiteDatabase db = null;
        try {
            db = mDBHelper.getWritableDatabase();
            db.insertWithOnConflict(NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
        finally {
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * Get a value for a key.
     * 
     * @param key The key.
     * @return The value if found, null otherwise.
     */
    public String getValue(String key)
    {
        String result = null;
        SQLiteDatabase db = null;
        try {
            db = mDBHelper.getReadableDatabase();
            String[] columns = new String[] {C_VALUE};
            String[] selectionArgs = new String[] {key};
            Cursor c = db.query(NAME, columns, C_KEY + " = ?", selectionArgs, null, null, null);
            if (c.moveToFirst()) {
                result = c.getString(c.getColumnIndex(C_VALUE));
            }
            c.close();
        }
        finally {
            if (db != null) {
                db.close();
            }
        }
        return result;
    }
}
