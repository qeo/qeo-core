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

/**
 * Class containing default values for the Qeo Settings. Can be overridden if using the embedded service.<br>
 * Most of these paramaters have to be set before doing anything Qeo related or they won't work.
 */
public final class QeoDefaults
{

    // NOTE: some default values will be set differently for the standalone Qeo service from
    // ServiceApplication.onCreate()
    private QeoDefaults()
    {
    }

    private static String sQeoStorageDirOverride = null;
    private static boolean sRemoteRegistrationServiceAvailable = false;
    private static boolean sRemoteRegistrationListenerAvailable = false;
    private static boolean sManifestEnabled = false;
    private static boolean sProxySecurityEnabled = false;
    private static String sPublicUrl = "https://join.qeo.org";
    // uncomment this line to use projectq.be
    //private static String sPublicUrl = "https://join.projectq.be";
    private static String sOpenIdRedirectUrl = "urn:qeo:oauth:2.0:android:easy-install";
    private static boolean sAllowSelfSignedCertificate = false;

    /**
     * Get the qeo storage dir.
     *
     * @return null to use default, full path otherwise.
     */
    public static String getQeoStorageDir()
    {
        return sQeoStorageDirOverride;
    }

    /**
     * Override the default qeo storage dir.
     *
     * @param path Full path.
     */
    public static void setQeoStorageDir(String path)
    {
        sQeoStorageDirOverride = path;
    }

    /**
     * Flag to permanently disable the remote registration service.
     *
     * @return true if enabled.
     */
    public static boolean isRemoteRegistrationServiceAvailable()
    {
        return sRemoteRegistrationServiceAvailable;
    }

    /**
     * Flag to permanently disable the remote registration service.
     * If enabled, the device will display notifications of unregistered Qeo devices in the realm if:
     * <ul>
     * <li>The current device is a realm admin (registered via openid or openid connect)</li>
     * </ul>
     * Note: this will increase memory/power/battery usage.
     *
     * @param remoteRegistrationServiceAvailable false to disable
     */
    public static void setRemoteRegistrationServiceAvailable(boolean remoteRegistrationServiceAvailable)
    {
        sRemoteRegistrationServiceAvailable = remoteRegistrationServiceAvailable;
    }

    /**
     * Flag to disable the remote registration listener.<br>
     *
     * @return true if enabled.
     */
    public static boolean isRemoteRegistrationListenerAvailable()
    {
        return sRemoteRegistrationListenerAvailable;
    }

    /**
     * Flag to disable the remote registration listener.
     * If enabled, the device will display notifications of unregistered Qeo devices in the realm if:
     * <ul>
     * <li>At least 1 Qeo app is running (or the app itself in case of embedded service)</li>
     * <li>The current device is a realm admin (registered via openid or openid connect)</li>
     * </ul>
     *
     * @param remoteRegistrationListenerAvailable false to disable
     */
    public static void setRemoteRegistrationListenerAvailable(boolean remoteRegistrationListenerAvailable)
    {
        sRemoteRegistrationListenerAvailable = remoteRegistrationListenerAvailable;
    }

    /**
     * Flag to disable the manifest popup.
     *
     * @return true if enable.
     */
    public static boolean isManifestEnabled()
    {
        return sManifestEnabled;
    }

    /**
     * Flag to disable the manifest popup.
     *
     * @param manifestEnabled false to disable, true to enable
     */
    public static void setManifestEnabled(boolean manifestEnabled)
    {
        sManifestEnabled = manifestEnabled;
    }

    /**
     * Root resource url.
     *
     * @return The url.
     */
    public static String getPublicUrl()
    {
        return sPublicUrl;
    }

    /**
     * Root resource url.
     *
     * @param publicUrl the url.
     */
    public static void setPublicUrl(String publicUrl)
    {
        sPublicUrl = publicUrl;
    }

    /**
     * The url to be show on the openId redirect page.
     *
     * @return redirect url.
     */
    public static String getOpenIdRedirectUrl()
    {
        return sOpenIdRedirectUrl;
    }

    /**
     * The url to be show on the openId redirect page.
     *
     * @param openIdRedirectUrl The url.
     */
    public static void setsOpenIdRedirectUrl(String openIdRedirectUrl)
    {
        sOpenIdRedirectUrl = openIdRedirectUrl;
    }

    /**
     * Boolean to indicate if self-signed certificate is allowed.
     *
     * @return true if allowed, false otherwise
     */
    public static boolean isAllowSelfSignedCertificate()
    {
        return sAllowSelfSignedCertificate;
    }

    /**
     * Boolean to indicate if self-signed certificate is allowed.
     *
     * @param allowSelfSignedCertificate true if allowed, false otherwise
     */
    public static void setAllowSelfSignedCertificate(boolean allowSelfSignedCertificate)
    {
        sAllowSelfSignedCertificate = allowSelfSignedCertificate;
    }

    /**
     * Check if the applicationSecurity is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public static boolean isProxySecurityEnabled()
    {
        return sProxySecurityEnabled;
    }

    /**
     * Enable/disable proxySecurity.<br>
     * This triggers checks for readers/writer to belong to the application that created them.
     *
     * @param proxySecurityEnabled true if enabled, false otherwise.
     */
    public static void setProxySecurityEnabled(boolean proxySecurityEnabled)
    {
        sProxySecurityEnabled = proxySecurityEnabled;
    }

}
