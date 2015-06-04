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

package org.qeo.json.internal;

import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.qeo.internal.common.ArrayData;
import org.qeo.internal.common.ArrayType;
import org.qeo.internal.common.Data;
import org.qeo.internal.common.EnumerationData;
import org.qeo.internal.common.ObjectData;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.common.PrimitiveArrayData;
import org.qeo.internal.common.PrimitiveArrayType;
import org.qeo.internal.common.PrimitiveData;
import org.qeo.internal.common.Type;
import org.qeo.internal.common.Type.MemberType;

/**
 * Class to translation JSON data to Qeo internal data.
 */
class JSONIntrospectionDataToQeo
{
    private static final Logger LOG = Logger.getLogger("JSONIntrospection");

    private PrimitiveData jsonToPrimitive(MemberType type, JSONObject json, String name, int id)
        throws JSONException
    {
        Object value = null;
        switch (type) {
            case TYPE_STRING:
                value = json.getString(name);
                break;
            case TYPE_INT:
                value = Integer.valueOf(json.getInt(name));
                break;
            case TYPE_LONG:
                value = Long.valueOf(json.getLong(name));
                break;
            case TYPE_SHORT:
                value = Short.valueOf((short) json.getInt(name));
                break;
            case TYPE_BYTE:
                value = Byte.valueOf((byte) json.getInt(name));
                break;
            case TYPE_FLOAT:
                value = new Float(json.getDouble(name));
                break;
            case TYPE_BOOLEAN:
                value = Boolean.valueOf(json.getBoolean(name));
                break;
            default:
                throw new IllegalStateException("unsupported primitive type: " + type);
        }
        return new PrimitiveData(id, value);
    }

    private PrimitiveArrayData jsonToPrimitiveArray(MemberType type, JSONArray arr, int id)
        throws JSONException
    {
        Object value = null;

        switch (type) {
            case TYPE_STRING:
                String[] stringArray = new String[arr.length()];
                for (int i = 0; i < arr.length(); ++i) {
                    stringArray[i] = arr.getString(i);
                }
                value = stringArray;
                break;
            case TYPE_INT:
                int[] intArray = new int[arr.length()];
                for (int i = 0; i < arr.length(); ++i) {
                    intArray[i] = arr.getInt(i);
                }
                value = intArray;
                break;
            case TYPE_LONG:
                long[] longArray = new long[arr.length()];
                for (int i = 0; i < arr.length(); ++i) {
                    longArray[i] = arr.getLong(i);
                }
                value = longArray;
                break;
            case TYPE_SHORT:
                short[] shortArray = new short[arr.length()];
                for (int i = 0; i < arr.length(); ++i) {
                    shortArray[i] = (short) arr.getInt(i);
                }
                value = shortArray;
                break;
            case TYPE_BYTE:
                byte[] byteArray = new byte[arr.length()];
                for (int i = 0; i < arr.length(); ++i) {
                    byteArray[i] = (byte) arr.getInt(i);
                }
                value = byteArray;
                break;
            case TYPE_FLOAT:
                float[] floatArray = new float[arr.length()];
                for (int i = 0; i < arr.length(); ++i) {
                    floatArray[i] = (float) arr.getDouble(i);
                }
                value = floatArray;
                break;
            case TYPE_BOOLEAN:
                boolean[] booleanArray = new boolean[arr.length()];
                for (int i = 0; i < arr.length(); ++i) {
                    booleanArray[i] = arr.getBoolean(i);
                }
                value = booleanArray;
                break;
            default:
                throw new IllegalStateException("unsupported primitive array type: " + type);
        }
        return new PrimitiveArrayData(id, value);
    }

    private ArrayData buildArray(JSONArray json, ArrayType type, int id)
        throws JSONException
    {
        ArrayData data = new ArrayData(id);
        Type elementType = type.getElementType();
        switch (type.getElementType().getType().getTypeImplementation()) {
            case OBJECT:
                for (int i = 0; i < json.length(); ++i) {
                    data.addElement(buildData(json.getJSONObject(i), (ObjectType) elementType, id));
                }
                break;
            case ENUM:
                for (int i = 0; i < json.length(); ++i) {
                    /* enumeration values are integers in JSON */
                    data.addElement(new EnumerationData(0, json.getInt(i)));
                }
                break;
            case ARRAY:
                throw new IllegalStateException("Array of array not yet supported");
            case PRIMITIVEARRAY:
                PrimitiveArrayType elementTypeArray = (PrimitiveArrayType) elementType;
                for (int i = 0; i < json.length(); ++i) {
                    data.addElement(jsonToPrimitiveArray(elementTypeArray.getElementType(), json.getJSONArray(i), 0));
                }
                break;
            case PRIMITIVE:
                throw new IllegalStateException("Array should not contain a primitive");
            default:
                throw new IllegalStateException("type not handled: "
                    + type.getElementType().getType().getTypeImplementation());
        }
        return data;
    }

    /**
     * Build internal Qeo data from JSON object.
     * 
     * @param json The json object.
     * @param type The type representing the object
     * @param id the current id of the class
     * @return Internal generated Qeo data
     */
    ObjectData buildData(JSONObject json, ObjectType type, int id)
    {
        if (type.getType() != MemberType.TYPE_CLASS) {
            throw new IllegalArgumentException("buildData expects a struct type");
        }

        ObjectData data = null;

        LOG.finer("Building data for struct " + type.getName() + "(" + type.getMemberName() + ")");
        try {
            data = new ObjectData(id);

            for (Type member : type.getMembers()) {
                String name = member.getMemberName();
                LOG.finer("Building member " + name);

                if (!json.has(name) || json.isNull(name)) {
                    throw new IllegalArgumentException("Missing property " + name);
                }

                Data dataMember = null;
                switch (member.getType().getTypeImplementation()) {
                    case PRIMITIVE:
                        dataMember = jsonToPrimitive(member.getType(), json, name, member.getId());
                        break;
                    case PRIMITIVEARRAY:
                        PrimitiveArrayType pat = (PrimitiveArrayType) member;
                        JSONArray arr1 = json.getJSONArray(name);
                        dataMember = jsonToPrimitiveArray(pat.getElementType(), arr1, member.getId());
                        break;
                    case OBJECT:
                        JSONObject sub = json.getJSONObject(name);
                        dataMember = buildData(sub, (ObjectType) member, member.getId());
                        break;
                    case ENUM:
                        /* enumeration values are integers in JSON */
                        int val = json.getInt(name);
                        dataMember = new EnumerationData(member.getId(), val);
                        break;
                    case ARRAY:
                        JSONArray arr2 = json.getJSONArray(name);
                        dataMember = buildArray(arr2, (ArrayType) member, member.getId());
                        break;
                    default:
                        throw new IllegalStateException("Unsupported typeimplementation: "
                            + member.getType().getTypeImplementation());
                }
                data.addMember(dataMember);

            }
        }
        catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
        return data;
    }
}
