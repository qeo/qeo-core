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

package org.qeo.android.service;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.android.internal.IServiceQeoPolicyUpdateCallback;
import org.qeo.android.internal.ParcelablePolicyIdentity;
import org.qeo.policy.AccessRule;
import org.qeo.policy.Identity;
import org.qeo.policy.PolicyUpdateListener;

/**
 * Wraps service policy update listener into actual Qeo policy update listener.
 */
public final class ConnectedPolicyUpdateListener
    implements PolicyUpdateListener
{
    private static final Logger LOG = Logger.getLogger(QeoService.TAG);
    private final IServiceQeoPolicyUpdateCallback mListener;

    /**
     * Construct new Qeo policy listener using service policy update listener.
     * 
     * @param listener The service listener
     */
    public ConnectedPolicyUpdateListener(IServiceQeoPolicyUpdateCallback listener)
    {
        mListener = listener;
    }

    @Override
    public AccessRule onPolicyUpdate(Identity identity)
    {
        boolean allow = false;

        try {
            LOG.finer("onPolicyUpdate: " + identity);
            allow = mListener.onPolicyUpdate(new ParcelablePolicyIdentity(identity));
        }
        catch (final Exception e) {
            LOG.log(Level.SEVERE, "mListener.onData", e);
        }
        return (allow ? AccessRule.ALLOW : AccessRule.DENY);
    }
}
