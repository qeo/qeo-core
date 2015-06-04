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

package org.qeo.deviceregistration.rest;

import java.util.List;

/**
 * This class represents a REST response holding the response code as well as the data.
 */
public class RestResponse
{
    private String mData;
    private List<String> mDetailedData;
    private int mCode;
    private long mDataLong;

    /**
     * Empty constructor.
     */
    public RestResponse()
    {
    }

    /**
     * Construct the RestResponse object with data and return code.
     * 
     * @param data - REST call data to return.
     * @param code - REST call result code.
     */
    public RestResponse(String data, int code)
    {
        mData = data;
        mCode = code;
    }

    /**
     * Construct the RestResponse object with list of data and return code.
     * 
     * @param data - list of data to return if REST call success
     * @param code - REST call result code
     */
    public RestResponse(List<String> data, int code)
    {
        mDetailedData = data;
        mCode = code;
    }

    /**
     * Get the result data from the RestResponse object.
     * 
     * @return REST API call result data.
     */
    public String getData()
    {
        return mData;
    }

    /**
     * Get the result data in list format from the RestResponse object.
     * 
     * @return REST API call result data in list format.
     */
    public List<String> getListData()
    {
        return mDetailedData;
    }

    /**
     * Get the result code from the RestResponse object.
     * 
     * @return REST API call result code.
     */
    public int getCode()
    {
        return mCode;
    }

    /**
     * Set some long data.
     * 
     * @param data data.
     */
    public void setDataLong(long data)
    {
        mDataLong = data;
    }

    /**
     * Return some long data set.
     * 
     * @return data.
     */
    public long getLongData()
    {
        return mDataLong;
    }
}
