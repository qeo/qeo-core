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

package org.qeo.internal.common;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.internal.reflection.ReflectionUtil;

/**
 * Representation of an object type for which data will be consumed and produced throughout Qeo.
 * 
 * This representation is reflection and DDS agnostic
 * 
 * The ObjectType class is recursive, a Type instance can contain references to other Type instances.
 */
public class ObjectType
    extends Type
{

    private static final Logger LOG = Logger.getLogger(Type.class.getName());

    /**
     * Hashtable used for performance reasons to allow fast lookup of a type based on its name.
     */
    private final Map<String, Type> mMembers;

    /**
     * True in case this field is a key or one of its members is a key.
     */
    private boolean mHasKeyMembers = false;

    /**
     * Constructor for a leave type.
     * 
     * @param name The name
     * @param id The id used to identify this type
     * @param key Whether it is key or not
     * @param memberName the name of this member
     */
    public ObjectType(String name, int id, boolean key, String memberName)
    {
        super(name, id, key, MemberType.TYPE_CLASS, memberName);
        this.mMembers = new HashMap<String, Type>();
    }

    /**
     * Constructor for a leave type.
     * 
     * @param name The name
     * @param id The id used to identify this type
     * @param key Whether it is key or not
     */
    public ObjectType(String name, int id, boolean key)
    {
        this(name, id, key, name);
    }

    /**
     * Copy constructor for a leave type.
     * 
     * @param type The type to copy from
     */
    public ObjectType(ObjectType type)
    {
        super(type);
        this.mMembers = type.mMembers;
    }

    /**
     * Add a member to a branch kind of type.
     * 
     * @param type The member to add
     * @param name The name of the member
     */
    public void addMember(Type type, String name)
    {
        if (type.isKey()) {
            mHasKeyMembers = true;
        }
        this.mMembers.put(name, type);
    }

    @Override
    public String toString()
    {
        return toString(0);
    }

    @Override
    public String toString(int level)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ObjectType: ");
        sb.append(mName);
        if (mMemberName != null) {
            sb.append(" - ");
            sb.append(mMemberName);
        }
        sb.append(" - ");
        sb.append(mMemberType.name());
        if (mId > 0) {
            // this info is only valid if it has an ID
            sb.append(" - ");
            sb.append(mId);
            sb.append(" - ");
            sb.append(mKey ? "key" : "no key");
        }
        if (mMembers.size() > 0) {
            sb.append(" - ");
            sb.append(mMembers.size());
            sb.append(" members => ");
            sb.append("{\n");
            // iterate over sorted keys for ease of debugging
            // this is slower, but it's for debugging only anyway.
            List<String> keys = new ArrayList<String>(mMembers.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                for (int i = -1; i < level; i++) {
                    sb.append("  "); // print at correct indentation
                }
                Type t = mMembers.get(key);
                if (t instanceof ObjectType) {
                    sb.append(((ObjectType) t).toString(level + 1));
                }
                else {
                    sb.append(t.toString());
                }
                sb.append("\n");
            }
            for (int i = 0; i < level; i++) {
                sb.append("  "); // print at correct indentation
            }
            sb.append("}");
        }
        return sb.toString();
    }

    /**
     * Get the list of members of this composite type.
     * 
     * @return The members.
     */
    public Collection<Type> getMembers()
    {
        return Collections.unmodifiableCollection(mMembers.values());
    }

    /**
     * Get an iterator for looping over all member (name,type)-pairs.
     * 
     * @return The iterator.
     */
    public Iterator<Map.Entry<String, Type>> getMembersIterator()
    {
        return mMembers.entrySet().iterator();
    }

    /**
     * Get the type of a named member of this composite type.
     * 
     * @param name The member name.
     * 
     * @return The member type.
     */
    public Type getTypeByName(String name)
    {
        return mMembers.get(name);
    }

    /**
     * Does this type contain key members?
     * 
     * @return True if it contains key members, false if not.
     */
    public boolean hasKeyMembers()
    {
        return mHasKeyMembers;
    }

    /**
     * Extract a data object based on an object T.
     * 
     * @param <T> The class of the Qeo data type.
     * @param t The object to be transformed
     * 
     * @return The constructed data object
     */
    @Override
    public <T> ObjectData toData(T t)
    {
        ObjectData data = null;
        try {
            data = new ObjectData(getId());
            if (t != null) {
                for (final Field field : t.getClass().getFields()) {
                    if (!ReflectionUtil.isQeoField(field)) {
                        continue;
                    }
                    Type memberType = getTypeByName(field.getName());
                    if (null == memberType) {
                        /* nested class? */
                        memberType = getTypeByName(field.getType().getName());
                    }
                    if (null == memberType) {
                        /* ignore unexpected field (might be subclass with extra fields of actual registered type) */
                        continue;
                    }
                    Data member = memberType.toData(field.get(t));
                    data.addMember(member);
                }
            }
            else {
                throw new IllegalArgumentException("The given struct is null or has a null field.");
            }
        }
        catch (final IllegalAccessException e) {
            LOG.log(Level.SEVERE, "Error creating data from class", e);
        }
        return data;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (mHasKeyMembers ? 1231 : 1237);
        result = prime * result + ((mMembers == null) ? 0 : mMembers.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {

        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ObjectType other = (ObjectType) obj;
        if (mHasKeyMembers != other.mHasKeyMembers) {
            return false;
        }
        if (mMembers == null) {
            if (other.mMembers != null) {
                return false;
            }
        }
        else if (!mMembers.equals(other.mMembers)) {
            return false;
        }
        return true;
    }

}
