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

import org.qeo.exception.OutOfResourcesException;
import org.qeo.internal.common.ArrayType;
import org.qeo.internal.common.EnumerationType;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.common.PrimitiveArrayType;
import org.qeo.internal.common.Type;
import org.qeo.internal.common.Type.MemberType;
import org.qeo.internal.reflection.ReflectionUtil;
import org.qeo.system.DeviceInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Wrapper class around a native type.
 */
public final class NativeType
{
    private final String mName;
    private final List<NativeType> mSubTypes = new ArrayList<NativeType>();
    private long mNativeType = 0;
    private static final Logger LOG = Logger.getLogger(NativeQeo.TAG);
    private static final Map<String, ObjectType> REGISTERED_TYPES = new HashMap<String, ObjectType>();

    static {
        //Add DeviceInfo to the registered types map since it's always published by the native layer.
        ReflectionUtil<DeviceInfo> reflectionUtil = new ReflectionUtil<DeviceInfo>(DeviceInfo.class);
        ObjectType td = reflectionUtil.typeFromTypedesc(DeviceInfo.class);
        REGISTERED_TYPES.put(DeviceInfo.class.getCanonicalName(), td);
    }

    /**
     * Retrieve a previously created type for the given class or create a new one if none was created yet.
     *
     * @param nativeQeo The NativeQeo object this reader belongs to
     * @param type The type to create.
     * @param name The name of the type
     *
     * @return The name of the created type.
     */
    public static synchronized NativeType fromFactory(NativeQeo nativeQeo, ObjectType type, String name)
    {
        NativeType t = null;
        int nativeError = 0;

        LOG.fine("create native type " + name);
        if (REGISTERED_TYPES.containsKey(name)) {
            if (!REGISTERED_TYPES.get(name).equals(type)) {
                throw new IllegalStateException("Can't register this Qeo type."
                    + " There is already a type with the name name but another definition registered."
                    + "\nNew type:\n" + type + "\nOld type:\n" + REGISTERED_TYPES.get(name));
            }
        }
        t = new NativeType(nativeQeo, type, name);
        nativeError = t.registerType(nativeQeo);
        if (0 != nativeError) {
            nativeDeleteType(t.getNativeType());
            NativeError.checkError(nativeError);
        }
        LOG.fine("create native type " + name + " for type " + t.getNativeType());
        REGISTERED_TYPES.put(name, type);
        return t;
    }

    private long addNativeMember(NativeQeo nativeQeo, Type type, String name)
    {
        long elementType = 0;

        if (type instanceof ObjectType) {
            NativeType elementTypeClass = NativeType.fromFactory(nativeQeo, (ObjectType) type, type.getName());
            mSubTypes.add(elementTypeClass);
            elementType = elementTypeClass.getNativeType();
        }
        else if (type instanceof EnumerationType) {
            elementType =
                NativeTypeSupport.nativeNewEnumeration(type.getName(), ((EnumerationType) type).getConstants());
        }
        else if (type instanceof ArrayType) {
            // An array type always has one member representing the type of the elements of the array
            Type memberType = ((ArrayType) type).getElementType();
            LOG.fine("Creating sequence of " + memberType.getType());
            long arrayElementType = addNativeMember(nativeQeo, memberType, memberType.getName());
            if (arrayElementType == 0) {
                throw new OutOfResourcesException("Failed to create array element type");
            }
            elementType = NativeTypeSupport.nativeNewSequence(arrayElementType);
            if (elementType == 0) {
                throw new OutOfResourcesException("Failed to create array type");
            }
            if (memberType.getType() != Type.MemberType.TYPE_CLASS) {
                LOG.fine("Cleanup temporary native type " + arrayElementType);
                // don't cleanup struct types. They're refcounted and will be deleted by the close function
                nativeDeleteType(arrayElementType);
            }
        }
        else if (type instanceof PrimitiveArrayType) {
            // An array type always has one member representing the type of the elements of the array
            MemberType memberType = ((PrimitiveArrayType) type).getElementType();
            LOG.fine("Creating sequence of " + memberType);
            long arrayElementType = NativeTypeSupport.getAccessor(memberType).getType();
            if (arrayElementType == 0) {
                throw new OutOfResourcesException("Failed to create array element type");
            }
            elementType = NativeTypeSupport.nativeNewSequence(arrayElementType);
            if (elementType == 0) {
                throw new OutOfResourcesException("Failed to create array type");
            }
            LOG.fine("Cleanup temporary native type " + arrayElementType);
            nativeDeleteType(arrayElementType);
        }
        else {
            elementType = NativeTypeSupport.getAccessor(type.getType()).getType();
        }
        return elementType;
    }

    private NativeType(NativeQeo nativeQeo, ObjectType type, String name)
    {
        this.mName = name;
        final int[] rc = new int[1];
        final Iterator<Map.Entry<String, Type>> it = type.getMembersIterator();
        if (!it.hasNext()) {
            throw new IllegalArgumentException("The type " + name + " does not contain public fields");
        }
        mNativeType = nativeCreateType(name, rc);
        NativeError.checkError(rc[0]);
        LOG.fine("create native type " + mNativeType + ", name: " + name);
        if (0 == mNativeType) {
            throw new OutOfResourcesException("Failed to create type");
        }
        while (it.hasNext()) {
            final Map.Entry<String, Type> entry = it.next();
            final String member = entry.getKey();
            final Type t = entry.getValue();

            long elementType = addNativeMember(nativeQeo, t, member);

            if (0 != elementType) {
                int nativeError = 0;
                final int[] id = {t.getId()};
                LOG.fine("add native type " + elementType + ", name: " + member);
                nativeError = nativeAddElement(mNativeType, elementType, member, id, t.isKey());
                t.setId(id[0]);
                if (t.getType() != Type.MemberType.TYPE_CLASS) {
                    // for non-nested types
                    nativeDeleteType(elementType);
                }
                NativeError.checkError(nativeError);
            }
        }
    }

    private static synchronized void removeRegisteredType(String name)
    {
        REGISTERED_TYPES.remove(name);
    }

    /**
     * Release any native resources associated with the type.
     */
    public synchronized void close()
    {
        /* release the native type */
        LOG.fine("delete native type " + mNativeType);
        nativeDeleteType(mNativeType);

        /* close any nested types */
        for (final NativeType t : mSubTypes) {
            t.close();
        }
        removeRegisteredType(mName);
    }

    /**
     * Get the native type pointer.
     *
     * @return The native type pointer.
     */
    public long getNativeType()
    {
        return mNativeType;
    }

    /**
     * Register the type so it can be used by readers and writers.
     *
     * @param nativeQeo The NativeQeo object this reader belongs to
     *
     * @return The native return code (see @{link NativeError.Error})
     */
    public int registerType(NativeQeo nativeQeo)
    {
        if (nativeQeo == null) {
            throw new IllegalStateException("NativeQeo is not initialized (type: " + mNativeType + " -- name: " + mName
                + ")");
        }
        return nativeRegisterType(nativeQeo.getNativeFactory(), mNativeType, mName);
    }

    /**
     * The next block contains the native functions needed by the NativeType class.
     */

    private static native long nativeCreateType(String name, int[] rc);

    /**
     * Delete a native type.
     *
     * @param nativeType The native type to be deleted
     */
    public static native void nativeDeleteType(long nativeType);

    private static native int nativeAddElement(long nativeType, long elementType, String name, int[] id, boolean isKey);

    private static native int nativeRegisterType(long factory, long nativeType, String name);

}
