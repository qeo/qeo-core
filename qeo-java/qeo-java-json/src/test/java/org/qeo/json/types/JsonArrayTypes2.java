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

/**
 * 
 */

public class JsonArrayTypes2
{
    public int id;
    public int[] myArrayOfInt;
    // public int[][] myArrayOfArrayOfInt;
    public SubClass[] myArrayOfStruct;

    // public int[][][] myArrayOfOfArrayArrayOfInt;

    public static class SubClass
    {
        public int id;
        public String name;

        @Override
        public String toString()
        {
            return "TestClass2:\n\tid: " + id + "\n\tname: " + name;
        }
    }
}
