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

package org.qeo.deviceregistration.helper;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.rest.RestHelper;
import org.qeo.jni.NativeQeo;
import org.qeo.system.RegistrationCredentials;
import org.qeo.system.RegistrationRequest;

/**
 * Utility class to convert RegistrationRequest into RegistrationCredentials.
 */
public final class RegistrationCredentialsHelper
{
    private static final Logger LOG = Logger.getLogger("RegistrationCredentialsHelper");
    private long mRealmId;
    private final long mUserId;
    private final String mDeviceName;
    private final String mRsaPublicKey;

    /**
     * Create an instance.
     *
     * @param userId       The id of the user in the realm.
     * @param deviceName   The name of the device to be created in the realm.
     * @param rsaPublicKey The public key of the remote device
     */
    public RegistrationCredentialsHelper(long userId, String deviceName, String rsaPublicKey)
    {
        mUserId = userId;
        mDeviceName = deviceName;
        mRsaPublicKey = rsaPublicKey;
        mRealmId = DeviceRegPref.getSelectedRealmId();
    }

    /**
     * Set the realm Id.
     * @param realmId The realm id.
     */
    public void setRealmId(long realmId)
    {
        mRealmId = realmId;
    }


    /**
     * Register a remote device.<br>
     * This call will:
     * <ul>
     * <li>Create a new device on the SMS</li>
     * <li>Fetch the generated OTC</li>
     * <li>Encrypt the OTC with the public key of the remote device.</li>
     * </ul>
     * Note that this call will be blocking.
     *
     * @return The encrypted OTC on success, null otherwise
     * @throws IOException If something went wrong.
     */
    public byte[] registerRemoteDevice() throws IOException
    {
        String otc;
        try {
            otc = RestHelper.addDevice(mRealmId, mUserId, mDeviceName);
        }
        catch (JSONException ex) {
            LOG.log(Level.WARNING, "Error parsing json", ex);
            return null;
        }
        if (otc == null) {
            LOG.warning("Can't generate OTC");
            return null;
        }
        byte[] encryptedOtc = encryptOtc(otc);
        if (encryptedOtc == null) {
            LOG.warning("Can't encrypt OTC");
            return null;
        }
        return encryptedOtc;
    }

    private byte[] encryptOtc(String otc)
    {
        // Write data to Qeo for sending to headless devices.
        return NativeQeo.getNativeSecurity().encryptOtc(otc, mRsaPublicKey);
    }

    /**
     * Convert registration request into registration credentials.
     *
     * @param req          The request.
     * @param encryptedOtc The encrypted OTC.
     * @param realmName    The name of the realm.
     * @return The registration credentials.
     */
    public static RegistrationCredentials createRegistrationCredentials(RegistrationRequest req, byte[] encryptedOtc,
                                                                        String realmName)
    {

        RegistrationCredentials r = new RegistrationCredentials();
        r.deviceId = req.deviceId;
        r.requestRSAPublicKey = req.rsaPublicKey;
        r.encryptedOtc = encryptedOtc;
        r.realmName = DeviceRegPref.getSelectedRealm();
        r.url = DeviceRegPref.getScepServerURL();
        return r;
    }


}
