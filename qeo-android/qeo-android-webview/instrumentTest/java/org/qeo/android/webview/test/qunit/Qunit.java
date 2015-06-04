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

package org.qeo.android.webview.test.qunit;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.webkit.JavascriptInterface;

/**
 * Class to register for qunit callbacks
 */
public class Qunit
{
    private static final String TAG = "QUnit";
    private final Semaphore testDone;
    private final Semaphore testStart;
    private int failed;
    private int passed;
    private int total;
    private int runtime;
    private String mLastTestName;
    private final QunitListener mListener;
    private boolean mCompleted = false;
    private final List<TestResult> mResults;

    public Qunit()
    {
        this(null);
    }

    public Qunit(QunitListener listener)
    {
        testDone = new Semaphore(0);
        testStart = new Semaphore(0);
        mListener = listener;
        mResults = new LinkedList<TestResult>();
    }

    /**
     * Wait for test to finish.
     * 
     * @param seconds timeout in seconds
     * @throws InterruptedException Exception.
     */
    public void waitForTestDone(int seconds)
        throws InterruptedException
    {
        if (!testDone.tryAcquire(seconds, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Tests did not finish within " + seconds + " seconds (" + mLastTestName
                + ")");
        }
    }

    /**
     * Wait for a test to start or all tests to be completed
     * 
     * @param seconds timeout in seconds
     * @return True if test was started, false if all tests are done
     * @throws InterruptedException Exception
     */
    public boolean waitForStartOrCompleted(int seconds)
        throws InterruptedException
    {
        if (!testStart.tryAcquire(seconds, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Tests did not finish within " + seconds + " seconds");
        }
        return !mCompleted;
    }

    @JavascriptInterface
    public void jUnitReport(String reportS)
    {
        // not used. How to get the junit xml on a decent way transferred to jenkins?
    }

    @JavascriptInterface
    public boolean hasManifestSupport()
    {
        return true;
    }

    @JavascriptInterface
    public void done(String detailsS)
    {
        Log.d(TAG, "Qunit tests done");
        JSONObject details;
        try {
            details = new JSONObject(detailsS);
            failed = details.getInt("failed");
            passed = details.getInt("passed");
            total = details.getInt("total");
            runtime = details.getInt("runtime");
            Log.i(TAG, "QUNIT DONE: total: " + total + " passed: " + passed + " failed: " + failed);
        }
        catch (JSONException e) {
            Log.e(TAG, "json parse error", e);
        }
        mCompleted = true;
        testStart.release();
    }

    @JavascriptInterface
    public void testDone(String detailsS)
    {
        JSONObject details;
        try {
            details = new JSONObject(detailsS);
            if (mListener != null) {
                mListener.testDone(details);
            }
            String name = details.getString("name");
            int failedT = details.getInt("failed");
            int passedT = details.getInt("passed");
            int totalT = details.getInt("total");
            int durationT = details.getInt("duration");
            Log.d(TAG, "END: " + name + " -- total: " + totalT + " (duration: " + durationT + ")");
            if (failedT > 0) {
                Log.w(TAG, "FAIL: " + name + " -- failure: " + failedT);
            }
            mResults.add(new TestResult(name, passedT, failedT, totalT, durationT));
        }
        catch (JSONException e) {
            Log.e(TAG, "json parse error", e);
        }
        testDone.release();
    }

    @JavascriptInterface
    public void testStart(String detailsS)
    {
        JSONObject details;
        try {
            details = new JSONObject(detailsS);
            String name = details.getString("name");
            Log.d(TAG, "START: " + name);
            mLastTestName = name;
        }
        catch (JSONException e) {
            Log.e(TAG, "json parse error", e);
        }
        testStart.release();
    }

    @JavascriptInterface
    public void log(String detailsS)
    {
        JSONObject details;
        try {
            details = new JSONObject(detailsS);
            boolean result = details.getBoolean("result");
            if (!result) {
                String source = details.getString("source");
                String message = details.optString("message", null);
                if (message == null) {
                    Log.e(TAG, "LOG ERROR: " + details.optString("actual") + " != " + details.optString("expected")
                        + "\n" + source);
                }
                else {
                    Log.e(TAG, "LOG ERROR: " + source + " -- " + message);
                }
            }
        }
        catch (JSONException e) {
            Log.e(TAG, "json parse error", e);
        }
    }

    public int getPassed()
    {
        return passed;
    }

    public int getTotal()
    {
        return total;
    }

    public int getRuntime()
    {
        return runtime;
    }

    public int getFailed()
    {
        return failed;
    }

    public List<TestResult> getResults()
    {
        return Collections.unmodifiableList(mResults);
    }

    public interface QunitListener
    {
        void testDone(JSONObject details);
    }

    public static class TestResult
    {
        private final String mName;
        private final int mPassed;
        private final int mFailed;

        private final int mTotal;
        private final int mDuration;

        public TestResult(String name, int passed, int failed, int total, int duration)
        {
            mName = name;
            mPassed = passed;
            mFailed = failed;
            mTotal = total;
            mDuration = duration;
        }

        public String getName()
        {
            return mName;
        }

        public int getPassed()
        {
            return mPassed;
        }

        public int getFailed()
        {
            return mFailed;
        }

        public int getTotal()
        {
            return mTotal;
        }

        public int getDuration()
        {
            return mDuration;
        }
    }

}
