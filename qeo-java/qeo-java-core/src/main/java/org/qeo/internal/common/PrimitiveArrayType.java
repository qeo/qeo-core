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
 * Representation of a primitive array type for which data will be consumed and produced throughout Qeo.
 * 
 * This representation is reflection and DDS agnostic
 */
public class PrimitiveArrayType
    extends Type
{
    private final MemberType mElementType;

    /**
     * Constructor for a primitive array type.
     * 
     * @param name The name
     * @param id The id used to identify this type
     * @param key Whether it is key or not
     * @param elementType The type of the arrays elements
     */
    public PrimitiveArrayType(String name, int id, boolean key, MemberType elementType)
    {
        super(name, id, key, getArrayType(elementType), name);
        mElementType = elementType;
    }

    private static MemberType getArrayType(MemberType elementType)
    {
        MemberType memberType;
        switch (elementType) {
            case TYPE_BOOLEAN:
                memberType = MemberType.TYPE_BOOLEANARRAY;
                break;
            case TYPE_BYTE:
                memberType = MemberType.TYPE_BYTEARRAY;
                break;
            case TYPE_SHORT:
                memberType = MemberType.TYPE_SHORTARRAY;
                break;
            case TYPE_INT:
                memberType = MemberType.TYPE_INTARRAY;
                break;
            case TYPE_LONG:
                memberType = MemberType.TYPE_LONGARRAY;
                break;
            case TYPE_FLOAT:
                memberType = MemberType.TYPE_FLOATARRAY;
                break;
            case TYPE_STRING:
                memberType = MemberType.TYPE_STRINGARRAY;
                break;
            default:
                throw new IllegalArgumentException("Invalid elementType: " + elementType);
        }
        return memberType;

    }

    /**
     * Copy constructor for a primitive array type.
     * 
     * @param type The type to copy from
     */
    public PrimitiveArrayType(PrimitiveArrayType type)
    {
        this(type.mName, type.mId, type.mKey, type.mElementType);
    }

    /**
     * Get the type of the element used in the array.
     * 
     * @return The element type. This will be a primitive type.
     */
    public MemberType getElementType()
    {
        return mElementType;
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
        sb.append("PrimitiveArrayType: ");
        if (mName != null) {
            sb.append(mName);
            sb.append(" - ");
        }
        sb.append(mMemberType.name());
        sb.append(" - ");
        sb.append(mId);
        sb.append(" - ");
        sb.append(mKey ? "key" : "no key");
        sb.append(" - (");
        sb.append(mElementType);
        sb.append(")");

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
    public <T> PrimitiveArrayData toData(T t)
    {
        return new PrimitiveArrayData(getId(), t);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((mElementType == null) ? 0 : mElementType.hashCode());
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
        PrimitiveArrayType other = (PrimitiveArrayType) obj;
        if (mElementType != other.mElementType) {
            return false;
        }
        return true;
    }
}
