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

package com.technicolor.qeo.codegen.type.qdm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * QDM module representation.
 */
public class QdmModule
{
    private final String mName;
    private final List<QdmStruct> mStructs;
    private final Map<String, TypeDef> mTypeDefs;
    private final List<QdmEnum> mEnums;

    /**
     * Create a QDM module.
     * 
     * @param name The module name.
     */
    public QdmModule(String name)
    {
        mName = name;
        mStructs = new ArrayList<QdmStruct>();
        mEnums = new ArrayList<QdmEnum>();
        mTypeDefs = new LinkedHashMap<String, TypeDef>();
    }

    /**
     * Add a QDM struct.
     * 
     * @param struct the qdm struct.
     */
    public void addStruct(QdmStruct struct)
    {
        mStructs.add(struct);
    }

    /**
     * Get a list of QDM structs defined.
     * 
     * @return The list of structs.
     */
    public List<QdmStruct> getStructs()
    {
        return Collections.unmodifiableList(mStructs);
    }

    /**
     * Get the name of the module.
     * 
     * @return The name.
     */
    public String getName()
    {
        return mName;
    }

    /**
     * Add a QDM typedef.
     * 
     * @param td The typedef.
     */
    public void addTypeDef(TypeDef td)
    {
        mTypeDefs.put(td.getName(), td);
    }

    /**
     * Get a typedef based on a name.
     * 
     * @param name The name of the typedef.
     * @return The typedef if found, null otherwise.
     */
    public TypeDef getTypeDef(String name)
    {
        return mTypeDefs.get(name);
    }

    /**
     * Get a list of typedefs.
     * 
     * @return The list of typedefs.
     */
    public Collection<TypeDef> getTypeDefs()
    {
        return Collections.unmodifiableCollection(mTypeDefs.values());
    }

    /**
     * Get the list of enums.
     * 
     * @return list values that the enum can have.
     */
    public List<QdmEnum> getEnums()
    {
        return mEnums;
    }

    /**
     * Add a new value to the list of enums.
     * 
     * @param enumValue Value to be added to the list of enums
     */
    public void addEnum(QdmEnum enumValue)
    {
        mEnums.add(enumValue);
    }

    /**
     * This function return the QdmEnumerator with the name passed as parameter.
     * 
     * @param qdmTypeNonBasic string
     * @return QdmEnumerator
     */
    public QdmEnum getEnum(String qdmTypeNonBasic)
    {
        int index = 0;
        for (QdmEnum mEnum : mEnums) {

            if (mEnum.getName().equals(qdmTypeNonBasic)) {
                return mEnums.get(index);
            }
            index++;
        }
        return null;
    }

    /**
     * This function return the position of the list where a QdmEnumerator with the same name as the parameter is.
     * 
     * @param qdmTypeNonBasic string
     * @return position of the list, -1 if it doesn't find it
     */
    public int containsEnum(String qdmTypeNonBasic)
    {
        int index = 0;
        for (QdmEnum mEnum : mEnums) {

            if (mEnum.getName().equals(qdmTypeNonBasic)) {
                return index;
            }
            index++;
        }
        return -1;

    }
}
