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

package org.qeo.sample.gauge;

/**
 * Container class for computed network Interface data.
 */
public class NetworkInterfaceSpeedData
{

    private String mDeviceId;

    private final String mIfName;

    private double mKbpsIn;
    private double mPcktsIn;
    private double mKbpsOut;
    private double mPcktsOut;

    /**
     * Constructor.
     *
     * @param ifName non-null interface name
     *
     */
    NetworkInterfaceSpeedData(String ifName)

    {
        if (ifName == null) {
            throw new NullPointerException();
        }
        mIfName = ifName;
    }

    /**
     * Getter method for interface name.
     *
     * @return Returns the interface name
     */
    public String getIfaceName()
    {
        return mIfName;
    }

    /**
     * Setter method for DeviceId.
     * 
     * @param deviceId -non null DeviceId to set
     */
    public void setDeviceId(String deviceId)
    {
        this.mDeviceId = deviceId;
    }

    /**
     * Getter method for Device Id.
     *
     * @return Returns the Device Id
     */
    public String getDeviceId()
    {
        return mDeviceId;
    }

    /**
     * Setter method for calculated data received in kilobytes per second over a certain interval..
     *
     * @param kbpsIn -data received in kilobytes
     */
    public void setKbpsIn(double kbpsIn)
    {
        this.mKbpsIn = kbpsIn;
    }

    /**
     * Getter method for data received in kilobytes/sec.
     *
     * @return Returns the data received in kilobytes
     */
    public double getKbpsIn()
    {
        return mKbpsIn;
    }

    /**
     * Setter method for calculated data received in packets per second over a certain interval..
     *
     * @param pcktsIn -data received in packets/sec
     */
    public void setPcktsIn(double pcktsIn)
    {
        this.mPcktsIn = pcktsIn;
    }

    /**
     * Getter method for data received in packets/sec.
     *
     * @return Returns the data received in packets.
     */
    public double getPcktsIn()
    {
        return mPcktsIn;
    }

    /**
     * Setter method for calculated data transmitted over in kilobytes per second over a certain interval..
     *
     * @param kbpsOut -data transmitted in kilobytes
     */
    public void setKbpsOut(double kbpsOut)
    {
        this.mKbpsOut = kbpsOut;
    }

    /**
     * Getter method for data transmitted in kilobytes/sec.
     *
     * @return Returns the data transmitted in kilobytes
     */
    public double getKbpsOut()
    {
        return mKbpsOut;
    }

    /**
     * Setter method for calculated data transmitted over in packets per second over a certain interval..
     *
     * @param pcktsOut -data transmitted in packets/sec
     */
    public void setPcktsOut(double pcktsOut)
    {
        this.mPcktsOut = pcktsOut;
    }

    /**
     * Getter method for data transmitted in packets/sec.
     *
     * @return Returns the data transmitted in packets
     */
    public double getPcktsOut()
    {
        return mPcktsOut;
    }

}
