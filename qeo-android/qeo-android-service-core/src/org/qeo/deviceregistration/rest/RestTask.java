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

package org.qeo.deviceregistration.rest;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.helper.DataHandler;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;

/**
 * Async task helper to execute rest calls to the qeo sms.
 *
 * @param <RETURN> Return value from doInbackground call.
 */
public abstract class RestTask<RETURN> extends AsyncCallbackTask<RETURN>
{
    private static final Logger LOG = Logger.getLogger("RestTask");
    /** ID of the current selected realm. */
    protected final long mRealmId;
    private boolean mEnableUrlLogging = false;

    /**
     * Create a new instance.
     */
    protected RestTask()
    {
        mRealmId = DeviceRegPref.getSelectedRealmId();
        if (mRealmId <= 0) {
            throw new IllegalStateException("Realm not yet set");
        }
    }

    /**
     * Execute the rest call on the current selected realm.
     *
     * @param method  The http method.
     * @param urlPart The url part to be added. Can be null.
     * @param params  Optional http parameters, can be null.
     * @return The http response.
     * @throws IOException On error.
     */
    protected HttpResponse exec(String method, String urlPart, Map<String, String> params) throws IOException
    {
        return exec(method, urlPart, params, null);
    }

    /**
     * Execute the rest call on the current selected realm.
     *
     * @param method  The http method.
     * @param urlPart The url part to be added. Can be null.
     * @param params  Optional http parameters, can be null.
     * @param content Optional http content
     * @return The http response.
     * @throws IOException On error.
     */
    protected HttpResponse exec(String method, String urlPart, Map<String, String> params,
                                HttpContent content) throws IOException
    {
        String url2 = RestFields.Realms.URL + "/" + mRealmId;
        if (urlPart != null) {
            url2 += urlPart;
        }
        final RestRequest request = new RestRequest(method, url2);

        String accessToken = DataHandler.getAccessTokenWithException();
        HttpRequest httpRestRequest = request.getHTTPRestRequest(accessToken);
        if (params != null) {
            GenericUrl url = httpRestRequest.getUrl();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                url.set(entry.getKey(), entry.getValue());
            }
        }
        if (mEnableUrlLogging) {
            LOG.info("Fetch url: " + httpRestRequest.getUrl());
        }
        if (content != null) {
            httpRestRequest.setContent(content);
        }
        return httpRestRequest.execute();
    }


    /**
     * Execute the rest call on the current selected realm. (HTTP GET)
     *
     * @param urlPart The url part to be added. Can be null.
     * @param params  Optional http parameters, can be null.
     * @return The http response.
     * @throws IOException On error.
     */
    protected HttpResponse exec(String urlPart, Map<String, String> params) throws IOException
    {
        return exec(HttpMethods.GET, urlPart, params);
    }

    /**
     * Enable url logging.
     */
    public void enableUrlLogging()
    {
        mEnableUrlLogging = true;
    }
}
