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

package com.technicolor.qeo.codegen.type;

import java.util.Iterator;

/**
 * Generic class that can be used to wrap the Container.getMembers() iterable in order to wrap each element inside a
 * ContainerMemberAdapter.
 * 
 * @param <T1> The class of the elements in the Iterable.
 * @param <T2>
 */
public class ContainerIterableAdapter<T1 extends ContainerMember, T2 extends ContainerMember>
    implements Iterable<T2>
{
    private final Iterable<T1> mIterable;
    private final ContainerMemberAdapter<T1, T2> mAdapter;

    /**
     * Construct a wrapped container iterable.
     * 
     * @param iterable The iterable to be adapted.
     * @param adapter The adapter to be used for the elements of the iterable.
     */
    public ContainerIterableAdapter(Iterable<T1> iterable, ContainerMemberAdapter<T1, T2> adapter)
    {
        mIterable = iterable;
        mAdapter = adapter;
    }

    @Override
    public Iterator<T2> iterator()
    {
        return new Iterator<T2>() {
            private final Iterator<T1> mIterator = mIterable.iterator();

            @Override
            public boolean hasNext()
            {
                return mIterator.hasNext();
            }

            @Override
            public T2 next()
            {
                return mAdapter.wrap(mIterator.next());
            }

            @Override
            public void remove()
            {
            }
        };
    }
}
