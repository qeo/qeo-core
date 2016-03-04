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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.qeo.android.service.core.R;
import org.qeo.deviceregistration.DeviceRegPref;

import android.content.Context;

import com.google.api.client.http.HttpMethods;

/**
 * Loader to get list of devices in a realm.
 */
public class GetDeviceListLoaderTask
    extends AbstractRestResponseLoaderTask
{
    private static final Logger LOG = Logger.getLogger("GetDeviceListLoaderTask");
    private static final String STATE_ENROLLED = "ENROLLED";
    private final Context mContext;

    private static RestRequest getRequest()
    {
        long realmId = DeviceRegPref.getSelectedRealmId();
        if (realmId <= 0) {
            throw new IllegalStateException("Realm not yet set");
        }

        final RestRequest request = new RestRequest(HttpMethods.GET, "realms/" + realmId + "/devices");
        return request;
    }

    /**
     * Constructor to make call to start the Loadertask for getting registered devices.
     * 
     * @param context -activity context.
     */
    public GetDeviceListLoaderTask(Context context)
    {
        super(context, getRequest());
        mContext = context;
    }

    @Override
    public RestResponse loadInBackground()
    {
        try {
            RestResponse restResponse = super.loadInBackground();
            if (restResponse == null) {
                return null;
            }
            JSONObject json = null;
            List<String> deviceList = new ArrayList<String>();
            if (!deviceList.isEmpty()) {
                deviceList.clear();
            }
            // Success case
            if (restResponse.getCode() >= 200 && restResponse.getCode() < 300) {
                String output = restResponse.getData();
                json = new JSONObject(output);
                JSONArray array = json.getJSONArray("devices");
                for (int i = 0; i < array.length(); i++) {

                    JSONObject jsonObj = array.getJSONObject(i);
                    String deviceState = jsonObj.getString("state");
                    String name = jsonObj.getString("name");
                    // Make sure that UI shows list of successfully registered devices only.
                    if (deviceState.equals(STATE_ENROLLED)) {
                        deviceList.add(name);
                    }
                    else {
                        LOG.fine("Device not enrolled: " + name + " -- state: " + deviceState);
                    }
                }

                restResponse = new RestResponse(deviceList, restResponse.getCode());
            }

            // Failure case
            else {

                if (restResponse.getCode() != 0) {
                    String errorMsg = getErrorMsg(restResponse.getData());
                    restResponse = new RestResponse(errorMsg, restResponse.getCode());
                }
                else {
                    restResponse = new RestResponse(mContext.getString(R.string.devicelist_error_msg), 0);
                }

            }
            return restResponse;
        }
        catch (JSONException e) {
            LOG.log(Level.WARNING, "Error getting list of devices", e);
            return new RestResponse(mContext.getString(R.string.devicelist_error_msg), 0);
        }
        catch (Exception e) {
            LOG.log(Level.WARNING, "Error getting list of devices", e);
            return new RestResponse(mContext.getString(R.string.devicelist_error_msg), 0);
        }

    }

}
