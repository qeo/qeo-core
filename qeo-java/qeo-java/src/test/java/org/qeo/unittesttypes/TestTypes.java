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

package org.qeo.unittesttypes;

import java.util.Arrays;

public class TestTypes
{
    public long longtype;
    public int inttype;
    public short shorttype;
    public String stringtype;
    public byte bytetype;
    public float floattype;
    public boolean booleantype;
    public TestEnum enumtype;
    public long[] longarraytype;
    public int[] intarraytype;
    public short[] shortarraytype;
    public String[] stringarraytype;
    public byte[] bytearraytype;
    public float[] floatarraytype;
    public boolean[] booleanarraytype;
    public TestEnum[] enumarraytype;

    public TestTypes()
    {

    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("TestTypes:");
        sb.append(" long - ");
        sb.append(this.longtype);
        sb.append(" int - ");
        sb.append(this.inttype);
        sb.append(" short - ");
        sb.append(this.shorttype);
        sb.append(" string - ");
        sb.append(this.stringtype);
        sb.append(" byte - ");
        sb.append(this.bytetype);
        sb.append(" float - ");
        sb.append(this.floattype);
        sb.append(" boolean - ");
        sb.append(this.booleantype);
        sb.append(" enum - ");
        sb.append(this.enumtype);
        sb.append(" longarray - ");
        sb.append(Arrays.toString(this.longarraytype));
        sb.append(" intarray - ");
        sb.append(Arrays.toString(this.intarraytype));
        sb.append(" shortarray - ");
        sb.append(Arrays.toString(this.shortarraytype));
        sb.append(" stringarray - ");
        sb.append(Arrays.toString(this.stringarraytype));
        sb.append(" bytearray - ");
        sb.append(Arrays.toString(this.bytearraytype));
        sb.append(" floatarray - ");
        sb.append(Arrays.toString(this.floatarraytype));
        sb.append(" booleanarray - ");
        sb.append(Arrays.toString(this.booleanarraytype));
        sb.append(" enumarray - ");
        sb.append(Arrays.toString(this.enumarraytype));
        return sb.toString();
    }

    // CHECKSTYLE.OFF: ParameterNumber - need more than 7 to test all types
    public TestTypes(long longtypeVal, int inttypeVal, short shorttypeVal, String stringtypeVal, byte bytetypeVal,
        float floattypeVal, boolean booleantypeVal, TestEnum enumtypeVal)
    // CHECKSTYLE.ON: ParameterNumber
    {
        super();
        longtype = longtypeVal;
        inttype = inttypeVal;
        shorttype = shorttypeVal;
        stringtype = stringtypeVal;
        bytetype = bytetypeVal;
        floattype = floattypeVal;
        booleantype = booleantypeVal;
        enumtype = enumtypeVal;
    }

    // CHECKSTYLE.OFF: ParameterNumber - need more than 7 to test all types
    public void setArrays(long[] longarrayVal, int[] intarrayVal, short[] shortarrayVal, String[] stringarrayVal,
        byte[] bytearrayVal, float[] floatarrayVal, boolean[] booleanarrayVal, TestEnum[] enumarrayVal)
    // CHECKSTYLE.ON: ParameterNumber
    {
        longarraytype = Arrays.copyOf(longarrayVal, longarrayVal.length);
        intarraytype = Arrays.copyOf(intarrayVal, intarrayVal.length);
        shortarraytype = Arrays.copyOf(shortarrayVal, shortarrayVal.length);
        stringarraytype = Arrays.copyOf(stringarrayVal, stringarrayVal.length);
        bytearraytype = Arrays.copyOf(bytearrayVal, bytearrayVal.length);
        floatarraytype = Arrays.copyOf(floatarrayVal, floatarrayVal.length);
        booleanarraytype = Arrays.copyOf(booleanarrayVal, booleanarrayVal.length);
        enumarraytype = Arrays.copyOf(enumarrayVal, enumarrayVal.length);
    }
}
