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
import java.util.logging.Logger;

import org.qeo.android.service.QeoSuspendHelper;
import org.qeo.android.service.ui.R;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.QeoManagementApp;
import org.qeo.deviceregistration.helper.DataHandler;
import org.qeo.deviceregistration.helper.RegistrationCredentialsWriterTask;
import org.qeo.deviceregistration.service.RegisterService;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

/**
 * Activity to be considered the Main for DeviceRegistration.
 */
public class ManagementActivity
    extends SherlockFragmentActivity
{

    /** Intent extra field to set start tab. */
    public static final String INTENT_EXTRA_START_TAB = "startTab";
    private static final Logger LOG = Logger.getLogger("ManagementActivity");
    private static final int REQUEST_CODE_SETTINGS = 2;
    private static final String TAB = "TAB";
    private static final String TAB_USERS = "USERS";
    /** Devices tab indicator. */
    public static final String TAB_DEVICES = "DEVICES";
    private TabHost mTabHost;
    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private LocalBroadcastManager mLbm;
    private AlertDialog mAlertDialog;
    private AsyncTask<Void, Void, Boolean> mCheckTokensTask = null;
    private GoogleApiClient mGoogleApiClient;
    private boolean mTabsAdded;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        QeoManagementApp.init(); // Initialize global variables

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_management);

        mLbm = LocalBroadcastManager.getInstance(this);
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);
        mTabsAdded = false;
        mAlertDialog = null;

        String startTab = getIntent().getStringExtra(INTENT_EXTRA_START_TAB);
        if (startTab != null) {
            mTabHost.setCurrentTabByTag(startTab);
        }
        else {
            if (savedInstanceState != null) {
                mTabHost.setCurrentTabByTag(savedInstanceState.getString(TAB));
            }
        }

        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this);
        builder.addApi(Plus.API);
        builder.addScope(Plus.SCOPE_PLUS_PROFILE);
        mGoogleApiClient = builder.build();

    }

    private void startWebviewLogin()
    {
        mLbm.registerReceiver(new WebviewLogin(), new IntentFilter(RegisterService.ACTION_LOGIN_FINISHED));
        Intent i = new Intent(this, WebViewActivity.class);
        startActivity(i);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        LOG.fine("onStart");
        QeoSuspendHelper.getInstance(this).resume();
        if (mAlertDialog != null) {
            mAlertDialog.show();
            mAlertDialog = null;
            return;
        }
        mGoogleApiClient.connect();

        //check if authentication is ok before displaying anything.
        checkAccessTokens();

        if (DataHandler.getNativeRealmId() == 0
            || (DataHandler.getNativeRealmId() == DeviceRegPref.getSelectedRealmId())) {
            // user is managing same realm as the device is in or native has no realm yet
            findViewById(R.id.activityTabscreen_textview_realmwarning).setVisibility(View.GONE);
        }
        else {
            findViewById(R.id.activityTabscreen_textview_realmwarning).setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putString(TAB, mTabHost.getCurrentTabTag());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop()
    {
        if (mCheckTokensTask != null) {
            mCheckTokensTask.cancel(true);
        }
        mGoogleApiClient.disconnect();
        QeoSuspendHelper.getInstance(this).suspend();
        super.onStop();
    }

    private void checkAccessTokens()
    {
        mCheckTokensTask = new AsyncTask<Void, Void, Boolean>()
        {

            @Override
            protected Boolean doInBackground(Void... params)
            {
                // try to get accessToken. Should normally be ok.
                if (DataHandler.getAccessToken() == null) {
                    if (!isCancelled()) {
                        LOG.warning("OAuth token invalid, starting re-login");
                        startWebviewLogin();
                        return false;
                    }
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result)
            {
                if (!result) {
                    String msg = getString(R.string.oauth_login_error_relogin);
                    Toast.makeText(ManagementActivity.this, msg, Toast.LENGTH_LONG).show();
                }
                else {
                    //ok
                    if (!mTabsAdded) {
                        mTabsAdapter.addTab(mTabHost.newTabSpec(TAB_USERS).setIndicator("Users"),
                            UserListFragment.class, null);
                        mTabsAdapter.addTab(mTabHost.newTabSpec(TAB_DEVICES).setIndicator("Devices"),
                            MultiDeviceFragment.class, null);
                        mTabsAdded = true;
                    }
                    if (DeviceRegPref.getSelectedRealmId() == 0) {
                        showNoRealmSelectedDialog();
                    }
                }
            }
        };
        mCheckTokensTask.execute();
    }

    private void showNoRealmSelectedDialog()
    {
        LOG.fine("Show select realm dialog");
        // this device is registered to qeo but the user is not an admin
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.no_realm_selected_title));
        builder.setMessage(getString(R.string.no_realm_selected));
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
        {

            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                finish();
            }
        });
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
        {

            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // realm not yet selected, do this first.
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SETTINGS);
                dialog.dismiss();
            }
        });
        builder.show();
    }

    /**
     * Adapter to fill tab data.
     */
    public static class TabsAdapter
        extends FragmentStatePagerAdapter
        implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener
    {
        private final Context mContext;
        private final TabHost mTabHost;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        /**
         * Static class showing the tabs on UI.
         */
        static final class TabInfo
        {
            private final Class<?> mClass;
            private final Bundle mParams;

            /**
             * Default constructor.
             * 
             * @param tag  .
             * @param obj  .
             * @param args .
             */
            TabInfo(String tag, Class<?> obj, Bundle args)
            {
                mClass = obj;
                mParams = args;
            }
        }

        /**
         * Create the default views to display tabs titles.
         */
        static class DummyTabFactory
            implements TabHost.TabContentFactory
        {
            private final Context mContext;

            /**
             * Default constructor.
             * 
             * @param context activity context.
             */
            public DummyTabFactory(Context context)
            {
                mContext = context;
            }

            @Override
            public View createTabContent(String tag)
            {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        /**
         * Setup the listeners for the TabHost and ViewPager.
         * 
         * @param activity .
         * @param tabHost  .
         * @param pager    .
         */
        public TabsAdapter(FragmentActivity activity, TabHost tabHost, ViewPager pager)
        {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mTabHost = tabHost;
            mViewPager = pager;
            mTabHost.setOnTabChangedListener(this);
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        /**
         * Adding a new tab to the Adapter.
         * 
         * @param tabSpec TabHost.TabSpe object.
         * @param clss    .
         * @param args    .
         */
        public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args)
        {
            tabSpec.setContent(new DummyTabFactory(mContext));
            String tag = tabSpec.getTag();

            TabInfo info = new TabInfo(tag, clss, args);
            mTabs.add(info);
            notifyDataSetChanged();
            mTabHost.addTab(tabSpec);

        }

        @Override
        public int getCount()
        {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position)
        {
            TabInfo info = mTabs.get(position);
            return Fragment.instantiate(mContext, info.mClass.getName(), info.mParams);
        }

        @Override
        public void onTabChanged(String tabId)
        {
            int position = mTabHost.getCurrentTab();
            mViewPager.setCurrentItem(position);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
        {
        }

        @Override
        public void onPageSelected(int position)
        {
            // Unfortunately when TabHost changes the current tab, it kindly
            // also takes care of putting focus on it when not in touch mode.
            // The jerk.
            // This hack tries to prevent this from pulling focus out of our
            // ViewPager.
            TabWidget widget = mTabHost.getTabWidget();
            int oldFocusability = widget.getDescendantFocusability();
            widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            mTabHost.setCurrentTab(position);
            widget.setDescendantFocusability(oldFocusability);
        }

        @Override
        public void onPageScrollStateChanged(int state)
        {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(R.menu.activity_management, menu);
        return true;
    }

    private final DialogInterface.OnClickListener mSignOutClickListener = new DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            DeviceRegPref.edit().setRefreshToken(null).setSelectedRealmId(0, "").apply();
            DataHandler.invalidateAccessToken();
            if (which == -3) { //neutral button
                //revoke
                if (mGoogleApiClient.isConnected()) {
                    Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
                }
                Toast.makeText(ManagementActivity.this, "Revoked access", Toast.LENGTH_LONG).show();
            }
            else if (which == -1) { //positive button
                //sign out
                if (mGoogleApiClient.isConnected()) {
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                }
                Toast.makeText(ManagementActivity.this, "Signed out", Toast.LENGTH_LONG).show();
            }
            else {
                throw new IllegalStateException("Unknown id: " + which);
            }

            finish();
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        if (item.getItemId() == R.id.mainActivity_menu_settings) {
            startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_CODE_SETTINGS);
            return true;
        }
        else if (item.getItemId() == R.id.mainActivity_menu_accountInfo) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle(getString(R.string.accountDetails));
            b.setNegativeButton(getString(R.string.close), null);
            b.setPositiveButton(getString(R.string.sign_out), mSignOutClickListener);
            String user = "<" + getString(R.string.unknown) + ">";
            if (mGoogleApiClient.isConnected()) {
                user = Plus.AccountApi.getAccountName(mGoogleApiClient);
                b.setNeutralButton("Revoke access", mSignOutClickListener);
            }
            b.setMessage(getString(R.string.accountDetailsMsg, user));
            b.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mLbm.registerReceiver(mRegCredReceiver, new IntentFilter(
            RegistrationCredentialsWriterTask.ACTION_REGISTRATION_QEO_DONE));
    }

    @Override
    protected void onResumeFragments()
    {
        super.onResumeFragments();
    }

    @Override
    protected void onPause()
    {
        mLbm.unregisterReceiver(mRegCredReceiver);
        super.onPause();
    }

    private final BroadcastReceiver mRegCredReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            boolean result =
                intent.getBooleanExtra(RegistrationCredentialsWriterTask.INTENT_EXTRA_REGISTRATION_RESULT, false);
            if (!result) {
                LOG.warning("Failure creating otc/registering device");
                Toast.makeText(ManagementActivity.this, "Failed to Send data over Qeo", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private class WebviewLogin
        extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            mLbm.unregisterReceiver(this); // unregister self
            boolean result = intent.getBooleanExtra(RegisterService.INTENT_EXTRA_SUCCESS, false);
            if (!result) {
                boolean error = intent.getBooleanExtra(RegisterService.INTENT_EXTRA_ERROR, false);
                if (error) {
                    // error case
                    String errorMsg = intent.getStringExtra(RegisterService.INTENT_EXTRA_ERRORMSG);
                    LOG.warning("OAuth login problem: " + errorMsg);
                    AlertDialog.Builder builder = new AlertDialog.Builder(ManagementActivity.this);
                    builder.setMessage(getString(R.string.oauth_error_msg) + (errorMsg == null ? "" : ": " + errorMsg));
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                    {

                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            ManagementActivity.this.finish();
                        }
                    });
                    mAlertDialog = builder.create();
                }
                else {
                    // just cancelled/aborted
                    finish();
                }
                return;
            }
            LOG.fine("OAuth login done");
        }
    }
}
