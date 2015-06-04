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

package org.qeo.android.internal;

import org.qeo.android.internal.IServiceQeoCallback;
import org.qeo.android.internal.IServiceQeoReaderCallback;
import org.qeo.android.internal.IServiceQeoPolicyUpdateCallback;
import org.qeo.android.internal.ParcelableSampleInfo;
import org.qeo.android.internal.ParcelableFilter;
import org.qeo.android.internal.ParcelableData;
import org.qeo.android.internal.ParcelableType;
import org.qeo.android.internal.ParcelableException;
import org.qeo.android.internal.ParcelableDeviceId;

/**
 * This is the interface to use the Qeo service.
 */
interface IServiceQeoV1
{

    /**
     * Register a new application to the Qeo service. The cb parameter serves as the identification of the application.
     *
     * @param cb contains the callbacks given by the application to call when the service is initialized, 
     *           closed or when an error occurred.
     */
    void register(IServiceQeoCallback cb);

    /**
     * Unregister a application to the Qeo service. The cb parameter serves as the identification of the application.
     *
     * @param cb contains the callbacks given by the application to call when the service is initialized, 
     *           closed or when an error occurred.
     */
    void unregister(IServiceQeoCallback cb);

    /**
     * Create a new reader on the Qeo service for a given type.
     *
     * @param cb The actual identification of the application that wants to create the reader
     * @param id The identity (default/open realm) for which the application that wants to create the reader
     * @param type The type for which the new reader is created
     * @param listener A set of callbacks called when e.g. data arrives on the reader
     * @param policyListener A set of callbacks called when policy on the reader gets updated
     * @param readerType A string that identifies the sort of reader to be created: State or Event
     * @param exception Possible exception that occurred in the Qeo service that is given back to the application
     *
     * @return an identification of the reader that was created or 0 in case of a failure.
     */
    long createReader(IServiceQeoCallback cb, in int id, in ParcelableType type, IServiceQeoReaderCallback listener,
        in IServiceQeoPolicyUpdateCallback policyListener, String readerType, out ParcelableException exception);

    /**
     * Remove a previously created reader.
     *
     * @param reader the identification of the reader to be removed
     */
    void removeReader(long reader);

    /**
     * Trigger a policy update for a reader.
     *
     * @param reader the identification of the reader
     * @param exception Possible exception that occurred in the Qeo service that is given back to the application
     */
    void updateReaderPolicy(long reader, out ParcelableException exception);

    /**
     * Create a new writer on the Qeo service for a given type.
     *
     * @param cb The actual identification of the application that wants to create the writer
     * @param id The identity (default/open realm) for which the application that wants to create the reader
     * @param type The type for which the new writer is created
     * @param policyListener A set of callbacks called when policy on the writer gets updated
     * @param writerType A string that identifies the sort of writer to be created: State or Event
     * @param exception Possible exception that occurred in the Qeo service that is given back to the application
     *
     * @return an identification of the writer that was created or 0 in case of a failure.
     */
    long createWriter(IServiceQeoCallback cb, in int id, in ParcelableType type,
        in IServiceQeoPolicyUpdateCallback policyListener, String writerType, out ParcelableException exception);

    /**
     * Remove a previously created writer.
     *
     * @param writer the identification of the writer to be removed
     */
    void removeWriter(long writer);

    /**
     * Trigger a policy update for a writer.
     *
     * @param writer the identification of the writer
     * @param exception Possible exception that occurred in the Qeo service that is given back to the application
     */
    void updateWriterPolicy(long writer, out ParcelableException exception);

    /**
     * Write some data on a given writer.
     *
     * @param writerId The writer id to write the new data to
     * @param firstBlock Indicated this is the first block of potentially multiple blocks.
     * @param lastBlock Indicated this is the last block of potentially multiple blocks.
     * @param totalSize The total size of all blocks in bytes.
     * @param data The data of the current block.
     * @param exception Possible exception that occurred in the Qeo service that is given back to the application
     */
    void write(long writerId, boolean firstBlock, boolean lastBlock, int totalSize, in byte[] data,
        out ParcelableException exception);

    /**
     * Remove some data on a given writer.
     *
     * @param writerId The writer id to remove new data from
     * @param firstBlock Indicated this is the first block of potentially multiple blocks.
     * @param lastBlock Indicated this is the last block of potentially multiple blocks.
     * @param totalSize The total size of all blocks in bytes.
     * @param data The data of the current block.
     * @param exception Possible exception that occurred in the Qeo service that is given back to the application
     */
    void remove(long writerId, boolean firstBlock, boolean lastBlock, int totalSize, in byte[] data,
        out ParcelableException exception);

    /**
     * Use this method to take data from a given reader. You can use the filter to specify more fine-grained 
     * what is requested. Taking data means that the data will disappear from the reader once it is taken.
     * In other words, that specific data can only be taken once.<br/>
     * The data won't be returned in this call but in asynchronous onData() callbacks.
     *
     * @param reader The reader to take the data from
     * @param filter The filter to specify more in detail what you want to take
     * @param sampleInfo Some metadata coming along with the data (this is an out parameter)
     * @param exception Possible exception that occurred in the Qeo service that is given back to the application
     *
     * @return 1 if there is data, 0 if there is no data.
     */
    int take(long reader, in ParcelableFilter filter, out ParcelableSampleInfo sampleInfo, out ParcelableException exception);

    /**
     * Use this method to read data from a given reader. You can use the filter to specify more fine-grained 
     * what is requested. Reading data means that the data is returned + it is kept in the readers cache too.
     * In other words the same data can be retrieved later again.<br/>
     * The data won't be returned in this call but in asynchronous onData() callbacks.
     *
     * @param reader The reader to read the data from
     * @param filter The filter to specify more in detail what you want to read
     * @param sampleInfo Some metadata coming along with the data (this is an out parameter)
     * @param exception Possible exception that occurred in the Qeo service that is given back to the application
     *
     * @return 1 if there is data, 0 if there is no data.
     */
    int read(long reader, in ParcelableFilter filter, out ParcelableSampleInfo sampleInfo, out ParcelableException exception);

    /**
     * This call will return the device id of the device running the Qeo service.
     *
     * @return the deviceId
     */
    ParcelableDeviceId getDeviceId();

    /**
     * Push the manifest to the service. A popup dialog will be shown to the user displaying all 
     * topics the application will be using.
     *
     * @param cb The callback to be called when the user has accepted/declined the manifest rules
     * @param manifest The complete manifest file in a string array, a string per line
     * @param exception An exception that was thrown while executing this action in the service
     */
    void pushManifest(IServiceQeoCallback cb, in String[] manifest, out ParcelableException exception);

    /**
     * Get the version number of the application for which a manifest is known by the service.
     * This can be used to determine whether the manifest file should be pushed again.
     *
     * @return The application version if known, -1 if the application is not yet known.
     */
    int getApplicationVersionForManifest();

    /**
     * Calling this method will ensure that there is no manifest popup shown, so no user interaction is 
     * needed. All permissions are then being accepted by default.
     * This will only work on debug builds of the Qeo service.
     *
     * @return True if the popup was really disabled, false otherwise
     */
    boolean disableManifestPopup();

    /**
     * Calling this method will trigger refreshing the policy file again.
     */
    void refreshPolicy();

    /**
     * Get the userId from the user in this realm.
     * 
     * @param factoryId the domainId of the factory.
     * @return the userId. Returns 0 if this realm does not have a user associated.
     */
    long factoryGetUserId(int factoryId);

    /**
     * Get the realmId from this realm.
     *
     * @param factoryId the domainId of the factory. 
     * @return the realmId. Returns 0 if this realm does not have an id.
     */
    long factoryGetRealmId(int factoryId);

    /**
     * Get the realm url from this realm for the management server.
     * 
     * @param factoryId the domainId of the factory.
     * @return the realm url. Returns null if there is no management server for this realm.
     */
    String factoryGetRealmUrl(int factoryId);

    /**
     * Configure Realm name, User name and device name to be used to register Qeo.
     *
     * @param realm The realm name
     * @param userName The user name
     * @param deviceName The device name
     */
    void setSecurityConfig(in String realm, in String userName, in String deviceName);

    /**
     * Set the Oauth code into the service.
     *
     * @param code The Oauth code
     */
    void setOAuthCode(in String code);

    /**
     * Continue authentication.
     *
     * @param type The type of data
     * @param data The data.
     */
    void continueAuthentication(int type, in String data);

    /**
     * Enables or disables background notifications for a reader.
     *
     * @param reader the identification of the reader
     * @param enabled True to enable or false to disable notifications.
     */
    void setReaderBackgroundNotification(long reader, boolean enabled);

    /**
     * Suspend Qeo operations. All non-vital network connections are closed and timers are stopped.
     * 
     * @see org.qeo.Notifiable
     * @see org.qeo.android.QeoConnectionListener.onWakeUp
     * @see resume
     */
    void suspend();

    /**
     * Resume Qeo operations. Re-establishes all connections as if nothing happened.
     * 
     * @see suspend
     */
    void resume();
}
