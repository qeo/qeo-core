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

import java.util.Iterator;

/**
 * A Qeo state reader.<br>
 * This reader extends the {@link Iterable} class so it can be used to iterate over this reader.<br>
 * Optional a {@link StateReaderListener} can be used to get notified if new data is available in the iterator.
 * 
 * @param <T> The class of the Qeo data type
 */
public interface StateReader<T>
    extends QeoReader<T>, Iterable<T>, Notifiable
{
    /**
     * An iterator for iterating over all instances of the class {@code T} .
     * 
     * An example of its usage:
     * 
     * <pre>
     * StateReader&lt;MyData&gt; myDataReader = qeo.createStateReader(MyData.class, null);
     * 
     * for (MyData md : myDataReader) {
     *     // do something usefull with md ...
     * }
     * </pre>
     * 
     * @return java Iterator object associated with the reader data.
     * @throws IllegalStateException if the reader has already been closed
     * @throws org.qeo.exception.OutOfResourcesException if not enough resources are available
     */
    @Override
    Iterator<T> iterator();
}
