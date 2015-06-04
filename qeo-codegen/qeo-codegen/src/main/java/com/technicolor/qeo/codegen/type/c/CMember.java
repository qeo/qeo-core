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

import com.technicolor.qeo.codegen.type.Member;

/**
 * Class to describe a C member to be used in the header.
 */
public class CMember
    extends Member
{
    /**
     * Create an instance.
     * 
     * @param name The member name.
     */
    public CMember(String name)
    {
        super(name);
    }
}
