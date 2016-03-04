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

import java.util.Arrays;

import org.qeo.unittesttypes.TestEnum;

/**
 * 
 */
public class JsonTestArrayTypes
{

    public long[] longarraytype;
    public int[] intarraytype;
    public short[] shortarraytype;
    public String[] stringarraytype;
    public float[] floatarraytype;
    public boolean[] booleanarraytype;
    public byte[] bytearraytype;
    public TestEnum[] enumarraytype;

    /** public TestTypes[] structarraytype; **/

    public JsonTestArrayTypes()
    {

    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("TestArrayTypes:");
        sb.append(" longarray - ");
        sb.append(Arrays.toString(this.longarraytype));
        sb.append(" intarray - ");
        sb.append(Arrays.toString(this.intarraytype));
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
        /**
         * sb.append(" structarray - "); sb.append(Arrays.toString(this.structarraytype));
         **/
        return sb.toString();
    }

    public JsonTestArrayTypes(long[] longArraytypeVal, int[] intArraytypeVal, String[] stringArraytypeVal,
        byte[] byteArraytypeVal, float[] floatArraytypeVal, boolean[] booleanArraytypeVal, TestEnum[] enumArraytypeVal)
    {
        super();

        if (longArraytypeVal != null) {
            longarraytype = Arrays.copyOf(longArraytypeVal, longArraytypeVal.length);
        }

        if (intArraytypeVal != null) {
            intarraytype = Arrays.copyOf(intArraytypeVal, intArraytypeVal.length);
        }

        if (stringArraytypeVal != null) {
            stringarraytype = Arrays.copyOf(stringArraytypeVal, stringArraytypeVal.length);
        }

        if (byteArraytypeVal != null) {
            bytearraytype = Arrays.copyOf(byteArraytypeVal, byteArraytypeVal.length);
        }

        if (floatArraytypeVal != null) {
            floatarraytype = Arrays.copyOf(floatArraytypeVal, floatArraytypeVal.length);
        }

        if (booleanArraytypeVal != null) {
            booleanarraytype = Arrays.copyOf(booleanArraytypeVal, booleanArraytypeVal.length);
        }
        if (enumArraytypeVal != null) {
            enumarraytype = Arrays.copyOf(enumArraytypeVal, enumArraytypeVal.length);
        }

        /**
         * if (structArraytypeVal != null) { structarraytype = Arrays.copyOf(structArraytypeVal,
         * structArraytypeVal.length); }
         **/

    }

}
