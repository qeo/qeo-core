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

package org.qeo.android.internal;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * In order to be able to send DeviceId object over the AIDL interface, we need to make it parcelable. Therefore this
 * class represents a parcelable DeviceId class.
 */
public class ParcelableDeviceId
    implements Parcelable
{
    /** Version of this class. This can be used later if backwards compatibility is needed. */
    private static final int VERSION = 1;
    private long mUpper;
    private long mLower;

    /**
     * This constructor takes 2 longs.
     * 
     * @param upper The MSB of DeviceId
     * @param lower The LSB of DeviceId
     */

    public ParcelableDeviceId(long upper, long lower)
    {
        mUpper = upper;
        mLower = lower;
    }

    /**
     * The default constructor.
     */
    public ParcelableDeviceId()
    {
        this.mUpper = 0;
        this.mLower = 0;
    }

    /**
     * Getter for the upper part of the device id.
     * 
     * @return the upper part
     */
    public long getMSB()
    {
        return this.mUpper;
    }

    /**
     * Getter for the upper part of the device id.
     * 
     * @return the upper part
     */
    public long getLSB()
    {
        return this.mLower;
    }

    /**
     * This constructor will construct the ParcelableDeviceId class based on a Parcel.
     * 
     * @param in The parcel to be read
     */
    public ParcelableDeviceId(Parcel in)
    {
        this();
        readFromParcel(in);
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeInt(VERSION);
        if (0 != mUpper && 0 != mLower) {
            out.writeLong(mUpper);
            out.writeLong(mLower);
        }
    }

    /**
     * Read the parcel in.
     * 
     * @param in The parcel to read from
     */
    public void readFromParcel(Parcel in)
    {
        in.readInt(); // read version int, not used for now
        mUpper = in.readLong();
        mLower = in.readLong();
    }

    /**
     * This field is needed for Android to be able to create new objects, individually or as arrays.
     */
    public static final Parcelable.Creator<ParcelableDeviceId> CREATOR = new Parcelable.Creator<ParcelableDeviceId>() {
        @Override
        public ParcelableDeviceId createFromParcel(Parcel in)
        {
            return new ParcelableDeviceId(in);
        }

        @Override
        public ParcelableDeviceId[] newArray(int size)
        {
            return new ParcelableDeviceId[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

}
