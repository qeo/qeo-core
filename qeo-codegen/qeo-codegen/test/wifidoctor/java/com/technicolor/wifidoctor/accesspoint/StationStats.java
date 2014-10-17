/*
 * Copyright (c) 2014 - Qeo LLC
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

/**************************************************************
 ********          THIS IS A GENERATED FILE         ***********
 **************************************************************/

package com.technicolor.wifidoctor.accesspoint;

import org.qeo.QeoType;

@QeoType
public class StationStats
{
    /**
     * MAC address associated with station
     */
    public String MACAddress;

    /**
     * expressed in Mbps
     */
    public int maxPhyRate;

    /**
     * expressed in dBm
     */
    public int RSSIuplink;

    public float avgSpatialStreamsUplink;

    public float avgSpatialStreamsDownlink;

    public int trainedPhyRateUplink;

    public int trainedPhyRateDownlink;

    public int dataRateUplink;

    public int dataRateDownlink;

    public int pctPowerSave;

    /**
     * Default constructor.  This is used by Qeo to construct new objects.
     */
    public StationStats()
    {
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }
        if ((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }
        final StationStats myObj = (StationStats) obj;
        if (!MACAddress.equals(myObj.MACAddress)) {
            return false;
        }
        if (maxPhyRate != myObj.maxPhyRate) {
            return false;
        }
        if (RSSIuplink != myObj.RSSIuplink) {
            return false;
        }
        if (avgSpatialStreamsUplink != myObj.avgSpatialStreamsUplink) {
            return false;
        }
        if (avgSpatialStreamsDownlink != myObj.avgSpatialStreamsDownlink) {
            return false;
        }
        if (trainedPhyRateUplink != myObj.trainedPhyRateUplink) {
            return false;
        }
        if (trainedPhyRateDownlink != myObj.trainedPhyRateDownlink) {
            return false;
        }
        if (dataRateUplink != myObj.dataRateUplink) {
            return false;
        }
        if (dataRateDownlink != myObj.dataRateDownlink) {
            return false;
        }
        if (pctPowerSave != myObj.pctPowerSave) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((MACAddress == null) ? 0 : MACAddress.hashCode());
        result = prime * result + maxPhyRate;
        result = prime * result + RSSIuplink;
        result = prime * result + Float.floatToIntBits(avgSpatialStreamsUplink);
        result = prime * result + Float.floatToIntBits(avgSpatialStreamsDownlink);
        result = prime * result + trainedPhyRateUplink;
        result = prime * result + trainedPhyRateDownlink;
        result = prime * result + dataRateUplink;
        result = prime * result + dataRateDownlink;
        result = prime * result + pctPowerSave;
        return result;
    }
}
