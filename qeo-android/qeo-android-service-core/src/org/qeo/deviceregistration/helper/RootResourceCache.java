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

package org.qeo.deviceregistration.helper;

import java.util.HashMap;
import java.util.Map;

/**
 * Cached version of the rootResource data.
 */
public final class RootResourceCache
{
    private static final Map<String, String> OPENID_PROVIDERS;

    static {
        OPENID_PROVIDERS = new HashMap<String, String>();
    }

    private RootResourceCache()
    {
    }

    /**
     * Store an openId provider.
     * 
     * @param name The name of the provider.
     * @param url The url of the provider.
     */
    public static void putOpenIdProvider(String name, String url)
    {
        OPENID_PROVIDERS.put(name, url);
    }

    /**
     * Get an openId provider by name.
     * 
     * @param name The name of the provider.
     * @return The url if known, null otherwise.
     */
    public static String getOpenIdProvider(String name)
    {
        return OPENID_PROVIDERS.get(name);
    }
}
