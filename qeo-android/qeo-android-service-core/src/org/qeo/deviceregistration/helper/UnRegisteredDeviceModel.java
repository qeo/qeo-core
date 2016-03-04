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

package org.qeo.deviceregistration.helper;

import org.qeo.deviceregistration.model.UnRegisteredDevice;
import org.qeo.system.RegistrationRequest;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Model class to reflect the current status of unregistered devices on the UI.
 */
public class UnRegisteredDeviceModel
    implements Parcelable, UnRegisteredDevice
{

    /**
     * Generates instances of the Parcelable class (UnRegisteredDeviceModel) from a Parcel.
     */
    public static final Creator<UnRegisteredDeviceModel> CREATOR = new Creator<UnRegisteredDeviceModel>() {
        @Override
        public UnRegisteredDeviceModel createFromParcel(Parcel in)
        {
            return new UnRegisteredDeviceModel(in);
        }

        @Override
        public UnRegisteredDeviceModel[] newArray(int size)
        {
            return new UnRegisteredDeviceModel[size];
        }
    };
    private final String mModelName;
    private final String mManufacturerName;
    private final String mUserFriendlyName;
    private final String mUserName;
    private final String mErrorMessage;
    private final String mRSAPublicKey;
    private final short mErrorCode;
    private final short mRegistrationStatus;
    private final long mUpper;
    private final long mLower;
    private final short mOTCEncryptVersion;

    /**
     * Create an UnRegisteredDeviceModel instance from a RegistrationRequest instance.
     * 
     * @param req The instance to parcel.
     */
    public UnRegisteredDeviceModel(RegistrationRequest req)
    {
        mUpper = req.deviceId.upper;
        mLower = req.deviceId.lower;
        mModelName = req.modelName;
        mManufacturerName = req.manufacturer;
        mRSAPublicKey = req.rsaPublicKey;
        mUserFriendlyName = req.userFriendlyName;
        mUserName = req.userName;
        mErrorMessage = req.errorMessage;
        mErrorCode = req.errorCode;
        mRegistrationStatus = req.registrationStatus;
        mOTCEncryptVersion = req.version;
    }

    /**
     * Create an UnRegisteredDeviceModel instance from a Parcel object.
     * 
     * @param in -parcel object
     */

    public UnRegisteredDeviceModel(Parcel in)
    {
        mUpper = in.readLong();
        mLower = in.readLong();
        mModelName = in.readString();
        mManufacturerName = in.readString();
        mRSAPublicKey = in.readString();
        mUserFriendlyName = in.readString();
        mUserName = in.readString();
        mErrorMessage = in.readString();
        mErrorCode = (short) in.readInt();
        mRegistrationStatus = (short) in.readInt();
        mOTCEncryptVersion = (short) in.readInt();

    }

    @Override
    public String getModelName()
    {
        return mModelName;
    }

    @Override
    public String getManufacturerName()
    {
        return mManufacturerName;
    }

    @Override
    public String getUserFriendlyName()
    {
        return mUserFriendlyName;
    }

    @Override
    public String getUserName()
    {
        return mUserName;
    }

    @Override
    public RegistrationStatusCode getRegistrationStatus()
    {
        return getRegistrationStatus(mRegistrationStatus);
    }

    /**
     * Convert the registrationStatusCode from the Qeo type into an enum.
     * 
     * @param registrationStatusCode The type code.
     * @return The enum.
     */
    public static RegistrationStatusCode getRegistrationStatus(short registrationStatusCode)
    {
        switch (registrationStatusCode) {
            case 0:
                return RegistrationStatusCode.UNREGISTERED;
            case 1:
                return RegistrationStatusCode.REGISTERING;
            case 2:
                return RegistrationStatusCode.REGISTERED;
            default:
                return RegistrationStatusCode.UNREGISTERED;
        }
    }

    @Override
    public ErrorCode getErrorCode()
    {

        switch (mErrorCode) {
            case 0:
                return ErrorCode.NONE;
            case 1:
                return ErrorCode.CANCELLED;
            case 2:
                return ErrorCode.REMOTE_REGISTRATION_TIMEOUT;
            case 3:
                return ErrorCode.NEGATIVE_CONFIRMATION;

            case 4:
                return ErrorCode.PLATFORM_FAILURE;

            case 5:
                return ErrorCode.INVALID_OTP;

            case 6:
                return ErrorCode.INTERNAL_ERROR;

            case 7:
                return ErrorCode.NETWORK_FAILURE;

            case 8:
                return ErrorCode.SSL_HANDSHAKE_FAILURE;

            case 9:
                return ErrorCode.RECEIVED_INVALID_CREDENTIALS;

            case 10:
                return ErrorCode.STORE_FAILURE;

            case 11:
                return ErrorCode.UNKNOWN;

            default:
                return ErrorCode.NONE;

        }

    }

    @Override
    public String getErrorMessage()
    {
        return mErrorMessage;
    }

    @Override
    public String getRSAPublicKey()
    {
        return mRSAPublicKey;
    }

    @Override
    public Long getUpper()
    {
        return mUpper;
    }

    @Override
    public Long getLower()
    {
        return mLower;
    }

    @Override
    public Short getVersion()
    {
        return mOTCEncryptVersion;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i)
    {

        parcel.writeLong(mUpper);
        parcel.writeLong(mLower);
        parcel.writeString(mModelName);
        parcel.writeString(mManufacturerName);
        parcel.writeString(mRSAPublicKey);
        parcel.writeString(mUserFriendlyName);
        parcel.writeString(mUserName);
        parcel.writeString(mErrorMessage);
        parcel.writeInt(mErrorCode);
        parcel.writeInt(mRegistrationStatus);
        parcel.writeInt(mOTCEncryptVersion);

    }

}
