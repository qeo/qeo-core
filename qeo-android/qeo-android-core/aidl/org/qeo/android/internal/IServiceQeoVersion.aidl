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

package org.qeo.android.internal;

/**
 * This is the interface to use the Qeo service.
 */
interface IServiceQeoVersion
{

    /**
     * Check if the AIDL version requested by the library is compatible with the service installed.
     * @param version Version string as will be used in action for the bind intent.
     * @return AidlConstants.RESULT_VERSION_OK if ok.
     *         AidlConstants.RESULT_VERSION_TOO_OLD if library is too old.
     *         AidlConstants.RESULT_VERSION_TOO_NEW if library is too new.
     */
    int checkVersionString(String version);

    /**
     * Check if the Qeo library is compatible with the service. Note: not used for now. Here for later usage if needed.
     * @param libraryVersion The version of the library.
     * @return if a positive number: the version of the service.
     *         If a negative number: the minimal required version of the library.
     */
    int checkVersion(int libraryVersion);

}
