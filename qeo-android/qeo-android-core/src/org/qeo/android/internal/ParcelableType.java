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

package org.qeo.android.internal;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.internal.common.ArrayType;
import org.qeo.internal.common.EnumerationType;
import org.qeo.internal.common.ObjectType;
import org.qeo.internal.common.PrimitiveArrayType;
import org.qeo.internal.common.PrimitiveType;
import org.qeo.internal.common.Type;
import org.qeo.internal.common.Type.MemberType;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * In order to be able to send Type object over the AIDL interface, we need to make it parcelable. Therefore this class
 * represents a parcelable Type class.
 */
public class ParcelableType
    implements Parcelable
{
    /** Version of this class. This can be used later if backwards compatibility is needed. */
    private static final int VERSION = 1;
    private ObjectType mType;
    private static final String TAG = "ParcelableType";
    private static final Logger LOG = Logger.getLogger(TAG);

    /**
     * This constructor takes a Type object and stores it internally.
     * 
     * @param type The Type
     */
    public ParcelableType(ObjectType type)
    {
        mType = type;
    }

    /**
     * Default constructor, mType will be set to null.
     */
    public ParcelableType()
    {
        mType = null;
    }

    /**
     * This constructor will construct the ParcelableType class based on a Parcel.
     * 
     * @param in The parcel to be read
     */
    public ParcelableType(Parcel in)
    {
        this();
        readFromParcel(in);
    }

    /**
     * Getter for the mType field.
     * 
     * @return the mType field
     */
    public ObjectType getType()
    {
        return mType;
    }

    @Override
    public int describeContents()
    {

        return 0;
    }

    /**
     * Write one specific mType object to the parcel. This function will be called recursively in case the written
     * object mType is MemberType.TYPE_CLASS
     * 
     * @param dest The parcel in which the object should be written
     */
    private void writeTypeToParcel(Parcel dest, Type type)
    {
        LOG.log(Level.FINE, "write <{0}> to parcel", type);
        dest.writeString(type.getName());
        dest.writeInt(type.getId());
        dest.writeByte((byte) (type.isKey() ? 1 : 0));
        dest.writeString(type.getType().name());
        if (type instanceof ObjectType) {
            dest.writeInt(((ObjectType) type).getMembers().size());
            final Iterator<Map.Entry<String, Type>> it = ((ObjectType) type).getMembersIterator();
            while (it.hasNext()) {
                final Map.Entry<String, Type> entry = it.next();
                final String member = entry.getKey();
                final Type t = entry.getValue();

                dest.writeString(member);
                writeTypeToParcel(dest, t);
            }
        }
        else if (type instanceof EnumerationType) {
            dest.writeStringArray(((EnumerationType) type).getConstants());
        }
        else if (type instanceof PrimitiveArrayType) {
            PrimitiveType elemType = new PrimitiveType(null, 0, false, ((PrimitiveArrayType) type).getElementType());
            writeTypeToParcel(dest, elemType);
        }
        else if (type instanceof ArrayType) {
            final Type elemType = ((ArrayType) type).getElementType();
            writeTypeToParcel(dest, elemType);
        }
    }

    /**
     * Write the mType into the parcel dest. The mType member field MUST NOT be null at this time.
     * 
     * @param dest The parcel in which the object should be written
     * @param flags Additional flags about how the object should be written
     */
    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(VERSION);
        if (null != mType) {
            writeTypeToParcel(dest, mType);
        }
    }

    /**
     * Read one specific mType object from the parcel. This function will be called recursively in case the read object
     * mType is MemberType.TYPE_CLASS
     * 
     * @param in The parcel from which the object should be read
     */
    private Type readTypeFromParcel(Parcel in)
    {
        String name = in.readString();
        int id = in.readInt();
        boolean key = (in.readByte() == 1);
        MemberType memberType = MemberType.valueOf(in.readString());
        Type newType = null;
        if (memberType.getTypeImplementation() == MemberType.TypeImplemtation.OBJECT) {
            newType = new ObjectType(name, id, key);
            final int numMembers = in.readInt();
            for (int i = 0; i < numMembers; i++) {
                final String memberName = in.readString();
                ((ObjectType) newType).addMember(readTypeFromParcel(in), memberName);
            }
        }
        else if (memberType.getTypeImplementation() == MemberType.TypeImplemtation.ENUM) {
            newType = new EnumerationType(name, null, id, key, in.createStringArray());
        }
        else if (memberType.getTypeImplementation() == MemberType.TypeImplemtation.PRIMITIVEARRAY) {
            newType = new PrimitiveArrayType(name, id, key, readTypeFromParcel(in).getType());
        }
        else if (memberType.getTypeImplementation() == MemberType.TypeImplemtation.ARRAY) {
            newType = new ArrayType(name, id, key, readTypeFromParcel(in));
        }
        else {
            newType = new PrimitiveType(name, id, key, memberType);
        }
        LOG.log(Level.FINE, "read <{0}> from parcel", newType);
        return newType;
    }

    /**
     * Read the parcel in and initialize a Type object. If the mType field is null at this time, instantiate a new Type
     * object.
     * 
     * @param in The parcel from which the object should be read
     */
    public void readFromParcel(Parcel in)
    {
        in.readInt(); // read version. Not used for now.
        mType = (ObjectType) readTypeFromParcel(in);
    }

    /**
     * This field is needed for Android to be able to create new objects, individually or as arrays.
     */
    public static final Parcelable.Creator<ParcelableType> CREATOR = new Parcelable.Creator<ParcelableType>() {
        @Override
        public ParcelableType createFromParcel(Parcel in)
        {
            return new ParcelableType(in);
        }

        @Override
        public ParcelableType[] newArray(int size)
        {
            return new ParcelableType[size];
        }
    };
}
