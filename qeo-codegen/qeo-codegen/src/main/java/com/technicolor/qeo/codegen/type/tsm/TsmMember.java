/*
 * Copyright (c) 2014 - Qeo LLC
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

package com.technicolor.qeo.codegen.type.tsm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

import com.technicolor.qeo.codegen.type.Member;

/**
 * class to represent an entry line in a TSM for a c/ objective c source file.
 */
public class TsmMember
    extends Member
{
    private String mFullName;
    private String mSize;
    private int mNelem;
    private String mOffset;
    private final SortedSet<String> mFlags;
    private String mTsm;
    private final List<TsmMember> mSeq;
    private String mLabel;

    /**
     * Create a CTsmMember object.
     * 
     * @param name Member name
     */
    public TsmMember(String name)
    {
        super(name);
        mFlags = new TreeSet<String>();
        mSeq = new ArrayList<TsmMember>();
    }

    /**
     * Get the name of the struct.
     * 
     * @return String value, name of the struct, composition between the module_structName
     */
    @Override
    public String getFullName()
    {
        return mFullName;
    }

    /**
     * Set the name of the struct.
     * 
     * @param fullName - Composition between the module_structName.
     */
    public void setFullName(String fullName)
    {
        mFullName = fullName;
    }

    /**
     * Get the size field.
     * 
     * @return The size field if defined. null if not defined.
     */
    public String getSize()
    {
        return mSize;
    }

    /**
     * Set the size field.
     * 
     * @param size the size value. can be null.
     */
    public void setSize(String size)
    {
        this.mSize = size;
    }

    /**
     * Get the number of elements.
     * 
     * @return The number of elements. Can be null.
     */
    public int getNelem()
    {
        return mNelem;
    }

    /**
     * Set the number of elements.
     * 
     * @param nelem The number of elements, can be null.
     */
    public void setNelem(int nelem)
    {
        this.mNelem = nelem;
    }

    /**
     * get the offset value.
     * 
     * @return The offset.
     */
    public String getOffset()
    {
        return mOffset;
    }

    /**
     * Set the offset.
     * 
     * @param offset The offset value.
     */
    public void setOffset(String offset)
    {
        this.mOffset = offset;
    }

    /**
     * Get the flags.
     * 
     * @return A string with all the flags concatenated by '|'
     */
    public String getFlags()
    {
        return StringUtils.join(mFlags, "|");
    }

    /**
     * Add a flag.
     * 
     * @param flag The flag value.
     */
    public void addFlag(String flag)
    {
        mFlags.add(flag);
    }

    /**
     * get the tsm value.
     * 
     * @return The tsm.
     */
    public String getTsm()
    {
        return mTsm;
    }

    /**
     * Set the tsm value.
     * 
     * @param tsm The tsm value
     */
    public void setTsm(String tsm)
    {
        this.mTsm = tsm;
    }

    /**
     * Add a CTsmMember to the CTsmMember.
     * 
     * @param seqElemType The element type of the sequence
     */
    public void addSeqElemType(TsmMember seqElemType)
    {
        mSeq.add(seqElemType);
    }

    /**
     * Get element type of the sequence.
     * 
     * @return A string with all the flags concatenated by '|'
     */
    public List<TsmMember> getSeqElemType()
    {
        return mSeq;
    }

    /**
     * Get the label.
     * 
     * @return string label
     */
    public String getLabel()
    {
        return this.mLabel;
    }

    /**
     * Set the label.
     * 
     * @param label string
     */
    public void setLabel(String label)
    {
        this.mLabel = label.toUpperCase(Locale.ENGLISH);

    }

}
