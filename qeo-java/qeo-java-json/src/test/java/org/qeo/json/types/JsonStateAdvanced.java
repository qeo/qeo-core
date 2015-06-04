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

package org.qeo.json.types;

import org.qeo.Key;

/**
 * 
 */
public class JsonStateAdvanced
{
    @Key
    public int id;
    public int id2;
    public int id3;
    @Key
    public SubClass1 keyedClass1;
    public SubClass1 unKeyedClass1;
    @Key
    public SubClass2 keyedClass2;
    public SubClass2 unKeyedClass2;

    public static class SubClass1
    {
        @Key
        public int id;
        public String name;
    }

    public static class SubClass2
    {
        public int id;
        public String name;
    }
}
