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

package com.technicolor.qeo.codegen.type;

/**
 * Interface for wrapping an existing container member.
 * 
 * @param <T1> The type that is being wrapped.
 * @param <T2>
 */
public interface ContainerMemberAdapter<T1 extends ContainerMember, T2 extends ContainerMember>
{
    /**
     * Actual call to be implemented for wrapping.
     * 
     * @param member The member to wrap.
     * @return The wrapped member.
     */
    T2 wrap(T1 member);
}
