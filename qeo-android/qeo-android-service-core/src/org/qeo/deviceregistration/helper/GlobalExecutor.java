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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;

/**
 * Class that contains an executor for submitting runnables.
 */
public class GlobalExecutor
{

    private final Executor mPoolService;

    /**
     * Constructor to initialize thread pool.
     */
    @SuppressLint("NewApi")
    public GlobalExecutor()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // if on Honeycomb or higher, re-use the threadpool from asynctask
            mPoolService = AsyncTask.THREAD_POOL_EXECUTOR;
        }
        else {
            // on 2.3, create our own pool
            mPoolService = Executors.newCachedThreadPool();
        }
    }

    /**
     * Api to submit the task to thread pool.
     * 
     * @param task Runnable instance to be submitted to thread Pool.
     */
    public void submitTaskToPool(Runnable task)
    {
        mPoolService.execute(task);
    }

}
