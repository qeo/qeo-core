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

package org.qeo;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that the class is a Qeo Type.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface QeoType {

    /**
     * Behavior of the Qeo type.
     */
    public enum Behavior {
        /** To indicate that this type is used as a subclass only in a Qeo Type. */
        NONE,
        /** Qeo Event type. */
        EVENT,
        /** Qeo State type. */
        STATE
    }

    /**
     * Behavior of the Qeo type.
     */
    Behavior behavior() default Behavior.NONE;
}
