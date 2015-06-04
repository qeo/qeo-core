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

package org.qeo.unittesttypes;

import java.util.Arrays;

public class TestArrayTypes
{
    public long[][][] longarray3dtype;
    public int[][][] intarray3dtype;
    public short[][][] shortarray3dtype;
    public String[][][] stringarray3dtype;
    public byte[][][] bytearray3dtype;
    public float[][][] floatarray3dtype;
    public boolean[][][] booleanarray3dtype;
    public TestEnum[][][] enumarray3dtype;

    public TestArrayTypes()
    {

    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("TestArrayTypes:");
        sb.append(" longarray - ");
        sb.append(Arrays.toString(this.longarray3dtype));
        sb.append(" intarray - ");
        sb.append(Arrays.toString(this.intarray3dtype));
        sb.append(" shortarray - ");
        sb.append(Arrays.toString(this.shortarray3dtype));
        sb.append(" stringarray - ");
        sb.append(Arrays.toString(this.stringarray3dtype));
        sb.append(" bytearray - ");
        sb.append(Arrays.toString(this.bytearray3dtype));
        sb.append(" floatarray - ");
        sb.append(Arrays.toString(this.floatarray3dtype));
        sb.append(" booleanarray - ");
        sb.append(Arrays.toString(this.booleanarray3dtype));
        sb.append(" enumarray - ");
        sb.append(Arrays.toString(this.enumarray3dtype));
        return sb.toString();
    }

    // CHECKSTYLE.OFF: ParameterNumber - need more than 7 to test all types
    public TestArrayTypes(long[][][] longarray3dVal, int[][][] intarray3dVal, short[][][] shortarray3dVal,
        String[][][] stringarray3dVal, byte[][][] bytearray3dVal, float[][][] floatarray3dVal,
        boolean[][][] booleanarray3dVal, TestEnum[][][] enumarray3dVal)
    // CHECKSTYLE.ON: ParameterNumber
    {
        longarray3dtype = Arrays.copyOf(longarray3dVal, longarray3dVal.length);
        intarray3dtype = Arrays.copyOf(intarray3dVal, intarray3dVal.length);
        shortarray3dtype = Arrays.copyOf(shortarray3dVal, shortarray3dVal.length);
        stringarray3dtype = Arrays.copyOf(stringarray3dVal, stringarray3dVal.length);
        bytearray3dtype = Arrays.copyOf(bytearray3dVal, bytearray3dVal.length);
        floatarray3dtype = Arrays.copyOf(floatarray3dVal, floatarray3dVal.length);
        booleanarray3dtype = Arrays.copyOf(booleanarray3dVal, booleanarray3dVal.length);
        enumarray3dtype = Arrays.copyOf(enumarray3dVal, enumarray3dVal.length);
    }
}
