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

import org.qeo.policy.Identity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * In order to be able to send DeviceId object over the AIDL interface, we need to make it parcelable. Therefore this
 * class represents a parcelable DeviceId class.
 */
public class ParcelablePolicyIdentity
    implements Parcelable
{
    /** Version of this class. This can be used later if backwards compatibility is needed. */
    private static final int VERSION = 1;
    private final long mUID;

    /**
     * Construct a parcelable identity.
     * 
     * @param identity The non-parcelable identity
     */

    public ParcelablePolicyIdentity(Identity identity)
    {
        if (null == identity) {
            mUID = -1;
        }
        else {
            mUID = identity.getUserID();
        }
    }

    /**
     * Get the identity.
     * 
     * @return the identity
     */
    public Identity getIdentity()
    {
        if (mUID == -1) {
            return null;
        }
        return new Identity(mUID);
    }

    /**
     * This constructor will construct the ParcelableDeviceId class based on a Parcel.
     * 
     * @param in The parcel to be read
     */
    public ParcelablePolicyIdentity(Parcel in)
    {
        in.readInt(); // read version. Not used for now.
        mUID = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeInt(VERSION);
        out.writeLong(mUID);
    }

    /**
     * This field is needed for Android to be able to create new objects, individually or as arrays.
     */
    public static final Parcelable.Creator<ParcelablePolicyIdentity> CREATOR =
        new Parcelable.Creator<ParcelablePolicyIdentity>() {
            @Override
            public ParcelablePolicyIdentity createFromParcel(Parcel in)
            {
                return new ParcelablePolicyIdentity(in);
            }

            @Override
            public ParcelablePolicyIdentity[] newArray(int size)
            {
                return new ParcelablePolicyIdentity[size];
            }
        };

    @Override
    public int describeContents()
    {
        return 0;
    }

}
