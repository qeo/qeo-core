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

package org.qeo.java;

import org.qeo.QeoFactory;
import org.qeo.exception.QeoException;
import org.qeo.internal.QeoListener;

/**
 * Listener interface with callbacks to indicate the state of the Qeo factory.
 */
public abstract class QeoConnectionListener
    implements QeoListener
{
    @Override
    public abstract void onQeoReady(QeoFactory qeo);

    @Override
    public void onQeoClosed(QeoFactory qeo)
    {
    }

    @Override
    public abstract void onQeoError(QeoException ex);

    @Override
    public boolean onStartAuthentication()
    {
        return false;
    }

    /**
     * Let the application know the native part updated its status.
     * 
     * @param status The new status
     * @param reason The corresponding update reason
     */
    public void onStatusUpdate(String status, String reason)
    {
    }

    /**
     * This callback needs to be overridden when using suspend. By default it will do nothing but you need to call the
     * resume method otherwise Qeo will remain suspended.
     * 
     * @param typeName The name of the type for which data arrived. Can be null if the suspend call failed.
     * 
     * @see org.qeo.java.QeoJava#suspend()
     * @see org.qeo.java.QeoJava#resume()
     */
    @Override
    public void onWakeUp(String typeName)
    {
    }

    @Override
    public void onBgnsConnectionChange(boolean connected)
    {
    }
}
