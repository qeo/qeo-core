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

package com.technicolor.qeo.codegen.type.java;

import java.util.HashSet;
import java.util.Set;

import com.technicolor.qeo.codegen.type.Member;

/**
 * Class to describe a Java member.
 */
public class JavaMember
    extends Member
{
    private boolean mKey;
    private boolean mArray;
    private static final Set<String> PRIMITIVES;

    static {
        PRIMITIVES = new HashSet<String>();
        PRIMITIVES.add("int");
        PRIMITIVES.add("long");
        PRIMITIVES.add("char");
        PRIMITIVES.add("byte");
        PRIMITIVES.add("short");
        PRIMITIVES.add("float");
        PRIMITIVES.add("double");
        PRIMITIVES.add("boolean");
    }

    /**
     * Create an instance.
     * 
     * @param name The member name.
     */
    public JavaMember(String name)
    {
        super(name);
    }

    /**
     * Check if this member is an array.
     * 
     * @return true if this member is an array
     */
    public boolean isArray()
    {
        return mArray;
    }

    /**
     * Indicates whether this field is an array or not.
     * 
     * @param array True if the field is an array.
     */
    public void setArray(boolean array)
    {
        this.mArray = array;
    }

    /**
     * check if the member is a key field.
     * 
     * @return True if it's a key, false otherwise.
     */
    public boolean isKey()
    {
        return mKey;
    }

    /**
     * Indicate if the field is a key field.
     * 
     * @param key True if the field is a key.
     */
    public void setKey(boolean key)
    {
        this.mKey = key;
    }

    /**
     * Check if the field is a primitive.
     * 
     * @return True if it's a primitive.
     */
    public boolean isPrimitive()
    {
        return PRIMITIVES.contains(getType());
    }
}
