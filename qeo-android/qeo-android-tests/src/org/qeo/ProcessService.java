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

package org.qeo;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.qeo.android.QeoAndroid;
import org.qeo.android.QeoConnectionListener;
import org.qeo.exception.QeoException;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

/**
 * Android service running in a separate process. Note that's impossible to get direct function call to this class from
 * a unit test since it will run in a separate process!
 */
public class ProcessService
    extends Service
{
    /** The intent identification string of this service. */
    public static final String INTENT_EXTRA_ID = "INTENT_ID";
    private static final String TAG = "JunitProcessService";
    private StateWriter<FeedBack> mFeedbackWriter = null;
    private Semaphore mSem;

    @Override
    public IBinder onBind(Intent intent)
    {
        return null; // no bind support
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        mSem = new Semaphore(0);

        Log.d(TAG, "Creating qeo");
        QeoAndroid.initQeo(getApplicationContext(), mConnectionListener, Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        FeedbackThread x = new FeedbackThread(intent.getExtras().getInt(INTENT_EXTRA_ID));
        x.start();
        return START_NOT_STICKY; // don't restart when killed
    }

    @Override
    public void onDestroy()
    {
        if (mFeedbackWriter != null) {
            mFeedbackWriter.close();
            mFeedbackWriter = null;
        }
        QeoAndroid.closeQeo(mConnectionListener);
    }

    private class FeedbackThread
        extends Thread
    {
        private final int mId;

        FeedbackThread(int id)
        {
            mId = id;
        }

        @Override
        public void run()
        {
            try {
                Log.d(TAG, "Waiting for qeo to be ready");
                if (mSem.tryAcquire(20, TimeUnit.SECONDS)) {
                    FeedBack f = new FeedBack();
                    f.id = mId;
                    f.ok = true;
                    f.pid = android.os.Process.myPid();
                    Log.d(TAG, "Sending feedback");
                    mFeedbackWriter.write(f);
                }
                else {
                    Log.e("ProcessService", "qeo not initialized in service");
                }
            }
            catch (InterruptedException e) {
                Log.e("ProcessService", "qeo not initialized in service", e);
            }
        }
    }

    private final QeoConnectionListener mConnectionListener = new QeoConnectionListener() {

        @Override
        public void onQeoReady(QeoFactory qeo)
        {
            Log.d(TAG, "Qeo ready");
            try {
                mFeedbackWriter = qeo.createStateWriter(FeedBack.class);
            }
            catch (QeoException e) {
                Log.e("ProcessService", "error creating feedback writer", e);
            }
            mSem.release();
        }

        @Override
        public void onQeoError(QeoException ex)
        {
            Log.e("ProcessService", "qeo error", ex);
        }

    };

    /**
     * Class that holds some feedback information to be sent over Qeo.
     */
    public static class FeedBack
    {
        /** Result. */
        public boolean ok;
        /** Identification. */
        @Key
        public int id;
        /** Process ID. */
        public int pid;
    }

}
