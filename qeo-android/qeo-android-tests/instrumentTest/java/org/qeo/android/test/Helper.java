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

package org.qeo.android.test;

import org.qeo.android.QeoAndroid;

/**
 * 
 */
public final class Helper
{
    private Helper()
    {
    }

    /**
     * Check if the service is embedded or not
     */
    public static synchronized boolean isEmbedded()
    {
        try {
            Class.forName(QeoAndroid.QEO_SERVICE_PACKAGE + ".ServiceApplication");
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }
}
