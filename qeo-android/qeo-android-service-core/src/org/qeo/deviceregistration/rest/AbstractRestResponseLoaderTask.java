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

package org.qeo.deviceregistration.rest;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.qeo.deviceregistration.helper.DataHandler;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

/**
 * Abstract class to load a rest call.
 */
public abstract class AbstractRestResponseLoaderTask
    extends AsyncTaskLoader<RestResponse>
{
    private static final Logger LOG = Logger.getLogger("AbstractRestResponseLoaderTask");
    private RestRequest mRequest;

    /**
     * Create an instance.
     * 
     * @param context Context of Activity.
     */
    protected AbstractRestResponseLoaderTask(Context context)
    {
        super(context);
    }

    /**
     * Create an instance.
     * 
     * @param context Context of Activity.
     * @param request RestRequest object to execute REST calls.
     */
    protected AbstractRestResponseLoaderTask(Context context, RestRequest request)
    {
        super(context);
        mRequest = request;
    }

    @Override
    public RestResponse loadInBackground()
    {
        try {
            String mAccessToken = DataHandler.getAccessToken();
            if (mAccessToken == null) {
                // something wrong with the tokens. Don't propagate errors to UI as re-login should happen
                LOG.fine("Token error, aborting loader.");
                reset();
                abandon();
                return null;
            }
            HttpResponse response = mRequest.getHTTPRestRequest(mAccessToken).execute();

            // Here we create our response and send it back to the LoaderCallbacks<RestResponse> implementation.
            RestResponse restResponse = new RestResponse(response.parseAsString(), response.getStatusCode());
            return restResponse;
        }
        catch (HttpResponseException e) {
            LOG.log(Level.FINE, "ERROR for " + this, e);
            return new RestResponse(e.getMessage(), e.getStatusCode());
        }
        catch (Exception e) {
            LOG.log(Level.FINE, "ERROR for " + this, e);
            return new RestResponse(e.getMessage(), 0);
        }
    }

    /**
     * RestRequest object.
     * 
     * @return returns RestRequest object.
     */
    public RestRequest getRestRequest()
    {
        return mRequest;
    }

    @Override
    public void deliverResult(RestResponse data)
    {
        // Here we cache our response.
        super.deliverResult(data);
    }

    @Override
    protected void onStartLoading()
    {
        forceLoad();
    }

    @Override
    protected void onStopLoading()
    {
        // This prevents the AsyncTask backing this
        // loader from completing if it is currently running.
        cancelLoad();
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        // Stop the Loader if it is currently running.
        onStopLoading();

    }

    /**
     * Try to extra error msg returned from SMS.
     * 
     * @param data The raw data.
     * @return The error msg.
     * @throws JSONException On json parsing exception.
     */
    protected String getErrorMsg(final String data)
        throws JSONException
    {
        int index = data.indexOf("{");
        String errorMsg;
        if (index != -1) {
            // rest response
            JSONObject json = new JSONObject(data.substring(index));
            errorMsg = json.optString("message", data);
        }
        else {
            errorMsg = data;
        }
        return errorMsg;
    }

}
