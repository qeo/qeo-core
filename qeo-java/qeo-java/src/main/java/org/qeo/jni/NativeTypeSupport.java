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

package org.qeo.jni;

import java.util.HashMap;
import java.util.Map;

import org.qeo.exception.OutOfResourcesException;
import org.qeo.internal.common.Data;
import org.qeo.internal.common.EnumerationData;
import org.qeo.internal.common.PrimitiveArrayData;
import org.qeo.internal.common.PrimitiveData;
import org.qeo.internal.common.Type;
import org.qeo.internal.common.Type.MemberType;

/**
 * Wrapper class for native data accessors.
 */
public final class NativeTypeSupport
{

    /**
     * Hashtable containing inner classes to convert to and from java class types to and from DDS.
     */
    private static final Map<MemberType, NativeTypeAccessor> TYPES;

    static {
        /** Create hashmap and populate it with existing types **/
        TYPES = new HashMap<Type.MemberType, NativeTypeAccessor>();
        TYPES.put(Type.MemberType.TYPE_STRING, new SampleSupportString());
        TYPES.put(Type.MemberType.TYPE_SHORT, new SampleSupportShort());
        TYPES.put(Type.MemberType.TYPE_INT, new SampleSupportInteger());
        TYPES.put(Type.MemberType.TYPE_LONG, new SampleSupportLong());
        TYPES.put(Type.MemberType.TYPE_FLOAT, new SampleSupportFloat());
        TYPES.put(Type.MemberType.TYPE_BYTE, new SampleSupportByte());
        TYPES.put(Type.MemberType.TYPE_BOOLEAN, new SampleSupportBoolean());
        TYPES.put(Type.MemberType.TYPE_STRINGARRAY, new SampleSupportStringArray());
        TYPES.put(Type.MemberType.TYPE_SHORTARRAY, new SampleSupportShortArray());
        TYPES.put(Type.MemberType.TYPE_INTARRAY, new SampleSupportIntArray());
        TYPES.put(Type.MemberType.TYPE_LONGARRAY, new SampleSupportLongArray());
        TYPES.put(Type.MemberType.TYPE_FLOATARRAY, new SampleSupportFloatArray());
        TYPES.put(Type.MemberType.TYPE_BYTEARRAY, new SampleSupportByteArray());
        TYPES.put(Type.MemberType.TYPE_BOOLEANARRAY, new SampleSupportBooleanArray());
        TYPES.put(Type.MemberType.TYPE_ENUM, new SampleSupportEnumeration());
    }

    private NativeTypeSupport()
    {
        // Utility class
    }

    /**
     * Convert byte array to dds byte[].
     */
    private static class SampleSupportByteArray
        implements NativeTypeAccessor
    {
        @Override
        public Data get(long nativeData, int id)
        {
            final byte[] value = nativeGetByteSequence(nativeData);
            if (value == null) {
                throw new OutOfResourcesException();
            }
            return new PrimitiveArrayData(id, value);
        }

        @Override
        public void set(long nativeData, int id, Data data)
        {
            if (data == null) {
                data = new PrimitiveArrayData(id, new byte[0]);
            }
            Object obj = ((PrimitiveArrayData) data).getValue();
            if (obj == null) {
                obj = new byte[] {};
            }
            if (obj instanceof byte[]) {
                final byte[] value = (byte[]) obj;
                NativeError.checkError(nativeSetByteSequence(nativeData, value, value.length));
            }
            else {
                throw new IllegalStateException("Object to set has incorrect type");
            }
        }

        @Override
        public long getType()
        {
            return 0;
        }
    }

    /**
     * Convert byte class to dds byte.
     */
    private static class SampleSupportByte
        implements NativeTypeAccessor
    {

        @Override
        public Data get(long nativeData, int id)
        {
            final byte[] value = new byte[1];
            NativeError.checkError(nativeGetByte(nativeData, id, value));
            return new PrimitiveData(id, value[0]);
        }

        @Override
        public void set(long nativeData, int id, Data data)
        {
            if (data == null) {
                data = new PrimitiveData(id, (byte) 0x0);
            }
            final Object obj = ((PrimitiveData) data).getValue();
            if (obj instanceof Byte) {
                NativeError.checkError(nativeSetByte(nativeData, id, ((Byte) obj).byteValue()));
            }
            else {
                throw new IllegalStateException("Object to set has incorrect type");
            }
        }

        @Override
        public long getType()
        {
            final long nativeType = nativeNewByte();

            if (nativeType == 0) {
                throw new OutOfResourcesException("Failed to get byte type");
            }
            return nativeType;
        }
    }

    /**
     * Convert long array to dds long[].
     */
    private static class SampleSupportLongArray
        implements NativeTypeAccessor
    {
        @Override
        public Data get(long nativeData, int id)
        {
            final long[] value = nativeGetLongSequence(nativeData);
            if (value == null) {
                throw new OutOfResourcesException();
            }
            return new PrimitiveArrayData(id, value);
        }

        @Override
        public void set(long nativeData, int id, Data data)
        {

            if (data == null) {
                data = new PrimitiveArrayData(id, new long[0]);
            }
            Object obj = ((PrimitiveArrayData) data).getValue();
            if (obj == null) {
                obj = new long[] {};
            }
            if (obj instanceof long[]) {
                final long[] value = (long[]) obj;
                NativeError.checkError(nativeSetLongSequence(nativeData, value, value.length));
            }
            else {
                throw new IllegalStateException("Object to set has incorrect type");
            }
        }

        @Override
        public long getType()
        {
            return 0;
        }
    }

    /**
     * Convert Long class to dds int64.
     */
    private static class SampleSupportLong
        implements NativeTypeAccessor
    {

        @Override
        public Data get(long nativeData, int id)
        {
            final long[] value = new long[1];

            NativeError.checkError(nativeGetLong(nativeData, id, value));
            return new PrimitiveData(id, value[0]);
        }

        @Override
        public void set(long nativeData, int id, Data data)
        {
            if (data == null) {
                data = new PrimitiveData(id, (long) 0);
            }
            final Object obj = ((PrimitiveData) data).getValue();
            if (obj instanceof Long) {
                NativeError.checkError(nativeSetLong(nativeData, id, ((Long) obj).longValue()));
            }
            else {
                throw new IllegalStateException("Object to set has incorrect type");
            }
        }

        @Override
        public long getType()
        {
            final long nativeType = NativeTypeSupport.nativeNewLong();

            if (nativeType == 0) {
                throw new OutOfResourcesException("Failed to get int64 type");
            }
            return nativeType;
        }
    }

    /**
     * Convert float array to dds float[].
     */
    private static class SampleSupportFloatArray
        implements NativeTypeAccessor
    {
        @Override
        public Data get(long nativeData, int id)
        {
            final float[] value = nativeGetFloatSequence(nativeData);
            if (value == null) {
                throw new OutOfResourcesException();
            }
            return new PrimitiveArrayData(id, value);
        }

        @Override
        public void set(long nativeData, int id, Data data)
        {

            if (data == null) {
                data = new PrimitiveArrayData(id, new float[0]);
            }
            Object obj = ((PrimitiveArrayData) data).getValue();
            if (obj == null) {
                obj = new float[] {};
            }
            if (obj instanceof float[]) {
                final float[] value = (float[]) obj;
                NativeError.checkError(nativeSetFloatSequence(nativeData, value, value.length));
            }
            else {
                throw new IllegalStateException("Object to set has incorrect type");
            }
        }

        @Override
        public long getType()
        {
            return 0;
        }
    }

    /**
     * Convert Float class to dds float32.
     */
    private static class SampleSupportFloat
        implements NativeTypeAccessor
    {

        @Override
        public Data get(long nativeData, int id)
        {
            final float[] value = new float[1];

            NativeError.checkError(nativeGetFloat(nativeData, id, value));
            return new PrimitiveData(id, value[0]);
        }

        @Override
        public void set(long nativeData, int id, Data data)
        {
            if (data == null) {
                data = new PrimitiveData(id, (float) 0);
            }
            final Object obj = ((PrimitiveData) data).getValue();
            if (obj instanceof Float) {
                NativeError.checkError(nativeSetFloat(nativeData, id, ((Float) obj).floatValue()));
            }
            else {
                throw new IllegalStateException("Object to set has incorrect type");
            }
        }

        @Override
        public long getType()
        {
            final long nativeType = nativeNewFloat();

            if (nativeType == 0) {
                throw new OutOfResourcesException("Failed to get float32 type");
            }
            return nativeType;
        }
    }

    /**
     * Convert String array to dds String[].
     */
    private static class SampleSupportStringArray
        implements NativeTypeAccessor
    {
        @Override
        public Data get(long nativeData, int id)
        {
            final Object[] value = nativeGetStringSequence(nativeData);
            if (value == null) {
                throw new OutOfResourcesException();
            }
            return new PrimitiveArrayData(id, value);
        }

        @Override
        public void set(long nativeData, int id, Data data)
        {

            if (data == null) {
                data = new PrimitiveArrayData(id, new String[0]);
            }
            Object obj = ((PrimitiveArrayData) data).getValue();
            if (obj == null) {
                obj = new String[] {};
            }
            if (obj instanceof String[]) {
                final String[] value = (String[]) obj;
                NativeError.checkError(nativeSetStringSequence(nativeData, value, value.length));
            }
            else {
                throw new IllegalStateException("Object to set has incorrect type");
            }
        }

        @Override
        public long getType()
        {
            return 0;
        }
    }

    /**
     * Convert String class to dds string.
     */
    private static class SampleSupportString
        implements NativeTypeAccessor
    {

        @Override
        public Data get(long nativeData, int id)
        {
            final String[] value = new String[1];

            NativeError.checkError(nativeGetString(nativeData, id, value));
            return new PrimitiveData(id, value[0]);
        }

        @Override
        public void set(long nativeData, int id, Data data)
        {
            if (data == null) {
                data = new PrimitiveData(id, "");
            }
            Object obj = ((PrimitiveData) data).getValue();
            if (obj == null) {
                obj = "";
            }
            if (obj instanceof String) {
                NativeError.checkError(nativeSetString(nativeData, id, (String) obj));
            }
            else {
                throw new IllegalStateException("Object to set has incorrect type");
            }

        }

        @Override
        public long getType()
        {
            final long nativeType = nativeNewString(0);

            if (nativeType == 0) {
                throw new OutOfResourcesException("Failed to get string type");
            }
            return nativeType;
        }
    }

    /**
     * Convert int array to dds int[].
     */
    private static class SampleSupportIntArray
        implements NativeTypeAccessor
    {
        @Override
        public Data get(long nativeData, int id)
        {
            final int[] value = nativeGetIntegerSequence(nativeData);
            if (value == null) {
                throw new OutOfResourcesException();
            }
            return new PrimitiveArrayData(id, value);
        }

        @Override
        public void set(long nativeData, int id, Data data)
        {

            if (data == null) {
                data = new PrimitiveArrayData(id, new int[0]);
            }
            Object obj = ((PrimitiveArrayData) data).getValue();
            if (obj == null) {
                obj = new int[] {};
            }
            if (obj instanceof int[]) {
                final int[] value = (int[]) obj;
                NativeError.checkError(nativeSetIntegerSequence(nativeData, value, value.length));
            }
            else {
                throw new IllegalStateException("Object to set has incorrect type");
            }
        }

        @Override
        public long getType()
        {
            return 0;
        }
    }

    /**
     * Convert Integer class to dds int32.
     */
    private static class SampleSupportInteger
        implements NativeTypeAccessor
    {

        @Override
        public Data get(long nativeData, int id)
        {
            final int[] value = new int[1];

            NativeError.checkError(nativeGetInteger(nativeData, id, value));
            return new PrimitiveData(id, value[0]);
        }

        @Override
        public void set(long nativeData, int id, Data data)
        {
            if (data == null) {
                data = new PrimitiveData(id, 0);
            }
            final Object obj = ((PrimitiveData) data).getValue();
            if (obj instanceof Integer) {
                NativeError.checkError(nativeSetInteger(nativeData, id, ((Integer) obj).intValue()));
            }
            else {
                throw new IllegalStateException("Object to set has incorrect type");
            }
        }

        @Override
        public long getType()
        {
            final long nativeType = nativeNewInteger();

            if (nativeType == 0) {
                throw new OutOfResourcesException("Failed to get int32 type");
            }
            return nativeType;
        }
    }

    /**
     * Convert short array to dds short[].
     */
    private static class SampleSupportShortArray
        implements NativeTypeAccessor
    {
        @Override
        public Data get(long nativeData, int id)
        {
            final short[] value = nativeGetShortSequence(nativeData);
            if (value == null) {
                throw new OutOfResourcesException();
            }
            return new PrimitiveArrayData(id, value);
        }

        @Override
        public void set(long nativeData, int id, Data data)
        {

            if (data == null) {
                data = new PrimitiveArrayData(id, new short[0]);
            }
            Object obj = ((PrimitiveArrayData) data).getValue();
            if (obj == null) {
                obj = new short[] {};
            }
            if (obj instanceof short[]) {
                final short[] value = (short[]) obj;
                NativeError.checkError(nativeSetShortSequence(nativeData, value, value.length));
            }
            else {
                throw new IllegalStateException("Object to set has incorrect type");
            }
        }

        @Override
        public long getType()
        {
            return 0;
        }
    }

    /**
     * Convert Integer class to dds int16.
     */
    private static class SampleSupportShort
        implements NativeTypeAccessor
    {

        @Override
        public Data get(long nativeData, int id)
        {
            final short[] value = new short[1];

            NativeError.checkError(nativeGetShort(nativeData, id, value));
            return new PrimitiveData(id, value[0]);
        }

        @Override
        public void set(long nativeData, int id, Data data)
        {
            if (data == null) {
                data = new PrimitiveData(id, 0);
            }
            final Object obj = ((PrimitiveData) data).getValue();
            if (obj instanceof Short) {
                NativeError.checkError(nativeSetShort(nativeData, id, ((Short) obj).shortValue()));
            }
            else {
                throw new IllegalStateException("Object to set has incorrect type");
            }
        }

        @Override
        public long getType()
        {
            final long nativeType = nativeNewShort();

            if (nativeType == 0) {
                throw new OutOfResourcesException("Failed to get int16 type");
            }
            return nativeType;
        }
    }

    /**
     * Convert boolean array to dds boolean[].
     */
    private static class SampleSupportBooleanArray
        implements NativeTypeAccessor
    {
        @Override
        public Data get(long nativeData, int id)
        {
            final boolean[] value = nativeGetBooleanSequence(nativeData);
            if (value == null) {
                throw new OutOfResourcesException();
            }
            return new PrimitiveArrayData(id, value);
        }

        @Override
        public void set(long nativeData, int id, Data data)
        {

            if (data == null) {
                data = new PrimitiveArrayData(id, new boolean[0]);
            }
            Object obj = ((PrimitiveArrayData) data).getValue();
            if (obj == null) {
                obj = new boolean[] {};
            }
            if (obj instanceof boolean[]) {
                final boolean[] value = (boolean[]) obj;
                NativeError.checkError(nativeSetBooleanSequence(nativeData, value, value.length));
            }
            else {
                throw new IllegalStateException("Object to set has incorrect type");
            }
        }

        @Override
        public long getType()
        {
            return 0;
        }
    }

    /**
     * Convert Boolean class to dds boolean.
     */
    private static class SampleSupportBoolean
        implements NativeTypeAccessor
    {

        @Override
        public Data get(long nativeData, int id)
        {
            final boolean[] value = new boolean[1];

            NativeError.checkError(nativeGetBoolean(nativeData, id, value));
            return new PrimitiveData(id, value[0]);
        }

        @Override
        public void set(long nativeData, int id, Data data)
        {
            if (data == null) {
                data = new PrimitiveData(id, false);
            }
            final Object obj = ((PrimitiveData) data).getValue();
            if (obj instanceof Boolean) {
                final boolean b = ((Boolean) obj).booleanValue();
                NativeError.checkError(nativeSetBoolean(nativeData, id, b));
            }
            else {
                throw new IllegalStateException("Object to set has incorrect type");
            }
        }

        @Override
        public long getType()
        {
            final long nativeType = nativeNewBoolean();

            if (nativeType == 0) {
                throw new OutOfResourcesException("Failed to get boolean type");
            }
            return nativeType;
        }
    }

    /**
     * Convert Enumeration class to dds int32.
     */
    private static class SampleSupportEnumeration
        implements NativeTypeAccessor
    {

        @Override
        public Data get(long nativeData, int id)
        {
            final int[] value = new int[1];

            NativeError.checkError(nativeGetInteger(nativeData, id, value));
            return new EnumerationData(id, value[0]);
        }

        @Override
        public void set(long nativeData, int id, Data data)
        {
            if (data == null) {
                data = new EnumerationData(id, 0);
            }
            NativeError.checkError(nativeSetInteger(nativeData, id, ((EnumerationData) data).getValue()));
        }

        @Override
        public long getType()
        {
            return 0; /* not supported */
        }
    }

    /**
     * Get an accessor for a certain type.
     * 
     * @param membertype The type for which to get an accessor
     * 
     * @return The accessor
     */
    public static NativeTypeAccessor getAccessor(Type.MemberType membertype)
    {
        return TYPES.get(membertype);
    }

    /**
     * The next block contains the native functions needed by the NativeType class.
     */

    private static native long nativeNewString(int size);

    private static native long nativeNewByte();

    private static native long nativeNewShort();

    private static native long nativeNewInteger();

    private static native long nativeNewLong();

    private static native long nativeNewFloat();

    private static native long nativeNewBoolean();

    /**
     * Create a new sequence type on native qeo-c.
     * 
     * @param elementType the type of the sequences elements
     * @return the sequence reference
     */
    public static native long nativeNewSequence(long elementType);

    /**
     * Create a new enumeration type on native qeo-c.
     * 
     * @param name name of the enumeration
     * @param constants array of enumeration constants
     * @return the enumeration reference
     */
    public static native long nativeNewEnumeration(String name, String[] constants);

    private static native int nativeSetString(long nativeData, int id, String data);

    private static native int nativeSetByte(long nativeData, int id, byte data);

    private static native int nativeSetShort(long nativeData, int id, short data);

    private static native int nativeSetInteger(long nativeData, int id, int data);

    private static native int nativeSetLong(long nativeData, int id, long data);

    private static native int nativeSetFloat(long nativeData, int id, float data);

    private static native int nativeSetBoolean(long nativeData, int id, boolean data);

    private static native int nativeSetStringSequence(long nativeData, Object[] data, int length);

    private static native int nativeSetByteSequence(long nativeData, byte[] data, int length);

    private static native int nativeSetShortSequence(long nativeData, short[] data, int length);

    private static native int nativeSetIntegerSequence(long nativeData, int[] data, int length);

    private static native int nativeSetLongSequence(long nativeData, long[] data, int length);

    private static native int nativeSetFloatSequence(long nativeData, float[] data, int length);

    private static native int nativeSetBooleanSequence(long nativeData, boolean[] data, int length);

    private static native int nativeGetString(long nativeData, int id, String[] data);

    private static native int nativeGetByte(long nativeData, int id, byte[] data);

    private static native int nativeGetShort(long nativeData, int id, short[] data);

    private static native int nativeGetInteger(long nativeData, int id, int[] data);

    private static native int nativeGetLong(long nativeData, int id, long[] data);

    private static native int nativeGetFloat(long nativeData, int id, float[] data);

    private static native int nativeGetBoolean(long nativeData, int id, boolean[] data);

    private static native Object[] nativeGetStringSequence(long nativeData);

    private static native byte[] nativeGetByteSequence(long nativeData);

    private static native short[] nativeGetShortSequence(long nativeData);

    private static native int[] nativeGetIntegerSequence(long nativeData);

    private static native long[] nativeGetLongSequence(long nativeData);

    private static native float[] nativeGetFloatSequence(long nativeData);

    private static native boolean[] nativeGetBooleanSequence(long nativeData);

}
