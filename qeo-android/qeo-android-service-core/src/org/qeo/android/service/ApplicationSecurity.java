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

import org.qeo.android.internal.IServiceQeoCallback;

import android.os.RemoteException;

/**
 * Class to contain security information (eg the manifest) from the connected application.
 */
public interface ApplicationSecurity
{

    /**
     * Register reader/writer id to be used for this application.
     *
     * @param id The id of the reader/writer
     */
    void registerReaderWriter(long id);


    /**
     * Unregister reader/writer id to be used for this application.
     *
     * @param id The id of the reader/writer
     */
    void unRegisterReaderWriter(long id);

    /**
     * Checks if this reader/writer is allowed to be used in this application.
     *
     * @param id The id of the reader/writer
     * @throws SecurityException if not allowed
     */
    void checkRegisteredReaderWriter(long id);

    /**
     * Insert manifest appinfo block the database. Clear the mAppName and mVersion when everything is saved in the
     * database.
     */
    void insertAppInfo();

    /**
     * Get the applicationversion of the application at the time the manifest was stored.
     *
     * @return The version number if known, -1 otherwise
     */
    int getAppVersion();

    /**
     * Check if the reader class is allowed for this application.
     *
     * @param reader The class name
     * @return true if allowed, false otherwise
     */
    boolean isAllowedReader(String reader);

    /**
     * Check if the writer class is allowed for this application.
     *
     * @param writer The class name
     * @return true if allowed, false otherwise
     */
    boolean isAllowedWriter(String writer);

    /**
     * Evaluate the manifest data of this class and return the result using the cb.
     *
     * @param serviceImpl Implementation of the service functions.
     * @param cb          The callback on which the onManifestReady should be called
     * @throws RemoteException Thrown when calling the onManifestReady callback fails.
     */
    void evaluateManifest(QeoServiceImpl serviceImpl, IServiceQeoCallback cb) throws RemoteException;

    /**
     * Parse the manifest and temporarily store its data.
     *
     * @param manifest The manifest to be parsed
     */
    void parseManifest(String[] manifest);
}
