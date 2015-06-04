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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.qeo.android.service.core.R;
import org.qeo.deviceregistration.helper.RealmCache;

import android.content.Context;

import com.google.api.client.http.HttpMethods;

/**
 * Loader to get list of realms from the SMS.
 */
public class GetRealmListLoaderTask
    extends AbstractRestResponseLoaderTask
{
    private static final Logger LOG = Logger.getLogger("GetRealmListLoaderTask");

    private final Context mCtx;

    /**
     * Constructor to make call to start the Loadertask for getting list of Realms.
     * 
     * @param context activity context.
     */
    public GetRealmListLoaderTask(Context context)
    {
        super(context, getRequest());
        mCtx = context;
    }

    private static RestRequest getRequest()
    {

        final RestRequest request = new RestRequest(HttpMethods.GET, "realms");
        return request;
    }

    @Override
    public RestResponse loadInBackground()
    {

        try {
            RestResponse restResponse = super.loadInBackground();
            JSONObject json = null;
            List<String> realmList = new ArrayList<String>();
            // Success case
            if (restResponse.getCode() >= 200 && restResponse.getCode() < 300) {
                RealmCache.clearRealmCache();
                String output = restResponse.getData();
                json = new JSONObject(output);
                JSONArray array = json.getJSONArray("realms");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject jsonObj = array.getJSONObject(i);
                    String name = jsonObj.getString("name");
                    long id = jsonObj.getLong("id");
                    realmList.add(name);
                    RealmCache.putRealmName(id, name);
                }

                Collections.sort(realmList);

                restResponse = new RestResponse(realmList, restResponse.getCode());
            }

            // Failure case
            else {

                if (restResponse.getCode() != 0) {
                    String errorMsg = getErrorMsg(restResponse.getData());
                    restResponse = new RestResponse(errorMsg, restResponse.getCode());
                }
                else {
                    restResponse = new RestResponse(mCtx.getString(R.string.realmlist_error_msg), 0);
                }

            }
            return restResponse;
        }
        catch (JSONException e) {
            LOG.log(Level.WARNING, "Error getting list of realms", e);
            return new RestResponse(mCtx.getString(R.string.realmlist_error_msg), 0);
        }
    }

}
