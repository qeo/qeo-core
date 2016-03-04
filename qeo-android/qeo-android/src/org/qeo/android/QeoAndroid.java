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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.QeoFactory;
import org.qeo.android.activity.ServiceInstallActivity;
import org.qeo.android.exception.QeoServiceNotFoundException;
import org.qeo.android.internal.AidlConstants;
import org.qeo.android.internal.EntityAccessorAndroid;
import org.qeo.android.internal.ParcelableDeviceId;
import org.qeo.android.internal.QeoConnection;
import org.qeo.android.internal.QeoLogger;
import org.qeo.android.internal.ServiceConnection;
import org.qeo.android.internal.ServiceDisconnectedException;
import org.qeo.internal.BaseFactory;
import org.qeo.system.DeviceId;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.widget.Toast;

/**
 * This class should be used for retrieval of a Qeo entity factory for Android.
 */
public final class QeoAndroid
    extends BaseFactory
{
    /**
     * Tag used in logcat.
     */
    public static final String TAG = "Qeo";

    /** Packagename of the qeo android service. */
    public static final String QEO_SERVICE_PACKAGE = "org.qeo.android.service";

    private static final Logger LOG = Logger.getLogger(TAG);
    private final QeoConnectionListener mListener;

    /**
     * Static initializer will merely set the LOG level.
     */
    static {
        QeoLogger.init();
        setLogLevel(Level.INFO);
    }

    private QeoAndroid(int domainId, QeoConnectionListener listener, EntityAccessorAndroid rwAndroid)
    {
        super(domainId, rwAndroid);
        LOG.fine("QeoAndroid() called");
        this.mListener = listener;
    }

    /**
     * Initializes a Qeo factory that can be used to create Qeo readers and writers.<br>
     * This factory should be closed when not needed anymore using {@link #closeQeo(QeoConnectionListener)}
     *
     * @param context  The Android application context
     * @param listener The listener used to inform the user when Qeo is ready to be used. Must be unique for every
     *                 initQeo call.
     * @param looper   The looper to be used for the ServiceReader listeners.
     * @throws IllegalArgumentException if context is null or is not an application context
     * @throws IllegalStateException    if multiple factories are being created with different looper or context
     */
    public static void initQeo(Context context, QeoConnectionListener listener, Looper looper)
    {
        initQeo(context, listener, looper, QeoFactory.DEFAULT_ID);
    }

    /**
     * Initializes a Qeo factory that can be used to create Qeo readers and writers.<br>
     * This factory should be closed when not needed anymore using {@link #closeQeo(QeoConnectionListener)}
     *
     * @param context  The Android application context
     * @param listener The listener used to inform the user when Qeo is ready to be used. Must be unique for every
     *                 initQeo call.
     * @param looper   The looper to be used for the ServiceReader listeners.
     * @param id       The identity for which the factory is retrieved. Use QeoFactory.DEFAULT_ID for the default realm,
     *                 QeoFactory.OPEN_ID for the open realm.
     * @throws IllegalArgumentException if context is null or is not an application context
     * @throws IllegalStateException    if multiple factories are being created with different looper or context
     */
    public static void initQeo(Context context, final QeoConnectionListener listener, Looper looper, int id)
    {
        if (listener == null) {
            throw new IllegalArgumentException("Listener needed!");
        }
        Handler handler;
        if (looper == null) {
            handler = new Handler();
        }
        else {
            handler = new Handler(looper);
        }
        Context appContext = context.getApplicationContext();

        // service ok, setup connection.
        try {
            QeoConnection qeoConnection = QeoConnection.getInstance(appContext, handler);
            EntityAccessorAndroid rwAndroid = new EntityAccessorAndroid(id, qeoConnection, looper);
            QeoAndroid qeo = new QeoAndroid(id, listener, rwAndroid);
            qeoConnection.addFactory(qeo);
        }
        catch (QeoServiceNotFoundException e) {
            LOG.info("Qeo service not yet installed");
            if (!listener.onServiceNotInstalled()) {
                // app did not handle service installation dialog itself, show default.
                try {
                    Intent i = new Intent(appContext, ServiceInstallActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    appContext.startActivity(i);
                }
                catch (ActivityNotFoundException ex) {
                    LOG.log(Level.SEVERE, "Can't launch qeo install activity", ex);
                    Toast.makeText(appContext, "Qeo not installed, can't use Qeo.", Toast.LENGTH_LONG).show();
                }

            }

            // raise error that service is not installed
            handler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    // do this in handler thread to make sure the init returns before the callback is called
                    listener.onQeoError(new QeoServiceNotFoundException());
                }
            });

            // return, nothing can be done.
        }
    }

    /**
     * Initializes a Qeo factory that can be used to create Qeo readers and writers.<br>
     * Each call to this function will result in a different factory instance. Each factory can create it's own readers
     * and writers and should be closed separately.<br>
     * This factory should be closed when not needed anymore using {@link #closeQeo(QeoConnectionListener)}
     *
     * @param context  The Android application context
     * @param listener The listener used to inform the user when Qeo is ready to be used. Must be unique for every
     *                 initQeo call.
     * @throws IllegalArgumentException if context is null or is not an application context
     * @throws IllegalStateException    if multiple factories are being created with different looper or context
     */
    public static void initQeo(Context context, QeoConnectionListener listener)
    {
        initQeo(context, listener, null, QeoFactory.DEFAULT_ID);
    }

    /**
     * Initializes a Qeo factory that can be used to create Qeo readers and writers.<br>
     * This factory should be closed when not needed anymore using {@link #closeQeo(QeoConnectionListener)}
     *
     * @param context  The Android application context
     * @param listener The listener used to inform the user when Qeo is ready to be used. Must be unique for every
     *                 initQeo call.
     * @param id       The identity for which the factory is retrieved. Use QeoFactory.DEFAULT_ID for the default realm,
     *                 QeoFactory.OPEN_ID for the open realm.
     * @throws IllegalArgumentException if context is null or is not an application context
     * @throws IllegalStateException    if multiple factories are being created with different looper or context
     */
    public static void initQeo(Context context, final QeoConnectionListener listener, int id)
    {
        initQeo(context, listener, null, id);
    }

    /**
     * Closes a Qeo factory represented by the listener.
     *
     * @param listener The listener used to create the factory.
     */
    public static void closeQeo(QeoConnectionListener listener)
    {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        try {
            LOG.fine("Closing QeoAndroid factory");
            QeoConnection.getInstance().closeFactory(listener);
        }
        catch (IllegalStateException exception) {
            LOG.fine("Exception caught while closing factory");
        }
    }

    /**
     * Suspend Qeo operations. All non-vital network connections are closed and timers are stopped.
     *
     * @see org.qeo.Notifiable
     * @see org.qeo.android.QeoConnectionListener#onWakeUp(java.lang.String)
     * @see #resume()
     */
    public static void suspend()
    {
        LOG.fine("calling suspend() to qeo-android-service");
        QeoConnection.getInstance().suspend();
    }

    /**
     * Resume Qeo operations. Re-establishes all connections as if nothing happened.
     *
     * @see #suspend()
     */
    public static void resume()
    {
        LOG.fine("calling resume() to qeo-android-service");
        QeoConnection.getInstance().resume();
    }

    /**
     * Get the deviceId if the device running this service.
     *
     * @return The deviceId or null if an error occurred
     */
    public static DeviceId getDeviceId()
    {
        DeviceId result = null;
        LOG.fine("calling getParcelableDeviceId() to qeo-android-service");
        try {
            // let's hope the connection is already initialized...
            ParcelableDeviceId parcelableDeviceId = QeoConnection.getInstance().getProxy().getDeviceId();
            result = new DeviceId();
            result.upper = parcelableDeviceId.getMSB();
            result.lower = parcelableDeviceId.getLSB();
        }
        catch (final Exception e) {
            LOG.log(Level.SEVERE, "Error fetching deviceId ", e);
        }
        return result;
    }

    /**
     * Configures the realm, user name and device name to be used when registering this device.
     *
     * @param realm      The realm name
     * @param userName   The user name
     * @param deviceName The device name
     */
    public static void setSecurityConfig(String realm, String userName, String deviceName)
    {
        QeoConnection.setSecurityConfig(realm, userName, deviceName);
    }

    /**
     * Set the OAuth code. This method needs to be called when QeoConnectionListener.onStartAuthentication has been
     * overridden and true was returned in that callback method.
     *
     * @param code The new OAuth code
     * @deprecated use {@link #continueAuthenticationOAuthCode(String)}
     */
    @Deprecated
    public static void setOAuthCode(String code)
    {
        continueAuthenticationOAuthCode(code);
    }

    private static void continueAuthentication(int type, String data)
    {
        LOG.fine("calling continueAuthentication to qeo-android-service");
        try {
            // let's hope the connection is already initialized...
            QeoConnection.getInstance().getProxy().continueAuthentication(type, data);
        }
        catch (final RemoteException e) {
            LOG.log(Level.SEVERE, "Error continueing authenticatino", e);
        }
        catch (ServiceDisconnectedException ex) {
            ex.throwNotInitException();
        }
    }

    /**
     * Continue authentication using an OAuth code. (used in OpenId)
     * This method needs to be called when {@link QeoConnectionListener#onStartAuthentication()} has been
     * overridden and true was returned in that callback method.
     *
     * @param code The Oauth code.
     */
    public static void continueAuthenticationOAuthCode(String code)
    {
        continueAuthentication(AidlConstants.AUTHENTICATION_DATA_CODE, code);
    }

    /**
     * Continue authentication using a JWT token. (used in OpenId connect)
     * This method needs to be called when {@link QeoConnectionListener#onStartAuthentication()} has been
     * overridden and true was returned in that callback method.
     *
     * @param jwt the token.
     */
    public static void continueAuthenticationJWT(String jwt)
    {
        continueAuthentication(AidlConstants.AUTHENTICATION_DATA_JWT, jwt);
    }

    /**
     * Continue authentication using an OTC.
     * This method needs to be called when {@link QeoConnectionListener#onStartAuthentication()} has been
     * overridden and true was returned in that callback method.
     *
     * @param otc The otc.
     * @param url The url.
     */
    public static void continueAuthenticationOTC(String otc, String url)
    {
        continueAuthentication(AidlConstants.AUTHENTICATION_DATA_OTC, otc + "|" + url);
    }

    /**
     * Cancel authentication.
     * This method needs to be called when {@link QeoConnectionListener#onStartAuthentication()} has been
     * overridden and true was returned in that callback method.
     */
    public static void continueAuthenticationCancel()
    {
        continueAuthentication(AidlConstants.AUTHENTICATION_DATA_CANCEL, null);
    }

    /**
     * Set the overall log level.
     *
     * @param level the new log level.
     */
    public static void setLogLevel(Level level)
    {
        Logger.getLogger("").setLevel(level);
    }

    private EntityAccessorAndroid getEntityAccessorAndroid()
    {
        return (EntityAccessorAndroid) mEntityAccessor;
    }

    /**
     * Get the callback listener associated with this factory.
     *
     * @return The listener
     */
    public QeoConnectionListener getListener()
    {
        return mListener;
    }

    @Override
    public long getUserId()
    {
        if (ServiceConnection.isConnected()) {
            try {
                return getEntityAccessorAndroid().getQeoConnection().getProxy().factoryGetUserId(mDomainId);
            }
            catch (final RemoteException e) {
                LOG.log(Level.SEVERE, "Error fetching userId", e);
            }
            catch (ServiceDisconnectedException ex) {
                ex.throwNotInitException();
            }
        }
        return 0;
    }

    @Override
    public long getRealmId()
    {
        if (ServiceConnection.isConnected()) {
            try {
                return getEntityAccessorAndroid().getQeoConnection().getProxy().factoryGetRealmId(mDomainId);
            }
            catch (final RemoteException e) {
                LOG.log(Level.SEVERE, "Error fetching userId", e);
            }
            catch (ServiceDisconnectedException ex) {
                ex.throwNotInitException();
            }
        }
        return 0;
    }

    @Override
    public String getRealmUrl()
    {
        if (ServiceConnection.isConnected()) {
            try {
                return getEntityAccessorAndroid().getQeoConnection().getProxy().factoryGetRealmUrl(mDomainId);
            }
            catch (final RemoteException e) {
                LOG.log(Level.SEVERE, "Error fetching userId", e);
            }
            catch (ServiceDisconnectedException ex) {
                ex.throwNotInitException();
            }
        }
        return null;
    }

}
