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

import java.util.List;

import org.qeo.android.service.ui.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * This class contains the functionality to show the list of registered devices.
 */
public class RegisteredDeviceListAdapter
    extends BaseAdapter
{

    // Declare Variables
    private final Context mContext;
    private final List<String> mDevicelist;
    private LayoutInflater mInflater;

    /**
     * Default constructor.
     * 
     * @param context - activity context.
     * @param deviceList - List of registered device list to show on UI.
     */
    public RegisteredDeviceListAdapter(Context context, List<String> deviceList)
    {
        this.mContext = context;
        this.mDevicelist = deviceList;
    }

    @Override
    public Object getItem(int position)
    {
        if (this.mDevicelist != null) {
            return this.mDevicelist.get(position);
        }
        else {
            return null;
        }
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder mViewHolder = null;

        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            mViewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.registereddevicelist, parent, false);
            mViewHolder.devicetxtView = (TextView) convertView.findViewById(R.id.registered_device_name);
            mViewHolder.deviceStateImgView = (ImageView) convertView.findViewById(R.id.registered_device_state);

            // Capture position and set to the TextViews
            mViewHolder.devicetxtView.setText(mDevicelist.get(position));

            // Capture position and set to the ImageView
            mViewHolder.deviceStateImgView.setImageResource(R.drawable.device_reg_success);
            convertView.setTag(mViewHolder);
        }
        else {
            mViewHolder = (ViewHolder) convertView.getTag();
            mViewHolder.devicetxtView.setText(mDevicelist.get(position));
            mViewHolder.deviceStateImgView.setImageResource(R.drawable.device_reg_success);
        }
        return convertView;
    }

    @Override
    public int getCount()
    {
        return mDevicelist.size();
    }

    /**
     * Class to hold the views to avoid recreation of these views if not required.
     */
    static class ViewHolder
    {
        /**
         * Text view to show Device details.
         */
        public TextView devicetxtView;
        /**
         * Image view to show Device registered status icon.
         */
        public ImageView deviceStateImgView;
    }
}
