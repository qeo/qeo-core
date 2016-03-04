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

package org.qeo.json.types;

import java.util.Random;

import org.qeo.Key;

public class JsonState
{
    @Key
    public String id; // key

    public String name; // non-key

    public int i; // non-key

    public JsonState()
    {
        name = null;
    }

    public JsonState(String idVal, String nameVal, int iVal)
    {
        super();
        this.id = idVal;
        this.name = nameVal;
        this.i = iVal;
    }

    public JsonState(String idVal, String nameVal)
    {
        this(idVal, nameVal, new Random().nextInt(100));
    }

    /**
     * Copy constructor
     */
    public JsonState(JsonState s)
    {
        this.id = s.id;
        this.name = s.name;
        this.i = s.i;
    }
}
