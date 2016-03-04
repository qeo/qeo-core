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

package org.qeo.json;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;
import org.qeo.EventReader;
import org.qeo.EventReaderListener;
import org.qeo.EventWriter;
import org.qeo.QeoFactory;
import org.qeo.QeoWriter;
import org.qeo.ReaderWriter;
import org.qeo.StateChangeReader;
import org.qeo.StateChangeReaderListener;
import org.qeo.StateReader;
import org.qeo.StateReaderListener;
import org.qeo.StateWriter;
import org.qeo.exception.QeoException;
import org.qeo.policy.AccessRule;
import org.qeo.policy.Identity;
import org.qeo.policy.PolicyUpdateListener;

/**
 * This class handles the grunt work for creating a Qeo proxy component with JSON readers and writers. It stores
 * readers, writers and iterators internally and represents them externally as integer handles. This approach allows
 * proxy components (a web service, a Javascript object injected into an Android WebView) to pass the handles back and
 * forth easily.
 */
public class QeoJSONProxy
{
    private final Map<Integer, QeoFactoryJSONWrapper> mFactories;
    private final Callbacks mCbs;

    // map for all reader/writers.
    // for type-safety a map per reader/writer type would be practical, but this would require a lot of maps.
    private final Map<Integer, Iterator<JSONObject>> mIterators;

    private final AtomicInteger mIdGenerator;

    /**
     * Construct a JSON proxy.
     * 
     * @param callbacks An implementation of the Callbacks interface that allows notifications to be delivered to the
     *            upper layers of the application.
     */
    public QeoJSONProxy(Callbacks callbacks)
    {

        mCbs = callbacks;
        mIdGenerator = new AtomicInteger();

        // make sure maps are threadsafe as callbacks from javascript are not guaranteed to be singletrheaded.
        // use custom ConcurrentHashMap constructor as per
        // http://ria101.wordpress.com/2011/12/12/concurrenthashmap-avoid-a-common-misuse/
        int initialCapacity = 8;
        float loadfactor = 0.9f;
        int concurrentlyLevel = 1;

        mIterators =
            new ConcurrentHashMap<Integer, Iterator<JSONObject>>(initialCapacity, loadfactor, concurrentlyLevel);
        mFactories =
            new ConcurrentHashMap<Integer, QeoFactoryJSONWrapper>(initialCapacity, loadfactor, concurrentlyLevel);
    }

    /**
     * Create a JSON factory.
     * 
     * @param qeo The regular qeo factory.
     * @return The id of the created json factory
     */
    public int createJsonFactory(QeoFactory qeo)
    {
        int id = getNextId();
        QeoFactoryJSON factory = QeoJSON.getFactory(qeo);
        mFactories.put(id, new QeoFactoryJSONWrapper(factory));
        return id;
    }

    private int getNextId()
    {
        // note: in theory this could overflow if many reader/writers/iterators are made...
        return mIdGenerator.incrementAndGet();
    }

    /**
     * Trigger a policy update.
     * 
     * @param factoryId The id of the factory
     * @param id The id of the reader/writer
     */
    public void updatePolicy(int factoryId, int id)
    {
        QeoFactoryJSONWrapper factory = mFactories.get(factoryId);
        if (factory != null) {
            ReaderWriter<?> rw = factory.getReaderWriter(id);
            rw.updatePolicy();
        }
    }

    /**
     * Creates a new StateWriter.
     * 
     * @param factoryId The id of the factory
     * @param typedesc Topic description in JSON format
     * @param enablePolicy Indicates if fine-grained policy needs to be enabled.
     * @return an integer handle for the StateWriter
     * @throws QeoException thrown when failing to create writer.
     */
    public int createStateWriter(int factoryId, JSONObject typedesc, boolean enablePolicy)
        throws QeoException
    {
        int id = getNextId();
        final QeoFactoryJSONWrapper factory = mFactories.get(factoryId);
        synchronized (factory) {
            StateWriter<JSONObject> writer;
            if (enablePolicy) {
                writer = factory.getFactory().createStateWriter(typedesc, new PolicyUpdateListenerProxy(id));
            }
            else {
                writer = factory.getFactory().createStateWriter(typedesc);
            }
            factory.addReaderWriter(id, writer);
        }
        return id;
    }

    /**
     * Creates a new EventWriter.
     * 
     * @param factoryId The id of the factory
     * @param typedesc Topic description in JSON format
     * @param enablePolicy Indicates if fine-grained policy needs to be enabled.
     * @return an integer handle for the EventWriter
     * @throws QeoException thrown when failing to create writer.
     */
    public int createEventWriter(int factoryId, JSONObject typedesc, boolean enablePolicy)
        throws QeoException
    {
        int id = getNextId();
        final QeoFactoryJSONWrapper factory = mFactories.get(factoryId);
        synchronized (factory) {
            EventWriter<JSONObject> writer;
            if (enablePolicy) {
                writer = factory.getFactory().createEventWriter(typedesc, new PolicyUpdateListenerProxy(id));
            }
            else {
                writer = factory.getFactory().createEventWriter(typedesc);
            }
            factory.addReaderWriter(id, writer);
        }
        return id;
    }

    /**
     * Creates a new StateReader.
     * 
     * @param factoryId The id of the factory
     * @param typedesc Topic description in JSON format
     * @param enablePolicy Indicates if fine-grained policy needs to be enabled.
     * @return an integer handle for the StateReader
     * @throws QeoException thrown when failing to create reader.
     */
    public int createStateReader(int factoryId, JSONObject typedesc, boolean enablePolicy)
        throws QeoException
    {
        int id = getNextId();
        final QeoFactoryJSONWrapper factory = mFactories.get(factoryId);
        synchronized (factory) {
            StateReader<JSONObject> reader;
            if (enablePolicy) {
                reader =
                    factory.getFactory().createStateReader(typedesc, new StateListener(id),
                        new PolicyUpdateListenerProxy(id));
            }
            else {
                reader = factory.getFactory().createStateReader(typedesc, new StateListener(id));
            }
            factory.addReaderWriter(id, reader);
        }

        return id;
    }

    /**
     * Creates a new StateChangeReader.
     * 
     * @param factoryId The id of the factory
     * @param typedesc Topic description in JSON format
     * @param enablePolicy Indicates if fine-grained policy needs to be enabled.
     * @return an integer handle for the StateChangeReader.
     * @throws QeoException thrown when failing to create reader.
     */
    public int createStateChangeReader(int factoryId, JSONObject typedesc, boolean enablePolicy)
        throws QeoException
    {
        int id = getNextId();
        final QeoFactoryJSONWrapper factory = mFactories.get(factoryId);
        synchronized (factory) {
            StateChangeReader<JSONObject> reader;
            if (enablePolicy) {
                reader =
                    factory.getFactory().createStateChangeReader(typedesc, new StateChangeListener(id),
                        new PolicyUpdateListenerProxy(id));
            }
            else {
                reader = factory.getFactory().createStateChangeReader(typedesc, new StateChangeListener(id));
            }
            factory.addReaderWriter(id, reader);
        }
        return id;
    }

    /**
     * Creates a new EventReader.
     * 
     * @param factoryId The id of the factory
     * @param typedesc Topic description in JSON format
     * @param enablePolicy Indicates if fine-grained policy needs to be enabled.
     * @return an integer handle for the EventReader
     * @throws QeoException thrown when failing to create reader.
     */
    public int createEventReader(int factoryId, JSONObject typedesc, boolean enablePolicy)
        throws QeoException
    {
        int id = getNextId();
        final QeoFactoryJSONWrapper factory = mFactories.get(factoryId);
        synchronized (factory) {
            EventReader<JSONObject> reader;
            if (enablePolicy) {
                reader =
                    factory.getFactory().createEventReader(typedesc, new EventListener(id),
                        new PolicyUpdateListenerProxy(id));
            }
            else {
                reader = factory.getFactory().createEventReader(typedesc, new EventListener(id));
            }
            factory.addReaderWriter(id, reader);
        }
        return id;
    }

    /**
     * Close json factory.
     * 
     * @param factoryId factory id
     */
    public void closeFactory(int factoryId)
    {
        QeoFactoryJSONWrapper factory = mFactories.remove(factoryId);
        if (factory != null) {
            // json factory is derived from generic factory and does not need to be closed
            synchronized (factory) {
                // get list of reader/writers on this factory and close them
                for (int id : factory.getReaderWriterIds()) {
                    ReaderWriter<?> readerWriter = factory.removeReaderWriter(id);
                    if (readerWriter == null) {
                        return; // already closed or wrong id
                    }
                    readerWriter.close();
                }
            }
        }
    }

    /**
     * Close a reader/writer.
     * 
     * @param factoryId The id of the factory
     * @param id the Reader/Writer's integer handle
     */
    public void closeReaderWriter(int factoryId, int id)
    {
        final QeoFactoryJSONWrapper factory = mFactories.get(factoryId);
        if (factory != null) {
            synchronized (factory) {
                ReaderWriter<?> readerWriter = factory.removeReaderWriter(id);
                if (readerWriter == null) {
                    return; // already closed or wrong id
                }
                readerWriter.close();
            }
        }

    }

    /**
     * Invoke StateWriter.remove().
     * 
     * @param factoryId The id of the factory
     * @param id The writer's integer handle
     * @param data The instance key, in JSON format
     */
    @SuppressWarnings("unchecked")
    public void removeState(int factoryId, int id, JSONObject data)
    {
        final QeoFactoryJSONWrapper factory = mFactories.get(factoryId);
        if (factory != null) {
            synchronized (factory) {
                StateWriter<JSONObject> writer = (StateWriter<JSONObject>) factory.getReaderWriter(id);
                writer.remove(data);
            }
        }
    }

    /**
     * Invoke Writer.write().
     * 
     * @param factoryId The id of the factory
     * @param id The writer's integer handle
     * @param data The data to write, in JSON format
     */
    @SuppressWarnings("unchecked")
    public void writeData(int factoryId, int id, JSONObject data)
    {
        final QeoFactoryJSONWrapper factory = mFactories.get(factoryId);
        if (factory != null) {
            synchronized (factory) {
                QeoWriter<JSONObject> writer = (QeoWriter<JSONObject>) factory.getReaderWriter(id);
                writer.write(data);
            }
        }
    }

    /**
     * Create an instance iterator for a StateReader.
     * 
     * @param factoryId The id of the factory
     * @param id the StateReader's integer handle
     * @return the integer handle for the created iterator, -1 on failure.
     */
    @SuppressWarnings("unchecked")
    public int getStateIterator(int factoryId, int id)
    {
        final QeoFactoryJSONWrapper factory = mFactories.get(factoryId);
        int iterId = -1;
        if (factory != null) {
            synchronized (factory) {
                StateReader<JSONObject> reader = (StateReader<JSONObject>) factory.getReaderWriter(id);
                if (reader == null) {
                    return -1;
                }
                Iterator<JSONObject> iter = reader.iterator();
                iterId = getNextId();
                mIterators.put(iterId, iter);
            }
        }
        return iterId;
    }

    /**
     * invoke iterator.hasNext().
     * 
     * @param id the iterator's integer handle
     * @return iterator.hasNext()
     */
    public boolean iteratorHasNext(int id)
    {
        Iterator<JSONObject> iter = mIterators.get(id);
        if (iter == null) {
            return false;
        }
        boolean hasnext = iter.hasNext();
        if (!hasnext) {
            mIterators.remove(id); /* auto-cleanup run-down iterators */
        }
        return hasnext;
    }

    /**
     * invoke iterator.next().
     * 
     * @param id the iterator's integer handle
     * @return iterator.next(), with the data encoded as JSON object
     */
    public JSONObject iteratorNext(int id)
    {
        Iterator<JSONObject> iter = mIterators.get(id);
        if (iter == null) {
            return null;
        }
        JSONObject next = iter.next();
        if (next == null) {
            mIterators.remove(id); /* auto-cleanup run-down iterators */
        }
        return next;
    }

    /**
     * Explicitly close an iterator. When an iterator runs to the end of its collection, it cleans up automatically. If
     * you want to discard it earlier, call this method. Otherwise, it will remain in the proxy's iterator hash table
     * and never get cleaned up.
     * 
     * @param id the iterator's integer handle.
     */
    public void iteratorClose(int id)
    {
        mIterators.remove(id);
    }

    /**
     * Get a list of reader/writer ids associated to this factory.
     * 
     * @param factoryId The id of the factory.
     * @return The list of ids. Returns null if the factory does not exist.
     */
    public Set<Integer> getReaderWriterIds(int factoryId)
    {
        QeoFactoryJSONWrapper factory = mFactories.get(factoryId);
        if (factory == null) {
            return null;
        }
        return factory.getReaderWriterIds();
    }

    // ////////////////////////////////////////////
    // helper classes
    // ////////////////////////////////////////////

    /**
     * Notification callback interface for QeoJSONProxy. QeoJSONProxy attaches its own listeners to all readers, and
     * sends back the callbacks with corresponding reader ids.
     */
    public interface Callbacks
    {
        /**
         * Callback for onData on an Event/StateReader.
         * 
         * @param readerId The id of the reader.
         * @param data The data in JSON format.
         */
        void onData(int readerId, JSONObject data);

        /**
         * Callback for onNoMoreData on an Event/StateReader.
         * 
         * @param readerId The id of the reader.
         */
        void onNoMoreData(int readerId);

        /**
         * Callback for onUpdate on a StateReader.
         * 
         * @param readerId The id of the reader.
         */
        void onStateUpdate(int readerId);

        /**
         * Callback for an onRemove instance.
         * 
         * @param readerId The id of the reader.
         * @param data The data in JSON format.
         */
        void onRemove(int readerId, JSONObject data);

        /**
         * Callback for a policyUpdate.
         * 
         * @param id The id of the associated reader/writer.
         * @param identity The identity.
         * @return The new policy
         */
        AccessRule onPolicyUpdate(int id, Identity identity);

    }

    /**
     * EventReaderListener used for all EventReaders.
     */
    private class EventListener
        implements EventReaderListener<JSONObject>
    {
        private final int mReaderId;

        EventListener(int id)
        {
            mReaderId = id;
        }

        @Override
        public void onData(JSONObject data)
        {
            mCbs.onData(mReaderId, data);
        }

        @Override
        public void onNoMoreData()
        {
            mCbs.onNoMoreData(mReaderId);
        }
    }

    /**
     * Generic listener for all StateChangeReaders.
     */
    private class StateChangeListener
        implements StateChangeReaderListener<JSONObject>
    {
        private final int mReaderId;

        StateChangeListener(int id)
        {
            mReaderId = id;
        }

        @Override
        public void onData(JSONObject data)
        {
            mCbs.onData(mReaderId, data);
        }

        @Override
        public void onRemove(JSONObject data)
        {
            mCbs.onRemove(mReaderId, data);
        }

        @Override
        public void onNoMoreData()
        {
            mCbs.onNoMoreData(mReaderId);
        }
    }

    /**
     * Generic listener for all StateReaders.
     */
    private class StateListener
        implements StateReaderListener
    {
        private final int mReaderId;

        StateListener(int id)
        {
            mReaderId = id;
        }

        @Override
        public void onUpdate()
        {
            mCbs.onStateUpdate(mReaderId);
        }
    }

    private class PolicyUpdateListenerProxy
        implements PolicyUpdateListener
    {
        private final int mId;

        PolicyUpdateListenerProxy(int id)
        {
            mId = id;
        }

        @Override
        public AccessRule onPolicyUpdate(Identity identity)
        {
            return mCbs.onPolicyUpdate(mId, identity);
        }
    }

    private static class QeoFactoryJSONWrapper
    {
        private final QeoFactoryJSON mQeoFactoryJSON;
        private final Map<Integer, ReaderWriter<?>> mReaderWriterIds;

        QeoFactoryJSONWrapper(QeoFactoryJSON factory)
        {
            mQeoFactoryJSON = factory;
            mReaderWriterIds = Collections.synchronizedMap(new HashMap<Integer, ReaderWriter<?>>());
        }

        public QeoFactoryJSON getFactory()
        {
            return mQeoFactoryJSON;
        }

        public void addReaderWriter(int id, ReaderWriter<?> rw)
        {
            mReaderWriterIds.put(id, rw);
        }

        public ReaderWriter<?> removeReaderWriter(int id)
        {
            return mReaderWriterIds.remove(id);
        }

        public ReaderWriter<?> getReaderWriter(int id)
        {
            return mReaderWriterIds.get(id);
        }

        public Set<Integer> getReaderWriterIds()
        {
            synchronized (mReaderWriterIds) {
                // return a new set.
                // Otherwise there's a risk of concurrentmodificationException when iterating over the set.
                return new HashSet<Integer>(mReaderWriterIds.keySet());
            }
        }
    }
}
