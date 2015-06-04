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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qeo.android.security.OtcActivity;
import org.qeo.android.service.QeoDefaults;
import org.qeo.android.service.ServiceApplication;
import org.qeo.android.service.ui.BuildConfig;
import org.qeo.android.service.ui.R;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.QeoManagementApp;
import org.qeo.deviceregistration.rootresource.LoadResourcesService;
import org.qeo.deviceregistration.service.OAuthTokenService;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/** Fragment for the webview. */
public class WebviewFragment
    extends SherlockFragment
{
    private static final Logger LOG = Logger.getLogger("WebViewFragment");

    private WebView mWebView;
    private Pattern mPatternQueryParameter;
    private WebViewActivity mActivity;
    private Bundle mWebviewState;
    private ResourcesBroadcastReceiver mResourcesBroadcastReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        QeoManagementApp.init(); // Initialize global variables
        mWebviewState = null;

        mActivity = (WebViewActivity) getActivity();

        // register receiver for root resource loader
        IntentFilter intentFilter = new IntentFilter(LoadResourcesService.ACTION_LOADING_DONE);
        mResourcesBroadcastReceiver = new ResourcesBroadcastReceiver();
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mResourcesBroadcastReceiver, intentFilter);

        Intent intentService = new Intent(getActivity(), LoadResourcesService.class);
        intentService.putExtra(LoadResourcesService.INTENT_ROOT_URL, DeviceRegPref.getScepServerURL());
        getActivity().startService(intentService);

        mActivity.setProgressBarIndeterminateVisibility(true);
        mActivity.setSupportProgressBarIndeterminateVisibility(true);

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy()
    {
        if (mResourcesBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mResourcesBroadcastReceiver);
            mResourcesBroadcastReceiver = null;
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_webview, container, false);
        mActivity = (WebViewActivity) getActivity();
        mPatternQueryParameter = Pattern.compile("^(.*)=(.*)$");

        mWebView = (WebView) rootView.findViewById(R.id.webViewAuthentication);
        initWebview();

        if (mWebviewState != null) {
            LOG.fine("restoring webview");
            mWebView.restoreState(mWebviewState);
            mWebviewState = null;
        }

        return rootView;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("SetJavaScriptEnabled")
    private void initWebview()
    {
        LOG.fine("initWebview");

        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        mWebView.setScrollbarFadingEnabled(true);
        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setAllowFileAccess(false);

        // don't save passwords or form data
        mWebView.getSettings().setSaveFormData(false);
        mWebView.getSettings().setSavePassword(false);

        // Load the URLs inside the mWebView, not in the external web browser
        mWebView.setWebViewClient(new OpenIdWebviewClient());
    }

    private void loadDefaultUrl()
    {
        mActivity.setProgressBarIndeterminateVisibility(true);
        mActivity.setSupportProgressBarIndeterminateVisibility(true);
        // Load login page
        loadOpenIdPage(DeviceRegPref.getAuthorizationUrl());

    }

    private void loadOpenIdPage(String openIdUrl)
    {
        LOG.fine("Using openId url " + openIdUrl);

        Uri.Builder builder = Uri.parse(openIdUrl).buildUpon();
        builder.appendQueryParameter("response_type", "code");
        builder.appendQueryParameter("client_id",
            ServiceApplication.getMetaData(ServiceApplication.META_DATA_QEO_CLIENT_ID));
        builder.appendQueryParameter("redirect_uri", QeoDefaults.getOpenIdRedirectUrl());
        Uri uri = builder.build();
        LOG.fine("Load url " + uri.toString());

        mWebView.loadUrl(uri.toString());
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        mWebviewState = new Bundle();
        // Save the state of the mWebView
        mWebView.saveState(mWebviewState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        if (mActivity.isOtcEnabled()) {
            LOG.fine("Enable OTC button");
            // Inflate the menu items for use in the action bar
            inflater.inflate(R.menu.webview_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle presses on the action bar items
        if (item.getItemId() == R.id.OTC_Login) {
            // the user chooses to enter OTC / scan QR
            mActivity.setOtcStarted(true);
            startActivity(new Intent(getActivity(), OtcActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Creates a Map from the query string included in the URL.
     * 
     * @param query the query string.
     * @return a map with key value pairs from the query string.
     */
    private Map<String, String> getQueryMap(String query)
    {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params) {
            Matcher m = mPatternQueryParameter.matcher(param);
            if (m.matches()) {
                map.put(m.group(1), m.group(2));
            }
        }

        return map;
    }

    /** OpenId login OK. */
    private void openOAuthReceived(String code)
    {
        LOG.fine("Got OAuth code, starting OAuthTokenService");

        // start the oauth service
        Intent i = new Intent(mActivity.getApplicationContext(), OAuthTokenService.class);
        i.putExtra(OAuthTokenService.INTENT_EXTRA_OAUTH_CODE, code);
        mActivity.startService(i);
    }

    /** OpenId login failed. */
    private void codeFailed()
    {
        mActivity.broadcastError("Error in openId login");
        // Close down this mWebView.
        mActivity.finish();
    }

    /**
     * Try if the webview can go back.
     * @return True if the webview did go back, false if there is no backstate.
     */
    public boolean tryGoBack()
    {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        else {
            return false;
        }
    }

    // /////////////////////
    // helper classes
    // /////////////////////

    private class ResourcesBroadcastReceiver
        extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String errorMessage = intent.getStringExtra(LoadResourcesService.INTENT_EXTRA_ERROR_MESSAGE);

            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(this);
            if (errorMessage == null || errorMessage.isEmpty()) {
                // load ok
                loadDefaultUrl();
            }
            else {
                LOG.warning("Error fetching root resource: " + DeviceRegPref.getScepServerURL() + " -- "
                    + errorMessage);
                Toast.makeText(getActivity(), getString(R.string.get_resource_error_msg) + " " + errorMessage,
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    private class OpenIdWebviewClient
        extends WebViewClient
    {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url)
        {
            Uri uri = Uri.parse(url);
            LOG.fine("Got webview url " + uri);

            // Gets the Query String from the URL if one is available.
            String q = uri.getQuery();
            // When it has no Query string, we can just ignore it and let
            // the normal URL handling take care of it. We are sure it does
            // not contain a Authorization Code then.

            if (q != null) {

                // If the url matches the redirect url we don't want the
                // mWebView to actually load the page. Instead we catch this
                // request and extract the Authorization Code from the
                // query. It is passed back to the Activity by the
                // OauthCallback.

                if (url.startsWith(QeoDefaults.getOpenIdRedirectUrl())) {
                    LOG.fine("Intercept openId resultPage");
                    Map<String, String> params = getQueryMap(q);
                    String code = params.get("code");
                    LOG.fine("code: " + code);
                    if (code != null && !code.isEmpty()) {
                        // openId login finished
                        openOAuthReceived(code);
                    }
                    else {
                        LOG.warning("Error in openId handling: " + url);
                        codeFailed();
                    }
                    return true; // handling overruled
                }
            }
            else {
                mActivity.setProgressBarIndeterminateVisibility(true);
                mActivity.setSupportProgressBarIndeterminateVisibility(true);
            }
            // Normal URL Handling applies here.
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error)
        {
            if (!QeoDefaults.isAllowSelfSignedCertificate()) {
                // default implementation
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.certificate_error));
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
                LOG.warning("Certificate error: " + error.toString());
                super.onReceivedSslError(view, handler, error);
            }
            else {
                // Workaround for Self Signed certificates. By default the
                // WebView will not load pages that are not trusted. We override
                // it by telling it to proceed when an error occurs.
                handler.proceed();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url)
        {
            LOG.fine("page load finished");
            mActivity.setProgressBarIndeterminateVisibility(false);
            mActivity.setSupportProgressBarIndeterminateVisibility(false);

            if (BuildConfig.DEBUG) {
                // UIautomator uses this, don't remove it. Only print on debug
                LOG.info("loaded url [" + url + "]");
            }
        }
    }
}
