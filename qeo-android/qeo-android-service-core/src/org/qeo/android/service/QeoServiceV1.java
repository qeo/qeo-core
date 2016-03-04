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

import org.qeo.QeoFactory;
import org.qeo.android.internal.IServiceQeoCallback;
import org.qeo.android.internal.IServiceQeoPolicyUpdateCallback;
import org.qeo.android.internal.IServiceQeoReaderCallback;
import org.qeo.android.internal.IServiceQeoV1;
import org.qeo.android.internal.ParcelableDeviceId;
import org.qeo.android.internal.ParcelableException;
import org.qeo.android.internal.ParcelableFilter;
import org.qeo.android.internal.ParcelableSampleInfo;
import org.qeo.android.internal.ParcelableType;
import org.qeo.internal.BaseFactory;

import android.os.RemoteException;

/**
 * Wrapper for V1 of qeo-service aidl on QeoServiceImpl.
 */
public class QeoServiceV1
    extends IServiceQeoV1.Stub
{

    private final QeoServiceImpl mServiceImpl;

    /**
     * Construct a new instance.
     * 
     * @param serviceImpl implementation of the aidl.
     */
    public QeoServiceV1(QeoServiceImpl serviceImpl)
    {
        mServiceImpl = serviceImpl;
    }

    /**
     * Initializes this service class.
     * 
     * @param qeo Reference to java qeo object
     * @param qeoOpen Reference to java qeo object for the open realm
     */
    public void init(BaseFactory qeo, BaseFactory qeoOpen)
    {
        mServiceImpl.init(qeo, qeoOpen);
    }

    /**
     * Cleanup references only needed for this specific connection.
     */
    void onUnbind()
    {
        mServiceImpl.onUnbind();
    }

    /**
     * Get the Qeo Factory.
     * 
     * @return the qeo factory.
     */
    public QeoFactory getQeoFactory()
    {
        return mServiceImpl.getQeoFactory();
    }

    @Override
    public void register(IServiceQeoCallback cb)
        throws RemoteException
    {
        mServiceImpl.register(cb);
    }

    @Override
    public void unregister(IServiceQeoCallback cb)
        throws RemoteException
    {
        mServiceImpl.unregister(cb);
    }

    @Override
    public long createReader(IServiceQeoCallback cb, int id, ParcelableType type, IServiceQeoReaderCallback listener,
        IServiceQeoPolicyUpdateCallback policyListener, String readerType, ParcelableException exception)
        throws RemoteException
    {
        return mServiceImpl.createReader(cb, id, type, listener, policyListener, readerType, exception);
    }

    @Override
    public void removeReader(long reader)
        throws RemoteException
    {
        mServiceImpl.removeReader(reader);
    }

    @Override
    public void updateReaderPolicy(long reader, ParcelableException exception)
        throws RemoteException
    {
        mServiceImpl.updateReaderPolicy(reader, exception);

    }

    @Override
    public void setReaderBackgroundNotification(long reader, boolean enabled)
        throws RemoteException
    {
        mServiceImpl.setReaderBackgroundNotification(reader, enabled);
    }

    @Override
    public long createWriter(IServiceQeoCallback cb, int id, ParcelableType type,
        IServiceQeoPolicyUpdateCallback policyListener, String writerType, ParcelableException exception)
        throws RemoteException
    {
        return mServiceImpl.createWriter(cb, id, type, policyListener, writerType, exception);
    }

    @Override
    public void removeWriter(long writer)
        throws RemoteException
    {
        mServiceImpl.removeWriter(writer);

    }

    @Override
    public void updateWriterPolicy(long writer, ParcelableException exception)
        throws RemoteException
    {
        mServiceImpl.updateWriterPolicy(writer, exception);
    }

    @Override
    public void write(long writerId, boolean firstBlock, boolean lastBlock, int totalSize, byte[] data,
        ParcelableException exception)
        throws RemoteException
    {
        mServiceImpl.write(writerId, firstBlock, lastBlock, totalSize, data, exception);
    }

    @Override
    public void remove(long writerId, boolean firstBlock, boolean lastBlock, int totalSize, byte[] data,
        ParcelableException exception)
        throws RemoteException
    {
        mServiceImpl.remove(writerId, firstBlock, lastBlock, totalSize, data, exception);
    }

    @Override
    public int take(long readerId, ParcelableFilter filter, ParcelableSampleInfo sampleInfo,
        ParcelableException exception)
        throws RemoteException
    {
        return mServiceImpl.take(readerId, filter, sampleInfo, exception);
    }

    @Override
    public int read(long readerId, ParcelableFilter filter, ParcelableSampleInfo sampleInfo,
        ParcelableException exception)
        throws RemoteException
    {
        return mServiceImpl.read(readerId, filter, sampleInfo, exception);
    }

    @Override
    public ParcelableDeviceId getDeviceId()
        throws RemoteException
    {
        return mServiceImpl.getDeviceId();
    }

    @Override
    public void pushManifest(IServiceQeoCallback cb, String[] manifest, ParcelableException exception)
        throws RemoteException
    {
        mServiceImpl.pushManifest(cb, manifest, exception);
    }

    @Override
    public int getApplicationVersionForManifest()
        throws RemoteException
    {
        return mServiceImpl.getApplicationVersionForManifest();
    }

    @Override
    public boolean disableManifestPopup()
        throws RemoteException
    {
        return mServiceImpl.disableManifestPopup();
    }

    @Override
    public void refreshPolicy()
        throws RemoteException
    {
        mServiceImpl.refreshPolicy();
    }

    @Override
    public long factoryGetUserId(int factoryId)
        throws RemoteException
    {
        return mServiceImpl.factoryGetUserId(factoryId);
    }

    @Override
    public long factoryGetRealmId(int factoryId)
        throws RemoteException
    {
        return mServiceImpl.factoryGetRealmId(factoryId);
    }

    @Override
    public String factoryGetRealmUrl(int factoryId)
        throws RemoteException
    {
        return mServiceImpl.factoryGetRealmUrl(factoryId);
    }

    @Override
    public void setSecurityConfig(String realm, String userName, String deviceName)
        throws RemoteException
    {
        mServiceImpl.setSecurityConfig(realm, userName, deviceName);
    }

    @Override
    public void setOAuthCode(String code)
        throws RemoteException
    {
        mServiceImpl.setOAuthCode(code);
    }

    @Override
    public void continueAuthentication(int type, String data) throws RemoteException
    {
        mServiceImpl.continueAuthentication(type, data);
    }

    @Override
    public void suspend()
        throws RemoteException
    {
        mServiceImpl.suspend();
    }

    @Override
    public void resume()
        throws RemoteException
    {
        mServiceImpl.resume();
    }
}
