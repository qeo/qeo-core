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

import java.util.ArrayList;

import com.technicolor.qeo.codegen.type.Container;

/**
 * This class contains all the functionality to build the intermediate parsed object.
 */
public class QdmStruct
    extends Container<QdmMember>

{
    private String mBehavior;
    private boolean mHasKey;

    /**
     * Create a new struct object. We can associate this class to a struct tag in the qdm.
     * 
     * @param name The name of the struct.
     */
    public QdmStruct(String name)
    {
        super(name);
        mMembers = new ArrayList<QdmMember>();
    }

    /**
     * Get the behavior of the struct.
     * 
     * @return behavior of the struct. This math with the value of the attribute 'behavior' of the tag struct in the
     *         qdm. There are two possible values: 'state' or 'event'
     */
    public String getBehavior()
    {
        return mBehavior;
    }

    /**
     * Set the behavior of the struct.
     * 
     * @param behavior string. This math with the value of the attribute 'behavior' of the tag struct in the qdm. There
     *            are two possible values: 'state' or 'event'
     */
    public void setBehavior(String behavior)
    {
        this.mBehavior = behavior;
    }

    /**
     * Get the value of hasKey. If one of the members is key the struct will be set as key.
     * 
     * @return true if one of the members of the struct is key, false otherwise.
     */
    public boolean getHasKey()
    {
        if (this.mHasKey) {
            return this.mHasKey;
        }
        else {
            return false;
        }
    }

    /**
     * Set the field hasKey. If one of the members is key the struct will be set as key.
     * 
     * @param hasKey boolean value, true if one of the members of the struct is key, false otherwise.
     */
    public void setHasKey(boolean hasKey)
    {
        this.mHasKey = hasKey;
    }
}
