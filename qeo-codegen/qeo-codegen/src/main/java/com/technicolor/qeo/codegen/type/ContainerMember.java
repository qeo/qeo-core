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
 * Class to represent an abstract container member type.
 */
public abstract class ContainerMember
{
    private String mDoc;
    private final String mName;

    /**
     * Create a container member.
     * 
     * @param name The member name.
     */
    public ContainerMember(String name)
    {
        mName = name;
    }

    /**
     * Get the member name.
     * 
     * @return The member name.
     */
    public final String getName()
    {
        return mName;
    }

    /**
     * Get the full name of the member (includes namespacing if needed).
     * 
     * @return The name of the member
     */
    public String getFullName()
    {
        return getName();
    }

    /**
     * Get the documentation for the member.
     * 
     * @return the documentation if set, null otherwise.
     */
    public String getDoc()
    {
        return mDoc;
    }

    /**
     * Add documentation at the beginning of the member.
     * 
     * @param doc The documentation to be added.
     */
    public void setDocAtTop(String doc)
    {
        if (doc != null) {
            if (mDoc != null) {
                this.mDoc = doc + "\n   * " + mDoc;
            }
            else {
                this.mDoc = doc;
            }
        }
    }

    /**
     * Add the documentation of the member. It concatenate it to the existing one if there is already any documentation
     * on the member.
     * 
     * @param doc The documentation to be added.
     */
    public void setDoc(String doc)
    {
        if (doc != null) {

            if (mDoc != null) {
                this.mDoc = mDoc + "\n   * " + doc;
            }
            else {
                this.mDoc = doc;
            }
        }
    }

}
