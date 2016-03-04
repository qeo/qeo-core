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

package org.qeo.jni;

import java.util.logging.Logger;

import org.qeo.exception.OutOfResourcesException;
import org.qeo.internal.common.ArrayData;
import org.qeo.internal.common.ArrayType;
import org.qeo.internal.common.Data;
import org.qeo.internal.common.EnumerationData;
import org.qeo.internal.common.EnumerationType;
import org.qeo.internal.common.ObjectData;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.common.PrimitiveArrayData;
import org.qeo.internal.common.PrimitiveArrayType;
import org.qeo.internal.common.Type;

/**
 * Wrapper class around a native data sample.
 */
public class NativeData
{
    private long mNativeData = 0;
    private static final Logger LOG = Logger.getLogger(NativeQeo.TAG);

    /**
     * Enum representing the status of the data (used in callbacks from DDS).
     * 
     * Remark: At the moment this needs to be lined up with the Qeo-C enum qeo_data_status_t
     */
    public enum Status {
        /** Notification of change, no actual data is present. */
        NOTIFY,
        /** Notification of data, data is present. */
        DATA,
        /** Notification of no more data, no actual data is present. */
        NO_MORE_DATA,
        /** Notification of removal, data is present. */
        REMOVE,
        /** An error occurred, no actual data is present. */
        ERROR
    };

    private NativeData()
    {
    }

    /**
     * Create a new data sample wrapper given a native data pointer.
     * 
     * @param nativeData The native data pointer
     */
    public NativeData(long nativeData)
    {
        this();
        this.mNativeData = nativeData;
    }

    /**
     * Create a new data sample wrapper using an existing reader.
     * 
     * @param reader The reader for which to create the data
     */
    public NativeData(NativeReader reader)
    {
        this();
        mNativeData = nativeNewReaderData(reader.getNativeReader());
        if (mNativeData == 0) {
            throw new OutOfResourcesException();
        }
        LOG.fine("NativeData (" + mNativeData + ") created for reader (" + reader.getNativeReader() + ")");
    }

    /**
     * Create a new native data sample based on the provided data.
     * 
     * @param writer The writer for which to create the data
     * @param data The actual data to be converted to native
     * @param type The type of this data (has to correspond to the writer)
     */
    public NativeData(NativeWriter writer, ObjectData data, ObjectType type)
    {
        this();
        if (type.getMembers().isEmpty()) {
            throw new IllegalStateException("Type does not contain public fields");
        }
        mNativeData = nativeNewWriterData(writer.getNativeWriter());
        addNativeMembers(data, type, mNativeData);
        LOG.fine("NativeData (" + mNativeData + ") created for writer (" + writer.getNativeWriter() + ")");
        if (mNativeData == 0) {
            throw new OutOfResourcesException();
        }
    }

    private void addNativeArrayElements(ArrayData arrayData, Type type, long nativeMember)
    {
        if (type instanceof EnumerationType) {
            /* Array of enums handled separately because it gets treated as a int32 sequence. */
            int[] enumArray = new int[arrayData.size()];
            int i = 0;

            for (final Data arrayElement : arrayData.getElements()) {
                enumArray[i++] = ((EnumerationData) arrayElement).getValue();
            }
            NativeTypeSupport.getAccessor(Type.MemberType.TYPE_INTARRAY).set(nativeMember, -1,
                new PrimitiveArrayData(-1, enumArray));
        }
        else {
            int index = 0;
            final long nativeArray = nativeGetSequence(nativeMember, arrayData.size());
            if (0 == nativeArray) {
                throw new OutOfResourcesException();
            }

            if (type instanceof ObjectType) {
                for (final Data arrayElement : arrayData.getElements()) {
                    if (arrayElement != null) {
                        final long nativeArrayElement = nativeGetSequenceElement(nativeArray, index);
                        addNativeMembers((ObjectData) arrayElement, (ObjectType) type, nativeArrayElement);
                    }
                    index++;
                }
            }
            else if (type instanceof PrimitiveArrayType) {
                for (final Data arrayElement : arrayData.getElements()) {
                    if (arrayElement != null) {
                        final long nativeArrayElement = nativeGetSequenceElement(nativeArray, index);
                        NativeTypeSupport.getAccessor(((PrimitiveArrayType) type).getType()).set(nativeArrayElement,
                            -1, arrayElement);
                    }
                    index++;
                }
            }
            else if (type instanceof ArrayType) {
                for (final Data arrayElement : arrayData.getElements()) {
                    if (arrayElement != null) {
                        final long nativeArrayElement = nativeGetSequenceElement(nativeArray, index);
                        addNativeArrayElements((ArrayData) arrayElement, ((ArrayType) type).getElementType(),
                            nativeArrayElement);
                    }
                    index++;
                }
            }
            else {
                throw new IllegalArgumentException();
            }

            NativeError.checkError(nativeSetSequence(nativeMember, nativeArray));
            nativeDeleteSequence(nativeMember, nativeArray);
        }
    }

    private void throwWriteMemberProblem(ObjectType type, Type member, Throwable cause)
    {
        throw new IllegalStateException("Problem writing \"" + type.getName() + "\", the member \"" + member.getName()
            + "\" is not defined", cause);
    }

    private void addNativeMembers(ObjectData data, ObjectType type, long nativeData)
    {
        for (final Type t : type.getMembers()) {
            final Data tdata = data.getContainedData(t.getId());
            if (tdata != null) {
                if (t instanceof ObjectType) {
                    final long memberData = nativeGetMemberData(nativeData, t.getId());
                    if (0 == memberData) {
                        throwWriteMemberProblem(type, t, null);
                    }
                    addNativeMembers((ObjectData) tdata, (ObjectType) t, memberData);
                    NativeError.checkError(nativeSetMemberData(nativeData, t.getId(), memberData));
                    nativeDeleteData(memberData);
                }
                else if (t instanceof PrimitiveArrayType) {
                    final long memberData = nativeGetMemberData(nativeData, t.getId());
                    if (0 == memberData) {
                        throwWriteMemberProblem(type, t, null);
                    }
                    NativeTypeSupport.getAccessor(((PrimitiveArrayType) t).getType()).set(memberData, t.getId(), tdata);
                    NativeError.checkError(nativeSetMemberData(nativeData, t.getId(), memberData));
                    nativeDeleteData(memberData);
                }
                else if (t instanceof ArrayType) {
                    final long memberData = nativeGetMemberData(nativeData, t.getId());
                    if (0 == memberData) {
                        throwWriteMemberProblem(type, t, null);
                    }
                    addNativeArrayElements((ArrayData) tdata, ((ArrayType) t).getElementType(), memberData);
                    NativeError.checkError(nativeSetMemberData(nativeData, t.getId(), memberData));
                    nativeDeleteData(memberData);
                }
                else {
                    /* Primitive or enumeration types */
                    NativeTypeSupport.getAccessor(t.getType()).set(nativeData, t.getId(), tdata);
                }
            }
        }
    }

    /**
     * Release any native resources associated with the data sample.
     */
    public void close()
    {
        nativeDeleteData(mNativeData);
        LOG.fine("NativeData (" + mNativeData + ") closed");
    }

    private ArrayData getArrayData(Type type, int id, long nativeMember)
    {
        ArrayData arrayData = new ArrayData(id);

        if (type instanceof EnumerationType) {
            /* Array of enums handled separately because it gets treated as a int32 sequence. */
            PrimitiveArrayData enumArray =
                (PrimitiveArrayData) NativeTypeSupport.getAccessor(Type.MemberType.TYPE_INTARRAY).get(nativeMember, -1);
            for (final int elem : (int[]) enumArray.getValue()) {
                arrayData.addElement(new EnumerationData(-1, elem));
            }
        }
        else {
            final long nativeArray = nativeGetSequence(nativeMember, 0);
            if (0 == nativeArray) {
                throw new OutOfResourcesException();
            }
            if (type instanceof ObjectType) {
                for (int i = 0; i < nativeGetSequenceSize(nativeArray); i++) {
                    Data arrayElement = null;
                    final long nativeArrayElement = nativeGetSequenceElement(nativeArray, i);
                    arrayElement = getData((ObjectType) type, type.getId(), nativeArrayElement);
                    arrayData.addElement(arrayElement);
                }
            }
            else if (type instanceof PrimitiveArrayType) {
                for (int i = 0; i < nativeGetSequenceSize(nativeArray); i++) {
                    Data arrayElement = null;
                    final long nativeArrayElement = nativeGetSequenceElement(nativeArray, i);
                    arrayElement =
                        NativeTypeSupport.getAccessor(((PrimitiveArrayType) type).getType())
                            .get(nativeArrayElement, -1);
                    arrayData.addElement(arrayElement);
                }
            }
            else if (type instanceof ArrayType) {
                for (int i = 0; i < nativeGetSequenceSize(nativeArray); i++) {
                    Data arrayElement = null;
                    final long nativeArrayElement = nativeGetSequenceElement(nativeArray, i);
                    arrayElement = getArrayData(((ArrayType) type).getElementType(), type.getId(), nativeArrayElement);
                    arrayData.addElement(arrayElement);
                }
            }
            else {
                throw new IllegalArgumentException();
            }
            nativeDeleteSequence(nativeMember, nativeArray);
        }
        return arrayData;
    }

    private void throwReadMemberProblem(ObjectType type, Type member, Throwable cause)
    {
        throw new IllegalStateException("Problem reading \"" + type.getName() + "\", the member \"" + member.getName()
            + "\" is not available", cause);
    }

    private ObjectData getData(ObjectType type, int id, long nativeData)
    {
        Status status = getStatus();
        LOG.fine("Reading native data for  " + type.getName() + " status: " + status);
        if ((status == Status.DATA) || (status == Status.REMOVE)) {
            ObjectData data = new ObjectData(id);

            for (final Type t : type.getMembers()) {
                LOG.fine("Reading native member " + t.getName());
                Data member = null;
                if (t instanceof ObjectType) {
                    final long nativeMember = nativeGetMemberData(nativeData, t.getId());
                    if (0 == nativeMember) {
                        throwReadMemberProblem(type, t, null);
                    }
                    member = getData((ObjectType) t, t.getId(), nativeMember);
                    nativeDeleteData(nativeMember);
                }
                else if (t instanceof PrimitiveArrayType) {
                    final long nativeMember = nativeGetMemberData(nativeData, t.getId());
                    if (0 == nativeMember) {
                        throwReadMemberProblem(type, t, null);
                    }
                    member =
                        NativeTypeSupport.getAccessor(((PrimitiveArrayType) t).getType()).get(nativeMember, t.getId());
                    nativeDeleteData(nativeMember);
                }
                else if (t instanceof ArrayType) {
                    final long nativeMember = nativeGetMemberData(nativeData, t.getId());
                    if (0 == nativeMember) {
                        throwReadMemberProblem(type, t, null);
                    }
                    member = getArrayData(((ArrayType) t).getElementType(), t.getId(), nativeMember);
                    nativeDeleteData(nativeMember);
                }
                else {
                    try {
                        member = NativeTypeSupport.getAccessor(t.getType()).get(nativeData, t.getId());
                    }
                    catch (RuntimeException ex) {
                        // a runtimeexception can be thrown if something goes wrong in native.
                        // unfortunately the error message does not contain any information on what is wrong
                        // hence re-throw the exception with a decent message
                        throwReadMemberProblem(type, t, ex);
                    }
                }
                data.addMember(member);
            }

            return data;
        }
        return null;
    }

    /**
     * Convert from native data to Java.
     * 
     * @param type The type of this data
     * @return The retrieved data.
     */
    public ObjectData getData(ObjectType type)
    {
        return getData(type, 0, mNativeData);
    }

    /**
     * Get the native data sample pointer.
     * 
     * @return The native data sample pointer.
     */
    public long getNativeData()
    {
        return mNativeData;
    }

    /**
     * Get the DDS instance handle associated with the data.
     * 
     * @return The DDS instance handle.
     */
    public int getInstanceHandle()
    {
        return nativeGetInstanceHandle(mNativeData);
    }

    /**
     * Get the data's status.
     * 
     * @return The status.
     */
    public Status getStatus()
    {
        return Status.values()[nativeGetStatus(mNativeData)];
    }

    /**
     * The next block contains the native functions needed by the NativeType class.
     */

    private static native long nativeNewWriterData(long nativeWriter);

    private static native long nativeNewReaderData(long nativeReader);

    private static native long nativeGetMemberData(long nativeData, int id);

    private static native int nativeSetMemberData(long nativeData, int id, long memberData);

    private static native long nativeGetSequence(long nativeData, int size);

    private static native int nativeGetSequenceSize(long sequence);

    private static native int nativeSetSequence(long nativeData, long sequence);

    private static native void nativeDeleteSequence(long nativeData, long sequence);

    private static native long nativeGetSequenceElement(long sequence, int index);

    private static native int nativeGetStatus(long nativeData);

    private static native int nativeGetInstanceHandle(long nativeData);

    private static native void nativeDeleteData(long nativeData);

}
