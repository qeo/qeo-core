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

package com.technicolor.qeo.codegen.type.json;

import java.util.HashSet;
import java.util.Set;

import com.technicolor.qeo.codegen.type.Member;

/**
 * Class to describe a JSON member.
 */
public class JsonMember
    extends Member
{
    private boolean mKey;
    private String mItemsType;
    private static final Set<String> TYPES;
    private int mLevel;

    static {
        TYPES = new HashSet<String>();
        TYPES.add("int16");
        TYPES.add("int32");
        TYPES.add("int64");
        TYPES.add("string");
        TYPES.add("byte");
        TYPES.add("float32");
        TYPES.add("boolean");
    }

    /**
     * Create an instance.
     * 
     * @param name The member name.
     */
    public JsonMember(String name)
    {
        super(name);
    }

    /**
     * Check if the member is a key field.
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
     * Check if the field is valid.
     * 
     * @return True if it's a supported type.
     */
    public boolean isBasicType()
    {
        return TYPES.contains(getType());
    }

    /**
     * Set the name of an nonBasic or the type of an Array, for the rest of types should be null.
     * 
     * @param itemsType - Type of the array or the struct.
     */
    public void setItemsType(String itemsType)
    {
        this.mItemsType = itemsType;
    }

    /**
     * Get the type of an array or the name of the struct of a nonBasic.
     * 
     * @return Type of an array, name of a nonBasic type, null for the rest.
     */
    public String getItemsType()
    {
        return mItemsType;
    }

    /**
     * Check if the array contains basic types.
     * 
     * @return true of is an array of basic types, false otherwise.
     */
    public boolean isArrayOfBasic()
    {
        return TYPES.contains(mItemsType);
    }

    /**
     * Get the level of arrays of arrays.
     * 
     * @return 0 if there is not an array of arrays.
     */
    public int getLevel()
    {
        return mLevel;
    }

    /**
     * Get the level of arrays of arrays.
     * 
     * @param level - 0 if there is not an array of arrays.
     */
    public void setLevel(int level)
    {
        mLevel = level;
    }
}
