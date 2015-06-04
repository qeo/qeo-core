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
 * Constants shared over aidl.
 */
public final class AidlConstants
{
    /** Version 0 of qeo service aidl. Only used for testing. */
    public static final String AIDL_SERVICE_ACTION_V0 = "QEO_SERVICE_V0";
    /** Version 1 of qeo service aidl. */
    public static final String AIDL_SERVICE_ACTION_V1 = "QEO_SERVICE_V1";

    /** Result code of service version check. Version is ok. */
    public static final int RESULT_VERSION_OK = 0;
    /** Result code of service version check. Version of the library is too old. */
    public static final int RESULT_VERSION_TOO_OLD = 1;
    /** Result code of service version check. Version of the library is too new. */
    public static final int RESULT_VERSION_TOO_NEW = -1;

    /** data type for cancelling on continueAuthentication call. */
    public static final int AUTHENTICATION_DATA_CANCEL = 1;
    /** data type for Outh code on continueAuthentication call. (used in OpenId) */
    public static final int AUTHENTICATION_DATA_CODE = 2;
    /** data type for JWT on continueAuthentication call. (used in OpenId connect)*/
    public static final int AUTHENTICATION_DATA_JWT = 3;
    /** data type for OTC code continueAuthentication call.*/
    public static final int AUTHENTICATION_DATA_OTC = 4;

    private AidlConstants()
    {
    }

}
