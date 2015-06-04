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

package org.qeo.internal.common;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.internal.reflection.ReflectionUtil;

/**
 * Representation of object Data consumed and produced throughout Qeo.
 * 
 * This representation is reflection and DDS agnostic
 * 
 * The ObjectData class is recursive, an ObjectData instance can contain references to other data instances.
 */
public class ObjectData
    extends Data
{
    private static final Logger LOG = Logger.getLogger(Data.class.getName());

    /**
     * Hashtable containing all the contained data members of this ObjectData instance.
     */
    private final Map<Integer, Data> mMembers;

    /**
     * Construct a new object data instance. Members will be added later.
     * 
     * @param id Id of the object
     */
    public ObjectData(int id)
    {
        super(id);
        this.mMembers = new HashMap<Integer, Data>();
    }

    /**
     * Copy constructor.
     * 
     * @param data the object to cpoy from
     */
    public ObjectData(ObjectData data)
    {
        super(data.getId());
        this.mMembers = data.mMembers;
    }

    /**
     * Add a member to this data object.
     * 
     * @param member member to add
     */
    public void addMember(Data member)
    {
        this.mMembers.put(member.getId(), member);
    }

    /**
     * Retrieve a contained data object from the members list that has this id.
     * 
     * @param id The id of the data object to search for
     * @return The data object or null in case not found
     */
    public Data getContainedData(int id)
    {
        return mMembers.get(id);
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ObjectData: ");
        for (Data data : mMembers.values()) {
            sb.append("\n").append(data.toString());
        }
        return sb.toString();
    }

    /**
     * Check whether the object data has members.
     * 
     * @return True if there are members, false otherwise.
     */
    public boolean hasMembers()
    {
        return mMembers != null && !mMembers.isEmpty();
    }

    /**
     * Get the members.
     * 
     * @return The members or null if none.
     */
    public Map<Integer, Data> getMembers()
    {
        return Collections.unmodifiableMap(mMembers);
    }

    /**
     * Construct and object from a data sample.
     * 
     * @param <T> The class of the Qeo data type.
     * @param clazz The class of the object.
     * @param type The type of the data sample.
     * 
     * @return The constructed object.
     */
    @Override
    public <T> T toObject(Class<T> clazz, Type type)
    {
        T obj = null;

        /* Create a new instance from the data class */
        try {
            obj = clazz.getConstructor().newInstance();
            if (null != obj) {
                for (final Field field : obj.getClass().getFields()) {
                    if (!ReflectionUtil.isQeoField(field)) {
                        continue;
                    }
                    Type memberType = ((ObjectType) type).getTypeByName(field.getName());
                    if (null == memberType) {
                        memberType = ((ObjectType) type).getTypeByName(field.getType().getName());
                    }
                    field.set(obj, getContainedData(memberType.getId()).toObject(field.getType(), memberType));
                }
            }
        }
        catch (final RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            // It makes no sense to throw an exception here. This is called from a callback and the user will never get
            // the exception
            LOG.log(Level.SEVERE, "Error creating class from data", e);
        }
        return obj;
    }
}
