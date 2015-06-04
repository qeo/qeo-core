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

package com.technicolor.qeo.codegen.type.qdm;


/**
 * QDM typedef representation.
 */
public abstract class TypeDef
{
    private final String mName;
    private String mType;
    private String mNonBasicType;
    private String mSequenceMaxLength;

    /**
     * Create a typedef instance.
     * 
     * @param name The name of the typedef
     */
    public TypeDef(String name)
    {
        this.mName = name;
    }

    /**
     * Get the name of the typedef.
     * 
     * @return The name.
     */
    public String getName()
    {
        return mName;
    }

    /**
     * Get the type of the typedef.
     * 
     * @return The type.
     */
    public String getType()
    {
        return mType;
    }

    /**
     * Set the type of the typedef.
     * 
     * @param type The type.
     */
    public void setType(String type)
    {
        this.mType = type;
    }


    /**
     * Get the nonbasictype field.
     * 
     * @return The nonbasictype field if defined. Null if not available.
     */
    public String getNonBasicType()
    {
        return mNonBasicType;
    }

    /**
     * Set the nonbasictype field.
     * 
     * @param nonBasicType The field value
     */
    public void setNonBasicType(String nonBasicType)
    {
        this.mNonBasicType = nonBasicType;
    }

    /**
     * Get the sequenceMaxLength field.
     * 
     * @return the field value if available, null otherwise.
     */
    public String getSequenceMaxLength()
    {
        return mSequenceMaxLength;
    }

    /**
     * Set the sequenceMaxLength field.
     * 
     * @param sequenceMaxLength The field content.
     */
    public void setSequenceMaxLength(String sequenceMaxLength)
    {
        this.mSequenceMaxLength = sequenceMaxLength;
    }
}
