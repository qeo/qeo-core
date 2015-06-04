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

import org.qeo.deviceregistration.DeviceRegPref;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.GenericData;

/**
 * Helper class that can execute rest calls.
 */
public class RestRequest
{
    private final GenericUrl mUrl;
    private final String mMethod;
    private GenericData mBody;

    /**
     * construct a RestRequest object with a certain method, token and url.
     * 
     * @param method Type of HTTP method(POST,GET).
     * @param path path to be added to the rest url.
     */
    public RestRequest(String method, String path)
    {
        mUrl = new GenericUrl(DeviceRegPref.getServerURL() + "v1/" + path);
        mMethod = method;
        mBody = new GenericData();
    }

    /**
     * Api to set extra information before making REST call.
     * 
     * @param key the key
     * @param value the value
     */
    public void setParameter(String key, String value)
    {
        mBody.set(key, value);
    }

    /**
     * Set the body of the RestRequest object.
     * 
     * @param key -
     * @param value -
     */
    public void setBody(String key, String value)
    {
        mBody = mBody.set(key, value);
    }

    /**
     * Get the url for this rest request.
     * 
     * @return The url in string format.
     */
    public String getUrl()
    {
        return mUrl.build();
    }

    /**
     * Build the HTTP request based on the data in the RestRequest object.
     * 
     * @param accessToken - Access token
     * @return HTTPRequest object with added info about Headers/Parser.
     * @throws IOException -
     */
    public HttpRequest getHTTPRestRequest(String accessToken)
        throws IOException

    {
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();

        HttpRequest request =
            requestFactory.buildRequest(mMethod, mUrl, mBody.isEmpty() ? null : new JsonHttpContent(
                new JacksonFactory(), mBody));
        request.getHeaders().setAuthorization("Bearer" + " " + accessToken);
        request.getHeaders().setContentType("application/json");
        request.getHeaders().setAccept("application/json");
        request.setParser(new JsonObjectParser(new JacksonFactory()));
        request.setThrowExceptionOnExecuteError(true);
        request.setLoggingEnabled(true);
        return request;
    }
}
