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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to represent a Header file for Objective-C.
 */
public class OCHeader
{
    private String mIfDefGuard;
    private final List<String> mLocalIncludes;
    private final List<String> mExtIncludes;
    private final Map<String, String> mDdsSequences;
    private final List<OCModule> mModules;

    /**
     * Create an Objective-C Header instance.
     */
    public OCHeader()
    {
        mLocalIncludes = new ArrayList<String>();
        mExtIncludes = new ArrayList<String>();
        mDdsSequences = new LinkedHashMap<String, String>();
        mModules = new ArrayList<OCModule>();
    }

    /**
     * Get the ifdef included guard to be printed.
     * 
     * @return The ifdef guard
     */
    public String getIfDefGuard()
    {
        return mIfDefGuard;
    }

    /**
     * Set the ifdef included guard.
     * 
     * @param ifDefGuard The guard to be printed
     */
    public void setIfDefGuard(String ifDefGuard)
    {
        this.mIfDefGuard = ifDefGuard;
    }

    /**
     * Get a list of local includes (to be added in "" quotes).
     * 
     * @return the list of includes.
     */
    public List<String> getLocalIncludes()
    {
        return Collections.unmodifiableList(mLocalIncludes);
    }

    /**
     * Get a list of external includes (to be added in <> quotes).
     * 
     * @return the list of external includes.
     */
    public List<String> getExternalIncludes()
    {
        return Collections.unmodifiableList(mExtIncludes);
    }

    /**
     * Get a map of dds sequences.
     * 
     * @return Map of dds sequences.
     */
    public Map<String, String> getDdsSequences()
    {
        return Collections.unmodifiableMap(mDdsSequences);
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
     * Add a dds sequence to be printed in this header.
     * 
     * @param type The sequence type
     * @param member The sequence struct member
     */
    public void addDdsSequence(String type, String member)
    {
        mDdsSequences.put(type, member);
    }

    /**
     * Add a Module to the header.
     * 
     * @param mModule New module to be added
     */
    public void addModule(OCModule mModule)
    {
        mModules.add(mModule);
    }

    /**
     * Get a list of Modules.
     * 
     * @return The list of modules
     */
    public List<OCModule> getModules()
    {
        return mModules;
    }
}
