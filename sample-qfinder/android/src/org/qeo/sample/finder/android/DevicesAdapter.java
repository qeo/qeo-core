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

package org.qeo.sample.finder.android;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.qeo.system.DeviceId;
import org.qeo.system.DeviceInfo;

import java.util.List;
import java.util.UUID;

/**
 * Adapter for Qeo devices.
 */
public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder>
{
    private final List<DeviceInfo> mDevices;
    private final DeviceClickListener mListener;
    private DeviceId mOwnDeviceId;
    private final int mColorWhite;
    private final int mColorRed;

    /**
     * Viewholder.
     */
    final class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        /** Title field. */
        final TextView mTitle;
        /** SubTitle field. */
        final TextView mSubTitle;

        /**
         * Create a new instance.
         * @param root The root view.
         */
        public ViewHolder(View root)
        {
            super(root);
            mTitle = (TextView) root.findViewById(android.R.id.text1);
            mSubTitle = (TextView) root.findViewById(android.R.id.text2);
            mSubTitle.setTextColor(root.getContext().getResources().getColor(android.R.color.darker_gray));
            root.setOnClickListener(this);
        }

        @Override
        public void onClick(View v)
        {
            onDeviceClick(getAdapterPosition());
        }
    }

    /**
     * Create a new instance.
     * @param ctx Android context.
     * @param devices the list of devices.
     * @param listener A listener for click feedback.
     */
    public DevicesAdapter(Context ctx, List<DeviceInfo> devices, DeviceClickListener listener)
    {
        mDevices = devices;
        mListener = listener;
        mColorWhite = ctx.getResources().getColor(android.R.color.white);
        mColorRed = ctx.getResources().getColor(android.R.color.holo_red_dark);
    }

    private void onDeviceClick(int position)
    {
        mListener.onItemClicked(mDevices.get(position));
    }

    /**
     * Set the deviceIf for this device in order to mark it.
     * @param deviceId the deviceId.
     */
    public void setOwnDeviceId(DeviceId deviceId)
    {
        mOwnDeviceId = deviceId;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View root = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent,
            false);
        ViewHolder vh = new ViewHolder(root);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        DeviceInfo device = mDevices.get(position);
        if (device.deviceId.equals(mOwnDeviceId)) {
            holder.mTitle.setTextColor(mColorRed);
        }
        else {
            holder.mTitle.setTextColor(mColorWhite);
        }
        holder.mTitle.setText(device.userFriendlyName);
        String deviceId = new UUID(device.deviceId.upper, device.deviceId.lower).toString();
        holder.mSubTitle.setText(deviceId);
    }

    @Override
    public int getItemCount()
    {
        return mDevices.size();
    }

    /**
     * Interface for adapter callbacks.
     */
    public interface DeviceClickListener
    {
        /**
         * When an item is clicked.
         * @param deviceInfo The deviceInfo.
         */
        void onItemClicked(DeviceInfo deviceInfo);
    }
}
