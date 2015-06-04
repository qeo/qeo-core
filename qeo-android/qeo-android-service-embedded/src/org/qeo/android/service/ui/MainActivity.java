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

package org.qeo.android.service.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.qeo.QeoFactory;
import org.qeo.android.service.QeoService;
import org.qeo.android.service.db.DBHelper;
import org.qeo.android.service.db.TableInfo;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.QeoManagementApp;
import org.qeo.deviceregistration.helper.DataHandler;
import org.qeo.exception.QeoException;
import org.qeo.java.QeoConnectionListener;
import org.qeo.java.QeoJava;
import org.qeo.jni.NativeQeo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

/**
 * Class to display information about your realm.
 */
public class MainActivity
    extends FragmentActivity
{
    private static final Logger LOG = Logger.getLogger("MainActivity");

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_single_fragment);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.containerSingleFragment, new MainFragment())
                .commit();
        }
        setTitle(R.string.title_info_activity);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class MainFragment
        extends Fragment
    {
        private static final String TITLE = "title";
        private static final String VALUE = "value";
        private ListView mListView;
        private final List<Map<String, String>> mItems = new ArrayList<Map<String, String>>();
        private SimpleAdapter mAdapter;
        private boolean mIsRegistered;
        private boolean mQeoInitDone;
        private QeoFactory mQeo = null;
        private AsyncTask<Void, Void, Void> mDataFetcher = null;

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            setHasOptionsMenu(true);
            mIsRegistered = false;

            // create dummy factory to fetch information
            QeoService.initNative(getActivity());
            QeoManagementApp.init();
            startQeo();

            String[] from = new String[]{TITLE, VALUE};
            int[] to = new int[]{android.R.id.text1, android.R.id.text2};
            mAdapter = new SimpleAdapter(getActivity(), mItems, android.R.layout.two_line_list_item, from, to);
        }

        private void startQeo()
        {
            mQeoInitDone = false;
            getActivity().setProgressBarIndeterminateVisibility(true);
            QeoManagementApp.getGlobalExecutor().submitTaskToPool(new Runnable()
            {

                @Override
                public void run()
                {
                    // do this in background, this will block
                    QeoJava.initQeo(mConnectionListener);
                }
            });
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            mListView = (ListView) rootView.findViewById(R.id.mainFragment_listview);
            mListView.setAdapter(mAdapter);
            return rootView;
        }

        @Override
        public void onStart()
        {
            super.onStart();
            if (mQeoInitDone && !mIsRegistered) {
                // if qeo init has finished but the device is not marked as registered, try to init again here to check
                // if registration is done know.
                // this can happen if the device got registered in the meanwhile from the management activity
                startQeo();
            }
        }

        @Override
        public void onDestroy()
        {
            QeoJava.closeQeo(mConnectionListener);
            mQeo = null;
            if (mDataFetcher != null) {
                mDataFetcher.cancel(true);
            }
            super.onDestroy();
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
        {
            inflater.inflate(R.menu.activity_main, menu);
        }

        private void startManagement()
        {
            startActivity(new Intent(getActivity(), org.qeo.deviceregistration.ui.ManagementActivity.class));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == R.id.mainActivity_menu_manage_realm) {
                if (!QeoManagementApp.isRealmAdmin() && mIsRegistered) {
                    // this device is registered to qeo but the user is not an admin
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getString(R.string.action_manage_realm));
                    builder.setMessage(getString(R.string.not_admin_user));
                    builder.setNegativeButton(android.R.string.cancel, null);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                    {

                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            startManagement();
                        }
                    });
                    builder.show();
                }
                else {
                    // is admin
                    startManagement();
                }
            }
            else if (id == R.id.mainActivity_menu_about) {
                startActivity(new Intent(getActivity(), AboutActivity.class));
                return true;
            }
            else if (id == R.id.mainActivity_menu_leave) {
                if (!mIsRegistered) {
                    Toast.makeText(getActivity(), R.string.notYetRegistered, Toast.LENGTH_SHORT).show();
                }
                else {
                    //ask for confirmation
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getString(R.string.leaveRealm));
                    builder.setMessage(getString(R.string.leaveRealmConfirmation));
                    builder.setNegativeButton(android.R.string.cancel, null);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                    {

                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            leaveRealm();
                        }
                    });
                    builder.show();
                }
            }
            return super.onOptionsItemSelected(item);
        }

        private void addItem(String title, String value)
        {
            Map<String, String> map = new HashMap<String, String>();
            map.put(TITLE, title);
            map.put(VALUE, value);
            mItems.add(map);
        }

        private void fetchData()
        {
            if (mDataFetcher != null) {
                mDataFetcher.cancel(true);
            }
            mDataFetcher = new AsyncTask<Void, Void, Void>()
            {

                @Override
                protected void onPostExecute(Void result)
                {
                    mAdapter.notifyDataSetChanged();
                    getActivity().setProgressBarIndeterminateVisibility(false);
                }

                @Override
                protected Void doInBackground(Void... params)
                {
                    mItems.clear();
                    String realmId = "Unknown";
                    String userId = "Unknown";
                    String url = "Unknown";
                    if (mIsRegistered && mQeo != null) {
                        long realmIdL = mQeo.getRealmId();
                        DataHandler.setNativeRealmId(realmIdL);

                        // get realm/user name from database. This will only be available if the user registered with
                        // openid
                        DBHelper dbHelper = new DBHelper(getActivity());
                        TableInfo tableInfo = new TableInfo(dbHelper);
                        String realmName = tableInfo.getValue(TableInfo.KEY_REALM_NAME);
                        String userName = tableInfo.getValue(TableInfo.KEY_REALM_USERNAME);
                        dbHelper.close();

                        realmId = format(realmName, realmIdL);
                        userId = format(userName, mQeo.getUserId());
                        url = mQeo.getRealmUrl();
                    }
                    addItem("Realm", realmId);
                    addItem("User", userId);
                    addItem("Realm management url", url);

                    return null;
                }

                private String format(String name, long id)
                {
                    if (name == null) {
                        return "Id: " + id;
                    }
                    else {
                        return name + " (0x" + Long.toHexString(id) + ")";
                    }
                }

            };
            mDataFetcher.execute();
        }

        private void leaveRealm()
        {
            new AsyncTask<Void, Void, Void>()
            {

                @Override
                protected Void doInBackground(Void... params)
                {
                    LOG.warning("Leaving realm");
                    if (mConnectionListener != null) {
                        QeoJava.closeQeo(mConnectionListener);
                    }
                    File path = new File(QeoService.getStorageDir(getActivity()));
                    LOG.fine("Path: " + path);
                    File[] files = path.listFiles();
                    if (files == null) {
                        LOG.warning("No storage dir");
                        return null;
                    }
                    for (File file : files) {
                        LOG.fine("File: " + file);
                        String name = file.getName();
                        if (name.endsWith("policy.mime") || name.equals("truststore.p12") || name.equals("url")) {
                            LOG.fine("->delete file");
                            if (!file.delete()) {
                                LOG.warning("Can't delete file " + file);
                            }
                        }
                    }
                    LOG.fine("Clear access tokens");
                    //clear access tokens too.
                    DeviceRegPref.edit().setRefreshToken(null).setSelectedRealmId(0, "").apply();
                    DataHandler.invalidateAccessToken();
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid)
                {
                    setRetainInstance(false);
                    getActivity().finish();
                }
            }.execute();
        }

        private final QeoConnectionListener mConnectionListener = new QeoConnectionListener()
        {

            private void fetchDataUI()
            {
                getActivity().runOnUiThread(new Runnable()
                {

                    @Override
                    public void run()
                    {
                        if (!mIsRegistered) {
                            Toast
                                .makeText(getActivity(), R.string.notYetRegistered, Toast.LENGTH_LONG)
                                .show();
                        }
                        fetchData();
                    }
                });
            }

            @Override
            public boolean onStartAuthentication()
            {
                LOG.fine("onStartAuthentication");
                NativeQeo.cancelRegistration();
                return true;
            }

            @Override
            public void onQeoReady(QeoFactory qeo)
            {
                LOG.fine("onQeoReady");
                mQeo = qeo;
                mIsRegistered = true;
                mQeoInitDone = true;
                fetchDataUI();
            }

            @Override
            public void onQeoError(QeoException ex)
            {
                LOG.fine("onQeoError");
                mIsRegistered = false;
                mQeoInitDone = true;
                QeoJava.closeQeo(mConnectionListener);
                fetchDataUI();
            }
        };
    }

}
