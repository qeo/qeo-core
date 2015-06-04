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

package org.qeo.internal.common;

/**
 * Representation of a primitive type for which data will be consumed and produced throughout Qeo.
 * 
 * This representation is reflection and DDS agnostic
 */
public class PrimitiveType
    extends Type
{
    /**
     * Constructor for a primitive type.
     * 
     * @param name The name
     * @param id The id used to identify this type
     * @param key Whether it is key or not
     * @param type Which type of data does this type represent
     */
    public PrimitiveType(String name, int id, boolean key, MemberType type)
    {
        super(name, id, key, type, name);
    }

    /**
     * Copy constructor for a primitive type.
     * 
     * @param type The type to copy from
     */
    public PrimitiveType(PrimitiveType type)
    {
        super(type);
    }

    @Override
    public String toString()
    {
        return toString(0);
    }

    @Override
    public String toString(int level)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("PrimitiveType: ");
        sb.append(mName);
        sb.append(" - ");
        sb.append(mMemberType.name());
        sb.append(" - ");
        sb.append(mId);
        sb.append(" - ");
        sb.append(mKey ? "key" : "no key");
        return sb.toString();
    }

    /**
     * Extract a data object based on an object T.
     * 
     * @param <T> The class of the Qeo data type.
     * @param t The object to be transformed
     * 
     * @return The constructed data object
     */
    @Override
    public <T> PrimitiveData toData(T t)
    {
        return new PrimitiveData(getId(), t);
    }
}
