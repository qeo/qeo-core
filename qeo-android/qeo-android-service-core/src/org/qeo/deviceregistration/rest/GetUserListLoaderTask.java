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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.qeo.android.service.core.R;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.helper.RealmCache;
import org.qeo.deviceregistration.rest.RestFields.Realms;
import org.qeo.deviceregistration.rest.RestFields.Users;

import android.content.Context;

import com.google.api.client.http.HttpMethods;

/**
 * Loader to get list of users.
 */
public class GetUserListLoaderTask
    extends AbstractRestResponseLoaderTask
{
    private static final Logger LOG = Logger.getLogger("GetUserListLoaderTask");

    private final Context mContext;

    private static RestRequest getRequest()
    {
        long realmId = DeviceRegPref.getSelectedRealmId();
        if (realmId <= 0) {
            throw new IllegalStateException("Realm not yet set");
        }
        final RestRequest request = new RestRequest(HttpMethods.GET, Realms.URL + "/" + realmId + "/" + Users.URL);
        return request;
    }

    /**
     * Constructor to make call to start the Loadertask for getting list of Users.
     *
     * @param context -activity context.
     */

    public GetUserListLoaderTask(Context context)
    {
        super(context, getRequest());
        mContext = context;
    }

    @Override
    public RestResponse loadInBackground()
    {
        LOG.fine("Loading user list");
        try {
            RestResponse restResponse = super.loadInBackground();
            if (restResponse == null) {
                return null;
            }
            LOG.fine("Loading user list got data");
            JSONObject json = null;
            List<String> userlist = new ArrayList<String>();
            if (!userlist.isEmpty()) {
                userlist.clear();
            }

            // Success case
            if (restResponse.getCode() >= 200 && restResponse.getCode() < 300) {
                RealmCache.clearUserCache();
                String output = restResponse.getData();
                json = new JSONObject(output);
                JSONArray array = json.getJSONArray(Users.USERS);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject jsonObj = array.getJSONObject(i);
                    String name = jsonObj.getString(Users.NAME);
                    userlist.add(name);
                    RealmCache.putUserName(jsonObj.getLong(Users.ID), name);
                }

                Collections.sort(userlist);
                restResponse = new RestResponse(userlist, restResponse.getCode());
            }

            // Failure case
            else {
                if (restResponse.getCode() != 0) {
                    String errorMsg = getErrorMsg(restResponse.getData());
                    restResponse = new RestResponse(errorMsg, restResponse.getCode());
                }
                else {
                    restResponse = new RestResponse(mContext.getString(R.string.userlist_error_msg), 0);
                }
            }
            LOG.fine("Loading user list returning");
            return restResponse;
        }
        catch (JSONException e) {
            LOG.log(Level.WARNING, "Can't get list of users", e);
            return new RestResponse(mContext.getString(R.string.userlist_error_msg), 0);
        }
        catch (Exception e) {
            LOG.log(Level.WARNING, "Can't get list of users", e);
            return new RestResponse(mContext.getString(R.string.userlist_error_msg), 0);
        }

    }
}
