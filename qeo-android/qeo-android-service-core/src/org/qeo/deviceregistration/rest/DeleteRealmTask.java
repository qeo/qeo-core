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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpResponse;

/**
 * Asynctask to delete a realm.
 */
public class DeleteRealmTask extends RestTask<Void>
{
    private static final Logger LOG = Logger.getLogger("DeleteRealmTask");

    @Override
    protected Void doInBackground(Void... paramss)
    {

        try {
            HttpResponse response = exec(HttpMethods.DELETE, null, null);
            if (!response.isSuccessStatusCode()) {
                LOG.severe("Error deleting realm: " + response.parseAsString());
                return null;
            }

            LOG.severe("!!!!!!!!!!!!");
            LOG.severe("REALM DELETED!");
            LOG.severe("!!!!!!!!!!!!");
        }
        catch (Exception e) {
            LOG.log(Level.WARNING, "exception", e);
        }

        return null;
    }


}
