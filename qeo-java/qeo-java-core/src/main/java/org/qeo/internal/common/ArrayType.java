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
 * Representation of an array type for which data will be consumed and produced throughout Qeo.
 * 
 * This representation is reflection and DDS agnostic
 * 
 * The ArrayType class contains a references to its element Type class.
 */
public class ArrayType
    extends Type
{
    /**
     * The element Type of this ArrayType.
     */
    protected final Type mElement;

    /**
     * Constructor for an array type.
     * 
     * @param name The name
     * @param id The id used to identify this type
     * @param key Whether it is key or not
     * @param element The type of the arrays elements
     */
    public ArrayType(String name, int id, boolean key, Type element)
    {
        super(name, id, key, MemberType.TYPE_ARRAY, name);
        mElement = element;
    }

    /**
     * Copy constructor for an array type.
     * 
     * @param type The type to copy from
     */
    public ArrayType(ArrayType type)
    {
        this(type.mName, type.mId, type.mKey, type.mElement);
    }

    /**
     * Get the type of the elements in the array.
     * 
     * @return the mElement
     */
    public Type getElementType()
    {
        return mElement;
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
        sb.append("ArrayType: ");
        sb.append(mName);
        sb.append(" - ");
        sb.append(mMemberType.name());
        sb.append(" - ");
        sb.append(mId);
        sb.append(" - ");
        sb.append(mKey ? "key" : "no key");
        if (mElement != null) {
            sb.append("\n");
            sb.append("  ");
            for (int i = -1; i < level; i++) {
                sb.append("  ");
            }
            sb.append(mElement.toString(level + 2));
        }
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
    public <T> ArrayData toData(T t)
    {
        ArrayData arrayData = new ArrayData(getId());
        if (t != null) {
            Object[] array = (Object[]) t;
            for (int i = 0; i < array.length; i++) {
                Data arrayMember = getElementType().toData(array[i]);
                arrayData.addElement(arrayMember);
            }
        }
        return arrayData;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((mElement == null) ? 0 : mElement.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ArrayType other = (ArrayType) obj;
        if (mElement == null) {
            if (other.mElement != null) {
                return false;
            }
        }
        else if (!mElement.equals(other.mElement)) {
            return false;
        }
        return true;
    }
}
