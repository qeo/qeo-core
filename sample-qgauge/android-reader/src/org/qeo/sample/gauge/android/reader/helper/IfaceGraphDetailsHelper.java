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

package org.qeo.sample.gauge.android.reader.helper;

import org.achartengine.model.TimeSeries;

/**
 * Helper class to save the different types of data for interface to handle screen rotation.
 */
public class IfaceGraphDetailsHelper
{
    private TimeSeries mCurrentdownstreamValue;

    private TimeSeries mCurrentupstreamValue;

    private int mGraphClickState;

    private int mCount;

    /**
     * Setter method for current saved graph state.
     * 
     * @param currentGraphState -graph clicked state to set
     */
    public void setGraphClickState(int currentGraphState)
    {
        this.mGraphClickState = currentGraphState;
    }

    /**
     * Getter method for current Graph state.
     * 
     * @return Returns the current status of graph.
     */
    public int getGraphClickState()
    {
        return mGraphClickState;
    }

    /**
     * Setter method for downstream TimeSeries.
     * 
     * @param currentdownstreamSeries -downstream Series to set
     */
    public void setCurrentdownstreamSeries(TimeSeries currentdownstreamSeries)
    {
        this.mCurrentdownstreamValue = currentdownstreamSeries;
    }

    /**
     * Getter method for current downstream TimeSeries value.
     * 
     * @return Returns the current downstream TimeSeries.
     */
    public TimeSeries getCurrentdownstreamSeries()
    {
        return mCurrentdownstreamValue;
    }

    /**
     * Setter method for upstream TimeSeries.
     * 
     * @param currentupstreamValue -upstream Series to set
     */
    public void setCurrentupstreamSeries(TimeSeries currentupstreamValue)
    {
        this.mCurrentupstreamValue = currentupstreamValue;
    }

    /**
     * Getter method for current upstream TimeSeries value.
     * 
     * @return Returns the current upstream TimeSeries.
     */
    public TimeSeries getCurrentupstreamSeries()
    {
        return mCurrentupstreamValue;
    }

    /**
     * Getter method to retrieve the current sample number.
     * 
     * @return Current sample number
     */
    public int getCount()
    {
        return mCount;
    }

    /**
     * Set the current sample number.
     * 
     * @param count The sample number to set
     */
    public void setCount(int count)
    {
        this.mCount = count;
    }
}
