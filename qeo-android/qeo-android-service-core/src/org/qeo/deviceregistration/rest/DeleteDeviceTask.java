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

import java.util.logging.Logger;

/**
 * Asynctask to delete a device.
 */
public class DeleteDeviceTask extends RestTask<Boolean>
{
    private static final Logger LOG = Logger.getLogger("DeleteDeviceTask");
    private final long mUserId;
    private final long mDeviceId;

    /**
     * Create an instance.
     * @param deviceId The device id.
     * @param userId The user id.
     */
    public DeleteDeviceTask(long deviceId, long userId)
    {
        mUserId = userId;
        mDeviceId = deviceId;
        throw new IllegalStateException("Currently not supported");
    }

    @Override
    protected Boolean doInBackground(Void... params)
    {
        //This won't work because:
        // The REST needs DELETE method, and the user id to be added to the body
        // The HTTP spec does not allow DELETE method with a body
        // The library used here checks for that and denies this.
//        try {
//            GenericData data = new GenericData();
//            data.put(Devices.USER, mUserId);
//            JsonHttpContent content = new JsonHttpContent(new JacksonFactory(), data);
//
//            HttpResponse response = exec(HttpMethods.DELETE, "/" + Devices.URL + "/" + mDeviceId, null, content);
//            if (!response.isSuccessStatusCode()) {
//                LOG.severe("Error deleting device " + mDeviceId + ": " + response.parseAsString());
//                return true;
//            }
//            LOG.fine("Delete completed");
//        }
//        catch (Exception e) {
//            LOG.log(Level.WARNING, "exception", e);
//        }

        return false;
    }


}
