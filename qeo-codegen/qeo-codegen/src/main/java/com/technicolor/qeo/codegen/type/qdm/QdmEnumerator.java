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

import java.util.Locale;

import com.technicolor.qeo.codegen.type.ContainerMember;

/**
 * Representation of an enumerator constant as defined by the QDM.
 */
public class QdmEnumerator
    extends ContainerMember
{
    /**
     * Create a QDM enumerator constant with the given name.
     * 
     * @param name of the enum type
     */
    public QdmEnumerator(String name)
    {
        super((null != name) ? name.toUpperCase(Locale.ENGLISH) : null);
    }
}
