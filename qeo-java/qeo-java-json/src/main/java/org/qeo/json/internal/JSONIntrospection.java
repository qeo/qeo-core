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

import org.json.JSONException;
import org.json.JSONObject;
import org.qeo.internal.common.ObjectData;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.reflection.IntrospectionUtil;

/**
 * <p>
 * Implementation for Qeo Introspection to handle JSON structures.
 * </p>
 */
public final class JSONIntrospection
    implements IntrospectionUtil<JSONObject, JSONObject>
{
    private final JSONIntrospectionType mJsonIntrospectionType;
    private final JSONIntrospectionDataToQeo mJsonIntrospectionDataToQeo;
    private final JSONIntrospectionDataToJson mJsonIntrospectionDataToJson;

    /**
     * Create an instance.
     */
    public JSONIntrospection()
    {
        mJsonIntrospectionType = new JSONIntrospectionType();
        mJsonIntrospectionDataToQeo = new JSONIntrospectionDataToQeo();
        mJsonIntrospectionDataToJson = new JSONIntrospectionDataToJson();
    }

    @Override
    public ObjectType typeFromTypedesc(JSONObject typedesc)
    {
        return mJsonIntrospectionType.typeFromTypedesc(typedesc);
    }

    @Override
    public ObjectData dataFromObject(JSONObject json, ObjectType type)
    {
        return mJsonIntrospectionDataToQeo.buildData(json, type, 0);
    }

    @Override
    public JSONObject objectFromData(ObjectData data, ObjectType type)
    {
        if (null == data) {
            return null;
        }
        try {
            return mJsonIntrospectionDataToJson.buildFromData(data, type);
        }
        catch (JSONException e) {
            throw new IllegalArgumentException("Can't make json object from internal data", e);
        }
    }

}
