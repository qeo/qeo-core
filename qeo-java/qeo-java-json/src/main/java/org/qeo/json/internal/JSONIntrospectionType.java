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

package org.qeo.json.internal;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.qeo.internal.common.ArrayType;
import org.qeo.internal.common.EnumerationType;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.common.PrimitiveArrayType;
import org.qeo.internal.common.PrimitiveType;
import org.qeo.internal.common.Type;
import org.qeo.internal.common.Type.MemberType;
import org.qeo.internal.common.Util;

/**
 * Class to handle the internal Qeo type creation from a JSON type definition.
 */
class JSONIntrospectionType
{
    private static final Logger LOG = Logger.getLogger("JSONIntrospection");

    private enum JSType {
        BOOLEAN, BYTE, INT16, INT32, INT64, FLOAT32, DOUBLE, STRING, OBJECT, ENUM, ARRAY;
    }

    private static final List<String> KNOWN_BEHAVIORS = Arrays.asList(new String[] {"STATE", "EVENT"});

    /**
     * Generate internal Qeo type from JSON description.
     * 
     * @param typedesc The JSON description.
     * @return The internal Qeo type
     */
    ObjectType typeFromTypedesc(JSONObject typedesc)
    {
        String behavior = null;

        try {
            /* parse the JSON string and extract basic information */
            LOG.fine("Start creating Qeo type from JSON");
            behavior = typedesc.getString("behavior").toUpperCase(Locale.ENGLISH);

            if (!KNOWN_BEHAVIORS.contains(behavior)) {
                throw new IllegalArgumentException("unknown behavior " + behavior);
            }

            // OK now build it as a struct type
            ObjectType type = buildStructType(typedesc, 0, false, null);
            LOG.log(Level.FINER, "Created type from JSON: ${0}", type);
            return type;

        }
        catch (JSONException e) {
            try {
                throw new IllegalArgumentException("Can't parse " + typedesc.toString(2), e);
            }
            catch (JSONException e1) {
                // this is bad...
                throw new IllegalStateException(e);
            }
        }

    }

    private MemberType getMemberType(JSONObject member)
        throws JSONException
    {
        String type = member.getString("type");
        JSType jsType = null;
        try {
            jsType = JSType.valueOf(type.toUpperCase(Locale.ENGLISH));
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Type \"" + type + "\" is not supported.");
        }
        MemberType memberType = null;
        switch (jsType) {
            case STRING:
                memberType = MemberType.TYPE_STRING;
                break;
            case INT16:
                memberType = MemberType.TYPE_SHORT;
                break;
            case INT32:
                memberType = MemberType.TYPE_INT;
                break;
            case INT64:
                memberType = MemberType.TYPE_LONG;
                break;
            case BOOLEAN:
                memberType = MemberType.TYPE_BOOLEAN;
                break;
            case FLOAT32:
                memberType = MemberType.TYPE_FLOAT;
                break;
            case BYTE:
                memberType = MemberType.TYPE_BYTE;
                break;
            case OBJECT:
                memberType = MemberType.TYPE_CLASS;
                break;
            case ENUM:
                memberType = MemberType.TYPE_ENUM;
                break;
            case ARRAY:
                memberType = MemberType.TYPE_ARRAY;
                break;
            default:
                throw new IllegalStateException("Unhandled type: " + jsType);
        }
        return memberType;
    }

    private Type buildArrayType(JSONObject member, String memberName, int id, boolean key)
        throws JSONException
    {
        LOG.fine("Building ARRAY type");
        Type t;
        JSONObject items = member.getJSONObject("items");
        MemberType subType = getMemberType(items);
        switch (subType.getTypeImplementation()) {
            case PRIMITIVE:
                // primitive array
                t = new PrimitiveArrayType(memberName, id, key, subType);
                break;
            case OBJECT: {
                // array of structs
                // sub-id and name are not relevant, so put to null
                ObjectType elemType = buildStructType(items.getJSONObject("item"), 0, false, null);
                t = new ArrayType(memberName, id, key, elemType);
                break;
            }
            case ENUM: {
                // array of enums
                // sub-id and name are not relevant, so put to null
                EnumerationType elemType = buildEnumType(items.getJSONObject("item"), 0, false, null);
                t = new ArrayType(memberName, id, key, elemType);
                break;
            }
            case ARRAY:
                // array of array
                // sub-id and name are not relevant, so put to null
                // Type elementArray = buildArrayType(items, null, 0, false);
                // t = new ArrayType(memberName, id, key, elementArray);

                // just remove this for array of array support.
                // type introspection is already working, only data translation needs to be done.
                throw new IllegalStateException("Array of Array not yet supported");
                // break;
            default:
                throw new IllegalStateException("Unsupported type: " + subType.getTypeImplementation());
        }
        return t;
    }

    private EnumerationType buildEnumType(JSONObject typedesc, int id, boolean isKey, String memberName)
        throws JSONException
    {
        LOG.fine("Building ENUM type");

        String name = typedesc.optString("enum", null);
        if (null == name) {
            throw new IllegalArgumentException("Missing mandatory field \"enum\"");
        }
        JSONObject values = typedesc.optJSONObject("values");
        if (null == values) {
            throw new IllegalArgumentException("Missing mandatory field \"values\"");
        }
        else if (0 == values.length()) {
            throw new IllegalArgumentException("Enumeration constants list is empty");
        }
        /* create list of enum constants sorted by label (the number associated with it) */
        String[] enumConstants = new String[values.length()];
        @SuppressWarnings("unchecked")
        Iterator<String> keys = values.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            int val = values.getInt(key);

            if (val >= enumConstants.length) {
                throw new IllegalArgumentException("Enumeration labels should be 0,1,2..[n-1]");
            }
            enumConstants[val] = key;
        }
        return new EnumerationType(name, memberName, id, isKey, enumConstants);
    }

    private ObjectType buildStructType(JSONObject typedesc, int typeId, boolean isKey, String structName)
        throws JSONException
    {
        LOG.fine("Building STRUCT type");
        String topicname = typedesc.optString("topic", null);
        if (topicname == null) {
            throw new IllegalArgumentException("Missing mandatory field \"topic\"");
        }
        JSONObject properties = typedesc.getJSONObject("properties");

        ObjectType type = new ObjectType(topicname.replaceAll("::", "."), typeId, isKey, structName);
        @SuppressWarnings("unchecked")
        Iterator<String> keys = properties.keys();
        while (keys.hasNext()) {
            String memberName = keys.next();
            LOG.fine("Building member " + memberName);
            JSONObject member = properties.getJSONObject(memberName);

            boolean key = member.optBoolean("key", false);
            LOG.fine("Member is key? " + key);
            MemberType memberTypeCode = getMemberType(member);

            Type t = null;
            int id = Util.calculateID(memberName);
            switch (memberTypeCode.getTypeImplementation()) {
                case PRIMITIVE:
                    t = new PrimitiveType(memberName, id, key, memberTypeCode);
                    break;
                case ARRAY:
                    t = buildArrayType(member, memberName, id, key);
                    break;
                case OBJECT:
                    t = buildStructType(member.getJSONObject("item"), id, key, memberName);
                    break;
                case ENUM:
                    t = buildEnumType(member.getJSONObject("item"), id, key, memberName);
                    break;
                default:
                    throw new IllegalStateException("Unknown type");
            }
            type.addMember(t, memberName);
        }
        if (type.getMembers().size() == 0) {
            throw new IllegalArgumentException("Struct " + topicname + " did not define any properties");
        }
        return type;
    }
}
