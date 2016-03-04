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

package org.qeo.deviceregistration.model;

import org.qeo.deviceregistration.helper.ErrorCode;
import org.qeo.deviceregistration.helper.RegistrationStatusCode;

/**
 * 
 * Device Interface needs to be implemented to display the Device details viz. name, description, interfaces to
 * differentiate between different devices.
 * 
 */
public interface UnRegisteredDevice
{

    /**
     * Gets the name of device.
     * 
     * @return model name of device
     */
    String getModelName();

    /**
     * Gets the manufacturer of device.
     * 
     * @return manufacturer name of device
     */
    String getManufacturerName();

    /**
     * Gets the user friendly of device.
     * 
     * @return User friendly name of device
     */
    String getUserFriendlyName();

    /**
     * Gets the Username of device.
     * 
     * @return Username of device
     */
    String getUserName();

    /**
     * Gets the RegistrationStatusCode of device.
     * 
     * @return current status of device
     */
    RegistrationStatusCode getRegistrationStatus();

    /**
     * Returns the errorcode if device registration fails.
     * 
     * @return error code.
     */
    ErrorCode getErrorCode();
    /**
     * Returns human readable error message.
     * 
     * @return error message in string format.
     */
    String getErrorMessage();
    /**
     * Returns the pem encoded string format of public key..
     * 
     * @return public key
     */
    String getRSAPublicKey();
    /**
     * Returns the upper bit of Qeo deviceId.
     * 
     * @return upper bit of deviceId
     */
    Long getUpper();
    /**
     * Returns the lower bit of Qeo deviceId.
     * 
     * @return lower bit of deviceId
     */
    Long getLower();

    /**
     * Returns the version number of encryption procedure for OTC.
     * 
     * @return version number.
     */
    Short getVersion();


}
