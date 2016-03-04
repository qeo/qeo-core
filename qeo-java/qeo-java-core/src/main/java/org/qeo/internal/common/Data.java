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

package org.qeo.internal.common;

/**
 * Base class used for the representation of actual Data consumed and produced throughout Qeo.
 * 
 * This representation is reflection and DDS agnostic
 */
public abstract class Data
{
    /**
     * Id of this data instance as used inside DDS.
     */
    private final int mId;

    /**
     * Construct a new leave data instance.
     * 
     * @param id Id of the object
     */
    protected Data(int id)
    {
        super();
        this.mId = id;
    }

    /**
     * Copy constructor.
     * 
     * @param data the object to cpoy from
     */
    protected Data(Data data)
    {
        super();
        this.mId = data.mId;
    }

    /**
     * Get the ID associated with the data.
     * 
     * @return The ID of the data
     */
    public int getId()
    {
        return mId;
    }

    /**
     * Construct and object from a data sample.
     * 
     * @param <T> The class of the Qeo data type.
     * @param clazz The class of the object.
     * @param type The type of the data sample.
     * 
     * @return The constructed object.
     */
    public abstract <T> Object toObject(Class<T> clazz, Type type);
}
