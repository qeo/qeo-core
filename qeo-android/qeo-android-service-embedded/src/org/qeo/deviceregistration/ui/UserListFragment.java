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

package org.qeo.deviceregistration.ui;

import java.net.HttpURLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.android.service.ui.R;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.QeoManagementApp;
import org.qeo.deviceregistration.rest.AddUserLoaderTask;
import org.qeo.deviceregistration.rest.GetUserListLoaderTask;
import org.qeo.deviceregistration.rest.RestHelper;
import org.qeo.deviceregistration.rest.RestResponse;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Fragment to display list of users from the SMS.
 */
public class UserListFragment
    extends SherlockListFragment
    implements LoaderManager.LoaderCallbacks<RestResponse>

{
    private static final String USER = "user";
    private static final Logger LOG = Logger.getLogger("UserListFragment");
    private ArrayAdapter<String> mUsersAdapter;
    private ErrorDialog mDialog;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        LOG.fine("OnStart");
    }

    @Override
    public void onResume()
    {
        super.onResume();
        LOG.fine("OnResume");
        if (DeviceRegPref.getRefreshToken() != null && DeviceRegPref.getSelectedRealmId() != 0) {
            getActivity().getSupportLoaderManager().restartLoader(QeoManagementApp.LOADER_GET_USERS_ID, null, this);
            IntentFilter filter = new IntentFilter();
            filter.addAction(RestHelper.ACTION_USER_ADDED);
            filter.addAction(RestHelper.ACTION_USER_ADDED_FAILURE);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mUserAddedListener, filter);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        mUsersAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);
        setListAdapter(mUsersAdapter);
        setListShown(false);
        mDialog =
            new ErrorDialog(getActivity(), getString(R.string.title_error_msg), getString(R.string.general_error_msg));
        setRetainInstance(true);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.tabscreen_users, menu);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mDialog.isShowing()) {
            mDialog.dismiss();
        }
        if (DeviceRegPref.getRefreshToken() != null) {
            getActivity().getSupportLoaderManager().destroyLoader(QeoManagementApp.LOADER_GET_USERS_ID);
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mUserAddedListener);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        if (item.getItemId() == R.id.menu_tabscreen_add_user) {
            showAddUserDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddUserDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.action_add_user);
        // builder.setMessage("Message");

        // Set an EditText view to get user input
        final EditText input = new EditText(getActivity());
        input.setHint(R.string.username);
        builder.setView(input);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                Bundle args = new Bundle();
                args.putString(USER, input.getText().toString());

                getActivity().getSupportLoaderManager().restartLoader(QeoManagementApp.LOADER_ADD_USER_ID, args,
                    UserListFragment.this);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    @Override
    public Loader<RestResponse> onCreateLoader(int id, Bundle bundle)
    {
        if (id == QeoManagementApp.LOADER_ADD_USER_ID) {
            setListShown(false);
            String userName = bundle.getString(USER);
            return new AddUserLoaderTask(getActivity(), userName);
        }
        else {
            setListShown(false);
            return new GetUserListLoaderTask(getActivity());
        }
    }

    @Override
    public void onLoadFinished(Loader<RestResponse> restResponseLoader, RestResponse restResponse)
    {
        if (restResponseLoader.getId() == QeoManagementApp.LOADER_GET_USERS_ID) {
            try {

                int code = restResponse.getCode();

                if (code == HttpURLConnection.HTTP_OK) {
                    mUsersAdapter.clear();
                    for (String i : restResponse.getListData()) {
                        mUsersAdapter.add(i);
                    }
                    mUsersAdapter.notifyDataSetChanged();
                }
                else {
                    if (DeviceRegPref.getSelectedRealmId() == 0) {
                        mUsersAdapter.clear();
                    }
                    mDialog.setErrorMessage(restResponse.getData());
                    mDialog.show();

                }
                setListShown(true);
            }
            catch (Exception e) {
                LOG.log(Level.WARNING, "Error getting users", e);
                mDialog.setErrorMessage(getString(R.string.userlist_error_msg));
                mDialog.show();
            }
        }

    }

    @Override
    public void onLoaderReset(Loader<RestResponse> restResponseLoader)
    {
        mUsersAdapter.clear();
    }

    private final BroadcastReceiver mUserAddedListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(RestHelper.ACTION_USER_ADDED_FAILURE)) {
                // failure
                String username = intent.getStringExtra(RestHelper.INTENT_EXTRA_USERNAME);
                String msg = intent.getStringExtra(RestHelper.INTENT_EXTRA_ERRORMSG);
                int code = intent.getIntExtra(RestHelper.INTENT_EXTRA_ERRORCODE, 0);
                LOG.warning("Error adding user " + username);
                AlertDialog.Builder alerDialog = new AlertDialog.Builder(getActivity());
                alerDialog.setTitle(R.string.title_error_msg);
                alerDialog.setMessage("Error adding user " + username + " (" + code + ")\n" + msg);
                alerDialog.setPositiveButton(android.R.string.ok, null);
                alerDialog.show();
            }

            LOG.fine("User added, refreshing user list");
            getActivity().getSupportLoaderManager().restartLoader(QeoManagementApp.LOADER_GET_USERS_ID, null,
                UserListFragment.this);

        }
    };

}
