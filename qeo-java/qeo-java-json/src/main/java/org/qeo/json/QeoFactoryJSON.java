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

package org.qeo.json;

import org.json.JSONObject;
import org.qeo.internal.CustomQeoFactory;

/**
 * QeoFactory that can work directly on JSON objects.
 */
public interface QeoFactoryJSON
    extends CustomQeoFactory<JSONObject, JSONObject>
{

}
