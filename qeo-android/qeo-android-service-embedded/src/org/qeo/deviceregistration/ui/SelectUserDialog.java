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

package org.qeo.deviceregistration.ui;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.qeo.android.service.ui.R;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.QeoManagementApp;
import org.qeo.deviceregistration.helper.RealmCache;
import org.qeo.deviceregistration.helper.RegistrationCredentialsWriterTask;
import org.qeo.deviceregistration.helper.UnRegisteredDeviceModel;
import org.qeo.deviceregistration.rest.AddUserLoaderTask;
import org.qeo.deviceregistration.rest.GetUserListLoaderTask;
import org.qeo.deviceregistration.rest.RestResponse;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Dialog to let user select a user.
 */
public class SelectUserDialog
    extends DialogFragment
    implements OnItemClickListener, OnClickListener, LoaderManager.LoaderCallbacks<RestResponse>
{
    private static final Logger LOG = Logger.getLogger("SelectUserDialog");
    private ArrayAdapter<String> mAdapter;
    private final List<String> mUsers = new LinkedList<String>();;
    private static final String DEVICE = "DEVICE";
    private static final String USER = "user";
    private ListView mListview;
    private ProgressBar mProgressBar;
    private UnRegisteredDeviceModel mDevice;
    private Context mAppContext;

    /**
     * Create an instance of this dialog.
     * 
     * @param device the unregistered device
     * @return The dialog
     */
    public static SelectUserDialog getInstance(UnRegisteredDeviceModel device)
    {
        LOG.fine("Creating selectUserDialog");
        SelectUserDialog dialog = new SelectUserDialog();
        Bundle args = new Bundle();
        args.putParcelable(DEVICE, device);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(false);
        mAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_single_choice, mUsers);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        getDialog().setTitle(R.string.select_user_title);

        View v = inflater.inflate(R.layout.fragment_select_user, container);
        Button b = (Button) v.findViewById(R.id.fragment_select_user_button_add_user);
        b.setOnClickListener(this);
        mListview = (ListView) v.findViewById(R.id.fragment_select_user_list);
        mListview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListview.setAdapter(mAdapter);
        mListview.setOnItemClickListener(this);
        mProgressBar = (ProgressBar) v.findViewById(R.id.fragment_select_user_progressBar);
        mListview.setVisibility(View.GONE);
        TextView infoView = (TextView) v.findViewById(R.id.fragment_select_user_textview_info);

        Bundle args = getArguments();
        mDevice = args.getParcelable(DEVICE);

        String msg =
            "Adding device " + mDevice.getUserFriendlyName() + "(" + mDevice.getUserName() + ")" + " to Qeo realm "
                + DeviceRegPref.getSelectedRealm();
        infoView.setText(msg);

        // Why can't the LOADER_GET_USERS_ID be used? it does not call the onLoadFinished again, strange.
        getActivity().getSupportLoaderManager().restartLoader(QeoManagementApp.LOADER_GET_USERS_ID2, null, this);

        return v;
    }

    private void selectUser(String name)
    {
        LOG.fine("Trying to select user " + name);
        // select user if already known
        int index = mUsers.indexOf(name);
        if (index != -1) {
            LOG.fine("Found at position " + index);
            mListview.setItemChecked(index, true);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        String username = mUsers.get(position);
        onUserSelected(username);
    }

    @Override
    public void onDestroy()
    {
        LOG.finest("onDestroy");
        super.onDestroy();
        if (DeviceRegPref.getRefreshToken() != null) {
            getActivity().getSupportLoaderManager().destroyLoader(QeoManagementApp.LOADER_ADD_USER_ID2);
            getActivity().getSupportLoaderManager().destroyLoader(QeoManagementApp.LOADER_GET_USERS_ID2);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        mAppContext = getActivity().getApplicationContext();
    }

    private void onUserSelected(String username)
    {
        LOG.fine("Selected user for registration: " + username);
        // fetch the realm userId
        long userId = RealmCache.getUserId(username);
        long realmId = DeviceRegPref.getSelectedRealmId();

        RegistrationCredentialsWriterTask task =
            new RegistrationCredentialsWriterTask(mAppContext, mDevice, realmId, userId);
        QeoManagementApp.getGlobalExecutor().submitTaskToPool(task);
        // close dialog
        getDialog().dismiss();
    }

    @Override
    public void onClick(View v)
    {
        // only add user button defines this onClick, no need to check for id.
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.action_add_user);
        // builder.setMessage("Message");

        // Set an EditText view to get user input
        final EditText input = new EditText(getActivity());
        input.setHint(R.string.username);
        input.setText(mDevice.getUserName());
        builder.setView(input);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                Bundle args = new Bundle();
                args.putString(USER, input.getText().toString());

                mProgressBar.setVisibility(View.VISIBLE);
                mListview.setVisibility(View.GONE);
                getActivity().getSupportLoaderManager().restartLoader(QeoManagementApp.LOADER_ADD_USER_ID2, args,
                    SelectUserDialog.this);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    @Override
    public Loader<RestResponse> onCreateLoader(int id, Bundle args)
    {
        if (id == QeoManagementApp.LOADER_ADD_USER_ID2) {
            // add user loader
            return new AddUserLoaderTask(getActivity(), args.getString(USER));
        }
        else {

            // get users loader
            LOG.fine("Create user list loader");
            return new GetUserListLoaderTask(getActivity());
        }
    }

    @Override
    public void onLoadFinished(Loader<RestResponse> loader, RestResponse data)
    {
        if (loader.getId() == QeoManagementApp.LOADER_ADD_USER_ID2) {
            LOG.fine("Loader finished: User added");
            // add user loader
            if (data.getCode() >= 200 && data.getCode() < 300) {
                LOG.fine("User added correctly ");
                onUserSelected(data.getData());
            }
            else {
                LOG.warning("Error adding the user: " + data.getData());
            }
        }
        else if (loader.getId() == QeoManagementApp.LOADER_GET_USERS_ID2) {
            // get users loader
            LOG.fine("Loader finished: loading users");
            mUsers.clear();
            mUsers.addAll(data.getListData());
            Collections.sort(mUsers);
            mAdapter.notifyDataSetChanged();
            mProgressBar.setVisibility(View.GONE);
            mListview.setVisibility(View.VISIBLE);
            selectUser(mDevice.getUserName());

        }
    }

    @Override
    public void onLoaderReset(Loader<RestResponse> loader)
    {
        LOG.finest("onLoaderReset: " + loader.getId());
    }
}
