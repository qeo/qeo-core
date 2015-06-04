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

package org.qeo.json;

import org.json.JSONObject;
import org.qeo.QeoFactory;
import org.qeo.internal.BaseFactory;
import org.qeo.internal.QeoFactoryWithIntrospection;
import org.qeo.internal.reflection.IntrospectionUtil;
import org.qeo.json.internal.JSONIntrospection;

/**
 * Utility class to create a QeoFactoryJson from a regular QeoFactory.
 */
public final class QeoJSON
{
    private QeoJSON()
    {
    }

    /**
     * Create a QeoFactoryJson.
     * 
     * @param qeo QeoFactory instance to derive from.
     * @return A QeoFactoryJson instance.
     */
    public static QeoFactoryJSON getFactory(QeoFactory qeo)
    {
        BaseFactory baseFactory = (BaseFactory) qeo;
        return new QeoFactoryJSONImpl(baseFactory, new JSONIntrospection());
    }

    private static class QeoFactoryJSONImpl
        extends QeoFactoryWithIntrospection<JSONObject, JSONObject>
        implements QeoFactoryJSON
    {

        QeoFactoryJSONImpl(BaseFactory baseFactory, IntrospectionUtil<JSONObject, JSONObject> introspectionUtil)
        {
            super(baseFactory, introspectionUtil);
        }
    }
}
