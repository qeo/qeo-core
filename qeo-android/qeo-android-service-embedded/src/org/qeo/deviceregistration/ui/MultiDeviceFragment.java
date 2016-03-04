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

import java.util.logging.Logger;

import org.qeo.android.service.ui.R;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Fragment class displaying unRegistered and Registered devices list.
 */
public class MultiDeviceFragment
    extends SherlockFragment
{
    private static final Logger LOG = Logger.getLogger("MultiDeviceFragment");
    private UnRegisteredDeviceListFragment mUnRegFrag;
    private RegisteredDeviceListFragment mRegFrag;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        LOG.fine("onCreate");
        setHasOptionsMenu(true);
        setRetainInstance(true); // retain instance to avoid reconnecting to Qeo
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.device_fragment, container, false);
        mUnRegFrag =
            (UnRegisteredDeviceListFragment) getChildFragmentManager().findFragmentById(R.id.unregistered_frag);
        mRegFrag =
            (RegisteredDeviceListFragment) getChildFragmentManager().findFragmentById(R.id.registered_frag);
        if (null == mUnRegFrag) {
            mUnRegFrag = new UnRegisteredDeviceListFragment();
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();

            transaction.add(R.id.unregistered_frag, mUnRegFrag).addToBackStack(null).commit();
        }

        if (null == mRegFrag) {
            mRegFrag = new RegisteredDeviceListFragment();
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();

            transaction.add(R.id.registered_frag, mRegFrag).addToBackStack(null).commit();
        }

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.tabscreen_devices, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        if (item.getItemId() == R.id.menu_tabscreen_refresh) {
            // trigger a refresh of the already registered devices.
            Toast.makeText(getSherlockActivity(), "Refreshing DeviceList", Toast.LENGTH_SHORT).show();
            if (mRegFrag != null) {
                mRegFrag.refresh();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
