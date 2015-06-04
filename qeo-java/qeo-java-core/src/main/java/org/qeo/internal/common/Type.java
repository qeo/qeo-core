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
 * Representation of a type for which data will be consumed and produced throughout Qeo.
 * 
 * This representation is reflection and DDS agnostic.
 * 
 * The Type class is recursive, a Type instance can contain references to other Type instances.
 */
public abstract class Type
{

    /**
     * Enumeration specifying a field type.
     */
    public static enum MemberType {

        /** This type is used to represent a container of other types. */
        TYPE_CLASS(TypeImplemtation.OBJECT),
        /** The enumeration type. */
        TYPE_ENUM(TypeImplemtation.ENUM),
        /** The array type. */
        TYPE_ARRAY(TypeImplemtation.ARRAY),
        /** String type. */
        TYPE_STRING(TypeImplemtation.PRIMITIVE),
        /** String array type. */
        TYPE_STRINGARRAY(TypeImplemtation.PRIMITIVEARRAY),
        /** Integer type (signed 32-bit). */
        TYPE_INT(TypeImplemtation.PRIMITIVE),
        /** Integer array type. */
        TYPE_INTARRAY(TypeImplemtation.PRIMITIVEARRAY),
        /** Long type (signed 64-bit). */
        TYPE_LONG(TypeImplemtation.PRIMITIVE),
        /** Long array type. */
        TYPE_LONGARRAY(TypeImplemtation.PRIMITIVEARRAY),
        /** Byte type (signed 8-bit). */
        TYPE_BYTE(TypeImplemtation.PRIMITIVE),
        /** Byte array type. */
        TYPE_BYTEARRAY(TypeImplemtation.PRIMITIVEARRAY),
        /** Single precision floating point type. */
        TYPE_FLOAT(TypeImplemtation.PRIMITIVE),
        /** Float array type. */
        TYPE_FLOATARRAY(TypeImplemtation.PRIMITIVEARRAY),
        /** Boolean type. */
        TYPE_BOOLEAN(TypeImplemtation.PRIMITIVE),
        /** Boolean array type. */
        TYPE_BOOLEANARRAY(TypeImplemtation.PRIMITIVEARRAY),
        /** Integer type (signed 16-bit). */
        TYPE_SHORT(TypeImplemtation.PRIMITIVE),
        /** Short array type. */
        TYPE_SHORTARRAY(TypeImplemtation.PRIMITIVEARRAY);

        private final TypeImplemtation mTypeImplementation;

        private MemberType(TypeImplemtation typeImplementation)
        {
            this.mTypeImplementation = typeImplementation;
        }

        /**
         * Indicate to which type the specific type belongs.
         */
        public static enum TypeImplemtation {
            /** Array type. */
            ARRAY,
            /** Primive type. */
            PRIMITIVE,
            /** Primive array type. */
            PRIMITIVEARRAY,
            /** Object (class) type. */
            OBJECT,
            /** Enumeration type. */
            ENUM,
        }

        /**
         * Get the type implementation.
         * 
         * @return The TypeImplementation
         */
        public TypeImplemtation getTypeImplementation()
        {
            return mTypeImplementation;
        }

    }

    /**
     * Name of the type. This equals the topic and type name for the main TYPE_CLASS type.
     */
    protected final String mName;

    /**
     * Id used inside DDS to identify this field type.
     */
    protected int mId;

    /**
     * True in case this field is a key.
     */
    protected final boolean mKey;

    /**
     * Which field type does this type represent.
     */
    protected MemberType mMemberType;

    /**
     * Field name if this type is used as a member field.
     */
    protected final String mMemberName;

    /**
     * Constructor for a leave type.
     * 
     * @param name The name
     * @param id The id used to identify this type
     * @param key Whether it is key or not
     * @param type Which type of data does this type represent
     * @param memberName The name of the member field that uses this type
     */
    public Type(String name, int id, boolean key, MemberType type, String memberName)
    {
        super();
        this.mName = name;
        this.mId = id;
        this.mKey = key;
        this.mMemberType = type;
        this.mMemberName = memberName;
    }

    /**
     * Copy constructor for a leave type.
     * 
     * @param type The type to copy from
     */
    public Type(Type type)
    {
        super();
        this.mName = type.mName;
        this.mId = type.mId;
        this.mKey = type.mKey;
        this.mMemberType = type.mMemberType;
        this.mMemberName = type.mMemberName;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("Type: ");
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
     * Create a toString representation, respecting indentation level.
     * 
     * @param level indentation level
     * @return toString representation
     */
    public abstract String toString(int level);

    /**
     * Get the type.
     * 
     * @return The type.
     */
    public MemberType getType()
    {
        return mMemberType;
    }

    /**
     * Get the member ID.
     * 
     * @return The member ID.
     */
    public int getId()
    {
        return mId;
    }

    /**
     * Set the member ID.
     * 
     * @param id The member ID.
     */
    public void setId(int id)
    {
        this.mId = id;
    }

    /**
     * Get the type name.
     * 
     * @return The type name.
     */
    public String getName()
    {
        return mName;
    }

    /**
     * Is this a key member (of its parent type)?
     * 
     * @return True if key, false if not.
     */
    public boolean isKey()
    {
        return mKey;
    }

    /**
     * Extract a data object based on an object T.
     * 
     * @param <T> The class of the Qeo data type.
     * @param t The object to be transformed
     * 
     * @return The constructed data object
     */
    public abstract <T> Data toData(T t);

    /**
     * Get the name of the member.
     * 
     * @return The name of the member
     */
    public String getMemberName()
    {
        return mMemberName;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + mId;
        result = prime * result + (mKey ? 1231 : 1237);
        result = prime * result + ((mMemberName == null) ? 0 : mMemberName.hashCode());
        result = prime * result + ((mMemberType == null) ? 0 : mMemberType.hashCode());
        result = prime * result + ((mName == null) ? 0 : mName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Type other = (Type) obj;
        if (mId != other.mId) {
            return false;
        }
        if (mKey != other.mKey) {
            return false;
        }
        if (mMemberName == null) {
            if (other.mMemberName != null) {
                return false;
            }
        }
        else if (!mMemberName.equals(other.mMemberName)) {
            return false;
        }
        if (mMemberType != other.mMemberType) {
            return false;
        }
        if (mName == null) {
            if (other.mName != null) {
                return false;
            }
        }
        else if (!mName.equals(other.mName)) {
            return false;
        }
        return true;
    }

}
