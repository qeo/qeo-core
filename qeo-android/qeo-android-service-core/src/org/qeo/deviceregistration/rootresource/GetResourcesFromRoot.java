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

import java.io.IOException;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.GenericData;

/**
 * Helper class that executes call to get root resource services.
 */
public class GetResourcesFromRoot
{
    private GenericUrl mUrl;
    private final String mMethod;
    private GenericData mBody;

    /**
     * Constructor method, initializes the attributes of this class.
     * 
     * @param method Type of HTTP method(GET).
     * @param url url for making REST calls.
     */
    public GetResourcesFromRoot(String method, String url)
    {
        mUrl = new GenericUrl(url);
        mMethod = method;
        mBody = new GenericData();
    }

    /**
     * This function generates and returns an HttpRequest based in the class attributes.
     * 
     * @return HTTPRequest object with added info about Headers/Parser.
     * @throws java.io.IOException -
     */
    public HttpRequest getHTTPResourceRequest()
        throws IOException

    {

        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();

        HttpRequest request =
            requestFactory.buildRequest(mMethod, mUrl, mBody.isEmpty() ? null : new JsonHttpContent(
                new JacksonFactory(), mBody));
        request.getHeaders().setContentType("application/json");
        request.getHeaders().setAccept("application/json");
        request.setFollowRedirects(true);
        request.setParser(new JsonObjectParser(new JacksonFactory()));
        request.setThrowExceptionOnExecuteError(true);
        request.setLoggingEnabled(true);
        return request;
    }
}
