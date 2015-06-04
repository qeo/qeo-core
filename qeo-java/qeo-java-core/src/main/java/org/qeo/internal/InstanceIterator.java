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

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.qeo.internal.common.ReaderFilter;
import org.qeo.internal.common.SampleInfo;

/**
 * State reader iterator implementation.
 * 
 * @param <TYPE> The class of the Qeo data type.
 * @param <DATA> The type of the Qeo data.
 */
public class InstanceIterator<TYPE, DATA>
    implements Iterator<DATA>
{
    private final ReaderBase<TYPE, DATA> mReader;
    private final ReaderFilter mFilter;
    private final SampleInfo mInfo;
    private DATA mSample; /* caches the returned value of a hasNext() */

    /**
     * Create an iterator for given reader.
     * 
     * @param reader The reader for which to create an iterator.
     */
    public InstanceIterator(ReaderBase<TYPE, DATA> reader)
    {
        mReader = reader;
        mFilter = new ReaderFilter();
        mInfo = new SampleInfo();
    }

    @Override
    public boolean hasNext()
    {
        if (null == mSample) {
            /* read instance, cache returned data */
            mSample = mReader.read(mFilter, mInfo);
        }
        return (null == mSample ? false : true);
    }

    @Override
    public DATA next()
    {
        DATA ret = null;

        /* return cached data or read new instance */
        ret = (null == mSample ? mReader.read(mFilter, mInfo) : mSample);
        mFilter.setInstanceHandle(mInfo.getInstanceHandle()); /* Move to the next sample */
        mSample = null; /* reset so we don't return it twice */
        if (null == ret) {
            throw new NoSuchElementException();
        }
        return ret;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
