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

/**
 * Constants used in REST.
 */
public final class RestFields
{
    private RestFields()
    {
    }

    /**
     * Realms.
     */
    public static final class Realms
    {
        private Realms()
        {
        }

        /** URL part. */
        public static final String URL = "realms";
        /** Root element of the array. */
        public static final String REALMS = "realms";
        /** ID field. */
        public static final String ID = "id";
        /** name field. */
        public static final String NAME = "name";
    }

    /**
     * Users.
     */
    public static final class Users
    {
        private Users()
        {
        }

        /** URL part. */
        public static final String URL = "users";
        /** Root element of the array. */
        public static final String USERS = "users";
        /** ID field. */
        public static final String ID = "id";
        /** Name field. */
        public static final String NAME = "name";
    }

    /**
     * Devices.
     */
    public static final class Devices
    {
        private Devices()
        {
        }

        /** URL part. */
        public static final String URL = "devices";
        /** Root element of the array. */
        public static final String DEVICES = "devices";
        /** HTTP parameter to select only for 1 user. */
        public static final String PARAM_USER_ID = "users";
        /** ID field. */
        public static final String ID = "id";
        /** Name field. */
        public static final String NAME = "name";
        /** DeviceId field. */
        public static final String DEVICE_ID = "device_id";
        /** Registration state. */
        public static final String STATE = "state";
        /** Realm field. */
        public static final String REALM = "realm";
        /** User field. */
        public static final String USER = "user";
    }
}
