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

package org.qeo;

import java.util.concurrent.Semaphore;

import org.qeo.testframework.QeoConnectionTestListener;
import org.qeo.testframework.QeoTestCase;

public class LimitOpenFactoryTest
    extends QeoTestCase
{

    private QeoFactory mQeoOpen;
    private QeoConnectionTestListener mQeoReadyListener;

    private static class Event1ReaderListener
        implements EventReaderListener<Event1>
    {
        private final Semaphore onEventSem = new Semaphore(0);

        @Override
        public void onData(Event1 t)
        {
            onEventSem.release();
        }

        @Override
        public void onNoMoreData()
        {
        }
    };

    private static class State1ReaderListener
        implements StateReaderListener
    {
        private final Semaphore onStateSem = new Semaphore(0);

        @Override
        public void onUpdate()
        {
            onStateSem.release();
        }
    };

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp(false);
        /* create an Open Domain Factory */
        mQeoReadyListener = createFactory(getFactoryOpenId());
        mQeoOpen = mQeoReadyListener.getFactory();
    }

    @Override
    public void tearDown()
        throws Exception
    {
        closeReaderWriters();
        closeFactory(mQeoReadyListener);
        super.tearDown();
    }

    /**
     * Try to use random topic on open domain for event.
     * 
     * @throws Exception
     */
    public void testState()
        throws Exception
    {
        final State1ReaderListener sl1 = new State1ReaderListener();

        StateReader<State1> sr = mQeoOpen.createStateReader(State1.class, sl1);
        addJunitReaderWriter(sr);
        StateWriter<State1> sw = mQeoOpen.createStateWriter(State1.class);
        addJunitReaderWriter(sw);

    }

    /**
     * Try to use random topic on open domain for event.
     * 
     * @throws Exception
     */
    public void testEvent()
        throws Exception
    {
        final Event1ReaderListener el1 = new Event1ReaderListener();
        EventReader<Event1> er = mQeoOpen.createEventReader(Event1.class, el1);
        addJunitReaderWriter(er);
        EventWriter<Event1> ew = mQeoOpen.createEventWriter(Event1.class);
        addJunitReaderWriter(ew);

    }

    public static class State1
    {
        @Key
        public int id;
        public String name;
    }

    public static class Event1
    {
        public int id;
        public String name;
    }
}
