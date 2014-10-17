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

package com.technicolor.qeo.codegen.type;

/**
 * Class to represent a member type.
 */
public abstract class Member
    extends ContainerMember
{
    private String mType;

    /**
     * Create an instance.
     * 
     * @param name The member name.
     */
    public Member(String name)
    {
        super(name);
    }

    /**
     * Get the type.
     * 
     * @return The type
     */
    public String getType()
    {
        return mType;
    }

    /**
     * Set the type.
     * 
     * @param type the type
     */
    public void setType(String type)
    {
        this.mType = type;
    }
}
