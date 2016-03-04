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

package org.qeo.android.service;

import java.util.logging.Logger;

import org.qeo.android.internal.IServiceQeoCallback;

import android.os.RemoteException;

/**
 * Implementation of the applicationSecurity for embedded service use.
 * This does almost nothing, everything is allowed.
 */
public class ApplicationSecurityEmbedded implements ApplicationSecurity
{
    private static final Logger LOG = Logger.getLogger("ApplicationSecurity");

    /**
     * Create an instance.
     */
    public ApplicationSecurityEmbedded()
    {
        LOG.fine("Create ApplicationSecurityEmbedded");
    }

    @Override
    public void registerReaderWriter(long id)
    {
        //nop
    }

    @Override
    public void unRegisterReaderWriter(long id)
    {
        //nop
    }

    @Override
    public void checkRegisteredReaderWriter(long id)
    {
        //nop
    }

    @Override
    public void insertAppInfo()
    {
        //nop
    }

    @Override
    public int getAppVersion()
    {
        return 0;
    }

    @Override
    public boolean isAllowedReader(String reader)
    {
        return true;
    }

    @Override
    public boolean isAllowedWriter(String writer)
    {
        return true;
    }

    @Override
    public void evaluateManifest(QeoServiceImpl serviceImpl, IServiceQeoCallback cb) throws RemoteException
    {
        cb.onManifestReady(true);
    }

    @Override
    public void parseManifest(String[] manifest)
    {
        //nop
    }
}
