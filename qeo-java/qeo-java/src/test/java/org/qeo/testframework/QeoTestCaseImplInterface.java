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

package org.qeo.testframework;

import org.qeo.internal.QeoListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

/**
 * Interface that specifies functions that have to be implemented in QeoTestCaseImpl.
 */
public interface QeoTestCaseImplInterface
{
    void setUp(boolean initQeo)
        throws Exception;

    /**
     * Create a factory and block untill ready.
     * 
     * @param id The id (open/closed)
     * @return The connection listener.
     */
    QeoConnectionTestListener createFactory(int id)
        throws InterruptedException;

    /**
     * Get the open domain id.
     * 
     * @return the id.
     */
    int getFactoryOpenId();

    /**
     * Get the closed domain id
     * 
     * @return the id.
     */
    int getFactoryClosedId();

    /**
     * Close a factory.
     * 
     * @param qeoConnectionListener the connection listener.
     */
    void closeFactory(QeoConnectionTestListener qeoConnectionListener);

    /**
     * Run a runnable on the qeo thread.
     * 
     * @param r the runnable.
     */
    void runOnQeoThread(Runnable r);

    /**
     * Create a factory, handling listener callback self. Don't block until ready but return immediately.
     * 
     * @param listener the callback listener.
     * @return the connection listener.
     */
    QeoConnectionTestListener createFactory(QeoListener listener);

    /**
     * Wait for factory semaphore to become ready, checking the dirty flag first.
     * 
     * @param sem the semaphore.
     * @throws InterruptedException exception
     */
    void waitForFactoryInit(Semaphore sem)
        throws InterruptedException;

    /**
     * Open an asset test file.
     * @param name The name of the file.
     * @return the inputstream.
     * @throws IOException on error.
     */
    InputStream openAsset(String name) throws IOException;

}
