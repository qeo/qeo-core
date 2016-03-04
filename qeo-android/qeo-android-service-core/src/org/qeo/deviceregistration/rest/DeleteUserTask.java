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

import org.qeo.deviceregistration.rest.RestFields.Users;

import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpResponse;

/**
 * AsyncTask to delete a user.<br>
 * It will return true if the action completed successfully.
 */
public class DeleteUserTask extends RestTask<Boolean>
{
    private static final Logger LOG = Logger.getLogger("DeleteUserTask");
    private final long mUserId;

    /**
     * Create an instance.
     *
     * @param userId The user id to be deleted.
     */
    public DeleteUserTask(long userId)
    {
        mUserId = userId;
    }

    @Override
    protected Boolean doInBackground(Void... params)
    {
        try {
            HttpResponse response = exec(HttpMethods.DELETE, "/" + Users.URL + "/" + mUserId, null);
            if (!response.isSuccessStatusCode()) {
                LOG.severe("Error deleting user " + mUserId + ": " + response.parseAsString());
                return false;
            }
            LOG.fine("Delete completed");
        }
        catch (Exception e) {
            LOG.log(Level.WARNING, "exception", e);
        }
        return true;
    }
}
