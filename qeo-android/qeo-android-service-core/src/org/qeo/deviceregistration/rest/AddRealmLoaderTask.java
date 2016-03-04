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
import org.qeo.android.service.core.R;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.helper.RealmCache;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.api.client.http.HttpMethods;

/**
 * LoaderTask to create a new Realm.
 */
public class AddRealmLoaderTask
    extends AbstractRestResponseLoaderTask
{
    private static final Logger LOG = Logger.getLogger("AddRealmLoaderTask");

    private final Context mContext;
    private final Intent mRealmProgressIntent;

    private static RestRequest getRequest(String realmName)
    {
        RestRequest request = new RestRequest(HttpMethods.POST, "realms");
        request.setParameter("name", realmName);
        return request;
    }

    /**
     * Create an instance.
     * 
     * @param context The android context.
     * @param realmName The name of the realm to be created.
     */
    public AddRealmLoaderTask(Context context, String realmName)
    {
        super(context, getRequest(realmName));

        mContext = context;
        mRealmProgressIntent = new Intent(mContext.getString(R.string.add_realm_progress_broadcast));
    }

    @Override
    public RestResponse loadInBackground()
    {

        try {
            RestResponse restResponse = super.loadInBackground();
            JSONObject json = null;
            // Success case
            if (restResponse.getCode() >= 200 && restResponse.getCode() < 300) {
                String output = restResponse.getData();
                json = new JSONObject(output);
                String realm = json.getString("name");
                long realmId = json.getLong("id");
                // DeviceRegPref.edit().setSelectedRealmId(realmId, realm).apply();
                RealmCache.putRealmName(realmId, realm);
                restResponse = new RestResponse(realm, restResponse.getCode());
            }

            // Failure case
            else {
                if (restResponse.getCode() != 0) {
                    String output = restResponse.getData();
                    output = output.substring(output.indexOf("{"));
                    json = new JSONObject(output);
                    restResponse = new RestResponse(json.getString("message"), restResponse.getCode());
                }
                else {
                    restResponse = new RestResponse(mContext.getString(R.string.add_realm_error_msg), 0);
                }

            }
            DeviceRegPref.edit().setShowRealmProgress(false).apply();
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(mRealmProgressIntent);
            return restResponse;

        }
        catch (JSONException e) {
            // Failure case
            LOG.log(Level.WARNING, "Error adding realm", e);
            return new RestResponse(mContext.getString(R.string.add_realm_error_msg), 0);
        }

    }

}
