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

package org.qeo.internal;

import org.qeo.EventReader;
import org.qeo.EventReaderListener;
import org.qeo.EventWriter;
import org.qeo.StateChangeReader;
import org.qeo.StateChangeReaderListener;
import org.qeo.StateReader;
import org.qeo.StateReaderListener;
import org.qeo.StateWriter;
import org.qeo.exception.QeoException;
import org.qeo.policy.PolicyUpdateListener;

/**
 * Qeo Factory that can be used with a custom introspection implementation.
 * 
 * @param <TYPE> The type description of the Qeo data type
 * @param <DATA> The type object of the Qeo data type
 */
public interface CustomQeoFactory<TYPE, DATA>
{

    /**
     * Create an EventReader.
     * 
     * @param type Type description.
     * @param listener Listener callback.
     * @return An EventWriter
     * @throws QeoException If the reader cannot be created.
     */
    EventReader<DATA> createEventReader(TYPE type, EventReaderListener<DATA> listener)
        throws QeoException;

    /**
     * Create an EventReader.
     * 
     * @param type Type description.
     * @param listener Listener callback.
     * @param policyListener the listener to use for policy update notifications
     * @return An EventWriter
     * @throws QeoException If the reader cannot be created.
     */
    EventReader<DATA> createEventReader(TYPE type, EventReaderListener<DATA> listener,
        PolicyUpdateListener policyListener)
        throws QeoException;

    /**
     * Create an EventWriter.
     * 
     * @param type Type description.
     * @return An EventWriter
     * @throws QeoException If the writer cannot be created.
     */
    EventWriter<DATA> createEventWriter(TYPE type)
        throws QeoException;

    /**
     * Create an EventWriter.
     * 
     * @param type Type description.
     * @param policyListener the listener to use for policy update notifications
     * @return An EventWriter
     * @throws QeoException If the writer cannot be created.
     */
    EventWriter<DATA> createEventWriter(TYPE type, PolicyUpdateListener policyListener)
        throws QeoException;

    /**
     * Create a StateReader.
     * 
     * @param type Type description.
     * @param listener Listener callback.
     * @return An StateRaader
     * @throws QeoException If the reader cannot be created.
     */
    StateReader<DATA> createStateReader(TYPE type, StateReaderListener listener)
        throws QeoException;

    /**
     * Create a StateReader.
     * 
     * @param type Type description.
     * @param listener Listener callback.
     * @param policyListener the listener to use for policy update notifications
     * @return An StateRaader
     * @throws QeoException If the reader cannot be created.
     */
    StateReader<DATA> createStateReader(TYPE type, StateReaderListener listener, PolicyUpdateListener policyListener)
        throws QeoException;

    /**
     * Create a StateChangeReader.
     * 
     * @param type Type description.
     * @param listener Listener callback.
     * @return A StateChangeReader
     * @throws QeoException If the reader cannot be created.
     */
    StateChangeReader<DATA> createStateChangeReader(TYPE type, StateChangeReaderListener<DATA> listener)
        throws QeoException;

    /**
     * Create a StateChangeReader.
     * 
     * @param type Type description.
     * @param listener Listener callback.
     * @param policyListener the listener to use for policy update notifications
     * @return A StateChangeReader
     * @throws QeoException If the reader cannot be created.
     */
    StateChangeReader<DATA> createStateChangeReader(TYPE type, StateChangeReaderListener<DATA> listener,
        PolicyUpdateListener policyListener)
        throws QeoException;

    /**
     * Create a StateWriter.
     * 
     * @param type Type description.
     * @return A StateWriter
     * @throws QeoException If the writer cannot be created.
     */
    StateWriter<DATA> createStateWriter(TYPE type)
        throws QeoException;

    /**
     * Create a StateWriter.
     * 
     * @param type Type description.
     * @param policyListener the listener to use for policy update notifications
     * @return A StateWriter
     * @throws QeoException If the writer cannot be created.
     */
    StateWriter<DATA> createStateWriter(TYPE type, PolicyUpdateListener policyListener)
        throws QeoException;
}
