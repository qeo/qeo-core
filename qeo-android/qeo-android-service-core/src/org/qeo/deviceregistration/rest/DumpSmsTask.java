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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.qeo.deviceregistration.rest.RestFields.Devices;
import org.qeo.deviceregistration.rest.RestFields.Users;

import com.google.api.client.http.HttpResponse;

/**
 * AsyncTask to dump the content of the SMS.
 */
public class DumpSmsTask extends RestTask<String>
{
    private static final Logger LOG = Logger.getLogger("DumpSmsTask");

    @Override
    protected String doInBackground(Void... paramss)
    {
        StringBuilder result = new StringBuilder();
        result.append("\n");
        HttpResponse response;
        try {
            response = exec("/" + Users.URL, null);
            if (!response.isSuccessStatusCode()) {
                LOG.severe("Error in getting users: " + response.parseAsString());
                return null;
            }

            JSONArray usersJson = new JSONObject(response.parseAsString()).getJSONArray(Users.USERS);
            for (int i = 0; i < usersJson.length(); ++i) {
                JSONObject userJson = usersJson.getJSONObject(i);
                int id = userJson.getInt(Users.ID);
                String name = userJson.getString(Users.NAME);
                result.append("User: ").append(name).append(" (").append(id).append(")\n");

                Map<String, String> params = new HashMap<>();
                params.put(Devices.PARAM_USER_ID, Integer.toString(id));
                response = exec("/" + Devices.URL, params);

                if (!response.isSuccessStatusCode()) {
                    LOG.severe("Error in getting devices for user: " + response.parseAsString());
                    return null;
                }

                JSONArray devicesJson = new JSONObject(response.parseAsString()).getJSONArray(Devices.DEVICES);
                for (int j = 0; j < devicesJson.length(); ++j) {
                    JSONObject deviceJson = devicesJson.getJSONObject(j);
                    int devId = deviceJson.getInt(Devices.ID);
                    String devName = deviceJson.getString(Devices.NAME);
                    String devDeviceId = deviceJson.getString(Devices.DEVICE_ID);
                    String devState = deviceJson.getString(Devices.STATE);
                    result.append("\tDevice: ").append(devId).append("\n");
                    result.append("\t\tName: ").append(devName).append("\n");
                    result.append("\t\tDeviceId: ").append(devDeviceId).append("\n");
                    result.append("\t\tState: ").append(devState).append("\n");
                }
            }

        }
        catch (Exception e) {
            LOG.log(Level.WARNING, "exception", e);
        }

        return result.toString();
    }

}
