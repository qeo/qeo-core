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

package org.qeo.android.internal;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import android.util.Log;

/**
 * QeoLogger is used in Qeo library for Android and Qeo service to reconfigure a log handler. That QeoLoggerHandler will
 * handle all log messages and publishes them using the correct logcat methods.
 */
public final class QeoLogger
{
    private static final Logger LOG = Logger.getLogger("QeoLogger");
    private static boolean sInitialized = false;

    /**
     * The QeoLoggerHandler is responsible for publishing log records making use if the logcat methods. Following
     * mapping is used.
     * <p>
     * Level.FINEST -> Log.v
     * </p>
     * <p>
     * Level.FINER -> Log.v
     * </p>
     * <p>
     * Level.FINE -> Log.v
     * </p>
     * <p>
     * Level.CONFIG -> Log.d
     * </p>
     * <p>
     * Level.INFO -> Log.i
     * </p>
     * <p>
     * Level.WARNING -> Log.w
     * </p>
     * <p>
     * Level.SEVERE -> Log.e
     * </p>
     */
    private static class QeoLoggerHandler
        extends Handler
    {
        @Override
        public void close()
        {
        }

        @Override
        public void flush()
        {
        }

        // format logger message according to java default behavior
        private String getMsg(LogRecord record)
        {
            String msg = record.getMessage();
            if (record.getParameters() != null) {
                int i = 0;
                for (Object obj : record.getParameters()) {
                    msg = msg.replace("{" + i + "}", obj.toString());
                    ++i;
                }
            }
            return msg;
        }

        @Override
        public void publish(LogRecord record)
        {
            if (null != record) {
                if ((record.getLevel() == Level.FINEST) || (record.getLevel() == Level.FINER)
                    || (record.getLevel() == Level.FINE)) {
                    Log.v(record.getLoggerName(), getMsg(record), record.getThrown());
                }
                else if (record.getLevel() == Level.CONFIG) {
                    Log.d(record.getLoggerName(), getMsg(record), record.getThrown());
                }
                else if (record.getLevel() == Level.INFO) {
                    Log.i(record.getLoggerName(), getMsg(record), record.getThrown());
                }
                else if (record.getLevel() == Level.WARNING) {
                    Log.w(record.getLoggerName(), getMsg(record), record.getThrown());
                }
                else if (record.getLevel() == Level.SEVERE) {
                    Log.e(record.getLoggerName(), getMsg(record), record.getThrown());
                }
                else {
                    Log.wtf(record.getLoggerName(), getMsg(record), record.getThrown());
                }
            }
        }

    }

    private QeoLogger()
    {
    }

    /**
     * Initialize the QeoLogger. We will try to get the parent logger, loop over its handlers and remove the standard
     * AndroidHandler. This way we can install our own handler that will, compared to the AndroidHandler, also log the
     * lowest levels (CNONFIG, FINE, FINER, FINEST) is the log level is set accordingly.
     */
    public static synchronized void init()
    {
        if (!sInitialized) {
            try {
                List<Handler> handlers = new ArrayList<Handler>();
                Logger root = Logger.getLogger("");
                for (final Handler handler : root.getHandlers()) {
                    if (handler.getClass().getName().contentEquals("com.android.internal.logging.AndroidHandler")) {
                        handlers.add(handler);
                    }
                }
                for (Handler handler : handlers) {
                    root.removeHandler(handler);
                }
                root.addHandler(new QeoLoggerHandler());
                sInitialized = true;
            }
            catch (ConcurrentModificationException ex) {
                // very occasionally the removeHandler call fails. This seems a bug in android itself not being
                // threadsafe. so ignore if this happens.
                LOG.warning("ConcurrentModificationException during setting of the logger. Logging may be inaccurate");
            }
        }
    }
}
