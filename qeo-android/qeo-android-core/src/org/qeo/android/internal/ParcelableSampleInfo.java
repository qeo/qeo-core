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

import org.qeo.internal.common.SampleInfo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * In order to be able to send SampleInfo object over the AIDL interface, we need to make it parcelable. Therefore this
 * class represents a parcelable SampleInfo class.
 */
public class ParcelableSampleInfo
    implements Parcelable
{
    /** Version of this class. This can be used later if backwards compatibility is needed. */
    private static final int VERSION = 1;
    private SampleInfo mInfo;

    /**
     * This constructor takes a SampleInfo object and stores it internally.
     * 
     * @param info The SampleInfo
     */
    public ParcelableSampleInfo(SampleInfo info)
    {
        super();
        mInfo = info;
    }

    /**
     * The default constructor.
     */
    public ParcelableSampleInfo()
    {
        this((SampleInfo) null);
    }

    /**
     * This constructor will construct the ParcelableSampleInfo class based on a Parcel.
     * 
     * @param in The parcel to be read
     */
    public ParcelableSampleInfo(Parcel in)
    {
        this();
        readFromParcel(in);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    /**
     * Getter for the mInfo field.
     * 
     * @return the mInfo field
     */
    public SampleInfo getInfo()
    {
        return mInfo;
    }

    /**
     * Setter for the mInfo field.
     * 
     * @param info the new info
     */
    public void setInfo(SampleInfo info)
    {
        mInfo = info;
    }

    /**
     * Write the mInfo into the parcel dest. The mInfo member field MUST NOT be null at this time.
     * 
     * @param dest The parcel in which the object should be written
     * @param flags Additional flags about how the object should be written
     */
    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(VERSION);
        if (null != mInfo) {
            dest.writeInt(mInfo.getInstanceHandle());
        }
    }

    /**
     * Read the parcel in and initialize a SampleInfo object. If the mInfo field is null at this time, instantiate a new
     * SampleInfo object.
     * 
     * @param in The parcel from which the object should be read
     */
    public void readFromParcel(Parcel in)
    {
        in.readInt(); // read version. Not used for now.
        if (null == mInfo) {
            mInfo = new SampleInfo();
        }
        mInfo.setInstanceHandle(in.readInt());
    }

    /**
     * This field is needed for Android to be able to create new objects, individually or as arrays.
     */
    public static final Parcelable.Creator<ParcelableSampleInfo> CREATOR =
        new Parcelable.Creator<ParcelableSampleInfo>() {
            @Override
            public ParcelableSampleInfo createFromParcel(Parcel in)
            {
                return new ParcelableSampleInfo(in);
            }

            @Override
            public ParcelableSampleInfo[] newArray(int size)
            {
                return new ParcelableSampleInfo[size];
            }
        };

}
