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

package org.qeo.deviceregistration.helper;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class to contain some caching of the realm data.
 */
public final class RealmCache
{
    private static final Map<Long, String> REALM_NAMES;
    private static final Map<Long, String> USER_NAMES;

    static {
        REALM_NAMES = new HashMap<Long, String>();
        USER_NAMES = new HashMap<Long, String>();
    }

    private RealmCache()
    {
    }

    /**
     * Put a realm name in the cache.
     * 
     * @param id The id
     * @param name The name
     */
    public static void putRealmName(long id, String name)
    {
        REALM_NAMES.put(id, name);
    }

    /**
     * Get a realm name from the cache.
     * 
     * @param id The id
     * @return The name if found, null otherwise.
     */
    public static String getRealmName(long id)
    {
        return REALM_NAMES.get(id);
    }

    /**
     * Get a realm name based on a realmId.<br>
     * <b>note that this function is slow</b>
     * 
     * @param name the name.
     * @return The id if found, 0 otherwise.
     */
    public static long getRealmId(String name)
    {
        for (Entry<Long, String> entry : REALM_NAMES.entrySet()) {
            if (entry.getValue().equals(name)) {
                return entry.getKey();
            }
        }
        return 0;
    }

    /**
     * Clear all the realm names in the the cache.
     */
    public static void clearRealmCache()
    {
        REALM_NAMES.clear();
    }

    /**
     * Put an username in the cache.
     * 
     * @param id The id
     * @param name The name
     */
    public static void putUserName(long id, String name)
    {
        USER_NAMES.put(id, name);
    }

    /**
     * Get an username from the cache.
     * 
     * @param id The id
     * @return The name if found, null otherwise.
     */
    public static String getUserName(long id)
    {
        return USER_NAMES.get(id);
    }

    /**
     * Get an username based on a userId.<br>
     * <b>note that this function is slow</b>
     * 
     * @param name the name.
     * @return The id if found, 0 otherwise.
     */
    public static long getUserId(String name)
    {
        for (Entry<Long, String> entry : USER_NAMES.entrySet()) {
            if (entry.getValue().equals(name)) {
                return entry.getKey();
            }
        }
        return 0;
    }

    /**
     * Clear all the usernames in the the cache.
     */
    public static void clearUserCache()
    {
        USER_NAMES.clear();
    }

    /**
     * Get a list of user names in the cache.
     * 
     * @return The list of users.
     */
    public static Collection<String> getUsers()
    {
        return Collections.unmodifiableCollection(USER_NAMES.values());
    }

}
