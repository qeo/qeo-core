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

package org.qeo.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Helper class for factory management.
 */
public class FactoryHelper
{
    private static final Logger LOG = Logger.getLogger("FactoryHelper");

    private final Set<ReaderWriterBase> mReaderWriters;

    /**
     * Create an instance.
     */
    public FactoryHelper()
    {
        mReaderWriters = new HashSet<ReaderWriterBase>();
    }

    /**
     * Register a reader/writer to this factory.
     * @param readerWriter The reader/writer.
     */
    public void addReaderWriter(ReaderWriterBase readerWriter)
    {
        LOG.fine("adding ReaderWriter: " + readerWriter);
        mReaderWriters.add(readerWriter);
    }

    /**
     * Remove a reader/writer from this factory.
     * @param readerWriter the reader/writer.
     */
    public void removeReaderWriter(ReaderWriterBase readerWriter)
    {
        LOG.fine("removing ReaderWriter: " + readerWriter);
        mReaderWriters.remove(readerWriter);
    }

    /**
     * Close all the readers/writers.
     */
    public void closeAllReaderWriter()
    {
        LOG.fine("clearing ReaderWriters");
        for (ReaderWriterBase rw : new HashSet<ReaderWriterBase>(mReaderWriters)) {
            rw.close();
        }
        mReaderWriters.clear();
    }
}
