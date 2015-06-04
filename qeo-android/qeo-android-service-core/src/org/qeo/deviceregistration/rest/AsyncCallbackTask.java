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

import android.os.AsyncTask;

/**
 * AsyncTask extension that allows to set a onPostExecute callback after the task was created.
 * @param <RETURN> The return type of the AsyncTask.
 */
public abstract class AsyncCallbackTask<RETURN> extends AsyncTask<Void, Void, RETURN>
{
    private AsyncTaskCallbacks<RETURN> mCallback;

    @Override
    protected void onPostExecute(RETURN aReturn)
    {
        super.onPostExecute(aReturn);
        if (mCallback != null) {
            mCallback.onPostExecute(aReturn);
        }
    }

    /**
     * Set the callback.
     * @param callbacks the callback.
     */
    public void setCallbacks(AsyncTaskCallbacks<RETURN> callbacks)
    {
        mCallback = callbacks;
    }

    /**
     * Interface that defines callbacks from the AsyncCallbackTask class.
     * @param <RETURN> The return type of the AsyncTask
     */
    public interface AsyncTaskCallbacks<RETURN>
    {
        /**
         * the onPostExecute from the AsyncTask.
         * @param x The return value.
         */
        void onPostExecute(RETURN x);
    }
}

