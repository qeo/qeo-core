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

package org.qeo.android;

import org.qeo.QeoFactory;
import org.qeo.exception.QeoException;
import org.qeo.internal.QeoListener;

/**
 * Listener interface with callbacks to indicate the state of the Qeo factory.
 */
public abstract class QeoConnectionListener
    implements QeoListener
{
    /**
     * Called if the Qeo initialization is finished. I.e. if the service connection has been established and security is
     * initialized.
     * 
     * @param qeo The qeo factory created.
     */
    @Override
    public abstract void onQeoReady(QeoFactory qeo);

    /**
     * Called if the service connection has been lost for some reason. This typically happens when the process hosting
     * the service has crashed or been killed. You will receive a call to onQeoReady as soon as the service is back up
     * and running.
     * 
     * This function is not called when you explicitly do a Qeo.close(), in that case you need to do the cleanup
     * yourself.
     * 
     * @param qeo The qeo factory associated.
     */
    @Override
    public void onQeoClosed(QeoFactory qeo)
    {

    }

    /**
     * Called if something goes wrong during Qeo initialization.<br>
     * Either onQeoReady or this function gets called.
     * 
     * @param ex The error cause.
     */
    @Override
    public abstract void onQeoError(QeoException ex);

    /**
     * This callback can be overridden if a custom UI/action wants to be shown if the Qeo service is not available.<br>
     * If not overridden, a default activity will be shown.<br>
     * If overridden, it should return true to indicate that the app handles it itself.
     * 
     * @return should return true if overridden to disable default behavior.
     */
    public boolean onServiceNotInstalled()
    {
        return false;
    }

    /**
     * This callback can be overridden if a custom UI/action wants to be shown to log in.<br>
     * If not overridden, a default webview activity will be shown.<br>
     * If overridden, it should return true to indicate that the app handles it itself. When done so, the app <br>
     * should provide the OAuth code after a successful login was done. <br>
     * DO NOT PROVIDE THE OAUTH CODE IN THIS CALLBACK!<br>
     * After this function, one of the following must be called or Qeo will block forever:
     * <ul>
     *     <li>{@link org.qeo.android.QeoAndroid#continueAuthenticationOAuthCode(String)}</li>
     *     <li>{@link org.qeo.android.QeoAndroid#continueAuthenticationJWT(String)}</li>
     *     <li>{@link org.qeo.android.QeoAndroid#continueAuthenticationOTC(String, String)}</li>
     *     <li>{@link QeoAndroid#continueAuthenticationCancel()}</li>
     * </ul>
     * 
     * @return should return true if overridden to disable default behavior.
     */
    @Override
    public boolean onStartAuthentication()
    {
        return false;
    }

    /**
     * This callback needs to be overridden when using suspend. By default it will do nothing but you need to call the
     * resume method otherwise Qeo will remain suspended.
     *
     * @param typeName The name of the type for which data arrived. Can be null if the suspend call failed.
     *
     * @see org.qeo.android.QeoAndroid#suspend()
     * @see org.qeo.android.QeoAndroid#resume()
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
