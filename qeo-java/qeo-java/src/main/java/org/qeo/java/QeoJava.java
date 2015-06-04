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

package org.qeo.java;

import org.qeo.QeoFactory;
import org.qeo.internal.BaseFactory;
import org.qeo.internal.java.EntityAccessorJava;
import org.qeo.jni.NativeFactory;
import org.qeo.jni.NativeQeo;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class should be used for retrieval of a Qeo entity factory for plain Java.
 */
public final class QeoJava
    extends BaseFactory
{
    private static final Logger LOG = Logger.getLogger("QeoJava");
    private NativeQeo mNativeFactory;
    private final QeoConnectionListener mListener;
    private static ArrayList<QeoJava> sQeoJavaFactories = new ArrayList<QeoJava>();

    private QeoJava(int domainId, QeoConnectionListener listener)
    {
        super(domainId, new EntityAccessorJava());
        mNativeFactory = null;
        mListener = listener;
    }

    private void setNativeFactory(NativeQeo nativeQeo)
    {
        if (nativeQeo == null) {
            throw new IllegalArgumentException("NativeQeo must not be null");
        }
        ((EntityAccessorJava) getEntityAccessor()).setNativeFactory(nativeQeo);
        mNativeFactory = nativeQeo;
    }

    /**
     * Returns a string containing the Qeo version.
     * 
     * @return The Qeo version string.
     */
    public static String getVersionString()
    {
        return NativeQeo.nativeGetVersionString();
    }

    /**
     * Retrieve a Qeo factory that can be used to create Qeo readers and writers. Default configuration is used.<br>
     * Each call to this function will result in a different factory instance. Each factory can create it's own readers
     * and writers and should be closed separately.
     * 
     * @param listener object that provides call back methods. Must be unique for every initQeo call.
     */
    public static void initQeo(QeoConnectionListener listener)
    {
        initQeo(QeoFactory.DEFAULT_ID, listener);
    }

    /**
     * Retrieve a Qeo factory that can be used to create Qeo readers and writers. Default configuration is used. At the
     * moment, only one can be created.
     * 
     * @param id The identity for which the factory is retrieved. Use QeoFactory.DEFAULT_ID for the default realm,
     *            QeoFactory.OPEN_ID for the open realm.
     * @param listener object that provides call back methods. Must be unique for every initQeo call.
     */
    public static void initQeo(int id, QeoConnectionListener listener)
    {
        QeoJava qeoJavaFactory;
        NativeQeo qeoNativeFactory;

        if (listener == null) {
            throw new IllegalArgumentException("Listener needed!");
        }
        synchronized (QeoJava.class) {
            qeoJavaFactory = new QeoJava(id, listener);
            LOG.fine("Created factory: " + qeoJavaFactory + "(listener: " + listener + ")");
            sQeoJavaFactories.add(qeoJavaFactory);
            LOG.fine("calling initNativeQeo for domain " + id);
            qeoNativeFactory = NativeQeo.createNativeQeo(id, listener);
            qeoJavaFactory.setNativeFactory(qeoNativeFactory);
        }
        //don't put next call in the synchronized block. This will block on authentication if needed.
        //this would mean no other factory could be created while the authentication call is active.
        qeoNativeFactory.init(qeoJavaFactory, listener);
        LOG.fine("initQeo done for domain " + id);
    }

    /**
     * Closes a Qeo factory represented by the listener.
     * 
     * @param listener object that provides call back methods
     */
    public static synchronized void closeQeo(QeoConnectionListener listener)
    {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        boolean found = false;
        for (QeoJava qeoFactory : sQeoJavaFactories) {
            if (qeoFactory.mListener == listener) {
                qeoFactory.cleanup();
                sQeoJavaFactories.remove(qeoFactory);
                // Close native qeo
                NativeQeo.closeNativeQeo(qeoFactory);
                found = true;
                break;
            }
        }
        if (!found) {
            LOG.warning("Trying to close a factory that is not open: " + listener);
        }
    }

    /**
     * Suspend Qeo operations. All non-vital network connections are closed and timers are stopped.
     * 
     * @see org.qeo.Notifiable
     * @see org.qeo.java.QeoConnectionListener#onWakeUp(java.lang.String)
     * @see #resume()
     */
    public static void suspend()
    {
        NativeQeo.suspend();
    }

    /**
     * Resume Qeo operations. Re-establishes all connections as if nothing happened.
     * 
     * @see #suspend()
     */
    public static void resume()
    {
        NativeQeo.resume();
    }

    /**
     * Set the overall log level.
     * 
     * @param level the new log level.
     */
    public static void setLogLevel(final Level level)
    {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run()
            {
                for (final Handler handler : Logger.getLogger("").getHandlers()) {
                    handler.setLevel(level);
                }
                Logger.getLogger("").setLevel(level);
                return null;
            }
        });
        NativeQeo.setLogLevel(level);
    }

    @Override
    public long getUserId()
    {
        return NativeFactory.getUserId(mNativeFactory);
    }

    @Override
    public long getRealmId()
    {
        return NativeFactory.getRealmId(mNativeFactory);
    }

    @Override
    public String getRealmUrl()
    {
        return NativeFactory.getRealmUrl(mNativeFactory);
    }
}
