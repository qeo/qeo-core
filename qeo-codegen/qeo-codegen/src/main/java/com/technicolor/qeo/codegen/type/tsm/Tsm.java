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

package com.technicolor.qeo.codegen.type.tsm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class to represent a TSM structure in C.
 */
public class Tsm
{
    private final List<TsmMember> mMembers;
    private final String mName;
    private String mBasicName;

    /**
     * Create a Ctsm instance.
     * 
     * @param name The name of the TSM structure.
     */
    public Tsm(String name)
    {
        mMembers = new ArrayList<TsmMember>();
        mName = name;
    }

    /**
     * Get the tsm name.
     * 
     * @return The name.
     */
    public String getName()
    {
        return mName;
    }

    /**
     * Add an entry to the tsm.
     * 
     * @param item The entry to add.
     */
    public void addMember(TsmMember item)
    {
        mMembers.add(item);
    }

    /**
     * Get a list of all entries.
     * 
     * @return The list of all entries.
     */
    public List<TsmMember> getMembers()
    {
        return Collections.unmodifiableList(mMembers);
    }

    /**
     * Get the struct name.
     * 
     * @return The name.
     */
    public String getBasicName()
    {
        return mBasicName;
    }

    /**
     * Set the struct name.
     * 
     * @param name of the struct
     */
    public void setBasicName(String name)
    {
        mBasicName = name;
    }
}
