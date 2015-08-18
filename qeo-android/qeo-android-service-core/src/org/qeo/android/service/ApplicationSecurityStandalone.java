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

package org.qeo.android.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qeo.android.internal.IServiceQeoCallback;
import org.qeo.android.service.db.TableManifestMeta;
import org.qeo.android.service.db.TableManifestRW;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Class to contain security information (eg the manifest) from the connected application.
 */
public class ApplicationSecurityStandalone implements ApplicationSecurity
{
    private static final Logger LOG = Logger.getLogger("ApplicationSecurity");
    /**Action to be broadcasted when the manifest dialog is dismissed.*/
    public static final String ACTION_MANIFEST_DIALOG_FINISHED = "actionManifestDialogFinished";
    /** The name of the title field in the intent. */
    public static final String INTENT_EXTRA_TITLE = "Title";
    /** The name of the permissions field in the intent. */
    public static final String INTENT_EXTRA_PERMISSIONS = "Permissions";
    /** The name of the UID field in the intent. */
    public static final String INTENT_EXTRA_UID = "INTENT_EXTRA_UID";
    /** The name of the result field in the intent. */
    public static final String INTENT_EXTRA_RESULT = "Result";

    private final Set<Long> mRegisteredReadersWriters;
    private final QeoService mService;
    private final int mUid;
    private final int mPid;
    private final String mAppLabel;
    private final String mPkgName;
    private IServiceQeoCallback mManifestDialogCallback;

    /**
     * Enum to indicate the section of the manifest file.
     */
    private enum ParseContext
    {
        META, APPLICATION
    }

    /**
     * Enum to indicate read and/or write state.
     */
    private enum RW
    {
        /** Read only. */
        R,
        /** Write only. */
        W,
        /** Read/write. */
        RW
    }

    /*
     * The next 4 fields are considered temporary. They will be initialized in parseManifest and they will be cleared in
     * insertAppInfo and insertReadersWriters.
     */
    private String mAppName = null;
    private int mVersion = -1;
    private int mAppVersion = -1;
    private final Map<String, RW> mReadersWriters = new HashMap<String, RW>();

    /**
     * Initialize a new instance.
     *
     * @param service Reference to the Qeo service
     * @param uid     the uid belonging to this content
     * @param pid     the pid belonging to this content
     */
    ApplicationSecurityStandalone(QeoService service, int uid, int pid)
    {
        LOG.fine("Create new application security for uid: " + uid + ", pid: " + pid);
        mService = service;
        mUid = uid;
        mPid = pid;
        mRegisteredReadersWriters = new HashSet<Long>();
        try {
            PackageManager pm = mService.getPackageManager();
            // Packages with sharedUserId set in manifest will have their uid concatenated in getNameForUid call
            // What will happen if multiple packages with same sharedUserId make user of Qeo??
            mPkgName = pm.getNameForUid(mUid).split(":")[0];
            if (mPkgName.equals("org.qeo.android.service")) {
                // special case, will only be triggered by unit tests
                mAppVersion = 1;
                mAppLabel = "junit";
            }
            else {
                mAppVersion = pm.getPackageInfo(mPkgName, 0).versionCode;
                ApplicationInfo appInfo = pm.getPackageInfo(mPkgName, 0).applicationInfo;
                if (appInfo == null) {
                    throw new SecurityException("Can't get application name for uid " + uid);
                }
                mAppLabel = appInfo.loadLabel(pm).toString();
                if (mAppLabel.isEmpty()) {
                    throw new SecurityException("Can't get application name for uid " + uid);
                }
            }
            LOG.fine("Application security created: " + mPkgName + " -- " + mAppVersion + " -- " + mAppLabel);
        }
        catch (NameNotFoundException e) {
            throw new SecurityException("Can't get application version for uid " + uid, e);
        }
        mManifestDialogCallback = null;
    }

    @Override
    public void registerReaderWriter(long id)
    {
        if (QeoDefaults.isProxySecurityEnabled()) {
            LOG.fine("register reader/writer: uid: " + mUid + " rw: " + id);
            synchronized (mRegisteredReadersWriters) {
                mRegisteredReadersWriters.add(id);
            }
        }
    }

    @Override
    public void unRegisterReaderWriter(long id)
    {
        if (QeoDefaults.isProxySecurityEnabled()) {
            LOG.fine("unregister reader/writer: uid: " + mUid + " rw: " + id);
            synchronized (mRegisteredReadersWriters) {
                mRegisteredReadersWriters.remove(id);
            }
        }
    }

    @Override
    public void checkRegisteredReaderWriter(long id)
    {
        if (QeoDefaults.isProxySecurityEnabled()) {
            LOG.fine("check reader/writer: uid: " + mUid + " rw: " + id);
            synchronized (mRegisteredReadersWriters) {
                if (!mRegisteredReadersWriters.contains(id)) {
                    throw new SecurityException("Not allowed to use reader/writer " + id);
                }
            }
        }
    }

    @Override
    public void insertAppInfo()
    {
        if (mAppName == null) {
            throw new SecurityException("\"appname\" not set in security manifest");
        }
        if (mVersion == -1) {
            throw new SecurityException("\"version\" not set in security manifest");
        }
        SQLiteDatabase db = mService.getDatabase();
        ContentValues values = new ContentValues();
        values.put(TableManifestMeta.C_ID, mUid);
        values.put(TableManifestMeta.C_PKG_NAME, mPkgName);
        values.put(TableManifestMeta.C_APP_NAME, mAppName);
        values.put(TableManifestMeta.C_VERSION, mVersion);
        values.put(TableManifestMeta.C_APP_VERSION, mAppVersion);
        db.insertWithOnConflict(TableManifestMeta.NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        mAppName = null;
        mVersion = 0;
    }

    /**
     * Insert allowed readers/writers into the database. Clear the mReadersWriters map when everything is saved in the
     * database.
     */
    private synchronized void insertReadersWriters()
    {
        SQLiteDatabase db = mService.getDatabase();

        // first delete all known readers/writers for this uid to ensure old ones get removed
        String where = TableManifestRW.C_UID + "=?";
        db.delete(TableManifestRW.NAME, where, new String[]{Integer.toString(mUid)});

        // add all
        for (Map.Entry<String, RW> entry : mReadersWriters.entrySet()) {
            ContentValues values = new ContentValues();
            values.put(TableManifestRW.C_NAME, entry.getKey());
            values.put(TableManifestRW.C_UID, mUid);
            values.put(TableManifestRW.C_PKG_NAME, mPkgName);
            LOG.fine("Add rw to table: " + entry.getKey() + " -- " + mUid + " -- " + mPkgName);
            switch (entry.getValue()) {
                case R: // read only
                    values.put(TableManifestRW.C_READ, 1);
                    values.put(TableManifestRW.C_WRITE, 0);
                    break;
                case W: // write only
                    values.put(TableManifestRW.C_READ, 0);
                    values.put(TableManifestRW.C_WRITE, 1);
                    break;
                case RW: // read/write only
                    values.put(TableManifestRW.C_READ, 1);
                    values.put(TableManifestRW.C_WRITE, 1);
                    break;
                default:
                    throw new IllegalStateException("value not handled");
            }
            db.insertOrThrow(TableManifestRW.NAME, null, values);
        }
        mReadersWriters.clear();
    }

    @Override
    public int getAppVersion()
    {
        SQLiteDatabase db = mService.getDatabase();
        String[] columns = new String[]{TableManifestMeta.C_APP_VERSION};
        String selection = TableManifestMeta.C_ID + "=? AND " + TableManifestMeta.C_PKG_NAME + "=?";
        String[] selectionArgs = new String[]{Integer.toString(mUid), mPkgName};
        Cursor cursor = db.query(TableManifestMeta.NAME, columns, selection, selectionArgs, null, null, null);
        if (cursor.moveToFirst()) {
            int version = cursor.getInt(0);
            cursor.close();
            return version;
        }
        return -1; // not yet known
    }

    private boolean isAllowedReaderWriter(String rw, boolean read)
    {
        SQLiteDatabase db = mService.getDatabase();
        String[] columns = new String[]{TableManifestRW.C_UID};
        LOG.fine("isAllowedReaderWriter: " + rw + " -- " + mUid + " -- " + mPkgName);
        String selection =
            TableManifestRW.C_NAME + " = ? AND " + TableManifestRW.C_UID + " = ? AND " + TableManifestRW.C_PKG_NAME
                + " = ? AND " + (read ? TableManifestRW.C_READ : TableManifestRW.C_WRITE) + " = ?";
        String[] selectionArgs = new String[]{rw, Integer.toString(mUid), mPkgName, "1"};
        Cursor cursor = db.query(TableManifestRW.NAME, columns, selection, selectionArgs, null, null, null);
        boolean ok = cursor.getCount() == 1;
        cursor.close();
        return ok;
    }

    @Override
    public boolean isAllowedReader(String reader)
    {
        return isAllowedReaderWriter(reader, true);
    }

    @Override
    public boolean isAllowedWriter(String writer)
    {
        return isAllowedReaderWriter(writer, false);
    }

    @Override
    public synchronized void evaluateManifest(QeoServiceImpl serviceImpl, IServiceQeoCallback cb)
        throws RemoteException
    {
        ArrayList<String> permissions = new ArrayList<String>();

        // verify if all readers/writer are already known in the database
        boolean allKnown = true;
        for (Map.Entry<String, RW> entry : mReadersWriters.entrySet()) {
            String name = entry.getKey();
            String permissionValue;
            switch (entry.getValue()) {
                case R:
                    if (!isAllowedReader(name)) {
                        allKnown = false;
                    }
                    permissionValue = "Read";
                    break;
                case W:
                    if (!isAllowedWriter(name)) {
                        allKnown = false;
                    }
                    permissionValue = "Write";
                    break;
                case RW:
                    if (!isAllowedReader(name) || !isAllowedWriter(name)) {
                        allKnown = false;
                    }
                    permissionValue = "Read/Write";
                    break;
                default:
                    throw new IllegalStateException("unhandled case: " + entry.getValue());
            }
            permissions.add(name + ": " + permissionValue);
        }
        if (!allKnown) {
            LOG.info("Qeo manifest accept needed for application with uid " + mUid);
            showDialog(serviceImpl, cb, permissions);
        }
        else {
            LOG.fine("Qeo manifest was already accepted for application with uid " + mUid);
            cb.onManifestReady(true);
        }
    }

    @Override
    public synchronized void parseManifest(String[] manifest)
    {
        ParseContext parseContext = null;
        Pattern pEmptyLine = Pattern.compile("^\\s*$");
        Pattern pCommentLine = Pattern.compile("^([^#]*)#.*$");
        Pattern pContext = Pattern.compile("^\\s*\\[(\\S+)\\]\\s*$");
        Pattern pkeyValue = Pattern.compile("^\\s*(\\S+)\\s*=\\s*(['\"])?([^'\"]+)(['\"])?\\s*$");
        Matcher m;

        for (String lineFull : manifest) {
            String line = lineFull;
            m = pCommentLine.matcher(line);
            if (m.matches()) {
                line = m.group(1); // throw away comments
            }
            if (pEmptyLine.matcher(line).matches()) {
                continue; // ignore
            }
            line = line.trim();

            // get context info
            m = pContext.matcher(line);
            if (m.matches()) {
                try {
                    parseContext = ParseContext.valueOf(m.group(1).toUpperCase(Locale.US));
                }
                catch (IllegalArgumentException e) {
                    throw new SecurityException("Invalid keyword in manifest file: " + line);
                }
                continue;
            }

            if (parseContext == null) {
                throw new SecurityException("Manifest should start with [...] block");
            }

            m = pkeyValue.matcher(line);
            if (!m.matches()) {
                throw new SecurityException("Invalid line in manifest: " + line);
            }
            String key = m.group(1);
            String open = m.group(2);
            String value = m.group(3);
            String close = m.group(4);
            if ((open == null && close == null) || (open != null && close != null && open.equals(close))) {
                // ok
                switch (parseContext) {
                    case META:
                        if (key.equals("appname")) {
                            mAppName = value;
                        }
                        else if (key.equals("version")) {
                            mVersion = Integer.parseInt(value);
                        }
                        else {
                            throw new SecurityException("Invalid line in manifest in [meta]: " + line);
                        }
                        break;
                    case APPLICATION:
                        if (key.contains(".")) {
                            throw new SecurityException("Topic name in manifest should not contain dots, wrong line: "
                                + line);
                        }
                        key = key.replaceAll("::", ".");
                        if (value.equals("r")) {
                            mReadersWriters.put(key, RW.R);
                        }
                        else if (value.equals("w")) {
                            mReadersWriters.put(key, RW.W);
                        }
                        else if (value.equals("rw")) {
                            mReadersWriters.put(key, RW.RW);
                        }
                        else {
                            throw new SecurityException("Manifest parse error: Can't handle value \"" + value
                                + "\" for application " + key);

                        }
                        break;
                    default:
                        throw new IllegalStateException("Error parsing manifest");
                }
            }
            else {
                throw new SecurityException("Invalid line in manifest: " + line);
            }
        }
    }

    /**
     * Show a dialog containing all permissions for this application.
     *
     * @param permissions the permissions to be displayed
     * @throws RemoteException Thrown when calling the onManifestReady callback fails
     */
    private void showDialog(QeoServiceImpl serviceImpl, IServiceQeoCallback cb, List<String> permissions)
        throws RemoteException
    {
        Boolean[] popupDisabled = {false};

        serviceImpl.checkPopupDisabled(popupDisabled);
        LOG.fine("Manifest popup " + (mService.isManifestPopupDisabled() || popupDisabled[0] ? "disabled" : "enabled"));
        if (mService.isManifestPopupDisabled() || popupDisabled[0]) {
            // popup disabled, just accept everything
            insertAppInfo();
            insertReadersWriters();
            cb.onManifestReady(true);
        }
        else {
            // launching activity to show the manifest popup
            try {
                Class<?> clazz = Class.forName("org.qeo.android.security.ManifestActivity");
                Intent intent = new Intent(mService, clazz);
                Bundle args = new Bundle();

                // save the callback in order to be able to use it in the onDialogFinished broadcast
                mManifestDialogCallback = cb;

                // Listen for intent signaling completion of manifest dialog
                LocalBroadcastManager.getInstance(mService).registerReceiver(mOnDialogFinished,
                    new IntentFilter(ACTION_MANIFEST_DIALOG_FINISHED));

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                args.putInt(INTENT_EXTRA_UID, mUid);
                args.putString(INTENT_EXTRA_TITLE, mAppLabel + " is requesting the following Qeo permissions");
                args.putStringArray(INTENT_EXTRA_PERMISSIONS, permissions.toArray(new String[permissions.size()]));
                intent.putExtras(args);
                mService.getApplicationContext().startActivity(intent);
            }
            catch (ClassNotFoundException e) {
                LOG.log(Level.SEVERE, "Error launching manifest activity", e);
            }

        }
    }

    /**
     * The broadcast receiver that gets notified when the user finished the Manifest dialog.
     */
    private final BroadcastReceiver mOnDialogFinished = new BroadcastReceiver()
    {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            LOG.fine("Manifest dialog completed broadcast event received");
            // Check if this broadcast intent is for us (based on mUid).
            if (mUid == intent.getIntExtra(INTENT_EXTRA_UID, -1)) {
                LocalBroadcastManager.getInstance(mService).unregisterReceiver(this);
                boolean result = intent.getBooleanExtra(INTENT_EXTRA_RESULT, false);
                if (result) {
                    LOG.fine("Save Qeo manifest data in database");
                    insertAppInfo();
                    insertReadersWriters();
                }
                try {
                    if (mManifestDialogCallback != null) {
                        LOG.fine("Sending onManifestReady callback for " + mUid + "/" + mPid);
                        mManifestDialogCallback.onManifestReady(result);
                    }
                    else {
                        LOG.warning("No callback found for uid " + mUid + " to send onManifestReady callback");
                    }
                }
                catch (RemoteException e) {
                    // Not much we can do here...
                    LOG.log(Level.WARNING, "Error calling onManifestReady for uid " + mUid, e);
                }
            }
            else {
                LOG.fine("Manifest dialog completed broadcast wrong uid: " + mUid + ",got "
                    + intent.getIntExtra(INTENT_EXTRA_UID, -1));
            }
        }
    };
}
