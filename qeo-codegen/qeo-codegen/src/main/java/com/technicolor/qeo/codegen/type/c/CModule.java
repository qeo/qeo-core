/*
 * Copyright (c) 2015 - Qeo LLC
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

import com.technicolor.qeo.codegen.Convert;
import com.technicolor.qeo.codegen.type.qdm.QdmModule;

/**
 * C module representation.
 */
public class CModule
    extends QdmModule
{
    private final List<CStruct> mStructs;
    private final List<CEnum> mEnums;

    /**
     * Create a C module.
     * 
     * @param name The module name.
     */
    public CModule(String name)
    {
        super(Convert.qdmToC(name));
        mStructs = new ArrayList<CStruct>();
        mEnums = new ArrayList<CEnum>();
    }

    /**
     * Add a C struct.
     * 
     * @param struct the c struct.
     */
    public void addStruct(CStruct struct)
    {
        mStructs.add(struct);
    }

    /**
     * Get a list of CStruct defined.
     * 
     * @return The list of OCStruct.
     */
    public List<CStruct> getStruct()
    {
        return Collections.unmodifiableList(mStructs);
    }

    /**
     * Add a new value to the list of enums.
     * 
     * @param enumValue Value to be added to the list of enums
     */
    public void addEnum(CEnum enumValue)
    {
        mEnums.add(enumValue);
    }

    /**
     * Get the list of enums.
     * 
     * @return list values that the enum can have.
     */
    public List<CEnum> getEnum()
    {
        return mEnums;
    }
}
