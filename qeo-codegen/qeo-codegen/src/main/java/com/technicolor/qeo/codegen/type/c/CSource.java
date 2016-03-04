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

package com.technicolor.qeo.codegen.type.c;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.technicolor.qeo.codegen.type.tsm.Tsm;

/**
 * Class to represent a C source file.
 */
public class CSource
{
    private final List<String> mIncludes;
    private final List<Tsm> mTsms;

    /**
     * Create an instance.
     */
    public CSource()
    {
        mTsms = new ArrayList<Tsm>();
        mIncludes = new ArrayList<String>();
    }

    /**
     * Add an include line.
     * 
     * @param include the include value.
     */
    public void addInclude(String include)
    {
        mIncludes.add(include);
    }

    /**
     * Get a list of all include lines.
     * 
     * @return The includes.
     */
    public List<String> getIncludes()
    {
        return Collections.unmodifiableList(mIncludes);
    }

    /**
     * Add a tsm entry.
     * 
     * @param tsm The tsm entry.
     */
    public void addTsm(Tsm tsm)
    {
        mTsms.add(tsm);
    }

    /**
     * Get a list of all tsms defined in this source file.
     * 
     * @return The list of tsms
     */
    public List<Tsm> getTsms()
    {
        return Collections.unmodifiableList(mTsms);
    }

    /**
     * Set the tsm List.
     * 
     * @param tsm The list of tsms
     */
    public void addTsmList(List<Tsm> tsm)
    {
        mTsms.addAll(tsm);

    }
}
