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

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.QeoFactory;
import org.qeo.android.service.LocalServiceConnection;
import org.qeo.android.service.ui.R;
import org.qeo.android.service.ServiceApplication;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.QeoManagementApp;
import org.qeo.deviceregistration.helper.ErrorCode;
import org.qeo.deviceregistration.helper.RegistrationStatusCode;
import org.qeo.deviceregistration.helper.UnRegisteredDeviceModel;
import org.qeo.deviceregistration.service.RemoteDeviceRegistration;
import org.qeo.exception.QeoException;
import org.qeo.java.QeoConnectionListener;
import org.qeo.system.RegistrationRequest;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;

/**
 * Fragment to display list of devices waiting remote registration.
 */
public class UnRegisteredDeviceListFragment
    extends SherlockListFragment
{
    private static final Logger LOG = Logger.getLogger("UnRegisteredDevicesFragment");
    /** Contains device that should be directly selected for registration. */
    public static final String INTENT_EXTRA_DEVICE_TO_REGISTER = "deviceToRegister";
    private final ArrayList<UnRegisteredDeviceModel> mDevices = new ArrayList<UnRegisteredDeviceModel>();
    private ErrorDialog mDialog;
    private UnRegisteredDeviceListAdapter mAdapter;
    private LocalBroadcastManager mLbm;
    private RemoteDeviceRegistration mDeviceRegisterService;
    private LocalServiceConnection mLocalServiceConnection;
    private boolean mQeoClosed;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // retainInstance is done in the parent fragment: MultiDeviceFragment
        super.onCreate(savedInstanceState);
        mLbm = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
        initQeo();
        mDeviceRegisterService = RemoteDeviceRegistration.getInstance();

        Intent i = getActivity().getIntent();
        if (i != null) {
            Bundle args = i.getExtras();
            if (args != null) {
                UnRegisteredDeviceModel model = args.getParcelable(INTENT_EXTRA_DEVICE_TO_REGISTER);
                if (model != null) {
                    // started from notification
                    showRegisterDeviceDialog(model);
                }
            }
        }

    }

    @Override
    public void onDestroy()
    {
        if (null != mLocalServiceConnection) {
            mLocalServiceConnection.close();
        }
        super.onDestroy();
    }

    private void initQeo()
    {
        if (DeviceRegPref.getSelectedRealmId() != 0) {
            // user has already selected a realm to manage. Only start if that's the case, otherwise you can't register
            // a device anyway
            mQeoClosed = false;
            mLocalServiceConnection = new LocalServiceConnection(getActivity(), mServiceConnection);
        }
        else {
            // no realm selected yet. Mark as closed so onResume will retry
            mQeoClosed = true;
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        // warning: getActivity() will return the old destroyed if using sub-fragments and setRetainInstanceState!
        // mActivity = (ManagementActivity) getParentFragment().getActivity();
        mDialog =
            new ErrorDialog(getActivity(), getString(R.string.title_error_msg), getString(R.string.general_error_msg));
        mAdapter = new UnRegisteredDeviceListAdapter(this.getActivity(), R.layout.unregistereddevicelist, mDevices);
        setListAdapter(null);
        setListAdapter(mAdapter);
        if (DeviceRegPref.getVersionDialogStatus()) {
            mDialog.setErrorMessage(getString(R.string.version_mismatch_error_msg));
            mDialog.show();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (mQeoClosed) {
            // means qeo service closed unexpected at some point, recover.
            initQeo();
        }
        mLbm.registerReceiver(mReceiver, new IntentFilter(RemoteDeviceRegistration.ACTION_UNREGISTERED_DEVICE_FOUND));
        updateDevicesList();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mLbm.unregisterReceiver(mReceiver);

        if (mDialog.isShowing()) {
            mDialog.dismiss();
        }

    }

    private void updateDevicesList()
    {
        LOG.fine("Update devicelist");
        if (mDeviceRegisterService != null) {
            mDevices.clear();
            for (RegistrationRequest req : mDeviceRegisterService.getUnregisteredDevices()) {
                UnRegisteredDeviceModel u = new UnRegisteredDeviceModel(req);
                if (u.getRegistrationStatus() != RegistrationStatusCode.REGISTERED) {
                    mDevices.add(u);
                }
                if (u.getErrorCode() != ErrorCode.NONE) {
                    createNotification(u.getErrorCode().name() + "\t" + u.getErrorMessage());
                }
            }
            mAdapter.notifyDataSetChanged();
            getListView().invalidateViews();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        LOG.fine("onListItemClick: " + position);
        UnRegisteredDeviceModel u = mAdapter.getItem(position);
        mAdapter.notifyDataSetChanged();
        getListView().invalidateViews();
        QeoManagementApp.setLastSelectedDevice(u);

        if (u.getVersion() != QeoManagementApp.CURRENT_OTC_ENCRYPT_VERSION) {
            mDialog.setErrorMessage(getString(R.string.version_mismatch_error_msg));
            mDialog.show();
            DeviceRegPref.edit().setVersionDialogStatus(true).apply();
        }
        else {
            showRegisterDeviceDialog(u);
        }
    }

    private void showRegisterDeviceDialog(UnRegisteredDeviceModel device)
    {
        LOG.fine("showUsersListDialog");
        DialogFragment newFragment = SelectUserDialog.getInstance(device);
        newFragment.show(getChildFragmentManager(), "dialog");
    }

    /**
     * Creates notification to be shown if device registration fails.
     * 
     * @param errorMessage ErrorMessage to show if device fails to register.
     */
    public void createNotification(String errorMessage)
    {
        NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(this.getSherlockActivity()).setSmallIcon(R.drawable.ic_stat_qeo)
                .setContentTitle("Device Registration Failed").setContentText(errorMessage).setAutoCancel(true);
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
            (NotificationManager) this.getSherlockActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        Intent resultIntent = new Intent();
        resultIntent.setClass(getSherlockActivity(), ManagementActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Because clicking the notification opens a new ("special") activity, there's
        // no need to create an artificial back stack.
        PendingIntent resultPendingIntent =
            PendingIntent.getActivity(this.getSherlockActivity(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        // Builds the notification and issues it.
        mNotifyMgr.notify(ServiceApplication.NOTIFICATION_REMOTE_REGISTRATION_FAILED, mBuilder.build());
    }

    /** broadcastreceiver that will be triggered if a new unregistered device is found. */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            LOG.fine("onReceive");
            if (intent.getBooleanExtra(RemoteDeviceRegistration.INTENT_EXTRA_RESULT, false)) {
                updateDevicesList();
            }
            else {
                String errorMsg = intent.getStringExtra(RemoteDeviceRegistration.INTENT_EXTRA_ERRORMSG);
                mDialog.setErrorMessage(getString(R.string.qeo_service_error_msg) + "\n" + errorMsg);
                mDialog.show();
            }

        }
    };

    private final QeoConnectionListener mServiceConnection = new QeoConnectionListener() {

        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            // nothing needed, will send broadcasts if needed.
        }

        @Override
        public void onQeoError(QeoException ex)
        {
            LOG.log(Level.WARNING, "Got QeoException", ex);
        }

    };

}
