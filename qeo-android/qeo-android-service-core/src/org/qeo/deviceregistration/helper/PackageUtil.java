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

import java.util.List;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * Utility class to check is any required package is installed in device or not.
 */
public class PackageUtil
{
    /**
     *Constructor to avoid CheckStyle error- Utility classes should not have a public or default constructor.
     */
    protected PackageUtil()
    {

    }
    /**
     * Check if the specified package is installed or not.
     * 
     * @param ctx Android context object
     * @param packageName The package name
     * @return if the package is installed return true, otherwise return false.
     */
    public static boolean isInstalled(Context ctx, String packageName)
    {
        PackageManager pm = ctx.getPackageManager();
        List<PackageInfo> lstPackages = pm.getInstalledPackages(0);
        for (PackageInfo pi : lstPackages) {
            // Logger.print(pi.packageName);
            if (pi.packageName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

}
