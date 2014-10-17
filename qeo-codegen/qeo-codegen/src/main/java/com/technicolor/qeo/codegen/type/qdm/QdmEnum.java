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

package com.technicolor.qeo.codegen.type.qdm;

import java.util.ArrayList;

import com.technicolor.qeo.codegen.type.Container;

/**
 * Representation of an enumerated type as defined by the QDM.
 */
public class QdmEnum
    extends Container<QdmEnumerator>
{

    /**
     * Create a QDM enum type with the given name.
     * 
     * @param name of the enum type
     */
    public QdmEnum(String name)
    {
        super(name);
        mMembers = new ArrayList<QdmEnumerator>();
    }

    /**
     * Add a new value to the list of enums.
     * 
     * @param enumValue Value to be added to the list of enums
     */
    public void addValue(QdmEnumerator enumValue)
    {
        mMembers.add(enumValue);
    }
}
