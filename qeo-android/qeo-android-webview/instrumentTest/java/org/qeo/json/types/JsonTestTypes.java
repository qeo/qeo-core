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

public class JsonTestTypes
{
    public long longtype;
    public int inttype;
    public String stringtype;
    public byte bytetype;
    public float floattype;
    public boolean booleantype;
    public short shorttype;
    public JsonTestEnum enumtype;

    public JsonTestTypes()
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
        return sb.toString();
    }

    // CHECKSTYLE.OFF: ParameterNumber - need more than 7 to test all types
    public JsonTestTypes(long longtypeVal, int inttypeVal, String stringtypeVal, byte bytetypeVal, byte[] byteArrayVal,
        float floattypeVal, boolean booleantypeVal, JsonTestEnum enumtypeVal)
    // CHECKSTYLE.ON: ParameterNumber
    {
        super();
        longtype = longtypeVal;
        inttype = inttypeVal;
        stringtype = stringtypeVal;
        bytetype = bytetypeVal;
        floattype = floattypeVal;
        booleantype = booleantypeVal;
        enumtype = enumtypeVal;
    }

}
