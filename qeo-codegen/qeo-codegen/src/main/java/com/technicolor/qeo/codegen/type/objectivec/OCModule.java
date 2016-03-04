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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.technicolor.qeo.codegen.Convert;
import com.technicolor.qeo.codegen.type.qdm.QdmModule;

/**
 * Objective c module representation.
 */
public class OCModule
    extends QdmModule
{
    private final List<OCStruct> mStructs;
    private final List<OCEnum> mEnums;

    /**
     * Create a Objective C module.
     * 
     * @param name The module name.
     */
    public OCModule(String name)
    {
        super(Convert.qdmToC(name));
        mStructs = new ArrayList<OCStruct>();
        mEnums = new ArrayList<OCEnum>();
    }

    /**
     * Add a OCStruct.
     * 
     * @param struct the OCStruct.
     */
    public void addStruct(OCStruct struct)
    {
        mStructs.add(struct);
    }

    /**
     * Get a list of OCStruct defined.
     * 
     * @return The list of OCStruct.
     */
    public List<OCStruct> getStruct()
    {
        return Collections.unmodifiableList(mStructs);
    }

    /**
     * Add a new value to the list of enums.
     * 
     * @param enumValue Value to be added to the list of enums
     */
    public void addEnum(OCEnum enumValue)
    {
        mEnums.add(enumValue);
    }

    /**
     * Get the list of enums.
     * 
     * @return list values that the enum can have.
     */
    public List<OCEnum> getEnum()
    {
        return mEnums;
    }
}
