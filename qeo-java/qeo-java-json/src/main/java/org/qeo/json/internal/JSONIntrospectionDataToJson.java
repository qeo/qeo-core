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

import java.lang.reflect.Array;
import java.util.Iterator;
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
import org.qeo.internal.common.PrimitiveData;
import org.qeo.internal.common.Type;
import org.qeo.internal.common.Type.MemberType;

/**
 * Class to translate Qeo internal data to JSON data.
 */
class JSONIntrospectionDataToJson
{
    private static final Logger LOG = Logger.getLogger("JSONIntrospection");

    private Object primitiveToJson(MemberType type, PrimitiveData data)
    {
        Object obj;
        if (type == MemberType.TYPE_LONG) {
            // special case for long. Since js cannot handle 64bits it's represented as a string.
            obj = Long.toString((Long) data.getValue());
        }
        else {
            obj = data.getValue();
        }
        return obj;
    }

    private JSONArray primitiveArrayToJson(MemberType type, PrimitiveArrayData data)
    {
        JSONArray jsonArray = new JSONArray();
        Object array = data.getValue();
        int size = Array.getLength(array);
        for (int i = 0; i < size; i++) {
            // special case for long. Since js cannot handle 64bits it's represented as a string.
            if (type == MemberType.TYPE_LONGARRAY) {
                jsonArray.put(Long.toString((Long) Array.get(array, i)));
            }
            else {
                jsonArray.put(Array.get(array, i));
            }
        }
        return jsonArray;
    }

    private JSONArray arrayToJson(ArrayType type, ArrayData data)
        throws JSONException
    {
        Type elementType = type.getElementType();
        JSONArray jsonArray = new JSONArray();
        switch (elementType.getType().getTypeImplementation()) {
            case PRIMITIVE:
                throw new IllegalStateException("Type ARRAY should not contain primitive data");
            case OBJECT: {
                Iterator<Data> objects = data.getElements().iterator();
                while (objects.hasNext()) {
                    ObjectData objectData = (ObjectData) objects.next();
                    JSONObject obj = buildFromData(objectData, (ObjectType) elementType);
                    jsonArray.put(obj);
                }
                break;
            }
            case ENUM: {
                Iterator<Data> objects = data.getElements().iterator();
                while (objects.hasNext()) {
                    EnumerationData enumData = (EnumerationData) objects.next();
                    jsonArray.put(enumData.getValue()); // enumeration values are integers in JSON
                }
                break;
            }
            case ARRAY:
            case PRIMITIVEARRAY:
                throw new IllegalStateException("Array of array not yet supported");
            default:
                throw new IllegalStateException("Unhandled array type: "
                    + elementType.getType().getTypeImplementation());

        }
        return jsonArray;
    }

    /**
     * Build JSONObject from Qeo internal data.
     * 
     * @param data Internal object data.
     * @param type Type describing the object.
     * @return A fully generated json object.
     * @throws JSONException Thrown if invalid json data is generated.
     */
    JSONObject buildFromData(ObjectData data, ObjectType type)
        throws JSONException
    {
        LOG.fine("Building JSONobject from internal qeo data");
        if (type.getType() != MemberType.TYPE_CLASS) {
            throw new IllegalArgumentException("buildFromData expects a struct type");
        }

        JSONObject json = new JSONObject();

        for (Type member : type.getMembers()) {
            Data dataMember = data.getContainedData(member.getId());
            String name = member.getMemberName();

            // make sure we only miss optional members
            if (null == dataMember) {
                if (!member.isKey()) {
                    continue;
                }
                else {
                    throw new IllegalArgumentException("missing key field " + name);
                }
            }

            Object obj;
            switch (member.getType().getTypeImplementation()) {
                case PRIMITIVE:
                    obj = primitiveToJson(member.getType(), (PrimitiveData) dataMember);
                    break;
                case OBJECT:
                    obj = buildFromData((ObjectData) dataMember, (ObjectType) member);
                    break;
                case ENUM:
                    /* enumeration values are integers in JSON */
                    obj = Integer.valueOf(((EnumerationData) dataMember).getValue());
                    break;
                case PRIMITIVEARRAY:
                    obj = primitiveArrayToJson(member.getType(), (PrimitiveArrayData) dataMember);
                    break;
                case ARRAY:
                    obj = arrayToJson((ArrayType) member, (ArrayData) dataMember);
                    break;
                default:
                    throw new IllegalStateException("Type implementation not supported: "
                        + member.getType().getTypeImplementation());
            }
            json.put(name, obj);
        }
        return json;
    }
}
