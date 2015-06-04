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

import java.io.Closeable;

/**
 * Base class for readers/writers.
 */
abstract class ReaderWriterBase implements Closeable
{
    private final FactoryHelper mFactoryHelper;

    /**
     * Constructor.
     * @param factoryHelper Helper class for factory management.
     */
    ReaderWriterBase(FactoryHelper factoryHelper)
    {
        mFactoryHelper = factoryHelper;
        mFactoryHelper.addReaderWriter(this);
    }

    @Override
    public void close()
    {
        mFactoryHelper.removeReaderWriter(this);
    }
}
