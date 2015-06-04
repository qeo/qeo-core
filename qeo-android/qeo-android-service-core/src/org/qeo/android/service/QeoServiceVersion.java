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

package org.qeo.android.service;

import java.util.logging.Logger;

import org.qeo.android.internal.AidlConstants;
import org.qeo.android.internal.IServiceQeoVersion;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * Service to get version information.
 */
public class QeoServiceVersion
    extends Service
{
    private static final Logger LOG = Logger.getLogger("QeoServiceVersion");

    /** version of the Qeo android service. Not used for now. */
    private static final int SERVICE_VERSION = 1;
    /** Minimal required version of the library needed to communicate. Not used for now. */
    private static final int REQUIRED_LIBRARY_VERSION = 1;

    @Override
    public IBinder onBind(Intent intent)
    {
        LOG.fine("onBind");
        return mBinderVersion;
    }

    private final IServiceQeoVersion.Stub mBinderVersion = new IServiceQeoVersion.Stub() {

        @Override
        public int checkVersion(int libraryVersion)
            throws RemoteException
        {
            LOG.fine("CheckVersion: " + libraryVersion);
            if (libraryVersion < REQUIRED_LIBRARY_VERSION) {
                return -1 * REQUIRED_LIBRARY_VERSION;
            }
            return SERVICE_VERSION;
        }

        @Override
        public int checkVersionString(String version)
            throws RemoteException
        {
            if (AidlConstants.AIDL_SERVICE_ACTION_V1.equals(version)) {
                // supported versions
                return AidlConstants.RESULT_VERSION_OK;
            }
            else if (AidlConstants.AIDL_SERVICE_ACTION_V0.equals(version)) {
                // versions no longer supported
                return AidlConstants.RESULT_VERSION_TOO_OLD;
            }
            else {
                // unknown version. Probably newer as the service.
                return AidlConstants.RESULT_VERSION_TOO_NEW;
            }
        }

    };
}
