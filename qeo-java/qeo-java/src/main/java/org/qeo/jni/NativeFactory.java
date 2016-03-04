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

package org.qeo.jni;

/**
 * Class to get information from a native factory.
 */
public final class NativeFactory
{
    private NativeFactory()
    {
    }

    /**
     * Get the realm Id.
     * 
     * @param nativeQeo The native factory Id.
     * @return realm id. -1 if invalid.
     */
    public static long getRealmId(NativeQeo nativeQeo)
    {
        return nativeGetRealmId(nativeQeo.getNativeFactory());
    }

    /**
     * Get the user Id.
     * 
     * @param nativeQeo the native factory Id.
     * @return user Id. -1 if invalid.
     */
    public static long getUserId(NativeQeo nativeQeo)
    {
        return nativeGetUserId(nativeQeo.getNativeFactory());
    }

    /**
     * Get the realm url.
     * 
     * @param nativeQeo the native factory Id.
     * @return Realm ur. null if invalid.
     */
    public static String getRealmUrl(NativeQeo nativeQeo)
    {
        String url = nativeGetRealmUrl(nativeQeo.getNativeFactory());
        if (url != null && url.endsWith("\n")) {
            // native might append a newline, remove it
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * Get the number of native factories currently created.
     * 
     * @return number of factories.
     */
    public static int getNumFactories()
    {
        return nativeGetNumFactories();
    }

    private static native long nativeGetRealmId(long factoryId);

    private static native long nativeGetUserId(long factoryId);

    private static native String nativeGetRealmUrl(long factoryId);

    private static native int nativeGetNumFactories();
}
