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

package org.qeo.util;

import java.lang.reflect.Field;

import org.qeo.internal.reflection.ReflectionUtil;

/**
 * Utility class for Qeo data types.
 */
public final class TypeUtil
{
    private TypeUtil()
    {
        // utility class
    }

    /**
     * This function compares if the values of 2 Qeo keyed objects have the same key value.
     * 
     * @param o1 First object
     * @param o2 Second object
     * @return True if the value of all the keyed fields is the same
     */
    public static boolean equalKeys(Object o1, Object o2)
    {
        if (o1 == null || o2 == null) {
            return false; // if one of both is null they can never be equal.
        }
        if (!o1.getClass().equals(o2.getClass())) {
            return false; // different object
        }

        for (final Field field : o1.getClass().getFields()) {
            if (ReflectionUtil.fieldIsKey(field)) {
                try {
                    final Object f1 = field.get(o1);
                    final Object f2 = field.get(o2);
                    if (f1 == null && f2 == null) {
                        continue; // this key is equal
                    }
                    if (f1 == null || f2 == null) {
                        return false; // only 1 is null
                    }
                    if (!f1.equals(f2)) {
                        return false; // different content
                    }
                }
                catch (final Exception e) {
                    throw new IllegalStateException("Can't access field " + field, e);
                }
            }
        }

        // all keys tested and ok
        return true;
    }
}
