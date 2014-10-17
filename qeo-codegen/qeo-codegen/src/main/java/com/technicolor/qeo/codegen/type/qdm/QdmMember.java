/*
 * Copyright (c) 2014 - Qeo LLC
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

import com.technicolor.qeo.codegen.type.Member;

/**
 * This class contains the methods to handle the member object. A member is an object used to keep all the information
 * related with the member tag of the qdm.
 */
public class QdmMember
    extends Member
{
    private boolean mKey;
    private String mNonBasicTypeName;
    private String mSequenceMaxLength;

    /**
     * Member constructor.
     * 
     * @param name The member name.
     */
    public QdmMember(String name)
    {
        super(name);
    }

    /**
     * Is a member set as key?
     * 
     * @return true is the member is key, false otherwise
     */
    public boolean isKey()
    {
        return mKey;
    }

    /**
     * Set the key with the given parameter.
     * 
     * @param key is true if the member tag has an attribute key="true", otherwise false.
     */
    public void setKey(boolean key)
    {
        this.mKey = key;
    }

    /**
     * Get the nonbasic type of the member.
     * 
     * @return The nonbasic type if defined. Null otherwise.
     */
    public String getNonBasicTypeName()
    {
        return mNonBasicTypeName;
    }

    /**
     * Set the nonbasic type name.
     * 
     * @param nonBasicTypeName The name.
     */
    public void setNonBasicTypeName(String nonBasicTypeName)
    {
        this.mNonBasicTypeName = nonBasicTypeName;
    }

    /**
     * Get the SequenceMaxLength field.
     * 
     * @return The field value if defined, null otherwise.
     */
    public String getSequenceMaxLength()
    {
        return mSequenceMaxLength;
    }

    /**
     * Set the SequenceMaxLength field.
     * 
     * @param sequenceMaxLength The field value
     */
    public void setSequenceMaxLength(String sequenceMaxLength)
    {
        this.mSequenceMaxLength = sequenceMaxLength;
    }

}
