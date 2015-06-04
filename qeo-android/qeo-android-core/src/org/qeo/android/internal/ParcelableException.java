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

import java.util.logging.Level;
import java.util.logging.Logger;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * In order to be able to send RuntimeException object over the AIDL interface, we need to make it parcelable. Therefore
 * this class represents a parcelable RuntimeException class.
 */
public class ParcelableException
    implements Parcelable
{
    /** Version of this class. This can be used later if backwards compatibility is needed. */
    private static final int VERSION = 1;
    private RuntimeException mException;
    private Boolean mValid;
    private static final String TAG = "ParcelableException";
    private static final Logger LOG = Logger.getLogger(TAG);

    /**
     * This constructor takes an exception object and stores it internally.
     * 
     * @param exception The exception
     */
    public ParcelableException(RuntimeException exception)
    {
        setException(exception);
    }

    /**
     * Default constructor, exception will be set to null.
     */
    public ParcelableException()
    {
        this((RuntimeException) null);
    }

    /**
     * This constructor will construct the ParcelableException class based on a Parcel.
     * 
     * @param in The parcel to be read
     */
    public ParcelableException(Parcel in)
    {
        this();
        readFromParcel(in);
    }

    /**
     * Getter for the exception field.
     * 
     * @return the exception field
     */
    public RuntimeException getException()
    {
        return mException;
    }

    /**
     * Setter for the exception field.
     * 
     * @param exception the exception field
     */
    public void setException(RuntimeException exception)
    {
        mException = exception;
        if (exception == null) {
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
     * Write the exception into the parcel dest. Note that only if the valid bit is set, exception will be written into
     * the parcel
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
                dest.writeSerializable(mException);
            }
        }
        catch (final RuntimeException e) {
            LOG.log(Level.SEVERE, "writing failed", e);
        }
    }

    /**
     * Read the parcel in and initialize an Exception object.
     * 
     * @param in The parcel from which the object should be read
     */
    public void readFromParcel(Parcel in)
    {
        try {
            mValid = (in.readByte() == 1);
            if (mValid) {
                in.readInt(); // read version, not used for now
                mException = (RuntimeException) in.readSerializable();
            }
        }
        catch (final RuntimeException e) {
            LOG.log(Level.SEVERE, "reading failed", e);
        }
    }

    /**
     * This field is needed for Android to be able to create new objects, individually or as arrays.
     */
    public static final Parcelable.Creator<ParcelableException> CREATOR =
        new Parcelable.Creator<ParcelableException>() {
            @Override
            public ParcelableException createFromParcel(Parcel in)
            {
                return new ParcelableException(in);
            }

            @Override
            public ParcelableException[] newArray(int size)
            {
                return new ParcelableException[size];
            }
        };
}
