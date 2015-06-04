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

import org.qeo.internal.common.ReaderFilter;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * In order to be able to send Filter object over the AIDL interface, we need to make it parcelable. Therefore this
 * class represents a parcelable Filter class.
 */
public class ParcelableFilter
    implements Parcelable
{
    /** Version of this class. This can be used later if backwards compatibility is needed. */
    private static final int VERSION = 1;
    private ReaderFilter mFilter;

    /**
     * This constructor takes a ReaderFilter object and stores it internally.
     * 
     * @param filter The ReaderFilter
     */
    public ParcelableFilter(ReaderFilter filter)
    {
        super();
        mFilter = filter;
    }

    /**
     * The default constructor.
     */
    public ParcelableFilter()
    {
        this((ReaderFilter) null);
    }

    /**
     * This constructor will construct the ParcelableFilter class based on a Parcel.
     * 
     * @param in The parcel to be read
     */
    public ParcelableFilter(Parcel in)
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
     * Getter for the mFilter field.
     * 
     * @return the mFilter field
     */
    public ReaderFilter getFilter()
    {
        return mFilter;
    }

    /**
     * Write the mFilter into the parcel dest. The mFilter member field MUST NOT be null at this time.
     * 
     * @param dest The parcel in which the object should be written
     * @param flags Additional flags about how the object should be written
     */
    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(VERSION);
        if (null != mFilter) {
            dest.writeInt(mFilter.getInstanceHandle());
        }
    }

    /**
     * Read the parcel in and initialize a ReaderFilter object. If the mFilter field is null at this time, instantiate a
     * new ReaderFilter object
     * 
     * @param in The parcel from which the object should be read
     */
    public void readFromParcel(Parcel in)
    {
        in.readInt(); // read version. Not used for now.
        if (null == mFilter) {
            mFilter = new ReaderFilter();
        }
        mFilter.setInstanceHandle(in.readInt());
    }

    /**
     * This field is needed for Android to be able to create new objects, individually or as arrays.
     */
    public static final Parcelable.Creator<ParcelableFilter> CREATOR = new Parcelable.Creator<ParcelableFilter>() {
        @Override
        public ParcelableFilter createFromParcel(Parcel in)
        {
            return new ParcelableFilter(in);
        }

        @Override
        public ParcelableFilter[] newArray(int size)
        {
            return new ParcelableFilter[size];
        }
    };

}
