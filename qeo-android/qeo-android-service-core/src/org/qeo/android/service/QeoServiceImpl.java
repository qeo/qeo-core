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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.qeo.QeoFactory;
import org.qeo.android.internal.AidlConstants;
import org.qeo.android.internal.IServiceQeoCallback;
import org.qeo.android.internal.IServiceQeoPolicyUpdateCallback;
import org.qeo.android.internal.IServiceQeoReaderCallback;
import org.qeo.android.internal.IServiceQeoV1;
import org.qeo.android.internal.ParcelableDeviceId;
import org.qeo.android.internal.ParcelableException;
import org.qeo.android.internal.ParcelableFilter;
import org.qeo.android.internal.ParcelableSampleInfo;
import org.qeo.android.internal.ParcelableType;
import org.qeo.deviceregistration.QeoManagementApp;
import org.qeo.internal.BaseFactory;
import org.qeo.internal.common.EntityType;

import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * Implementation of the IServiceQeo Stub AIDL that handles most of the functions.
 */
public class QeoServiceImpl
    // implementing the IServiceQeoV1 interface is not strictly needed but avoids duplication of javadoc
    implements IServiceQeoV1
{
    private static final Logger LOG = Logger.getLogger("QeoServiceImpl");
    private BaseFactory mQeo;
    private BaseFactory mQeoOpen;
    private Boolean mInit;
    private Boolean mDisableManifestPopup;
    private final Map<String, ApplicationSecurity> mAppSec;
    private final QeoService mService;

    /**
     * Create QeoServiceImpl instance.
     * 
     * @param service the service
     */
    QeoServiceImpl(QeoService service)
    {
        mService = service;
        mInit = false;
        mDisableManifestPopup = false;
        mAppSec = new HashMap<String, ApplicationSecurity>();
    }

    /**
     * Initializes this service class.
     * 
     * @param qeo Reference to java qeo object
     * @param qeoOpen Reference to java qeo object for the open realm
     */
    void init(BaseFactory qeo, BaseFactory qeoOpen)
    {
        mQeo = qeo;
        mQeoOpen = qeoOpen;
        mInit = true;
    }

    @Override
    public void register(IServiceQeoCallback cb)
        throws RemoteException
    {
        LOG.fine("Registering client to this service: " + cb.asBinder());
        mService.register(cb);
    }

    @Override
    public void unregister(IServiceQeoCallback cb)
        throws RemoteException
    {
        LOG.fine("Unregistering client from this service: " + cb.asBinder());
        mService.unregister(cb);
    }

    @Override
    public ParcelableDeviceId getDeviceId()
    {
        DeviceInfoAndroid deviceInfoAndroid = DeviceInfoAndroid.getInstance();
        ParcelableDeviceId result = null;
        LOG.fine("Get your own deviceId");
        result =
            new ParcelableDeviceId(deviceInfoAndroid.getDeviceInfo().deviceId.upper,
                deviceInfoAndroid.getDeviceInfo().deviceId.lower);
        return result;
    }

    @Override
    public void refreshPolicy()
    {
        mService.refreshPolicy();
    }

    @Override
    public void setSecurityConfig(String realm, String userName, String deviceName)
        throws RemoteException
    {
        QeoManagementApp.setRealmName(realm);
        QeoManagementApp.setUserName(userName);
        QeoManagementApp.setDeviceName(deviceName);
    }

    @Override
    public void setOAuthCode(String code)
    {
        mService.continueAuthentication(AidlConstants.AUTHENTICATION_DATA_CODE, code);
    }

    @Override
    public void continueAuthentication(int type, String data) throws RemoteException
    {
        mService.continueAuthentication(type, data);
    }

    @Override
    public void suspend()
        throws RemoteException
    {
        mService.suspend();
    }

    @Override
    public void resume()
        throws RemoteException
    {
        mService.resume();
    }

    /**
     * Get the Qeo Factory.
     * 
     * @return the qeo factory.
     */
    public QeoFactory getQeoFactory()
    {
        return mQeo;
    }

    private ApplicationSecurity getAppSec()
    {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        String key = "U" + uid + "P" + pid;
        ApplicationSecurity appSec = mAppSec.get(key);
        if (appSec == null) {
            LOG.fine("Create new ApplicationSecurity for " + key);
            if (ServiceApplication.isEmbeddedService()) {
                appSec = new ApplicationSecurityEmbedded();
            }
            else {
                appSec = new ApplicationSecurityStandalone(mService, uid, pid);
            }
            mAppSec.put(key, appSec);
        }
        else {
            LOG.fine("Recycle ApplicationSecurity for " + key);
        }
        return appSec;
    }

    /**
     * Check that the manifest popup has been disabled (e.g. for unit testing). We explicitly don't work with a return
     * value but with a return parameter due to the fact that we need to able to remove calls to this function in
     * release mode (using ProGuard).
     * 
     * @param disabled Will be configured with the local value of the boolean
     */
    public void checkPopupDisabled(Boolean[] disabled)
    {
        LOG.fine("Check popup disabled: " + (mDisableManifestPopup ? "yes" : "no"));
        disabled[0] = mDisableManifestPopup;
    }

    private BaseFactory getFactory(int factoryId)
    {
        return (factoryId == QeoFactory.OPEN_ID) ? mQeoOpen : mQeo;
    }

    @Override
    public long createWriter(IServiceQeoCallback isqc, int id, ParcelableType type,
        IServiceQeoPolicyUpdateCallback policyListener, String writerType, ParcelableException exception)
    {
        if (!mInit) {
            LOG.severe("Qeo not yet initialized while trying to create writer");
            throw new IllegalStateException("Qeo not yet initialized");
        }
        long dataWriter = 0;

        try {
            ApplicationSecurity appSec = getAppSec();
            if (!appSec.isAllowedWriter(type.getType().getName())) {
                throw new SecurityException("Not allowed to create writer " + type.getType().getName()
                    + ", it must be listed in the manifest file");
            }
            BaseFactory qeo = getFactory(id);
            dataWriter =
                ConnectedWriter.addWriter(qeo, type, policyListener, EntityType.valueOf(writerType), isqc.asBinder());
            LOG.fine("Created DDS writer <" + dataWriter + ">");
            appSec.registerReaderWriter(dataWriter);
        }
        catch (final RuntimeException e) {
            exception.setException(e);
        }
        return dataWriter;
    }

    @Override
    public void removeWriter(long writer)
    {
        if (!mInit) {
            LOG.severe("Qeo not yet initialized while trying to remove writer");
            throw new IllegalStateException("Qeo not yet initialized");
        }
        ApplicationSecurity appSec = getAppSec();
        appSec.checkRegisteredReaderWriter(writer); // only allowed to remove your own writers
        ConnectedWriter.getWriter(writer).close();
        appSec.unRegisterReaderWriter(writer);
        LOG.fine("Removed DDS writer <" + writer + ">");
    }

    @Override
    public void updateWriterPolicy(long writer, ParcelableException exception)
        throws RemoteException
    {
        try {
            getAppSec().checkRegisteredReaderWriter(writer);
            ConnectedWriter.getWriter(writer).updatePolicy();
        }
        catch (final RuntimeException e) {
            exception.setException(e);
        }
    }

    @Override
    public long createReader(IServiceQeoCallback isqc, int id, ParcelableType type, IServiceQeoReaderCallback listener,
        IServiceQeoPolicyUpdateCallback policyListener, String readerType, ParcelableException exception)
    {
        if (!mInit) {
            LOG.severe("Qeo not yet initialized while trying to create reader");
            throw new IllegalStateException("Qeo not yet initialized");
        }
        long dataReader = 0;
        LOG.finest("Creating reader for " + type.getType().getName() + " (domain " + id + ")");

        try {
            ApplicationSecurity appSec = getAppSec();
            if (!appSec.isAllowedReader(type.getType().getName())) {
                throw new SecurityException("Not allowed to create reader " + type.getType().getName()
                    + ", it must be listed in the manifest file");
            }
            BaseFactory qeo = getFactory(id);
            if (qeo == null) {
                throw new IllegalStateException("Factory with id " + id + " is not initialized");
            }
            dataReader =
                ConnectedReader.addReader(qeo, type, listener, policyListener, EntityType.valueOf(readerType),
                    isqc.asBinder());
            LOG.fine("Created DDS reader <" + dataReader + ">");
            appSec.registerReaderWriter(dataReader);
        }
        catch (final RuntimeException e) {
            exception.setException(e);
        }
        return dataReader;
    }

    @Override
    public void removeReader(long reader)
    {
        if (!mInit) {
            LOG.severe("Qeo not yet initialized while trying to remove reader");
            throw new IllegalStateException("Qeo not yet initialized");
        }
        ApplicationSecurity appSec = getAppSec();
        appSec.checkRegisteredReaderWriter(reader); // only allowed to remove your own readers
        ConnectedReader.getReader(reader).close();
        appSec.unRegisterReaderWriter(reader);
        LOG.fine("Removed DDS reader <" + reader + ">");
    }

    @Override
    public void updateReaderPolicy(long reader, ParcelableException exception)
        throws RemoteException
    {
        try {
            getAppSec().checkRegisteredReaderWriter(reader);
            ConnectedReader.getReader(reader).updatePolicy();
        }
        catch (final RuntimeException e) {
            exception.setException(e);
        }
    }

    @Override
    public void setReaderBackgroundNotification(long reader, boolean enabled)
        throws RemoteException
    {
        getAppSec().checkRegisteredReaderWriter(reader);
        ConnectedReader.getReader(reader).setBackgroundNotification(enabled);
    }

    @Override
    public void write(long writerId, boolean firstBlock, boolean lastBlock, int totalSize, byte[] data,
        ParcelableException exception)
    {
        LOG.finest("Write call for writer " + writerId);
        try {
            getAppSec().checkRegisteredReaderWriter(writerId);
            ConnectedWriter cw = ConnectedWriter.getWriter(writerId);
            cw.write(firstBlock, lastBlock, totalSize, data);
        }
        catch (final RuntimeException e) {
            exception.setException(e);
        }
    }

    @Override
    public void remove(long writerId, boolean firstBlock, boolean lastBlock, int totalSize, byte[] data,
        ParcelableException exception)
    {
        LOG.fine("Removing data in DDS <" + writerId + ">");
        try {
            getAppSec().checkRegisteredReaderWriter(writerId);
            ConnectedWriter cw = ConnectedWriter.getWriter(writerId);
            cw.remove(firstBlock, lastBlock, totalSize, data);
        }
        catch (final RuntimeException e) {
            exception.setException(e);
        }
    }

    @Override
    public int take(long readerId, ParcelableFilter filter, ParcelableSampleInfo sampleInfo,
        ParcelableException exception)
    {
        int result = 0;

        LOG.fine("Take data from DDS <" + readerId + ">");
        try {
            getAppSec().checkRegisteredReaderWriter(readerId);
            result = ConnectedReader.getReader(readerId).take(filter, sampleInfo);
        }
        catch (final RuntimeException e) {
            exception.setException(e);
        }

        return result;
    }

    @Override
    public int read(long readerId, ParcelableFilter filter, ParcelableSampleInfo sampleInfo,
        ParcelableException exception)
    {
        int result = 0;

        LOG.fine("Read data from DDS <" + readerId + ">");
        try {
            getAppSec().checkRegisteredReaderWriter(readerId);
            result = ConnectedReader.getReader(readerId).read(filter, sampleInfo);
        }
        catch (final RuntimeException e) {
            result = 0; // in the case of an exception always put the result to 0
            exception.setException(e);
        }

        return result;
    }

    @Override
    public void pushManifest(IServiceQeoCallback cb, String[] manifest, ParcelableException exception)
        throws RemoteException
    {
        LOG.fine("Push manifest");
        try {
            ApplicationSecurity appSec = getAppSec();

            appSec.parseManifest(manifest);
            appSec.evaluateManifest(this, cb);
        }
        catch (RuntimeException e) {
            exception.setException(e);
        }
    }

    @Override
    public int getApplicationVersionForManifest()
        throws RemoteException
    {
        LOG.fine("Check application version");
        return getAppSec().getAppVersion();
    }

    @Override
    public boolean disableManifestPopup()
        throws RemoteException
    {
        Boolean[] popupDisabled = {false};

        LOG.fine("Manifest popup disabled");
        mDisableManifestPopup = true;
        checkPopupDisabled(popupDisabled);

        return popupDisabled[0];
    }

    /**
     * Cleanup references only needed for this specific connection.
     */
    void onUnbind()
    {
        // no more need to keep application security
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        String key = "U" + uid + "P" + pid;
        LOG.fine("Remove ApplicationSecurity for " + key);
        mAppSec.remove(key);
    }

    @Override
    public long factoryGetUserId(int factoryId)
        throws RemoteException
    {
        QeoFactory qeo = getFactory(factoryId);
        return qeo.getUserId();
    }

    @Override
    public long factoryGetRealmId(int factoryId)
        throws RemoteException
    {
        QeoFactory qeo = getFactory(factoryId);
        return qeo.getRealmId();
    }

    @Override
    public String factoryGetRealmUrl(int factoryId)
        throws RemoteException
    {
        QeoFactory qeo = getFactory(factoryId);
        return qeo.getRealmUrl();
    }

    /**
     * Not used here.
     * 
     * @return always null.
     */
    @Override
    public IBinder asBinder()
    {
        return null;
    }
}
