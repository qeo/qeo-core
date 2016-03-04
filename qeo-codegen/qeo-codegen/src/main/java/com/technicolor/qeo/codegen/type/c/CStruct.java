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
import java.util.LinkedHashMap;
import java.util.Map;

import com.technicolor.qeo.codegen.type.Container;

/**
 * Class to represent a C struct.
 */
public class CStruct
    extends Container<CMember>

{
    private final String mType;
    private final Map<String, String> mDdsSequences;

    /**
     * Create an instance of CStruct.
     * 
     * @param moduleName The moduleName.
     * @param baseName The basename. The struct name and struct type will be derived from this.
     */
    public CStruct(String moduleName, String baseName)
    {
        super(moduleName + "_" + baseName + "_t");
        mType = moduleName + "_" + baseName + "_type";
        mMembers = new ArrayList<CMember>();
        mDdsSequences = new LinkedHashMap<String, String>();
    }

    /**
     * This function returns the type of the struct.
     * 
     * @param moduleName The moduleName.
     * @param baseName string name
     * @return struct type to be used in the tsm generation
     */
    public static String constructType(String moduleName, String baseName)
    {
        return moduleName + "_" + baseName + "_type";
    }

    /**
     * This function returns the full struct name (module_baseName_t).
     * 
     * @param moduleName The moduleName.
     * @param baseName string name
     * @return struct name to be used by the tsm generation
     */
    public static String constructName(String moduleName, String baseName)
    {
        return moduleName + "_" + baseName + "_t";
    }

    /**
     * Get the type name.
     * 
     * @return The type.
     */
    public String getType()
    {
        return mType;
    }

    /**
     * Add a dds sequence to be printed in this cStruct.
     * 
     * @param type The sequence type
     * @param member The sequence struct member
     */
    public void addDdsSequence(String type, String member)
    {
        mDdsSequences.put(member, type);
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
}
