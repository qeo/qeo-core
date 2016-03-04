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

import java.util.logging.Logger;

import org.qeo.deviceregistration.DeviceRegPref;

import android.content.Context;

/**
 * Loader to add a user on the SMS.
 */
public class AddUserLoaderTask
    extends AbstractRestResponseLoaderTask
{
    private static final Logger LOG = Logger.getLogger("AddUserLoaderTask");
    private final Context mContext;
    private final String mUsername;
    private final long mRealmId;

    /**
     * Constructor to make call to start the Loadertask for adding user.
     * 
     * @param context activity context.
     * @param userName the name of the user to be added.
     */
    public AddUserLoaderTask(Context context, String userName)
    {
        super(context, null);
        mContext = context;
        mUsername = userName;
        mRealmId = DeviceRegPref.getSelectedRealmId();
    }

    @Override
    public RestResponse loadInBackground()
    {
        LOG.fine("Adding user " + mUsername + " in background");
        RestHelper restHelper = new RestHelper(mContext);
        return restHelper.addUserRawWithBroadcast(mRealmId, mUsername);
    }

}
