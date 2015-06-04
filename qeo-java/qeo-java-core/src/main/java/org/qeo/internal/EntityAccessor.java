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

package org.qeo.internal;

import org.qeo.exception.QeoException;
import org.qeo.internal.common.EntityType;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.common.ReaderEntity;
import org.qeo.internal.common.ReaderListener;
import org.qeo.internal.common.WriterEntity;
import org.qeo.policy.PolicyUpdateListener;

/**
 * Interface to define entities that could differ for different implementations (eg java and android).
 */
public interface EntityAccessor
{

    /**
     * Create a WriterEntity.
     * 
     * @param type The type to create the writer for.
     * @param etype The type of writer.
     * @param policyListener An (optional) policy update listener to attach to the writer
     * @return The WriterEntity implementation.
     * 
     * @throws QeoException If the creation of the writer failed.
     */
    WriterEntity getWriter(ObjectType type, EntityType etype, PolicyUpdateListener policyListener)
        throws QeoException;

    /**
     * Create a ReaderEntity.
     * 
     * @param type The type to create the reader for.
     * @param etype The type of reader.
     * @param listener An optional listener to call when needed.
     * @param policyListener An (optional) policy update listener to attach to the reader
     * @return The ReaderEntity implementation.
     * 
     * @throws QeoException If the creation of the reader failed.
     */
    ReaderEntity getReader(ObjectType type, EntityType etype, ReaderListener listener,
        PolicyUpdateListener policyListener)
        throws QeoException;
}
