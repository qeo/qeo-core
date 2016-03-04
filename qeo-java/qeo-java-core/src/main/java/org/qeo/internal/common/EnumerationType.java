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

import java.util.Arrays;

/**
 * Representation of an enumeration type for which data will be consumed and produced throughout Qeo.
 * 
 * This representation is reflection and DDS agnostic
 */
public class EnumerationType
    extends Type
{
    private final String[] mEnumConstants;

    /**
     * Constructor for an array type.
     * 
     * @param typeName The name of the type
     * @param memberName The name of the member
     * @param id The id used to identify this type
     * @param key Whether it is key or not
     * @param enumConstants The enumeration constants
     */
    public EnumerationType(String typeName, String memberName, int id, boolean key, Object[] enumConstants)
    {
        super(typeName, id, key, MemberType.TYPE_ENUM, memberName);
        mEnumConstants = new String[enumConstants.length];
        for (int i = 0; i < enumConstants.length; i++) {
            mEnumConstants[i] = enumConstants[i].toString();
        }
    }

    /**
     * Get the enumeration constants.
     * 
     * @return The enumeration constants
     */
    public String[] getConstants()
    {
        return mEnumConstants.clone();
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
        sb.append("EnumerationType: ");
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
        String sep = "";
        for (String c : mEnumConstants) {
            sb.append(sep);
            sep = ", ";
            sb.append(c);
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public <T> Data toData(T t)
    {
        return new EnumerationData(getId(), (Enum<?>) t);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(mEnumConstants);
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
        EnumerationType other = (EnumerationType) obj;
        if (!Arrays.equals(mEnumConstants, other.mEnumConstants)) {
            return false;
        }
        return true;
    }
}
