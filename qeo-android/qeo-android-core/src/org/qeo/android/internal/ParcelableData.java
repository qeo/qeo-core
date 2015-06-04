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

import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.internal.common.ArrayData;
import org.qeo.internal.common.Data;
import org.qeo.internal.common.EnumerationData;
import org.qeo.internal.common.ObjectData;
import org.qeo.internal.common.PrimitiveArrayData;
import org.qeo.internal.common.PrimitiveData;
import org.qeo.internal.common.Type.MemberType;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * In order to be able to send Data object over the AIDL interface, we need to make it parcelable. Therefore this class
 * represents a parcelable Data class.
 */
public class ParcelableData
    implements Parcelable
{
    /** Version of this class. This can be used later if backwards compatibility is needed. */
    private static final int VERSION = 1;
    private ObjectData mData;
    private Boolean mValid;
    private int mVersion;
    private static final String TAG = "ParcelableData";
    private static final Logger LOG = Logger.getLogger(TAG);

    /**
     * This constructor takes a Data object and stores it internally.
     * 
     * @param data The Data
     */
    public ParcelableData(ObjectData data)
    {
        mData = data;
        if (data == null) {
            mValid = false;
        }
        else {
            mValid = true;
        }
    }

    /**
     * Default constructor, mData will be set to null.
     */
    public ParcelableData()
    {
        this((ObjectData) null);
    }

    /**
     * This constructor will construct the ParcelableData class based on a Parcel.
     * 
     * @param in The parcel to be read
     */
    public ParcelableData(Parcel in)
    {
        this();
        readFromParcel(in);
    }

    /**
     * Getter for the mData field.
     * 
     * @return the mData field
     */
    public ObjectData getData()
    {
        return mData;
    }

    /**
     * Setter for the mData field.
     * 
     * @param data the data field
     */
    public void setData(ObjectData data)
    {
        mData = data;
        if (data == null) {
            mValid = false;
        }
        else {
            mValid = true;
        }
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    /**
     * Write one specific data object to the parcel. This method will dispatch to more specific function depending on
     * the type of data.
     * 
     * @param dest The parcel in which the object should be written
     * @param data The data to be parceled.
     */
    private void writeDataToParcel(Parcel dest, Data data)
    {
        if (data instanceof PrimitiveData) {
            writeDataToParcel(dest, (PrimitiveData) data);
        }
        else if (data instanceof PrimitiveArrayData) {
            writeDataToParcel(dest, (PrimitiveArrayData) data);
        }
        else if (data instanceof ObjectData) {
            writeDataToParcel(dest, (ObjectData) data);
        }
        else if (data instanceof ArrayData) {
            writeDataToParcel(dest, (ArrayData) data);
        }
        else if (data instanceof EnumerationData) {
            writeDataToParcel(dest, (EnumerationData) data);
        }
        else {
            LOG.severe("unsupported type: " + data.getClass());
        }
    }

    /**
     * Write a primitive data object to the parcel.
     * 
     * @param dest The parcel in which the object should be written
     * @param data The data to be parceled.
     */
    private void writeDataToParcel(Parcel dest, PrimitiveData data)
    {
        Object value = data.getValue();

        LOG.log(Level.FINE, "write <{0}> to parcel", data);
        dest.writeInt(data.getId());
        dest.writeString(MemberType.TypeImplemtation.PRIMITIVE.name());
        if (null == value) {
            /* We use an empty type string, to indicate that the data contains a null value, only id is valid */
            dest.writeString("");
        }
        else if (value instanceof Integer) {
            dest.writeString(MemberType.TYPE_INT.name());
            dest.writeInt((Integer) value);
        }
        else if (value instanceof String) {
            dest.writeString(MemberType.TYPE_STRING.name());
            dest.writeString((String) value);
        }
        else if (value instanceof Byte) {
            dest.writeString(MemberType.TYPE_BYTE.name());
            dest.writeByte((Byte) value);
        }
        else if (value instanceof Short) {
            /* write as int */
            dest.writeString(MemberType.TYPE_SHORT.name());
            dest.writeInt((Short) value);
        }
        else if (value instanceof Long) {
            dest.writeString(MemberType.TYPE_LONG.name());
            dest.writeLong((Long) value);
        }
        else if (value instanceof Float) {
            dest.writeString(MemberType.TYPE_FLOAT.name());
            dest.writeFloat((Float) value);
        }
        else if (value instanceof Boolean) {
            dest.writeString(MemberType.TYPE_BOOLEAN.name());
            dest.writeInt((Boolean) value ? 1 : 0);
        }
        else {
            LOG.severe("unsupported type: " + value.getClass());
        }
    }

    /**
     * Write a primitive data object to the parcel.
     * 
     * @param dest The parcel in which the object should be written
     * @param data The data to be parceled.
     */
    private void writeDataToParcel(Parcel dest, PrimitiveArrayData data)
    {
        Object value = data.getValue();

        LOG.log(Level.FINE, "write <{0}> to parcel", data);
        dest.writeInt(data.getId());
        dest.writeString(MemberType.TypeImplemtation.PRIMITIVEARRAY.name());
        if (null == value) {
            /* TYPE_PRIMITIVEARRAY is used to indicate that the data contains a null value, only id is valid */
            dest.writeString("");
        }
        else if (value instanceof int[]) {
            dest.writeString(MemberType.TYPE_INTARRAY.name());
            dest.writeInt(((int[]) value).length);
            if (((int[]) value).length > 0) {
                dest.writeIntArray((int[]) value);
            }
        }
        else if (value instanceof String[]) {
            dest.writeString(MemberType.TYPE_STRINGARRAY.name());
            dest.writeInt(((String[]) value).length);
            if (((String[]) value).length > 0) {
                dest.writeStringArray((String[]) value);
            }
        }
        else if (value instanceof byte[]) {
            dest.writeString(MemberType.TYPE_BYTEARRAY.name());
            dest.writeInt(((byte[]) value).length);
            if (((byte[]) value).length > 0) {
                dest.writeByteArray((byte[]) value);
            }
        }
        else if (value instanceof short[]) {
            dest.writeString(MemberType.TYPE_SHORTARRAY.name());
            dest.writeInt(((short[]) value).length);
            if (((short[]) value).length > 0) {
                int[] intArray = new int[((short[]) value).length];
                for (int i = 0; i < ((short[]) value).length; i++) {
                    intArray[i] = ((short[]) value)[i];
                }
                dest.writeIntArray(intArray);
            }
        }
        else if (value instanceof long[]) {
            dest.writeString(MemberType.TYPE_LONGARRAY.name());
            dest.writeInt(((long[]) value).length);
            if (((long[]) value).length > 0) {
                dest.writeLongArray((long[]) value);
            }
        }
        else if (value instanceof float[]) {
            dest.writeString(MemberType.TYPE_FLOATARRAY.name());
            dest.writeInt(((float[]) value).length);
            if (((float[]) value).length > 0) {
                dest.writeFloatArray((float[]) value);
            }
        }
        else if (value instanceof boolean[]) {
            dest.writeString(MemberType.TYPE_BOOLEANARRAY.name());
            dest.writeInt(((boolean[]) value).length);
            if (((boolean[]) value).length > 0) {
                dest.writeBooleanArray((boolean[]) value);
            }
        }
        else {
            LOG.severe("unsupported type: " + value.getClass());
        }
    }

    /**
     * Write an object data object to the parcel.
     * 
     * @param dest The parcel in which the object should be written
     * @param data The data to be parceled.
     */
    private void writeDataToParcel(Parcel dest, ObjectData data)
    {
        LOG.log(Level.FINE, "write <{0}> to parcel", data);
        dest.writeInt(data.getId());
        dest.writeString(MemberType.TypeImplemtation.OBJECT.name());
        if (null != data.getMembers()) {
            dest.writeInt(data.getMembers().size());
            for (Entry<Integer, Data> member : data.getMembers().entrySet()) {
                writeDataToParcel(dest, member.getValue());
            }
        }
    }

    /**
     * Write an enumeration data object to the parcel.
     * 
     * @param dest The parcel in which the object should be written
     * @param data The data to be parceled.
     */
    private void writeDataToParcel(Parcel dest, EnumerationData data)
    {
        LOG.log(Level.FINE, "write <{0}> to parcel", data);
        dest.writeInt(data.getId());
        dest.writeString(MemberType.TypeImplemtation.ENUM.name());
        dest.writeInt(data.getValue());
    }

    /**
     * Write an array data object to the parcel.
     * 
     * @param dest The parcel in which the object should be written
     * @param data The data to be parceled.
     */
    private void writeDataToParcel(Parcel dest, ArrayData data)
    {
        LOG.log(Level.FINE, "write <{0}> to parcel", data);
        dest.writeInt(data.getId());
        dest.writeString(MemberType.TypeImplemtation.ARRAY.name());
        dest.writeInt(data.size());
        for (final Data e : data.getElements()) {
            writeDataToParcel(dest, e);
        }
    }

    /**
     * Write the mData into the parcel dest. Note that only if the mValid bit is set, mData will be written into the
     * parcel.
     * 
     * @param dest The parcel in which the object should be written
     * @param flags Additional flags about how the object should be written
     */
    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        try {
            dest.writeByte((byte) (mValid ? 1 : 0));
            dest.writeInt(VERSION);
            if (mValid) {
                writeDataToParcel(dest, mData);
            }
        }
        catch (final Exception e) {
            LOG.log(Level.SEVERE, "writing failed", e);
        }
    }

    private ObjectData readObjectFromParcel(Parcel in, int id)
    {
        int size = in.readInt();
        ObjectData data = new ObjectData(id);
        LOG.log(Level.FINE, "read <{0}, {1} members> from parcel", new Object[] {data, size});
        for (int i = 0; i < size; i++) {
            data.addMember(readDataFromParcel(in));
        }
        return data;
    }

    private EnumerationData readEnumerationFromParcel(Parcel in, int id)
    {
        return new EnumerationData(id, in.readInt());
    }

    private ArrayData readArrayFromParcel(Parcel in, int id)
    {
        int size = in.readInt();
        ArrayData data = new ArrayData(id);
        LOG.log(Level.FINE, "read <{0}, {1} elements> from parcel", new Object[] {data, size});
        for (int i = 0; i < size; i++) {
            data.addElement(readDataFromParcel(in));
        }
        return data;
    }

    private PrimitiveArrayData readPrimitiveArrayFromParcel(Parcel in, int id)
    {
        PrimitiveArrayData newData = null;
        String typeString = in.readString();
        if (typeString.contentEquals("")) {
            /* If the type string is empty, it means that the data contains a null value, only id is valid */
            newData = new PrimitiveArrayData(id, null);
        }
        else {
            MemberType type = MemberType.valueOf(typeString);
            int size = in.readInt();
            switch (type) {
                case TYPE_INTARRAY: {
                    int[] array = new int[size];
                    if (0 != size) {
                        in.readIntArray(array);
                    }
                    newData = new PrimitiveArrayData(id, array);
                    break;
                }
                case TYPE_STRINGARRAY: {
                    String[] array = new String[size];
                    if (0 != size) {
                        in.readStringArray(array);
                    }
                    newData = new PrimitiveArrayData(id, array);
                    break;
                }
                case TYPE_BYTEARRAY: {
                    byte[] array = new byte[size];
                    if (0 != size) {
                        in.readByteArray(array);
                    }
                    newData = new PrimitiveArrayData(id, array);
                    break;
                }
                case TYPE_SHORTARRAY: {
                    short[] array = new short[size];
                    if (0 != size) {
                        int[] intArray = new int[size];
                        in.readIntArray(intArray);
                        for (int i = 0; i < size; i++) {
                            array[i] = (short) intArray[i];
                        }
                    }
                    newData = new PrimitiveArrayData(id, array);
                    break;
                }
                case TYPE_LONGARRAY: {
                    long[] array = new long[size];
                    if (0 != size) {
                        in.readLongArray(array);
                    }
                    newData = new PrimitiveArrayData(id, array);
                    break;
                }
                case TYPE_FLOATARRAY: {
                    float[] array = new float[size];
                    if (0 != size) {
                        in.readFloatArray(array);
                    }
                    newData = new PrimitiveArrayData(id, array);
                    break;
                }
                case TYPE_BOOLEANARRAY: {
                    boolean[] array = new boolean[size];
                    if (0 != size) {
                        in.readBooleanArray(array);
                    }
                    newData = new PrimitiveArrayData(id, array);
                    break;
                }
                default:
                    throw new IllegalArgumentException("unsupported primitive array type");
            }
        }
        return newData;
    }

    private PrimitiveData readPrimitiveFromParcel(Parcel in, int id)
    {
        PrimitiveData newData = null;
        String typeString = in.readString();
        if (typeString.contentEquals("")) {
            /* If the type string is empty, it means that the data contains a null value, only id is valid */
            newData = new PrimitiveData(id, null);
        }
        else {
            MemberType type = MemberType.valueOf(typeString);
            switch (type) {
                case TYPE_INT:
                    newData = new PrimitiveData(id, in.readInt());
                    break;
                case TYPE_STRING:
                    newData = new PrimitiveData(id, in.readString());
                    break;
                case TYPE_BYTE:
                    newData = new PrimitiveData(id, in.readByte());
                    break;
                case TYPE_SHORT:
                    /* read as int */
                    newData = new PrimitiveData(id, (short) in.readInt());
                    break;
                case TYPE_LONG:
                    newData = new PrimitiveData(id, in.readLong());
                    break;
                case TYPE_FLOAT:
                    newData = new PrimitiveData(id, in.readFloat());
                    break;
                case TYPE_BOOLEAN:
                    newData = new PrimitiveData(id, (in.readInt() == 0) ? false : true);
                    break;
                default:
                    throw new IllegalArgumentException("unsupported primitive type");
            }
        }
        return newData;
    }

    /**
     * Read one specific mData object from the parcel. This function will be called recursively in case the read object
     * type is MemberType.TYPE_CLASS
     * 
     * @param in The parcel from which the object should be read
     */
    private Data readDataFromParcel(Parcel in)
    {
        Data newData = null;
        int id = 0;
        MemberType.TypeImplemtation type;
        String typeImplementation;

        id = in.readInt();
        typeImplementation = in.readString();
        type = MemberType.TypeImplemtation.valueOf(typeImplementation);
        switch (type) {
            case OBJECT: {
                newData = readObjectFromParcel(in, id);
                break;
            }
            case ENUM: {
                newData = readEnumerationFromParcel(in, id);
                break;
            }
            case ARRAY: {
                newData = readArrayFromParcel(in, id);
                break;
            }
            case PRIMITIVEARRAY: {
                newData = readPrimitiveArrayFromParcel(in, id);
                break;
            }
            case PRIMITIVE: {
                newData = readPrimitiveFromParcel(in, id);
                break;
            }
            default:
                throw new IllegalArgumentException("unsupported type");
        }
        LOG.log(Level.FINE, "read <{0}> from parcel", newData);
        return newData;
    }

    /**
     * Read the parcel in and initialize a Data object.
     * 
     * @param in The parcel from which the object should be read
     */
    public void readFromParcel(Parcel in)
    {
        try {
            mValid = (in.readByte() == 1);
            if (mValid) {
                mVersion = in.readInt();
                LOG.fine("Got parcel object version: " + mVersion);
                mData = (ObjectData) readDataFromParcel(in);
            }
            else {
                // parcel is empty, this will be the case if the data == null
                LOG.fine("Empty parcel data");
            }
        }
        catch (final Exception e) {
            LOG.log(Level.SEVERE, "reading failed", e);
        }
    }

    /**
     * This field is needed for Android to be able to create new objects, individually or as arrays.
     */
    public static final Parcelable.Creator<ParcelableData> CREATOR = new Parcelable.Creator<ParcelableData>() {
        @Override
        public ParcelableData createFromParcel(Parcel in)
        {
            return new ParcelableData(in);
        }

        @Override
        public ParcelableData[] newArray(int size)
        {
            return new ParcelableData[size];
        }
    };

    @Override
    public String toString()
    {
        if (mData != null) {
            return mData.toString();
        }
        else {
            return "Empty";
        }
    }
}
