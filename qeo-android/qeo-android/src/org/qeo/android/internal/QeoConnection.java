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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.android.QeoAndroid;
import org.qeo.android.QeoConnectionListener;
import org.qeo.android.exception.ManifestParseException;
import org.qeo.android.exception.ManifestRejectedException;
import org.qeo.android.exception.OtcDialogCanceledException;
import org.qeo.android.exception.QeoSecurityInitFailedException;
import org.qeo.android.exception.QeoServiceNotFoundException;
import org.qeo.exception.QeoException;

import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;

/**
 * The Qeo connection depends on the existence of a service connection. It represents the layer on top of the service
 * connection. This Qeo connection singleton is created when calling getInstance(context, looper) for the first time. At
 * that time a corresponding service connection is being instantiated. One of the first jobs to be done by this Qeo
 * connection is the Qeo initialization towards the Qeo service.
 */
public final class QeoConnection
{
    private static final Object LOCK = new Object();
    private static boolean sDisableManifestPopup = false;

    private static final Logger LOG = Logger.getLogger(QeoAndroid.TAG);
    private static QeoConnection sInstance = null;

    private final ServiceConnection mServiceConnection;
    private final Map<QeoAndroid, Boolean> mFactories = new HashMap<QeoAndroid, Boolean>();

    private boolean mConnected = false;
    private final Handler mOnReadyHandler;
    private static String[] sManifest = null;
    private static String sRealm = null;
    private static String sUserName = null;
    private static String sDeviceName = null;
    private static boolean sSuspended = false;

    private QeoConnection(Context context, Handler handler)
        throws QeoServiceNotFoundException
    {
        LOG.fine("QeoConnection() called");
        mOnReadyHandler = handler;
        mServiceConnection = ServiceConnection.getInstance(context);
        mServiceConnection.addListener(mConnListener);
    }

    /**
     * Sets a flag to indicate that the manifest popup must not be shown but simply accepted. To have it's desired
     * effect, this method must be called before calling getInstance on this class.
     *
     * @remark Calling this method when the release version of the Qeo service is installed has no effect.
     */
    public static void disableManifestPopup()
    {
        sDisableManifestPopup = true;
    }

    /**
     * Get the Qeo connection singleton. If it does not exist, instantiate it.
     *
     * @param context The application context
     * @param handler The handler to post callbacks.
     *
     * @return the Qeo connection singleton
     *
     * @throws IllegalStateException if the context or looper parameter is null or if they do not correspond with the
     *             one already used
     * @throws QeoServiceNotFoundException if the Qeo service is not present
     */
    public static QeoConnection getInstance(Context context, Handler handler)
        throws QeoServiceNotFoundException
    {
        LOG.fine("QeoConnection.getInstance(...) called");
        // make sure to take same lock in getInstance as in close
        // (getInstance is static, close is not)
        synchronized (LOCK) {
            if (null == sInstance) {
                LOG.info("initialize Qeo connection");
                sInstance = new QeoConnection(context, handler);
            }
            else {
                if (context != null && context != sInstance.mServiceConnection.getContext()) {
                    throw new IllegalStateException(
                        "Creating multiple factories with a different context is unsupported");
                }
            }
        }
        return sInstance;
    }

    /**
     * Getter for the Qeo connection singleton.
     *
     * @return the Qeo connection singleton
     */
    public static QeoConnection getInstance()
    {
        synchronized (LOCK) {
            if (null == sInstance) {
                throw new IllegalStateException("Qeo connection not initialized");
            }
        }
        return sInstance;
    }

    private void callOnReady(final QeoAndroid factory)
    {
        if (sSuspended) {
            //if a factory is suspended before, always resume if a new factory is created.
            LOG.fine("Resuming as new factory is created");
            resume();
        }
        // call the onQeoReady in a Handler to ensure it comes on the correct thread.
        mOnReadyHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                synchronized (LOCK) {
                    LOG.fine("callOnReady for factory: " + factory + " will be called");
                    if (mFactories.containsKey(factory) && !mFactories.get(factory)) {
                        // check if listener is still there (can already be removed if the factory was already closed)
                        // and it has not yet been called (eg by creating a 2nd factory)
                        mFactories.put(factory, true); // mark as called
                        factory.getListener().onQeoReady(factory);
                    }
                    else {
                        LOG.fine("callOnReady for factory: " + factory + " is already called, ignoring!");
                    }
                }
            }
        });
    }

    private void callOnError(final QeoAndroid factory, final QeoException ex)
    {
        callOnError(factory, ex, false);
    }

    private void callOnError(final QeoAndroid factory, final QeoException ex, final boolean force)
    {
        // call the onError in a Handler to ensure it comes on the correct thread.
        mOnReadyHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                LOG.fine("callOnError for factory: " + factory);
                synchronized (LOCK) {
                    if (force) {
                        factory.getListener().onQeoError(ex);
                    }
                    else {
                        if (mFactories.containsKey(factory) && !mFactories.get(factory)) {
                            // check if listener is still there (can already be removed if the factory was already
                            // closed)
                            // and it has not yet been called (eg by creating a 2nd factory)
                            mFactories.put(factory, true); // mark as called
                            factory.getListener().onQeoError(ex);
                        }
                    }
                }
            }
        });
    }

    private boolean callOnStartAuthentication(final QeoAndroid factory)
    {
        synchronized (LOCK) {
            LOG.fine("callOnStartAuthentication for factory: " + factory + " will be called");
            if (mFactories.containsKey(factory) && !mFactories.get(factory)) {
                // check if listener is still there (can already be removed if the factory was already closed)
                // and it has not yet been called (eg by creating a 2nd factory)
                if (factory.getListener().onStartAuthentication()) {
                    return true;
                }
            }
            else {
                LOG.fine("callOnStartAuthentication for factory: " + factory + " is already called, ignoring!");
            }
        }
        return false;
    }

    private void callOnWakeUp(final QeoAndroid factory, final String typeName)
    {
        // call the onWakeUp in a Handler to ensure it comes on the correct thread.
        mOnReadyHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                LOG.fine("callOnWakeUp on: " + typeName + " for factory: " + factory);
                synchronized (LOCK) {
                    if (mFactories.containsKey(factory)) {
                        factory.getListener().onWakeUp(typeName);
                    }
                }
            }
        });
    }

    private void callOnBgnsConnected(final QeoAndroid factory, final boolean state)
    {
        // call the onWakeUp in a Handler to ensure it comes on the correct thread.
        mOnReadyHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                LOG.fine("callOnBgnsConnected for factory: " + factory);
                synchronized (LOCK) {
                    if (mFactories.containsKey(factory)) {
                        factory.getListener().onBgnsConnectionChange(state);
                    }
                }
            }
        });
    }

    /**
     * Add a Qeo factory for Android. When the Qeo connection is in the connected state, call the onQeoready callback.
     *
     * @param factory The factory to add.
     */
    public void addFactory(final QeoAndroid factory)
    {
        LOG.fine("QeoConnection.addFactory() called");
        synchronized (LOCK) {
            mFactories.put(factory, false);
            if (mConnected) {
                callOnReady(factory);
            }
        }
    }

    /**
     * Get a Qeo factory for Android corresponding to a specific listener.
     *
     * @param listener The listener used to look for a factory
     * @return the Qeo factory or null if not found.
     */
    private QeoAndroid getFactory(final QeoConnectionListener listener)
    {
        LOG.fine("QeoConnection.addFactory() called");
        synchronized (LOCK) {
            for (QeoAndroid factory : mFactories.keySet()) {
                if (factory.getListener() == listener) {
                    return factory;
                }
            }
        }
        return null;
    }

    /**
     * Close a factory for a connection listener.
     *
     * @param listener The listener.
     */
    public void closeFactory(final QeoConnectionListener listener)
    {
        QeoAndroid factory = getFactory(listener);
        LOG.fine("Closing factory: " + factory);
        if (factory != null) {
            factory.cleanup();
            removeFactory(factory);
        }
    }

    /**
     * Remove a factory. When the list of factories becomes empty, the connection is closed.
     *
     * @param factory The factory to remove.
     */
    private void removeFactory(QeoAndroid factory)
    {
        LOG.fine("QeoConnection.removeFactory() called");
        synchronized (LOCK) {
            mFactories.remove(factory);
            if (mFactories.isEmpty()) {
                close();
            }
        }
    }

    /**
     * Indication of the connection state.
     *
     * @return true if the Qeo connection is connected
     */
    boolean isConnected()
    {
        LOG.fine("QeoConnection.isConnected() called");
        return mConnected;
    }

    /**
     * return the service connections proxy, to be used for calling interface methods.
     * @throws ServiceDisconnectedException If the Qeo service is not connected.
     * @return the IServiceQeo proxy
     */
    public IServiceQeoV1 getProxy() throws ServiceDisconnectedException
    {
        return mServiceConnection.getProxy();
    }

    /**
     * Getter for the Qeo service callbacks.
     *
     * @return the IServiceCallback object
     */
    IServiceQeoCallback getServiceQeoCallback()
    {
        return mServiceQeoCb;
    }

    private static void setSuspended(boolean value)
    {
        sSuspended = value;
    }

    /**
     * Suspend Qeo.
     */
    public void suspend()
    {
        try {
            getProxy().suspend();
            setSuspended(true);
        }
        catch (final RemoteException e) {
            LOG.log(Level.SEVERE, "Error suspending", e);
        }
        catch (final ServiceDisconnectedException e) {
            e.throwNotInitException();
        }
    }

    /**
     * Resume Qeo.
     */
    public void resume()
    {
        try {
            getProxy().resume();
            setSuspended(false);
        }
        catch (final RemoteException e) {
            LOG.log(Level.SEVERE, "Error resuming", e);
        }
        catch (final ServiceDisconnectedException e) {
            e.throwNotInitException();
        }
    }

    private void close()
    {
        LOG.fine("QeoConnection.close() called");
        synchronized (LOCK) {
            sInstance = null;
            if (mConnected) {
                try {
                    IServiceQeoV1 proxy = getProxy();
                    if (proxy != null) {
                        // check if proxy is available. On service crash this will be null.
                        try {
                            proxy.unregister(mServiceQeoCb);
                        }
                        catch (final RemoteException e) {
                            LOG.log(Level.SEVERE, "Error unregistering service callback", e);
                        }
                    }
                }
                catch (final ServiceDisconnectedException e) {
                    LOG.warning("Service already disconnected while closing factory");
                }
            }
            mConnected = false;
            mServiceConnection.removeListener(mConnListener);
            LOG.fine("QeoConnection closed");
        }
    }

    private final ServiceConnectionListener mConnListener = new ServiceConnectionListener()
    {
        @Override
        public void onConnected()
        {
            LOG.fine("ServiceConnectionListener.onConnected() called");
            try {
                getProxy().setSecurityConfig(sRealm, sUserName, sDeviceName);
                getProxy().register(mServiceQeoCb);
            }
            catch (final Exception e) {
                LOG.log(Level.SEVERE, "", e);
            }
        }

        @Override
        public void onDisconnected()
        {
            LOG.fine("ServiceConnectionListener.onDisconnected() called");
            boolean wasConnected = mConnected;
            // make a shallow copy to avoid concurrent modification errors and deadlocks
            Set<QeoAndroid> factories = new HashSet<QeoAndroid>(mFactories.keySet());
            for (final QeoAndroid factory : factories) {
                removeFactory(factory); // remove factory
            }

            // notify client that everything is closed, do this at the very last at the client may try to setup a
            // new
            // connection directly from this call.
            for (final QeoAndroid factory : factories) {
                if (wasConnected) {
                    // if connection was established, send onQeoClosed
                    factory.getListener().onQeoClosed(factory); // send call to client app
                }
                else {
                    // if connection did not get estrablished, send onQeoError.
                    // onQeoReady would not have been called in this case.
                    factory.getListener().onQeoError(new QeoException("Could not initialize Qeo"));
                }
            }

        }

        @Override
        public void onError(QeoException ex)
        {
            LOG.fine("ServiceConnectionListener.onError() called");
            synchronized (LOCK) {

                for (final QeoAndroid factory : mFactories.keySet()) {
                    callOnError(factory, ex);
                }
            }
            close();
        }
    };

    /**
     * Read the manifest (text) file from the AssetsManagers.
     *
     * @param fileName the file to read
     * @return the content of the file represented as a String[]
     * @throws ManifestParseException if the manifest could not be found, opened, read or closed
     */
    private String[] readManifest(String fileName)
        throws ManifestParseException
    {
        String[] manifest = new String[]{};
        Context context = mServiceConnection.getContext();
        BufferedReader manifestReader = null;

        try {
            ArrayList<String> manifestList = new ArrayList<String>();
            InputStream manifestStream = context.getResources().getAssets().open(fileName);
            manifestReader = new BufferedReader(new InputStreamReader(manifestStream, "US-ASCII"));
            String line = null;
            while ((line = manifestReader.readLine()) != null) {
                manifestList.add(line);
            }
            manifest = manifestList.toArray(manifest);
        }
        catch (IOException e) {
            throw new ManifestParseException("Couldn't find or read the manifest file (assets/" + fileName + ")");
        }
        finally {
            try {
                if (manifestReader != null) {
                    manifestReader.close();
                }
            }
            catch (IOException e) {
                throw new ManifestParseException("Couldn't close the manifest file (assets/" + fileName + ")");
            }
        }

        return manifest;
    }

    /**
     * Push the manifest file contents towards the QeoService.
     */
    private void pushManifest()
    {
        final ParcelableException exception = new ParcelableException();
        try {
            if (sDisableManifestPopup) {
                LOG.info("Manifest popup disabled by application");
                sDisableManifestPopup = getProxy().disableManifestPopup();
            }
            String[] manifest = sManifest;
            if (manifest == null) {
                // normal case, manifest is read from the filesystem
                manifest = readManifest("QeoManifest.mf");
            }
            getProxy().pushManifest(mServiceQeoCb, manifest, exception);
        }
        catch (QeoException ex) {
            mConnListener.onError(ex);
        }
        catch (RemoteException e) {
            LOG.log(Level.SEVERE, "Failed to push manifest", e);
            mConnListener.onError(new QeoException("Failed to push manifest", e));
        }
        if (null != exception.getException()) {
            Exception ex = exception.getException();
            ManifestParseException qex = new ManifestParseException("Error in manifest: " + ex.getMessage(), ex);
            mConnListener.onError(qex);
        }
    }

    /**
     * Set the content of the manifest file instead of parsing the default file location. This should only be used for
     * special cases.
     *
     * @param content The unparsed manifest file content. This is an array containing 1 line of the file per element.
     */
    public static void setManifest(String[] content)
    {
        sManifest = content.clone();
    }

    /**
     * Configures the realm, user name and device name to be used when registering this device.
     *
     * @param realm The realm name
     * @param userName The user name
     * @param deviceName The device name
     */
    public static void setSecurityConfig(String realm, String userName, String deviceName)
    {
        sRealm = realm;
        sUserName = userName;
        sDeviceName = deviceName;
    }

    private final IServiceQeoCallback.Stub mServiceQeoCb = new IServiceQeoCallback.Stub()
    {

        @Override
        public void onRegistered()
            throws RemoteException
        {
            LOG.fine("IServiceQeoCallback.onRegistered() called");
            if (ServiceConnection.isConnected()) {
                pushManifest();
            }
            else {
                // can happen in a timing where the connection is closed but the service was just about to call this
                LOG.warning("onRegistered callback while not connected");
            }
        }

        @Override
        public void onManifestReady(boolean result)
            throws RemoteException
        {
            LOG.fine("IServiceQeoCallback.onManifestReady() called");
            synchronized (LOCK) {
                LOG.fine("IServiceQeoCallback.onManifestReady() connected");
                mConnected = true;
                if (result) {
                    LOG.fine("Manifest accepted!");
                    for (final QeoAndroid factory : mFactories.keySet()) {
                        callOnReady(factory);
                    }
                }
                else {
                    LOG.warning("Manifest rejected");
                    mConnListener.onError(new ManifestRejectedException());
                }

            }
        }

        @Override
        public void onOtpDialogCanceled()
            throws RemoteException
        {
            LOG.warning("OTP Dialog cancelled");
            mConnListener.onError(new OtcDialogCanceledException());
        }

        @Override
        public void onSecurityInitFailed(String reason)
            throws RemoteException
        {
            LOG.warning("Security init failed: " + reason);
            mConnListener.onError(new QeoSecurityInitFailedException(reason));
        }

        @Override
        public void onUnregistered()
            throws RemoteException
        {
            LOG.fine("IServiceQeoCallback.onUnregistered() called");
        }

        @Override
        public boolean onStartAuthentication()
            throws RemoteException
        {
            for (final QeoAndroid factory : mFactories.keySet()) {
                if (callOnStartAuthentication(factory)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onWakeUp(String typeName)
            throws RemoteException
        {
            for (final QeoAndroid factory : mFactories.keySet()) {
                callOnWakeUp(factory, typeName);
            }
        }

        @Override
        public void onBgnsConnected(boolean state)
            throws RemoteException
        {
            for (final QeoAndroid factory : mFactories.keySet()) {
                callOnBgnsConnected(factory, state);
            }
        }

    };
}
