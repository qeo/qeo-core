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

package org.qeo.sample.simplechat;

/**
 * A simple chat message.
 */
public class ChatMessage
{
    /**
     * The user sending the message.
     */
    public String from;

    /**
     * The message.
     */
    public String message;

    /**
     * A default constructor is <b>required</b> in Qeo.
     * 
     * It is used to create a new instance of this object before filling in the variables that originate from Qeo.
     **/
    public ChatMessage()
    {
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }
        if ((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }
        final ChatMessage myObj = (ChatMessage) obj;
        if (!from.equals(myObj.from)) {
            return false;
        }
        if (!message.equals(myObj.message)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((from == null) ? 0 : from.hashCode());
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        return result;
    }
}
