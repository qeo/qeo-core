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

package org.qeo.internal.reflection;

import org.qeo.internal.common.ObjectData;
import org.qeo.internal.common.ObjectType;

/**
 * Interface to define a class that can be used for type introspection.
 * 
 * @param <TYPE> The type description of the Qeo data type
 * @param <DATA> The type object of the Qeo data type
 */
public interface IntrospectionUtil<TYPE, DATA>
{
    /**
     * Construct a type from the description.
     * 
     * @param typedesc The typedesciption
     * @return The internal type representation
     */
    ObjectType typeFromTypedesc(TYPE typedesc);

    /**
     * Construct a data object from the generic type.
     * 
     * @param obj The object to be used
     * @param type The type representation
     * @return The internal data object
     */
    ObjectData dataFromObject(DATA obj, ObjectType type);

    /**
     * Construct the end-user data object from an internal type.
     * 
     * @param data The internal data.
     * @param type The type representation
     * @return The end-user data
     */
    DATA objectFromData(ObjectData data, ObjectType type);
}
