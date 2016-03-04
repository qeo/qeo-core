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

package org.qeo.uitest.common;

import android.util.Log;

/**
 * 
 */
public class LoggerUI
{
    private final String mTag;
    private Level mLevel;

    private LoggerUI(String tag)
    {
        mTag = tag;
        mLevel = Level.INFO;
    }

    public static LoggerUI getLogger(Class<?> clazz)
    {
        return new LoggerUI("UILOG-" + clazz.getSimpleName());
    }

    public void setLevel(Level level)
    {
        mLevel = level;
    }

    public void w(String msg)
    {
        if (mLevel.ordinal() <= Level.WARNING.ordinal()) {
            Log.w(mTag, msg);
        }
    }

    public void w(String msg, Exception ex)
    {
        if (mLevel.ordinal() <= Level.WARNING.ordinal()) {
            Log.w(mTag, msg, ex);
        }
    }

    public void i(String msg)
    {
        if (mLevel.ordinal() <= Level.INFO.ordinal()) {
            Log.i(mTag, msg);
        }
    }

    public void i(String msg, Exception ex)
    {
        if (mLevel.ordinal() <= Level.INFO.ordinal()) {
            Log.i(mTag, msg, ex);
        }
    }

    public void d(String msg)
    {
        if (mLevel.ordinal() <= Level.DEBUG.ordinal()) {
            Log.d(mTag, msg);
        }
    }

    public void d(String msg, Exception ex)
    {
        if (mLevel.ordinal() <= Level.DEBUG.ordinal()) {
            Log.d(mTag, msg, ex);
        }
    }

    public static enum Level {
        ALL, DEBUG, INFO, WARNING;
    }
}
