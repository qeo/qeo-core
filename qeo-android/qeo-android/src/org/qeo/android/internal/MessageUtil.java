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

package org.qeo.android.internal;

/**
 * This utility class contains some finals used in messages send from the AIDL communication thread to the applications
 * thread.
 */
public final class MessageUtil
{
    private MessageUtil()
    {

    }

    /**
     * Message send from the AIDL communication thread to the applications thread when no more data is available.
     */
    public static final int MSG_NO_MORE_DATA = 1;

    /**
     * Message send from the AIDL communication thread to the applications thread when removed data is available.
     */
    public static final int MSG_REMOVE = 2;

    /**
     * Message send from the AIDL communication thread to the applications thread when data is available.
     */
    public static final int MSG_DATA = 3;

    /**
     * Message send from the AIDL communication thread to the applications thread when data is available on a state
     * reader.
     */
    public static final int MSG_UPDATE = 4;

    /** Message send from the AIDL thread to the application thread when an exception is raised. */
    public static final int MSG_EXCEPTION = 5;

    /*
     * Collection of key id's used inside bundles to identify a specific bundle item.
     */

    /** To specify the collection of data fields. */
    public static final String KEY_DATA = "Data";
    /** Key to mark an exception in a bundle. */
    public static final String KEY_EXCEPTION = "Exception";

}
