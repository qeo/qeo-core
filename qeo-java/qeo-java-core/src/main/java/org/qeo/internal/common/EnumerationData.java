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
 * Representation of enumeration Data consumed and produced throughout Qeo.
 * 
 * This representation is reflection and DDS agnostic
 */
public class EnumerationData
    extends Data
{
    /**
     * Enumeration object corresponding with the ordinal value (or null if not yet deduced).
     */
    private Enum<?> mValue = null;

    /**
     * Ordinal value of enumeration object.
     */
    private int mOrdinal = -1;

    /**
     * Construct an enumeration object with given id and ordinal value.
     * 
     * @param id The id as known inside DDS
     * @param ord The ordinal value
     */
    public EnumerationData(int id, int ord)
    {
        super(id);
        mOrdinal = ord;
    }

    /**
     * Construct an enumeration object with given id and enum value.
     * 
     * @param id The id as known inside DDS
     * @param value The enum value object
     */
    public EnumerationData(int id, Enum<?> value)
    {
        super(id);
        if (null != value) {
            mValue = value;
            mOrdinal = value.ordinal();
        }
    }

    /**
     * Get the enumeration object's ordinal value.
     * 
     * @return The ordinal value
     */
    public int getValue()
    {
        return mOrdinal;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("EnumerationData: ");
        sb.append("(").append(getId()).append(") ");
        if (mValue != null) {
            sb.append(mValue.getClass().getName());
        }
        else {
            sb.append("???");
        }
        sb.append(" - ");
        sb.append(mOrdinal);
        return sb.toString();
    }

    @Override
    public <T> Object toObject(Class<T> clazz, Type type)
    {
        T obj = clazz.cast(mValue);

        if (null == obj) {
            T[] c = clazz.getEnumConstants();
            if ((mOrdinal >= 0) && (mOrdinal < c.length)) {
                obj = c[mOrdinal];
            }
        }
        return obj;
    }
}
