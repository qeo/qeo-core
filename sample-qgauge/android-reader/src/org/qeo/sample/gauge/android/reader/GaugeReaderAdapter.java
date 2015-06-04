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

package org.qeo.sample.gauge.android.reader;

import java.util.ArrayList;

import org.qeo.sample.gauge.android.reader.interfaces.Device;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * GaugeReaderAdapter displays the list of all publisher devices publishing traffic stats over Qeo.
 *
 *
 */
public class GaugeReaderAdapter
        extends ArrayAdapter<Device>
{
    private final Context mContext;
    private final int mTextViewResourceId;

    /**
     * Constructs a GaugeReaderAdapter.
     *
     * @param context Activitycontext
     * @param textViewResourceId resourceid of textview
     * @param devices List of devices
     */
    public GaugeReaderAdapter(Context context, int textViewResourceId, ArrayList<Device> devices)
    {
        super(context, textViewResourceId, devices);
        mContext = context;
        mTextViewResourceId = textViewResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View root;
        final Device device = getItem(position);

        if (null == convertView) {
            root = LayoutInflater.from(mContext).inflate(mTextViewResourceId, (ViewGroup) convertView);
        }
        else {
            root = convertView;
        }

        TextView deviceName = (TextView) root.findViewById(R.id.device_name);
        TextView deviceDescription = (TextView) root.findViewById(R.id.device_description);
        // ImageButton deviceType = (ImageButton) root.findViewById(R.id.device_type_icon);

        deviceName.setText(device.getName());
        deviceDescription.setText(device.getDescription());

        return root;
    }
}
