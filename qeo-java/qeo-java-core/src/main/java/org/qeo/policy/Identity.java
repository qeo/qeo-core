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

package org.qeo.policy;

/**
 * An identity as used in Qeo policy files.
 */
public class Identity
{

    private final long mUID;

    /**
     * Initialize identity with given user ID.
     * 
     * @param uid The user ID.
     */
    public Identity(long uid)
    {
        mUID = uid;
    }

    /**
     * Get the user ID associated with this identity.
     * 
     * @return The user ID.
     */
    public long getUserID()
    {
        return mUID;
    }

    @Override
    public String toString()
    {
        return "Qeo Identity: UID: " + mUID;
    }
}
