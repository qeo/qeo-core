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

import java.io.UnsupportedEncodingException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Helper class used for utility functions inside this namespace.
 */
public final class Util
{
    private Util()
    {
        // Utility class
    }

    /**
     * Calculate the DDS ID based on a string. The ID is derived from this name in the following way:
     * <ol>
     * <li>Calculate a 32 bit CRC based on the name
     * <li>Take the 28 least significant bits from it
     * <li>If the result is 0 or 1, add 2 to it because 0 and 1 have a special meaning in DDS
     * </ol>
     * 
     * @param name the string for which to calculate the ID
     * @return the ID as a 28-bit integer
     */
    public static int calculateID(String name)
    {
        /* First calculate the 32 bit crc */
        byte[] bytes;
        try {
            bytes = name.getBytes("UTF8");
        }
        catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException("Can't calculate ID", e);
        }
        final Checksum checksum = new CRC32();
        checksum.update(bytes, 0, bytes.length);
        long value = checksum.getValue();

        /* now take the 28 least significant bits */
        value = value & 0x0FFFFFFF;

        /* now change 0 and 1 to something else */
        if (value < 2) {
            value += 2;
        }
        return (int) value;
    }
}
