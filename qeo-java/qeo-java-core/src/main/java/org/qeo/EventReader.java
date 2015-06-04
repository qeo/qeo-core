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

package org.qeo;

/**
 * Inteface describing a Qeo event reader.<br>
 * An {@link EventReaderListener} will be attached to receive the events
 * 
 * @param <T> The class of the Qeo data type
 */
public interface EventReader<T>
    extends QeoReader<T>
{
}
