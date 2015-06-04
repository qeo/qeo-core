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

package org.qeo.android.internal;

import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.android.QeoAndroid;
import org.qeo.policy.AccessRule;
import org.qeo.policy.Identity;
import org.qeo.policy.PolicyUpdateListener;

import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;

/**
 * This is the actual implementation of the IServiceQeoPolicyUpdateCallback AIDL interface.
 */
class PolicyUpdateHandler
    extends IServiceQeoPolicyUpdateCallback.Stub
{
    private static final String TAG = "PolicyUpdateHandler";
    private static final Logger LOG = Logger.getLogger(QeoAndroid.TAG + "." + TAG);
    private final Handler mHandler;
    private final PolicyUpdateListener mPolicyListener;

    /**
     * Create an instance.
     * 
     * @param looper the looper for callbacks. Can be null to use default looper.
     * @param policyUpdateListener The listener where the policyupdates should be passed to.
     */
    public PolicyUpdateHandler(Looper looper, PolicyUpdateListener policyUpdateListener)
    {
        if (looper == null) {
            mHandler = new Handler();
        }
        else {
            mHandler = new Handler(looper);
        }
        mPolicyListener = policyUpdateListener;
    }

    @Override
    public boolean onPolicyUpdate(final ParcelablePolicyIdentity identity)
        throws RemoteException
    {
        LOG.fine("policy update callback");
        AccessRule perm = null;
        try {
            PolicyUpdaterRunnable pur;
            if (Thread.currentThread().getId() == mHandler.getLooper().getThread().getId()) {
                // threadId is the same as the looper, so just call the function directly
                pur = new PolicyUpdaterRunnable(identity.getIdentity(), null);
                pur.run(); // execute runnable directly, don't put on a thread
            }
            else {
                final Semaphore sem = new Semaphore(0);
                // post on handler thread.
                pur = new PolicyUpdaterRunnable(identity.getIdentity(), sem);
                mHandler.post(pur);
                // wait for callback to be completed
                sem.acquire();
            }
            perm = pur.mPerm;

        }
        catch (Exception e) {
            LOG.log(Level.WARNING, "Uncaught exception in onPolicyUpdate", e);
        }
        return (AccessRule.ALLOW == perm ? true : false);
    }

    private class PolicyUpdaterRunnable
        implements Runnable
    {
        AccessRule mPerm;
        private final Identity mIdentity;
        private final Semaphore mSem;

        public PolicyUpdaterRunnable(Identity identity, Semaphore sem)
        {
            mIdentity = identity;
            mSem = sem;
        }

        @Override
        public void run()
        {
            mPerm = mPolicyListener.onPolicyUpdate(mIdentity);
            if (mSem != null) {
                mSem.release();
            }

        }
    }

}
