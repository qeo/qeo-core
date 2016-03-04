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

import org.qeo.android.service.ui.R;
import org.qeo.deviceregistration.QeoManagementApp;
import org.qeo.deviceregistration.helper.ErrorCode;
import org.qeo.deviceregistration.helper.RegistrationStatusCode;
import org.qeo.deviceregistration.helper.UnRegisteredDeviceModel;
import org.qeo.deviceregistration.model.UnRegisteredDevice;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Adapter to populate list of unregistered devices in the realm (devices found over open domain).
 */
public class UnRegisteredDeviceListAdapter
    extends ArrayAdapter<UnRegisteredDeviceModel>
{
    // Declare Variables
    private final Context mContext;
    private final int mResource;

    /**
     * Create an instance.
     * 
     * @param context The context.
     * @param resource resourceId.
     * @param devices list of devices.
     */
    public UnRegisteredDeviceListAdapter(Context context, int resource, ArrayList<UnRegisteredDeviceModel> devices)
    {
        super(context, resource, devices);
        this.mContext = context;
        this.mResource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {

        ViewHolder mViewHolder = null;
        final UnRegisteredDevice device = getItem(position);

        if (convertView == null) {
            mViewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(mResource, null);
            mViewHolder.mDevicetxtView = (TextView) convertView.findViewById(R.id.unregistered_device_name);
            mViewHolder.mUserTxtView = (TextView) convertView.findViewById(R.id.user_name);
            mViewHolder.mDeviceStateImgView = (ImageView) convertView.findViewById(R.id.unregistered_device_state);
        }
        else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }

        // Capture position and set to the TextViews
        mViewHolder.mUserTxtView.setText(device.getUserFriendlyName());
        mViewHolder.mDevicetxtView.setText(device.getUserName());
        // Capture position and set to the ImageView

        UnRegisteredDeviceModel u = getItem(position);
        RegistrationStatusCode status = u.getRegistrationStatus();

        if (status == RegistrationStatusCode.UNREGISTERED && u.getErrorCode() == ErrorCode.NONE
            && u.getVersion() == QeoManagementApp.CURRENT_OTC_ENCRYPT_VERSION) {
            mViewHolder.mDeviceStateImgView.setImageResource(R.drawable.device_unreg);
        }
        else if (status == RegistrationStatusCode.UNREGISTERED && u.getErrorCode() != ErrorCode.NONE) {
            mViewHolder.mDeviceStateImgView.setImageResource(R.drawable.device_reg_fail);
        }
        else if (status == RegistrationStatusCode.REGISTERING) {
            mViewHolder.mDeviceStateImgView.setImageResource(R.drawable.device_registering);
        }
        else if (u.getVersion() != QeoManagementApp.CURRENT_OTC_ENCRYPT_VERSION) {
            mViewHolder.mDeviceStateImgView.setImageResource(R.drawable.device_reg_fail);
        }
        convertView.setTag(mViewHolder);

        return convertView;
    }

    /**
     * Class to hold the views to avoid recreation of these views if not required.
     */
    static class ViewHolder
    {
        /**
         * User text view.
         */
        public TextView mUserTxtView;

        /**
         * Device text view widget.
         */
        public TextView mDevicetxtView;

        /**
         * Device image view widget.
         */
        public ImageView mDeviceStateImgView;
    }
}
