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

package com.technicolor.qeo.codegen.type.objectivec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.technicolor.qeo.codegen.type.tsm.Tsm;

/**
 * Class to represent a C source file.
 */
public class OCSource
{
    private final List<String> mLocalIncludes;
    private final List<String> mExtIncludes;
    private final List<Tsm> mTsms;
    private final List<OCStruct> mStructs;

    /**
     * Create an instance.
     */
    public OCSource()
    {
        mTsms = new ArrayList<Tsm>();
        mLocalIncludes = new ArrayList<String>();
        mExtIncludes = new ArrayList<String>();
        mStructs = new ArrayList<OCStruct>();
    }

    /**
     * Add a tsm entry.
     * 
     * @param tsm The tsm entry.
     */
    public void addTsmList(List<Tsm> tsm)
    {
        mTsms.addAll(tsm);
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
     * Get the c structs to be defined in this source file.
     * 
     * @return The list of structs.
     */
    public List<OCStruct> getStructs()
    {
        return Collections.unmodifiableList(mStructs);
    }

    /**
     * Add a c struct to be defined in this source file.
     * 
     * @param struct The struct.
     */
    public void addStruct(OCStruct struct)
    {
        mStructs.add(struct);
    }

    /**
     * Add a local include to be printed in this header. To be in "" quotes.
     * 
     * @param include The include string
     */
    public void addLocalInclude(String include)
    {
        mLocalIncludes.add(include);
    }

    /**
     * Add a external include to be printed in this header. To be in <> quotes.
     * 
     * @param include The include string
     */
    public void addExternalInclude(String include)
    {
        mExtIncludes.add(include);
    }

    /**
     * Get a list of includes local includes (to be added in "" quotes).
     * 
     * @return the list of includes.
     */
    public List<String> getLocalIncludes()
    {
        return Collections.unmodifiableList(mLocalIncludes);
    }

    /**
     * Get a list of includes local includes (to be added in <> quotes).
     * 
     * @return the list of includes.
     */
    public List<String> getExternalIncludes()
    {
        return Collections.unmodifiableList(mExtIncludes);
    }

}
