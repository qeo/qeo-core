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

package org.qeo.deviceregistration.rootresource;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.qeo.deviceregistration.DeviceRegPref;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

/**
 * Intent service to load root resource.
 */
public class LoadResourcesService
    extends IntentService
{
    /** Intent data containing the root URL to be used in this service. */
    public static final String INTENT_ROOT_URL = "rootUrl";
    /** Broadcast action to sent back the result of the services task. */
    public static final String ACTION_LOADING_DONE = "loadingDone";
    /** Error message to be sent in the broadcast action. */
    public static final String INTENT_EXTRA_ERROR_MESSAGE = "errorMessage";
    /** Code to be sent in the broadcast action. */
    public static final String INTENT_EXTRA_CODE = "code";

    private static final Logger LOG = Logger.getLogger("LoadResourcesService");

    private static boolean sLoaded = false;
    private static int sStatusCode;

    /**
     * Constructor method.
     */
    public LoadResourcesService()
    {
        super("LoadResourcesService");
    }

    private static void broadcastResponse(Context ctx, String errorMessage, int code)
    {
        Intent intent = new Intent(ACTION_LOADING_DONE);
        intent.putExtra(INTENT_EXTRA_ERROR_MESSAGE, errorMessage);
        intent.putExtra(INTENT_EXTRA_CODE, code);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        String mRootUrl = intent.getStringExtra(INTENT_ROOT_URL);
        load(this, mRootUrl);
    }

    /**
     * Indicate if the Root resource has been loaded.
     * @return true if loaded. False otherwise.
     */
    public static boolean isLoaded()
    {
        return sLoaded;
    }

    /**
     * Load the root resource. Use this call to invoke this service in a blocking way.
     * @param ctx The android context.
     */
    public static void load(Context ctx)
    {
        load(ctx, DeviceRegPref.getScepServerURL());
    }

    private static void broadcastOk(Context ctx)
    {
        broadcastResponse(ctx, "", sStatusCode);
    }

    private static synchronized void load(Context ctx, String url)
    {
        if (sLoaded) {
            broadcastOk(ctx);
            return;
        }
        try {
            // make sure there is a trailing /, proxy needs it.
            if (!url.endsWith("/")) {
                url += "/";
            }
            GetResourcesFromRoot mRequest = new GetResourcesFromRoot(HttpMethods.GET, url);
            HttpResponse response = null;
            response = mRequest.getHTTPResourceRequest().execute();

            JSONObject json = null;
            if (response.isSuccessStatusCode()) {
                String output = response.parseAsString();
                json = new JSONObject(output);
                String rootUrl = json.getString("href");
                JSONObject restObject = json.getJSONObject("management");
                String restUrl = restObject.getString("href");

                JSONObject oauthObject = json.getJSONObject("oauth");

                JSONObject token = oauthObject.getJSONObject("token");
                String tokenURL = token.getString("href");

                JSONObject authorizationObject = oauthObject.getJSONObject("authorization");
                String authorizationUrl = authorizationObject.getString("href");

                DeviceRegPref.setResourceURL(rootUrl, restUrl, tokenURL, authorizationUrl);

                sStatusCode = response.getStatusCode();
                broadcastOk(ctx);
                sLoaded = true;
            }
            else {
                broadcastResponse(ctx, "failure", response.getStatusCode());
            }

        }
        catch (HttpResponseException e) {
            LOG.log(Level.WARNING, "Error fetching root resource", e);
            broadcastResponse(ctx, "Can't fetch root resource: " + e.getMessage(), e.getStatusCode());
        }
        catch (Exception e) {
            LOG.log(Level.WARNING, "Error fetching root resource", e);
            broadcastResponse(ctx, "Can't fetch root resource: " + e.getMessage(), 0);
        }
    }

}
