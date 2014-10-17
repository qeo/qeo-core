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
import java.util.Map;

import com.technicolor.qeo.codegen.type.Container;

/**
 * Class to represent a Objective-C struct.
 */
public class OCStruct
    extends Container<OCMember>
{
    private String mFullName;
    private final String mType;
    private final Map<String, String> mDdsSequences;

    /**
     * Create an instance of 0CStruct.
     * 
     * @param moduleName of the struct.
     * @param baseName The basename. The struct name and struct type will be derived from this.
     */
    public OCStruct(String moduleName, String baseName)
    {
        super(baseName);
        mFullName = moduleName + "_" + baseName;
        mType = constructType(moduleName, baseName);
        mMembers = new ArrayList<OCMember>();
        mDdsSequences = new LinkedHashMap<String, String>();
    }

    /**
     * This function returns the type of the struct.
     * 
     * @param moduleName of the struct.
     * @param baseName The basename. The struct name and struct type will be derived from this.
     * @return struct type to be used in the tsm generation
     */
    public static String constructType(String moduleName, String baseName)
    {
        return moduleName + "_" + baseName + "_type";
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
     * Add a dds sequence to be printed in this OCStruct.
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

    /**
     * Get the name of the struct.
     * 
     * @return String value, name of the struct, composition between the module_structName
     */
    @Override
    public String getFullName()
    {
        return mFullName;
    }

    /**
     * Set the name of the struct.
     * 
     * @param fullName - Composition between the module_structName.
     */
    public void setFullName(String fullName)
    {
        mFullName = fullName;
    }
}
