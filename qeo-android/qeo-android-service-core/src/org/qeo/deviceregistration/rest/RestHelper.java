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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.qeo.android.service.core.R;
import org.qeo.deviceregistration.helper.DataHandler;
import org.qeo.deviceregistration.helper.RealmCache;
import org.qeo.deviceregistration.rest.RestFields.Realms;
import org.qeo.deviceregistration.rest.RestFields.Users;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpResponse;

/**
 * This class contains some helper methods on the REST api.
 */
public class RestHelper
{
    private static final Logger LOG = Logger.getLogger("RestHelper");
    /** Action to be broadcasted if new user is added. */
    public static final String ACTION_USER_ADDED = "actionUserAdded";
    /** Action to be broadcasted if new user added failed. */
    public static final String ACTION_USER_ADDED_FAILURE = "actionUserAddedFailure";
    /** The username for a new added user . */
    public static final String INTENT_EXTRA_USERNAME = "username";
    /** Errorcode (int). */
    public static final String INTENT_EXTRA_ERRORCODE = "errorCode";
    /** Error message. */
    public static final String INTENT_EXTRA_ERRORMSG = "errorMessage";
    private final Context mCtx;

    /**
     * The constructor.
     *
     * @param ctx the context.
     */
    public RestHelper(Context ctx)
    {
        mCtx = ctx;
    }

    /**
     * Checks if a certain realm already exists.
     *
     * @param realmName the name of the realm to check
     * @return the id if it exists or -1
     * @throws IOException   in case of an IO error
     * @throws JSONException in case of a JSON parsing error
     */
    public static long getRealm(String realmName)
        throws IOException, JSONException
    {
        long id = -1;
        RestRequest request = new RestRequest(HttpMethods.GET, Realms.URL + "/");

        JSONObject json = execute(request);
        if (null != json) {
            JSONArray array = json.getJSONArray(Realms.REALMS);
            for (int i = 0; i < array.length(); i++) {
                JSONObject jsonObj = array.getJSONObject(i);
                String name = jsonObj.getString(Realms.NAME);
                if (realmName.equals(name)) {
                    id = jsonObj.getLong(Realms.ID);
                }
            }
        }
        return id;
    }

    /**
     * Add a new realm if it didnt't exist yet.
     *
     * @param realmName The name of the realm
     * @return the id if it exists or -1
     * @throws IOException   in case of an IO error
     * @throws JSONException in case of a JSON parsing error
     */
    public static long addRealm(String realmName)
        throws IOException, JSONException
    {
        long id = getRealm(realmName);

        if (id == -1) {
            RestRequest request = new RestRequest(HttpMethods.POST, Realms.URL + "/");
            request.setBody(Realms.NAME, realmName);

            JSONObject json = execute(request);
            if (null != json) {
                id = json.getLong(Realms.ID);
            }
        }
        return id;
    }

    /**
     * Checks if a certain user for a given realm exists.
     *
     * @param realmId  The id of the realm
     * @param userName The name of the user
     * @return the id if it exists or -1
     * @throws IOException   in case of an IO error
     * @throws JSONException in case of a JSON parsing error
     */
    public static long getUser(long realmId, String userName)
        throws IOException, JSONException
    {
        long id = -1;
        RestRequest request = new RestRequest(HttpMethods.GET, Realms.URL + "/" + realmId + "/" + Users.URL);

        JSONObject json = execute(request);
        if (null != json) {
            JSONArray array = json.getJSONArray(Users.USERS);
            for (int i = 0; i < array.length(); i++) {
                JSONObject jsonObj = array.getJSONObject(i);
                String name = jsonObj.getString(Users.NAME);
                if (userName.equals(name)) {
                    id = jsonObj.getLong(Users.ID);
                }
            }
        }
        return id;
    }

    /**
     * Add a new user for a given realm if it didnt't exist yet.
     *
     * @param realmId  The id of the realm
     * @param userName The name of the user
     * @return the id if it exists or -1
     * @throws IOException   in case of an IO error
     * @throws JSONException in case of a JSON parsing error
     */
    public static long addUser(long realmId, String userName)
        throws IOException, JSONException
    {
        long id = getUser(realmId, userName);

        if (id == -1) {
            RestResponse response = execAddUserRaw(realmId, userName, null);
            id = response.getLongData();
        }
        return id;
    }

    /**
     * Add a new user for a given realm if it didnt't exist yet.
     *
     * @param realmId  The id of the realm
     * @param userName The name of the user
     * @return the id if it exists or -1
     * @throws IOException   in case of an IO error
     * @throws JSONException in case of a JSON parsing error
     */
    public long addUserWithBroadcast(long realmId, String userName)
        throws IOException, JSONException
    {
        long id = getUser(realmId, userName);

        if (id == -1) {
            RestResponse response = addUserRawWithBroadcast(realmId, userName);
            id = response.getLongData();
        }
        return id;
    }

    /**
     * Add a new user for a given realm. It will send broadcast on success and failure.
     *
     * @param realmId  The id of the realm
     * @param username The name of the user.
     * @return restresponse.
     */
    public RestResponse addUserRawWithBroadcast(long realmId, String username)
    {
        return execAddUserRaw(realmId, username, mCtx);
    }

    private static RestResponse execAddUserRaw(long realmId, String username, Context ctx)
    {
        RestResponse restResponse;
        try {
            RestRequest request = new RestRequest(HttpMethods.POST, "realms/" + realmId + "/users");
            request.setBody("name", username);
            HttpResponse response = executeRaw(request);
            int status = response.getStatusCode();
            JSONObject json = null;
            if (response.isSuccessStatusCode()) {
                // Success case
                json = new JSONObject(response.parseAsString());
                String user = json.getString("name");
                if (!user.equals(username)) {
                    LOG.warning("User added is different from requested: " + user + " <> " + username);
                }
                Long userId = json.getLong("id");

                RealmCache.putUserName(userId, user);
                restResponse = new RestResponse(user, status);
                restResponse.setDataLong(userId);

                if (ctx != null) {
                    Intent userAdded = new Intent(ACTION_USER_ADDED);
                    userAdded.putExtra(INTENT_EXTRA_USERNAME, user);
                    LocalBroadcastManager.getInstance(ctx).sendBroadcast(userAdded);
                }
            }
            else {
                // Failure case
                if (status != 0) {
                    String output = response.parseAsString();
                    output = output.substring(output.indexOf("{"));
                    json = new JSONObject(output);
                    restResponse = new RestResponse(json.optString("message", "Unknown error"), status);
                }
                else {
                    String msg = "Error adding user";
                    if (ctx != null) {
                        msg = ctx.getString(R.string.add_user_error_msg);
                    }
                    restResponse = new RestResponse(msg, 0);
                }
                if (ctx != null) {
                    Intent userAddedFailed = new Intent(ACTION_USER_ADDED_FAILURE);
                    userAddedFailed.putExtra(INTENT_EXTRA_ERRORCODE, restResponse.getCode());
                    userAddedFailed.putExtra(INTENT_EXTRA_ERRORMSG, restResponse.getData());
                    userAddedFailed.putExtra(INTENT_EXTRA_USERNAME, username);
                    LocalBroadcastManager.getInstance(ctx).sendBroadcast(userAddedFailed);
                }
            }
            return restResponse;

        }

        catch (JSONException e) {
            LOG.log(Level.WARNING, "Error adding user to realm", e);
            return new RestResponse("Failed to add user", 0);
        }
        catch (Exception e) {
            LOG.log(Level.WARNING, "Error adding user to realm", e);
            return new RestResponse("Failed to add user", 0);
        }
    }

    /**
     * Add a new device for a given realm and user.
     *
     * @param realmId    The id of the realm
     * @param userId     The id of the user
     * @param deviceName The name of the device
     * @return the id if it exists or -1
     * @throws IOException   in case of an IO error
     * @throws JSONException in case of a JSON parsing error
     */
    public static String addDevice(long realmId, long userId, String deviceName)
        throws IOException, JSONException
    {
        LOG.fine("Adding device \"" + deviceName + "\" to realm " + realmId + " (user: " + userId + ")");
        String otcCode = null;
        RestRequest request = new RestRequest(HttpMethods.POST, "realms/" + realmId + "/devices");
        request.setBody("name", deviceName);
        request.setBody("user", String.valueOf(userId));

        JSONObject json = execute(request);
        if (null != json) {
            otcCode = ((JSONObject) json.get("otc")).getString("code");
        }

        return otcCode;
    }

    private static HttpResponse executeRaw(RestRequest request)
        throws IOException
    {
        String mAccessToken = DataHandler.getAccessToken();
        HttpResponse response = request.getHTTPRestRequest(mAccessToken).execute();
        return response;
    }

    private static JSONObject execute(RestRequest request)
        throws IOException, JSONException
    {
        return parseResponse(executeRaw(request));
    }

    private static JSONObject parseResponse(HttpResponse response)
        throws IOException, JSONException
    {
        JSONObject json = null;
        RestResponse restResponse = new RestResponse(response.parseAsString(), response.getStatusCode());

        // Success case
        if (restResponse.getCode() >= 200 && restResponse.getCode() < 300) {
            String output = restResponse.getData();
            json = new JSONObject(output);
        }
        // Failure case
        else {
            if (restResponse.getCode() != 0) {
                String output = restResponse.getData();
                output = output.substring(output.indexOf("{"));
                json = new JSONObject(output);
                LOG.warning("Error executing rest call ( " + response.getRequest().getUrl() + ") " + output);
            }
            else {
                LOG.warning("Error executing rest call ( " + response.getRequest().getUrl() + ")");
            }

        }
        return json;
    }

}
