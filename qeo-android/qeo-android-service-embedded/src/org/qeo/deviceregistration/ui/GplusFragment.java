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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.android.service.ui.R;
import org.qeo.android.service.ServiceApplication;
import org.qeo.deviceregistration.service.OAuthTokenService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

/**
 * Google plus login fragment.
 */
public class GplusFragment
    extends SherlockFragment
    implements OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
    View.OnTouchListener
{

    private static final Logger LOG = Logger.getLogger("GplusFragment");

    // private static final int DIALOG_GET_GOOGLE_PLAY_SERVICES = 1;
    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_CODE_GET_GOOGLE_PLAY_SERVICES = 2;

    private GoogleApiClient mGoogleApiClient;

    private Button mGplusButtonSignIn;
    private Button mButtonOther;
    private ConnectionResult mConnectionResult;
    private TextView mTextViewStatus;
    private boolean mDowntouch = false;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        LOG.finest("onCreate");
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(getActivity());
        builder.addApi(Plus.API);
        builder.addScope(Plus.SCOPE_PLUS_PROFILE);
        builder.addConnectionCallbacks(this);
        builder.addOnConnectionFailedListener(this);
        mGoogleApiClient = builder.build();
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        LOG.finest("onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_gplus, container, false);

        mGplusButtonSignIn = (Button) rootView.findViewById(R.id.fragmentGplus_Button_signIn);
        mGplusButtonSignIn.setOnClickListener(this);
        mGplusButtonSignIn.setEnabled(false);
        mGplusButtonSignIn.setOnTouchListener(this);
        mButtonOther = (Button) rootView.findViewById(R.id.fragmentGplus_Button_other);
        mButtonOther.setOnClickListener(this);
        mTextViewStatus = (TextView) rootView.findViewById(R.id.fragmentGplus_TextView_Status);
        mTextViewStatus.setVisibility(View.INVISIBLE);

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        LOG.fine("OnActivityResult: " + requestCode + " -- " + resultCode);
        if (requestCode == REQUEST_CODE_SIGN_IN || requestCode == REQUEST_CODE_GET_GOOGLE_PLAY_SERVICES) {
            if (resultCode == Activity.RESULT_CANCELED) {
                updateButtons(false);
                getActivity().setProgressBarIndeterminateVisibility(false);
            }
            else if (resultCode == Activity.RESULT_OK) {
                LOG.fine("OnActivityResult: RESULT_OK");
                if (!mGoogleApiClient.isConnected()) {
                    LOG.fine("OnActivityResult: Google api not yet connected");
                    if (!mGoogleApiClient.isConnecting()) {
                        LOG.fine("OnActivityResult: start google api connect");
                        // This time, connect should succeed.
                        mGoogleApiClient.connect();
                    }
                    else {
                        LOG.fine("OnActivityResult: Google api connect already in progress");
                    }
                }
            }
        }
    }

    @Override
    public void onClick(View view)
    {
        LOG.finest("onClick");

        int id = view.getId();
        if (id == R.id.fragmentGplus_Button_signIn) {
            int available = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
            if (available != ConnectionResult.SUCCESS) {
                LOG.warning("Google play services error: " + available);
                if (GooglePlayServicesUtil.isUserRecoverableError(available)) {
                    GooglePlayServicesUtil.showErrorDialogFragment(available, getActivity(),
                        REQUEST_CODE_GET_GOOGLE_PLAY_SERVICES);
                }
                new AlertDialog.Builder(getActivity())
                    .setMessage(getString(R.string.google_play_service_not_supported)).setCancelable(true).show();
                return;
            }

            try {
                LOG.info("Signing in to Google plus");
                mGplusButtonSignIn.setEnabled(false);
                mGplusButtonSignIn.setBackgroundResource(R.drawable.common_signin_btn_text_disabled_dark);
                mTextViewStatus.setVisibility(View.VISIBLE);
                mTextViewStatus.setText("Signing in to Google+");
                getActivity().setProgressBarIndeterminateVisibility(true);
                mConnectionResult.startResolutionForResult(getActivity(), REQUEST_CODE_SIGN_IN);
            }
            catch (IntentSender.SendIntentException e) {
                // Fetch a new result to start.
                mGoogleApiClient.connect();
            }
        }
        else if (id == R.id.fragmentGplus_Button_other) {
            WebviewFragment fragment = new WebviewFragment();
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();

            // Replace whatever is in the fragment_container view with this fragment,
            // and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.containerSingleFragment, fragment);
            transaction.addToBackStack(null);

            // Commit the transaction
            transaction.commit();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        //react to touch events for sign in button
        if (v.getId() == mGplusButtonSignIn.getId()) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mGplusButtonSignIn.setBackgroundResource(R.drawable.common_signin_btn_text_focus_dark);
                mDowntouch = true;
            }
            else if (event.getAction() == MotionEvent.ACTION_UP) {
                mGplusButtonSignIn.setBackgroundResource(R.drawable.common_signin_btn_text_dark);
                if (mDowntouch) {
                    mDowntouch = false;
                    v.performClick();
                }
            }
        }
        return false;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        LOG.finest("onStart");
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiverLogin,
            new IntentFilter(OAuthTokenService.ACTION_OAUTH_TOKEN_READY));
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop()
    {
        LOG.finest("onStop");
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiverLogin);
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint)
    {
        LOG.fine("onConnected");
        updateButtons(true); // signed in
        getServerToken();
    }

    @Override
    public void onConnectionSuspended(int cause)
    {
        LOG.fine("onConnectionsSuspended");
        // mSignInStatus.setText(R.string.loading_status);
        mGoogleApiClient.connect();
        updateButtons(false); // signed out
    }

    @Override
    public void onConnectionFailed(ConnectionResult result)
    {
        LOG.fine("onConnectionFailed");
        mConnectionResult = result;
        mGplusButtonSignIn.setEnabled(true);
        updateButtons(false); // signed out
    }

    private void updateButtons(boolean isSignedIn)
    {
        LOG.fine("UpdateButtons: " + isSignedIn + " -- " + mConnectionResult);
        if (isSignedIn) {
            //mGplusButtonSignIn.setVisibility(View.INVISIBLE);
        }
        else {
            if (mConnectionResult == null) {
                // Disable the sign-in button until onConnectionFailed is called with result.
                mGplusButtonSignIn.setVisibility(View.INVISIBLE);
                // mSignInStatus.setText(getString(R.string.loading_status));
            }
            else {
                // Enable the sign-in button since a connection result is available.
                mGplusButtonSignIn.setVisibility(View.VISIBLE);
                mGplusButtonSignIn.setEnabled(true);
                mGplusButtonSignIn.setBackgroundResource(R.drawable.common_signin_btn_text_normal_dark);
                mTextViewStatus.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void getServerToken()
    {
        LOG.fine("GetServerToken");
        AsyncTask<Void, Void, String> myTask = new AsyncTask<Void, Void, String>()
        {

            @Override
            protected String doInBackground(Void... params)
            {
                String myApiKey = ServiceApplication.getMetaData(ServiceApplication.META_DATA_GOOGLE_CLIENT_ID);
                LOG.fine("Using google client id: " + myApiKey);

                String scopes = "audience:server:client_id:" + myApiKey;
                String jwt = null;
                try {
                    jwt = GoogleAuthUtil.getToken(getActivity(), // Context context
                        Plus.AccountApi.getAccountName(mGoogleApiClient), // String accountName
                        scopes // String scope
                    );
                }
                catch (IOException transientEx) {
                    // network or server error, the call is expected to succeed if you try again later.
                    // Don't attempt to call again immediately - the request is likely to
                    // fail, you'll hit quotas or back-off.
                    LOG.log(Level.WARNING, "IOException", transientEx);
                }
                catch (UserRecoverableAuthException e) {
                    // Requesting an authorization code will always throw
                    // UserRecoverableAuthException on the first call to GoogleAuthUtil.getToken
                    // because the user must consent to offline access to their data. After
                    // consent is granted control is returned to your activity in onActivityResult
                    // and the second call to GoogleAuthUtil.getToken will succeed.
                    // startActivityForResult(e.getIntent(), AUTH_CODE_REQUEST_CODE);
                    LOG.log(Level.WARNING, "UserRecoverableAuthException", e);
                }
                catch (GoogleAuthException authEx) {
                    // Failure. The call is not expected to ever succeed so it should not be
                    // retried.
                    LOG.log(Level.WARNING, "authEx", authEx);
                }

                LOG.fine("JWT token: " + jwt);
                return jwt;
            }

            @Override
            protected void onPostExecute(String jwt)
            {
                LOG.fine("Got JWT, starting OAuthTokenService");

                mTextViewStatus.setVisibility(View.VISIBLE);
                mTextViewStatus.setText("Signing in to Qeo");
                // start the oauth service
                Intent i = new Intent(getActivity(), OAuthTokenService.class);
                i.putExtra(OAuthTokenService.INTENT_EXTRA_JWT, jwt);
                getActivity().startService(i);
            }
        };
        myTask.execute();

    }

    private BroadcastReceiver mBroadcastReceiverLogin = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equals(OAuthTokenService.ACTION_OAUTH_TOKEN_READY)) {
                boolean result = intent.getBooleanExtra(OAuthTokenService.INTENT_EXTRA_SUCCESS, false);
                if (!result) {
                    LOG.fine("Authentication problem. Sign our from google+");
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    mGoogleApiClient.disconnect();
                    mGoogleApiClient.connect();
                    getActivity().setProgressBarIndeterminateVisibility(false);
                }
            }
        }
    };
}
