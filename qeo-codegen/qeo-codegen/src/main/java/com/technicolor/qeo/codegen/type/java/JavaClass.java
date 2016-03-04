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

package com.technicolor.qeo.codegen.type.java;

import java.util.ArrayList;
import java.util.Locale;

import com.technicolor.qeo.codegen.type.Container;

/**
 * Class to represent a java class.
 */
public class JavaClass
    extends Container<JavaMember>
{
    private final String mPackageName;
    private String mBehavior;
    private boolean mHasKeys;
    private boolean mHasArrays;

    /**
     * Generate an instance.
     * 
     * @param packageName The java package name.
     * @param className The java class name.
     */
    public JavaClass(String packageName, String className)
    {
        super(className);
        mPackageName = packageName;
        mMembers = new ArrayList<JavaMember>();
        mHasKeys = false;
        mHasArrays = false;
        mBehavior = null;
    }

    /**
     * Get the package name.
     * 
     * @return The name.
     */
    public String getPackageName()
    {
        return mPackageName;
    }

    /**
     * Check if the class contains arrays that are relevant for equals and hashCode functions.
     * 
     * @return True if there are keys.
     */
    public boolean isHasRelevantArrays()
    {
        return mHasArrays;
    }

    /**
     * Set if the class has arrays that are relevant for equals and hashCode functions.
     * 
     * @param hasArrays true if the class has arrays.
     */
    public void setHasRelevantArrays(boolean hasArrays)
    {
        this.mHasArrays = hasArrays;
    }

    /**
     * Check if the class contains keyed members.
     * 
     * @return True if there are keys.
     */
    public boolean isHasKeys()
    {
        return mHasKeys;
    }

    /**
     * Set if the class has keyed members.
     * 
     * @param hasKeys true if the class has keys.
     */
    public void setHasKeys(boolean hasKeys)
    {
        this.mHasKeys = hasKeys;
    }

    /**
     * Set the behavior of the class.
     * 
     * @param behavior - state or event
     */
    public void setBehavior(String behavior)
    {
        if (behavior != null) {
            mBehavior = behavior.toUpperCase(Locale.ENGLISH);
        }
    }

    /**
     * Get the behavior of the class.
     * 
     * @return behavior
     */
    public String getBehavior()
    {
        return mBehavior;

    }
}
