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

package org.qeo.sample.gauge.android.reader.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qeo.sample.gauge.NetworkInterfaceSpeedData;
import org.qeo.sample.gauge.android.reader.GaugeReaderApplication;
import org.qeo.sample.gauge.android.reader.R;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TableRow;

/**
 * Fragment class containing the UI part to display the list of interfaces and speedgraph data.
 */
public class InterfaceListFragment
        extends Fragment
{
    private Map<String, SpeedGraphView> mGraphMap = new HashMap<String, SpeedGraphView>();
    private static final String EXTRA_ORIENTATION_STATE = "Orientation";
    private LinearLayout mScrollView;
    private Activity mActivity;

    /**
     * Create a new instance of InterfaceListFragment, initialized to show the interface details for the device at
     * 'index'.
     * 
     * @param index position of selected device
     * @return returns InterfaceListFragment object
     */

    public static InterfaceListFragment newInstance(int index)
    {
        InterfaceListFragment f = new InterfaceListFragment();

        Bundle args = new Bundle();
        args.putInt("index", index);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        GaugeReaderApplication.sCurrentOrientation = getResources().getConfiguration().orientation;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {

        View v = inflater.inflate(R.layout.interface_details_fragment, container, false);
        mScrollView = (LinearLayout) v.findViewById(R.id.fragment_device_details_scrollview);

        if (savedInstanceState != null) {
            GaugeReaderApplication.sLastOrientation = savedInstanceState.getInt(EXTRA_ORIENTATION_STATE);
        }

        refreshUIData();

        return v;
    }

    /**
     * Creates a new graph for a given interface name.
     * 
     * @param ifname The interface name.
     * @return the newly created speed graph.
     */
    private SpeedGraphView createNewGraphView(String ifname)
    {
        SpeedGraphView speedGraph = new SpeedGraphView(mActivity, ifname);
        LayoutParams params = new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f);
        mScrollView.addView(speedGraph, params);
        return speedGraph;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mScrollView != null) {
            mScrollView.removeAllViews();
        }
        if (mGraphMap.size() != 0) {
            mGraphMap.clear();
        }
        mGraphMap = null;
        mScrollView = null;
    }

    /**
     * Refreshes the Fragment data.
     * 
     * @param activity reference of activity
     */
    public void updateTrafficStatsData(Activity activity)
    {
        mActivity = activity;
        refreshUIData();

    }

    private void refreshUIData()
    {
        final List<NetworkInterfaceSpeedData> msgDataList = GaugeReaderApplication.sDataListForClickedIface;
        final List<String> interfaceList = GaugeReaderApplication.sInterfaceList;

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run()
            {

                // update UI for data received from reader

                if (msgDataList != null && msgDataList.size() != 0) {
                    for (NetworkInterfaceSpeedData m : msgDataList) {

                        if (mGraphMap.get(m.getIfaceName()) == null) {
                            // This interface is not known yet. We need to create a control for it.
                            mGraphMap.put(m.getIfaceName(), createNewGraphView(m.getIfaceName()));
                        }
                        mGraphMap.get(m.getIfaceName()).addNewSpeedData((int) m.getKbpsIn(), (int) m.getKbpsOut());

                        GaugeReaderApplication.sIfaceGraphHelperMap.put(m.getIfaceName(),
                                mGraphMap.get(m.getIfaceName()).getIfaceCurerntDetails());

                    }
                }
                // update UI for interfaces from removed publisher

                if (interfaceList != null && interfaceList.size() != 0) {
                    for (String key : interfaceList) {
                        if (mGraphMap.get(key) != null) {
                            mScrollView.removeView(mGraphMap.get(key));
                            mGraphMap.remove(key);
                        }
                    }
                }

                mScrollView.invalidate();
            }
        });
        GaugeReaderApplication.sDataListForClickedIface.clear();
        GaugeReaderApplication.sInterfaceList.clear();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_ORIENTATION_STATE, GaugeReaderApplication.sCurrentOrientation);

    }
}
