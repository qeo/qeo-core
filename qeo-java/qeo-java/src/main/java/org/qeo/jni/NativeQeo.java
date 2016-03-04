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

import org.qeo.QeoFactory;
import org.qeo.exception.QeoException;
import org.qeo.internal.java.BgnsCallbacks;
import org.qeo.internal.java.QeoJavaCallbackHandler;
import org.qeo.java.QeoConnectionListener;
import org.qeo.java.QeoJava;
import org.qeo.system.DeviceInfo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class for native Qeo initialization.
 */
public final class NativeQeo implements QeoJavaCallbackHandler
{
    /**
     * Logging tag to be used by all Native*** classes.
     */
    public static final String TAG = "NativeQeo";

    private static final int LOG_NATIVE_DEBUG = 0;
    private static final int LOG_NATIVE_INFO = 1;
    private static final int LOG_NATIVE_WARNING = 2;
    private static final int LOG_NATIVE_ERROR = 3;

    private static NativeSecurity sSecurity = new NativeSecurity();

    private static final Logger LOG = Logger.getLogger(TAG);
    private static boolean sDebug = false;
    private static final Map<Integer, NativeQeo> NATIVE_FACTORIES;
    private static final Map<Integer, String> TCP_SERVERS;
    private static DeviceInfo sDeviceInfo = null;
    private static final Set<BgnsCallbacks> BGNS_CALLBACKS;

    private long mNativeFactory = 0;
    private long mRefCount = 0;
    private boolean mStartOtpRetrievalCalled = false;
    private boolean mInitDoneCalled = false;
    private boolean mQeoInitOk = false;
    private final int mId;
    private boolean mIsInit = false;
    private final Object mInitLock = new Object();
    private final List<QeoFactory> mFactory = new ArrayList<QeoFactory>();
    private final List<QeoConnectionListener> mListener = new ArrayList<QeoConnectionListener>();
    private static boolean sInvalidOTCEvent = false;

    private static void loadLibraryFromResource(URL lib)
    {
        try {
            final File libfile = File.createTempFile("lib-", ".lib");
            int len;
            final byte[] buffer = new byte[8192];

            libfile.deleteOnExit();
            InputStream in = null;
            OutputStream out = null;
            try {
                in = lib.openStream();
                out = new BufferedOutputStream(new FileOutputStream(libfile));
                while ((len = in.read(buffer)) > -1) {
                    out.write(buffer, 0, len);
                }
            }
            finally {
                if (null != out) {
                    out.close();
                }
                if (null != in) {
                    in.close();
                }
            }
            System.load(libfile.getAbsolutePath());
            if (!sDebug) {
                if (!libfile.delete()) {
                    LOG.warning("failed to delete temporary copy of native library");
                }
            }
        }
        catch (final IOException e) {
            throw new RuntimeException("failed to load native library: " + lib.toString(), e);
        }
    }

    /**
     * Load a native library into memory.
     *
     * @param name The name of the library
     */
    public static void loadLibrary(String name)
    {
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        URL nativeLib = null;

        if ((null != osName) && (null != osArch)) {
            final StringBuilder fname = new StringBuilder();

            osName = osName.replaceAll(" ", "");
            fname.append("/native/");
            fname.append(osArch);
            fname.append('/');
            fname.append(osName);
            if (sDebug) {
                fname.append("/debug");
            }
            fname.append("/lib");
            fname.append(name);
            fname.append(".so");
            if (osName.contains("Linux") || osName.contains("MacOSX")) {
                final String javaRuntime = System.getProperty("java.runtime.name");

                if ((javaRuntime == null) || !javaRuntime.toLowerCase(Locale.getDefault()).contains("android "
                    + "runtime")) {
                    System.out.println("loading " + fname.toString());
                    /* for non-Android try locating resource in jar */
                    nativeLib = NativeQeo.class.getResource(fname.toString());
                }
            }
        }
        if (null == nativeLib) {
            /* try loading from actual system */
            try {
                LOG.fine("Loading native library: " + name);
                System.loadLibrary(name);
            }
            catch (final UnsatisfiedLinkError e) {
                throw new RuntimeException("native library " + name + " for platform not found", e);
            }
        }
        else {
            /* load library from resource */
            LOG.fine("Loading native library from resource: " + nativeLib);
            loadLibraryFromResource(nativeLib);
        }
    }

    /**
     * Returns the object responsible for the security related native calls.
     *
     * @return The security object
     */
    public static NativeSecurity getNativeSecurity()
    {
        return sSecurity;
    }

    /**
     * Construct the Java counterpart of the qeo-c Factory. Only one factory can exist per id.
     *
     * @param id The id for which the factory is retrieved. Use DEFAULT for now.
     * @param listener The object that provides call back methods
     * @return the new created native qeo object
     */
    public static NativeQeo createNativeQeo(int id, QeoConnectionListener listener)
    {
        NativeQeo nativeQeo;

        LOG.fine("Creating NativeQeo with id:" + id);
        sInvalidOTCEvent = false;
        synchronized (NATIVE_FACTORIES) {
            if (NATIVE_FACTORIES.containsKey(id)) {
                LOG.finest("Use cached NativeQeo");
                nativeQeo = NATIVE_FACTORIES.get(id);
            }
            else {
                LOG.finest("Create new NativeQeo");
                nativeQeo = new NativeQeo(id, listener);
            }
            nativeQeo.mRefCount++;
            NATIVE_FACTORIES.put(id, nativeQeo);
        }

        return nativeQeo;
    }

    /**
     * Close the Java counterpart of the qeo-c Factory.
     *
     * @param qeoJava The qeoJava factory.
     */
    public static void closeNativeQeo(QeoJava qeoJava)
    {
        NativeQeo nativeQeo;

        int id = qeoJava.getDomainId();
        synchronized (NATIVE_FACTORIES) {
            if (NATIVE_FACTORIES.containsKey(id)) {
                LOG.fine("Closing NativeQeo with id: " + id);
                nativeQeo = NATIVE_FACTORIES.get(id);
                nativeQeo.close(qeoJava);
                LOG.fine("NativeQeo with id: " + id + " closed");
            }
        }
    }

    /**
     * Called by the native code when a background notification arrives. It will dispatch the wake-up call to all
     * listeners of all open factories.
     *
     * @param typeName The name of the type for which data arrived
     */
    @NativeCallback
    private static void dispatchWakeUp(String typeName)
    {
        LOG.fine("dispatchWakeUp");
        synchronized (NATIVE_FACTORIES) {
            for (final NativeQeo nativeQeo : NATIVE_FACTORIES.values()) {
                if (QeoJava.OPEN_ID != nativeQeo.mId) {
                    for (final QeoConnectionListener listener : nativeQeo.mListener) {
                        listener.onWakeUp(typeName);
                    }
                }
            }
        }
        synchronized (BGNS_CALLBACKS) {
            for (BgnsCallbacks cb : BGNS_CALLBACKS) {
                cb.dispatchWakeUp(typeName);
            }
        }
    }

    @NativeCallback
    private static void onBgnsConnected(int fd, boolean state)
    {
        LOG.fine("onBgnsConnected: " + state);
        synchronized (NATIVE_FACTORIES) {
            for (final NativeQeo nativeQeo : NATIVE_FACTORIES.values()) {
                if (QeoJava.OPEN_ID != nativeQeo.mId) {
                    for (final QeoConnectionListener listener : nativeQeo.mListener) {
                        listener.onBgnsConnectionChange(state);
                    }
                }
            }
        }
        synchronized (BGNS_CALLBACKS) {
            for (BgnsCallbacks cb : BGNS_CALLBACKS) {
                cb.onBgnsConnected(fd, state);
            }
        }
    }

    /**
     * Add listener for BGNS callbacks.
     * @param cb The listener.
     */
    public static void addBgnsCallbacks(BgnsCallbacks cb)
    {
        synchronized (BGNS_CALLBACKS) {
            BGNS_CALLBACKS.add(cb);
        }
    }

    /**
     * Remove a listener for BGNS callbacks.
     * @param cb The listener.
     */
    public static void removeBgnsCallbacks(BgnsCallbacks cb)
    {
        synchronized (BGNS_CALLBACKS) {
            BGNS_CALLBACKS.remove(cb);
        }
    }

    /**
     * Suspend Qeo.
     */
    public static void suspend()
    {
        LOG.fine("Suspend Qeo");
        bgnsSuspend();
    }

    /**
     * Resume Qeo.
     */
    public static void resume()
    {
        LOG.fine("Resume Qeo");
        bgnsResume();
    }

    /**
     * Release any native resources associated with the type.
     *
     * @param factory The factory on which the callbacks were called
     */
    private void close(QeoFactory factory)
    {
        LOG.fine("Closing factory: " + factory);
        boolean nativeCancel = false;
        if (mStartOtpRetrievalCalled) {
            LOG.fine("Cancel OTP registration");
            // this will also cleanup native factory, so don't do native close later on.
            nativeCancelRegistration();
            nativeCancel = true;
        }
        nativeInterrupt(mNativeFactory);
        synchronized (mInitLock) {
            boolean needClose = false;
            synchronized (NATIVE_FACTORIES) {
                mRefCount--;
                if (mRefCount == 0) {
                    NATIVE_FACTORIES.remove(mId);
                    needClose = true;
                }
            }
            if (needClose && mIsInit && !nativeCancel) {
                LOG.fine("Closing factory native: " + factory);
                nativeClose(mNativeFactory);
            }
            if (mFactory.contains(factory)) {
                LOG.fine("removing factory: " + factory);
                removeFactory(factory);
            }
        }
    }

    static {
        NATIVE_FACTORIES = new HashMap<Integer, NativeQeo>();
        TCP_SERVERS = new HashMap<Integer, String>();
        AccessController.doPrivileged(new PrivilegedAction<Object>()
        {
            @Override
            public Object run()
            {
                sDebug = "1".equals(System.getenv("QEO_DEBUG"));
                loadLibrary("qeoJNI");
                initCertValidator(CertChainValidator.class);
                return null;
            }
        });
        BGNS_CALLBACKS = new HashSet<BgnsCallbacks>();
    }

    /**
     * Construct the Java counterpart of the qeo-c Factory.
     *
     * @param id The id for which the factory is retrieved. Use DEFAULT for now.
     * @param listener The object that provides call back methods
     */
    private NativeQeo(int id, QeoConnectionListener listener)
    {
        mId = id;
        mListener.add(listener);
    }

    /**
     * Clear the state of static variables.
     */
    public static synchronized void clearState()
    {
        TCP_SERVERS.clear();
        sDeviceInfo = null;
    }

    /**
     * Construct the Java counterpart of the qeo-c Factory. Only one factory can exist per id.
     *
     * @param factory The factory on which the callbacks are called
     * @param listener The object that provides call back methods
     */
    private synchronized void addFactory(QeoFactory factory, QeoConnectionListener listener)
    {
        LOG.fine("Adding factory");
        int index = mListener.indexOf(listener);

        LOG.fine("NativeQeo factory: " + mNativeFactory + " updated ref count:" + mRefCount);
        if (index == -1) {
            mListener.add(listener);
            mFactory.add(factory);
        }
        else {
            mFactory.add(index, factory);
        }
        if (mInitDoneCalled) {
            if (mQeoInitOk) {
                listener.onQeoReady(factory);
            }
            else {
                if (sInvalidOTCEvent) {
                    listener.onQeoError(new QeoException("Invalid OTC"));
                }
                else {
                    listener.onQeoError(null);
                }
            }
        }
        LOG.fine("Adding factory done");
    }

    private synchronized void removeFactory(QeoFactory factory)
    {
        mListener.remove(mFactory.indexOf(factory));
        mFactory.remove(factory);
        LOG.fine("NativeQeo factory: " + mNativeFactory + " updated ref count:" + mRefCount);
    }

    /**
     * Sets the TCP server.
     *
     * @param id the id
     * @param tcpServer the TCP server
     */
    public static void setTcpServer(int id, String tcpServer)
    {
        TCP_SERVERS.put(id, tcpServer);
    }

    /**
     * Sets the device info.
     *
     * @param deviceInfo the device info
     */
    public static synchronized void setDeviceInfo(DeviceInfo deviceInfo)
    {
        sDeviceInfo = deviceInfo;
    }

    /**
     * Disable Qeo security. Will only work in debug build.
     *
     * @param noSecurity True to disable security, false to enable security.
     */
    public static void setSecurityDisabled(boolean noSecurity)
    {
        sSecurity.noSecurity(noSecurity);
    }

    /**
     * Initialize native Qeo.<br>
     * This will potentially block if security input is needed.<br>
     * Can be called multiple times and is thread safe, will return immediately if already initialized.
     *
     * @param factory the qeo factory
     * @param listener the connection listener
     *
     */
    public void init(QeoFactory factory, QeoConnectionListener listener)
    {
        synchronized (mInitLock) {
            addFactory(factory, listener);
            if (mIsInit) {
                return;
            }
            String tcpServer;

            synchronized (NativeQeo.class) {
                // this call may initialize globals for all factories, so make sure only to run 1 at once.
                mNativeFactory = nativeOpen(mId);
            }
            if (mNativeFactory == 0) {
                throw new IllegalStateException("Can't create native factory");
            }
            if (TCP_SERVERS.containsKey(mId)) {
                tcpServer = TCP_SERVERS.get(mId);
                if (null != tcpServer) {
                    configTcpServer(tcpServer);
                }
            }
            configDeviceInfo(); // set the deviceinfo

            LOG.fine("native init for domain " + mId);
            int rc;
            if (mId == QeoJava.OPEN_ID) {
                // for open domain no otp and status update functions have to be registered
                rc = nativeInit(this, mNativeFactory, null, null, "onQeoInitDone");
            }
            else {
                rc = nativeInit(this, mNativeFactory, "onStartOtpRetrieval", "onStatusUpdate", "onQeoInitDone");
            }
            NativeError.checkError(rc);
            mIsInit = true;
        }
    }

    /**
     * Trigger a refresh of the security policy.
     */
    public void refreshPolicy()
    {
        NativeError.checkError(nativeRefreshPolicy(mNativeFactory));
    }

    /**
     * Enable/disable UDP mode.
     *
     * @param enabled True if enabled, false if disabled.
     */
    public static void setUdpMode(boolean enabled)
    {
        NativeError.checkError(nativeSetUdpMode(enabled));
    }

    /**
     * Set the address where a Qeo forwarder is running. This should be called before calling
     * {@link #init(QeoFactory, QeoConnectionListener)}.
     *
     * @param tcpServer The TCP server IP address and port.
     */
    public void configTcpServer(String tcpServer)
    {
        NativeError.checkError(nativeConfigTcpServer(mNativeFactory, tcpServer));
    }

    /**
     * Set the Device Information to be used when initializing Qeo. This should be called before calling init}
     * .
     */
    private void configDeviceInfo()
    {
        synchronized (NativeQeo.class) {
            if (sDeviceInfo != null) {
                // only set if there is info
                String[] devInfoArray = new String[8];
                devInfoArray[0] = sDeviceInfo.manufacturer;
                devInfoArray[1] = sDeviceInfo.modelName;
                devInfoArray[2] = sDeviceInfo.productClass;
                devInfoArray[3] = sDeviceInfo.serialNumber;
                devInfoArray[4] = sDeviceInfo.hardwareVersion;
                devInfoArray[5] = sDeviceInfo.softwareVersion;
                devInfoArray[6] = sDeviceInfo.userFriendlyName;
                devInfoArray[7] = sDeviceInfo.configURL;

                long[] devIdArray = {sDeviceInfo.deviceId.upper, sDeviceInfo.deviceId.lower};
                NativeError.checkError(nativeConfigDeviceInfo(devInfoArray, devIdArray));
                sDeviceInfo = null; // clear data to avoid setting it multiple times.
            }
        }
    }

    /**
     * Set the Registration credentials for Qeo.
     *
     * @param otp The one time password to send to the authentication server.
     * @param url The URL of the authentication server.
     */
    public static void setRegistrationCredentials(String otp, String url)
    {
        NativeError.checkError(nativeSetRegistrationCredentials(otp, url));
    }

    /**
     * Cancel the registration process when asked for OTP credentials and URL.
     */
    public static void cancelRegistration()
    {
        NativeError.checkError(nativeCancelRegistration());
    }

    /**
     * The nativeFactory id can be requested by other Native... classes.
     *
     * @return the sNativeFactory
     */
    public long getNativeFactory()
    {
        return mNativeFactory;
    }

    /**
     * Set DDS flag. This will only have effect if qeo-c is compiled in debug mode.
     *
     * @param key The key
     * @param value The value
     */
    public static void setDssFlag(String key, String value)
    {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Invalid key " + key);
        }
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Invalid value " + value);
        }
        LOG.fine("Setting DDS flag " + key + "=" + value);
        if (nativeSetDdsParameter(key, value) != 0) {
            LOG.warning("Can't set DDS parameter " + key + "=" + value + " (are you using a debug build?)");
        }
    }

    /**
     * Set Qeo parameter.
     *
     * @param key The key
     * @param value The value
     */
    public static void setQeoParameter(String key, String value)
    {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Invalid key " + key);
        }
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Invalid value " + value);
        }
        LOG.fine("Setting Qeo parameter " + key + "=" + value);
        int rc = nativeSetQeoParameter(key, value);
        NativeError.checkError(rc, "Can't set Qeo parameter " + key + "=" + value);
    }

    /**
     * Cleanup native part.<br>
     * This preDestroy should be called <b>before</b> the DDS domain participants are removed. This ensures the DDS core
     * thread is still running and hence DDS cleanup can be done here.
     */
    public static void preDestroy()
    {
        int rc = nativePreDestroy();
        NativeError.Error err = NativeError.translateError(rc);
        if (err != NativeError.Error.OK) {
            LOG.warning("Native preDestroy returned an error");
        }
    }

    /**
     * Cleanup native part.<br>
     * This postDestroy should be called <b>after</b> the DDS domain participants are removed. To be used for final Qeo
     * cleanup.
     */
    public static void postDestroy()
    {
        int rc = nativePostDestroy();
        NativeError.Error err = NativeError.translateError(rc);
        if (err != NativeError.Error.OK) {
            LOG.warning("Native postDestroy returned an error");
        }
    }

    /**
     * Returns a string containing the Qeo version.
     *
     * @return The Qeo version string.
     */
    public static native String nativeGetVersionString();

    @Override
    @NativeCallback
    public synchronized void onStartOtpRetrieval()
    {
        LOG.fine("onStartOtpRetrieval for domain " + mId);
        if (mNativeFactory == 0) {
            LOG.warning("got onStartOtpRetrieval without native factory, cancelling request");
            nativeCancelRegistration();
            return;
        }
        mStartOtpRetrievalCalled = true;
        for (QeoConnectionListener listener : mListener) {
            listener.onStartAuthentication();
        }
    }

    /**
     * setInvalidOTC true when an invalid OTC is entered. Must be static.
     *
     */
    public static void setInvalidOTC()
    {
        sInvalidOTCEvent = true;
    }

    @Override
    @NativeCallback
    public synchronized void onStatusUpdate(String status, String reason)
    {
        LOG.fine("onStatusUpdate for domain " + mId + "(" + status + " -- " + reason + ")");
        if (reason.equals("Invalid OTC")) {
            setInvalidOTC();
        }
        for (QeoConnectionListener listener : mListener) {
            listener.onStatusUpdate(status, reason);
        }
    }

    @Override
    @NativeCallback
    public synchronized void onQeoInitDone(boolean result)
    {
        LOG.fine("onQeoInitDone for domain " + mId + "(" + result + ")");
        QeoFactory factory;

        mInitDoneCalled = true;
        mStartOtpRetrievalCalled = false;
        mQeoInitOk = result;
        int i = 0;
        int factorySize = mFactory.size();
        for (QeoConnectionListener listener : mListener) {
            if (i == factorySize) {
                LOG.fine("factory not yet set for onQeoInitDone, skipping");
                break; // no more factories
            }
            factory = mFactory.get(i);
            if (result) {
                listener.onQeoReady(factory);
            }
            else {
                listener.onQeoError(null);
            }
            i++;
        }
    }

    /**
     * Set the loglevel to native.
     *
     * @param level The loglevel
     */
    public static void setLogLevel(Level level)
    {
        int nativeLevel;
        if (level.intValue() >= Level.SEVERE.intValue()) {
            nativeLevel = LOG_NATIVE_ERROR;
        }
        else if (level.intValue() >= Level.INFO.intValue()) {
            // even on java INFO level put the native level on WARNING
            // the native INFO level contains also debugging stuff.
            nativeLevel = LOG_NATIVE_WARNING;
        }
        else if (level.intValue() >= Level.FINE.intValue()) {
            nativeLevel = LOG_NATIVE_INFO;
        }
        else {
            nativeLevel = LOG_NATIVE_DEBUG;
        }
        nativeSetLogLevel(nativeLevel);
    }

    /**
     * Change the default file storage path (for testing purposes only). Should be called before any other native Qeo
     * method.
     *
     * @param path The path to be used.
     */
    public static void setStoragePath(String path)
    {
        LOG.fine("Set custom storage path: " + path);
        nativeSetStoragePath(path);
    }

    private static native void nativeSetStoragePath(String path);

    private static native long nativeOpen(int id);

    private static native int nativeConfigTcpServer(long factory, String tcpServer);

    private static native int nativeConfigDeviceInfo(String[] devInfo, long[] devId);

    private static native int nativeInit(QeoJavaCallbackHandler qeoCbHandler, long factory, String otpCb, String
        updateCb, String initDoneCb);

    private static native int nativeRefreshPolicy(long factory);

    private static native void nativeClose(long factory);

    private static native void nativeInterrupt(long factory);

    private static native int nativeSetRegistrationCredentials(String otp, String url);

    private static native int nativeCancelRegistration();

    private static native int nativeSetDdsParameter(String key, String value);

    private static native int nativeSetQeoParameter(String key, String value);

    private static native int nativePreDestroy();

    private static native int nativePostDestroy();

    private static native int nativeSetUdpMode(boolean value);

    private static native void nativeSetLogLevel(int level);

    private static native void initCertValidator(Class<CertChainValidator> clazz);

    private static native void bgnsSuspend();

    private static native void bgnsResume();
}
