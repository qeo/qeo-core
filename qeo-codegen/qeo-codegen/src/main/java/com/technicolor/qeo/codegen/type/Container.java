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

import java.util.List;

/**
 * Class to represent an abstract container of members.
 * 
 * @param <T> Container element type
 */
public abstract class Container<T extends ContainerMember>
{
    private String mDoc;

    /**
     * Container name.
     */
    protected String mName;

    /**
     * List of container members.
     */
    protected List<T> mMembers;

    /**
     * Father container name.
     */
    protected String mFullName;

    /**
     * Create a container.
     * 
     * @param name The container name.
     */
    public Container(String name)
    {
        mName = name;
    }

    /**
     * Get the documentation of the container.
     * 
     * @return doc the contents. Can be null.
     */
    public String getDoc()
    {
        return mDoc;
    }

    /**
     * Set the documentation of the container.
     * 
     * @param doc the contents, can be null.
     */
    public void setDoc(String doc)
    {
        mDoc = doc;
    }

    /**
     * Get the name of the container.
     * 
     * @return The name of the container
     */
    public final String getName()
    {
        return mName;
    }

    /**
     * Get the full name of the container (includes namespacing if needed).
     * 
     * @return The namespace
     */
    public String getFullName()
    {
        return mFullName;
    }

    /**
     * Set the full name of the container (includes namespacing if needed).
     * 
     * @param fullName The namespace
     */
    public void setFullName(String fullName)
    {
        mFullName = fullName;
    }

    /**
     * Add a member.
     * 
     * @param member The member to be added.
     */
    public void addMember(T member)
    {
        mMembers.add(member);
    }

    /**
     * Get a list of members.
     * 
     * @return List of members. Type to be defined by the subclass.
     */
    public Iterable<T> getMembers()
    {
        return mMembers;
    }

    /**
     * Get the size of the container.
     * 
     * @return The size of the container.
     */
    public int size()
    {
        return (null != mMembers) ? mMembers.size() : 0;
    }
}
