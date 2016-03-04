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

package com.technicolor.qeo.codegen.type.objectivec;

import com.technicolor.qeo.codegen.type.Member;

/**
 * Class to describe a Objective-C member to be used in the header.
 */
public class OCMember
    extends Member
{
    private boolean mBasic = false;

    /**
     * Create an instance of OCMember.
     * 
     * @param name The member name.
     */
    public OCMember(String name)
    {
        super(name);
    }

    /**
     * Change the status of the member to basic if it is true, to nonbasic if is false.
     * 
     * @param bool , boolean true for basic.
     */
    public void setBasic(boolean bool)
    {
        mBasic = bool;

    }

    /**
     * Get the status of the member.
     * 
     * @return true basic, false nonbasic.
     */
    public boolean getBasic()
    {
        return mBasic;
    }

}
