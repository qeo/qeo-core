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

package org.qeo;

import org.qeo.exception.QeoException;
import org.qeo.policy.PolicyUpdateListener;

/**
 * Factory interface to create Qeo readers and writers.<br>
 * An instance of this factory can be created by:
 * <ul>
 * <li>Plain java: <code>QeoJava</code> in <code>org.qeo.java</code>
 * <li>Android: <code>QeoAndroid</code> in <code>org.qeo.android</code>
 * </ul>
 * The factory instance should be properly closed if none of the readers/writers created are needed anymore to free
 * allocated resources.
 */
public interface QeoFactory
{
    /**
     * Used for a factory with the default identity. If no realm is found, the registration procedure will be triggered.
     */
    int DEFAULT_ID = 0;

    /**
     * Used for a factory within the open realm for which no authentication is required.
     */
    int OPEN_ID = 1;

    /**
     * Create a new Qeo event reader for the given class.
     * 
     * @param clazz the class for which to create the reader
     * @param listener the listener to use for data reception notifications
     * @param <T> The class of the Qeo data type. This class must not contain any keyed members.
     * 
     * @return the reader or null on failure
     * 
     * @throws org.qeo.exception.OutOfResourcesException if not enough resources are available
     * @throws IllegalArgumentException if the reader can not be created due to invalid input arguments
     * @throws QeoException if the reader can not be created
     */
    <T> EventReader<T> createEventReader(Class<T> clazz, EventReaderListener<T> listener)
        throws QeoException;

    /**
     * Create a new Qeo event reader for the given class.
     * 
     * @param clazz the class for which to create the reader
     * @param listener the listener to use for data reception notifications
     * @param policyListener the listener to use for policy update notifications
     * @param <T> The class of the Qeo data type. This class must not contain any keyed members.
     * 
     * @return the reader or null on failure
     * 
     * @throws org.qeo.exception.OutOfResourcesException if not enough resources are available
     * @throws IllegalArgumentException if the reader can not be created due to invalid input arguments
     * @throws QeoException if the reader can not be created
     */
    <T> EventReader<T> createEventReader(Class<T> clazz, EventReaderListener<T> listener,
        PolicyUpdateListener policyListener)
        throws QeoException;

    /**
     * Create a new Qeo event writer for the given class.
     * 
     * @param clazz the class for which to create the writer
     * @param <T> The class of the Qeo data type. This class must not contain any keyed members.
     * 
     * @return the writer or null on failure
     * 
     * @throws org.qeo.exception.OutOfResourcesException if not enough resources are available
     * @throws IllegalArgumentException if the writer can not be created due to invalid input arguments
     * @throws QeoException if the writer can not be created
     */
    <T> EventWriter<T> createEventWriter(Class<T> clazz)
        throws QeoException;

    /**
     * Create a new Qeo event writer for the given class.
     * 
     * @param clazz the class for which to create the writer
     * @param policyListener the listener to use for policy update notifications
     * @param <T> The class of the Qeo data type. This class must not contain any keyed members.
     * 
     * @return the writer or null on failure
     * 
     * @throws org.qeo.exception.OutOfResourcesException if not enough resources are available
     * @throws IllegalArgumentException if the writer can not be created due to invalid input arguments
     * @throws QeoException if the writer can not be created
     */
    <T> EventWriter<T> createEventWriter(Class<T> clazz, PolicyUpdateListener policyListener)
        throws QeoException;

    /**
     * Create a new Qeo state reader for the given class.
     * 
     * @param clazz the class for which to create the reader
     * @param listener the listener to use for notifications
     * @param <T> The class of the Qeo data type. This class must contain at least one keyed members.
     * 
     * @return the reader or null on failure
     * 
     * @throws org.qeo.exception.OutOfResourcesException if not enough resources are available
     * @throws IllegalArgumentException if the reader can not be created due to invalid input arguments
     * @throws QeoException if the reader can not be created
     */
    <T> StateReader<T> createStateReader(Class<T> clazz, StateReaderListener listener)
        throws QeoException;

    /**
     * Create a new Qeo state reader for the given class.
     * 
     * @param clazz the class for which to create the reader
     * @param listener the listener to use for notifications
     * @param policyListener the listener to use for policy update notifications
     * @param <T> The class of the Qeo data type. This class must contain at least one keyed members.
     * 
     * @return the reader or null on failure
     * 
     * @throws org.qeo.exception.OutOfResourcesException if not enough resources are available
     * @throws IllegalArgumentException if the reader can not be created due to invalid input arguments
     * @throws QeoException if the reader can not be created
     */
    <T> StateReader<T> createStateReader(Class<T> clazz, StateReaderListener listener,
        PolicyUpdateListener policyListener)
        throws QeoException;

    /**
     * Create a new Qeo state reader for the given class.
     * 
     * @param clazz the class for which to create the reader
     * @param listener the listener to use for data reception notifications
     * @param <T> The class of the Qeo data type. This class must contain at least one keyed members.
     * 
     * @return the reader or null on failure
     * 
     * @throws org.qeo.exception.OutOfResourcesException if not enough resources are available
     * @throws IllegalArgumentException if the reader can not be created due to invalid input arguments
     * @throws QeoException if the reader can not be created
     */
    <T> StateChangeReader<T> createStateChangeReader(Class<T> clazz, StateChangeReaderListener<T> listener)
        throws QeoException;

    /**
     * Create a new Qeo state reader for the given class.
     * 
     * @param clazz the class for which to create the reader
     * @param listener the listener to use for data reception notifications
     * @param policyListener the listener to use for policy update notifications
     * @param <T> The class of the Qeo data type. This class must contain at least one keyed members.
     * 
     * @return the reader or null on failure
     * 
     * @throws org.qeo.exception.OutOfResourcesException if not enough resources are available
     * @throws IllegalArgumentException if the reader can not be created due to invalid input arguments
     * @throws QeoException if the reader can not be created
     */
    <T> StateChangeReader<T> createStateChangeReader(Class<T> clazz, StateChangeReaderListener<T> listener,
        PolicyUpdateListener policyListener)
        throws QeoException;

    /**
     * Create a new Qeo state writer for the given class.
     * 
     * @param clazz the class for which to create the writer
     * @param <T> The class of the Qeo data type. This class must contain at least one keyed members.
     * 
     * @return the writer or null on failure
     * 
     * @throws org.qeo.exception.OutOfResourcesException if not enough resources are available
     * @throws IllegalArgumentException if the writer can not be created due to invalid input arguments
     * @throws QeoException if the writer can not be created
     */
    <T> StateWriter<T> createStateWriter(Class<T> clazz)
        throws QeoException;

    /**
     * Create a new Qeo state writer for the given class.
     * 
     * @param clazz the class for which to create the writer
     * @param policyListener the listener to use for policy update notifications
     * @param <T> The class of the Qeo data type. This class must contain at least one keyed members.
     * 
     * @return the writer or null on failure
     * 
     * @throws org.qeo.exception.OutOfResourcesException if not enough resources are available
     * @throws IllegalArgumentException if the writer can not be created due to invalid input arguments
     * @throws QeoException if the writer can not be created
     */
    <T> StateWriter<T> createStateWriter(Class<T> clazz, PolicyUpdateListener policyListener)
        throws QeoException;

    /**
     * Get the userId from the user in this realm.
     * 
     * @return the userId. Returns 0 if this realm does not have a user associated.
     */
    long getUserId();

    /**
     * Get the realmId from this realm.
     * 
     * @return the realmId. Returns 0 if this realm does not have an id.
     */
    long getRealmId();

    /**
     * Get the realm url from this realm for the management server.
     * 
     * @return the realm url. Returns null if there is no management server for this realm.
     */
    String getRealmUrl();
}
