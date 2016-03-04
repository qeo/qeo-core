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

package org.qeo.deviceregistration.service;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.helper.DataHandler;
import org.qeo.deviceregistration.helper.JWTTokenRequest;
import org.qeo.deviceregistration.rootresource.LoadResourcesService;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.TokenRequest;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

/**
 * Gets OAuth access/refresh token from an openId code.
 */
public class OAuthTokenService
    extends IntentService
{
    private static final Logger LOG = Logger.getLogger("OAuthTokenService");
    /** broadcast action that will be sent out if the oauth login is ready. */
    public static final String ACTION_OAUTH_TOKEN_READY = "actionOAuthTokenReady";
    /** The fetched openId token. */
    public static final String INTENT_EXTRA_OAUTH_CODE = "OAuthCode";
    /** The fetched JWT token. */
    public static final String INTENT_EXTRA_JWT = "JWT";
    /** true on success, false otherwise. */
    public static final String INTENT_EXTRA_SUCCESS = "success";
    /** Optional errormessage. */
    public static final String INTENT_EXTRA_ERRORMSG = "errorMSG";
    private LocalBroadcastManager mLbm;

    /**
     * Public constructor, do not use directly.
     */
    public OAuthTokenService()
    {
        super("OAuthTokenService");
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        mLbm = LocalBroadcastManager.getInstance(this);
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        LOG.info("Signing in to Qeo");

        String openIdCode = intent.getStringExtra(INTENT_EXTRA_OAUTH_CODE);
        String jwt = intent.getStringExtra(INTENT_EXTRA_JWT);
        if (openIdCode == null && jwt == null) {
            LOG.warning("unknown intent: " + intent);
            return;
        }
        String errorMsg = execute(this, openIdCode, jwt);
        boolean result = (errorMsg == null);
        Intent resultIntent = new Intent(ACTION_OAUTH_TOKEN_READY);
        resultIntent.putExtra(INTENT_EXTRA_SUCCESS, result);
        resultIntent.putExtra(INTENT_EXTRA_ERRORMSG, errorMsg);
        LOG.fine("Broadcasting result from " + ACTION_OAUTH_TOKEN_READY + ": " + result);
        mLbm.sendBroadcast(resultIntent);
    }

    /**
     * Login to the Qeo SMS using and OpenId code. This call store the refresh/access tokens in the preferences.
     * @param ctx Android context
     * @param code The OpenId Code.
     */
    public static void loginWithOpenId(Context ctx, String code)
    {
        execute(ctx, code, null);
    }

    /**
     * Login to the Qeo SMS using and OpenId Connect JWT token.
     * This call store the refresh/access tokens in the preferences.
     * @param ctx Android context.
     * @param jwt JWT token.
     */
    public static void loginWithOpenIdConnect(Context ctx, String jwt)
    {
        execute(ctx, null, jwt);
    }

    private static String execute(Context ctx, String openIdCode, String jwt)
    {
        TokenRequest tokenRequest;
        if (!LoadResourcesService.isLoaded()) {
            //load root resource
            LoadResourcesService.load(ctx);
        }
        GenericUrl url = new GenericUrl(DeviceRegPref.getOauthServerURL());
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();


        if (openIdCode != null) {
            LOG.fine("Need to execute AuthorizationCodeTokenRequest: " + openIdCode);
            tokenRequest = new AuthorizationCodeTokenRequest(transport, jsonFactory, url, openIdCode);
        }
        else if (jwt != null) {
            LOG.fine("Need to execute JWTTokenRequest: " + jwt);
            tokenRequest = new JWTTokenRequest(transport, jsonFactory, url, jwt);
        }
        else {
            throw new IllegalStateException("openIdCode and jwt cannot be both null");

        }

        boolean result;
        String errorMsg = null;
        try {
            result = DataHandler.fetchTokens(tokenRequest);
            //if this did now throw an exception, accesstoken is OK.
        }
        catch (IOException e) {
            String msg = "Error fetching Qeo OAuth tokens";
            LOG.log(Level.WARNING, msg, e);
            result = false;
            errorMsg = msg + ": " + e.getMessage();
        }
        if (result) {
            return null;
        }
        else {
            if (errorMsg == null) {
                errorMsg = "Unknown error";
            }
            return errorMsg;
        }
    }

}
