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

package com.technicolor.qeo.codegen.type.json;

import java.util.ArrayList;
import java.util.List;

import com.technicolor.qeo.codegen.type.Container;
import com.technicolor.qeo.codegen.type.qdm.QDM;

/**
 * Class to represent a JSON struct.
 */
public class JsonStruct
    extends Container<JsonMember>
{
    private final String mTopic;
    private String mBehavior;
    private final List<JsonMember> mBasicMembers;
    private final List<JsonMember> mStructMembers;
    private final List<JsonMember> mArrayMembers;

    /**
     * Generate an instance.
     * 
     * @param topicName The JSON topic name (module.structName).
     * @param structName The name of the struct.
     */
    public JsonStruct(String topicName, String structName)
    {
        super(structName);
        mTopic = topicName;
        mBehavior = null;
        mBasicMembers = new ArrayList<JsonMember>();
        mStructMembers = new ArrayList<JsonMember>();
        mArrayMembers = new ArrayList<JsonMember>();

    }

    /**
     * Get the topic name (module.structName).
     * 
     * @return The name.
     */
    public String getTopic()
    {
        return mTopic;
    }

    /**
     * Add a JSON member.
     * 
     * @param member The member to be added.
     */
    @Override
    public void addMember(JsonMember member)
    {
        if (member.getType().equals(QDM.STRING_NON_BASIC)) {
            mStructMembers.add(member);
        }
        else {
            if (member.getType().equals("array")) {
                mArrayMembers.add(member);
            }
            else {
                mBasicMembers.add(member);
            }
        }
    }

    /**
     * Get a list of JSON members.
     * 
     * @return The list of members.
     */
    @Override
    public Iterable<JsonMember> getMembers()
    {
        List<JsonMember> allMembers = mBasicMembers;
        allMembers.addAll(mStructMembers);
        allMembers.addAll(mArrayMembers);

        return allMembers;
    }

    /**
     * Get the behavior.
     * 
     * @return state or event.
     */
    public String getBehavior()
    {
        return mBehavior;
    }

    /**
     * Set the behavior, if the struct has keyed members it should be set to 'state'.
     * 
     * @param behavior 'state' or 'event'.
     */
    public void setBehavior(String behavior)
    {
        this.mBehavior = behavior;
    }
}
