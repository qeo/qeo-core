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

import android.view.KeyEvent;

import com.android.uiautomator.core.UiDevice;

/**
 * Helper class to send keyboard events.
 */
public class KeyEventHelper
{
    private final UiDevice mDev;

    public KeyEventHelper(UiDevice dev)
    {
        mDev = dev;
    }

    public void pressTab()
    {
        mDev.pressKeyCode(KeyEvent.KEYCODE_TAB);
    }

    public void pressEnter()
    {
        mDev.pressKeyCode(KeyEvent.KEYCODE_ENTER);
    }

    public void sendString(String msg)
    {
        for (int i = 0; i < msg.length(); ++i) {
            char c = msg.charAt(i);
            if (c >= 'a' && c <= 'z') {
                int offset = c - 'a';
                mDev.pressKeyCode(KeyEvent.KEYCODE_A + offset);
            }
            else if (c >= '0' && c <= '9') {
                int offset = c - '0';
                mDev.pressKeyCode(KeyEvent.KEYCODE_0 + offset);
            }
            else if (c == '.') {
                mDev.pressKeyCode(KeyEvent.KEYCODE_PERIOD);
            }
            else if (c == '-') {
                mDev.pressKeyCode(KeyEvent.KEYCODE_MINUS);
            }

            else if (c == ':') {
                mDev.pressKeyCode(KeyEvent.KEYCODE_SEMICOLON, 1);
            }
            else if (c == '/') {
                mDev.pressKeyCode(KeyEvent.KEYCODE_SLASH);
            }
            else if (c == '@') {
                mDev.pressKeyCode(KeyEvent.KEYCODE_AT);
            }
            else {
                throw new IllegalArgumentException("Unsupported char: '" + c + "'");
            }
        }
    }
}
