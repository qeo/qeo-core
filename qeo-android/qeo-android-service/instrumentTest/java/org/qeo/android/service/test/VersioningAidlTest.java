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

package org.qeo.android.service.test;

import org.qeo.android.internal.AidlConstants;
import org.qeo.android.internal.IServiceQeoVersion;
import org.qeo.android.service.QeoServiceVersion;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.test.ServiceTestCase;

/**
 * 
 */
public class VersioningAidlTest
    extends ServiceTestCase<QeoServiceVersion>
{

    public VersioningAidlTest()
    {
        super(QeoServiceVersion.class);
    }

    private IServiceQeoVersion doBind()
    {
        IBinder b = this.bindService(new Intent(getContext(), QeoServiceVersion.class));
        return IServiceQeoVersion.Stub.asInterface(b);
    }

    public void testVersionOk()
        throws RemoteException
    {
        IServiceQeoVersion service = doBind();
        assertEquals(AidlConstants.RESULT_VERSION_OK, service.checkVersionString(AidlConstants.AIDL_SERVICE_ACTION_V1));
    }

    public void testVersionOld()
        throws RemoteException
    {
        IServiceQeoVersion service = doBind();
        assertEquals(AidlConstants.RESULT_VERSION_TOO_OLD,
            service.checkVersionString(AidlConstants.AIDL_SERVICE_ACTION_V0));
    }

    public void testVersionNew()
        throws RemoteException
    {
        IServiceQeoVersion service = doBind();
        // just send unknown version string. This should be interpreted by service as being too new.
        assertEquals(AidlConstants.RESULT_VERSION_TOO_NEW, service.checkVersionString("FAKE"));
    }
}
