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

import org.qeo.android.internal.ParcelablePolicyIdentity;

/**
 * This interface contains the policy update callback method. The qeo service uses this callback
 * to notify applications about the state of their reader.
 */
interface IServiceQeoPolicyUpdateCallback
{
    /**
     * This callback is called whenever the policy is being updated. This callback enables an application to restrict
     * the list of identities that are allowed for reading. During a policy update it will be called for each identity
     * that is relevant. At the end of the update it will be called with a null identity to signal the end of the
     * update.
     * 
     * @param identity The identity The identity that is about to be allowed.
     * 
     * @return To allow the identity return true. To disallow the identity return
     *         false.
     */
    boolean onPolicyUpdate(in ParcelablePolicyIdentity identity);
}
