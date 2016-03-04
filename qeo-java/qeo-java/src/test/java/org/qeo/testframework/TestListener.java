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

package org.qeo.testframework;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.qeo.StateChangeReaderListener;

/**
 * Generic listener with semaphores enabled that can be used in unit tests<br/>
 * Can be used for both event and state listener
 * 
 * @param <T>
 */
public class TestListener<T>
    implements StateChangeReaderListener<T>
{

    protected final String name;
    protected T lastReceivedItem = null;
    protected T lastRemovedItem = null;
    public final Semaphore onDataSem = new Semaphore(0);
    public final Semaphore onRemoveSem = new Semaphore(0);
    public final Semaphore onNoMoreDataSem = new Semaphore(0);
    // list that keeps all samples that arrive in ondata. They don't get removed with onRemove
    private final List<T> receivedStates;
    private int numReceived;
    private boolean keepsamples;

    public TestListener(String listenerName)
    {
        name = listenerName;
        receivedStates = new LinkedList<T>();
        numReceived = 0;
        keepsamples = false;
    }

    public TestListener()
    {
        this(null);
    }

    public void setKeepSamples(boolean keepsamplesValue)
    {
        keepsamples = keepsamplesValue;
    }

    @Override
    public void onData(T currentState)
    {
        QeoTestCase.println("L: " + (name == null ? "" : name + " ") + "onData");
        numReceived++;
        lastReceivedItem = currentState;
        if (keepsamples) {
            receivedStates.add(currentState);
        }
        onDataSem.release();
    }

    @Override
    public void onNoMoreData()
    {
        QeoTestCase.println("L: " + (name == null ? "" : name + " ") + "onNoMoreData");
        onNoMoreDataSem.release();
    }

    @Override
    public void onRemove(T state)
    {
        QeoTestCase.println("L: " + (name == null ? "" : name + " ") + "onRemove");
        lastRemovedItem = state;
        onRemoveSem.release();
    }

    public void reset()
    {
        numReceived = 0;
        lastReceivedItem = null;
        lastRemovedItem = null;
        receivedStates.clear();
    }

    public boolean isEmpty()
    {
        return numReceived == 0;
    }

    public int getNumReceived()
    {
        return numReceived;
    }

    public T getLastReceived()
    {
        return lastReceivedItem;
    }

    public T getLastRemoved()
    {
        return lastRemovedItem;
    }

    public List<T> getReceivedStates()
    {
        return receivedStates;
    }

    public String getName()
    {
        return name;
    }

}
