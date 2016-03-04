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

package org.qeo.android.internal;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.qeo.android.QeoAndroid;
import org.qeo.android.exception.QeoServiceNotFoundException;
import org.qeo.android.exception.QeoServiceTooOldException;
import org.qeo.exception.QeoException;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

/**
 * The service connection is a singleton that represents the connection between Qeo library for Android and Qeo service.
 * The singleton will be created when calling getInstance(context) for the first time. at that time binding to the Qeo
 * service is also done.
 */
public final class ServiceConnection
{
    private static final String AIDL_INTERFACE_VERSION = AidlConstants.AIDL_SERVICE_ACTION_V1;
    private static final Logger LOG = Logger.getLogger("ServiceConnection");
    private static final Object LOCK = new Object();
    private static final String SERVICE_QEO = QeoAndroid.QEO_SERVICE_PACKAGE + ".QeoService";
    private static final String SERVICE_VERSION = QeoAndroid.QEO_SERVICE_PACKAGE + ".QeoServiceVersion";
    private static ServiceConnection sInstance = null;

    private final Context mContext;
    private final Set<ServiceConnectionListener> mListeners = new HashSet<ServiceConnectionListener>();

    private boolean mConnected = false;
    private boolean mConnectedVersion = false;
    private IServiceQeoV1 mServiceQeo = null;
    private static Boolean sUseEmbeddedService = null;
    private static boolean sVersionCheckOk = false;

    /**
     * Bind to the QeoService and store the context.
     * 
     * @param context
     * 
     * @throws QeoServiceNotFoundException
     */
    private ServiceConnection(Context context)
        throws QeoServiceNotFoundException
    {
        LOG.fine("ServiceConnection() called");
        if (!(context instanceof Application)) {
            throw new IllegalArgumentException("application context needed");
        }
        mContext = context;
        bindVersionService();
        LOG.fine("Qeo connection created");
    }

    /**
     * Get the service connection singleton. If it does not exist, instantiate it.
     * 
     * @param context The application context
     * 
     * @return the service connection singleton
     * 
     * @throws IllegalArgumentException if the context parameter is null
     * @throws QeoServiceNotFoundException if the Qeo service is not present
     */
    static ServiceConnection getInstance(Context context)
        throws QeoServiceNotFoundException
    {
        LOG.fine("ServiceConnection.getInstance(...) called");
        // make sure to take same lock in getInstance as in close
        // (getInstance is static, close is not)
        synchronized (LOCK) {
            if (null == sInstance) {
                if (null == context) {
                    throw new IllegalArgumentException("invalid context");
                }
                LOG.info("initialize Qeo service connection");
                sInstance = new ServiceConnection(context);
            }
        }
        return sInstance;
    }

    /**
     * Call this function befory any Qeo init to make use of an embedded Qeo service instead of the default service.
     */
    private static synchronized boolean useEmbeddedService(Context ctx)
    {
        if (sUseEmbeddedService != null) {
            // already figured out
            return sUseEmbeddedService;
        }

        LOG.fine("Detecting if service is embedded");
        // try to get embedded service class
        try {
            Class<?> serviceClass = Class.forName(QeoAndroid.QEO_SERVICE_PACKAGE + ".ServiceApplication");
            // found ServiceApplication class
            LOG.fine("found service class: " + serviceClass);

            // invoke the initServiceApp method to initialize global variables.
            Method initMethod = serviceClass.getMethod("initServiceApp", Context.class);
            initMethod.invoke(null, ctx);

            sUseEmbeddedService = true;
        }
        catch (ClassNotFoundException e) {
            LOG.info("Service not embedded");
            sUseEmbeddedService = false;
        }
        catch (Exception e) {
            // should never happen. If the class is found, this method should be there too.
            throw new IllegalStateException("Error initializing embedded service", e);
        }

        if (sUseEmbeddedService) {
            try {
                //check if default UI is also embedded
                Class<?> serviceClass = Class.forName(QeoAndroid.QEO_SERVICE_PACKAGE + ".ui.QeoDefaultsUI");
                // found ServiceApplication class
                LOG.fine("found UI service class: " + serviceClass);

                // invoke the init method to initialize global variables.
                Method initMethod = serviceClass.getMethod("init", Context.class);
                initMethod.invoke(null, ctx);

                sUseEmbeddedService = true;
            }
            catch (ClassNotFoundException e) {
                LOG.fine("No default qeo ui embedded");
                //embedded UI is optional
            }
            catch (Exception e) {
                // should never happen. If the class is found, this method should be there too.
                throw new IllegalStateException("Error initializing embedded UI", e);
            }
        }

        LOG.fine("Is service embedded? " + sUseEmbeddedService);
        return sUseEmbeddedService;
    }

    /**
     * Check if connected with the qeo service.
     * 
     * @return true if connection is available, false otherwise.
     */
    public static boolean isConnected()
    {
        synchronized (LOCK) {
            if (sInstance == null) {
                return false;
            }
        }
        return sInstance.mConnected;
    }

    /**
     * Close the service connection.
     */
    private void close()
    {
        LOG.fine("ServiceConnection.close() called");
        synchronized (LOCK) {
            unbind();
            unbindVersion();
            sInstance = null;

        }
        LOG.fine("ServiceConnection closed");
    }

    /**
     * Add a new service connection listener to this service connection. When the service connection is in the connected
     * state, call the onConnected callback.
     * 
     * @param listener The listener to add
     */
    void addListener(ServiceConnectionListener listener)
    {
        LOG.fine("ServiceConnection.addListener() called");
        synchronized (mListeners) {
            mListeners.add(listener);
            if (mConnected) {
                listener.onConnected();
            }
        }
    }

    /**
     * Get the context object.
     * 
     * @return The application context
     */
    Context getContext()
    {
        return mContext;
    }

    /**
     * Remove a listener. When the list of listeners becomes empty, the connection is closed.
     * 
     * @param listener The listener to remove.
     */
    void removeListener(ServiceConnectionListener listener)
    {
        LOG.fine("ServiceConnection.removeListener() called");
        synchronized (mListeners) {
            mListeners.remove(listener);
            if (mListeners.isEmpty()) {
                close();
            }
        }
    }

    /**
     * Getter for the IServiceQeo interface object used to communicate with the Qeo service.
     *
     * @throws ServiceDisconnectedException If the Qeo service is not connected.
     * @return the IServiceQeo object
     */
    public IServiceQeoV1 getProxy() throws ServiceDisconnectedException
    {
        synchronized (LOCK) {
            if (!mConnected) {
                throw new ServiceDisconnectedException();
            }
            return mServiceQeo;
        }
    }

    /**
     * Binds to the QeoVersion service.
     */
    private void bindVersionService()
        throws QeoServiceNotFoundException
    {
        LOG.fine("bindVersionService called. Cached? " + sVersionCheckOk);
        if (sVersionCheckOk) {
            // cache version check. If cached value is ok skip check next time.
            bindQeoService();
        }
        else {
            // version check not yet done, do it.
            Intent intent = getServiceIntent(SERVICE_VERSION);
            if (!mContext.bindService(intent, mServiceConnectionVersion, Context.BIND_AUTO_CREATE)) {
                if (sUseEmbeddedService) {
                    throw new IllegalStateException("Service not found while using embedded service. "
                        + "Are correct services added to the AndroidManifest? " + intent);
                }
                else {
                    throw new QeoServiceNotFoundException();
                }
            }
        }
    }

    /**
     * Binds to the real qeo service.
     */
    private void bindQeoService()
        throws QeoServiceNotFoundException
    {
        LOG.fine("bindQeoService called");
        Intent intent = getServiceIntent(SERVICE_QEO);
        // set the requested aidl version as action.
        // don't put this as an extras field as binder caching can return incorrect binder in that case.
        intent.setAction(AIDL_INTERFACE_VERSION);
        intent.setType(mContext.getPackageName());
        if (!mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            if (sUseEmbeddedService) {
                throw new IllegalStateException("Service not found while using embedded service. "
                    + "Are correct services added to the AndroidManifest? " + intent);
            }
            else {
                throw new QeoServiceNotFoundException();
            }
        }
    }

    private Intent getServiceIntent(String serviceName)
    {
        final Intent intent = new Intent();

        String pkgName = QeoAndroid.QEO_SERVICE_PACKAGE;
        if (useEmbeddedService(mContext)) {
            // embedded service, use own package name
            pkgName = mContext.getPackageName();
        }
        LOG.fine("Creating intent for " + pkgName + "@" + serviceName);
        intent.setClassName(pkgName, serviceName);
        return intent;
    }

    private static void setVersionCheck(boolean value)
    {
        sVersionCheckOk = value;
    }

    /**
     * Unbind this Qeo object to the Qeo service.
     */
    private void unbind()
    {
        LOG.fine("ServiceConnection.unbind() called: " + mConnected);
        if (mConnected) {
            mContext.unbindService(mConnection);
            mConnected = false;
        }
    }

    private void unbindVersion()
    {
        LOG.fine("unbindVersion() called: " + mConnectedVersion);
        if (mConnectedVersion) {
            mContext.unbindService(mServiceConnectionVersion);
            mConnectedVersion = false;
        }
    }

    /**
     * Class for interacting with the main interface of the QeoService.
     */
    private final android.content.ServiceConnection mConnection = new ServiceConnectionWrapper() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            LOG.fine("ServiceConnection.onServiceConnected() called: " + className);
            synchronized (LOCK) {
                mServiceQeo = IServiceQeoV1.Stub.asInterface(service);

                mConnected = true;
                LOG.fine("Service connection established");
            }
            for (final ServiceConnectionListener listener : mListeners) {
                listener.onConnected();
            }
        }
    };

    private final ServiceConnectionWrapper mServiceConnectionVersion = new ServiceConnectionWrapper() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder)
        {
            mConnectedVersion = true;
            LOG.fine("ServiceConnectionVersion connected: " + name);
            IServiceQeoVersion service = IServiceQeoVersion.Stub.asInterface(serviceBinder);
            try {

                // check if qeo service can handle the requested aidl version
                int result = service.checkVersionString(AIDL_INTERFACE_VERSION);
                switch (result) {
                    case AidlConstants.RESULT_VERSION_OK:
                        // all fine
                        setVersionCheck(true);
                        bindQeoService(); // bind real interface
                        unbindVersion(); // unbind self
                        break;
                    case AidlConstants.RESULT_VERSION_TOO_OLD:
                        notifyError(new QeoException("This Qeo version is no longer supported."));
                        break;
                    case AidlConstants.RESULT_VERSION_TOO_NEW:
                        notifyError(new QeoServiceTooOldException());
                        break;
                    default:
                        throw new IllegalArgumentException("Uknown returncode");
                }
            }
            catch (Exception e) {
                notifyError(new QeoException("Unknown Qeo error", e));
                return;
            }

        }
    };

    private abstract class ServiceConnectionWrapper
        implements android.content.ServiceConnection
    {
        protected void notifyError(QeoException ex)
        {
            for (final ServiceConnectionListener listener : mListeners) {
                listener.onError(ex);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className)
        {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            LOG.warning("Connection to qeo-android-service lost!");
            synchronized (LOCK) {
                mServiceQeo = null;
            }
            for (final ServiceConnectionListener listener : mListeners) {
                listener.onDisconnected();
                // this call will ensure all QeoConnection objects get destroyed which will then destroy the
                // ServiceConnection instance.
            }
        }
    }
}
